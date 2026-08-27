export type AssignmentRuleMatchType = 'DEFAULT' | 'SUBJECT_KEYWORD' | 'MAILBOX' | 'FROM_EMAIL'

export type AssignmentRule = {
  id: number
  groupId: number
  ruleName: string
  enabled: boolean
  priorityOrder: number
  defaultRule: boolean
  matchType: AssignmentRuleMatchType
  matchValue: string | null
  assigneeId: number
  assigneeName: string | null
  notifyEnabled: boolean
  createdAt: string | null
  updatedAt: string | null
}

export type AssignmentRuleSummary = {
  totalCount: number
  enabledCount: number
  disabledCount: number
  defaultCount: number
}

export type AssignmentRuleListResponse = {
  records: AssignmentRule[]
  summary: AssignmentRuleSummary
}

export type AssignmentRuleFormState = {
  id: number | null
  groupId: string
  ruleName: string
  enabled: boolean
  priorityOrder: number
  defaultRule: boolean
  matchType: AssignmentRuleMatchType
  matchValue: string
  assigneeId: string
  notifyEnabled: boolean
}

export type AssignmentRuleTestForm = {
  mailboxId: string
  subject: string
  fromEmail: string
}

export type AssignmentRuleMatchResponse = {
  matched: boolean
  ruleId: number | null
  ruleName: string | null
  matchType: AssignmentRuleMatchType | null
  matchValue: string | null
  assigneeId: number | null
  assigneeName: string | null
  notifyEnabled: boolean | null
}

export type AssignmentRuleConfirmAction = {
  type: 'delete'
  rule: AssignmentRule
} | null

export type AssignmentRuleGroup = {
  id: number
  enterpriseId: number
  groupName: string
  enabled: boolean
  remark: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type AssignmentRuleGroupListResponse = {
  records: AssignmentRuleGroup[]
  totalCount: number
  enabledCount: number
  disabledCount: number
}

export type AssignmentRuleGroupFormState = {
  id: number | null
  enterpriseId: string
  groupName: string
  enabled: boolean
  remark: string
}
