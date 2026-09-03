-- SLA 通知节点开关：默认仅发送首次响应预警/超时、解决预警/超时。
ALTER TABLE `mt_sla_policy`
  ADD COLUMN `response_warning_notify_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '首次响应预警通知：0关闭，1开启' AFTER `escalate_after_breach_hours`,
  ADD COLUMN `response_breach_notify_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '首次响应超时通知：0关闭，1开启' AFTER `response_warning_notify_enabled`,
  ADD COLUMN `response_escalation_notify_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '首次响应超时升级通知：0关闭，1开启' AFTER `response_breach_notify_enabled`,
  ADD COLUMN `resolve_warning_notify_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '解决预警通知：0关闭，1开启' AFTER `response_escalation_notify_enabled`,
  ADD COLUMN `resolve_breach_notify_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '解决超时通知：0关闭，1开启' AFTER `resolve_warning_notify_enabled`,
  ADD COLUMN `resolve_escalation_notify_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '解决超时升级通知：0关闭，1开启' AFTER `resolve_breach_notify_enabled`;

ALTER TABLE `mt_mail_send_log`
  MODIFY COLUMN `send_type` VARCHAR(32) NOT NULL COMMENT '发送类型：AUTO_REPLY/ASSIGN_NOTIFY/AGENT_REPLY/SLA_RESPONSE_WARNING/SLA_RESPONSE_BREACH/SLA_RESPONSE_ESCALATION/SLA_RESOLVE_WARNING/SLA_RESOLVE_BREACH/SLA_RESOLVE_ESCALATION';

ALTER TABLE `mt_feishu_send_log`
  MODIFY COLUMN `send_type` VARCHAR(32) NOT NULL COMMENT 'ASSIGN_NOTIFY/SLA_RESPONSE_WARNING/SLA_RESPONSE_BREACH/SLA_RESPONSE_ESCALATION/SLA_RESOLVE_WARNING/SLA_RESOLVE_BREACH/SLA_RESOLVE_ESCALATION/TEST',
  MODIFY COLUMN `send_status` VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SUCCESS/FAILED/FINAL_FAILED/CANCELLED';
