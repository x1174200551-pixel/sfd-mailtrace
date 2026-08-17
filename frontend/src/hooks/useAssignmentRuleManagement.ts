import { useCallback, useEffect, useMemo, useState } from 'react'
import { message } from 'antd'
import { assignmentRuleApi } from '../api/assignment-rules'
import { userApi } from '../api/users'
import { emptyAssignmentRuleForm, emptyAssignmentRuleTestForm } from '../constants/assignment-rules'
import type {
  AssignmentRule,
  AssignmentRuleConfirmAction,
  AssignmentRuleFormState,
  AssignmentRuleListResponse,
  AssignmentRuleMatchResponse,
  AssignmentRuleTestForm,
} from '../types/assignment-rule'
import type { Mailbox } from '../types/mailbox'
import type { ManagedUser } from '../types/user'

type UseAssignmentRuleManagementParams = {
  activeMenu: string
  canReadAssignmentRules: boolean
  canReadUsers: boolean
  handleAuthExpired: (error: unknown) => boolean
  mailboxes: Mailbox[]
  token: string
}

function toAssignmentRuleForm(rule: AssignmentRule): AssignmentRuleFormState {
  return {
    id: rule.id,
    ruleName: rule.ruleName,
    enabled: rule.enabled,
    priorityOrder: rule.priorityOrder,
    defaultRule: rule.defaultRule,
    matchType: rule.matchType,
    matchValue: rule.matchValue || '',
    assigneeId: String(rule.assigneeId),
    notifyEnabled: rule.notifyEnabled,
  }
}

