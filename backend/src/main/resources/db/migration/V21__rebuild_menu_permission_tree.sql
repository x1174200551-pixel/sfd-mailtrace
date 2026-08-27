-- 重建 RBAC 菜单与操作权限基础数据。
-- 当前项目尚未生产，保留角色和用户关系，仅重建权限定义与角色权限关系。
-- 兼容 MySQL 5.7：不使用 CTE、窗口函数或 CHECK 约束。

DELETE FROM `mt_role_permission`;
DELETE FROM `mt_permission`;

-- 一级菜单：与当前前端侧边栏保持一致。
INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('menu:workspace', '我的工作', 'MENU', NULL, 'WORKSPACE', NULL, NULL, '一级菜单分组', 1, 1, 10, 'SYSTEM', 'SYSTEM'),
  ('menu:enterprise_config', '企业配置', 'MENU', NULL, 'ENTERPRISE', NULL, NULL, '一级菜单分组', 1, 1, 20, 'SYSTEM', 'SYSTEM'),
  ('menu:organization_permissions', '组织权限', 'MENU', NULL, 'ORGANIZATION', NULL, NULL, '一级菜单分组', 1, 1, 30, 'SYSTEM', 'SYSTEM'),
  ('menu:system_operations', '系统运维', 'MENU', NULL, 'OPERATIONS', NULL, NULL, '一级菜单分组', 1, 1, 40, 'SYSTEM', 'SYSTEM');

SET @menu_workspace_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:workspace' LIMIT 1);
SET @menu_enterprise_config_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:enterprise_config' LIMIT 1);
SET @menu_organization_permissions_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:organization_permissions' LIMIT 1);
SET @menu_system_operations_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:system_operations' LIMIT 1);

-- 二级菜单。
INSERT INTO `mt_permission` (
  `permission_code`, `permission_name`, `permission_type`, `parent_id`, `module_code`, `route_path`, `action_code`,
  `permission_desc`, `is_system`, `is_enabled`, `sort_order`, `created_by`, `updated_by`
) VALUES
  ('menu:dashboard', '工作台', 'MENU', @menu_workspace_id, 'WORKSPACE', '工作台', NULL, '工作台页面入口', 1, 1, 11, 'SYSTEM', 'SYSTEM'),
  ('menu:tickets', '全部工单', 'MENU', @menu_workspace_id, 'TICKET', '全部工单', NULL, '工单列表页面入口', 1, 1, 12, 'SYSTEM', 'SYSTEM'),
  ('menu:customers', '客户管理', 'MENU', @menu_workspace_id, 'CUSTOMER', '客户管理', NULL, '客户管理页面入口', 1, 1, 13, 'SYSTEM', 'SYSTEM'),

  ('menu:enterprises', '企业管理', 'MENU', @menu_enterprise_config_id, 'ENTERPRISE', '企业管理', NULL, '企业管理页面入口', 1, 1, 21, 'SYSTEM', 'SYSTEM'),
  ('menu:mailboxes', '邮箱配置', 'MENU', @menu_enterprise_config_id, 'MAILBOX', '邮箱配置', NULL, '邮箱配置页面入口', 1, 1, 22, 'SYSTEM', 'SYSTEM'),
  ('menu:notification_templates', '通知模板', 'MENU', @menu_enterprise_config_id, 'TEMPLATE', '通知模板', NULL, '通知模板页面入口', 1, 1, 23, 'SYSTEM', 'SYSTEM'),
  ('menu:sla_policies', 'SLA策略', 'MENU', @menu_enterprise_config_id, 'SLA', 'SLA策略', NULL, 'SLA策略页面入口', 1, 1, 24, 'SYSTEM', 'SYSTEM'),
  ('menu:assignment_rules', '分配规则', 'MENU', @menu_enterprise_config_id, 'ASSIGNMENT', '分配规则', NULL, '分配规则页面入口', 1, 1, 25, 'SYSTEM', 'SYSTEM'),
  ('menu:work_calendars', '工作日历', 'MENU', @menu_enterprise_config_id, 'CALENDAR', '工作日历', NULL, '工作日历页面入口', 1, 1, 26, 'SYSTEM', 'SYSTEM'),

  ('menu:departments', '组织管理', 'MENU', @menu_organization_permissions_id, 'DEPARTMENT', '组织管理', NULL, '组织管理页面入口', 1, 1, 31, 'SYSTEM', 'SYSTEM'),
  ('menu:users', '用户管理', 'MENU', @menu_organization_permissions_id, 'USER', '用户管理', NULL, '用户管理页面入口', 1, 1, 32, 'SYSTEM', 'SYSTEM'),
  ('menu:roles', '角色管理', 'MENU', @menu_organization_permissions_id, 'ROLE', '角色管理', NULL, '角色管理页面入口', 1, 1, 33, 'SYSTEM', 'SYSTEM'),

  ('menu:mail_fetch_logs', '收件记录', 'MENU', @menu_system_operations_id, 'MAIL', '收件记录', NULL, '收件记录页面入口', 1, 1, 41, 'SYSTEM', 'SYSTEM'),
  ('menu:mail_send_logs', '发件记录', 'MENU', @menu_system_operations_id, 'MAIL', '发件记录', NULL, '发件记录页面入口', 1, 1, 42, 'SYSTEM', 'SYSTEM'),
  ('menu:ticket_number_rule', '编号规则', 'MENU', @menu_system_operations_id, 'SYSTEM', '编号规则', NULL, '编号规则页面入口', 1, 1, 43, 'SYSTEM', 'SYSTEM');

