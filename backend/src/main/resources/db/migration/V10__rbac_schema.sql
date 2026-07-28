-- AUTH-S2-DB-01 RBAC table schema

CREATE TABLE `mt_role` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code`       VARCHAR(64)  NOT NULL COMMENT '角色编码：ADMIN/AGENT/SUPERVISOR等',
  `role_name`       VARCHAR(64)  NOT NULL COMMENT '角色名称',
  `role_desc`       VARCHAR(512)          DEFAULT NULL COMMENT '角色说明',
  `is_system`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否系统内置角色：0否，1是',
  `is_enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `sort_order`      INT          NOT NULL DEFAULT 100 COMMENT '排序值，数字越小越靠前',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_role_code` (`role_code`),
  KEY `idx_mt_role_enabled` (`is_enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC角色表';

CREATE TABLE `mt_permission` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_code`   VARCHAR(128) NOT NULL COMMENT '权限编码，如 menu:ticket、ticket:reply',
  `permission_name`   VARCHAR(128) NOT NULL COMMENT '权限名称',
  `permission_type`   VARCHAR(32)  NOT NULL COMMENT '权限类型：MENU/ACTION/API',
  `parent_id`         BIGINT                DEFAULT NULL COMMENT '父权限ID，用于菜单树或权限分组',
  `module_code`       VARCHAR(64)           DEFAULT NULL COMMENT '模块编码：TICKET/MAILBOX/SLA等',
  `route_path`        VARCHAR(256)          DEFAULT NULL COMMENT '前端路由或菜单标识',
  `action_code`       VARCHAR(64)           DEFAULT NULL COMMENT '动作编码：READ/CREATE/UPDATE/DELETE/REPLY等',
  `permission_desc`   VARCHAR(512)          DEFAULT NULL COMMENT '权限说明',
  `is_system`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否系统内置权限：0否，1是',
  `is_enabled`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
  `sort_order`        INT          NOT NULL DEFAULT 100 COMMENT '排序值，数字越小越靠前',
  `created_by`        VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`        VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_permission_code` (`permission_code`),
  KEY `idx_mt_permission_parent` (`parent_id`, `sort_order`),
  KEY `idx_mt_permission_type` (`permission_type`, `is_enabled`),
  KEY `idx_mt_permission_module` (`module_code`, `action_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC权限表';

CREATE TABLE `mt_role_permission` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id`         BIGINT       NOT NULL COMMENT '角色ID',
  `permission_id`   BIGINT       NOT NULL COMMENT '权限ID',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_role_permission` (`role_id`, `permission_id`),
  KEY `idx_mt_role_permission_role` (`role_id`),
  KEY `idx_mt_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC角色权限关系表';

CREATE TABLE `mt_user_role` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
  `role_id`         BIGINT       NOT NULL COMMENT '角色ID',
  `is_primary`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否主角色：0否，1是',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_user_role` (`user_id`, `role_id`),
  KEY `idx_mt_user_role_user` (`user_id`),
  KEY `idx_mt_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC用户角色关系表';

CREATE TABLE `mt_role_data_scope` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id`         BIGINT       NOT NULL COMMENT '角色ID',
  `resource_type`   VARCHAR(64)  NOT NULL COMMENT '资源类型：TICKET/CUSTOMER等',
  `scope_code`      VARCHAR(64)  NOT NULL COMMENT '数据范围编码：ALL/SELF/TEAM/DEPT/DEPT_AND_CHILDREN',
  `scope_desc`      VARCHAR(512)          DEFAULT NULL COMMENT '数据范围说明',
  `created_by`      VARCHAR(64)  NOT NULL COMMENT '创建人',
  `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by`      VARCHAR(64)  NOT NULL COMMENT '最后更新人',
  `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_role_data_scope` (`role_id`, `resource_type`, `scope_code`),
  KEY `idx_mt_role_data_scope_role` (`role_id`),
  KEY `idx_mt_role_data_scope_resource` (`resource_type`, `scope_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC角色数据范围表';
