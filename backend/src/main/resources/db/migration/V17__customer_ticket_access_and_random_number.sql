-- 客户查看工单基础字段和新编号规则默认参数

ALTER TABLE `mt_ticket`
  ADD COLUMN `customer_access_code_hash` VARCHAR(128) DEFAULT NULL COMMENT '客户查看校验码Hash' AFTER `customer_email`,
  ADD COLUMN `customer_access_expires_at` DATETIME(3) DEFAULT NULL COMMENT '客户查看有效期' AFTER `customer_access_code_hash`,
  ADD COLUMN `customer_access_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '客户查看开关：0否，1是' AFTER `customer_access_expires_at`,
  ADD KEY `idx_mt_ticket_customer_access_expires_at` (`customer_access_expires_at`);

INSERT INTO `mt_sys_param` (
  `param_key`,
  `param_value`,
  `param_desc`,
  `created_by`,
  `updated_by`
) VALUES
  ('ticket.no.date_format', 'yyMMddHHmmss', '工单号日期格式', 'SYSTEM', 'SYSTEM'),
  ('ticket.no.seq_length', '6', '工单号随机数位数', 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
  `param_value` = VALUES(`param_value`),
  `param_desc` = VALUES(`param_desc`),
  `updated_by` = 'SYSTEM';

UPDATE `mt_notification_template`
SET `content_tpl` = '您好，您的邮件已进入工单系统。\n\n工单号：{ticket_no}\n工单主题：{subject}\n查看链接：{customer_ticket_url}\n校验码：{customer_ticket_code}\n有效期至：{customer_ticket_expires_at}\n\n我们会尽快处理并回复您。',
    `updated_by` = 'SYSTEM'
WHERE `template_code` = 'AUTO_REPLY'
  AND `content_tpl` NOT LIKE '%{customer_ticket_url}%';