SET @menu_dashboard_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:dashboard' LIMIT 1);
SET @menu_tickets_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:tickets' LIMIT 1);
SET @menu_customers_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:customers' LIMIT 1);
SET @menu_enterprises_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:enterprises' LIMIT 1);
SET @menu_mailboxes_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:mailboxes' LIMIT 1);
SET @menu_notification_templates_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:notification_templates' LIMIT 1);
SET @menu_sla_policies_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:sla_policies' LIMIT 1);
SET @menu_assignment_rules_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:assignment_rules' LIMIT 1);
SET @menu_work_calendars_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:work_calendars' LIMIT 1);
SET @menu_departments_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:departments' LIMIT 1);
SET @menu_users_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:users' LIMIT 1);
SET @menu_roles_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:roles' LIMIT 1);
SET @menu_mail_fetch_logs_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:mail_fetch_logs' LIMIT 1);
SET @menu_mail_send_logs_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:mail_send_logs' LIMIT 1);
SET @menu_ticket_number_rule_id = (SELECT `id` FROM `mt_permission` WHERE `permission_code` = 'menu:ticket_number_rule' LIMIT 1);

-- 三级操作权限：菜单仅控制入口，ACTION 控制后端业务操作。
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

  ('enterprise:read', '查看企业', 'ACTION', @menu_enterprises_id, 'ENTERPRISE', NULL, 'READ', '查看企业配置', 1, 1, 4010, 'SYSTEM', 'SYSTEM'),
  ('enterprise:create', '新建企业', 'ACTION', @menu_enterprises_id, 'ENTERPRISE', NULL, 'CREATE', '新建企业配置', 1, 1, 4020, 'SYSTEM', 'SYSTEM'),
  ('enterprise:update', '编辑企业', 'ACTION', @menu_enterprises_id, 'ENTERPRISE', NULL, 'UPDATE', '编辑企业配置', 1, 1, 4030, 'SYSTEM', 'SYSTEM'),
  ('enterprise:enable', '启停企业', 'ACTION', @menu_enterprises_id, 'ENTERPRISE', NULL, 'ENABLE', '启用或停用企业', 1, 1, 4040, 'SYSTEM', 'SYSTEM'),

  ('mailbox:read', '查看邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'READ', '查看邮箱配置', 1, 1, 5010, 'SYSTEM', 'SYSTEM'),
  ('mailbox:create', '新建邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'CREATE', '新建邮箱配置', 1, 1, 5020, 'SYSTEM', 'SYSTEM'),
  ('mailbox:update', '编辑邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'UPDATE', '编辑邮箱配置', 1, 1, 5030, 'SYSTEM', 'SYSTEM'),
  ('mailbox:enable', '启停邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'ENABLE', '启用或停用邮箱配置', 1, 1, 5040, 'SYSTEM', 'SYSTEM'),
  ('mailbox:delete', '删除邮箱配置', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'DELETE', '删除邮箱配置', 1, 1, 5050, 'SYSTEM', 'SYSTEM'),
  ('mailbox:test_connection', '测试邮箱连接', 'ACTION', @menu_mailboxes_id, 'MAILBOX', NULL, 'TEST_CONNECTION', '测试 IMAP/SMTP 连接', 1, 1, 5060, 'SYSTEM', 'SYSTEM'),

  ('notification_template:read', '查看通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'READ', '查看通知模板', 1, 1, 6010, 'SYSTEM', 'SYSTEM'),
  ('notification_template:create', '新建通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'CREATE', '新建通知模板', 1, 1, 6020, 'SYSTEM', 'SYSTEM'),
  ('notification_template:update', '编辑通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'UPDATE', '编辑通知模板', 1, 1, 6030, 'SYSTEM', 'SYSTEM'),
  ('notification_template:preview', '预览通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'PREVIEW', '预览通知模板渲染结果', 1, 1, 6040, 'SYSTEM', 'SYSTEM'),
  ('notification_template:delete', '删除通知模板', 'ACTION', @menu_notification_templates_id, 'TEMPLATE', NULL, 'DELETE', '删除未被邮箱引用的通知模板', 1, 1, 6050, 'SYSTEM', 'SYSTEM'),

  ('sla_policy:read', '查看 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'READ', '查看 SLA 策略', 1, 1, 7010, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:create', '新建 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'CREATE', '新建 SLA 策略', 1, 1, 7020, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:update', '编辑 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'UPDATE', '编辑 SLA 策略', 1, 1, 7030, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:enable', '启停 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'ENABLE', '启用或停用 SLA 策略', 1, 1, 7040, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:default', '设置默认 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'DEFAULT', '设置默认 SLA 策略', 1, 1, 7050, 'SYSTEM', 'SYSTEM'),
  ('sla_policy:delete', '删除 SLA 策略', 'ACTION', @menu_sla_policies_id, 'SLA', NULL, 'DELETE', '删除 SLA 策略', 1, 1, 7060, 'SYSTEM', 'SYSTEM'),

  ('assignment_rule:read', '查看分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'READ', '查看分配规则', 1, 1, 8010, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:create', '新建分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'CREATE', '新建分配规则', 1, 1, 8020, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:update', '编辑分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'UPDATE', '编辑分配规则', 1, 1, 8030, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:enable', '启停分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'ENABLE', '启用或停用分配规则', 1, 1, 8040, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:sort', '排序分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'SORT', '调整分配规则优先级', 1, 1, 8050, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:test_match', '测试分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'TEST_MATCH', '测试分配规则匹配结果', 1, 1, 8060, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule:delete', '删除分配规则', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'DELETE', '删除分配规则', 1, 1, 8070, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:read', '查看分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_READ', '查看企业分配规则组', 1, 1, 8110, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:create', '新建分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_CREATE', '新建企业分配规则组', 1, 1, 8120, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:update', '编辑分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_UPDATE', '编辑企业分配规则组', 1, 1, 8130, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:enable', '启停分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_ENABLE', '启用或停用企业分配规则组', 1, 1, 8140, 'SYSTEM', 'SYSTEM'),
  ('assignment_rule_group:delete', '删除分配规则组', 'ACTION', @menu_assignment_rules_id, 'ASSIGNMENT', NULL, 'GROUP_DELETE', '删除企业分配规则组', 1, 1, 8150, 'SYSTEM', 'SYSTEM'),

  ('work_calendar:read', '查看工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'READ', '查看工作日历', 1, 1, 9010, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:create', '新建工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'CREATE', '新建工作日历', 1, 1, 9020, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:update', '编辑工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'UPDATE', '编辑工作日历', 1, 1, 9030, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:default', '设置默认工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'DEFAULT', '设置默认工作日历', 1, 1, 9040, 'SYSTEM', 'SYSTEM'),
  ('work_calendar:delete', '删除工作日历', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'DELETE', '删除工作日历', 1, 1, 9050, 'SYSTEM', 'SYSTEM'),
  ('holiday:read', '查看节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_READ', '查看节假日', 1, 1, 9060, 'SYSTEM', 'SYSTEM'),
  ('holiday:import', '导入法定节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_IMPORT', '导入法定节假日模板', 1, 1, 9070, 'SYSTEM', 'SYSTEM'),
  ('holiday:create', '新建节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_CREATE', '新建节假日', 1, 1, 9080, 'SYSTEM', 'SYSTEM'),
  ('holiday:update', '编辑节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_UPDATE', '编辑节假日', 1, 1, 9090, 'SYSTEM', 'SYSTEM'),
  ('holiday:delete', '删除节假日', 'ACTION', @menu_work_calendars_id, 'CALENDAR', NULL, 'HOLIDAY_DELETE', '删除节假日', 1, 1, 9100, 'SYSTEM', 'SYSTEM'),

  ('department:read', '查看组织', 'ACTION', @menu_departments_id, 'DEPARTMENT', NULL, 'READ', '查看部门树、部门详情和成员摘要', 1, 1, 10010, 'SYSTEM', 'SYSTEM'),
  ('department:create', '新建部门', 'ACTION', @menu_departments_id, 'DEPARTMENT', NULL, 'CREATE', '新建部门节点', 1, 1, 10020, 'SYSTEM', 'SYSTEM'),
  ('department:update', '编辑部门', 'ACTION', @menu_departments_id, 'DEPARTMENT', NULL, 'UPDATE', '编辑部门及成员关系', 1, 1, 10030, 'SYSTEM', 'SYSTEM'),
  ('department:enable', '启停部门', 'ACTION', @menu_departments_id, 'DEPARTMENT', NULL, 'ENABLE', '启用或停用部门', 1, 1, 10040, 'SYSTEM', 'SYSTEM'),

  ('user:read', '查看用户', 'ACTION', @menu_users_id, 'USER', NULL, 'READ', '查看用户列表', 1, 1, 11010, 'SYSTEM', 'SYSTEM'),
  ('user:create', '新建用户', 'ACTION', @menu_users_id, 'USER', NULL, 'CREATE', '新建系统用户', 1, 1, 11020, 'SYSTEM', 'SYSTEM'),
  ('user:update', '编辑用户', 'ACTION', @menu_users_id, 'USER', NULL, 'UPDATE', '编辑系统用户资料和角色', 1, 1, 11030, 'SYSTEM', 'SYSTEM'),
  ('user:enable', '启停用户', 'ACTION', @menu_users_id, 'USER', NULL, 'ENABLE', '启用或停用用户', 1, 1, 11040, 'SYSTEM', 'SYSTEM'),
  ('user:reset_password', '重置密码', 'ACTION', @menu_users_id, 'USER', NULL, 'RESET_PASSWORD', '重置用户密码', 1, 1, 11050, 'SYSTEM', 'SYSTEM'),

  ('role:read', '查看角色', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'READ', '查看角色列表和权限树', 1, 1, 12010, 'SYSTEM', 'SYSTEM'),
  ('role:create', '新建角色', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'CREATE', '新建自定义角色', 1, 1, 12020, 'SYSTEM', 'SYSTEM'),
  ('role:update', '编辑角色', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'UPDATE', '编辑自定义角色', 1, 1, 12030, 'SYSTEM', 'SYSTEM'),
  ('role:enable', '启停角色', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'ENABLE', '启用或停用自定义角色', 1, 1, 12040, 'SYSTEM', 'SYSTEM'),
  ('role:permission_update', '配置角色权限', 'ACTION', @menu_roles_id, 'ROLE', NULL, 'PERMISSION_UPDATE', '保存角色菜单和操作权限', 1, 1, 12050, 'SYSTEM', 'SYSTEM'),

  ('mail_fetch_log:read', '查看收件记录', 'ACTION', @menu_mail_fetch_logs_id, 'MAIL', NULL, 'FETCH_LOG_READ', '查看收件记录', 1, 1, 13010, 'SYSTEM', 'SYSTEM'),
  ('mail_send_log:read', '查看发件记录', 'ACTION', @menu_mail_send_logs_id, 'MAIL', NULL, 'SEND_LOG_READ', '查看发件记录', 1, 1, 14010, 'SYSTEM', 'SYSTEM'),
  ('mail_send:test', '测试发送邮件', 'ACTION', @menu_mail_send_logs_id, 'MAIL', NULL, 'SEND_TEST', '发送测试邮件', 1, 1, 14020, 'SYSTEM', 'SYSTEM'),
  ('mail_send:retry', '重试发送邮件', 'ACTION', @menu_mail_send_logs_id, 'MAIL', NULL, 'SEND_RETRY', '重试失败邮件', 1, 1, 14030, 'SYSTEM', 'SYSTEM'),
  ('ticket_number_rule:read', '查看编号规则', 'ACTION', @menu_ticket_number_rule_id, 'SYSTEM', NULL, 'READ', '查看工单编号规则', 1, 1, 15010, 'SYSTEM', 'SYSTEM'),
  ('ticket_number_rule:preview', '预览编号规则', 'ACTION', @menu_ticket_number_rule_id, 'SYSTEM', NULL, 'PREVIEW', '预览工单编号规则', 1, 1, 15020, 'SYSTEM', 'SYSTEM'),
  ('ticket_number_rule:update', '编辑编号规则', 'ACTION', @menu_ticket_number_rule_id, 'SYSTEM', NULL, 'UPDATE', '编辑工单编号规则', 1, 1, 15030, 'SYSTEM', 'SYSTEM');

-- 当前开发阶段：所有现有角色拥有全部菜单与操作权限。
INSERT INTO `mt_role_permission` (`role_id`, `permission_id`, `created_by`, `updated_by`)
SELECT r.`id`, p.`id`, 'SYSTEM', 'SYSTEM'
FROM `mt_role` r
CROSS JOIN `mt_permission` p
WHERE r.`is_deleted` = 0
  AND p.`is_deleted` = 0
  AND p.`is_enabled` = 1;
