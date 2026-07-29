-- AUTH-S3-BE-01 department management menu and permissions

SET @menu_system_management_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:system_management');

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('menu:departments', '组织管理', 'MENU', @menu_system_management_id, 'DEPARTMENT', '组织管理', NULL, '组织管理页面入口', 1, 1, 55, 'SYSTEM', 'SYSTEM')
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

SET @menu_departments_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:departments');

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('department:read', '查看组织', 'ACTION', @menu_departments_id, 'DEPARTMENT', NULL, 'READ', '查看部门树、部门详情和成员摘要', 1, 1, 13010, 'SYSTEM', 'SYSTEM'),
  ('department:create', '新建部门', 'ACTION', @menu_departments_id, 'DEPARTMENT', NULL, 'CREATE', '新建部门节点', 1, 1, 13020, 'SYSTEM', 'SYSTEM'),
  ('department:update', '编辑部门', 'ACTION', @menu_departments_id, 'DEPARTMENT', NULL, 'UPDATE', '编辑部门名称、说明、负责人和排序', 1, 1, 13030, 'SYSTEM', 'SYSTEM'),
  ('department:enable', '启停部门', 'ACTION', @menu_departments_id, 'DEPARTMENT', NULL, 'ENABLE', '启用或停用部门', 1, 1, 13040, 'SYSTEM', 'SYSTEM')
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
    'menu:departments',
    'department:read',
    'department:create',
    'department:update',
    'department:enable'
  )
ON DUPLICATE KEY UPDATE
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;
