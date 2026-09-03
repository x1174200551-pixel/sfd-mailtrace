-- SLA 两阶段通知状态：保留原有两个截止时间作为建单快照，新增预警/升级触发时间和阶段级幂等时间。
ALTER TABLE `mt_ticket`
  ADD COLUMN `sla_warning_remain_hours_snapshot` INT DEFAULT NULL COMMENT '建单时预警提前工作小时数快照' AFTER `sla_policy_id`,
  ADD COLUMN `sla_escalate_after_breach_hours_snapshot` INT DEFAULT NULL COMMENT '建单时超时升级工作小时数快照' AFTER `sla_warning_remain_hours_snapshot`,
  ADD COLUMN `sla_response_warning_at` DATETIME(3) DEFAULT NULL COMMENT '首次响应SLA预警触发时间（建单快照）' AFTER `sla_response_deadline`,
  ADD COLUMN `sla_response_escalation_at` DATETIME(3) DEFAULT NULL COMMENT '首次响应SLA超时升级触发时间（建单快照）' AFTER `sla_response_warning_at`,
  ADD COLUMN `sla_resolve_warning_at` DATETIME(3) DEFAULT NULL COMMENT '解决SLA预警触发时间（建单快照）' AFTER `sla_resolve_deadline`,
  ADD COLUMN `sla_resolve_escalation_at` DATETIME(3) DEFAULT NULL COMMENT '解决SLA超时升级触发时间（建单快照）' AFTER `sla_resolve_warning_at`,
  ADD COLUMN `sla_response_warning_triggered_at` DATETIME(3) DEFAULT NULL COMMENT '首次响应SLA预警事件创建时间' AFTER `sla_breach_notified`,
  ADD COLUMN `sla_response_breach_triggered_at` DATETIME(3) DEFAULT NULL COMMENT '首次响应SLA超时事件创建时间' AFTER `sla_response_warning_triggered_at`,
  ADD COLUMN `sla_response_escalation_triggered_at` DATETIME(3) DEFAULT NULL COMMENT '首次响应SLA升级事件创建时间' AFTER `sla_response_breach_triggered_at`,
  ADD COLUMN `sla_resolve_warning_triggered_at` DATETIME(3) DEFAULT NULL COMMENT '解决SLA预警事件创建时间' AFTER `sla_response_escalation_triggered_at`,
  ADD COLUMN `sla_resolve_breach_triggered_at` DATETIME(3) DEFAULT NULL COMMENT '解决SLA超时事件创建时间' AFTER `sla_resolve_warning_triggered_at`,
  ADD COLUMN `sla_resolve_escalation_triggered_at` DATETIME(3) DEFAULT NULL COMMENT '解决SLA升级事件创建时间' AFTER `sla_resolve_breach_triggered_at`,
  ADD KEY `idx_mt_ticket_sla_response_schedule` (`sla_response_deadline`, `sla_response_warning_at`, `sla_response_breach_triggered_at`),
  ADD KEY `idx_mt_ticket_sla_resolve_schedule` (`sla_resolve_deadline`, `sla_resolve_warning_at`, `sla_resolve_breach_triggered_at`);

-- 老字段继续作为汇总兼容字段；新的阶段字段才承担幂等控制。
ALTER TABLE `mt_mail_send_log`
  MODIFY COLUMN `send_type` VARCHAR(32) NOT NULL COMMENT '发送类型：AUTO_REPLY/ASSIGN_NOTIFY/AGENT_REPLY/SLA_WARNING/SLA_BREACH/SLA_ESCALATION';

ALTER TABLE `mt_feishu_send_log`
  MODIFY COLUMN `send_type` VARCHAR(32) NOT NULL COMMENT 'ASSIGN_NOTIFY/SLA_WARNING/SLA_BREACH/SLA_ESCALATION/TEST';
