package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailThreadHeaders;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.OutboundMailRequest;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 自动回执服务。建单后向客户发送工单编号回执邮件。
 * 发送失败不回滚工单，仅记录 send_log + 打印日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReplyService {

    private static final String AUTO_REPLY_TYPE = "AUTO_REPLY";

    private final NotificationTemplateMapper templateMapper;
    private final MailboxMapper mailboxMapper;
    private final TicketMapper ticketMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final UserMapper userMapper;
    private final MailSendService mailSendService;
    private final CustomerTicketAccessService customerTicketAccessService;

    /**
     * 发送自动回执。
     *
     * @param ticketId 工单 ID
     * @param mailboxId 发件邮箱 ID
     */
    public AutoReplyResult sendAutoReply(Long ticketId, Long mailboxId, String customerAccessCode) {
        return sendAutoReply(ticketId, mailboxId, customerAccessCode, null);
    }

    /**
     * 发送自动回执，并显式引用本次触发建单的客户来信。
     */
    public AutoReplyResult sendAutoReply(Long ticketId, Long mailboxId, String customerAccessCode,
                                         Long parentMessageRowId) {
        try {
            // 1、查工单
            TicketEntity ticket = ticketMapper.selectById(ticketId);
            if (ticket == null) {
                log.warn("自动回执跳过：工单不存在 ticketId={}", ticketId);
                return AutoReplyResult.fail("工单不存在：" + ticketId);
            }

            // 2、查邮箱配置是否启用了自动回执
            MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
            if (mailbox == null || !Boolean.TRUE.equals(mailbox.getAutoReplyEnabled())) {
                log.info("自动回执跳过：邮箱未启用自动回执 ticketId={} mailboxId={}", ticketId, mailboxId);
                return AutoReplyResult.fail("邮箱未启用自动回执：" + mailboxId);
            }

            // 3、只读取邮箱显式绑定且启用的全局自动回复模板。
            if (mailbox.getAutoReplyTemplateId() == null) {
                log.info("自动回执跳过：邮箱未绑定自动回复模板 ticketId={} mailboxId={}", ticketId, mailboxId);
                return AutoReplyResult.fail("邮箱未绑定自动回复模板");
            }
            NotificationTemplateEntity template = templateMapper.selectById(mailbox.getAutoReplyTemplateId());
            if (template == null || !Boolean.TRUE.equals(template.getEnabled())
                    || !AUTO_REPLY_TYPE.equals(template.getTemplateType())) {
                log.warn("自动回执跳过：邮箱绑定模板不可用 ticketId={} mailboxId={} templateId={}",
                        ticketId, mailboxId, mailbox.getAutoReplyTemplateId());
                return AutoReplyResult.fail("邮箱绑定的自动回复模板不存在、已停用或类型不匹配");
            }

            // 4、在发送前固化实际使用的模板快照；SMTP 失败也保留本次策略选择证据。
            ticket.setAutoReplyTemplateId(template.getId());
            ticketMapper.update(null, new LambdaUpdateWrapper<TicketEntity>()
                    .eq(TicketEntity::getId, ticketId)
                    .set(TicketEntity::getAutoReplyTemplateId, template.getId())
                    .set(TicketEntity::getUpdatedBy, "system"));

            // 5、渲染模板
            String customerName = ticket.getCustomerEmail() != null ? ticket.getCustomerEmail() : "客户";
            String mailboxEmail = mailbox.getSmtpUsername() != null ? mailbox.getSmtpUsername() : "noreply@ntn.fziot";
            String assigneeName = resolveAssigneeName(ticket);

            Map<String, String> variables = new HashMap<>();
            variables.put("ticket_no", ticket.getTicketNo() != null ? ticket.getTicketNo() : "");
            variables.put("customer_email", ticket.getCustomerEmail() != null ? ticket.getCustomerEmail() : "");
            variables.put("customer_name", customerName);
            variables.put("mailbox_email", mailboxEmail);
            variables.put("assignee_name", assigneeName);
            variables.put("subject", ticket.getSubject() != null ? ticket.getSubject() : "");
            variables.put("customer_ticket_url", customerTicketAccessService.buildTicketUrl(ticket));
            variables.put("customer_ticket_code", customerAccessCode != null ? customerAccessCode : "");
            variables.put("customer_ticket_expires_at",
                    customerTicketAccessService.formatExpiresAt(ticket.getCustomerAccessExpiresAt()));

            TicketMessageEntity parentMessage = resolveParentMessage(ticketId, parentMessageRowId);
            String originalSubject = parentMessage != null && parentMessage.getSubject() != null
                    ? parentMessage.getSubject() : ticket.getSubject();
            String subject = MailThreadHeaders.buildReplySubject(originalSubject);
            String content = render(template.getContentTpl(), variables);

            MailThreadHeaders threadHeaders = MailThreadHeaders.forReply(
                    parentMessage == null ? null : parentMessage.getMessageId(),
                    parentMessage == null ? null : parentMessage.getMailReferences(),
                    resolveReplyToAddress(mailbox));

            TicketMessageEntity outboundMessage = new TicketMessageEntity();
            outboundMessage.setTicketId(ticket.getId());
            outboundMessage.setDirection(TicketBizService.DIRECTION_OUTBOUND);
            outboundMessage.setSendStatus("PENDING");
            outboundMessage.setInReplyTo(threadHeaders.inReplyTo());
            outboundMessage.setMailReferences(threadHeaders.references());
            outboundMessage.setFromAddress(resolveReplyToAddress(mailbox));
            outboundMessage.setToAddress(ticket.getCustomerEmail());
            outboundMessage.setSubject(subject);
            if (isHtml(content)) {
                outboundMessage.setContentHtml(content);
            } else {
                outboundMessage.setContentText(content);
            }
            outboundMessage.setCreatedBy("system");
            outboundMessage.setUpdatedBy("system");
            ticketMessageMapper.insert(outboundMessage);

            // 6、发送并把企业、工单和模板元数据写入发件日志。
            MailSendService.SendResult result;
            try {
                result = mailSendService.sendThreadedMail(new OutboundMailRequest(
                        mailboxId, ticketId, outboundMessage.getId(), template.getId(), template.getTemplateType(),
                        AUTO_REPLY_TYPE, ticket.getCustomerEmail(), subject, content,
                        isHtml(content) ? MailSendService.CONTENT_TYPE_HTML : MailSendService.CONTENT_TYPE_TEXT,
                        null, threadHeaders.inReplyTo(), threadHeaders.references(), threadHeaders.replyToAddress()));
            } catch (Exception exception) {
                log.warn("自动回执发送异常 ticketId={} mailboxId={}", ticketId, mailboxId, exception);
                result = MailSendService.SendResult.fail("发送异常：" + exception.getClass().getSimpleName());
            }

            outboundMessage.setMessageId(MailThreadHeaders.normalizeMessageId(result.messageId()));
            outboundMessage.setSendStatus(switch (result.deliveryStatus()) {
                case SUCCESS -> "SUCCESS";
                case FAILED -> "FAILED";
                case QUEUED, UNKNOWN -> "PENDING";
            });
            if (result.deliveryStatus() == MailSendService.DeliveryStatus.SUCCESS) {
                outboundMessage.setSentAt(LocalDateTime.now());
            }
            ticketMessageMapper.updateById(outboundMessage);

            if (result.deliveryStatus() == MailSendService.DeliveryStatus.SUCCESS) {
                log.info("自动回执发送成功 ticketId={} ticketNo={} customer={}", ticketId,
                        ticket.getTicketNo(), ticket.getCustomerEmail());
            } else if (result.deliveryStatus() == MailSendService.DeliveryStatus.QUEUED) {
                log.info("自动回执已进入事务提交后发送队列 ticketId={} ticketNo={}",
                        ticketId, ticket.getTicketNo());
            } else {
                log.warn("自动回执发送失败 ticketId={} reason={}", ticketId, result.message());
                // 失败不回滚工单，仅记录日志
            }
            return AutoReplyResult.fromSendResult(result, subject, content);

        } catch (Exception e) {
            log.error("自动回执异常 ticketId={} mailboxId={}", ticketId, mailboxId, e);
            // 任何异常都不影响工单
            return AutoReplyResult.fail("自动回执异常：" + e.getClass().getSimpleName());
        }
    }

    private TicketMessageEntity resolveParentMessage(Long ticketId, Long parentMessageRowId) {
        if (parentMessageRowId == null) {
            log.warn("自动回执缺少显式父消息，将降级为独立邮件 ticketId={}", ticketId);
            return null;
        }
        TicketMessageEntity parent = ticketMessageMapper.selectById(parentMessageRowId);
        if (parent == null || !ticketId.equals(parent.getTicketId())
                || !TicketBizService.DIRECTION_INBOUND.equals(parent.getDirection())) {
            log.warn("自动回执父消息无效，将降级为独立邮件 ticketId={} parentMessageRowId={}",
                    ticketId, parentMessageRowId);
            return null;
        }
        if (MailThreadHeaders.normalizeMessageId(parent.getMessageId()) == null) {
            log.warn("自动回执父消息缺少有效 Message-ID，将降级为独立邮件 ticketId={} parentMessageRowId={}",
                    ticketId, parentMessageRowId);
            return null;
        }
        return parent;
    }

    private String resolveReplyToAddress(MailboxEntity mailbox) {
        if (mailbox.getEmailAddress() != null && !mailbox.getEmailAddress().isBlank()) {
            return mailbox.getEmailAddress();
        }
        return mailbox.getSmtpUsername();
    }

    private String resolveAssigneeName(TicketEntity ticket) {
        if (ticket.getAssigneeId() == null) {
            return "未分配";
        }
        UserEntity user = userMapper.selectById(ticket.getAssigneeId());
        if (user == null) {
            return "未分配";
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        if (user.getAccount() != null && !user.getAccount().isBlank()) {
            return user.getAccount();
        }
        return "未分配";
    }

    private String render(String template, Map<String, String> variables) {
        if (template == null) return "";
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (entry.getValue() != null) {
                rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return rendered;
    }

    public record AutoReplyResult(
            boolean success,
            String message,
            String messageId,
            String subject,
            String contentText,
            String contentHtml
    ) {
        public static AutoReplyResult fromSendResult(MailSendService.SendResult result, String subject, String content) {
            boolean html = isHtml(content);
            return new AutoReplyResult(
                    result.success(),
                    result.message(),
                    result.messageId(),
                    subject,
                    html ? null : content,
                    html ? content : null
            );
        }

        public static AutoReplyResult fail(String message) {
            return new AutoReplyResult(false, message, null, null, null, null);
        }

    }

    private static boolean isHtml(String content) {
        return content != null && (content.stripLeading().startsWith("<")
                || content.contains("</") || content.contains("<br"));
    }
}
