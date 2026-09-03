package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaNotificationPolicyService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketAttachmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import com.ntn.fziot.mailtrace.application.bizservice.ticket.TicketReplyDeliveryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 邮件发送服务。负责 SMTP 发信、不可变发送快照、失败重试与关联消息状态回写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendService {

    public static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    public static final String CONTENT_TYPE_TEXT = "text/plain; charset=UTF-8";
    private static final String OPERATOR_SYSTEM = "SYSTEM";
    private static final long MAX_ATTACHMENT_BYTES = 25L * 1024 * 1024;

    private final MailboxMapper mailboxMapper;
    private final MailSendLogMapper mailSendLogMapper;
    private final TicketAttachmentMapper ticketAttachmentMapper;
    private final FileStorageService fileStorageService;
    private final MailPasswordCipher mailPasswordCipher;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    private final MailDeliveryStateService deliveryStateService;
    private final TicketReplyDeliveryService ticketReplyDeliveryService;
    private final SlaNotificationPolicyService slaNotificationPolicyService;

    /**
     * 独立通知发送入口。分配、SLA、测试邮件不构造工单会话关系。
     */
    public SendResult sendRawMail(Long mailboxId, String toAddress, String subject, String content, String sendType) {
        return sendRawMail(mailboxId, toAddress, subject, content, sendType, null, null, sendType);
    }

    public SendResult sendRawMail(Long mailboxId, String toAddress, String subject, String content, String sendType,
                                  Long ticketId, Long templateId, String templateType) {
        return sendOutboundMail(new OutboundMailRequest(
                mailboxId, ticketId, null, templateId, templateType, sendType,
                toAddress, subject, content, detectContentType(content),
                null, null, null, null));
    }

    public SendResult sendRawMail(Long mailboxId, String toAddress, String subject, String content) {
        return sendRawMail(mailboxId, toAddress, subject, content, "AUTO_REPLY");
    }

    /**
     * 工单线程邮件入口。请求中的线程字段会在 SMTP 前完整保存，后续重试只读取快照。
     */
    public SendResult sendThreadedMail(OutboundMailRequest request) {
        if (request == null) {
            return SendResult.fail("发送请求不能为空");
        }
        return sendOutboundMail(request);
    }

    private SendResult sendOutboundMail(OutboundMailRequest request) {
        enterpriseMailboxAccessService.assertSystemMailboxOperational(request.mailboxId());
        MailboxEntity mailbox = mailboxMapper.selectById(request.mailboxId());
        if (mailbox == null) {
            return SendResult.fail("邮箱不存在：" + request.mailboxId());
        }
        String smtpPassword;
        try {
            smtpPassword = mailPasswordCipher.decrypt(mailbox.getSmtpPasswordEnc());
        } catch (Exception e) {
            return SendResult.fail("SMTP 密码解密失败");
        }

        String fromAddress = getFromAddress(mailbox);
        String messageId = MailThreadHeaders.normalizeMessageId(request.messageId());
        if (messageId == null) {
            messageId = MailThreadHeaders.generateMessageId(fromAddress);
        }
        String inReplyTo = MailThreadHeaders.normalizeMessageId(request.inReplyTo());
        String references = MailThreadHeaders.normalizeReferences(request.references());
        String replyTo = normalizeAddress(request.replyToAddress());
        String contentType = normalizeContentType(request.contentType(), request.content());

        OutboundMailRequest snapshot = new OutboundMailRequest(
                request.mailboxId(), request.ticketId(), request.ticketMessageId(),
                request.templateId(), blankToDefault(request.templateType(), request.sendType()),
                request.sendType(), request.toAddress(), request.subject(), request.content(), contentType,
                messageId, inReplyTo, references, replyTo);
        MailSendLogEntity sendLog = createLog(mailbox, snapshot);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            Long sendLogId = sendLog.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        dispatchPending(sendLogId);
                    } catch (RuntimeException exception) {
                        log.error("事务提交后的邮件发送调度异常 sendLogId={}", sendLogId, exception);
                    }
                }
            });
            return SendResult.queued("邮件已进入发送队列", snapshot.messageId());
        }
        return dispatchPending(sendLog.getId());
    }

    public SendResult sendTestMail(Long mailboxId, String toAddress) {
        enterpriseMailboxAccessService.assertSystemMailboxOperational(mailboxId);
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        if (mailbox == null) {
            return SendResult.fail("邮箱不存在：" + mailboxId);
        }
        if (mailbox.getSmtpHost() == null || mailbox.getSmtpHost().isBlank()) {
            return SendResult.fail("邮箱未配置 SMTP：请先填写 SMTP 服务器地址");
        }
        return sendRawMail(mailboxId, toAddress, "MailTrace 测试邮件",
                "这是一封来自 MailTrace 邮件工单系统的测试邮件。\n\n如果您收到此邮件，说明 SMTP 配置正确，发信服务正常工作。",
                "TEST", null, null, "TEST");
    }

    public SendResult sendTestMail(CurrentUserPrincipal principal, Long mailboxId, String toAddress) {
        permissionService.assertPermission(principal, "mail_send:test", "无权发送测试邮件");
        enterpriseMailboxAccessService.assertMailboxOperational(principal, mailboxId);
        return sendTestMail(mailboxId, toAddress);
    }

    public SendResult dispatchPending(Long sendLogId) {
        if (!deliveryStateService.claimInitial(sendLogId)) {
            return SendResult.fail("发送任务已被其他执行器处理：" + sendLogId);
        }
        return deliverClaimed(sendLogId, false);
    }

    /**
     * 重试失败邮件。线程头、Message-ID、主题、正文和 MIME 类型全部来自首次发送快照。
     */
    public SendResult retrySend(Long sendLogId) {
        MailSendLogEntity logEntity = mailSendLogMapper.selectById(sendLogId);
        if (logEntity == null) {
            return SendResult.fail("发送日志不存在：" + sendLogId);
        }
        if (!"FAILED".equals(logEntity.getSendStatus())) {
            return SendResult.fail("只有失败状态的记录可以重试，当前状态：" + logEntity.getSendStatus());
        }
        int retryCount = logEntity.getRetryCount() == null ? 0 : logEntity.getRetryCount();
        int maxRetry = logEntity.getMaxRetry() == null ? 5 : logEntity.getMaxRetry();
        if (retryCount >= maxRetry) {
            return SendResult.fail("已达到最大重试次数：" + maxRetry);
        }
        enterpriseMailboxAccessService.assertSystemMailboxOperational(logEntity.getMailboxId());
        MailboxEntity mailbox = mailboxMapper.selectById(logEntity.getMailboxId());
        if (mailbox == null) {
            return SendResult.fail("发件邮箱不存在：" + logEntity.getMailboxId());
        }
        String smtpPassword;
        try {
            smtpPassword = mailPasswordCipher.decrypt(mailbox.getSmtpPasswordEnc());
        } catch (Exception e) {
            return SendResult.fail("SMTP 密码解密失败");
        }
        if (!deliveryStateService.claimRetry(sendLogId)) {
            return SendResult.fail("发送任务已被其他执行器处理：" + sendLogId, logEntity.getMessageId());
        }
        return deliverClaimed(sendLogId, true);
    }

    public SendResult retrySend(CurrentUserPrincipal principal, Long sendLogId) {
        permissionService.assertPermission(principal, "mail_send:retry", "无权重试发送邮件");
        MailSendLogEntity logEntity = mailSendLogMapper.selectById(sendLogId);
        if (logEntity != null) {
            enterpriseMailboxAccessService.assertMailboxOperational(principal, logEntity.getMailboxId());
        }
        return retrySend(sendLogId);
    }

    private SendResult deliverClaimed(Long sendLogId, boolean retry) {
        MailSendLogEntity logEntity = deliveryStateService.reload(sendLogId);
        if (logEntity == null) {
            return SendResult.fail("发送日志不存在：" + sendLogId);
        }
        if (!slaNotificationPolicyService.isDeliveryEnabled(logEntity.getTicketId(), logEntity.getSendType())) {
            deliveryStateService.markCancelled(sendLogId, "SLA通知节点已关闭或工单禁止通知");
            return SendResult.fail("SLA 通知已按当前策略取消", logEntity.getMessageId());
        }
        MailboxEntity mailbox = mailboxMapper.selectById(logEntity.getMailboxId());
        if (mailbox == null) {
            return persistKnownFailure(logEntity, "发件邮箱不存在：" + logEntity.getMailboxId(), retry);
        }
        String smtpPassword;
        try {
            enterpriseMailboxAccessService.assertSystemMailboxOperational(logEntity.getMailboxId());
            smtpPassword = mailPasswordCipher.decrypt(mailbox.getSmtpPasswordEnc());
        } catch (Exception exception) {
            return persistKnownFailure(logEntity, truncateError(exception), retry);
        }

        long start = System.currentTimeMillis();
        Transport transport = null;
        OutboundMailRequest snapshot = buildRetrySnapshot(mailbox, logEntity);

        try {
            List<MailAttachmentContent> attachments = loadAttachments(snapshot.ticketMessageId());
            transport = openTransport(mailbox, smtpPassword);
            MimeMessage message = buildMessage(mailbox, getFromAddress(mailbox), snapshot, attachments);
            transport.sendMessage(message, message.getAllRecipients());
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String errorMsg = truncateError(e);
            log.warn("邮件发送失败 sendLogId={} retry={} to={} messageId={} 耗时={}ms reason={}",
                    logEntity.getId(), retry, snapshot.toAddress(), snapshot.messageId(), elapsed, errorMsg);
            return persistKnownFailure(logEntity, errorMsg, retry);
        } finally {
            closeTransport(transport);
        }

        long elapsed = System.currentTimeMillis() - start;
        LocalDateTime sentAt = LocalDateTime.now();
        log.info("邮件发送成功 sendLogId={} retry={} to={} subject={} messageId={} 耗时={}ms",
                logEntity.getId(), retry, snapshot.toAddress(), snapshot.subject(), snapshot.messageId(), elapsed);
        try {
            deliveryStateService.markSuccess(logEntity.getId(), sentAt);
        } catch (RuntimeException exception) {
            log.error("SMTP 已接收邮件，但本地成功状态回写失败；停止自动重试 sendLogId={} messageId={}",
                    logEntity.getId(), snapshot.messageId(), exception);
            markDeliveryUnknown(logEntity.getId(), "SMTP已接收，但本地成功状态回写失败，请人工核实");
            return SendResult.unknown("SMTP 已接收邮件，但本地状态待人工核实", snapshot.messageId());
        }
        try {
            ticketReplyDeliveryService.completeBySendLog(logEntity.getId());
        } catch (RuntimeException exception) {
            log.error("邮件已成功投递，但客服回复工单状态补偿失败 sendLogId={}", logEntity.getId(), exception);
        }
        return SendResult.ok((retry ? "重试" : "") + "发送成功，耗时 " + elapsed + "ms", snapshot.messageId());
    }

    private SendResult persistKnownFailure(MailSendLogEntity logEntity, String errorMessage, boolean retry) {
        try {
            deliveryStateService.markFailed(logEntity.getId(), errorMessage);
            return SendResult.fail((retry ? "重试失败：" : "发送失败：") + errorMessage,
                    logEntity.getMessageId());
        } catch (RuntimeException stateException) {
            log.error("SMTP 发送失败后，本地失败状态回写异常；停止自动重试 sendLogId={}",
                    logEntity.getId(), stateException);
            markDeliveryUnknown(logEntity.getId(), "SMTP异常且本地失败状态回写失败，请人工核实");
            return SendResult.unknown("发送结果状态无法可靠落库，请人工核实", logEntity.getMessageId());
        }
    }

    private void markDeliveryUnknown(Long sendLogId, String reason) {
        try {
            if (!deliveryStateService.markUnknown(sendLogId, reason)) {
                log.error("邮件投递未知状态未能立即落库 sendLogId={}", sendLogId);
            }
        } catch (RuntimeException exception) {
            log.error("邮件投递未知状态落库异常 sendLogId={}", sendLogId, exception);
        }
    }

    OutboundMailRequest buildRetrySnapshot(MailboxEntity mailbox, MailSendLogEntity logEntity) {
        String messageId = MailThreadHeaders.normalizeMessageId(logEntity.getMessageId());
        if (messageId == null) {
            messageId = MailThreadHeaders.generateMessageId(getFromAddress(mailbox));
            logEntity.setMessageId(messageId);
        }
        return new OutboundMailRequest(
                mailbox.getId(), logEntity.getTicketId(), logEntity.getTicketMessageId(),
                logEntity.getTemplateId(), logEntity.getTemplateType(), logEntity.getSendType(),
                logEntity.getToAddress(), logEntity.getSubject(), logEntity.getContentBody(),
                normalizeContentType(logEntity.getContentType(), logEntity.getContentBody()),
                messageId, MailThreadHeaders.normalizeMessageId(logEntity.getInReplyTo()),
                MailThreadHeaders.normalizeReferences(logEntity.getMailReferences()),
                normalizeAddress(logEntity.getReplyToAddress()));
    }

    MimeMessage buildMessage(MailboxEntity mailbox, String fromAddress,
                             OutboundMailRequest snapshot) throws Exception {
        return buildMessage(mailbox, fromAddress, snapshot, List.of());
    }

    MimeMessage buildMessage(MailboxEntity mailbox, String fromAddress,
                             OutboundMailRequest snapshot,
                             List<MailAttachmentContent> attachments) throws Exception {
        Properties mailProps = new Properties();
        mailProps.put("mail.mime.charset", "UTF-8");
        MimeMessage message = new FixedMessageIdMimeMessage(
                Session.getInstance(mailProps), snapshot.messageId());
        message.setFrom(new InternetAddress(fromAddress, mailbox.getSmtpFromName(), "UTF-8"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(snapshot.toAddress()));
        message.setSubject(snapshot.subject(), "UTF-8");
        if (snapshot.replyToAddress() != null) {
            message.setReplyTo(new InternetAddress[]{new InternetAddress(snapshot.replyToAddress())});
        }
        String inReplyToHeader = MailThreadHeaders.toHeaderValue(snapshot.inReplyTo());
        if (inReplyToHeader != null) {
            message.setHeader("In-Reply-To", inReplyToHeader);
        }
        if (snapshot.references() != null) {
            message.setHeader("References", snapshot.references());
        }
        if (attachments == null || attachments.isEmpty()) {
            if (CONTENT_TYPE_HTML.equals(snapshot.contentType())) {
                message.setContent(snapshot.content() == null ? "" : snapshot.content(), CONTENT_TYPE_HTML);
            } else {
                message.setText(snapshot.content() == null ? "" : snapshot.content(), "UTF-8");
            }
        } else {
            MimeMultipart multipart = new MimeMultipart("mixed");
            MimeBodyPart bodyPart = new MimeBodyPart();
            if (CONTENT_TYPE_HTML.equals(snapshot.contentType())) {
                bodyPart.setContent(snapshot.content() == null ? "" : snapshot.content(), CONTENT_TYPE_HTML);
            } else {
                bodyPart.setText(snapshot.content() == null ? "" : snapshot.content(), "UTF-8");
            }
            multipart.addBodyPart(bodyPart);
            for (MailAttachmentContent attachment : attachments) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new DataHandler(new ByteArrayDataSource(
                        attachment.content(), attachment.contentType())));
                attachmentPart.setFileName(attachment.fileName());
                attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT);
                multipart.addBodyPart(attachmentPart);
            }
            message.setContent(multipart);
        }
        message.saveChanges();
        return message;
    }

    private Transport openTransport(MailboxEntity mailbox, String smtpPassword) throws MessagingException {
        Integer port = mailbox.getSmtpPort() == null ? 587 : mailbox.getSmtpPort();
        boolean ssl = Boolean.TRUE.equals(mailbox.getSmtpSslEnabled());
        boolean implicitSsl = ssl && port == 465;
        String protocol = implicitSsl ? "smtps" : "smtp";
        String prefix = "mail." + protocol;

        Properties props = new Properties();
        props.put(prefix + ".connectiontimeout", "15000");
        props.put(prefix + ".timeout", "15000");
        props.put(prefix + ".writetimeout", "15000");
        props.put(prefix + ".auth", "true");
        props.put(prefix + ".starttls.enable", String.valueOf(ssl && !implicitSsl));
        props.put(prefix + ".ssl.enable", String.valueOf(implicitSsl));

        Session session = Session.getInstance(props);
        Transport transport = session.getTransport(protocol);
        transport.connect(mailbox.getSmtpHost(), port, mailbox.getSmtpUsername(), smtpPassword);
        return transport;
    }

    private String getFromAddress(MailboxEntity mailbox) {
        if (mailbox.getSmtpUsername() != null && !mailbox.getSmtpUsername().isBlank()) {
            return mailbox.getSmtpUsername();
        }
        if (mailbox.getEmailAddress() != null && !mailbox.getEmailAddress().isBlank()) {
            return mailbox.getEmailAddress();
        }
        return "noreply@ntn.fziot";
    }

    private MailSendLogEntity createLog(MailboxEntity mailbox, OutboundMailRequest snapshot) {
        MailSendLogEntity log = new MailSendLogEntity();
        log.setTicketId(snapshot.ticketId());
        log.setTicketMessageId(snapshot.ticketMessageId());
        log.setMailboxId(mailbox.getId());
        log.setEnterpriseId(mailbox.getEnterpriseId());
        log.setSendType(snapshot.sendType());
        log.setTemplateId(snapshot.templateId());
        log.setTemplateType(blankToDefault(snapshot.templateType(), snapshot.sendType()));
        log.setToAddress(snapshot.toAddress());
        log.setSubject(snapshot.subject());
        log.setContentBody(snapshot.content());
        log.setMessageId(snapshot.messageId());
        log.setInReplyTo(snapshot.inReplyTo());
        log.setMailReferences(snapshot.references());
        log.setReplyToAddress(snapshot.replyToAddress());
        log.setContentType(snapshot.contentType());
        log.setSendStatus("PENDING");
        log.setRetryCount(0);
        log.setMaxRetry(5);
        log.setCreatedBy(OPERATOR_SYSTEM);
        log.setUpdatedBy(OPERATOR_SYSTEM);
        mailSendLogMapper.insert(log);
        return log;
    }

    private List<MailAttachmentContent> loadAttachments(Long ticketMessageId) throws Exception {
        if (ticketMessageId == null) {
            return List.of();
        }
        List<TicketAttachmentEntity> entities = ticketAttachmentMapper.selectList(
                new LambdaQueryWrapper<TicketAttachmentEntity>()
                        .eq(TicketAttachmentEntity::getMessageId, ticketMessageId)
                        .and(wrapper -> wrapper.isNull(TicketAttachmentEntity::getIsInline)
                                .or().eq(TicketAttachmentEntity::getIsInline, false))
                        .orderByAsc(TicketAttachmentEntity::getId));
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        long totalBytes = 0;
        List<MailAttachmentContent> attachments = new ArrayList<>(entities.size());
        for (TicketAttachmentEntity entity : entities) {
            if (entity.getObjectKey() == null || entity.getObjectKey().isBlank()) {
                throw new IllegalArgumentException("附件存储键为空：" + entity.getId());
            }
            byte[] content;
            try (InputStream inputStream = fileStorageService.download(entity.getObjectKey())) {
                content = inputStream.readAllBytes();
            }
            totalBytes += content.length;
            if (totalBytes > MAX_ATTACHMENT_BYTES) {
                throw new IllegalArgumentException("附件总大小超过 25MB");
            }
            String fileName = sanitizeFileName(entity.getFileName(), entity.getId());
            String contentType = entity.getContentType() == null || entity.getContentType().isBlank()
                    ? "application/octet-stream" : entity.getContentType();
            attachments.add(new MailAttachmentContent(fileName, contentType, content));
        }
        return attachments;
    }

    private String sanitizeFileName(String fileName, Long attachmentId) {
        String normalized = fileName == null ? "attachment-" + attachmentId : fileName;
        normalized = normalized.replace('\r', '_').replace('\n', '_').trim();
        return normalized.isEmpty() ? "attachment-" + attachmentId : normalized;
    }

    private void closeTransport(Transport transport) {
        if (transport != null && transport.isConnected()) {
            try {
                transport.close();
            } catch (MessagingException ignored) {
                // 关闭连接失败不覆盖本次发送结果。
            }
        }
    }

    private String truncateError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    static String detectContentType(String content) {
        return content != null && (content.stripLeading().startsWith("<")
                || content.contains("</") || content.contains("<br"))
                ? CONTENT_TYPE_HTML : CONTENT_TYPE_TEXT;
    }

    private String normalizeContentType(String contentType, String content) {
        return contentType != null && contentType.toLowerCase().startsWith("text/html")
                ? CONTENT_TYPE_HTML
                : contentType != null && contentType.toLowerCase().startsWith("text/plain")
                ? CONTENT_TYPE_TEXT
                : detectContentType(content);
    }

    private String normalizeAddress(String value) {
        if (value == null || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static final class FixedMessageIdMimeMessage extends MimeMessage {
        private final String fixedMessageId;

        private FixedMessageIdMimeMessage(Session session, String fixedMessageId) {
            super(session);
            this.fixedMessageId = fixedMessageId;
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", MailThreadHeaders.toHeaderValue(fixedMessageId));
        }
    }

    record MailAttachmentContent(String fileName, String contentType, byte[] content) {
    }

    public enum DeliveryStatus {
        QUEUED, SUCCESS, FAILED, UNKNOWN
    }

    public record SendResult(boolean success, String message, String messageId, DeliveryStatus deliveryStatus) {
        public static SendResult ok(String message) {
            return new SendResult(true, message, null, DeliveryStatus.SUCCESS);
        }

        public static SendResult ok(String message, String messageId) {
            return new SendResult(true, message, messageId, DeliveryStatus.SUCCESS);
        }

        public static SendResult queued(String message, String messageId) {
            return new SendResult(true, message, messageId, DeliveryStatus.QUEUED);
        }

        public static SendResult fail(String message) {
            return new SendResult(false, message, null, DeliveryStatus.FAILED);
        }

        public static SendResult fail(String message, String messageId) {
            return new SendResult(false, message, messageId, DeliveryStatus.FAILED);
        }

        public static SendResult unknown(String message, String messageId) {
            return new SendResult(false, message, messageId, DeliveryStatus.UNKNOWN);
        }

    }
}
