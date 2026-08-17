import type { TicketSummary } from './ticket'

export type DashboardSummary = {
  totalCount: number
  pendingAssignCount: number
  processingCount: number
  waitingCustomerCount: number
  slaOverdueCount: number
  closedTodayCount: number
  activeCount: number
}

export type DashboardTodoListResponse = {
  records: TicketSummary[]
  totalCount: number
  processingCount: number
  waitingCustomerCount: number
  slaOverdueCount: number
  limit: number
}
