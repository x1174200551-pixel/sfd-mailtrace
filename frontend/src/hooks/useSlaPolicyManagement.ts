import { useCallback, useEffect, useMemo, useState } from 'react'
import { message } from 'antd'
import { slaPolicyApi } from '../api/sla-policies'
import { workCalendarApi } from '../api/work-calendars'
import { emptySlaPolicyForm } from '../constants/sla-policies'
import { enterpriseApi } from '../api/enterprises'
import type { EnterpriseOption } from '../types/enterprise'
import type {
  SlaPolicy,
  SlaPolicyConfirmAction,
  SlaPolicyFormState,
  SlaPolicyListResponse,
} from '../types/sla-policy'
import type { WorkCalendar } from '../types/work-calendar'
import {
  resolveSlaPreview,
  slaPreviewBaseTime,
  toSlaPolicyForm,
  workdayLabel,
} from '../utils/work-calendar'

type UseSlaPolicyManagementParams = {
  activeMenu: string
  canReadSlaPolicies: boolean
  canReadWorkCalendars: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useSlaPolicyManagement({
  activeMenu,
  canReadSlaPolicies,
  canReadWorkCalendars,
  handleAuthExpired,
  token,
}: UseSlaPolicyManagementParams) {
  const [slaPoliciesData, setSlaPoliciesData] = useState<SlaPolicyListResponse | null>(null)
  const [slaPoliciesLoading, setSlaPoliciesLoading] = useState(false)
  const [slaPoliciesError, setSlaPoliciesError] = useState('')
  const [slaPolicyKeyword, setSlaPolicyKeyword] = useState('')
  const [slaPolicyEnabledFilter, setSlaPolicyEnabledFilter] = useState('ALL')
  const [slaPolicyDefaultFilter, setSlaPolicyDefaultFilter] = useState('ALL')
  const [slaPolicyForm, setSlaPolicyForm] = useState<SlaPolicyFormState>(emptySlaPolicyForm)
  const [slaPolicyDirty, setSlaPolicyDirty] = useState(false)
  const [slaPolicySaving, setSlaPolicySaving] = useState(false)
  const [slaPolicyActionLoading, setSlaPolicyActionLoading] = useState(false)
  const [slaPolicyConfirmAction, setSlaPolicyConfirmAction] = useState<SlaPolicyConfirmAction>(null)
  const [workCalendars, setWorkCalendars] = useState<WorkCalendar[]>([])
  const [workCalendarsLoading, setWorkCalendarsLoading] = useState(false)
  const [slaEnterpriseOptions, setSlaEnterpriseOptions] = useState<EnterpriseOption[]>([])
  const [slaEnterpriseFilter, setSlaEnterpriseFilter] = useState('ALL')

  useEffect(() => {
    if (!token || activeMenu !== 'SLA策略') return
    void enterpriseApi.options().then((options) => {
      setSlaEnterpriseOptions(options)
      setSlaEnterpriseFilter((value) => value === 'ALL' && options[0] ? String(options[0].id) : value)
    }).catch(() => setSlaEnterpriseOptions([]))
  }, [activeMenu, token])

  const fetchSlaPolicies = useCallback(async () => {
    if (!token || activeMenu !== 'SLA策略') return
    if (!canReadSlaPolicies) {
      setSlaPoliciesData(null)
      setSlaPoliciesError('当前账号没有 SLA 策略管理权限')
      return
    }

    setSlaPoliciesLoading(true)
    setSlaPoliciesError('')
    try {
      const data = await slaPolicyApi.list({
        enterpriseId: slaEnterpriseFilter === 'ALL' ? undefined : Number(slaEnterpriseFilter),
        keyword: slaPolicyKeyword.trim(),
        enabled: slaPolicyEnabledFilter !== 'ALL' ? slaPolicyEnabledFilter : undefined,
        defaultPolicy: slaPolicyDefaultFilter !== 'ALL' ? slaPolicyDefaultFilter : undefined,
      })
      setSlaPoliciesData(data)
      const selected = data.records.find((policy) => policy.id === slaPolicyForm.id) || data.records[0] || null
      if (selected && !slaPolicyDirty) {
        setSlaPolicyForm(toSlaPolicyForm(selected))
      }
      if (!selected && !slaPolicyDirty) {
        setSlaPolicyForm({
          ...emptySlaPolicyForm,
          enterpriseId: slaEnterpriseFilter === 'ALL' ? '' : slaEnterpriseFilter,
          calendarId: workCalendars[0] ? String(workCalendars[0].id) : '',
        })
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : 'SLA 策略加载失败')
    } finally {
      setSlaPoliciesLoading(false)
    }
  }, [
    activeMenu,
    canReadSlaPolicies,
    handleAuthExpired,
    slaPolicyDefaultFilter,
    slaPolicyDirty,
    slaPolicyEnabledFilter,
    slaPolicyForm.id,
    slaPolicyKeyword,
    slaEnterpriseFilter,
    token,
    workCalendars,
  ])

  useEffect(() => {
    void fetchSlaPolicies()
  }, [fetchSlaPolicies])

  const fetchWorkCalendarsForSla = useCallback(async () => {
    if (!token || activeMenu !== 'SLA策略' || !canReadWorkCalendars) return
    setWorkCalendarsLoading(true)
    try {
      const data = await workCalendarApi.list({ enterpriseId: slaEnterpriseFilter === 'ALL' ? undefined : Number(slaEnterpriseFilter) })
      setWorkCalendars(data.records)
      setSlaPolicyForm((form) => {
        if (form.calendarId || !data.records[0]) return form
        return { ...form, calendarId: String(data.records[0].id) }
      })
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : '工作日历加载失败')
    } finally {
      setWorkCalendarsLoading(false)
    }
  }, [activeMenu, canReadWorkCalendars, handleAuthExpired, slaEnterpriseFilter, token])

  useEffect(() => {
    void fetchWorkCalendarsForSla()
  }, [fetchWorkCalendarsForSla])

  const resetSlaPolicyFilters = useCallback(() => {
    setSlaPolicyKeyword('')
    setSlaPolicyEnabledFilter('ALL')
    setSlaPolicyDefaultFilter('ALL')
  }, [])

  const updateSlaPolicyForm = useCallback((patch: Partial<SlaPolicyFormState>) => {
    setSlaPolicyForm((value) => {
      const next = { ...value, ...patch }
      if (patch.defaultPolicy === true) {
        next.enabled = true
      }
      if (patch.enabled === false && value.defaultPolicy) {
        next.defaultPolicy = false
      }
      return next
    })
    setSlaPolicyDirty(true)
    setSlaPoliciesError('')
  }, [])

  const selectSlaPolicy = useCallback((policy: SlaPolicy) => {
    setSlaPolicyForm(toSlaPolicyForm(policy))
    setSlaPolicyDirty(false)
    setSlaPoliciesError('')
  }, [])

  const openCreateSlaPolicy = useCallback(() => {
    setSlaPolicyForm({
      ...emptySlaPolicyForm,
      enterpriseId: slaEnterpriseFilter === 'ALL' ? (slaEnterpriseOptions[0] ? String(slaEnterpriseOptions[0].id) : '') : slaEnterpriseFilter,
      calendarId: workCalendars[0] ? String(workCalendars[0].id) : '',
    })
    setSlaPolicyDirty(true)
    setSlaPoliciesError('')
  }, [slaEnterpriseFilter, slaEnterpriseOptions, workCalendars])

  const buildSlaPolicyPayload = useCallback(() => ({
    enterpriseId: Number(slaPolicyForm.enterpriseId),
    policyName: slaPolicyForm.policyName.trim(),
    enabled: slaPolicyForm.enabled,
    defaultPolicy: slaPolicyForm.defaultPolicy,
    responseHours: Number(slaPolicyForm.responseHours),
    resolveHours: slaPolicyForm.resolveHours.trim() ? Number(slaPolicyForm.resolveHours) : null,
    warningRemainHours: Number(slaPolicyForm.warningRemainHours),
    escalateAfterBreachHours: slaPolicyForm.escalateAfterBreachHours.trim()
      ? Number(slaPolicyForm.escalateAfterBreachHours)
      : null,
    calendarId: slaPolicyForm.calendarId ? Number(slaPolicyForm.calendarId) : null,
  }), [slaPolicyForm])

  const saveSlaPolicy = useCallback(async () => {
    if (!token) return
    setSlaPolicySaving(true)
    setSlaPoliciesError('')
    try {
      const saved = await slaPolicyApi.save(slaPolicyForm.id, buildSlaPolicyPayload())
      setSlaPolicyForm(toSlaPolicyForm(saved))
      setSlaPolicyDirty(false)
      await fetchSlaPolicies()
      message.success('SLA 策略已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : 'SLA 策略保存失败')
    } finally {
      setSlaPolicySaving(false)
    }
  }, [buildSlaPolicyPayload, fetchSlaPolicies, handleAuthExpired, slaPolicyForm.id, token])

  const toggleSlaPolicy = useCallback(async (policy: SlaPolicy, enabled: boolean) => {
    if (!token) return
    setSlaPolicyActionLoading(true)
    setSlaPoliciesError('')
    try {
      const saved = await slaPolicyApi.setEnabled(policy.id, enabled)
      if (slaPolicyForm.id === saved.id) {
        setSlaPolicyForm(toSlaPolicyForm(saved))
        setSlaPolicyDirty(false)
      }
      await fetchSlaPolicies()
      message.success(enabled ? 'SLA 策略已启用' : 'SLA 策略已停用')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : 'SLA 策略启停失败')
    } finally {
      setSlaPolicyActionLoading(false)
    }
  }, [fetchSlaPolicies, handleAuthExpired, slaPolicyForm.id, token])

  const setDefaultSlaPolicy = useCallback(async (policy: SlaPolicy) => {
    if (!token) return
    setSlaPolicyActionLoading(true)
    setSlaPoliciesError('')
    try {
      const saved = await slaPolicyApi.setDefault(policy.id)
      setSlaPolicyForm(toSlaPolicyForm(saved))
      setSlaPolicyDirty(false)
      await fetchSlaPolicies()
      message.success('默认 SLA 策略已更新')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : '默认策略设置失败')
    } finally {
      setSlaPolicyActionLoading(false)
    }
  }, [fetchSlaPolicies, handleAuthExpired, token])

  const submitSlaPolicyConfirm = useCallback(async () => {
    if (!token || !slaPolicyConfirmAction) return
    setSlaPolicyActionLoading(true)
    setSlaPoliciesError('')
    try {
      await slaPolicyApi.delete(slaPolicyConfirmAction.policy.id)
      if (slaPolicyForm.id === slaPolicyConfirmAction.policy.id) {
        setSlaPolicyForm({
          ...emptySlaPolicyForm,
          enterpriseId: slaEnterpriseFilter === 'ALL' ? '' : slaEnterpriseFilter,
          calendarId: workCalendars[0] ? String(workCalendars[0].id) : '',
        })
        setSlaPolicyDirty(false)
      }
      setSlaPolicyConfirmAction(null)
      await fetchSlaPolicies()
      message.success('SLA 策略已删除')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : 'SLA 策略删除失败')
    } finally {
      setSlaPolicyActionLoading(false)
    }
  }, [fetchSlaPolicies, handleAuthExpired, slaEnterpriseFilter, slaPolicyConfirmAction, slaPolicyForm.id, token, workCalendars])

  const slaPolicyRecords = slaPoliciesData?.records ?? []
  const selectedSlaPolicy = slaPolicyForm.id
    ? slaPolicyRecords.find((policy) => policy.id === slaPolicyForm.id) || null
    : null
  const selectedWorkCalendar = workCalendars.find((calendar) => String(calendar.id) === slaPolicyForm.calendarId) || null
  const slaCalendarOptions = useMemo(() => workCalendars.map((calendar) => ({
    value: String(calendar.id),
    label: `${calendar.calendarName} / ${calendar.timezone} / ${workdayLabel(calendar.workdays)} ${calendar.workStartTime}-${calendar.workEndTime}`,
  })), [workCalendars])
  const slaPreview = resolveSlaPreview(slaPolicyForm, selectedWorkCalendar)
  const slaCalendarCount = new Set(slaPolicyRecords.map((policy) => policy.calendarId)).size
  const slaResolveHoursInvalid = Boolean(
    slaPolicyForm.resolveHours.trim()
    && Number(slaPolicyForm.resolveHours) < Number(slaPolicyForm.responseHours),
  )
  const slaWarningInvalid = Number(slaPolicyForm.warningRemainHours) >= Number(slaPolicyForm.responseHours)

  return {
    fetchSlaPolicies,
    resetSlaPolicyFilters,
    saveSlaPolicy,
    selectSlaPolicy,
    selectedSlaPolicy,
    selectedWorkCalendar,
    setDefaultSlaPolicy,
    setSlaPolicyConfirmAction,
    setSlaPolicyDefaultFilter,
    setSlaPolicyEnabledFilter,
    setSlaPolicyKeyword,
    slaCalendarCount,
    slaCalendarOptions,
    slaPoliciesData,
    slaPoliciesError,
    slaPoliciesLoading,
    slaPolicyActionLoading,
    slaPolicyConfirmAction,
    slaPolicyDefaultFilter,
    slaPolicyDirty,
    slaPolicyEnabledFilter,
    slaPolicyForm,
    slaPolicyKeyword,
    slaPolicySaving,
    slaEnterpriseFilter,
    slaEnterpriseOptions,
    slaPreview,
    slaPreviewBaseTime,
    slaResolveHoursInvalid,
    slaWarningInvalid,
    submitSlaPolicyConfirm,
    toggleSlaPolicy,
    updateSlaPolicyForm,
    setSlaEnterpriseFilter,
    openCreateSlaPolicy,
    workCalendars,
    workCalendarsLoading,
  }
}
