-- P1-W1-BE-09 编号规则默认参数

INSERT INTO `mt_sys_param` (
  `param_key`,
  `param_value`,
  `param_desc`,
  `created_by`,
  `updated_by`
) VALUES
  ('ticket.no.enabled', 'true', '工单编号规则启用状态', 'SYSTEM', 'SYSTEM'),
  ('ticket.no.date_format', 'yyyyMMdd', '工单号日期格式', 'SYSTEM', 'SYSTEM'),
  ('ticket.no.seq_length', '4', '工单号流水位数', 'SYSTEM', 'SYSTEM'),
  ('ticket.no.separator', '-', '工单号分隔符', 'SYSTEM', 'SYSTEM'),
  ('ticket.no.description', '客户来信自动建单时生成唯一工单号；邮件线程关联会优先匹配主题中的工单号。', '工单编号规则业务说明', 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
  `param_value` = VALUES(`param_value`),
  `param_desc` = VALUES(`param_desc`),
  `updated_by` = 'SYSTEM';
