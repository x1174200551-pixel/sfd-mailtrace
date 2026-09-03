-- 企业级飞书群机器人通知。
-- MySQL 5.7 兼容：不使用 CTE、窗口函数、CHECK 或 SKIP LOCKED。

ALTER TABLE `mt_enterprise`
  ADD COLUMN `feishu_notify_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用企业飞书群通知：0否，1是' AFTER `is_enabled`,
  ADD COLUMN `feishu_group_name` VARCHAR(128) DEFAULT NULL COMMENT '企业飞书通知群名称，仅展示' AFTER `feishu_notify_enabled`,
  ADD COLUMN `feishu_webhook_url` VARCHAR(1024) DEFAULT NULL COMMENT '飞书群机器人Webhook地址' AFTER `feishu_group_name`,
  ADD COLUMN `feishu_signing_secret` VARCHAR(512) DEFAULT NULL COMMENT '飞书群机器人签名密钥' AFTER `feishu_webhook_url`,
  ADD COLUMN `feishu_config_version` INT NOT NULL DEFAULT 0 COMMENT '飞书配置版本，Webhook或签名变更时递增' AFTER `feishu_signing_secret`,
  ADD COLUMN `feishu_connection_status` VARCHAR(32) NOT NULL DEFAULT 'UNCONFIGURED' COMMENT '群机器人状态：UNCONFIGURED/UNTESTED/OK/ERROR' AFTER `feishu_config_version`,
  ADD COLUMN `feishu_last_test_at` DATETIME(3) DEFAULT NULL COMMENT '最近测试时间' AFTER `feishu_connection_status`,
  ADD COLUMN `feishu_last_error` VARCHAR(512) DEFAULT NULL COMMENT '最近一次脱敏错误摘要' AFTER `feishu_last_test_at`,
  ADD KEY `idx_mt_enterprise_feishu` (`feishu_notify_enabled`, `feishu_connection_status`);

ALTER TABLE `mt_user`
  ADD COLUMN `feishu_open_id` VARCHAR(128) DEFAULT NULL COMMENT '飞书OpenId，用于企业通知群@处理人，由用户管理人工维护' AFTER `email`,
  ADD KEY `idx_mt_user_feishu_open_id` (`feishu_open_id`);

CREATE TABLE `mt_feishu_send_log` (
  `id`                        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_event_id`           BIGINT                 DEFAULT NULL COMMENT '工单事件ID，业务通知幂等键',
  `ticket_id`                 BIGINT                 DEFAULT NULL COMMENT '工单ID，测试消息可空',
  `mailbox_id`                BIGINT                 DEFAULT NULL COMMENT '邮箱ID，测试消息可空',
  `enterprise_id`             BIGINT        NOT NULL COMMENT '企业ID',
  `enterprise_config_version` INT           NOT NULL DEFAULT 0 COMMENT '任务创建时企业飞书配置版本',
  `user_id`                   BIGINT        NOT NULL COMMENT '被@的系统用户ID',
  `send_type`                 VARCHAR(32)   NOT NULL COMMENT 'ASSIGN_NOTIFY/SLA_WARNING/SLA_BREACH/TEST',
  `template_id`               BIGINT                 DEFAULT NULL COMMENT '公共通知模板ID',
  `open_id_snapshot`          VARCHAR(128)  NOT NULL COMMENT '创建任务时飞书OpenId快照',
  `group_bot_name`            VARCHAR(128)           DEFAULT NULL COMMENT '企业通知群名称快照',
  `title`                     VARCHAR(512)  NOT NULL COMMENT '渲染后标题',
  `content_body`              TEXT          NOT NULL COMMENT '脱敏截断后的群消息摘要',
  `card_content`              MEDIUMTEXT    NOT NULL COMMENT '最终卡片JSON，不含Webhook和签名',
  `send_status`               VARCHAR(24)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SUCCESS/FAILED/FINAL_FAILED',
  `retry_count`               INT           NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry`                 INT           NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  `next_retry_at`             DATETIME(3)            DEFAULT NULL COMMENT '下次重试时间',
  `response_code`             VARCHAR(64)            DEFAULT NULL COMMENT '飞书业务码或HTTP状态',
  `response_message`          VARCHAR(1000)          DEFAULT NULL COMMENT '脱敏后的响应摘要',
  `sent_at`                   DATETIME(3)            DEFAULT NULL COMMENT '发送成功时间',
  `created_by`                VARCHAR(64)   NOT NULL COMMENT '创建人',
  `created_at`                DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`                VARCHAR(64)   NOT NULL COMMENT '最后更新人',
  `updated_at`                DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`                TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_feishu_event_user` (`ticket_event_id`, `user_id`),
  KEY `idx_mt_feishu_retry` (`send_status`, `next_retry_at`),
  KEY `idx_mt_feishu_ticket` (`ticket_id`),
  KEY `idx_mt_feishu_enterprise` (`enterprise_id`, `created_at`),
  KEY `idx_mt_feishu_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业飞书群机器人发送任务与审计日志';
