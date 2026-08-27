-- MODEL-P3 企业和规则组配置权限。
-- 仅新增 RBAC 种子数据，不修改业务表；兼容 MySQL 5.7。

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES (
  'menu:enterprise_config', '企业配置', 'MENU', NULL, 'ENTERPRISE', NULL, NULL,
  '一级菜单分组', 1, 1, 35, 'SYSTEM', 'SYSTEM'
)
ON DUPLICATE KEY UPDATE
  `permission_name` = VALUES(`permission_name`),
  `permission_type` = VALUES(`permission_type`),
  `parent_id` = VALUES(`parent_id`),
  `module_code` = VALUES(`module_code`),
  `route_path` = VALUES(`route_path`),
  `action_code` = VALUES(`action_code`),
  `permission_desc` = VALUES(`permission_desc`),
  `is_system` = VALUES(`is_system`),
  `is_enabled` = VALUES(`is_enabled`),
  `sort_order` = VALUES(`sort_order`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

SET @menu_enterprise_config_id = (
  SELECT `id` FROM `mt_permission`
  WHERE `permission_code` = 'menu:enterprise_config' AND `is_deleted` = 0
  LIMIT 1
);
SET @menu_assignment_rules_id = (
  SELECT `id` FROM `mt_permission`
  WHERE `permission_code` = 'menu:assignment_rules' AND `is_deleted` = 0
  LIMIT 1
);
SET @menu_notification_templates_id = (
  SELECT `id` FROM `mt_permission`
  WHERE `permission_code` = 'menu:notification_templates' AND `is_deleted` = 0
  LIMIT 1
);

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('menu:enterprises', '企业管理', 'MENU', @menu_enterprise_config_id, 'ENTERPRISE', '企业管理', NULL, '企业管理页面入口', 1, 1, 36, 'SYSTEM', 'SYSTEM'),
  ('enterprise:read', '查看企业', 'ACTION', @menu_enterprise_config_id, 'ENTERPRISE', NULL, 'READ', '查看企业配置', 1, 1, 12010, 'SYSTEM', 'SYSTEM'),
  ('enterprise:create', '新建企业', 'ACTION', @menu_enterprise_config_id, 'ENTERPRISE', NULL, 'CREATE', '新建企业配置', 1, 1, 12020, 'SYSTEM', 'SYSTEM'),
  ('enterprise:update', '编辑企业', 'ACTION', @menu_enterprise_config_id, 'ENTERPRISE', NULL, 'UPDATE', '编辑企业配置', 1, 1, 12030, 'SYSTEM', 'SYSTEM'),
  ('enterprise:enable', '启停企业', 'ACTION', @menu_enterprise_config_id, 'ENTERPRISE', NULL, 'ENABLE', '启用或停用企业', 1, 1, 12040, 'SYSTEM', 'SYSTEM'),
  ('notification_template:delete', '删除通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'DELETE', '删除未被邮箱引用的通知模板', 1, 1, 12050, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:read', '查看分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_READ', '查看企业分配规则组', 1, 1, 12110, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:create', '新建分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_CREATE', '新建企业分配规则组', 1, 1, 12120, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:update', '编辑分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_UPDATE', '编辑企业分配规则组', 1, 1, 12130, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:enable', '启停分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_ENABLE', '启用或停用企业分配规则组', 1, 1, 12140, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:delete', '删除分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_DELETE', '删除企业分配规则组', 1, 1, 12150, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
  `permission_name` = VALUES(`permission_name`),
  `permission_type` = VALUES(`permission_type`),
  `parent_id` = VALUES(`parent_id`),
  `module_code` = VALUES(`module_code`),
  `route_path` = VALUES(`route_path`),
  `action_code` = VALUES(`action_code`),
  `permission_desc` = VALUES(`permission_desc`),
  `is_system` = VALUES(`is_system`),
  `is_enabled` = VALUES(`is_enabled`),
  `sort_order` = VALUES(`sort_order`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

INSERT INTO `mt_role_permission` (`role_id`, `permission_id`, `created_by`, `updated_by`)
SELECT r.`id`, p.`id`, 'SYSTEM', 'SYSTEM'
FROM `mt_role` r
JOIN `mt_permission` p
WHERE r.`role_code` = 'ADMIN'
  AND r.`is_deleted` = 0
  AND p.`is_deleted` = 0
  AND p.`permission_code` IN (
    'menu:enterprise_config', 'menu:enterprises',
    'enterprise:read', 'enterprise:create', 'enterprise:update', 'enterprise:enable',
    'notification_template:delete',
    'assignment_rule_group:read', 'assignment_rule_group:create', 'assignment_rule_group:update',
    'assignment_rule_group:enable', 'assignment_rule_group:delete'
  )
ON DUPLICATE KEY UPDATE
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;
