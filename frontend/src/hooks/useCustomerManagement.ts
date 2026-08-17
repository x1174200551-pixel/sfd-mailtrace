import { useCallback, useEffect, useState } from 'react'
import { customerApi } from '../api/customers'
import { ticketApi } from '../api/tickets'
import type { CustomerPageResponse, CustomerReadonly } from '../types/customer'
import type { TicketPageResponse } from '../types/ticket'

type UseCustomerManagementParams = {
  activeMenu: string
  canReadCustomers: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useCustomerManagement({
  activeMenu,
  canReadCustomers,
  handleAuthExpired,
  token,
}: UseCustomerManagementParams) {
  const [customerKeyword, setCustomerKeyword] = useState('')
  const [customerPage, setCustomerPage] = useState(1)
  const [customerPageSize, setCustomerPageSize] = useState(20)
  const [customersData, setCustomersData] = useState<CustomerPageResponse | null>(null)
  const [customersLoading, setCustomersLoading] = useState(false)
  const [customersError, setCustomersError] = useState('')
  const [selectedCustomerEmail, setSelectedCustomerEmail] = useState('')
  const [customerDetail, setCustomerDetail] = useState<CustomerReadonly | null>(null)
  const [customerDetailLoading, setCustomerDetailLoading] = useState(false)
  const [customerDetailError, setCustomerDetailError] = useState('')
  const [customerTicketsData, setCustomerTicketsData] = useState<TicketPageResponse | null>(null)
  const [customerTicketsLoading, setCustomerTicketsLoading] = useState(false)
  const [customerTicketsError, setCustomerTicketsError] = useState('')

  const fetchCustomers = useCallback(async () => {
    if (!token || activeMenu !== '客户管理') return
    if (!canReadCustomers) {
      setCustomersData(null)
      setCustomersError('当前账号没有客户查看权限')
      setSelectedCustomerEmail('')
      return
    }

    setCustomersLoading(true)
    setCustomersError('')
    try {
      const data = await customerApi.list({
        keyword: customerKeyword.trim(),
        page: customerPage,
        size: customerPageSize,
      })
      setCustomersData(data)
      setSelectedCustomerEmail((current) => {
        if (current && data.records.some((customer) => customer.email === current)) return current
        return data.records[0]?.email || ''
      })
    } catch (error) {
      if (handleAuthExpired(error)) return
      setCustomersError(error instanceof Error ? error.message : '客户列表加载失败')
    } finally {
      setCustomersLoading(false)
    }
  }, [
    activeMenu,
    canReadCustomers,
    customerKeyword,
    customerPage,
    customerPageSize,
    handleAuthExpired,
    token,
  ])

  useEffect(() => {
    void fetchCustomers()
  }, [fetchCustomers])

  const fetchCustomerDetail = useCallback(async () => {
    if (!token || activeMenu !== '客户管理' || !canReadCustomers) return
    if (!selectedCustomerEmail) {
      setCustomerDetail(null)
      setCustomerDetailError('')
      return
    }

    setCustomerDetailLoading(true)
    setCustomerDetailError('')
    try {
      const data = await customerApi.detail(selectedCustomerEmail)
      setCustomerDetail(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setCustomerDetail(null)
      setCustomerDetailError(error instanceof Error ? error.message : '客户详情加载失败')
    } finally {
      setCustomerDetailLoading(false)
    }
  }, [activeMenu, canReadCustomers, handleAuthExpired, selectedCustomerEmail, token])

  useEffect(() => {
    void fetchCustomerDetail()
  }, [fetchCustomerDetail])

  const fetchCustomerTickets = useCallback(async () => {
    if (!token || activeMenu !== '客户管理' || !canReadCustomers || !selectedCustomerEmail) {
      setCustomerTicketsData(null)
      return
    }

    setCustomerTicketsLoading(true)
    setCustomerTicketsError('')
    try {
      const data = await ticketApi.list({
        keyword: selectedCustomerEmail,
        page: 1,
        size: 5,
      })
      setCustomerTicketsData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setCustomerTicketsError(error instanceof Error ? error.message : '关联工单加载失败')
    } finally {
      setCustomerTicketsLoading(false)
    }
  }, [activeMenu, canReadCustomers, handleAuthExpired, selectedCustomerEmail, token])

  useEffect(() => {
    void fetchCustomerTickets()
  }, [fetchCustomerTickets])

  const changeCustomerKeyword = useCallback((value: string) => {
    setCustomerKeyword(value)
    setCustomerPage(1)
  }, [])

  const changeCustomerPage = useCallback((page: number, size: number) => {
    setCustomerPage(page)
    setCustomerPageSize(size)
  }, [])

  const searchCustomers = useCallback(() => {
    setCustomerPage(1)
    void fetchCustomers()
  }, [fetchCustomers])

  return {
    changeCustomerKeyword,
    changeCustomerPage,
    customerDetail,
    customerDetailError,
    customerDetailLoading,
    customerKeyword,
    customerPage,
    customerPageSize,
    customerTicketsData,
    customerTicketsError,
    customerTicketsLoading,
    customersData,
    customersError,
    customersLoading,
    fetchCustomerDetail,
    fetchCustomerTickets,
    fetchCustomers,
    searchCustomers,
    selectedCustomerEmail,
    setSelectedCustomerEmail,
  }
}