export function useAssignmentRuleManagement({
  activeMenu,
  canReadAssignmentRules,
  canReadUsers,
  handleAuthExpired,
  mailboxes,
  token,
}: UseAssignmentRuleManagementParams) {
  const [assignmentRulesData, setAssignmentRulesData] = useState<AssignmentRuleListResponse | null>(null)
  const [assignmentRulesLoading, setAssignmentRulesLoading] = useState(false)
  const [assignmentRulesError, setAssignmentRulesError] = useState('')
  const [assignmentKeyword, setAssignmentKeyword] = useState('')
  const [assignmentEnabledFilter, setAssignmentEnabledFilter] = useState('ALL')
  const [assignmentMatchTypeFilter, setAssignmentMatchTypeFilter] = useState('ALL')
  const [assignmentForm, setAssignmentForm] = useState<AssignmentRuleFormState>(emptyAssignmentRuleForm)
  const [assignmentRuleDirty, setAssignmentRuleDirty] = useState(false)
  const [assignmentSaving, setAssignmentSaving] = useState(false)
  const [assignmentActionLoading, setAssignmentActionLoading] = useState(false)
  const [assignmentConfirmAction, setAssignmentConfirmAction] = useState<AssignmentRuleConfirmAction>(null)
  const [assignmentAssignees, setAssignmentAssignees] = useState<ManagedUser[]>([])
  const [assignmentTestForm, setAssignmentTestForm] = useState<AssignmentRuleTestForm>(emptyAssignmentRuleTestForm)
  const [assignmentTesting, setAssignmentTesting] = useState(false)
  const [assignmentMatchResult, setAssignmentMatchResult] = useState<AssignmentRuleMatchResponse | null>(null)

  const fetchAssignmentRules = useCallback(async () => {
    if (!token || activeMenu !== '分配规则') return
    if (!canReadAssignmentRules) {
      setAssignmentRulesData(null)
      setAssignmentRulesError('当前账号没有分配规则管理权限')
      return
    }

    setAssignmentRulesLoading(true)
    setAssignmentRulesError('')
    try {
      const data = await assignmentRuleApi.list({
        enabled: assignmentEnabledFilter !== 'ALL' ? assignmentEnabledFilter : undefined,
        keyword: assignmentKeyword.trim() || undefined,
        matchType: assignmentMatchTypeFilter !== 'ALL' ? assignmentMatchTypeFilter : undefined,
      })
      setAssignmentRulesData(data)
      const selected = data.records.find((rule) => rule.id === assignmentForm.id) || data.records[0] || null
      if (selected && !assignmentRuleDirty) {
        setAssignmentForm(toAssignmentRuleForm(selected))
        setAssignmentMatchResult(null)
      }
      if (!selected && !assignmentRuleDirty) {
        setAssignmentForm(emptyAssignmentRuleForm)
        setAssignmentMatchResult(null)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '分配规则加载失败')
    } finally {
      setAssignmentRulesLoading(false)
    }
  }, [
    activeMenu,
    assignmentEnabledFilter,
    assignmentForm.id,
    assignmentKeyword,
    assignmentMatchTypeFilter,
    assignmentRuleDirty,
    canReadAssignmentRules,
    handleAuthExpired,
    token,
  ])

  useEffect(() => {
    void fetchAssignmentRules()
  }, [fetchAssignmentRules])

  const fetchAssignmentAssignees = useCallback(async () => {
    if (!token || activeMenu !== '分配规则' || !canReadUsers) return
    try {
      const data = await userApi.list({ page: 1, size: 100, roleCode: 'AGENT', enabled: true })
      setAssignmentAssignees(data.records)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentAssignees([])
    }
  }, [activeMenu, canReadUsers, handleAuthExpired, token])

  useEffect(() => {
    void fetchAssignmentAssignees()
  }, [fetchAssignmentAssignees])

  const resetAssignmentFilters = useCallback(() => {
    setAssignmentKeyword('')
    setAssignmentEnabledFilter('ALL')
    setAssignmentMatchTypeFilter('ALL')
  }, [])

  const updateAssignmentForm = useCallback((patch: Partial<AssignmentRuleFormState>) => {
    setAssignmentForm((value) => {
      const next = { ...value, ...patch }
      if (patch.matchType === 'DEFAULT') {
        next.defaultRule = true
        next.matchValue = ''
      } else if (patch.matchType) {
        next.defaultRule = false
      }
      return next
    })
    setAssignmentRuleDirty(true)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }, [])

  const selectAssignmentRule = useCallback((rule: AssignmentRule) => {
    setAssignmentForm(toAssignmentRuleForm(rule))
    setAssignmentRuleDirty(false)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }, [])

  const openCreateAssignmentRule = useCallback(() => {
    setAssignmentForm({
      ...emptyAssignmentRuleForm,
      priorityOrder: (assignmentRulesData?.records.length ?? 0) * 10 + 10,
      assigneeId: assignmentAssignees[0] ? String(assignmentAssignees[0].id) : '',
    })
    setAssignmentRuleDirty(true)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }, [assignmentAssignees, assignmentRulesData])

  const buildAssignmentRulePayload = useCallback(() => ({
    ruleName: assignmentForm.ruleName.trim(),
    enabled: assignmentForm.enabled,
    priorityOrder: Number(assignmentForm.priorityOrder),
    defaultRule: assignmentForm.matchType === 'DEFAULT',
    matchType: assignmentForm.matchType,
    matchValue: assignmentForm.matchType === 'DEFAULT' ? '' : assignmentForm.matchValue.trim(),
    assigneeId: assignmentForm.assigneeId ? Number(assignmentForm.assigneeId) : null,
    notifyEnabled: assignmentForm.notifyEnabled,
  }), [assignmentForm])

  const saveAssignmentRule = useCallback(async () => {
    if (!token) return
    setAssignmentSaving(true)
    setAssignmentRulesError('')
    try {
      const saved = await assignmentRuleApi.save(assignmentForm.id, buildAssignmentRulePayload())
      setAssignmentForm(toAssignmentRuleForm(saved))
      setAssignmentRuleDirty(false)
      await fetchAssignmentRules()
      message.success('分配规则已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '分配规则保存失败')
    } finally {
      setAssignmentSaving(false)
    }
  }, [assignmentForm.id, buildAssignmentRulePayload, fetchAssignmentRules, handleAuthExpired, token])

  const toggleAssignmentRule = useCallback(async (rule: AssignmentRule, enabled: boolean) => {
    if (!token) return
    setAssignmentActionLoading(true)
    setAssignmentRulesError('')
    try {
      const saved = await assignmentRuleApi.setEnabled(rule.id, enabled)
      if (assignmentForm.id === saved.id) {
        setAssignmentForm(toAssignmentRuleForm(saved))
        setAssignmentRuleDirty(false)
      }
      await fetchAssignmentRules()
      message.success(enabled ? '规则已启用' : '规则已停用')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '规则启停失败')
    } finally {
      setAssignmentActionLoading(false)
    }
  }, [assignmentForm.id, fetchAssignmentRules, handleAuthExpired, token])

  const moveAssignmentRule = useCallback(async (rule: AssignmentRule, direction: 1 | -1) => {
    if (!token || !assignmentRulesData) return
    const records = [...assignmentRulesData.records].sort((a, b) => a.priorityOrder - b.priorityOrder || a.id - b.id)
    const index = records.findIndex((item) => item.id === rule.id)
    const targetIndex = index + direction
    if (index < 0 || targetIndex < 0 || targetIndex >= records.length) return
    const current = records[index]
    const target = records[targetIndex]
    records[index] = { ...target, priorityOrder: current.priorityOrder }
    records[targetIndex] = { ...current, priorityOrder: target.priorityOrder }

    setAssignmentActionLoading(true)
    setAssignmentRulesError('')
    try {
      await assignmentRuleApi.sort(records.map((item) => ({ id: item.id, priorityOrder: item.priorityOrder })))
      await fetchAssignmentRules()
      message.success('规则排序已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '规则排序失败')
    } finally {
      setAssignmentActionLoading(false)
    }
  }, [assignmentRulesData, fetchAssignmentRules, handleAuthExpired, token])

  const runAssignmentRuleTest = useCallback(async () => {
    if (!token) return
    setAssignmentTesting(true)
    setAssignmentRulesError('')
    try {
      const mailbox = mailboxes.find((item) => String(item.id) === assignmentTestForm.mailboxId)
      const result = await assignmentRuleApi.testMatch({
        fromEmail: assignmentTestForm.fromEmail.trim(),
        mailboxAddress: mailbox?.emailAddress || '',
        mailboxId: assignmentTestForm.mailboxId ? Number(assignmentTestForm.mailboxId) : null,
        subject: assignmentTestForm.subject.trim(),
      })
      setAssignmentMatchResult(result)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '测试匹配失败')
    } finally {
      setAssignmentTesting(false)
    }
  }, [assignmentTestForm, handleAuthExpired, mailboxes, token])

  const submitAssignmentConfirm = useCallback(async () => {
    if (!token || !assignmentConfirmAction) return
    setAssignmentActionLoading(true)
    setAssignmentRulesError('')
    try {
      await assignmentRuleApi.delete(assignmentConfirmAction.rule.id)
      if (assignmentForm.id === assignmentConfirmAction.rule.id) {
        setAssignmentForm(emptyAssignmentRuleForm)
        setAssignmentRuleDirty(false)
      }
      setAssignmentConfirmAction(null)
      await fetchAssignmentRules()
      message.success('分配规则已删除')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '分配规则删除失败')
    } finally {
      setAssignmentActionLoading(false)
    }
  }, [assignmentConfirmAction, assignmentForm.id, fetchAssignmentRules, handleAuthExpired, token])

  const assignmentRecords = assignmentRulesData?.records ?? []
  const selectedAssignmentRule = assignmentForm.id
    ? assignmentRecords.find((rule) => rule.id === assignmentForm.id) || null
    : null
  const assignmentMailboxOptions = useMemo(() => mailboxes.map((mailbox) => ({
    value: String(mailbox.id),
    label: `${mailbox.mailboxName} ${mailbox.emailAddress}`,
  })), [mailboxes])
  const assignmentAssigneeOptions = useMemo(() => assignmentAssignees.map((agent) => ({
    value: String(agent.id),
    label: `${agent.displayName} / ${agent.email}`,
  })), [assignmentAssignees])

  return {
    assignmentActionLoading,
    assignmentAssigneeOptions,
    assignmentAssignees,
    assignmentConfirmAction,
    assignmentEnabledFilter,
    assignmentForm,
    assignmentKeyword,
    assignmentMailboxOptions,
    assignmentMatchResult,
    assignmentMatchTypeFilter,
    assignmentRuleDirty,
    assignmentRulesData,
    assignmentRulesError,
    assignmentRulesLoading,
    assignmentSaving,
    assignmentTestForm,
    assignmentTesting,
    fetchAssignmentRules,
    moveAssignmentRule,
    openCreateAssignmentRule,
    resetAssignmentFilters,
    runAssignmentRuleTest,
    saveAssignmentRule,
    selectAssignmentRule,
    selectedAssignmentRule,
    setAssignmentConfirmAction,
    setAssignmentEnabledFilter,
    setAssignmentKeyword,
    setAssignmentMatchTypeFilter,
    setAssignmentTestForm,
    submitAssignmentConfirm,
    toggleAssignmentRule,
    updateAssignmentForm,
  }
}
