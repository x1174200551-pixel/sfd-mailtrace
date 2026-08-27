-- 通知模板改为全局共享，并由邮箱按发送场景显式绑定。
-- 兼容 MySQL 5.7：不使用窗口函数、CTE 或 CHECK 约束。

-- 1、移除 V18 引入的企业维度索引，先处理跨企业重复编码。
ALTER TABLE `mt_notification_template`
  DROP INDEX `uk_mt_template_enterprise_code`,
  DROP INDEX `idx_mt_template_enterprise_type`;

UPDATE `mt_notification_template` AS `template`
JOIN (
  SELECT *
  FROM (
    SELECT `template_code`, MIN(`id`) AS `keep_id`
    FROM `mt_notification_template`
    GROUP BY `template_code`
    HAVING COUNT(*) > 1
  ) AS `duplicate_code`
) AS `duplicate_template`
  ON `duplicate_template`.`template_code` = `template`.`template_code`
SET `template`.`template_code` = CONCAT(LEFT(`template`.`template_code`, 48), '_', `template`.`id`),
    `template`.`updated_by` = 'SYSTEM'
WHERE `template`.`id` <> `duplicate_template`.`keep_id`;

-- 2、模板库不再保存企业归属，编码在全局范围唯一。
ALTER TABLE `mt_notification_template`
  DROP COLUMN `enterprise_id`,
  ADD UNIQUE KEY `uk_mt_notification_template_code` (`template_code`),
  ADD KEY `idx_mt_template_type_enabled` (`template_type`, `is_enabled`);

-- 3、修正历史模板类型并补齐全局默认场景模板。
UPDATE `mt_notification_template`
SET `template_type` = CASE
      WHEN `template_code` = 'AUTO_REPLY' THEN 'AUTO_REPLY'
      WHEN `template_code` = 'ASSIGN_NOTIFY' THEN 'ASSIGN_NOTIFY'
      WHEN `template_code` = 'AGENT_REPLY' THEN 'AGENT_REPLY'
      WHEN `template_code` = 'SLA_WARNING' THEN 'SLA_WARNING'
      WHEN `template_code` = 'SLA_BREACH' THEN 'SLA_BREACH'
      ELSE COALESCE(`template_type`, 'SYSTEM')
    END,
    `updated_by` = 'SYSTEM';

INSERT INTO `mt_notification_template` (
  `template_code`, `template_type`, `template_name`, `subject_tpl`, `content_tpl`,
  `is_enabled`, `created_by`, `updated_by`, `is_deleted`
) VALUES
  ('AUTO_REPLY', 'AUTO_REPLY', '自动回执模板', '您的工单已创建：{ticket_no}',
   '您好，您的邮件已进入工单系统。\n\n工单号：{ticket_no}\n工单主题：{subject}\n查看链接：{customer_ticket_url}\n校验码：{customer_ticket_code}\n有效期至：{customer_ticket_expires_at}\n\n我们会尽快处理并回复您。',
   1, 'SYSTEM', 'SYSTEM', 0),
  ('ASSIGN_NOTIFY', 'ASSIGN_NOTIFY', '分配通知模板', '新工单待处理：{ticket_no}',
   '您好，{assignee_name}，您有一个新分配工单。\n\n工单号：{ticket_no}\n主题：{subject}\n客户：{customer_email}\n请及时处理：{ticket_link}',
   1, 'SYSTEM', 'SYSTEM', 0),
  ('AGENT_REPLY', 'AGENT_REPLY', '处理人回复模板', '关于工单 {ticket_no} 的回复',
   '您好，关于您的工单 {ticket_no}，我们回复如下：\n\n{reply_content}',
   1, 'SYSTEM', 'SYSTEM', 0),
  ('SLA_WARNING', 'SLA_WARNING', 'SLA 预警模板', 'SLA 即将超时：{ticket_no}',
   '工单 {ticket_no} 即将在 {sla_deadline} 超时，请尽快处理。\n\n工单主题：{subject}\n处理人：{assignee_name}',
   1, 'SYSTEM', 'SYSTEM', 0),
  ('SLA_BREACH', 'SLA_BREACH', 'SLA 超时模板', 'SLA 已超时：{ticket_no}',
   '工单 {ticket_no} 已超过 SLA 时限，请管理员关注并升级处理。\n\n客户：{customer_email}\n链接：{ticket_link}',
   1, 'SYSTEM', 'SYSTEM', 0)
