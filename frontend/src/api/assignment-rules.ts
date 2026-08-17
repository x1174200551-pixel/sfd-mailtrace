import { requestApi } from '../shared/api/request'
import type {
  AssignmentRule,
  AssignmentRuleListResponse,
  AssignmentRuleMatchResponse,
  AssignmentRuleMatchType,
} from '../types/assignment-rule'

export type AssignmentRuleQuery = {
  enabled?: string
  keyword?: string
  matchType?: string
}

export type AssignmentRulePayload = {
  assigneeId: number | null
  defaultRule: boolean
  enabled: boolean
  matchType: AssignmentRuleMatchType
  matchValue: string
  notifyEnabled: boolean
  priorityOrder: number
  ruleName: string
}

export type AssignmentRuleSortItem = {
  id: number
  priorityOrder: number
}

export type AssignmentRuleTestPayload = {
  fromEmail: string
  mailboxAddress: string
  mailboxId: number | null
  subject: string
}

function toQuery(params: Record<string, string | number | boolean | null | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, String(value))
    }
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export const assignmentRuleApi = {
  list(params: AssignmentRuleQuery = {}) {
    return requestApi<AssignmentRuleListResponse>(`/api/v1/assignment-rules${toQuery(params)}`)
  },

  save(ruleId: number | null, payload: AssignmentRulePayload) {
    return requestApi<AssignmentRule>(
      ruleId ? `/api/v1/assignment-rules/${ruleId}` : '/api/v1/assignment-rules',
      {
        method: ruleId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      },
    )
  },

  setEnabled(ruleId: number, enabled: boolean) {
    return requestApi<AssignmentRule>(`/api/v1/assignment-rules/${ruleId}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },

  sort(rules: AssignmentRuleSortItem[]) {
    return requestApi<AssignmentRule[]>('/api/v1/assignment-rules/sort', {
      method: 'PUT',
      body: JSON.stringify({ rules }),
    })
  },

  testMatch(payload: AssignmentRuleTestPayload) {
    return requestApi<AssignmentRuleMatchResponse>('/api/v1/assignment-rules/test-match', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  delete(ruleId: number) {
    return requestApi<void>(`/api/v1/assignment-rules/${ruleId}`, {
      method: 'DELETE',
    })
  },
}
