-- AUTH-S3-DB-01 organization schema

CREATE TABLE `mt_department` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`       BIGINT                DEFAULT NULL COMMENT '父部门ID，根部门为空',
  `dept_code`       VARCHAR(64)  NOT NULL COMMENT '部门编码',
  `dept_name`       VARCHAR(128) NOT NULL COMMENT '部门名称',
  `dept_desc`       VARCHAR(512)          DEFAULT NULL COMMENT '部门说明',
  `leader_user_id`  BIGINT                DEFAULT NULL COMMENT '部门负责人用户ID',
  `dept_path`       VARCHAR(512)          DEFAULT NULL COMMENT '部门路径，用于后续下级部门范围查询',
  `is_enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `sort_order`      INT          NOT NULL DEFAULT 100 COMMENT '排序值，数字越小越靠前',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_department_code` (`dept_code`),
  KEY `idx_mt_department_parent` (`parent_id`, `sort_order`),
  KEY `idx_mt_department_enabled` (`is_enabled`, `sort_order`),
  KEY `idx_mt_department_leader` (`leader_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织部门表';

CREATE TABLE `mt_user_department` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
  `department_id`   BIGINT       NOT NULL COMMENT '部门ID',
  `is_primary`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否主部门：0否，1是',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_user_department` (`user_id`, `department_id`),
  KEY `idx_mt_user_department_user` (`user_id`, `is_primary`),
  KEY `idx_mt_user_department_department` (`department_id`, `is_primary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户部门关系表';

INSERT INTO `mt_department` (
  `parent_id`, `dept_code`, `dept_name`, `dept_desc`, `leader_user_id`, `dept_path`,
  `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES (
  NULL, 'DEFAULT', '默认部门', '系统迁移默认部门，用于承接历史用户', NULL, '/DEFAULT/',
  1, 10, 'SYSTEM', 'SYSTEM'
)
ON DUPLICATE KEY UPDATE
  `dept_name` = VALUES(`dept_name`),
  `dept_desc` = VALUES(`dept_desc`),
  `is_enabled` = VALUES(`is_enabled`),
  `sort_order` = VALUES(`sort_order`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

SET @default_department_id = (SELECT `id` FROM `mt_department` WHERE `dept_code` = 'DEFAULT');

INSERT INTO `mt_user_department` (
  `user_id`, `department_id`, `is_primary`, `created_by`, `updated_by`
)
SELECT u.`id`, @default_department_id, 1, 'SYSTEM', 'SYSTEM'
FROM `mt_user` u
WHERE u.`is_deleted` = 0
  AND @default_department_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `is_primary` = 1,
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;
