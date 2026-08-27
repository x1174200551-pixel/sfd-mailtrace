import { useCallback, useEffect, useState } from 'react'
import dayjs from 'dayjs'
import { dashboardApi } from '../api/dashboard'
import { enterpriseApi } from '../api/enterprises'
import { mailboxApi } from '../api/mailboxes'
import { ApiError } from '../shared/api/error-handler'
import type { DashboardReport, DashboardSummary, DashboardTodoListResponse } from '../types/dashboard'
import type { EnterpriseOption } from '../types/enterprise'
import type { MailboxOption } from '../types/mailbox'

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
  const [dashboardEnterpriseFilter, setDashboardEnterpriseFilter] = useState('ALL')
  const [dashboardMailboxFilter, setDashboardMailboxFilter] = useState('ALL')
  const [dashboardEnterpriseOptions, setDashboardEnterpriseOptions] = useState<EnterpriseOption[]>([])
  const [dashboardMailboxOptions, setDashboardMailboxOptions] = useState<MailboxOption[]>([])

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
      const filter = {
        enterpriseId: dashboardEnterpriseFilter === 'ALL' ? undefined : Number(dashboardEnterpriseFilter),
        mailboxId: dashboardMailboxFilter === 'ALL' ? undefined : Number(dashboardMailboxFilter),
      }
      const [summary, todos, report] = await Promise.all([
        dashboardApi.summary(filter),
        dashboardApi.myTodos(5, filter),
        dashboardApi.report(filter),
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
  }, [activeMenu, canReadDashboard, dashboardEnterpriseFilter, dashboardMailboxFilter, handleAuthExpired, token])

  useEffect(() => {
    if (!token || activeMenu !== '工作台') return
    void Promise.all([
      enterpriseApi.options(),
      mailboxApi.options(dashboardEnterpriseFilter === 'ALL' ? undefined : Number(dashboardEnterpriseFilter)),
    ]).then(([enterprises, mailboxes]) => {
      setDashboardEnterpriseOptions(enterprises)
      setDashboardMailboxOptions(mailboxes)
      setDashboardMailboxFilter((current) => current === 'ALL' || mailboxes.some((mailbox) => String(mailbox.id) === current) ? current : 'ALL')
    }).catch((error) => {
      if (!handleAuthExpired(error)) setDashboardError(error instanceof Error ? error.message : '工作台筛选项加载失败')
    })
  }, [activeMenu, dashboardEnterpriseFilter, handleAuthExpired, token])

  const changeDashboardEnterpriseFilter = useCallback((value: string) => {
    setDashboardEnterpriseFilter(value)
    setDashboardMailboxFilter('ALL')
  }, [])

  useEffect(() => {
    if (activeMenu === '工作台') {
      void fetchDashboard()
    }
  }, [activeMenu, fetchDashboard])

  return {
    changeDashboardEnterpriseFilter,
    dashboardEnterpriseFilter,
    dashboardEnterpriseOptions,
    dashboardError,
    dashboardLoading,
    dashboardMailboxFilter,
    dashboardMailboxOptions,
    dashboardReport,
    dashboardSummary,
    dashboardTodos,
    dashboardUpdatedAt,
    fetchDashboard,
    setDashboardMailboxFilter,
  }
}
