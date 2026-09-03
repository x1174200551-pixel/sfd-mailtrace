-- 邮件投递一致性：记录客服回复业务状态是否已完成补偿。
-- 兼容 MySQL 5.7：仅使用普通列、索引和条件更新。

ALTER TABLE `mt_ticket_message`
  ADD COLUMN `delivery_completed_at` DATETIME(3) DEFAULT NULL COMMENT '外发成功后的工单状态补偿完成时间' AFTER `send_status`,
  ADD KEY `idx_mt_ticket_message_delivery` (`direction`, `send_status`, `delivery_completed_at`, `id`);

-- 历史成功外发已经由旧流程同步更新过工单状态，避免升级后重复补偿事件。
UPDATE `mt_ticket_message`
SET `delivery_completed_at` = COALESCE(`sent_at`, `updated_at`, `created_at`)
WHERE `direction` = 'OUTBOUND'
  AND `send_status` = 'SUCCESS'
  AND `delivery_completed_at` IS NULL
  AND `is_deleted` = 0;
