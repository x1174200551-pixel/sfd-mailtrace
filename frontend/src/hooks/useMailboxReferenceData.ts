import { useCallback, useEffect, useState } from 'react'
import { mailboxApi } from '../api/mailboxes'
import { userApi } from '../api/users'
import { enterpriseApi } from '../api/enterprises'
import { notificationTemplateApi } from '../api/notification-templates'
import { slaPolicyApi } from '../api/sla-policies'
import { assignmentRuleGroupApi } from '../api/assignment-rules'
import type { AssignmentRuleGroup } from '../types/assignment-rule'
import type { EnterpriseOption } from '../types/enterprise'
import type { NotificationTemplate } from '../types/notification-template'
import type { SlaPolicy } from '../types/sla-policy'
import type { MailboxOption } from '../types/mailbox'
import type { ManagedUser } from '../types/user'

type UseMailboxReferenceDataParams = {
  activeMenu: string
  handleAuthExpired: (error: unknown) => boolean
  token: string
  selectedEnterpriseId?: string
  selectedMailboxId?: number | null
}

export function useMailboxReferenceData({
  activeMenu,
  handleAuthExpired,
  token,
  selectedEnterpriseId,
  selectedMailboxId,
}: UseMailboxReferenceDataParams) {
  const [mailboxAssignees, setMailboxAssignees] = useState<ManagedUser[]>([])
  const [mailboxes, setMailboxes] = useState<MailboxOption[]>([])
  const [enterpriseOptions, setEnterpriseOptions] = useState<EnterpriseOption[]>([])
  const [mailboxTemplateOptions, setMailboxTemplateOptions] = useState<NotificationTemplate[]>([])
  const [mailboxSlaOptions, setMailboxSlaOptions] = useState<SlaPolicy[]>([])
  const [mailboxRuleGroupOptions, setMailboxRuleGroupOptions] = useState<AssignmentRuleGroup[]>([])

  const fetchMailboxAssignees = useCallback(async () => {
    if (!token || activeMenu !== '邮箱配置') return
    const enterpriseId = Number(selectedEnterpriseId)
    if (!selectedMailboxId && !enterpriseId) {
      setMailboxAssignees([])
      return
    }

    try {
      setMailboxAssignees(await userApi.assignableOptions(
        selectedMailboxId ? { mailboxId: selectedMailboxId } : { enterpriseId },
      ))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxAssignees([])
    }
  }, [activeMenu, handleAuthExpired, selectedEnterpriseId, selectedMailboxId, token])

  useEffect(() => {
    void fetchMailboxAssignees()
  }, [fetchMailboxAssignees])

  const fetchMailboxStrategyReferences = useCallback(async () => {
    if (!token || activeMenu !== '邮箱配置') return
    try {
      const enterprises = await enterpriseApi.options()
      setEnterpriseOptions(enterprises)
      const templates = await notificationTemplateApi.list()
      setMailboxTemplateOptions(templates.records.filter((template) => template.enabled))
      const enterpriseId = Number(selectedEnterpriseId)
      if (!enterpriseId) {
        setMailboxSlaOptions([])
        setMailboxRuleGroupOptions([])
        return
      }
      const [policies, groups] = await Promise.all([
        slaPolicyApi.list({ enterpriseId, enabled: 'true' }),
        assignmentRuleGroupApi.options(enterpriseId, true),
      ])
      setMailboxSlaOptions(policies.records)
      setMailboxRuleGroupOptions(groups)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setEnterpriseOptions([])
      setMailboxTemplateOptions([])
      setMailboxSlaOptions([])
      setMailboxRuleGroupOptions([])
    }
  }, [activeMenu, handleAuthExpired, selectedEnterpriseId, token])

  useEffect(() => {
    void fetchMailboxStrategyReferences()
  }, [fetchMailboxStrategyReferences])

  const fetchMailboxList = useCallback(async () => {
    if (!token || activeMenu !== '分配规则') return
    try {
      setMailboxes(await mailboxApi.options(undefined, true))
    } catch {
      setMailboxes([])
    }
  }, [activeMenu, token])

  useEffect(() => {
    void fetchMailboxList()
  }, [activeMenu, fetchMailboxList])

  return {
    mailboxAssignees,
    enterpriseOptions,
    mailboxTemplateOptions,
    mailboxSlaOptions,
    mailboxRuleGroupOptions,
    mailboxes,
  }
}