ON DUPLICATE KEY UPDATE
  `template_type` = VALUES(`template_type`),
  `is_enabled` = 1,
  `is_deleted` = 0,
  `updated_by` = 'SYSTEM';

UPDATE `mt_notification_template`
SET `content_tpl` = CONCAT(`content_tpl`, '\n\n{reply_content}'),
    `updated_by` = 'SYSTEM'
WHERE `template_code` = 'AGENT_REPLY'
  AND `content_tpl` NOT LIKE '%{reply_content}%';

-- 4、邮箱按实际发送场景选择全局模板。
ALTER TABLE `mt_mailbox`
  ADD COLUMN `assignment_notify_template_id` BIGINT DEFAULT NULL COMMENT '分配通知模板ID' AFTER `auto_reply_template_id`,
  ADD COLUMN `agent_reply_template_id` BIGINT DEFAULT NULL COMMENT '处理人回复模板ID' AFTER `assignment_notify_template_id`,
  ADD COLUMN `sla_warning_template_id` BIGINT DEFAULT NULL COMMENT 'SLA预警模板ID' AFTER `agent_reply_template_id`,
  ADD COLUMN `sla_breach_template_id` BIGINT DEFAULT NULL COMMENT 'SLA超时模板ID' AFTER `sla_warning_template_id`,
  ADD KEY `idx_mt_mailbox_assign_notify_tpl` (`assignment_notify_template_id`),
  ADD KEY `idx_mt_mailbox_agent_reply_tpl` (`agent_reply_template_id`),
  ADD KEY `idx_mt_mailbox_sla_warning_tpl` (`sla_warning_template_id`),
  ADD KEY `idx_mt_mailbox_sla_breach_tpl` (`sla_breach_template_id`);

SET @auto_reply_template_id = (
  SELECT `id` FROM `mt_notification_template`
  WHERE `template_code` = 'AUTO_REPLY' AND `is_deleted` = 0 LIMIT 1
);
SET @assignment_notify_template_id = (
  SELECT `id` FROM `mt_notification_template`
  WHERE `template_code` = 'ASSIGN_NOTIFY' AND `is_deleted` = 0 LIMIT 1
);
SET @agent_reply_template_id = (
  SELECT `id` FROM `mt_notification_template`
  WHERE `template_code` = 'AGENT_REPLY' AND `is_deleted` = 0 LIMIT 1
);
SET @sla_warning_template_id = (
  SELECT `id` FROM `mt_notification_template`
  WHERE `template_code` = 'SLA_WARNING' AND `is_deleted` = 0 LIMIT 1
);
SET @sla_breach_template_id = (
  SELECT `id` FROM `mt_notification_template`
  WHERE `template_code` = 'SLA_BREACH' AND `is_deleted` = 0 LIMIT 1
);

UPDATE `mt_mailbox`
SET `auto_reply_template_id` = COALESCE(`auto_reply_template_id`, @auto_reply_template_id),
    `assignment_notify_template_id` = COALESCE(`assignment_notify_template_id`, @assignment_notify_template_id),
    `agent_reply_template_id` = COALESCE(`agent_reply_template_id`, @agent_reply_template_id),
    `sla_warning_template_id` = COALESCE(`sla_warning_template_id`, @sla_warning_template_id),
    `sla_breach_template_id` = COALESCE(`sla_breach_template_id`, @sla_breach_template_id),
    `updated_by` = 'SYSTEM';
