-- AUTH-S3-BE-03 supervisor role init
-- 启用 SUPERVISOR 角色、默认菜单/动作权限和部门数据范围配置种子

INSERT INTO `mt_role` (
  `role_code`, `role_name`, `role_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('SUPERVISOR', '部门主管', '可查看和管理自己部门及下级部门的工单、客户与工作台数据', 1, 1, 15, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `role_desc` = VALUES(`role_desc`),
  `is_system` = VALUES(`is_system`),
  `is_enabled` = VALUES(`is_enabled`),
  `sort_order` = VALUES(`sort_order`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

-- SUPERVISOR 菜单权限（工作台 + 工单中心 + 客户 + 组织只读）
INSERT INTO `mt_role_permission` (`role_id`, `permission_id`, `created_by`, `updated_by`)
SELECT r.`id`, p.`id`, 'SYSTEM', 'SYSTEM'
FROM `mt_role` r
JOIN `mt_permission` p
WHERE r.`role_code` = 'SUPERVISOR'
  AND r.`is_deleted` = 0
  AND p.`is_deleted` = 0
  AND p.`permission_code` IN (
    -- 一级菜单
    'menu:workspace',
    'menu:ticket_center',
    'menu:mail_management',
    -- 二级菜单
    'menu:dashboard',
    'menu:tickets',
    'menu:customers',
    'menu:mailboxes',
    'menu:mail_fetch_logs',
    'menu:mail_send_logs',
    -- 动作权限：工作台
    'dashboard:read',
    -- 动作权限：工单操作（同 AGENT 范围）
    'ticket:read',
    'ticket:claim',
    'ticket:reply',
    'ticket:note',
    'ticket:assign',
    'ticket:close',
    'ticket:update_status',
    'ticket:update_priority',
    'ticket:update_remark',
    -- 动作权限：附件
    'ticket_attachment:read',
    'ticket_attachment:upload',
    'ticket_attachment:download',
    'ticket_attachment:delete',
    -- 动作权限：客户
    'customer:read',
    -- 动作权限：邮件管理只读
    'mailbox:read',
    'mail_fetch_log:read',
    'mail_send_log:read'
  )
ON DUPLICATE KEY UPDATE
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

-- SUPERVISOR 数据范围：部门及下级
INSERT INTO `mt_role_data_scope` (`role_id`, `resource_type`, `scope_code`, `scope_desc`, `created_by`, `updated_by`)
SELECT r.`id`, scope_seed.`resource_type`, scope_seed.`scope_code`, scope_seed.`scope_desc`, 'SYSTEM', 'SYSTEM'
FROM `mt_role` r
JOIN (
  SELECT 'SUPERVISOR' AS `role_code`, 'TICKET'    AS `resource_type`, 'DEPT_AND_CHILDREN' AS `scope_code`, '部门及下级工单数据' AS `scope_desc`
  UNION ALL SELECT 'SUPERVISOR', 'CUSTOMER',  'DEPT_AND_CHILDREN', '部门及下级工单关联客户聚合数据'
  UNION ALL SELECT 'SUPERVISOR', 'DASHBOARD', 'DEPT_AND_CHILDREN', '部门及下级工单统计'
) scope_seed ON scope_seed.`role_code` = r.`role_code`
WHERE r.`is_deleted` = 0
ON DUPLICATE KEY UPDATE
  `scope_desc` = VALUES(`scope_desc`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;
