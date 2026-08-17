import { useCallback, useEffect, useState } from 'react'
import { systemConfigApi } from '../api/system-config'
import { emptyTicketRuleForm } from '../constants/system-config'
import type { TicketNumberRule, TicketRuleFormState } from '../types/system-config'

type UseTicketNumberRuleManagementParams = {
  activeMenu: string
  canReadTicketNumberRule: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

function toTicketRuleForm(rule: TicketNumberRule): TicketRuleFormState {
  return {
    enabled: rule.enabled,
    prefix: rule.prefix,
    dateFormat: rule.dateFormat,
    seqLength: rule.seqLength,
    separator: rule.separator,
    description: rule.description,
  }
}

export function useTicketNumberRuleManagement({
  activeMenu,
  canReadTicketNumberRule,
  handleAuthExpired,
  token,
}: UseTicketNumberRuleManagementParams) {
  const [ticketRule, setTicketRule] = useState<TicketNumberRule | null>(null)
  const [ticketRuleForm, setTicketRuleForm] = useState<TicketRuleFormState>(emptyTicketRuleForm)
  const [ticketRuleDirty, setTicketRuleDirty] = useState(false)
  const [ticketRuleLoading, setTicketRuleLoading] = useState(false)
  const [ticketRuleSaving, setTicketRuleSaving] = useState(false)
  const [ticketRulePreviewLoading, setTicketRulePreviewLoading] = useState(false)
  const [ticketRuleError, setTicketRuleError] = useState('')
  const [ticketRuleMessage, setTicketRuleMessage] = useState('')
  const [ticketRuleConfirmOpen, setTicketRuleConfirmOpen] = useState(false)

  const fetchTicketRule = useCallback(async () => {
    if (!token || activeMenu !== '编号规则') return
    if (!canReadTicketNumberRule) {
      setTicketRule(null)
      setTicketRuleError('当前账号没有编号规则管理权限')
      return
    }

    setTicketRuleLoading(true)
    setTicketRuleError('')
    setTicketRuleMessage('')
    try {
      const data = await systemConfigApi.ticketNumberRule()
      setTicketRule(data)
      setTicketRuleForm(toTicketRuleForm(data))
      setTicketRuleDirty(false)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketRuleError(error instanceof Error ? error.message : '编号规则加载失败')
    } finally {
      setTicketRuleLoading(false)
    }
  }, [activeMenu, canReadTicketNumberRule, handleAuthExpired, token])

  useEffect(() => {
    void fetchTicketRule()
  }, [fetchTicketRule])

  const updateTicketRuleForm = useCallback((patch: Partial<TicketRuleFormState>) => {
    setTicketRuleForm((value) => ({ ...value, ...patch }))
    setTicketRuleDirty(true)
    setTicketRuleMessage('')
  }, [])

  const previewTicketRule = useCallback(async () => {
    if (!token) return
    setTicketRulePreviewLoading(true)
    setTicketRuleError('')
    setTicketRuleMessage('')
    try {
      const data = await systemConfigApi.previewTicketNumberRule(ticketRuleForm)
      setTicketRule(data)
      setTicketRuleForm(toTicketRuleForm(data))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketRuleError(error instanceof Error ? error.message : '规则预览失败')
    } finally {
      setTicketRulePreviewLoading(false)
    }
  }, [handleAuthExpired, ticketRuleForm, token])

  const saveTicketRule = useCallback(async () => {
    if (!token) return
    setTicketRuleSaving(true)
    setTicketRuleError('')
    setTicketRuleMessage('')
    try {
      const data = await systemConfigApi.saveTicketNumberRule(ticketRuleForm)
      setTicketRule(data)
      setTicketRuleForm(toTicketRuleForm(data))
      setTicketRuleDirty(false)
      setTicketRuleConfirmOpen(false)
      setTicketRuleMessage('编号规则已保存，后续新建工单将使用当前规则。')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketRuleError(error instanceof Error ? error.message : '编号规则保存失败')
    } finally {
      setTicketRuleSaving(false)
    }
  }, [handleAuthExpired, ticketRuleForm, token])

  const resetTicketRule = useCallback(() => {
    updateTicketRuleForm(emptyTicketRuleForm)
    setTicketRuleMessage('已恢复默认规则，保存前可先生成预览确认。')
  }, [updateTicketRuleForm])

  const clearTicketRuleFeedback = useCallback(() => {
    setTicketRuleError('')
    setTicketRuleMessage('')
  }, [])

  return {
    clearTicketRuleFeedback,
    closeTicketRuleConfirm: () => setTicketRuleConfirmOpen(false),
    fetchTicketRule,
    openTicketRuleConfirm: () => setTicketRuleConfirmOpen(true),
    previewTicketRule,
    resetTicketRule,
    saveTicketRule,
    ticketRule,
    ticketRuleConfirmOpen,
    ticketRuleDirty,
    ticketRuleError,
    ticketRuleForm,
    ticketRuleLoading,
    ticketRuleMessage,
    ticketRulePreviewLoading,
    ticketRuleSaving,
    updateTicketRuleForm,
  }
}
