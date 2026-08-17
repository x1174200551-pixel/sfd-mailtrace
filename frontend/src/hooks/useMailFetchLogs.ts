import { useCallback, useEffect, useState } from 'react'
import { mailFetchLogApi } from '../api/mail-logs'
import { ApiError } from '../shared/api/error-handler'
import type { MailFetchLog, MailFetchLogPageResponse, MailFetchLogStats } from '../types/mail-logs'

type UseMailFetchLogsParams = {
  activeMenu: string
  canReadFetchLogs: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useMailFetchLogs({
  activeMenu,
  canReadFetchLogs,
  handleAuthExpired,
  token,
}: UseMailFetchLogsParams) {
  const [fetchLogMailboxFilter, setFetchLogMailboxFilter] = useState('')
  const [fetchLogSuccessFilter, setFetchLogSuccessFilter] = useState('ALL')
  const [fetchLogStartFrom, setFetchLogStartFrom] = useState('')
  const [fetchLogStartTo, setFetchLogStartTo] = useState('')
  const [fetchLogPage, setFetchLogPage] = useState(1)
  const [fetchLogPageSize, setFetchLogPageSize] = useState(10)
  const [fetchLogsData, setFetchLogsData] = useState<MailFetchLogPageResponse | null>(null)
  const [fetchLogsLoading, setFetchLogsLoading] = useState(false)
  const [fetchLogsError, setFetchLogsError] = useState('')
  const [fetchLogDetail, setFetchLogDetail] = useState<MailFetchLog | null>(null)
  const [fetchLogStats, setFetchLogStats] = useState<MailFetchLogStats | null>(null)

  const fetchFetchLogs = useCallback(async () => {
    if (!token || activeMenu !== '收件记录') return
    if (!canReadFetchLogs) {
      setFetchLogsData(null)
      setFetchLogsError('当前账号没有收件记录查看权限')
      return
    }

    setFetchLogsLoading(true)
    setFetchLogsError('')
    try {
      const data = await mailFetchLogApi.list({
        mailboxId: fetchLogMailboxFilter,
        page: fetchLogPage,
        size: fetchLogPageSize,
        startFrom: fetchLogStartFrom,
        startTo: fetchLogStartTo,
        success: fetchLogSuccessFilter !== 'ALL' ? fetchLogSuccessFilter : undefined,
      })
      setFetchLogsData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      const msg = error instanceof ApiError ? error.message : '加载拉取日志失败'
      setFetchLogsError(msg)
    } finally {
      setFetchLogsLoading(false)
    }
  }, [
    activeMenu,
    canReadFetchLogs,
    fetchLogMailboxFilter,
    fetchLogPage,
    fetchLogPageSize,
    fetchLogStartFrom,
    fetchLogStartTo,
    fetchLogSuccessFilter,
    handleAuthExpired,
    token,
  ])

  const fetchFetchLogStats = useCallback(async () => {
    if (!token || !canReadFetchLogs) return
    try {
      const data = await mailFetchLogApi.stats()
      setFetchLogStats(data)
    } catch {
      // 统计加载失败不影响主列表展示。
    }
  }, [canReadFetchLogs, token])

  useEffect(() => {
    if (activeMenu === '收件记录') {
      void fetchFetchLogs()
      void fetchFetchLogStats()
    }
  }, [activeMenu, fetchFetchLogs, fetchFetchLogStats])

  const clearFetchLogFilters = useCallback(() => {
    setFetchLogMailboxFilter('')
    setFetchLogSuccessFilter('ALL')
    setFetchLogStartFrom('')
    setFetchLogStartTo('')
    setFetchLogPage(1)
  }, [])

  const changeFetchLogMailboxFilter = useCallback((value: string) => {
    setFetchLogMailboxFilter(value)
    setFetchLogPage(1)
  }, [])

  const changeFetchLogPage = useCallback((page: number, size: number) => {
    setFetchLogPage(page)
    setFetchLogPageSize(size)
  }, [])

  const queryFetchLogs = useCallback(() => {
    setFetchLogPage(1)
    void fetchFetchLogs()
  }, [fetchFetchLogs])

  const refreshFetchLogs = useCallback(() => {
    void fetchFetchLogs()
  }, [fetchFetchLogs])

  const changeFetchLogSuccessFilter = useCallback((value: string) => {
    setFetchLogSuccessFilter(value)
    setFetchLogPage(1)
  }, [])

  const changeFetchLogTimeRange = useCallback((startFrom: string, startTo: string) => {
    setFetchLogStartFrom(startFrom)
    setFetchLogStartTo(startTo)
    setFetchLogPage(1)
  }, [])

  return {
    changeFetchLogMailboxFilter,
    changeFetchLogPage,
    changeFetchLogSuccessFilter,
    changeFetchLogTimeRange,
    clearFetchLogFilters,
    fetchFetchLogs,
    fetchLogDetail,
    fetchLogMailboxFilter,
    fetchLogPage,
    fetchLogPageSize,
    fetchLogStartFrom,
    fetchLogStartTo,
    fetchLogStats,
    fetchLogSuccessFilter,
    fetchLogsData,
    fetchLogsError,
    fetchLogsLoading,
    queryFetchLogs,
    refreshFetchLogs,
    setFetchLogDetail,
  }
}
