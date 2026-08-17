package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final MailSendService mailSendService;

    /**
     * 发送自动回执。
     *
     * @param ticketId 工单 ID
     * @param mailboxId 发件邮箱 ID
     */
    public MailSendService.SendResult sendAutoReply(Long ticketId, Long mailboxId) {
        try {
            // 1、查工单
            TicketEntity ticket = ticketMapper.selectById(ticketId);
            if (ticket == null) {
                log.warn("自动回执跳过：工单不存在 ticketId={}", ticketId);
                return MailSendService.SendResult.fail("工单不存在：" + ticketId);
            }

            // 2、查邮箱配置是否启用了自动回执
            MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
            if (mailbox == null || !Boolean.TRUE.equals(mailbox.getAutoReplyEnabled())) {
                log.info("自动回执跳过：邮箱未启用自动回执 ticketId={} mailboxId={}", ticketId, mailboxId);
                return MailSendService.SendResult.fail("邮箱未启用自动回执：" + mailboxId);
            }

            // 3、查 AUTO_REPLY 模板
            NotificationTemplateEntity template = templateMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NotificationTemplateEntity>()
                            .eq(NotificationTemplateEntity::getTemplateCode, AUTO_REPLY_CODE)
                            .last("LIMIT 1"));
            if (template == null || !Boolean.TRUE.equals(template.getEnabled())) {
                log.warn("自动回执跳过：AUTO_REPLY 模板未找到或未启用 ticketId={}", ticketId);
                return MailSendService.SendResult.fail("AUTO_REPLY 模板未找到或未启用");
            }

            // 4、渲染模板
            String customerName = ticket.getCustomerEmail() != null ? ticket.getCustomerEmail() : "客户";
            String mailboxEmail = mailbox.getSmtpUsername() != null ? mailbox.getSmtpUsername() : "noreply@ntn.fziot";
            String mailboxName = mailbox.getSmtpFromName() != null ? mailbox.getSmtpFromName() : mailbox.getMailboxName();

            Map<String, String> variables = Map.of(
                    "ticket_no", ticket.getTicketNo() != null ? ticket.getTicketNo() : "",
                    "customer_email", ticket.getCustomerEmail() != null ? ticket.getCustomerEmail() : "",
                    "customer_name", customerName,
                    "mailbox_email", mailboxEmail,
                    "subject", ticket.getSubject() != null ? ticket.getSubject() : ""
            );

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
            return result;

        } catch (Exception e) {
            log.error("自动回执异常 ticketId={} mailboxId={}", ticketId, mailboxId, e);
            // 任何异常都不影响工单
            return MailSendService.SendResult.fail("自动回执异常：" + e.getClass().getSimpleName());
        }
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
}
