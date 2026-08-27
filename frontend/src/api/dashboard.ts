import { requestApi } from '../shared/api/request'
import type { DashboardReport, DashboardSummary, DashboardTodoListResponse } from '../types/dashboard'

function toQuery(params: Record<string, string | number | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && String(value).trim() !== '') search.set(key, String(value))
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export type DashboardFilterQuery = { enterpriseId?: number; mailboxId?: number }

export const dashboardApi = {
  summary(params: DashboardFilterQuery = {}) {
    return requestApi<DashboardSummary>(`/api/v1/dashboard/summary${toQuery(params)}`)
  },

  myTodos(limit = 5, params: DashboardFilterQuery = {}) {
    return requestApi<DashboardTodoListResponse>(`/api/v1/dashboard/my-todos${toQuery({ ...params, limit })}`)
  },

  report(params: DashboardFilterQuery = {}) {
    return requestApi<DashboardReport>(`/api/v1/dashboard/report${toQuery(params)}`)
  },
}
