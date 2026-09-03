import { useCallback, useEffect, useState } from 'react'
import { message } from 'antd'
import { ticketApi } from '../api/tickets'
import { enterpriseApi } from '../api/enterprises'
import { mailboxApi } from '../api/mailboxes'
import { ApiError } from '../shared/api/error-handler'
import type { TicketAttachment, TicketDetail, TicketPageResponse, TicketStats } from '../types/ticket'
import type { EnterpriseOption } from '../types/enterprise'
import type { MailboxOption } from '../types/mailbox'

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
  const [ticketEnterpriseFilter, setTicketEnterpriseFilter] = useState('ALL')
  const [ticketMailboxFilter, setTicketMailboxFilter] = useState('ALL')
  const [ticketEnterpriseOptions, setTicketEnterpriseOptions] = useState<EnterpriseOption[]>([])
  const [ticketMailboxOptions, setTicketMailboxOptions] = useState<MailboxOption[]>([])
  const [ticketPage, setTicketPage] = useState(1)
  const [ticketPageSize] = useState(10)
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
        enterpriseId: ticketEnterpriseFilter === 'ALL' ? undefined : Number(ticketEnterpriseFilter),
        keyword: ticketKeyword.trim() || undefined,
        mailboxId: ticketMailboxFilter === 'ALL' ? undefined : Number(ticketMailboxFilter),
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
    ticketEnterpriseFilter,
    ticketMailboxFilter,
    ticketPage,
    ticketPageSize,
    ticketSlaBreachedOnly,
    ticketStatusTab,
    token,
  ])

  useEffect(() => {
    if (!token || activeMenu !== '全部工单') return
    void Promise.all([
      enterpriseApi.options(),
      mailboxApi.options(ticketEnterpriseFilter === 'ALL' ? undefined : Number(ticketEnterpriseFilter)),
    ]).then(([enterprises, mailboxes]) => {
      setTicketEnterpriseOptions(enterprises)
      setTicketMailboxOptions(mailboxes)
      setTicketMailboxFilter((current) => current === 'ALL' || mailboxes.some((mailbox) => String(mailbox.id) === current) ? current : 'ALL')
    }).catch((error) => {
      if (!handleAuthExpired(error)) message.error(error instanceof Error ? error.message : '工单筛选项加载失败')
    })
  }, [activeMenu, handleAuthExpired, ticketEnterpriseFilter, token])

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
    setTicketEnterpriseFilter('ALL')
    setTicketMailboxFilter('ALL')
    setTicketStatusTab('ALL')
    setTicketSlaBreachedOnly(false)
    setTicketPage(1)
  }, [])

  const changeTicketKeyword = useCallback((value: string) => {
    setTicketKeyword(value)
    setTicketPage(1)
  }, [])

  const changeTicketEnterpriseFilter = useCallback((value: string) => {
    setTicketEnterpriseFilter(value)
    setTicketMailboxFilter('ALL')
    setTicketPage(1)
  }, [])

  const changeTicketMailboxFilter = useCallback((value: string) => {
    setTicketMailboxFilter(value)
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
    setTicketsError('')
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
      setShowTicketDetailPage(false)
      setTicketDetail(null)
      setTicketsError(error instanceof ApiError ? error.message : '工单详情加载失败或无权访问')
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
    changeTicketEnterpriseFilter,
    changeTicketKeyword,
    changeTicketMailboxFilter,
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
    ticketEnterpriseFilter,
    ticketEnterpriseOptions,
    ticketKeyword,
    ticketMailboxFilter,
    ticketMailboxOptions,
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
