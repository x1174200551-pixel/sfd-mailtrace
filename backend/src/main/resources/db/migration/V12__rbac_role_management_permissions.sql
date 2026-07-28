-- AUTH-S2.5-BE-01/02 role management menu and permissions

SET @menu_system_management_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:system_management');

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('menu:roles', '角色管理', 'MENU', @menu_system_management_id, 'ROLE', '角色管理', NULL, '角色管理页面入口', 1, 1, 54, 'SYSTEM', 'SYSTEM')
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

SET @menu_roles_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:roles');

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('role:read', '查看角色', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'READ', '查看角色列表、权限树和角色详情', 1, 1, 12010, 'SYSTEM', 'SYSTEM'),
  ('role:create', '新建角色', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'CREATE', '新建自定义角色', 1, 1, 12020, 'SYSTEM', 'SYSTEM'),
  ('role:update', '编辑角色', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'UPDATE', '编辑自定义角色基础信息', 1, 1, 12030, 'SYSTEM', 'SYSTEM'),
  ('role:enable', '启停角色', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'ENABLE', '启用或停用自定义角色', 1, 1, 12040, 'SYSTEM', 'SYSTEM'),
  ('role:permission_update', '配置角色权限', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'PERMISSION_UPDATE', '保存角色菜单权限、操作权限和默认数据范围', 1, 1, 12050, 'SYSTEM', 'SYSTEM')
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
    'menu:roles',
    'role:read',
    'role:create',
    'role:update',
    'role:enable',
    'role:permission_update'
  )
ON DUPLICATE KEY UPDATE
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;
