package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Properties;

/**
 * 邮件发送服务。负责 SMTP 发信、发送日志记录、失败异常捕获。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendService {

    private static final String OPERATOR_SYSTEM = "SYSTEM";

    private final MailboxMapper mailboxMapper;
    private final MailSendLogMapper mailSendLogMapper;
    private final MailPasswordCipher mailPasswordCipher;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    /**
     * 发送一封指定内容的邮件（供自动回执、分配通知等内部调用）。
     *
     * @param mailboxId 发件邮箱 ID
     * @param toAddress 收件人
     * @param subject   主题
     * @param content   正文
     * @param sendType  发送类型（AUTO_REPLY / ASSIGN_NOTIFY 等）
     * @return 发送结果
     */
    public SendResult sendRawMail(Long mailboxId, String toAddress, String subject, String content, String sendType) {
        return sendRawMail(mailboxId, toAddress, subject, content, sendType, null, null, sendType);
    }

    public SendResult sendRawMail(Long mailboxId, String toAddress, String subject, String content, String sendType,
                                  Long ticketId, Long templateId, String templateType) {
        enterpriseMailboxAccessService.assertSystemMailboxOperational(mailboxId);
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        if (mailbox == null) {
            return SendResult.fail("邮箱不存在：" + mailboxId);
        }
        String smtpPassword;
        try {
            smtpPassword = mailPasswordCipher.decrypt(mailbox.getSmtpPasswordEnc());
        } catch (Exception e) {
            return SendResult.fail("SMTP 密码解密失败");
        }
        return doSend(mailbox, smtpPassword, toAddress, getFromAddress(mailbox), subject, content, sendType,
                ticketId, templateId, templateType);
    }

    public SendResult sendRawMail(Long mailboxId, String toAddress, String subject, String content) {
        return sendRawMail(mailboxId, toAddress, subject, content, "AUTO_REPLY");
    }

    /**
     * 发送一封测试邮件。
     *
     * @param mailboxId 发件邮箱 ID
     * @param toAddress 收件人邮箱
     * @return 发送结果描述
     */
    public SendResult sendTestMail(Long mailboxId, String toAddress) {
        // 1、查邮箱配置
        enterpriseMailboxAccessService.assertSystemMailboxOperational(mailboxId);
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        if (mailbox == null) {
            return SendResult.fail("邮箱不存在：" + mailboxId);
        }
        if (mailbox.getSmtpHost() == null || mailbox.getSmtpHost().isBlank()) {
            return SendResult.fail("邮箱未配置 SMTP：请先填写 SMTP 服务器地址");
        }

        // 2、解密 SMTP 密码
        String smtpPassword;
        try {
            smtpPassword = mailPasswordCipher.decrypt(mailbox.getSmtpPasswordEnc());
        } catch (Exception e) {
            log.error("SMTP 密码解密失败 mailboxId={}", mailboxId, e);
            return SendResult.fail("SMTP 密码解密失败");
        }

        // 3、构建并发送邮件
        return doSend(mailbox, smtpPassword, toAddress,
                getFromAddress(mailbox), "MailTrace 测试邮件",
                "这是一封来自 MailTrace 邮件工单系统的测试邮件。\n\n如果您收到此邮件，说明 SMTP 配置正确，发信服务正常工作。",
                "TEST", null, null, "TEST");
    }

    public SendResult sendTestMail(CurrentUserPrincipal principal, Long mailboxId, String toAddress) {
        permissionService.assertPermission(principal, "mail_send:test", "无权发送测试邮件");
        enterpriseMailboxAccessService.assertMailboxOperational(principal, mailboxId);
        return sendTestMail(mailboxId, toAddress);
    }

    /**
     * 执行 SMTP 发送并记录发送日志。
     */
    private SendResult doSend(MailboxEntity mailbox, String smtpPassword,
                              String toAddress, String fromAddress,
                              String subject, String content, String sendType,
                              Long ticketId, Long templateId, String templateType) {
        long start = System.currentTimeMillis();
        Transport transport = null;
        MailSendLogEntity logEntity = createLog(
                mailbox, ticketId, templateId, templateType, toAddress, subject, content, sendType);

        try {
            // 建立 SMTP 连接
            transport = openTransport(mailbox, smtpPassword);

            // 构建 MimeMessage（使用标准 Session 确保 UTF-8 编码）
            Properties mailProps = new Properties();
            mailProps.put("mail.mime.charset", "UTF-8");
            MimeMessage msg = new MimeMessage(Session.getInstance(mailProps));
            msg.setFrom(new InternetAddress(fromAddress, mailbox.getSmtpFromName(), "UTF-8"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
            msg.setSubject(subject, "UTF-8");
            // 设置正文 — 优先 HTML，降级纯文本
            boolean isHtml = content != null && (content.startsWith("<") || content.contains("</") || content.contains("<br"));
            if (isHtml) {
                msg.setContent(content, "text/html; charset=UTF-8");
            } else {
                msg.setText(content, "UTF-8");
            }
            msg.saveChanges();
            String messageId = msg.getMessageID();

            // 发送
            transport.sendMessage(msg, msg.getAllRecipients());

            // 成功 → 更新日志
            long elapsed = System.currentTimeMillis() - start;
            log.info("邮件发送成功 mailboxId={} to={} subject={} 耗时={}ms",
                    mailbox.getId(), toAddress, subject, elapsed);
            logEntity.setSendStatus("SUCCESS");
            logEntity.setSentAt(LocalDateTime.now());
            logEntity.setErrorMessage(null);
            mailSendLogMapper.updateById(logEntity);

            return SendResult.ok("发送成功，耗时 " + elapsed + "ms", messageId);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String errorMsg = truncateError(e);
            log.warn("邮件发送失败 mailboxId={} to={} subject={} 耗时={}ms reason={}",
                    mailbox.getId(), toAddress, subject, elapsed, errorMsg);

            // 失败 → 更新日志
            logEntity.setSendStatus("FAILED");
            logEntity.setErrorMessage(errorMsg);
            mailSendLogMapper.updateById(logEntity);

            return SendResult.fail("发送失败：" + errorMsg);

        } finally {
            closeTransport(transport);
        }
    }

    /**
     * 重试发送一封失败邮件。直接更新已有日志，不新建。
     */
    public SendResult retrySend(Long sendLogId) {
        // 1、查发送日志
        MailSendLogEntity logEntity = mailSendLogMapper.selectById(sendLogId);
        if (logEntity == null) {
            return SendResult.fail("发送日志不存在：" + sendLogId);
        }
        if (!"FAILED".equals(logEntity.getSendStatus())) {
            return SendResult.fail("只有失败状态的记录可以重试，当前状态：" + logEntity.getSendStatus());
        }
        enterpriseMailboxAccessService.assertSystemMailboxOperational(logEntity.getMailboxId());
        // 2、查邮箱配置
        MailboxEntity mailbox = mailboxMapper.selectById(logEntity.getMailboxId());
        if (mailbox == null) {
            return SendResult.fail("发件邮箱不存在：" + logEntity.getMailboxId());
        }
        // 3、解密密码
        String smtpPassword;
        try {
            smtpPassword = mailPasswordCipher.decrypt(mailbox.getSmtpPasswordEnc());
        } catch (Exception e) {
            return SendResult.fail("SMTP 密码解密失败");
        }
        // 4、执行发送
        return doRetrySend(mailbox, smtpPassword, logEntity);
    }

    public SendResult retrySend(CurrentUserPrincipal principal, Long sendLogId) {
        permissionService.assertPermission(principal, "mail_send:retry", "无权重试发送邮件");
        MailSendLogEntity logEntity = mailSendLogMapper.selectById(sendLogId);
        if (logEntity != null) {
            enterpriseMailboxAccessService.assertMailboxOperational(principal, logEntity.getMailboxId());
        }
        return retrySend(sendLogId);
    }

    private SendResult doRetrySend(MailboxEntity mailbox, String smtpPassword, MailSendLogEntity logEntity) {
        long start = System.currentTimeMillis();
        Transport transport = null;
        try {
            transport = openTransport(mailbox, smtpPassword);
            Properties mailProps = new Properties();
            mailProps.put("mail.mime.charset", "UTF-8");
            MimeMessage msg = new MimeMessage(Session.getInstance(mailProps));
            msg.setFrom(new InternetAddress(getFromAddress(mailbox), mailbox.getSmtpFromName(), "UTF-8"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(logEntity.getToAddress()));
            msg.setSubject(logEntity.getSubject(), "UTF-8");
            msg.setText(logEntity.getContentBody() != null ? logEntity.getContentBody() : "", "UTF-8");
            msg.saveChanges();
            String messageId = msg.getMessageID();
            transport.sendMessage(msg, msg.getAllRecipients());
            long elapsed = System.currentTimeMillis() - start;
            log.info("邮件重试成功 sendLogId={} to={} subject={} 耗时={}ms",
                    logEntity.getId(), logEntity.getToAddress(), logEntity.getSubject(), elapsed);
            logEntity.setSendStatus("SUCCESS");
            logEntity.setSentAt(LocalDateTime.now());
            logEntity.setErrorMessage(null);
            mailSendLogMapper.updateById(logEntity);
            return SendResult.ok("重试发送成功，耗时 " + elapsed + "ms", messageId);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String errorMsg = truncateError(e);
            log.warn("邮件重试失败 sendLogId={} to={} 耗时={}ms reason={}",
                    logEntity.getId(), logEntity.getToAddress(), elapsed, errorMsg);
            logEntity.setSendStatus("FAILED");
            logEntity.setRetryCount(logEntity.getRetryCount() == null ? 0 : logEntity.getRetryCount() + 1);
            logEntity.setErrorMessage(errorMsg);
            mailSendLogMapper.updateById(logEntity);
            return SendResult.fail("重试失败：" + errorMsg);
        } finally {
            closeTransport(transport);
        }
    }

    /**
     * 建立 SMTP Transport 连接。
     */
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
        return "noreply@ntn.fziot";
    }

    private MailSendLogEntity createLog(MailboxEntity mailbox, Long ticketId, Long templateId, String templateType,
                                        String toAddress, String subject, String contentBody, String sendType) {
        MailSendLogEntity log = new MailSendLogEntity();
        log.setTicketId(ticketId);
        log.setMailboxId(mailbox.getId());
        log.setEnterpriseId(mailbox.getEnterpriseId());
        log.setSendType(sendType);
        log.setTemplateId(templateId);
        log.setTemplateType(templateType == null || templateType.isBlank() ? sendType : templateType);
        log.setToAddress(toAddress);
        log.setSubject(subject);
        log.setContentBody(contentBody);
        log.setSendStatus("PENDING");
        log.setRetryCount(0);
        log.setMaxRetry(5);
        log.setCreatedBy(OPERATOR_SYSTEM);
        log.setUpdatedBy(OPERATOR_SYSTEM);
        mailSendLogMapper.insert(log);
        return log;
    }

    private void closeTransport(Transport transport) {
        if (transport != null && transport.isConnected()) {
            try {
                transport.close();
            } catch (MessagingException ignored) {
                // ignore close errors
            }
        }
    }

    private String truncateError(Exception e) {
        String msg = e.getMessage();
        return msg != null && msg.length() > 500 ? msg.substring(0, 500) : (msg != null ? msg : e.getClass().getSimpleName());
    }

    // ========== 内部结果类 ==========

    public record SendResult(boolean success, String message, String messageId) {
        public static SendResult ok(String message) {
            return new SendResult(true, message, null);
        }

        public static SendResult ok(String message, String messageId) {
            return new SendResult(true, message, messageId);
        }
        public static SendResult fail(String message) {
            return new SendResult(false, message, null);
        }
    }
}
