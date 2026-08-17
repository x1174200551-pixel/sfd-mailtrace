import { requestApi } from '../shared/api/request'
import type { DashboardSummary, DashboardTodoListResponse } from '../types/dashboard'

export const dashboardApi = {
  summary() {
    return requestApi<DashboardSummary>('/api/v1/dashboard/summary')
  },

  myTodos(limit = 5) {
    return requestApi<DashboardTodoListResponse>(`/api/v1/dashboard/my-todos?limit=${limit}`)
  },
}
