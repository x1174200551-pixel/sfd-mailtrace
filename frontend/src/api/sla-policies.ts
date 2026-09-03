import { requestApi } from '../shared/api/request'
import type { SlaPolicy, SlaPolicyListResponse } from '../types/sla-policy'

export type SlaPolicyQuery = {
  enterpriseId?: number
  defaultPolicy?: string
  enabled?: string
  keyword?: string
}

export type SlaPolicyPayload = {
  enterpriseId: number
  calendarId: number | null
  defaultPolicy: boolean
  enabled: boolean
  escalateAfterBreachHours: number | null
  responseWarningNotifyEnabled: boolean
  responseBreachNotifyEnabled: boolean
  responseEscalationNotifyEnabled: boolean
  resolveWarningNotifyEnabled: boolean
  resolveBreachNotifyEnabled: boolean
  resolveEscalationNotifyEnabled: boolean
  policyName: string
  resolveHours: number | null
  responseHours: number
  warningRemainHours: number
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

export const slaPolicyApi = {
  list(params: SlaPolicyQuery = {}) {
    return requestApi<SlaPolicyListResponse>(`/api/v1/sla-policies${toQuery(params)}`)
  },

  save(policyId: number | null, payload: SlaPolicyPayload) {
    return requestApi<SlaPolicy>(policyId ? `/api/v1/sla-policies/${policyId}` : '/api/v1/sla-policies', {
      method: policyId ? 'PUT' : 'POST',
      body: JSON.stringify(payload),
    })
  },

  setEnabled(policyId: number, enabled: boolean) {
    return requestApi<SlaPolicy>(`/api/v1/sla-policies/${policyId}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },

  setDefault(policyId: number) {
    return requestApi<SlaPolicy>(`/api/v1/sla-policies/${policyId}/default`, {
      method: 'PATCH',
      body: JSON.stringify({ defaultPolicy: true }),
    })
  },

  delete(policyId: number) {
    return requestApi<void>(`/api/v1/sla-policies/${policyId}`, {
      method: 'DELETE',
    })
  },
}
