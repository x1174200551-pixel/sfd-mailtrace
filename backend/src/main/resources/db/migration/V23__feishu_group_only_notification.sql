-- 飞书通知调整为只发送到企业通知群，不再按用户 OpenId @ 个人。
-- V22 已经执行，不修改其历史语义；本迁移只做向前兼容升级。
-- MySQL 5.7 兼容：执行前已确认不存在重复的非空 ticket_event_id，可收敛为事件级幂等。

ALTER TABLE `mt_feishu_send_log`
  DROP INDEX `uk_mt_feishu_event_user`,
  DROP INDEX `idx_mt_feishu_user`,
  CHANGE COLUMN `user_id` `assignee_user_id` BIGINT DEFAULT NULL COMMENT '工单处理人ID，仅用于业务追踪，不作为飞书接收人',
  MODIFY COLUMN `open_id_snapshot` VARCHAR(128) DEFAULT NULL COMMENT '历史飞书OpenId快照，群通知模式不再写入',
  ADD UNIQUE KEY `uk_mt_feishu_event` (`ticket_event_id`),
  ADD KEY `idx_mt_feishu_assignee` (`assignee_user_id`);

-- mt_user.feishu_open_id 作为历史兼容字段暂时保留，当前业务代码不再读取或写入。
