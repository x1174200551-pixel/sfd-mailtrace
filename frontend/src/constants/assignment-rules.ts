import type { AssignmentRule, AssignmentRuleFormState, AssignmentRuleTestForm } from '../types/assignment-rule'

export const emptyAssignmentRuleForm: AssignmentRuleFormState = {
  id: null,
  ruleName: '',
  enabled: true,
  priorityOrder: 100,
  defaultRule: false,
  matchType: 'SUBJECT_KEYWORD',
  matchValue: '',
  assigneeId: '',
  notifyEnabled: true,
}

export const emptyAssignmentRuleTestForm: AssignmentRuleTestForm = {
  mailboxId: '',
  subject: '',
  fromEmail: '',
}

export function assignmentMatchTypeLabel(value: string | null) {
  return ({
    DEFAULT: '默认兜底',
    SUBJECT_KEYWORD: '主题关键词',
    MAILBOX: '来源邮箱',
    FROM_EMAIL: '客户邮箱',
  } as Record<string, string>)[value || ''] || value || '-'
}

export function assignmentRuleText(rule: AssignmentRule) {
  if (rule.matchType === 'DEFAULT') return 'DEFAULT · 未命中其他规则时兜底'
  return `${assignmentMatchTypeLabel(rule.matchType)} = ${rule.matchValue || '-'}`
}
