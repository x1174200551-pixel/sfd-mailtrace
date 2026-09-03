import { useCallback, useEffect, useMemo, useState } from 'react'
import { message } from 'antd'
import { assignmentRuleApi, assignmentRuleGroupApi } from '../api/assignment-rules'
import { enterpriseApi } from '../api/enterprises'
import { userApi } from '../api/users'
import { emptyAssignmentRuleForm, emptyAssignmentRuleTestForm } from '../constants/assignment-rules'
import type {
  AssignmentRule,
  AssignmentRuleConfirmAction,
  AssignmentRuleFormState,
  AssignmentRuleGroup,
  AssignmentRuleGroupFormState,
  AssignmentRuleGroupListResponse,
  AssignmentRuleListResponse,
  AssignmentRuleMatchResponse,
  AssignmentRuleTestForm,
} from '../types/assignment-rule'
import type { MailboxOption } from '../types/mailbox'
import type { ManagedUser } from '../types/user'
import type { EnterpriseOption } from '../types/enterprise'

type UseAssignmentRuleManagementParams = {
  activeMenu: string
  canReadAssignmentRules: boolean
  handleAuthExpired: (error: unknown) => boolean
  mailboxes: MailboxOption[]
  token: string
}

function toAssignmentRuleForm(rule: AssignmentRule): AssignmentRuleFormState {
  return {
    id: rule.id,
    groupId: String(rule.groupId),
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
  handleAuthExpired,
  mailboxes,
  token,
}: UseAssignmentRuleManagementParams) {
  const [assignmentRulesData, setAssignmentRulesData] = useState<AssignmentRuleListResponse | null>(null)
  const [assignmentRulesLoading, setAssignmentRulesLoading] = useState(false)
  const [assignmentRulesError, setAssignmentRulesError] = useState('')
  const [assignmentKeyword, setAssignmentKeyword] = useState('')
  const [debouncedAssignmentKeyword, setDebouncedAssignmentKeyword] = useState('')
  const [assignmentEnabledFilter, setAssignmentEnabledFilter] = useState('ALL')
  const [assignmentMatchTypeFilter, setAssignmentMatchTypeFilter] = useState('ALL')
  const [assignmentForm, setAssignmentForm] = useState<AssignmentRuleFormState>(emptyAssignmentRuleForm)
  const [assignmentFormBaseline, setAssignmentFormBaseline] = useState<AssignmentRuleFormState>(emptyAssignmentRuleForm)
  const assignmentRuleDirty = useMemo(
    () => JSON.stringify(assignmentForm) !== JSON.stringify(assignmentFormBaseline),
    [assignmentForm, assignmentFormBaseline],
  )
  const [assignmentRuleCreating, setAssignmentRuleCreating] = useState(false)
  const [assignmentSaving, setAssignmentSaving] = useState(false)
  const [assignmentActionLoading, setAssignmentActionLoading] = useState(false)
  const [assignmentConfirmAction, setAssignmentConfirmAction] = useState<AssignmentRuleConfirmAction>(null)
  const [assignmentAssignees, setAssignmentAssignees] = useState<ManagedUser[]>([])
  const [assignmentTestForm, setAssignmentTestForm] = useState<AssignmentRuleTestForm>(emptyAssignmentRuleTestForm)
  const [assignmentTesting, setAssignmentTesting] = useState(false)
  const [assignmentMatchResult, setAssignmentMatchResult] = useState<AssignmentRuleMatchResponse | null>(null)
  const [assignmentEnterpriseOptions, setAssignmentEnterpriseOptions] = useState<EnterpriseOption[]>([])
  const [selectedAssignmentEnterpriseId, setSelectedAssignmentEnterpriseId] = useState('')
  const [assignmentGroupsData, setAssignmentGroupsData] = useState<AssignmentRuleGroupListResponse | null>(null)
  const [assignmentGroupsLoading, setAssignmentGroupsLoading] = useState(false)
  const [assignmentGroupSaving, setAssignmentGroupSaving] = useState(false)
  const [selectedAssignmentGroupId, setSelectedAssignmentGroupId] = useState<number | null>(null)
  const [assignmentGroupForm, setAssignmentGroupForm] = useState<AssignmentRuleGroupFormState>({
    id: null, enterpriseId: '', groupName: '', enabled: true, remark: '',
  })

  useEffect(() => {
    if (!token || activeMenu !== '分配规则') return
    void enterpriseApi.options(true)
      .then((options) => {
        setAssignmentEnterpriseOptions(options)
        setSelectedAssignmentEnterpriseId((value) => value || (options[0] ? String(options[0].id) : ''))
      })
      .catch((error) => {
        if (!handleAuthExpired(error)) setAssignmentRulesError(error instanceof Error ? error.message : '企业选项加载失败')
      })
  }, [activeMenu, handleAuthExpired, token])

  const fetchAssignmentGroups = useCallback(async () => {
    if (!token || activeMenu !== '分配规则' || !selectedAssignmentEnterpriseId) return
    setAssignmentGroupsLoading(true)
    try {
      const data = await assignmentRuleGroupApi.list({ enterpriseId: Number(selectedAssignmentEnterpriseId) })
      setAssignmentGroupsData(data)
      const selected = data.records.find((group) => group.id === selectedAssignmentGroupId) || data.records[0] || null
      setSelectedAssignmentGroupId(selected?.id ?? null)
      setAssignmentGroupForm(selected ? {
        id: selected.id,
        enterpriseId: String(selected.enterpriseId),
        groupName: selected.groupName,
        enabled: selected.enabled,
        remark: selected.remark || '',
      } : { id: null, enterpriseId: selectedAssignmentEnterpriseId, groupName: '', enabled: true, remark: '' })
      const groupId = selected ? String(selected.id) : ''
      setAssignmentForm((value) => ({ ...value, groupId }))
      setAssignmentFormBaseline((value) => ({ ...value, groupId }))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '分配规则组加载失败')
    } finally {
      setAssignmentGroupsLoading(false)
    }
  }, [activeMenu, handleAuthExpired, selectedAssignmentEnterpriseId, selectedAssignmentGroupId, token])

  useEffect(() => {
    void fetchAssignmentGroups()
  }, [fetchAssignmentGroups])

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedAssignmentKeyword(assignmentKeyword), 300)
    return () => window.clearTimeout(timer)
  }, [assignmentKeyword])

  const fetchAssignmentRules = useCallback(async () => {
    if (!token || activeMenu !== '分配规则') return
    if (!canReadAssignmentRules) {
      setAssignmentRulesData(null)
      setAssignmentRulesError('当前账号没有分配规则管理权限')
      return
    }

    if (!selectedAssignmentGroupId) {
      setAssignmentRulesData(null)
      const emptyForm = { ...emptyAssignmentRuleForm, groupId: assignmentForm.groupId }
      setAssignmentForm(emptyForm)
      setAssignmentFormBaseline(emptyForm)
      return
    }
    setAssignmentRulesLoading(true)
    setAssignmentRulesError('')
    try {
      const data = await assignmentRuleApi.list({
        groupId: selectedAssignmentGroupId,
        enabled: assignmentEnabledFilter !== 'ALL' ? assignmentEnabledFilter : undefined,
        keyword: debouncedAssignmentKeyword.trim() || undefined,
        matchType: assignmentMatchTypeFilter !== 'ALL' ? assignmentMatchTypeFilter : undefined,
      })
      setAssignmentRulesData(data)
      const selected = assignmentRuleCreating
        ? null
        : data.records.find((rule) => rule.id === assignmentForm.id) || data.records[0] || null
      if (selected && !assignmentRuleDirty) {
        const selectedForm = toAssignmentRuleForm(selected)
        setAssignmentForm(selectedForm)
        setAssignmentFormBaseline(selectedForm)
        setAssignmentMatchResult(null)
      }
      if (!assignmentRuleCreating && !selected && !assignmentRuleDirty) {
        const emptyForm = { ...emptyAssignmentRuleForm, groupId: String(selectedAssignmentGroupId) }
        setAssignmentForm(emptyForm)
        setAssignmentFormBaseline(emptyForm)
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
    assignmentForm.groupId,
    assignmentForm.id,
    debouncedAssignmentKeyword,
    assignmentMatchTypeFilter,
    assignmentRuleCreating,
    assignmentRuleDirty,
    canReadAssignmentRules,
    handleAuthExpired,
    token,
    selectedAssignmentGroupId,
  ])

  useEffect(() => {
    void fetchAssignmentRules()
  }, [fetchAssignmentRules])

  const fetchAssignmentAssignees = useCallback(async () => {
    if (!token || activeMenu !== '分配规则') return
    if (!selectedAssignmentGroupId) {
      setAssignmentAssignees([])
      return
    }
    try {
      setAssignmentAssignees(await userApi.assignableOptions({ assignmentRuleGroupId: selectedAssignmentGroupId }))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentAssignees([])
    }
  }, [activeMenu, handleAuthExpired, selectedAssignmentGroupId, token])

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
      next.defaultRule = next.matchType === 'DEFAULT'
      return next
    })
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }, [])

  const selectAssignmentRule = useCallback((rule: AssignmentRule) => {
    const selectedForm = toAssignmentRuleForm(rule)
    setAssignmentForm(selectedForm)
    setAssignmentFormBaseline(selectedForm)
    setAssignmentRuleCreating(false)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }, [])

  const openCreateAssignmentRule = useCallback(() => {
    const draftForm = {
      ...emptyAssignmentRuleForm,
      groupId: selectedAssignmentGroupId ? String(selectedAssignmentGroupId) : '',
      priorityOrder: (assignmentRulesData?.records.length ?? 0) * 10 + 10,
      assigneeId: assignmentAssignees[0] ? String(assignmentAssignees[0].id) : '',
    }
    setAssignmentForm(draftForm)
    setAssignmentFormBaseline(draftForm)
    setAssignmentRuleCreating(true)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }, [assignmentAssignees, assignmentRulesData, selectedAssignmentGroupId])

  const buildAssignmentRulePayload = useCallback(() => ({
    groupId: Number(assignmentForm.groupId),
    ruleName: assignmentForm.ruleName.trim(),
    enabled: assignmentForm.enabled,
    priorityOrder: Number(assignmentForm.priorityOrder),
    defaultRule: assignmentForm.matchType === 'DEFAULT',
    matchType: assignmentForm.matchType,
    matchValue: assignmentForm.matchValue.trim(),
    assigneeId: assignmentForm.assigneeId ? Number(assignmentForm.assigneeId) : null,
    notifyEnabled: assignmentForm.notifyEnabled,
  }), [assignmentForm])

  const selectAssignmentGroup = useCallback((group: AssignmentRuleGroup) => {
    setSelectedAssignmentGroupId(group.id)
    setAssignmentGroupForm({
      id: group.id,
      enterpriseId: String(group.enterpriseId),
      groupName: group.groupName,
      enabled: group.enabled,
      remark: group.remark || '',
    })
    const emptyForm = { ...emptyAssignmentRuleForm, groupId: String(group.id) }
    setAssignmentForm(emptyForm)
    setAssignmentFormBaseline(emptyForm)
    setAssignmentRuleCreating(false)
    setAssignmentMatchResult(null)
  }, [])

  const openCreateAssignmentGroup = useCallback(() => {
    setAssignmentGroupForm({ id: null, enterpriseId: selectedAssignmentEnterpriseId, groupName: '', enabled: true, remark: '' })
  }, [selectedAssignmentEnterpriseId])

  const saveAssignmentGroup = useCallback(async () => {
    if (!assignmentGroupForm.enterpriseId || !assignmentGroupForm.groupName.trim()) return false
    setAssignmentGroupSaving(true)
    setAssignmentRulesError('')
    try {
      const saved = await assignmentRuleGroupApi.save(assignmentGroupForm.id, {
        enterpriseId: Number(assignmentGroupForm.enterpriseId),
        groupName: assignmentGroupForm.groupName.trim(),
        enabled: assignmentGroupForm.enabled,
        remark: assignmentGroupForm.remark.trim(),
      })
      setSelectedAssignmentGroupId(saved.id)
      await fetchAssignmentGroups()
      message.success('分配规则组已保存')
      return true
    } catch (error) {
      if (!handleAuthExpired(error)) setAssignmentRulesError(error instanceof Error ? error.message : '分配规则组保存失败')
      return false
    } finally {
      setAssignmentGroupSaving(false)
    }
  }, [assignmentGroupForm, fetchAssignmentGroups, handleAuthExpired])

  const toggleAssignmentGroup = useCallback(async (group: AssignmentRuleGroup) => {
    setAssignmentGroupSaving(true)
    try {
      await assignmentRuleGroupApi.setEnabled(group.id, !group.enabled)
      await fetchAssignmentGroups()
    } catch (error) {
      if (!handleAuthExpired(error)) setAssignmentRulesError(error instanceof Error ? error.message : '规则组状态更新失败')
    } finally {
      setAssignmentGroupSaving(false)
    }
  }, [fetchAssignmentGroups, handleAuthExpired])

  const saveAssignmentRule = useCallback(async () => {
    if (!token) return false
    setAssignmentSaving(true)
    setAssignmentRulesError('')
    try {
      const saved = await assignmentRuleApi.save(assignmentForm.id, buildAssignmentRulePayload())
      const savedForm = toAssignmentRuleForm(saved)
      setAssignmentForm(savedForm)
      setAssignmentFormBaseline(savedForm)
      setAssignmentRuleCreating(false)
      await fetchAssignmentRules()
      message.success('分配规则已保存')
      return true
    } catch (error) {
      if (handleAuthExpired(error)) return false
      setAssignmentRulesError(error instanceof Error ? error.message : '分配规则保存失败')
      return false
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
        const savedForm = toAssignmentRuleForm(saved)
        setAssignmentForm(savedForm)
        setAssignmentFormBaseline(savedForm)
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
    const records = assignmentRulesData.records
      .filter((item) => !item.defaultRule && item.matchType !== 'DEFAULT')
      .sort((a, b) => a.priorityOrder - b.priorityOrder || a.id - b.id)
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
      const defaultRules = assignmentRulesData.records.filter((item) => item.defaultRule || item.matchType === 'DEFAULT')
      await assignmentRuleApi.sort([...records, ...defaultRules].map((item) => ({ id: item.id, priorityOrder: item.priorityOrder })))
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
        setAssignmentFormBaseline(emptyAssignmentRuleForm)
        setAssignmentRuleCreating(false)
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
  const discardAssignmentRuleChanges = useCallback(() => {
    const savedRule = assignmentForm.id
      ? assignmentRulesData?.records.find((rule) => rule.id === assignmentForm.id)
      : null
    const restoredForm = savedRule
      ? toAssignmentRuleForm(savedRule)
      : { ...emptyAssignmentRuleForm, groupId: selectedAssignmentGroupId ? String(selectedAssignmentGroupId) : '' }
    setAssignmentForm(restoredForm)
    setAssignmentFormBaseline(restoredForm)
    setAssignmentRuleCreating(false)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }, [assignmentForm.id, assignmentRulesData, selectedAssignmentGroupId])
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
    assignmentEnterpriseOptions,
    assignmentGroupForm,
    assignmentGroupSaving,
    assignmentGroupsData,
    assignmentGroupsLoading,
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
    discardAssignmentRuleChanges,
    fetchAssignmentRules,
    fetchAssignmentGroups,
    moveAssignmentRule,
    openCreateAssignmentRule,
    openCreateAssignmentGroup,
    resetAssignmentFilters,
    runAssignmentRuleTest,
    saveAssignmentRule,
    saveAssignmentGroup,
    selectAssignmentRule,
    selectAssignmentGroup,
    selectedAssignmentEnterpriseId,
    selectedAssignmentGroupId,
    selectedAssignmentRule,
    setAssignmentConfirmAction,
    setAssignmentEnabledFilter,
    setAssignmentKeyword,
    setAssignmentMatchTypeFilter,
    setAssignmentTestForm,
    setAssignmentGroupForm: (patch: Partial<AssignmentRuleGroupFormState>) => setAssignmentGroupForm((value) => ({ ...value, ...patch })),
    setSelectedAssignmentEnterpriseId: (value: string) => {
      setSelectedAssignmentEnterpriseId(value)
      setSelectedAssignmentGroupId(null)
      setAssignmentRulesData(null)
      setAssignmentForm(emptyAssignmentRuleForm)
      setAssignmentFormBaseline(emptyAssignmentRuleForm)
      setAssignmentRuleCreating(false)
      setAssignmentMatchResult(null)
    },
    submitAssignmentConfirm,
    toggleAssignmentRule,
    toggleAssignmentGroup,
    updateAssignmentForm,
  }
}
