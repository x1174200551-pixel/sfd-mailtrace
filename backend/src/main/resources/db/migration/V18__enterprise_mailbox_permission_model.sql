-- MODEL-P0-EXPAND 企业邮箱权限模型扩展。
-- 本阶段只扩表和回填，不切换现有角色数据范围；新增归属字段保持可空，兼容旧应用写入。

CREATE TABLE `mt_enterprise` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enterprise_name` VARCHAR(128) NOT NULL COMMENT '企业名称',
  `contact_name`    VARCHAR(64)           DEFAULT NULL COMMENT '联系人',
  `contact_email`   VARCHAR(128)          DEFAULT NULL COMMENT '联系邮箱',
  `contact_phone`   VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
  `is_enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `remark`          VARCHAR(512)          DEFAULT NULL COMMENT '备注',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_enterprise_name` (`enterprise_name`),
  KEY `idx_mt_enterprise_enabled` (`is_enabled`, `enterprise_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业业务归属表';

INSERT INTO `mt_enterprise` (
  `enterprise_name`, `is_enabled`, `remark`, `created_by`, `updated_by`
) VALUES (
  '默认企业', 1, '系统迁移默认企业，用于承接历史业务数据', 'SYSTEM', 'SYSTEM'
)
ON DUPLICATE KEY UPDATE
  `is_enabled` = 1,
  `remark` = VALUES(`remark`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

SET @default_enterprise_id = (
  SELECT `id`
  FROM `mt_enterprise`
  WHERE `enterprise_name` = '默认企业'
    AND `is_deleted` = 0
  LIMIT 1
);

CREATE TABLE `mt_user_data_grant` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
  `grant_type`      VARCHAR(32)  NOT NULL COMMENT '授权类型：ENTERPRISE/MAILBOX',
  `enterprise_id`   BIGINT                DEFAULT NULL COMMENT '授权企业ID',
  `mailbox_id`      BIGINT                DEFAULT NULL COMMENT '授权邮箱ID',
  `is_enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `remark`          VARCHAR(512)          DEFAULT NULL COMMENT '授权备注',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_user_grant_enterprise` (`user_id`, `grant_type`, `enterprise_id`),
  UNIQUE KEY `uk_mt_user_grant_mailbox` (`user_id`, `grant_type`, `mailbox_id`),
  KEY `idx_mt_user_data_grant_user` (`user_id`, `is_enabled`),
  KEY `idx_mt_user_data_grant_enterprise` (`enterprise_id`, `is_enabled`),
  KEY `idx_mt_user_data_grant_mailbox` (`mailbox_id`, `is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户企业和邮箱数据授权表';

CREATE TABLE `mt_assignment_rule_group` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enterprise_id`   BIGINT       NOT NULL COMMENT '所属企业ID',
  `group_name`      VARCHAR(128) NOT NULL COMMENT '规则组名称',
  `is_enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `remark`          VARCHAR(512)          DEFAULT NULL COMMENT '备注',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_assignment_group_name` (`enterprise_id`, `group_name`),
  KEY `idx_mt_assignment_group_enterprise` (`enterprise_id`, `is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业分配规则组表';

INSERT INTO `mt_assignment_rule_group` (
  `enterprise_id`, `group_name`, `is_enabled`, `remark`, `created_by`, `updated_by`
)
SELECT @default_enterprise_id, '默认规则组', 1, '系统迁移默认规则组，用于承接历史分配规则', 'SYSTEM', 'SYSTEM'
WHERE @default_enterprise_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `is_enabled` = 1,
  `remark` = VALUES(`remark`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

SET @default_assignment_group_id = (
  SELECT `id`
  FROM `mt_assignment_rule_group`
  WHERE `enterprise_id` = @default_enterprise_id
    AND `group_name` = '默认规则组'
    AND `is_deleted` = 0
  LIMIT 1
);

ALTER TABLE `mt_mailbox`
  ADD COLUMN `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业ID' AFTER `id`,
  ADD COLUMN `sla_policy_id` BIGINT DEFAULT NULL COMMENT '邮箱绑定的SLA策略ID' AFTER `auto_reply_template_id`,
  ADD COLUMN `assignment_rule_group_id` BIGINT DEFAULT NULL COMMENT '邮箱绑定的分配规则组ID' AFTER `sla_policy_id`,
  ADD COLUMN `assignment_fallback_type` VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '规则未命中处理方式：NONE/DEFAULT_ASSIGNEE' AFTER `assignment_rule_group_id`,
  ADD KEY `idx_mt_mailbox_enterprise` (`enterprise_id`, `is_enabled`),
  ADD KEY `idx_mt_mailbox_sla_policy` (`sla_policy_id`),
  ADD KEY `idx_mt_mailbox_assignment_group` (`assignment_rule_group_id`);

UPDATE `mt_mailbox`
SET `enterprise_id` = @default_enterprise_id,
    `assignment_fallback_type` = 'DEFAULT_ASSIGNEE',
    `updated_by` = 'SYSTEM'
WHERE `enterprise_id` IS NULL
  AND @default_enterprise_id IS NOT NULL;

ALTER TABLE `mt_notification_template`
  ADD COLUMN `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业ID' AFTER `id`,
  ADD COLUMN `template_type` VARCHAR(32) DEFAULT NULL COMMENT '模板类型' AFTER `template_code`,
  ADD KEY `idx_mt_template_enterprise_type` (`enterprise_id`, `template_type`, `is_enabled`);

UPDATE `mt_notification_template`
SET `enterprise_id` = @default_enterprise_id,
    `template_type` = CASE
      WHEN `template_code` LIKE 'AUTO_REPLY%' THEN 'AUTO_REPLY'
      WHEN `template_code` LIKE 'ASSIGN_NOTIFY%' THEN 'ASSIGN_NOTIFY'
      WHEN `template_code` LIKE 'SLA_WARNING%' THEN 'SLA_WARNING'
      WHEN `template_code` LIKE 'SLA_BREACH%' THEN 'SLA_BREACH'
      ELSE 'SYSTEM'
    END,
    `updated_by` = 'SYSTEM'
WHERE @default_enterprise_id IS NOT NULL;

ALTER TABLE `mt_notification_template`
  DROP INDEX `uk_mt_notification_template_code`,
  ADD UNIQUE KEY `uk_mt_template_enterprise_code` (`enterprise_id`, `template_code`);

ALTER TABLE `mt_sla_policy`
  ADD COLUMN `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业ID' AFTER `id`,
  ADD KEY `idx_mt_sla_policy_enterprise` (`enterprise_id`, `is_enabled`);

UPDATE `mt_sla_policy`
SET `enterprise_id` = @default_enterprise_id,
    `updated_by` = 'SYSTEM'
WHERE `enterprise_id` IS NULL
  AND @default_enterprise_id IS NOT NULL;

ALTER TABLE `mt_work_calendar`
  ADD COLUMN `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业ID' AFTER `id`,
  ADD KEY `idx_mt_work_calendar_enterprise` (`enterprise_id`);

UPDATE `mt_work_calendar`
SET `enterprise_id` = @default_enterprise_id,
    `updated_by` = 'SYSTEM'
WHERE `enterprise_id` IS NULL
  AND @default_enterprise_id IS NOT NULL;

ALTER TABLE `mt_customer`
  ADD COLUMN `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业ID' AFTER `id`,
  DROP INDEX `uk_mt_customer_email`,
  ADD UNIQUE KEY `uk_mt_customer_enterprise_email` (`enterprise_id`, `email`),
  ADD KEY `idx_mt_customer_enterprise` (`enterprise_id`);

UPDATE `mt_customer`
SET `enterprise_id` = @default_enterprise_id,
    `updated_by` = 'SYSTEM'
WHERE `enterprise_id` IS NULL
  AND @default_enterprise_id IS NOT NULL;

ALTER TABLE `mt_ticket`
  ADD COLUMN `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业ID' AFTER `id`,
  ADD COLUMN `auto_reply_template_id` BIGINT DEFAULT NULL COMMENT '建单时使用的自动回复模板ID' AFTER `sla_policy_id`,
  ADD COLUMN `assignment_rule_group_id` BIGINT DEFAULT NULL COMMENT '建单时使用的分配规则组ID' AFTER `auto_reply_template_id`,
  ADD COLUMN `assignment_rule_id` BIGINT DEFAULT NULL COMMENT '建单时命中的分配规则ID' AFTER `assignment_rule_group_id`,
  ADD KEY `idx_mt_ticket_enterprise` (`enterprise_id`, `created_at`),
  ADD KEY `idx_mt_ticket_assignment_group` (`assignment_rule_group_id`),
  ADD KEY `idx_mt_ticket_assignment_rule` (`assignment_rule_id`);

UPDATE `mt_ticket` t
LEFT JOIN `mt_mailbox` m ON m.`id` = t.`mailbox_id`
SET t.`enterprise_id` = COALESCE(m.`enterprise_id`, @default_enterprise_id),
    t.`updated_by` = 'SYSTEM'
WHERE t.`enterprise_id` IS NULL
  AND @default_enterprise_id IS NOT NULL;

ALTER TABLE `mt_assignment_rule`
  ADD COLUMN `group_id` BIGINT DEFAULT NULL COMMENT '所属分配规则组ID' AFTER `id`,
  ADD KEY `idx_mt_assignment_rule_group` (`group_id`, `is_enabled`, `priority_order`);

UPDATE `mt_assignment_rule`
SET `group_id` = @default_assignment_group_id,
    `updated_by` = 'SYSTEM'
WHERE `group_id` IS NULL
  AND @default_assignment_group_id IS NOT NULL;

ALTER TABLE `mt_mail_fetch_log`
  ADD COLUMN `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业ID' AFTER `mailbox_id`,
  ADD KEY `idx_mt_mail_fetch_enterprise` (`enterprise_id`, `started_at`);

UPDATE `mt_mail_fetch_log` l
LEFT JOIN `mt_mailbox` m ON m.`id` = l.`mailbox_id`
SET l.`enterprise_id` = COALESCE(m.`enterprise_id`, @default_enterprise_id),
    l.`updated_by` = 'SYSTEM'
WHERE l.`enterprise_id` IS NULL
  AND @default_enterprise_id IS NOT NULL;

ALTER TABLE `mt_mail_send_log`
  ADD COLUMN `enterprise_id` BIGINT DEFAULT NULL COMMENT '所属企业ID' AFTER `mailbox_id`,
  ADD COLUMN `template_id` BIGINT DEFAULT NULL COMMENT '发送使用的模板ID' AFTER `send_type`,
  ADD COLUMN `template_type` VARCHAR(32) DEFAULT NULL COMMENT '发送使用的模板类型' AFTER `template_id`,
  ADD KEY `idx_mt_mail_send_enterprise` (`enterprise_id`, `created_at`),
  ADD KEY `idx_mt_mail_send_template` (`template_id`, `template_type`);

UPDATE `mt_mail_send_log` l
LEFT JOIN `mt_mailbox` m ON m.`id` = l.`mailbox_id`
LEFT JOIN `mt_ticket` t ON t.`id` = l.`ticket_id`
SET l.`enterprise_id` = COALESCE(m.`enterprise_id`, t.`enterprise_id`, @default_enterprise_id),
    l.`template_type` = COALESCE(l.`template_type`, l.`send_type`),
    l.`updated_by` = 'SYSTEM'
WHERE @default_enterprise_id IS NOT NULL;

INSERT INTO `mt_user_data_grant` (
  `user_id`, `grant_type`, `enterprise_id`, `mailbox_id`, `is_enabled`, `remark`, `created_by`, `updated_by`
)
SELECT
  u.`id`, 'ENTERPRISE', @default_enterprise_id, NULL, 1,
  '历史普通用户默认企业授权', 'SYSTEM', 'SYSTEM'
FROM `mt_user` u
WHERE u.`is_deleted` = 0
  AND @default_enterprise_id IS NOT NULL
  AND UPPER(COALESCE(u.`role_code`, '')) <> 'ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM `mt_user_role` ur
    INNER JOIN `mt_role` r ON r.`id` = ur.`role_id`
    WHERE ur.`user_id` = u.`id`
      AND ur.`is_deleted` = 0
      AND r.`is_deleted` = 0
      AND UPPER(r.`role_code`) = 'ADMIN'
  )
ON DUPLICATE KEY UPDATE
  `is_enabled` = 1,
  `remark` = VALUES(`remark`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;
