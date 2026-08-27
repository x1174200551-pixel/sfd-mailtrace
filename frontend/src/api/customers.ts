import { requestApi } from '../shared/api/request'
import type { CustomerPageResponse, CustomerReadonly } from '../types/customer'

export type CustomerQuery = {
  enterpriseId?: number
  keyword?: string
  mailboxId?: number
  page: number
  size: number
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

export const customerApi = {
  list(params: CustomerQuery) {
    return requestApi<CustomerPageResponse>(`/api/v1/customers${toQuery(params)}`)
  },

  detail(enterpriseId: number, email: string) {
    return requestApi<CustomerReadonly>(`/api/v1/customers/${encodeURIComponent(email)}${toQuery({ enterpriseId })}`)
  },
}
