import { requestApi } from '../shared/api/request'
import type { Enterprise, EnterpriseListResponse, EnterpriseOption } from '../types/enterprise'

export type EnterprisePayload = {
  enterpriseName: string
  contactName: string
  contactEmail: string
  contactPhone: string
  enabled: boolean
  remark: string
}

function toQuery(params: Record<string, string | number | boolean | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && String(value).trim()) search.set(key, String(value))
  })
  return search.size ? `?${search.toString()}` : ''
}

export const enterpriseApi = {
  list(params: { keyword?: string; enabled?: boolean; page: number; size: number }) {
    return requestApi<EnterpriseListResponse>(`/api/v1/enterprises${toQuery(params)}`)
  },

  options(enabled?: boolean) {
    return requestApi<EnterpriseOption[]>(`/api/v1/enterprises/options${toQuery({ enabled })}`)
  },

  save(id: number | null, payload: EnterprisePayload) {
    return requestApi<Enterprise>(id ? `/api/v1/enterprises/${id}` : '/api/v1/enterprises', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(payload),
    })
  },

  setEnabled(id: number, enabled: boolean) {
    return requestApi<Enterprise>(`/api/v1/enterprises/${id}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },
}
