-- SLA 通知历史基线：本迁移执行前已经存在的工单不参与新的两阶段 SLA 通知。
-- 新建工单使用默认值 0，继续正常执行首次响应/解决的预警、超时和升级通知。
ALTER TABLE `mt_ticket`
  ADD COLUMN `sla_notification_suppressed` TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '是否抑制SLA通知：0否，1是（V27历史工单基线）'
    AFTER `sla_resolve_escalation_triggered_at`,
  ADD KEY `idx_mt_ticket_sla_notification` (`sla_notification_suppressed`, `status`);

-- V27 上线前的存量工单全部静默，不补发当前或未来的 SLA 通知。
UPDATE `mt_ticket`
SET `sla_notification_suppressed` = 1,
    `updated_by` = 'system'
WHERE `is_deleted` = 0;

-- 停止存量工单已经排队但尚未投递的 SLA 邮件，保留日志用于审计。
UPDATE `mt_mail_send_log`
SET `send_status` = 'CANCELLED',
    `next_retry_at` = NULL,
    `error_message` = 'V27历史工单SLA通知基线：停止补发',
    `updated_by` = 'SYSTEM'
WHERE `is_deleted` = 0
  AND `send_type` IN ('SLA_WARNING', 'SLA_BREACH', 'SLA_ESCALATION')
  AND `send_status` IN ('PENDING', 'FAILED', 'SENDING', 'RETRYING');

-- 停止存量工单已经排队但尚未投递的飞书 SLA 消息，成功记录保持不变。
UPDATE `mt_feishu_send_log`
SET `send_status` = 'FINAL_FAILED',
    `next_retry_at` = NULL,
    `response_code` = 'HISTORY_SUPPRESSED',
    `response_message` = 'V27历史工单SLA通知基线：停止补发',
    `updated_by` = 'system'
WHERE `is_deleted` = 0
  AND `send_type` IN ('SLA_WARNING', 'SLA_BREACH', 'SLA_ESCALATION')
  AND `send_status` IN ('PENDING', 'FAILED', 'SENDING');

ALTER TABLE `mt_mail_send_log`
  MODIFY COLUMN `send_status` VARCHAR(16) NOT NULL
    COMMENT '状态：PENDING/SENDING/SUCCESS/FAILED/RETRYING/DELIVERY_UNKNOWN/CANCELLED';
