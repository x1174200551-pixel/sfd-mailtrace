import { useCallback, useEffect, useState } from 'react'
import { mailSendLogApi } from '../api/mail-logs'
import { enterpriseApi } from '../api/enterprises'
import { mailboxApi } from '../api/mailboxes'
import { ApiError } from '../shared/api/error-handler'
import type { MailSendLog, MailSendLogPageResponse, MailSendLogStats } from '../types/mail-logs'
import type { EnterpriseOption } from '../types/enterprise'
import type { MailboxOption } from '../types/mailbox'

type UseMailSendLogsParams = {
  activeMenu: string
  canReadSendLogs: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useMailSendLogs({
  activeMenu,
  canReadSendLogs,
  handleAuthExpired,
  token,
}: UseMailSendLogsParams) {
  const [sendLogPage, setSendLogPage] = useState(1)
  const [sendLogPageSize, setSendLogPageSize] = useState(10)
  const [sendLogMailboxFilter, setSendLogMailboxFilter] = useState('')
  const [sendLogEnterpriseFilter, setSendLogEnterpriseFilter] = useState('ALL')
  const [sendLogEnterpriseOptions, setSendLogEnterpriseOptions] = useState<EnterpriseOption[]>([])
  const [sendLogMailboxOptions, setSendLogMailboxOptions] = useState<MailboxOption[]>([])
  const [sendLogTypeFilter, setSendLogTypeFilter] = useState('ALL')
  const [sendLogStatusFilter, setSendLogStatusFilter] = useState('ALL')
  const [sendLogStartFrom, setSendLogStartFrom] = useState('')
  const [sendLogStartTo, setSendLogStartTo] = useState('')
  const [sendLogsData, setSendLogsData] = useState<MailSendLogPageResponse | null>(null)
  const [sendLogsLoading, setSendLogsLoading] = useState(false)
  const [sendLogsError, setSendLogsError] = useState('')
  const [sendLogDetail, setSendLogDetail] = useState<MailSendLog | null>(null)
  const [sendLogStats, setSendLogStats] = useState<MailSendLogStats | null>(null)
  const [sendPendingCount, setSendPendingCount] = useState(0)

  const fetchSendLogs = useCallback(async () => {
    if (!token || activeMenu !== '发件记录') return
    if (!canReadSendLogs) {
      setSendLogsData(null)
      setSendLogsError('当前账号没有发件记录查看权限')
      return
    }
    setSendLogsLoading(true)
    setSendLogsError('')
    try {
      const data = await mailSendLogApi.list({
        enterpriseId: sendLogEnterpriseFilter === 'ALL' ? undefined : sendLogEnterpriseFilter,
        mailboxId: sendLogMailboxFilter,
        page: sendLogPage,
        sendStatus: sendLogStatusFilter !== 'ALL' ? sendLogStatusFilter : undefined,
        sendType: sendLogTypeFilter !== 'ALL' ? sendLogTypeFilter : undefined,
        size: sendLogPageSize,
        startFrom: sendLogStartFrom,
        startTo: sendLogStartTo,
      })
      setSendLogsData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSendLogsError(error instanceof ApiError ? error.message : '加载发送日志失败')
    } finally {
      setSendLogsLoading(false)
    }
  }, [
    activeMenu,
    canReadSendLogs,
    handleAuthExpired,
    sendLogMailboxFilter,
    sendLogEnterpriseFilter,
    sendLogPage,
    sendLogPageSize,
    sendLogStartFrom,
    sendLogStartTo,
    sendLogStatusFilter,
    sendLogTypeFilter,
    token,
  ])

  useEffect(() => {
    if (!token || activeMenu !== '发件记录') return
    void Promise.all([
      enterpriseApi.options(),
      mailboxApi.options(sendLogEnterpriseFilter === 'ALL' ? undefined : Number(sendLogEnterpriseFilter)),
    ]).then(([enterprises, mailboxes]) => {
      setSendLogEnterpriseOptions(enterprises)
      setSendLogMailboxOptions(mailboxes)
      setSendLogMailboxFilter((current) => !current || mailboxes.some((mailbox) => String(mailbox.id) === current) ? current : '')
    }).catch((error) => {
      if (!handleAuthExpired(error)) setSendLogsError(error instanceof Error ? error.message : '发件筛选项加载失败')
    })
  }, [activeMenu, handleAuthExpired, sendLogEnterpriseFilter, token])

  const fetchSendLogStats = useCallback(async () => {
    if (!token || !canReadSendLogs) return
    try {
      const data = await mailSendLogApi.stats()
      setSendLogStats(data)
    } catch {
      // 统计加载失败不影响主列表展示。
    }
  }, [canReadSendLogs, token])

  const fetchSendPendingCount = useCallback(async () => {
    if (!token || !canReadSendLogs) return
    try {
      const count = await mailSendLogApi.pendingCount()
      setSendPendingCount(count)
    } catch {
      // 待发送角标失败不影响菜单渲染。
    }
  }, [canReadSendLogs, token])

  useEffect(() => {
    if (activeMenu === '发件记录') {
      void fetchSendLogs()
      void fetchSendLogStats()
    }
  }, [activeMenu, fetchSendLogs, fetchSendLogStats])

  useEffect(() => {
    if (token) void fetchSendPendingCount()
  }, [token, fetchSendPendingCount])

  const clearSendLogFilters = useCallback(() => {
    setSendLogEnterpriseFilter('ALL')
    setSendLogMailboxFilter('')
    setSendLogTypeFilter('ALL')
    setSendLogStatusFilter('ALL')
    setSendLogStartFrom('')
    setSendLogStartTo('')
    setSendLogPage(1)
  }, [])

  const changeSendLogEnterpriseFilter = useCallback((value: string) => {
    setSendLogEnterpriseFilter(value)
    setSendLogMailboxFilter('')
    setSendLogPage(1)
  }, [])

  const changeSendLogMailboxFilter = useCallback((value: string) => {
    setSendLogMailboxFilter(value)
    setSendLogPage(1)
  }, [])

  const changeSendLogPage = useCallback((page: number, size: number) => {
    setSendLogPage(page)
    setSendLogPageSize(size)
  }, [])

  const querySendLogs = useCallback(() => {
    setSendLogPage(1)
    void fetchSendLogs()
  }, [fetchSendLogs])

  const refreshSendLogs = useCallback(() => {
    void fetchSendLogs()
    void fetchSendLogStats()
  }, [fetchSendLogStats, fetchSendLogs])

  const changeSendLogStatusFilter = useCallback((value: string) => {
    setSendLogStatusFilter(value)
    setSendLogPage(1)
  }, [])

  const changeSendLogTimeRange = useCallback((startFrom: string, startTo: string) => {
    setSendLogStartFrom(startFrom)
    setSendLogStartTo(startTo)
    setSendLogPage(1)
  }, [])

  const changeSendLogTypeFilter = useCallback((value: string) => {
    setSendLogTypeFilter(value)
    setSendLogPage(1)
  }, [])

  return {
    changeSendLogEnterpriseFilter,
    changeSendLogMailboxFilter,
    changeSendLogPage,
    changeSendLogStatusFilter,
    changeSendLogTimeRange,
    changeSendLogTypeFilter,
    clearSendLogFilters,
    querySendLogs,
    refreshSendLogs,
    sendLogDetail,
    sendLogEnterpriseFilter,
    sendLogEnterpriseOptions,
    sendLogMailboxFilter,
    sendLogMailboxOptions,
    sendLogPage,
    sendLogPageSize,
    sendLogStartFrom,
    sendLogStartTo,
    sendLogStats,
    sendLogStatusFilter,
    sendLogTypeFilter,
    sendLogsData,
    sendLogsError,
    sendLogsLoading,
    sendPendingCount,
    setSendLogDetail,
  }
}
