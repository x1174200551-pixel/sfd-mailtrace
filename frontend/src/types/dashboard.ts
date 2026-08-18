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

export type DashboardMetricItem = {
  label: string
  value: string
  detail: string
  tone: string
}

export type DashboardChartItem = {
  label: string
  value: number
  tone: string
}

export type DashboardFlowItem = DashboardMetricItem & {
  iconKey: string
}

export type DashboardActionItem = {
  label: string
  detail: string
  value: number
  tone: string
  iconKey: string
  targetMenu: string | null
  ticketStatus: string | null
  slaBreachedOnly: boolean
}

export type DashboardAssigneeLoad = {
  name: string
  detail: string
  value: number
  overdue: boolean
}

export type DashboardQualityCheck = {
  label: string
  detail: string
  value: number
  tone: string
  iconKey: string
  targetMenu: string | null
  ticketStatus: string | null
  slaBreachedOnly: boolean
}

export type DashboardReport = {
  efficiency: {
    completionRate: number
    items: DashboardMetricItem[]
  }
  priorityDistribution: {
    maxValue: number
    items: DashboardChartItem[]
  }
  slaHealth: {
    statusText: string
    tone: string
    items: DashboardMetricItem[]
  }
  mailFlow: {
    statusText: string
    tone: string
    items: DashboardFlowItem[]
  }
  actionPanel: {
    tagText: string
    tone: string
    items: DashboardActionItem[]
  }
  assigneeLoads: DashboardAssigneeLoad[]
  qualityChecks: DashboardQualityCheck[]
}
