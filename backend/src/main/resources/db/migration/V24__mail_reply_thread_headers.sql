-- 邮件回复线程化：保存外发线程快照与工单消息发送状态。
-- 兼容 MySQL 5.7：不使用 CTE、窗口函数、CHECK 或 JSON 类型。

ALTER TABLE `mt_mail_send_log`
  ADD COLUMN `ticket_message_id` BIGINT DEFAULT NULL COMMENT '关联工单消息ID' AFTER `ticket_id`,
  ADD COLUMN `message_id` VARCHAR(255) DEFAULT NULL COMMENT '固定外发Message-ID，不含尖括号' AFTER `content_body`,
  ADD COLUMN `in_reply_to` VARCHAR(255) DEFAULT NULL COMMENT '父消息Message-ID，不含尖括号' AFTER `message_id`,
  ADD COLUMN `mail_references` VARCHAR(1000) DEFAULT NULL COMMENT '规范化References头' AFTER `in_reply_to`,
  ADD COLUMN `reply_to_address` VARCHAR(256) DEFAULT NULL COMMENT '外发Reply-To地址' AFTER `mail_references`,
  ADD COLUMN `content_type` VARCHAR(64) DEFAULT NULL COMMENT '正文MIME类型及字符集' AFTER `reply_to_address`,
  ADD KEY `idx_mt_mail_send_log_message` (`ticket_message_id`),
  ADD KEY `idx_mt_mail_send_log_message_id` (`message_id`);

ALTER TABLE `mt_ticket_message`
  ADD COLUMN `send_status` VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT 'PENDING/SUCCESS/FAILED' AFTER `direction`,
  ADD KEY `idx_mt_ticket_message_parent` (`ticket_id`, `direction`, `is_deleted`, `id`);

-- 历史入站、内部消息以及有 Message-ID 的外发消息按成功处理。
UPDATE `mt_ticket_message`
SET `send_status` = 'SUCCESS'
WHERE (`direction` <> 'OUTBOUND' OR (`message_id` IS NOT NULL AND `message_id` <> ''))
  AND `is_deleted` = 0;

-- 历史外发没有 Message-ID 时无法证明已成功投递，按失败状态保守回填。
UPDATE `mt_ticket_message`
SET `send_status` = 'FAILED'
WHERE `direction` = 'OUTBOUND'
  AND (`message_id` IS NULL OR `message_id` = '')
  AND `is_deleted` = 0;
