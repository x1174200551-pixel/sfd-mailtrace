package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaCheckService {

    public static final String EVENT_SLA_WARNING = "SLA_WARNING";
    public static final String EVENT_SLA_BREACH = "SLA_BREACH";

    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String OPERATOR_SYSTEM = "system";
    private static final int DEFAULT_WARNING_REMAIN_HOURS = 1;

    private final TicketMapper ticketMapper;
    private final TicketEventMapper ticketEventMapper;
    private final SlaPolicyMapper slaPolicyMapper;
    private final NotificationTemplateMapper notificationTemplateMapper;
    private final UserMapper userMapper;
    private final MailSendService mailSendService;

    /**
     * 扫描 SLA 即将超时和已超时工单。
     */
    @Transactional
    public SlaCheckResult checkDueTickets(LocalDateTime now) {
        LocalDateTime checkAt = now != null ? now : LocalDateTime.now();

        // 1、读取仍在处理中的 SLA 候选工单，已关闭/已取消不再提醒。
        List<TicketEntity> candidates = ticketMapper.selectList(new LambdaQueryWrapper<TicketEntity>()
                .notIn(TicketEntity::getStatus, STATUS_CLOSED, STATUS_CANCELLED)
                .and(wrapper -> wrapper
                        .isNotNull(TicketEntity::getSlaResponseDeadline)
                        .or()
                        .isNotNull(TicketEntity::getSlaResolveDeadline)));

        // 2、逐单判断当前应检查首次响应还是解决 SLA，并按 breach 优先级处理。
        int warningCount = 0;
        int breachCount = 0;
        for (TicketEntity ticket : candidates) {
            PendingDeadline pending = resolvePendingDeadline(ticket);
            if (pending == null) {
                continue;
            }
            if (isBreached(checkAt, pending.deadline())) {
                if (markBreached(ticket, pending, checkAt)) {
                    breachCount++;
                }
                continue;
            }
            if (isWarningDue(ticket, pending, checkAt) && markWarning(ticket, pending, checkAt)) {
                warningCount++;
            }
        }

        // 3、返回本轮扫描摘要，供定时任务日志和后续运维观察使用。
        return new SlaCheckResult(candidates.size(), warningCount, breachCount);
    }

    private PendingDeadline resolvePendingDeadline(TicketEntity ticket) {
        if (ticket.getFirstReplyAt() == null && ticket.getSlaResponseDeadline() != null) {
            return new PendingDeadline("首次响应", ticket.getSlaResponseDeadline());
        }
        if (ticket.getClosedAt() == null && ticket.getSlaResolveDeadline() != null) {
            return new PendingDeadline("解决", ticket.getSlaResolveDeadline());
        }
        return null;
    }

    private boolean isBreached(LocalDateTime checkAt, LocalDateTime deadline) {
        return checkAt.isAfter(deadline);
    }

    private boolean isWarningDue(TicketEntity ticket, PendingDeadline pending, LocalDateTime checkAt) {
        if (Boolean.TRUE.equals(ticket.getSlaWarningSent())) {
            return false;
        }
        int warningRemainHours = resolveWarningRemainHours(ticket.getSlaPolicyId());
        return !checkAt.isBefore(pending.deadline().minusHours(warningRemainHours));
    }

    private int resolveWarningRemainHours(Long policyId) {
        if (policyId == null) {
            return DEFAULT_WARNING_REMAIN_HOURS;
        }
        SlaPolicyEntity policy = slaPolicyMapper.selectById(policyId);
        if (policy == null || policy.getWarningRemainHours() == null || policy.getWarningRemainHours() <= 0) {
            return DEFAULT_WARNING_REMAIN_HOURS;
        }
        return policy.getWarningRemainHours();
    }

    private boolean markWarning(TicketEntity ticket, PendingDeadline pending, LocalDateTime eventAt) {
        // 1、使用数据库条件保护，避免重复 warning。
        int updated = ticketMapper.update(null, new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticket.getId())
                .eq(TicketEntity::getSlaWarningSent, false)
                .set(TicketEntity::getSlaWarningSent, true)
                .set(TicketEntity::getUpdatedBy, OPERATOR_SYSTEM));
        if (updated <= 0) {
            return false;
        }

        // 2、写入工单事件，记录当前是哪类 SLA 即将超时。
        recordEvent(ticket.getId(), EVENT_SLA_WARNING,
                pending.type() + " SLA 即将超时，截止时间：" + pending.deadline(), eventAt);
        sendSlaReminder(ticket, pending, EVENT_SLA_WARNING);
        return true;
    }

    private boolean markBreached(TicketEntity ticket, PendingDeadline pending, LocalDateTime eventAt) {
        // 1、使用数据库条件保护，避免重复 breach；超时时同时补齐 warning 标识。
        int updated = ticketMapper.update(null, new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticket.getId())
                .eq(TicketEntity::getSlaBreachNotified, false)
                .set(TicketEntity::getSlaBreached, true)
                .set(TicketEntity::getSlaBreachNotified, true)
                .set(TicketEntity::getSlaWarningSent, true)
                .set(TicketEntity::getUpdatedBy, OPERATOR_SYSTEM));
        if (updated <= 0) {
            return false;
        }

        // 2、写入工单事件，记录当前是哪类 SLA 已超时。
        recordEvent(ticket.getId(), EVENT_SLA_BREACH,
                pending.type() + " SLA 已超时，截止时间：" + pending.deadline(), eventAt);
        sendSlaReminder(ticket, pending, EVENT_SLA_BREACH);
        return true;
    }

    private void sendSlaReminder(TicketEntity ticket, PendingDeadline pending, String sendType) {
        // 1、SLA 提醒默认通知当前处理人；无处理人或处理人邮箱缺失时只保留事件。
        if (ticket.getAssigneeId() == null) {
            log.warn("SLA 提醒邮件跳过：工单未分配处理人 ticketId={} type={}", ticket.getId(), sendType);
            return;
        }
        UserEntity assignee = userMapper.selectById(ticket.getAssigneeId());
        if (assignee == null || assignee.getEmail() == null || assignee.getEmail().isBlank()) {
            log.warn("SLA 提醒邮件跳过：处理人不存在或邮箱为空 ticketId={} assigneeId={} type={}",
                    ticket.getId(), ticket.getAssigneeId(), sendType);
            return;
        }

        // 2、优先使用启用模板；模板不存在或停用时使用系统兜底文案，避免提醒链路中断。
        RenderedReminder reminder = renderReminder(ticket, pending, assignee, sendType);
        MailSendService.SendResult result = mailSendService.sendRawMail(
                ticket.getMailboxId(), assignee.getEmail(), reminder.subject(), reminder.content(), sendType);
        if (result.success()) {
            log.info("SLA 提醒邮件已发送 ticketId={} assigneeId={} type={}",
                    ticket.getId(), ticket.getAssigneeId(), sendType);
        } else {
            log.warn("SLA 提醒邮件发送失败 ticketId={} assigneeId={} type={} reason={}",
                    ticket.getId(), ticket.getAssigneeId(), sendType, result.message());
        }
    }

    private RenderedReminder renderReminder(TicketEntity ticket, PendingDeadline pending,
                                            UserEntity assignee, String sendType) {
        NotificationTemplateEntity template = notificationTemplateMapper.selectOne(
                new LambdaQueryWrapper<NotificationTemplateEntity>()
                        .eq(NotificationTemplateEntity::getTemplateCode, sendType)
                        .eq(NotificationTemplateEntity::getEnabled, true)
                        .last("LIMIT 1"));
        String subject;
        String content;
        if (template != null) {
            subject = renderTemplate(template.getSubjectTpl(), ticket, pending, assignee);
            content = renderTemplate(template.getContentTpl(), ticket, pending, assignee);
        } else {
            String statusText = EVENT_SLA_WARNING.equals(sendType) ? "即将超时" : "已超时";
            subject = pending.type() + " SLA " + statusText + "：" + ticket.getTicketNo();
            content = "您好，" + assignee.getDisplayName() + "，\n\n"
                    + "工单 " + ticket.getTicketNo() + " 的" + pending.type() + " SLA " + statusText + "。\n"
                    + "主题：" + safe(ticket.getSubject()) + "\n"
                    + "客户：" + safe(ticket.getCustomerEmail()) + "\n"
                    + "截止时间：" + pending.deadline() + "\n\n"
                    + "请登录系统及时处理。";
        }
        return new RenderedReminder(subject, content);
    }

    private String renderTemplate(String template, TicketEntity ticket, PendingDeadline pending, UserEntity assignee) {
        return safe(template)
                .replace("{ticket_no}", safe(ticket.getTicketNo()))
                .replace("{subject}", safe(ticket.getSubject()))
                .replace("{customer_email}", safe(ticket.getCustomerEmail()))
                .replace("{assignee_name}", safe(assignee.getDisplayName()))
                .replace("{sla_deadline}", String.valueOf(pending.deadline()))
                .replace("{ticket_link}", "请登录系统查看工单详情")
                .replace("{sla_type}", pending.type());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void recordEvent(Long ticketId, String eventType, String content, LocalDateTime eventAt) {
        TicketEventEntity event = new TicketEventEntity();
        event.setTicketId(ticketId);
        event.setEventType(eventType);
        event.setEventContent(content);
        event.setOperator(OPERATOR_SYSTEM);
        event.setEventAt(eventAt);
        event.setCreatedBy(OPERATOR_SYSTEM);
        event.setUpdatedBy(OPERATOR_SYSTEM);
        ticketEventMapper.insert(event);
    }

    private record PendingDeadline(String type, LocalDateTime deadline) {
    }

    private record RenderedReminder(String subject, String content) {
    }
}
