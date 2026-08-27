import { requestApi } from '../shared/api/request'
import type {
  AssignmentRule,
  AssignmentRuleGroup,
  AssignmentRuleGroupListResponse,
  AssignmentRuleListResponse,
  AssignmentRuleMatchResponse,
  AssignmentRuleMatchType,
} from '../types/assignment-rule'

export type AssignmentRuleQuery = {
  groupId?: number
  enabled?: string
  keyword?: string
  matchType?: string
}

export type AssignmentRulePayload = {
  groupId: number
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

export type AssignmentRuleGroupPayload = {
  enterpriseId: number
  groupName: string
  enabled: boolean
  remark: string
}

export const assignmentRuleGroupApi = {
  list(params: { enterpriseId?: number; keyword?: string; enabled?: boolean } = {}) {
    return requestApi<AssignmentRuleGroupListResponse>(`/api/v1/assignment-rule-groups${toQuery(params)}`)
  },

  options(enterpriseId?: number, enabled = true) {
    return requestApi<AssignmentRuleGroup[]>(`/api/v1/assignment-rule-groups/options${toQuery({ enterpriseId, enabled })}`)
  },

  save(groupId: number | null, payload: AssignmentRuleGroupPayload) {
    return requestApi<AssignmentRuleGroup>(
      groupId ? `/api/v1/assignment-rule-groups/${groupId}` : '/api/v1/assignment-rule-groups',
      { method: groupId ? 'PUT' : 'POST', body: JSON.stringify(payload) },
    )
  },

  setEnabled(groupId: number, enabled: boolean) {
    return requestApi<AssignmentRuleGroup>(`/api/v1/assignment-rule-groups/${groupId}/enabled`, {
      method: 'PATCH', body: JSON.stringify({ enabled }),
    })
  },

  delete(groupId: number) {
    return requestApi<void>(`/api/v1/assignment-rule-groups/${groupId}`, { method: 'DELETE' })
  },
}
