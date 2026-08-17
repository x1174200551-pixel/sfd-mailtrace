import { useCallback, useEffect, useState } from 'react'
import { mailboxApi } from '../api/mailboxes'
import { userApi } from '../api/users'
import type { Mailbox } from '../types/mailbox'
import type { ManagedUser } from '../types/user'

type UseMailboxReferenceDataParams = {
  activeMenu: string
  canReadUsers: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useMailboxReferenceData({
  activeMenu,
  canReadUsers,
  handleAuthExpired,
  token,
}: UseMailboxReferenceDataParams) {
  const [mailboxAssignees, setMailboxAssignees] = useState<ManagedUser[]>([])
  const [mailboxes, setMailboxes] = useState<Mailbox[]>([])

  const fetchMailboxAssignees = useCallback(async () => {
    if (!token || activeMenu !== '邮箱配置' || !canReadUsers) return

    try {
      const data = await userApi.list({ page: 1, size: 100, roleCode: 'AGENT', enabled: true })
      setMailboxAssignees(data.records)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxAssignees([])
    }
  }, [activeMenu, canReadUsers, handleAuthExpired, token])

  useEffect(() => {
    void fetchMailboxAssignees()
  }, [fetchMailboxAssignees])

  const fetchMailboxList = useCallback(async () => {
    if (!token) return
    try {
      const data = await mailboxApi.list({ page: 1, size: 100 })
      setMailboxes(data.records)
    } catch {
      setMailboxes([])
    }
  }, [token])

  useEffect(() => {
    if (activeMenu === '收件记录' || activeMenu === '发件记录' || activeMenu === '分配规则') {
      void fetchMailboxList()
    }
  }, [activeMenu, fetchMailboxList])

  return {
    mailboxAssignees,
    mailboxes,
  }
}
