import { requestApi } from '../shared/api/request'
import type { DashboardReport, DashboardSummary, DashboardTodoListResponse } from '../types/dashboard'

export const dashboardApi = {
  summary() {
    return requestApi<DashboardSummary>('/api/v1/dashboard/summary')
  },

  myTodos(limit = 5) {
    return requestApi<DashboardTodoListResponse>(`/api/v1/dashboard/my-todos?limit=${limit}`)
  },

  report() {
    return requestApi<DashboardReport>('/api/v1/dashboard/report')
  },
}
