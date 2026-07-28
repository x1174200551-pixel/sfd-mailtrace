-- AUTH-S2-DB-02 RBAC initial roles, permissions and data scopes

INSERT INTO `mt_role` (
  `role_code`, `role_name`, `role_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('ADMIN', '系统管理员', '拥有全部菜单、动作和数据范围权限', 1, 1, 10, 'SYSTEM', 'SYSTEM'),
  ('AGENT', '客服处理人', '可处理自己负责和未分配池内的工单，查看相关客户与工作台数据', 1, 1, 20, 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `role_desc` = VALUES(`role_desc`),
  `is_system` = VALUES(`is_system`),
  `is_enabled` = VALUES(`is_enabled`),
  `sort_order` = VALUES(`sort_order`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('menu:workspace', '工作空间', 'MENU', NULL, 'WORKSPACE', NULL, NULL, '一级菜单分组', 1, 1, 10, 'SYSTEM', 'SYSTEM'),
  ('menu:ticket_center', '工单中心', 'MENU', NULL, 'TICKET', NULL, NULL, '一级菜单分组', 1, 1, 20, 'SYSTEM', 'SYSTEM'),
  ('menu:mail_management', '邮件管理', 'MENU', NULL, 'MAIL', NULL, NULL, '一级菜单分组', 1, 1, 30, 'SYSTEM', 'SYSTEM'),
  ('menu:sla_management', 'SLA管理', 'MENU', NULL, 'SLA', NULL, NULL, '一级菜单分组', 1, 1, 40, 'SYSTEM', 'SYSTEM'),
  ('menu:system_management', '系统管理', 'MENU', NULL, 'SYSTEM', NULL, NULL, '一级菜单分组', 1, 1, 50, 'SYSTEM', 'SYSTEM')
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

SET @menu_workspace_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:workspace');
SET @menu_ticket_center_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:ticket_center');
SET @menu_mail_management_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:mail_management');
SET @menu_sla_management_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:sla_management');
SET @menu_system_management_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:system_management');

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('menu:dashboard', '工作台', 'MENU', @menu_workspace_id, 'WORKSPACE', '工作台', NULL, '工作台页面入口', 1, 1, 11, 'SYSTEM', 'SYSTEM'),
  ('menu:tickets', '全部工单', 'MENU', @menu_ticket_center_id, 'TICKET', '全部工单', NULL, '工单列表页面入口', 1, 1, 21, 'SYSTEM', 'SYSTEM'),
  ('menu:customers', '客户管理', 'MENU', @menu_ticket_center_id, 'CUSTOMER', '客户管理', NULL, '客户只读页面入口', 1, 1, 22, 'SYSTEM', 'SYSTEM'),
  ('menu:mailboxes', '邮箱配置', 'MENU', @menu_mail_management_id, 'MAILBOX', '邮箱配置', NULL, '邮箱配置页面入口', 1, 1, 31, 'SYSTEM', 'SYSTEM'),
  ('menu:mail_fetch_logs', '收件记录', 'MENU', @menu_mail_management_id, 'MAIL', '收件记录', NULL, '收件记录页面入口', 1, 1, 32, 'SYSTEM', 'SYSTEM'),
  ('menu:mail_send_logs', '发件记录', 'MENU', @menu_mail_management_id, 'MAIL', '发件记录', NULL, '发件记录页面入口', 1, 1, 33, 'SYSTEM', 'SYSTEM'),
  ('menu:assignment_rules', '分配规则', 'MENU', @menu_sla_management_id, 'ASSIGNMENT', '分配规则', NULL, '分配规则页面入口', 1, 1, 41, 'SYSTEM', 'SYSTEM'),
  ('menu:sla_policies', 'SLA策略', 'MENU', @menu_sla_management_id, 'SLA', 'SLA策略', NULL, 'SLA策略页面入口', 1, 1, 42, 'SYSTEM', 'SYSTEM'),
  ('menu:work_calendars', '工作日历', 'MENU', @menu_sla_management_id, 'CALENDAR', '工作日历', NULL, '工作日历页面入口', 1, 1, 43, 'SYSTEM', 'SYSTEM'),
  ('menu:users', '用户管理', 'MENU', @menu_system_management_id, 'USER', '用户管理', NULL, '用户管理页面入口', 1, 1, 51, 'SYSTEM', 'SYSTEM'),
  ('menu:ticket_number_rule', '编号规则', 'MENU', @menu_system_management_id, 'SYSTEM', '编号规则', NULL, '编号规则页面入口', 1, 1, 52, 'SYSTEM', 'SYSTEM'),
  ('menu:notification_templates', '通知模板', 'MENU', @menu_system_management_id, 'TEMPLATE', '通知模板', NULL, '通知模板页面入口', 1, 1, 53, 'SYSTEM', 'SYSTEM')
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

SET @menu_dashboard_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:dashboard');
SET @menu_tickets_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:tickets');
SET @menu_customers_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:customers');
SET @menu_users_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:users');
SET @menu_mailboxes_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:mailboxes');
SET @menu_mail_fetch_logs_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:mail_fetch_logs');
SET @menu_mail_send_logs_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:mail_send_logs');
SET @menu_assignment_rules_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:assignment_rules');
SET @menu_sla_policies_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:sla_policies');
SET @menu_work_calendars_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:work_calendars');
SET @menu_notification_templates_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:notification_templates');
SET @menu_ticket_number_rule_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:ticket_number_rule');

INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('dashboard:read', '查看工作台', 'ACTION', @menu_dashboard_id, 'WORKSPACE', NULL, 'READ', '查看工作台统计和待办', 1, 1, 1010, 'SYSTEM', 'SYSTEM'),
  ('ticket:read', '查看工单', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'READ', '查看工单列表、统计和详情', 1, 1, 2010, 'SYSTEM', 'SYSTEM'),
  ('ticket:claim', '领取工单', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'CLAIM', '领取未分配工单', 1, 1, 2020, 'SYSTEM', 'SYSTEM'),
  ('ticket:reply', '回复客户', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'REPLY', '对外回复客户邮件', 1, 1, 2030, 'SYSTEM', 'SYSTEM'),
  ('ticket:note', '内部备注', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'NOTE', '添加内部备注', 1, 1, 2040, 'SYSTEM', 'SYSTEM'),
  ('ticket:assign', '转派工单', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'ASSIGN', '调整工单处理人', 1, 1, 2050, 'SYSTEM', 'SYSTEM'),
  ('ticket:close', '关闭工单', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'CLOSE', '关闭工单', 1, 1, 2060, 'SYSTEM', 'SYSTEM'),
  ('ticket:update_status', '变更工单状态', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'UPDATE_STATUS', '手动变更工单状态', 1, 1, 2070, 'SYSTEM', 'SYSTEM'),
  ('ticket:update_priority', '变更工单优先级', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'UPDATE_PRIORITY', '手动变更工单优先级', 1, 1, 2080, 'SYSTEM', 'SYSTEM'),
  ('ticket:update_remark', '编辑工单备注', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'UPDATE_REMARK', '编辑工单备注字段', 1, 1, 2090, 'SYSTEM', 'SYSTEM'),
  ('ticket_attachment:read', '查看工单附件', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'ATTACHMENT_READ', '查看工单附件列表', 1, 1, 2110, 'SYSTEM', 'SYSTEM'),
  ('ticket_attachment:upload', '上传工单附件', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'ATTACHMENT_UPLOAD', '上传工单附件', 1, 1, 2120, 'SYSTEM', 'SYSTEM'),
  ('ticket_attachment:download', '下载工单附件', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'ATTACHMENT_DOWNLOAD', '下载工单附件', 1, 1, 2130, 'SYSTEM', 'SYSTEM'),
  ('ticket_attachment:delete', '删除工单附件', 'ACTION', @menu_tickets_id, 'TICKET', NULL, 'ATTACHMENT_DELETE', '删除工单附件', 1, 1, 2140, 'SYSTEM', 'SYSTEM'),
  ('customer:read', '查看客户', 'ACTION', @menu_customers_id, 'CUSTOMER', NULL, 'READ', '查看可见工单关联客户', 1, 1, 3010, 'SYSTEM', 'SYSTEM'),
  ('user:read', '查看用户', 'ACTION', @menu_users_id, 'USER', NULL, 'READ', '查看用户列表', 1, 1, 4010, 'SYSTEM', 'SYSTEM'),
  ('user:create', '新建用户', 'ACTION', @menu_users_id, 'USER', NULL, 'CREATE', '新建系统用户', 1, 1, 4020, 'SYSTEM', 'SYSTEM'),
  ('user:update', '编辑用户', 'ACTION', @menu_users_id, 'USER', NULL, 'UPDATE', '编辑系统用户资料和角色', 1, 1, 4030, 'SYSTEM', 'SYSTEM'),
  ('user:enable', '启停用户', 'ACTION', @menu_users_id, 'USER', NULL, 'ENABLE', '启用或停用用户', 1, 1, 4040, 'SYSTEM', 'SYSTEM'),
  ('user:reset_password', '重置密码', 'ACTION', @menu_users_id, 'USER', NULL, 'RESET_PASSWORD', '重置用户密码', 1, 1, 4050, 'SYSTEM', 'SYSTEM'),
  ('mailbox:read', '查看邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'READ', '查看邮箱配置', 1, 1, 5010, 'SYSTEM', 'SYSTEM'),
  ('mailbox:create', '新建邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'CREATE', '新建邮箱配置', 1, 1, 5020, 'SYSTEM', 'SYSTEM'),
  ('mailbox:update', '编辑邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'UPDATE', '编辑邮箱配置', 1, 1, 5030, 'SYSTEM', 'SYSTEM'),
  ('mailbox:enable', '启停邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'ENABLE', '启用或停用邮箱配置', 1, 1, 5040, 'SYSTEM', 'SYSTEM'),
  ('mailbox:delete', '删除邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'DELETE', '删除邮箱配置', 1, 1, 5050, 'SYSTEM', 'SYSTEM'),
  ('mailbox:test_connection', '测试邮箱连接', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'TEST_CONNECTION', '测试 IMAP/SMTP 连接', 1, 1, 5060, 'SYSTEM', 'SYSTEM'),
  ('mail_fetch_log:read', '查看收件记录', 'ACTION', @menu_mail_fetch_logs_id, 'MAIL', NULL, 'FETCH_LOG_READ', '查看收件记录', 1, 1, 6010, 'SYSTEM', 'SYSTEM'),
  ('mail_send_log:read', '查看发件记录', 'ACTION', @menu_mail_send_logs_id, 'MAIL', NULL, 'SEND_LOG_READ', '查看发件记录', 1, 1, 6020, 'SYSTEM', 'SYSTEM'),
  ('mail_send:test', '测试发送邮件', 'ACTION', @menu_mail_send_logs_id, 'MAIL', NULL, 'SEND_TEST', '发送测试邮件', 1, 1, 6030, 'SYSTEM', 'SYSTEM'),
  ('mail_send:retry', '重试发送邮件', 'ACTION', @menu_mail_send_logs_id, 'MAIL', NULL, 'SEND_RETRY', '重试失败邮件', 1, 1, 6040, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:read', '查看分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'READ', '查看分配规则', 1, 1, 7010, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:create', '新建分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'CREATE', '新建分配规则', 1, 1, 7020, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:update', '编辑分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'UPDATE', '编辑分配规则', 1, 1, 7030, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:enable', '启停分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'ENABLE', '启用或停用分配规则', 1, 1, 7040, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:sort', '排序分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'SORT', '调整分配规则优先级', 1, 1, 7050, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:test_match', '测试分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'TEST_MATCH', '测试分配规则匹配结果', 1, 1, 7060, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:delete', '删除分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'DELETE', '删除分配规则', 1, 1, 7070, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:read', '查看 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'READ', '查看 SLA 策略', 1, 1, 8010, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:create', '新建 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'CREATE', '新建 SLA 策略', 1, 1, 8020, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:update', '编辑 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'UPDATE', '编辑 SLA 策略', 1, 1, 8030, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:enable', '启停 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'ENABLE', '启用或停用 SLA 策略', 1, 1, 8040, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:default', '设置默认 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'DEFAULT', '设置默认 SLA 策略', 1, 1, 8050, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:delete', '删除 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'DELETE', '删除 SLA 策略', 1, 1, 8060, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:read', '查看工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'READ', '查看工作日历', 1, 1, 9010, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:create', '新建工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'CREATE', '新建工作日历', 1, 1, 9020, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:update', '编辑工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'UPDATE', '编辑工作日历', 1, 1, 9030, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:default', '设置默认工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'DEFAULT', '设置默认工作日历', 1, 1, 9040, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:delete', '删除工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'DELETE', '删除工作日历', 1, 1, 9050, 'SYSTEM', 'SYSTEM'),
  ('holiday:read', '查看节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_READ', '查看节假日', 1, 1, 9060, 'SYSTEM', 'SYSTEM'),
  ('holiday:import', '导入法定节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_IMPORT', '从三方模板导入法定节假日', 1, 1, 9070, 'SYSTEM', 'SYSTEM'),
  ('holiday:create', '新建节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_CREATE', '新建节假日', 1, 1, 9080, 'SYSTEM', 'SYSTEM'),
  ('holiday:update', '编辑节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_UPDATE', '编辑节假日', 1, 1, 9090, 'SYSTEM', 'SYSTEM'),
  ('holiday:delete', '删除节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_DELETE', '删除节假日', 1, 1, 9100, 'SYSTEM', 'SYSTEM'),
  ('notification_template:read', '查看通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'READ', '查看通知模板', 1, 1, 10010, 'SYSTEM', 'SYSTEM'),
  ('notification_template:create', '新建通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'CREATE', '新建通知模板', 1, 1, 10020, 'SYSTEM', 'SYSTEM'),
  ('notification_template:update', '编辑通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'UPDATE', '编辑通知模板', 1, 1, 10030, 'SYSTEM', 'SYSTEM'),
  ('notification_template:preview', '预览通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'PREVIEW', '预览通知模板渲染结果', 1, 1, 10040, 'SYSTEM', 'SYSTEM'),
  ('ticket_number_rule:read', '查看编号规则', 'ACTION', @menu_ticket_number_rule_id, 'SYSTEM', NULL, 'READ', '查看工单编号规则', 1, 1, 11010, 'SYSTEM', 'SYSTEM'),
  ('ticket_number_rule:preview', '预览编号规则', 'ACTION', @menu_ticket_number_rule_id, 'SYSTEM', NULL, 'PREVIEW', '预览工单编号规则', 1, 1, 11020, 'SYSTEM', 'SYSTEM'),
  ('ticket_number_rule:update', '编辑编号规则', 'ACTION', @menu_ticket_number_rule_id, 'SYSTEM', NULL, 'UPDATE', '编辑工单编号规则', 1, 1, 11030, 'SYSTEM', 'SYSTEM')
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
ON DUPLICATE KEY UPDATE
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

INSERT INTO `mt_role_permission` (`role_id`, `permission_id`, `created_by`, `updated_by`)
SELECT r.`id`, p.`id`, 'SYSTEM', 'SYSTEM'
FROM `mt_role` r
JOIN `mt_permission` p
WHERE r.`role_code` = 'AGENT'
  AND r.`is_deleted` = 0
  AND p.`is_deleted` = 0
  AND p.`permission_code` IN (
    'menu:workspace',
    'menu:dashboard',
    'menu:ticket_center',
    'menu:tickets',
    'menu:customers',
    'dashboard:read',
    'ticket:read',
    'ticket:claim',
    'ticket:reply',
    'ticket:note',
    'ticket:assign',
    'ticket:close',
    'ticket:update_status',
    'ticket:update_priority',
    'ticket:update_remark',
    'ticket_attachment:read',
    'ticket_attachment:upload',
    'ticket_attachment:download',
    'ticket_attachment:delete',
    'customer:read'
  )
ON DUPLICATE KEY UPDATE
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

INSERT INTO `mt_user_role` (`user_id`, `role_id`, `is_primary`, `created_by`, `updated_by`)
SELECT u.`id`, r.`id`, 1, 'SYSTEM', 'SYSTEM'
FROM `mt_user` u
JOIN `mt_role` r ON r.`role_code` = u.`role_code`
WHERE u.`is_deleted` = 0
  AND r.`is_deleted` = 0
ON DUPLICATE KEY UPDATE
  `is_primary` = 1,
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;

INSERT INTO `mt_role_data_scope` (`role_id`, `resource_type`, `scope_code`, `scope_desc`, `created_by`, `updated_by`)
SELECT r.`id`, scope_seed.`resource_type`, scope_seed.`scope_code`, scope_seed.`scope_desc`, 'SYSTEM', 'SYSTEM'
FROM `mt_role` r
JOIN (
  SELECT 'ADMIN' AS `role_code`, 'TICKET' AS `resource_type`, 'ALL' AS `scope_code`, '全部工单数据' AS `scope_desc`
  UNION ALL SELECT 'ADMIN', 'CUSTOMER', 'ALL', '全部客户聚合数据'
  UNION ALL SELECT 'ADMIN', 'DASHBOARD', 'ALL', '全部工作台统计数据'
  UNION ALL SELECT 'AGENT', 'TICKET', 'SELF', '自己负责工单 + 未分配池'
  UNION ALL SELECT 'AGENT', 'CUSTOMER', 'SELF', '自己可见工单关联客户'
  UNION ALL SELECT 'AGENT', 'DASHBOARD', 'SELF', '自己负责工单 + 未分配池统计'
) scope_seed ON scope_seed.`role_code` = r.`role_code`
WHERE r.`is_deleted` = 0
ON DUPLICATE KEY UPDATE
  `scope_desc` = VALUES(`scope_desc`),
  `updated_by` = 'SYSTEM',
  `is_deleted` = 0;
