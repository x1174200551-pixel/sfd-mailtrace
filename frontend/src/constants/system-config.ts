import type { SystemGroup, TicketRuleFormState } from '../types/system-config'

export const emptyTicketRuleForm: TicketRuleFormState = {
  enabled: true,
  prefix: 'TCK',
  dateFormat: 'yyyyMMdd',
  seqLength: 4,
  separator: '-',
  description: '客户来信自动建单时生成唯一工单号；邮件线程关联会优先匹配主题中的工单号。',
}

export const systemGroups: SystemGroup[] = [
  {
    key: 'ticket',
    title: '工单编号规则',
    summary: '前缀、日期格式、流水位数',
    detail: '业务人员可在此维护工单号生成规则，保存后仅影响后续新建工单。',
    owner: '业务可配',
  },
  {
    key: 'mail',
    title: '邮件处理策略',
    summary: '重试次数、拉取间隔等由管理员维护',
    detail: '邮件轮询、重试、附件限制等参数会影响后台任务稳定性，当前页面仅展示边界，不开放业务录入。',
    owner: '运维维护',
  },
  {
    key: 'notice',
    title: '通知与提醒',
    summary: 'SLA 预警、分配通知开关',
    detail: '通知内容已在通知模板中维护，提醒策略后续会随 SLA 模块单独设计，不在编号规则页混合编辑。',
    owner: '后续设计',
  },
  {
    key: 'security',
    title: '安全与审计',
    summary: '会话超时、操作日志保留',
    detail: '会话、安全和审计保留属于系统级策略，后续按管理员能力单独设计，不由业务人员直接填写参数。',
    owner: '管理员维护',
  },
]
