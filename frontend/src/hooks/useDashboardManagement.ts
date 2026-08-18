import { useCallback, useEffect, useState } from 'react'
import dayjs from 'dayjs'
import { dashboardApi } from '../api/dashboard'
import { ApiError } from '../shared/api/error-handler'
import type { DashboardReport, DashboardSummary, DashboardTodoListResponse } from '../types/dashboard'

type UseDashboardManagementParams = {
  activeMenu: string
  canReadDashboard: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useDashboardManagement({
  activeMenu,
  canReadDashboard,
  handleAuthExpired,
  token,
}: UseDashboardManagementParams) {
  const [dashboardSummary, setDashboardSummary] = useState<DashboardSummary | null>(null)
  const [dashboardTodos, setDashboardTodos] = useState<DashboardTodoListResponse | null>(null)
  const [dashboardReport, setDashboardReport] = useState<DashboardReport | null>(null)
  const [dashboardLoading, setDashboardLoading] = useState(false)
  const [dashboardError, setDashboardError] = useState('')
  const [dashboardUpdatedAt, setDashboardUpdatedAt] = useState<string | null>(null)

  const fetchDashboard = useCallback(async () => {
    if (!token || activeMenu !== '工作台') return
    if (!canReadDashboard) {
      setDashboardSummary(null)
      setDashboardTodos(null)
      setDashboardReport(null)
      setDashboardError('当前账号没有工作台查看权限')
      return
    }
    setDashboardLoading(true)
    setDashboardError('')
    try {
      const [summary, todos, report] = await Promise.all([
        dashboardApi.summary(),
        dashboardApi.myTodos(5),
        dashboardApi.report(),
      ])
      setDashboardSummary(summary)
      setDashboardTodos(todos)
      setDashboardReport(report)
      setDashboardUpdatedAt(dayjs().format('YYYY-MM-DD HH:mm'))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setDashboardError(error instanceof ApiError ? error.message : '加载工作台数据失败')
    } finally {
      setDashboardLoading(false)
    }
  }, [activeMenu, canReadDashboard, handleAuthExpired, token])

  useEffect(() => {
    if (activeMenu === '工作台') {
      void fetchDashboard()
    }
  }, [activeMenu, fetchDashboard])

  return {
    dashboardError,
    dashboardLoading,
    dashboardReport,
    dashboardSummary,
    dashboardTodos,
    dashboardUpdatedAt,
    fetchDashboard,
  }
}
