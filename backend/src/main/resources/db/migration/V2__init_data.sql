-- P1-W1-BE-01 初始化登录所需基础数据

INSERT INTO `mt_user` (
  `account`,
  `password_hash`,
  `display_name`,
  `email`,
  `role_code`,
  `is_enabled`,
  `created_by`,
  `updated_by`
) VALUES (
  'admin',
  '$2a$10$Z/c6q5zPa5L3Zz4ATiapEeGv/gYDsBxn17yPGyEcSMAGDsms7Nbp2',
  '系统管理员',
  'admin@ntn.fziot',
  'ADMIN',
  1,
  'SYSTEM',
  'SYSTEM'
) ON DUPLICATE KEY UPDATE
  `display_name` = VALUES(`display_name`),
  `email` = VALUES(`email`),
  `role_code` = VALUES(`role_code`),
  `is_enabled` = VALUES(`is_enabled`),
  `updated_by` = 'SYSTEM';

INSERT INTO `mt_sys_param` (
  `param_key`,
  `param_value`,
  `param_desc`,
  `created_by`,
  `updated_by`
) VALUES
  ('ticket.no.prefix', 'TCK', '工单号前缀', 'SYSTEM', 'SYSTEM'),
  ('auth.default.admin.account', 'admin', '默认管理员账号', 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
  `param_value` = VALUES(`param_value`),
  `param_desc` = VALUES(`param_desc`),
  `updated_by` = 'SYSTEM';

INSERT INTO `mt_notification_template` (
  `template_code`,
  `template_name`,
  `subject_tpl`,
  `content_tpl`,
  `is_enabled`,
  `created_by`,
  `updated_by`
) VALUES (
  'AUTO_REPLY',
  '自动回执模板',
  '您的工单已创建：{ticket_no}',
  '您好，您的邮件已进入工单系统，工单号：{ticket_no}。',
  1,
  'SYSTEM',
  'SYSTEM'
) ON DUPLICATE KEY UPDATE
  `template_name` = VALUES(`template_name`),
  `subject_tpl` = VALUES(`subject_tpl`),
  `content_tpl` = VALUES(`content_tpl`),
  `is_enabled` = VALUES(`is_enabled`),
  `updated_by` = 'SYSTEM';
