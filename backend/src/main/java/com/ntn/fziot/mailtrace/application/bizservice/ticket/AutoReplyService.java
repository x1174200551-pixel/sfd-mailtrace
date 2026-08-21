package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动回执服务。建单后向客户发送工单编号回执邮件。
 * 发送失败不回滚工单，仅记录 send_log + 打印日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoReplyService {

    private static final String AUTO_REPLY_CODE = "AUTO_REPLY";

    private final NotificationTemplateMapper templateMapper;
    private final MailboxMapper mailboxMapper;
    private final TicketMapper ticketMapper;
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

            // 3、查 AUTO_REPLY 模板
            NotificationTemplateEntity template = templateMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NotificationTemplateEntity>()
                            .eq(NotificationTemplateEntity::getTemplateCode, AUTO_REPLY_CODE)
                            .last("LIMIT 1"));
            if (template == null || !Boolean.TRUE.equals(template.getEnabled())) {
                log.warn("自动回执跳过：AUTO_REPLY 模板未找到或未启用 ticketId={}", ticketId);
                return AutoReplyResult.fail("AUTO_REPLY 模板未找到或未启用");
            }

            // 4、渲染模板
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

            String subject = render(template.getSubjectTpl(), variables);
            String content = render(template.getContentTpl(), variables);

            // 5、发送
            MailSendService.SendResult result = mailSendService.sendRawMail(
                    mailboxId, ticket.getCustomerEmail(), subject, content);

            if (result.success()) {
                log.info("自动回执发送成功 ticketId={} ticketNo={} customer={}",
                        ticketId, ticket.getTicketNo(), ticket.getCustomerEmail());
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

        private static boolean isHtml(String content) {
            return content != null && (content.startsWith("<") || content.contains("</") || content.contains("<br"));
        }
    }
}
