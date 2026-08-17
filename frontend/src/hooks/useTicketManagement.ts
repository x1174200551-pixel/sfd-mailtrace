import { useCallback, useEffect, useState } from 'react'
import { message } from 'antd'
import { ticketApi } from '../api/tickets'
import { ApiError } from '../shared/api/error-handler'
import type { TicketAttachment, TicketDetail, TicketPageResponse, TicketStats } from '../types/ticket'

type UseTicketManagementParams = {
  activeMenu: string
  canOpenTicketList: boolean
  canReadTickets: boolean
  handleAuthExpired: (error: unknown) => boolean
  onActiveMenuChange: (menu: string) => void
  token: string
}

export function useTicketManagement({
  activeMenu,
  canOpenTicketList,
  canReadTickets,
  handleAuthExpired,
  onActiveMenuChange,
  token,
}: UseTicketManagementParams) {
  const [ticketStatusTab, setTicketStatusTab] = useState('ALL')
  const [ticketSlaBreachedOnly, setTicketSlaBreachedOnly] = useState(false)
  const [ticketKeyword, setTicketKeyword] = useState('')
  const [ticketPage, setTicketPage] = useState(1)
  const [ticketPageSize] = useState(20)
  const [ticketsData, setTicketsData] = useState<TicketPageResponse | null>(null)
  const [ticketsLoading, setTicketsLoading] = useState(false)
  const [ticketsError, setTicketsError] = useState('')
  const [ticketDetail, setTicketDetail] = useState<TicketDetail | null>(null)
  const [ticketDetailTab, setTicketDetailTab] = useState('mail')
  const [showTicketDetailPage, setShowTicketDetailPage] = useState(false)
  const [msgFilter, setMsgFilter] = useState('ALL')
  const [msgSortAsc, setMsgSortAsc] = useState(true)
  const [ticketAttachments, setTicketAttachments] = useState<TicketAttachment[]>([])
  const [remarkDraft, setRemarkDraft] = useState('')
  const [ticketStats, setTicketStats] = useState<TicketStats | null>(null)

  const fetchTickets = useCallback(async () => {
    if (!token || activeMenu !== '全部工单') return
    if (!canReadTickets) {
      setTicketsData(null)
      setTicketsError('当前账号没有工单查看权限')
      return
    }
    setTicketsLoading(true)
    setTicketsError('')
    try {
      const data = await ticketApi.list({
        keyword: ticketKeyword.trim() || undefined,
        page: ticketPage,
        size: ticketPageSize,
        slaBreached: ticketSlaBreachedOnly || undefined,
        status: ticketStatusTab !== 'ALL' ? ticketStatusTab : undefined,
      })
      setTicketsData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketsError(error instanceof ApiError ? error.message : '加载工单失败')
    } finally {
      setTicketsLoading(false)
    }
  }, [
    activeMenu,
    canReadTickets,
    handleAuthExpired,
    ticketKeyword,
    ticketPage,
    ticketPageSize,
    ticketSlaBreachedOnly,
    ticketStatusTab,
    token,
  ])

  const fetchTicketStats = useCallback(async () => {
    if (!token) return
    try {
      const data = await ticketApi.stats()
      setTicketStats(data)
    } catch {
      // 统计不阻塞主列表展示。
    }
  }, [token])

  useEffect(() => {
    if (activeMenu === '全部工单') {
      void fetchTickets()
      void fetchTicketStats()
    }
  }, [activeMenu, fetchTicketStats, fetchTickets])

  const navigateToTickets = useCallback((status: string = 'ALL', slaBreachedOnly = false) => {
    if (!canOpenTicketList) {
      message.warning('当前账号没有工单列表查看权限')
      return
    }
    setTicketStatusTab(status)
    setTicketSlaBreachedOnly(slaBreachedOnly)
    setTicketKeyword('')
    setTicketPage(1)
    setShowTicketDetailPage(false)
    onActiveMenuChange('全部工单')
  }, [canOpenTicketList, onActiveMenuChange])

  const resetTicketFilters = useCallback(() => {
    setTicketKeyword('')
    setTicketStatusTab('ALL')
    setTicketSlaBreachedOnly(false)
    setTicketPage(1)
  }, [])

  const changeTicketKeyword = useCallback((value: string) => {
    setTicketKeyword(value)
    setTicketPage(1)
  }, [])

  const changeTicketStatus = useCallback((status: string) => {
    setTicketStatusTab(status)
    setTicketSlaBreachedOnly(false)
    setTicketPage(1)
  }, [])

  const searchTickets = useCallback(() => {
    setTicketPage(1)
    void fetchTickets()
  }, [fetchTickets])

  const searchTicketsByKeyword = useCallback(async (keyword: string) => {
    const normalizedKeyword = keyword.trim()
    if (!normalizedKeyword) return
    if (!canOpenTicketList) {
      message.warning('当前账号没有工单列表查看权限')
      return
    }
    if (!canReadTickets) {
      setTicketsData(null)
      setTicketsError('当前账号没有工单查看权限')
      return
    }

    setTicketKeyword(normalizedKeyword)
    setTicketStatusTab('ALL')
    setTicketSlaBreachedOnly(false)
    setTicketPage(1)
    setShowTicketDetailPage(false)
    onActiveMenuChange('全部工单')
    setTicketsLoading(true)
    setTicketsError('')
    try {
      const data = await ticketApi.list({
        keyword: normalizedKeyword,
        page: 1,
        size: ticketPageSize,
      })
      setTicketsData(data)
      void fetchTicketStats()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketsError(error instanceof ApiError ? error.message : '搜索工单失败')
    } finally {
      setTicketsLoading(false)
    }
  }, [
    canOpenTicketList,
    canReadTickets,
    fetchTicketStats,
    handleAuthExpired,
    onActiveMenuChange,
    ticketPageSize,
  ])

  const refreshTickets = useCallback(() => {
    void fetchTickets()
    void fetchTicketStats()
  }, [fetchTicketStats, fetchTickets])

  const handleBackToList = useCallback(() => {
    setShowTicketDetailPage(false)
    setTicketDetail(null)
    setTicketDetailTab('mail')
    setMsgFilter('ALL')
    setMsgSortAsc(false)
    setTicketAttachments([])
  }, [])

  const handleOpenDetail = useCallback(async (id: number) => {
    setShowTicketDetailPage(true)
    setTicketDetailTab('mail')
    if (!token) return
    try {
      const data = await ticketApi.detail(id)
      setTicketDetail(data)
      setRemarkDraft(data.remark || '')
      try {
        const atts = await ticketApi.attachments(id)
        setTicketAttachments(atts || [])
      } catch {
        setTicketAttachments([])
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
    }
  }, [handleAuthExpired, token])

  const reloadTicketDetail = useCallback(async () => {
    if (!token || !ticketDetail) return
    try {
      const data = await ticketApi.detail(ticketDetail.id)
      setTicketDetail(data)
      setRemarkDraft(data.remark || '')
      try {
        const atts = await ticketApi.attachments(ticketDetail.id)
        setTicketAttachments(atts || [])
      } catch {
        // 附件刷新失败不影响详情主体。
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
    }
  }, [handleAuthExpired, ticketDetail, token])

  const saveTicketRemark = useCallback(async (remark: string) => {
    if (!ticketDetail) return
    try {
      await ticketApi.updateRemark(ticketDetail.id, remark)
      message.success('备注已保存')
      await reloadTicketDetail()
    } catch (error: any) {
      message.error(error?.message || '备注保存失败')
    }
  }, [reloadTicketDetail, ticketDetail])

  const openTicketFromCustomer = useCallback((ticketId: number, customerEmail: string) => {
    setTicketKeyword(customerEmail)
    onActiveMenuChange('全部工单')
    void handleOpenDetail(ticketId)
  }, [handleOpenDetail, onActiveMenuChange])

  return {
    changeTicketKeyword,
    changeTicketStatus,
    fetchTicketStats,
    fetchTickets,
    handleBackToList,
    handleOpenDetail,
    msgFilter,
    msgSortAsc,
    navigateToTickets,
    openTicketFromCustomer,
    refreshTickets,
    reloadTicketDetail,
    remarkDraft,
    resetTicketFilters,
    saveTicketRemark,
    searchTickets,
    searchTicketsByKeyword,
    setMsgFilter,
    setMsgSortAsc,
    setRemarkDraft,
    setShowTicketDetailPage,
    setTicketAttachments,
    setTicketDetailTab,
    setTicketPage,
    showTicketDetailPage,
    ticketAttachments,
    ticketDetail,
    ticketDetailTab,
    ticketKeyword,
    ticketPage,
    ticketPageSize,
    ticketsData,
    ticketsError,
    ticketsLoading,
    ticketSlaBreachedOnly,
    ticketStats,
    ticketStatusTab,
  }
}
