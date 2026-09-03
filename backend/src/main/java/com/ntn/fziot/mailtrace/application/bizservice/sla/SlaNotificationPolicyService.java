package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * SLA 通知节点开关的唯一判定入口。缺少策略或历史字段为空时使用产品默认值：
 * 响应/解决的预警和超时开启，两个升级节点关闭。
 */
@Service
@RequiredArgsConstructor
public class SlaNotificationPolicyService {

    public static final String RESPONSE_WARNING = "SLA_RESPONSE_WARNING";
    public static final String RESPONSE_BREACH = "SLA_RESPONSE_BREACH";
    public static final String RESPONSE_ESCALATION = "SLA_RESPONSE_ESCALATION";
    public static final String RESOLVE_WARNING = "SLA_RESOLVE_WARNING";
    public static final String RESOLVE_BREACH = "SLA_RESOLVE_BREACH";
    public static final String RESOLVE_ESCALATION = "SLA_RESOLVE_ESCALATION";

    private static final Set<String> SLA_SEND_TYPES = Set.of(
            RESPONSE_WARNING, RESPONSE_BREACH, RESPONSE_ESCALATION,
            RESOLVE_WARNING, RESOLVE_BREACH, RESOLVE_ESCALATION);

    private final TicketMapper ticketMapper;
    private final SlaPolicyMapper slaPolicyMapper;

    public NotificationSettings resolve(Long policyId) {
        return NotificationSettings.from(policyId == null ? null : slaPolicyMapper.selectById(policyId));
    }

    public boolean isEnabled(NotificationSettings settings, String sendType) {
        NotificationSettings effective = settings == null ? NotificationSettings.defaults() : settings;
        return switch (sendType) {
            case RESPONSE_WARNING -> effective.responseWarning();
            case RESPONSE_BREACH -> effective.responseBreach();
            case RESPONSE_ESCALATION -> effective.responseEscalation();
            case RESOLVE_WARNING -> effective.resolveWarning();
            case RESOLVE_BREACH -> effective.resolveBreach();
            case RESOLVE_ESCALATION -> effective.resolveEscalation();
            default -> true;
        };
    }

    /** 实际投递前再次读取策略，确保关闭节点后待发送及失败重试任务不会继续外发。 */
    public boolean isDeliveryEnabled(Long ticketId, String sendType) {
        if (!isSlaType(sendType)) {
            return true;
        }
        TicketEntity ticket = ticketId == null ? null : ticketMapper.selectById(ticketId);
        if (ticket == null || Boolean.TRUE.equals(ticket.getSlaNotificationSuppressed())) {
            return false;
        }
        return isEnabled(resolve(ticket.getSlaPolicyId()), sendType);
    }

    public boolean isSlaType(String sendType) {
        return sendType != null && SLA_SEND_TYPES.contains(sendType);
    }

    public record NotificationSettings(
            boolean responseWarning,
            boolean responseBreach,
            boolean responseEscalation,
            boolean resolveWarning,
            boolean resolveBreach,
            boolean resolveEscalation) {

        static NotificationSettings from(SlaPolicyEntity policy) {
            if (policy == null) {
                return defaults();
            }
            return new NotificationSettings(
                    defaultOn(policy.getResponseWarningNotifyEnabled()),
                    defaultOn(policy.getResponseBreachNotifyEnabled()),
                    Boolean.TRUE.equals(policy.getResponseEscalationNotifyEnabled()),
                    defaultOn(policy.getResolveWarningNotifyEnabled()),
                    defaultOn(policy.getResolveBreachNotifyEnabled()),
                    Boolean.TRUE.equals(policy.getResolveEscalationNotifyEnabled()));
        }

        static NotificationSettings defaults() {
            return new NotificationSettings(true, true, false, true, true, false);
        }

        private static boolean defaultOn(Boolean value) {
            return !Boolean.FALSE.equals(value);
        }
    }
}
