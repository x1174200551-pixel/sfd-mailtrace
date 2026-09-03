package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.notification.FeishuNotificationService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaCheckService {

    public static final String EVENT_SLA_WARNING = "SLA_WARNING";
    public static final String EVENT_SLA_BREACH = "SLA_BREACH";
    public static final String EVENT_SLA_ESCALATION = "SLA_ESCALATION";

    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String OPERATOR_SYSTEM = "system";
    private static final DateTimeFormatter SLA_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TicketMapper ticketMapper;
    private final TicketEventMapper ticketEventMapper;
    private final NotificationTemplateMapper notificationTemplateMapper;
    private final MailboxMapper mailboxMapper;
    private final UserMapper userMapper;
    private final MailSendService mailSendService;
    private final FeishuNotificationService feishuNotificationService;
    private final SlaDeadlineService slaDeadlineService;
    private final SlaNotificationPolicyService slaNotificationPolicyService;

    /** 扫描全部有 SLA 快照的非取消工单。 */
    @Transactional
    public SlaCheckResult checkDueTickets(LocalDateTime now) {
        LocalDateTime checkAt = now != null ? now : LocalDateTime.now();
        List<TicketEntity> candidates = ticketMapper.selectSlaCandidates(checkAt);
        return evaluateTickets(candidates, checkAt);
    }

    /** 首次回复或关闭事务完成后的定点补偿入口。 */
    @Transactional
    public SlaCheckResult checkTicket(Long ticketId, LocalDateTime now) {
        TicketEntity ticket = ticketId == null ? null : ticketMapper.selectById(ticketId);
        if (ticket == null || STATUS_CANCELLED.equals(ticket.getStatus())
                || Boolean.TRUE.equals(ticket.getSlaNotificationSuppressed())) {
            return new SlaCheckResult(0, 0, 0, 0);
        }
        return evaluateTickets(List.of(ticket), now != null ? now : LocalDateTime.now());
    }

    private SlaCheckResult evaluateTickets(List<TicketEntity> tickets, LocalDateTime checkAt) {
        int warningCount = 0;
        int breachCount = 0;
        int escalationCount = 0;
        int scannedCount = 0;
        Map<Long, SlaNotificationPolicyService.NotificationSettings> notificationSettingsCache = new HashMap<>();
        for (TicketEntity ticket : tickets) {
            if (Boolean.TRUE.equals(ticket.getSlaNotificationSuppressed())) {
                continue;
            }
            scannedCount++;
            ensureScheduleSnapshot(ticket);
            SlaNotificationPolicyService.NotificationSettings notificationSettings = ticket.getSlaPolicyId() == null
                    ? slaNotificationPolicyService.resolve(null)
                    : notificationSettingsCache.computeIfAbsent(
                    ticket.getSlaPolicyId(), slaNotificationPolicyService::resolve);
            StageResult response = evaluateStage(ticket, SlaStage.RESPONSE, checkAt, notificationSettings);
            StageResult resolve = evaluateStage(ticket, SlaStage.RESOLVE, checkAt, notificationSettings);
            warningCount += response.warningCount() + resolve.warningCount();
            breachCount += response.breachCount() + resolve.breachCount();
            escalationCount += response.escalationCount() + resolve.escalationCount();
        }
        return new SlaCheckResult(scannedCount, warningCount, breachCount, escalationCount);
    }

    private StageResult evaluateStage(TicketEntity ticket, SlaStage stage, LocalDateTime checkAt,
                                      SlaNotificationPolicyService.NotificationSettings notificationSettings) {
        StageSnapshot snapshot = stage.snapshot(ticket);
        if (snapshot.deadline() == null) {
            return StageResult.none();
        }
        boolean completed = snapshot.completedAt() != null;
        boolean completedLate = completed && snapshot.completedAt().isAfter(snapshot.deadline());
        boolean currentlyBreached = !completed && !checkAt.isBefore(snapshot.deadline());

        // 截止点优先于预警点；错过预警窗口时只发超时，不补发过期预警。
        if ((completedLate || currentlyBreached) && snapshot.breachTriggeredAt() == null
                && trigger(ticket, stage, SlaAction.BREACH, checkAt, snapshot, notificationSettings)) {
            return new StageResult(0, 1, 0);
        }
        if (!completed && checkAt.isBefore(snapshot.deadline())
                && snapshot.warningAt() != null && !checkAt.isBefore(snapshot.warningAt())
                && snapshot.warningTriggeredAt() == null
                && trigger(ticket, stage, SlaAction.WARNING, checkAt, snapshot, notificationSettings)) {
            return new StageResult(1, 0, 0);
        }
        if (!completed && snapshot.breachTriggeredAt() != null
                && snapshot.escalationAt() != null && !checkAt.isBefore(snapshot.escalationAt())
                && snapshot.escalationTriggeredAt() == null
                && trigger(ticket, stage, SlaAction.ESCALATION, checkAt, snapshot, notificationSettings)) {
            return new StageResult(0, 0, 1);
        }
        return StageResult.none();
    }

    /** 老工单只补齐派生的预警/升级时间，绝不修改已落库的两个截止时间。 */
    private void ensureScheduleSnapshot(TicketEntity ticket) {
        boolean responseMissing = ticket.getSlaResponseDeadline() != null
                && ticket.getSlaResponseWarningAt() == null;
        boolean resolveMissing = ticket.getSlaResolveDeadline() != null
                && ticket.getSlaResolveWarningAt() == null;
        boolean configSnapshotMissing = ticket.getSlaWarningRemainHoursSnapshot() == null;
        if (!responseMissing && !resolveMissing && !configSnapshotMissing) {
            return;
        }
        SlaDeadlineResult derived = slaDeadlineService.calculateForStoredDeadlines(
                ticket.getSlaPolicyId(), ticket.getSlaResponseDeadline(), ticket.getSlaResolveDeadline());
        if (derived == null) {
            log.warn("旧工单 SLA 调度快照无法补齐，保留原截止时间继续检查 ticketId={} policyId={}",
                    ticket.getId(), ticket.getSlaPolicyId());
            return;
        }
        if (derived.responseWarningAt() == null && derived.resolveWarningAt() == null) {
            return;
        }
        LambdaUpdateWrapper<TicketEntity> update = new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticket.getId())
                .set(ticket.getSlaWarningRemainHoursSnapshot() == null,
                        TicketEntity::getSlaWarningRemainHoursSnapshot, derived.warningRemainHours())
                .set(ticket.getSlaEscalateAfterBreachHoursSnapshot() == null,
                        TicketEntity::getSlaEscalateAfterBreachHoursSnapshot, derived.escalateAfterBreachHours())
                .set(responseMissing, TicketEntity::getSlaResponseWarningAt, derived.responseWarningAt())
                .set(responseMissing, TicketEntity::getSlaResponseEscalationAt, derived.responseEscalationAt())
                .set(resolveMissing, TicketEntity::getSlaResolveWarningAt, derived.resolveWarningAt())
                .set(resolveMissing, TicketEntity::getSlaResolveEscalationAt, derived.resolveEscalationAt())
                .set(TicketEntity::getUpdatedBy, OPERATOR_SYSTEM);
        ticketMapper.update(null, update);
        if (ticket.getSlaWarningRemainHoursSnapshot() == null) {
            ticket.setSlaWarningRemainHoursSnapshot(derived.warningRemainHours());
        }
        if (ticket.getSlaEscalateAfterBreachHoursSnapshot() == null) {
            ticket.setSlaEscalateAfterBreachHoursSnapshot(derived.escalateAfterBreachHours());
        }
        if (responseMissing) {
            ticket.setSlaResponseWarningAt(derived.responseWarningAt());
            ticket.setSlaResponseEscalationAt(derived.responseEscalationAt());
        }
        if (resolveMissing) {
            ticket.setSlaResolveWarningAt(derived.resolveWarningAt());
            ticket.setSlaResolveEscalationAt(derived.resolveEscalationAt());
        }
    }

    private boolean trigger(TicketEntity ticket, SlaStage stage, SlaAction action,
                            LocalDateTime eventAt, StageSnapshot snapshot,
                            SlaNotificationPolicyService.NotificationSettings notificationSettings) {
        LambdaUpdateWrapper<TicketEntity> update = new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticket.getId());
        stage.applyCompletionGuard(update, snapshot, action);
        stage.applyIdempotencyGuard(update, action);
        stage.markTriggered(update, action, eventAt);
        if (action == SlaAction.WARNING) {
            update.set(TicketEntity::getSlaWarningSent, true);
        } else if (action == SlaAction.BREACH) {
            update.set(TicketEntity::getSlaBreached, true)
                    .set(TicketEntity::getSlaBreachNotified, true)
                    .set(TicketEntity::getSlaWarningSent, true);
        }
        update.set(TicketEntity::getUpdatedBy, OPERATOR_SYSTEM);
        if (ticketMapper.update(null, update) <= 0) {
            return false;
        }

        String eventType = stage.sendType(action);
        boolean notificationEnabled = slaNotificationPolicyService.isEnabled(notificationSettings, eventType);
        Long eventId = recordEvent(ticket.getId(), eventType,
                eventContent(stage, action, snapshot, notificationEnabled), eventAt);
        if (notificationEnabled) {
            sendSlaReminder(ticket, stage, action, snapshot, eventAt, eventId);
        } else {
            log.info("SLA 节点已记录但通知关闭 ticketId={} type={}", ticket.getId(), eventType);
        }
        return true;
    }

    private String eventContent(SlaStage stage, SlaAction action, StageSnapshot snapshot,
                                boolean notificationEnabled) {
        String content = switch (action) {
            case WARNING -> stage.label() + " SLA 即将超时，截止时间：" + snapshot.deadline();
            case BREACH -> stage.label() + " SLA 已超时，截止时间：" + snapshot.deadline();
            case ESCALATION -> stage.label() + " SLA 超时升级提醒，截止时间：" + snapshot.deadline();
        };
        return notificationEnabled ? content : content + "；通知已按 SLA 策略关闭";
    }

    private void sendSlaReminder(TicketEntity ticket, SlaStage stage, SlaAction action,
                                 StageSnapshot snapshot, LocalDateTime eventAt, Long eventId) {
        UserEntity assignee = ticket.getAssigneeId() == null ? null : userMapper.selectById(ticket.getAssigneeId());
        String assigneeName = assignee == null
                ? (ticket.getAssigneeId() == null ? "未分配" : "处理人不存在")
                : safe(assignee.getDisplayName());
        RenderedReminder reminder = renderReminder(ticket, stage, action, snapshot, eventAt, assigneeName);

        if (assignee == null || assignee.getEmail() == null || assignee.getEmail().isBlank()) {
            log.info("SLA 提醒邮件跳过：处理人邮箱为空 ticketId={} assigneeId={} type={}",
                    ticket.getId(), ticket.getAssigneeId(), reminder.sendType());
        } else {
            try {
                MailSendService.SendResult result = mailSendService.sendRawMail(
                        ticket.getMailboxId(), assignee.getEmail(), reminder.subject(), reminder.content(),
                        reminder.sendType(), ticket.getId(), reminder.templateId(), reminder.templateType());
                log.info("SLA 提醒邮件任务创建 ticketId={} assigneeId={} type={} accepted={} deliveryStatus={}",
                        ticket.getId(), ticket.getAssigneeId(), reminder.sendType(), result.success(), result.deliveryStatus());
            } catch (RuntimeException exception) {
                log.error("SLA 提醒邮件任务创建失败 ticketId={} assigneeId={} type={}",
                        ticket.getId(), ticket.getAssigneeId(), reminder.sendType(), exception);
            }
        }

        try {
            Long feishuLogId = feishuNotificationService.enqueue(
                    eventId, ticket, ticket.getAssigneeId(), assigneeName,
                    reminder.sendType(), reminder.templateId(), reminder.subject(), reminder.content());
            if (feishuLogId != null) {
                log.info("SLA 飞书任务已创建 ticketId={} assigneeId={} type={} sendLogId={}",
                        ticket.getId(), ticket.getAssigneeId(), reminder.sendType(), feishuLogId);
            }
        } catch (RuntimeException exception) {
            log.error("SLA 飞书任务创建失败 ticketId={} assigneeId={} type={}",
                    ticket.getId(), ticket.getAssigneeId(), reminder.sendType(), exception);
        }
    }

    private RenderedReminder renderReminder(TicketEntity ticket, SlaStage stage, SlaAction action,
                                            StageSnapshot snapshot, LocalDateTime eventAt, String assigneeName) {
        String sendType = stage.sendType(action);
        String templateType = action == SlaAction.WARNING ? EVENT_SLA_WARNING : EVENT_SLA_BREACH;
        MailboxEntity mailbox = mailboxMapper.selectById(ticket.getMailboxId());
        Long templateId = mailbox == null ? null
                : action == SlaAction.WARNING ? mailbox.getSlaWarningTemplateId() : mailbox.getSlaBreachTemplateId();
        NotificationTemplateEntity candidate = templateId == null
                ? null : notificationTemplateMapper.selectById(templateId);
        NotificationTemplateEntity template = candidate != null
                && Boolean.TRUE.equals(candidate.getEnabled())
                && templateType.equals(candidate.getTemplateType()) ? candidate : null;
        String subject;
        String content;
        if (template != null) {
            subject = renderTemplate(template.getSubjectTpl(), ticket, stage, action, snapshot, eventAt, assigneeName);
            content = renderTemplate(template.getContentTpl(), ticket, stage, action, snapshot, eventAt, assigneeName);
        } else {
            String statusText = switch (action) {
                case WARNING -> "即将超时";
                case BREACH -> "已超时";
                case ESCALATION -> "超时升级提醒";
            };
            subject = stage.label() + " SLA " + statusText + "：" + ticket.getTicketNo();
            content = "工单 " + ticket.getTicketNo() + " 的" + stage.label() + " SLA " + statusText + "。\n"
                    + "处理人：" + assigneeName + "\n"
                    + "主题：" + safe(ticket.getSubject()) + "\n"
                    + "客户：" + safe(ticket.getCustomerEmail()) + "\n"
                    + "截止时间：" + formatTime(snapshot.deadline()) + "\n\n"
                    + "请登录系统及时处理。";
        }
        String titlePrefix = "【" + stage.label() + action.titleLabel() + "】";
        if (!subject.startsWith(titlePrefix)) {
            subject = titlePrefix + subject;
        }
        content = decorateContentWithSlaMetadata(content, ticket, stage, action, eventAt);
        return new RenderedReminder(subject, content, template == null ? null : template.getId(),
                template == null ? templateType : template.getTemplateType(), sendType);
    }

    private String renderTemplate(String template, TicketEntity ticket, SlaStage stage, SlaAction action,
                                  StageSnapshot snapshot, LocalDateTime eventAt, String assigneeName) {
        return safe(template)
                .replace("{ticket_no}", safe(ticket.getTicketNo()))
                .replace("{subject}", safe(ticket.getSubject()))
                .replace("{customer_email}", safe(ticket.getCustomerEmail()))
                .replace("{assignee_name}", safe(assigneeName))
                .replace("{sla_deadline}", formatTime(snapshot.deadline()))
                .replace("{ticket_link}", "请登录系统查看工单详情")
                .replace("{sla_stage}", stage.label())
                .replace("{sla_action}", action.titleLabel())
                .replace("{sla_response_deadline}", formatTime(ticket.getSlaResponseDeadline()))
                .replace("{sla_resolve_deadline}", formatTime(ticket.getSlaResolveDeadline()))
                .replace("{sla_triggered_at}", formatTime(eventAt))
                .replace("{sla_overdue_hours}", action == SlaAction.ESCALATION
                        ? String.valueOf(ticket.getSlaEscalateAfterBreachHoursSnapshot() == null
                        ? 0 : ticket.getSlaEscalateAfterBreachHoursSnapshot()) : "0");
    }

    private String slaMetadata(TicketEntity ticket, SlaStage stage, SlaAction action, LocalDateTime eventAt) {
        return "SLA 节点：" + stage.label() + action.titleLabel() + "\n"
                + "首次响应截止：" + formatTime(ticket.getSlaResponseDeadline()) + "\n"
                + "解决截止：" + formatTime(ticket.getSlaResolveDeadline()) + "\n"
                + "本次触发时间：" + formatTime(eventAt);
    }

    private String decorateContentWithSlaMetadata(String content, TicketEntity ticket, SlaStage stage,
                                                  SlaAction action, LocalDateTime eventAt) {
        String metadata = slaMetadata(ticket, stage, action, eventAt);
        if (content != null && (content.stripLeading().startsWith("<") || content.contains("</"))) {
            return "<div>" + metadata.replace("\n", "<br/>") + "</div><br/>" + content;
        }
        return metadata + "\n\n" + safe(content);
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "未配置" : value.format(SLA_TIME_FORMATTER);
    }

    private Long recordEvent(Long ticketId, String eventType, String content, LocalDateTime eventAt) {
        TicketEventEntity event = new TicketEventEntity();
        event.setTicketId(ticketId);
        event.setEventType(eventType);
        event.setEventContent(content);
        event.setOperator(OPERATOR_SYSTEM);
        event.setEventAt(eventAt);
        event.setCreatedBy(OPERATOR_SYSTEM);
        event.setUpdatedBy(OPERATOR_SYSTEM);
        ticketEventMapper.insert(event);
        return event.getId();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private enum SlaAction {
        WARNING("_WARNING", "预警"),
        BREACH("_BREACH", "超时"),
        ESCALATION("_ESCALATION", "超时升级");

        private final String eventSuffix;
        private final String titleLabel;

        SlaAction(String eventSuffix, String titleLabel) {
            this.eventSuffix = eventSuffix;
            this.titleLabel = titleLabel;
        }

        String eventSuffix() {
            return eventSuffix;
        }

        String titleLabel() {
            return titleLabel;
        }
    }

    private enum SlaStage {
        RESPONSE("首次响应", "SLA_RESPONSE"),
        RESOLVE("解决", "SLA_RESOLVE");

        private final String label;
        private final String eventPrefix;

        SlaStage(String label, String eventPrefix) {
            this.label = label;
            this.eventPrefix = eventPrefix;
        }

        String label() {
            return label;
        }

        String eventPrefix() {
            return eventPrefix;
        }

        String sendType(SlaAction action) {
            return eventPrefix + action.eventSuffix();
        }

        StageSnapshot snapshot(TicketEntity ticket) {
            if (this == RESPONSE) {
                LocalDateTime completedAt = ticket.getFirstReplyAt() != null
                        ? ticket.getFirstReplyAt() : ticket.getClosedAt();
                return new StageSnapshot(ticket.getSlaResponseDeadline(), ticket.getSlaResponseWarningAt(),
                    ticket.getSlaResponseEscalationAt(), completedAt, ticket.getFirstReplyAt() == null && ticket.getClosedAt() != null,
                    ticket.getSlaResponseWarningTriggeredAt(), ticket.getSlaResponseBreachTriggeredAt(),
                    ticket.getSlaResponseEscalationTriggeredAt());
            }
            return new StageSnapshot(ticket.getSlaResolveDeadline(), ticket.getSlaResolveWarningAt(),
                    ticket.getSlaResolveEscalationAt(), ticket.getClosedAt(), false,
                    ticket.getSlaResolveWarningTriggeredAt(), ticket.getSlaResolveBreachTriggeredAt(),
                    ticket.getSlaResolveEscalationTriggeredAt());
        }

        void applyCompletionGuard(LambdaUpdateWrapper<TicketEntity> update, StageSnapshot snapshot,
                                  SlaAction action) {
            update.ne(TicketEntity::getStatus, STATUS_CANCELLED);
            LocalDateTime completedAt = snapshot.completedAt();
            if (action != SlaAction.BREACH || completedAt == null) {
                if (this == RESPONSE) {
                    update.isNull(TicketEntity::getFirstReplyAt)
                            .isNull(TicketEntity::getClosedAt);
                } else {
                    update.isNull(TicketEntity::getClosedAt);
                }
            } else if (this == RESPONSE && snapshot.completedByClose()) {
                update.isNull(TicketEntity::getFirstReplyAt)
                        .eq(TicketEntity::getClosedAt, completedAt);
            } else if (this == RESPONSE) {
                update.eq(TicketEntity::getFirstReplyAt, completedAt);
            } else {
                update.eq(TicketEntity::getClosedAt, completedAt);
            }
        }

        void applyIdempotencyGuard(LambdaUpdateWrapper<TicketEntity> update, SlaAction action) {
            if (this == RESPONSE) {
                switch (action) {
                    case WARNING -> update.isNull(TicketEntity::getSlaResponseWarningTriggeredAt);
                    case BREACH -> update.isNull(TicketEntity::getSlaResponseBreachTriggeredAt);
                    case ESCALATION -> update.isNull(TicketEntity::getSlaResponseEscalationTriggeredAt);
                }
            } else {
                switch (action) {
                    case WARNING -> update.isNull(TicketEntity::getSlaResolveWarningTriggeredAt);
                    case BREACH -> update.isNull(TicketEntity::getSlaResolveBreachTriggeredAt);
                    case ESCALATION -> update.isNull(TicketEntity::getSlaResolveEscalationTriggeredAt);
                }
            }
        }

        void markTriggered(LambdaUpdateWrapper<TicketEntity> update, SlaAction action, LocalDateTime eventAt) {
            if (this == RESPONSE) {
                switch (action) {
                    case WARNING -> update.set(TicketEntity::getSlaResponseWarningTriggeredAt, eventAt);
                    case BREACH -> update.set(TicketEntity::getSlaResponseBreachTriggeredAt, eventAt);
                    case ESCALATION -> update.set(TicketEntity::getSlaResponseEscalationTriggeredAt, eventAt);
                }
            } else {
                switch (action) {
                    case WARNING -> update.set(TicketEntity::getSlaResolveWarningTriggeredAt, eventAt);
                    case BREACH -> update.set(TicketEntity::getSlaResolveBreachTriggeredAt, eventAt);
                    case ESCALATION -> update.set(TicketEntity::getSlaResolveEscalationTriggeredAt, eventAt);
                }
            }
        }
    }

    private record StageSnapshot(LocalDateTime deadline, LocalDateTime warningAt, LocalDateTime escalationAt,
                                 LocalDateTime completedAt, boolean completedByClose,
                                 LocalDateTime warningTriggeredAt,
                                 LocalDateTime breachTriggeredAt, LocalDateTime escalationTriggeredAt) {
    }

    private record StageResult(int warningCount, int breachCount, int escalationCount) {
        static StageResult none() {
            return new StageResult(0, 0, 0);
        }
    }

    private record RenderedReminder(String subject, String content, Long templateId,
                                    String templateType, String sendType) {
    }
}
