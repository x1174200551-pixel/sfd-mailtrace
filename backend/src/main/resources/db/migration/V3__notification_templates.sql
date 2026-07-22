-- P1-W1-BE-08 初始化通知模板完整场景

INSERT INTO `mt_notification_template` (
  `template_code`,
  `template_name`,
  `subject_tpl`,
  `content_tpl`,
  `is_enabled`,
  `created_by`,
  `updated_by`
) VALUES
  (
    'AUTO_REPLY',
    '自动回执模板',
    '您的工单已创建：{ticket_no}',
    '您好，您的邮件已进入工单系统。\n\n工单号：{ticket_no}\n工单主题：{subject}\n当前处理人：{assignee_name}\n\n我们会尽快处理并回复您。',
    1,
    'SYSTEM',
    'SYSTEM'
  ),
  (
    'ASSIGN_NOTIFY',
    '分配通知模板',
    '新工单待处理：{ticket_no}',
    '您好，您有一个新分配工单。\n\n工单号：{ticket_no}\n主题：{subject}\n客户：{customer_email}\n请及时处理：{ticket_link}',
    1,
    'SYSTEM',
    'SYSTEM'
  ),
  (
    'AGENT_REPLY',
    '处理人回复模板',
    '关于工单 {ticket_no} 的回复',
    '您好，关于您的工单 {ticket_no}，我们回复如下：\n\n',
    1,
    'SYSTEM',
    'SYSTEM'
  ),
  (
    'SLA_WARNING',
    'SLA 预警模板',
    'SLA 即将超时：{ticket_no}',
    '工单 {ticket_no} 即将在 {sla_deadline} 超时，请尽快处理。\n\n工单主题：{subject}\n处理人：{assignee_name}',
    1,
    'SYSTEM',
    'SYSTEM'
  ),
  (
    'SLA_BREACH',
    'SLA 超时模板',
    'SLA 已超时：{ticket_no}',
    '工单 {ticket_no} 已超过 SLA 时限，请管理员关注并升级处理。\n\n客户：{customer_email}\n链接：{ticket_link}',
    0,
    'SYSTEM',
    'SYSTEM'
  )
ON DUPLICATE KEY UPDATE
  `template_name` = VALUES(`template_name`),
  `subject_tpl` = VALUES(`subject_tpl`),
  `content_tpl` = VALUES(`content_tpl`),
  `is_enabled` = VALUES(`is_enabled`),
  `updated_by` = 'SYSTEM';
