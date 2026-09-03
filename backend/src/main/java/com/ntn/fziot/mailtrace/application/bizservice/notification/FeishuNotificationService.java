package com.ntn.fziot.mailtrace.application.bizservice.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaNotificationPolicyService;
import com.ntn.fziot.mailtrace.application.event.FeishuTaskCreatedEvent;
import com.ntn.fziot.mailtrace.infrastructure.feishu.FeishuGroupBotClient;
import com.ntn.fziot.mailtrace.infrastructure.feishu.FeishuGroupBotProperties;
import com.ntn.fziot.mailtrace.infrastructure.feishu.FeishuGroupCardRenderer;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.FeishuGroupTestResponse;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.FeishuSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.FeishuSendLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuNotificationService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;
    private static final String OPERATOR_SYSTEM = "system";
    private static final int BATCH_SIZE = 20;
    private static final int STALE_SENDING_MINUTES = 10;

    private final FeishuGroupBotProperties properties;
    private final FeishuGroupCardRenderer cardRenderer;
    private final FeishuGroupBotClient client;
    private final EnterpriseMapper enterpriseMapper;
    private final FeishuSendLogMapper sendLogMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SlaNotificationPolicyService slaNotificationPolicyService;

    public Long enqueue(Long ticketEventId, TicketEntity ticket, Long assigneeUserId, String assigneeName,
                        String sendType, Long templateId, String title, String content) {
        if (!properties.isEnabled() || ticket == null || ticket.getEnterpriseId() == null) {
            return null;
        }
        EnterpriseEntity enterprise = enterpriseMapper.selectById(ticket.getEnterpriseId());
        if (!canSendBusinessNotification(enterprise)) {
            return null;
        }
        TaskCreation task = createTask(ticketEventId, ticket.getId(), ticket.getMailboxId(), ticket.getTicketNo(),
                ticket.getPriority(), enterprise, assigneeUserId, assigneeName,
                sendType, templateId, title, content);
        if (task.created()) {
            eventPublisher.publishEvent(new FeishuTaskCreatedEvent(task.logId()));
        }
        return task.logId();
    }

    public FeishuGroupTestResponse sendTest(Long enterpriseId) {
        if (!properties.isEnabled()) {
            throw new BusinessException(CODE_BAD_REQUEST, "飞书群通知全局开关未启用");
        }
        EnterpriseEntity enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(CODE_NOT_FOUND, "企业不存在");
        }
        if (!Boolean.TRUE.equals(enterprise.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "企业已停用，不能测试飞书通知群");
        }
        validateEnterpriseConfig(enterprise);
        Long logId = createTask(null, null, null, null, null, enterprise, null, null, "TEST", null,
                "MailTrace 飞书群通知测试",
                "这是一条企业通知群配置测试消息，请在群内确认是否正常显示。").logId();
        if (logId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "测试消息创建失败");
        }
        processLog(logId);
        FeishuSendLogEntity result = sendLogMapper.selectById(logId);
        boolean accepted = result != null && "SUCCESS".equals(result.getSendStatus());
        updateEnterpriseTestStatus(enterpriseId, accepted, accepted ? null : safeMessage(result));
        return new FeishuGroupTestResponse(
                accepted,
                accepted
                        ? "测试消息已发送到该企业通知群，请在群内确认是否正常显示"
                        : "测试消息发送失败：" + safeMessage(result),
                logId);
    }

    public BatchResult processPendingBatch() {
        if (!properties.isEnabled()) {
            return new BatchResult(0, 0, 0);
        }
        LocalDateTime now = LocalDateTime.now();
        sendLogMapper.recoverStaleSending(now);
        List<FeishuSendLogEntity> tasks = sendLogMapper.selectPendingForSend(now, BATCH_SIZE);
        int success = 0;
        int failed = 0;
        for (FeishuSendLogEntity task : tasks) {
            if (processLog(task.getId())) {
                success++;
            } else {
                failed++;
            }
        }
        return new BatchResult(tasks.size(), success, failed);
    }

    /** 事务提交后的即时派发入口；抢占失败代表任务已被其他执行器处理。 */
    public boolean processPending(Long logId) {
        if (!properties.isEnabled() || logId == null) {
            return false;
        }
        return processLog(logId);
    }

    public void validateWebhook(String webhookUrl) {
        client.validateWebhook(webhookUrl);
    }

    private TaskCreation createTask(Long ticketEventId, Long ticketId, Long mailboxId,
                                    String ticketNo, String priority,
                                    EnterpriseEntity enterprise, Long assigneeUserId, String assigneeName,
                                    String sendType, Long templateId, String title, String content) {
        FeishuGroupCardRenderer.RenderedCard card = cardRenderer.render(
                sendType, ticketId, ticketNo, priority, title, content, assigneeName);
        FeishuSendLogEntity task = new FeishuSendLogEntity();
        task.setTicketEventId(ticketEventId);
        task.setTicketId(ticketId);
        task.setMailboxId(mailboxId);
        task.setEnterpriseId(enterprise.getId());
        task.setEnterpriseConfigVersion(defaultVersion(enterprise.getFeishuConfigVersion()));
        task.setAssigneeUserId(assigneeUserId);
        task.setSendType(sendType);
        task.setTemplateId(templateId);
        task.setGroupBotName(enterprise.getFeishuGroupName());
        task.setTitle(card.title());
        task.setContentBody(card.content());
        task.setCardContent(card.cardJson());
        task.setSendStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetry(properties.getMaxRetry());
        task.setNextRetryAt(LocalDateTime.now());
        task.setCreatedBy(OPERATOR_SYSTEM);
        task.setUpdatedBy(OPERATOR_SYSTEM);
        try {
            sendLogMapper.insert(task);
            return new TaskCreation(task.getId(), true);
        } catch (DuplicateKeyException exception) {
            if (ticketEventId == null) {
                throw exception;
            }
            FeishuSendLogEntity existing = sendLogMapper.selectOne(
                    new LambdaQueryWrapper<FeishuSendLogEntity>()
                            .eq(FeishuSendLogEntity::getTicketEventId, ticketEventId)
                            .last("LIMIT 1"));
            return new TaskCreation(existing == null ? null : existing.getId(), false);
        }
    }

    private boolean processLog(Long logId) {
        // SENDING 状态下 next_retry_at 作为任务租约到期时间，进程异常后可由下一轮安全恢复。
        LocalDateTime leaseUntil = LocalDateTime.now().plusMinutes(STALE_SENDING_MINUTES);
        if (sendLogMapper.claimForSend(logId, leaseUntil) != 1) {
            return false;
        }
        FeishuSendLogEntity task = sendLogMapper.selectById(logId);
        EnterpriseEntity enterprise = task == null ? null : enterpriseMapper.selectById(task.getEnterpriseId());
        if (task == null || enterprise == null) {
            markFinalFailed(task, "CONFIG_MISSING", "企业飞书配置不存在");
            return false;
        }
        if (!slaNotificationPolicyService.isDeliveryEnabled(task.getTicketId(), task.getSendType())) {
            markCancelled(task, "SLA_POLICY_DISABLED", "SLA通知节点已关闭或工单禁止通知");
            return false;
        }
        if (!Boolean.TRUE.equals(enterprise.getEnabled())) {
            markFinalFailed(task, "ENTERPRISE_DISABLED", "企业已停用");
            return false;
        }
        if (!"TEST".equals(task.getSendType()) && !Boolean.TRUE.equals(enterprise.getFeishuNotifyEnabled())) {
            markFinalFailed(task, "CHANNEL_DISABLED", "企业飞书通知已关闭");
            return false;
        }
        if (defaultVersion(enterprise.getFeishuConfigVersion()) != defaultVersion(task.getEnterpriseConfigVersion())) {
            markFinalFailed(task, "CONFIG_CHANGED", "企业飞书配置已变更，旧任务不再发送");
            return false;
        }
        try {
            validateEnterpriseConfig(enterprise);
        } catch (BusinessException exception) {
            markFinalFailed(task, "CONFIG_INVALID", exception.getMessage());
            return false;
        }

        FeishuGroupBotClient.SendResult result = client.send(
                enterprise.getFeishuWebhookUrl(), enterprise.getFeishuSigningSecret(), task.getCardContent());
        if (result.success()) {
            task.setSendStatus("SUCCESS");
            task.setResponseCode(result.code());
            task.setResponseMessage("success");
            task.setSentAt(LocalDateTime.now());
            task.setNextRetryAt(null);
            task.setUpdatedBy(OPERATOR_SYSTEM);
            sendLogMapper.updateById(task);
            return true;
        }
        int retryCount = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        task.setRetryCount(retryCount);
        task.setResponseCode(result.code());
        task.setResponseMessage(truncate(result.message(), 1000));
        boolean retry = result.retryable() && retryCount <= defaultRetry(task.getMaxRetry());
        task.setSendStatus(retry ? "FAILED" : "FINAL_FAILED");
        task.setNextRetryAt(retry ? LocalDateTime.now().plusMinutes(retryDelayMinutes(retryCount)) : null);
        task.setUpdatedBy(OPERATOR_SYSTEM);
        sendLogMapper.updateById(task);
        return false;
    }

    private void validateEnterpriseConfig(EnterpriseEntity enterprise) {
        if (enterprise.getFeishuGroupName() == null || enterprise.getFeishuGroupName().isBlank()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请配置飞书通知群名称");
        }
        if (enterprise.getFeishuSigningSecret() == null || enterprise.getFeishuSigningSecret().isBlank()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请配置飞书群机器人签名密钥");
        }
        try {
            client.validateWebhook(enterprise.getFeishuWebhookUrl());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CODE_BAD_REQUEST, exception.getMessage());
        }
    }

    private boolean canSendBusinessNotification(EnterpriseEntity enterprise) {
        if (enterprise == null || !Boolean.TRUE.equals(enterprise.getEnabled())
                || !Boolean.TRUE.equals(enterprise.getFeishuNotifyEnabled())) {
            return false;
        }
        try {
            validateEnterpriseConfig(enterprise);
            return true;
        } catch (BusinessException exception) {
            log.warn("企业飞书通知配置不可用 enterpriseId={} reason={}", enterprise.getId(), exception.getMessage());
            return false;
        }
    }

    private void markFinalFailed(FeishuSendLogEntity task, String code, String message) {
        if (task == null) {
            return;
        }
        task.setSendStatus("FINAL_FAILED");
        task.setResponseCode(code);
        task.setResponseMessage(truncate(message, 1000));
        task.setNextRetryAt(null);
        task.setUpdatedBy(OPERATOR_SYSTEM);
        sendLogMapper.updateById(task);
    }

    private void markCancelled(FeishuSendLogEntity task, String code, String message) {
        task.setSendStatus("CANCELLED");
        task.setResponseCode(code);
        task.setResponseMessage(truncate(message, 1000));
        task.setNextRetryAt(null);
        task.setUpdatedBy(OPERATOR_SYSTEM);
        sendLogMapper.updateById(task);
    }

    private void updateEnterpriseTestStatus(Long enterpriseId, boolean success, String error) {
        enterpriseMapper.update(null, new LambdaUpdateWrapper<EnterpriseEntity>()
                .eq(EnterpriseEntity::getId, enterpriseId)
                .set(EnterpriseEntity::getFeishuConnectionStatus, success ? "OK" : "ERROR")
                .set(EnterpriseEntity::getFeishuLastTestAt, LocalDateTime.now())
                .set(EnterpriseEntity::getFeishuLastError, success ? null : truncate(error, 512))
                .set(EnterpriseEntity::getUpdatedBy, OPERATOR_SYSTEM));
    }

    private String safeMessage(FeishuSendLogEntity task) {
        if (task == null || task.getResponseMessage() == null || task.getResponseMessage().isBlank()) {
            return "飞书群消息发送失败";
        }
        return truncate(task.getResponseMessage(), 500);
    }

    private int retryDelayMinutes(int retryCount) {
        return switch (retryCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 15;
            case 4 -> 30;
            default -> 60;
        };
    }

    private int defaultRetry(Integer value) {
        return value == null ? properties.getMaxRetry() : value;
    }

    private int defaultVersion(Integer value) {
        return value == null ? 0 : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record BatchResult(int total, int success, int failed) {
    }

    private record TaskCreation(Long logId, boolean created) {
    }
}
