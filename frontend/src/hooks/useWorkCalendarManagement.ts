import { useCallback, useEffect, useMemo, useState } from 'react'
import { message } from 'antd'
import dayjs from 'dayjs'
import { slaPolicyApi } from '../api/sla-policies'
import { holidayApi, workCalendarApi } from '../api/work-calendars'
import { enterpriseApi } from '../api/enterprises'
import type { EnterpriseOption } from '../types/enterprise'
import { emptyHolidayForm, emptyWorkCalendarForm } from '../constants/work-calendars'
import type { SlaPolicy } from '../types/sla-policy'
import type {
  Holiday,
  HolidayFormState,
  HolidayListResponse,
  WorkCalendar,
  WorkCalendarConfirmAction,
  WorkCalendarFormState,
  WorkCalendarListResponse,
} from '../types/work-calendar'
import {
  buildMonthCells,
  calendarPreviewBaseTime,
  parseClockMinutes,
  resolveCalendarSlaExample,
  toHolidayForm,
  toWorkCalendarForm,
} from '../utils/work-calendar'

type UseWorkCalendarManagementParams = {
  activeMenu: string
  canReadHolidays: boolean
  canReadSlaPolicies: boolean
  canReadWorkCalendars: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useWorkCalendarManagement({
  activeMenu,
  canReadHolidays,
  canReadSlaPolicies,
  canReadWorkCalendars,
  handleAuthExpired,
  token,
}: UseWorkCalendarManagementParams) {
  const [workCalendarData, setWorkCalendarData] = useState<WorkCalendarListResponse | null>(null)
  const [workCalendarsLoading, setWorkCalendarsLoading] = useState(false)
  const [workCalendarError, setWorkCalendarError] = useState('')
  const [workCalendarKeyword, setWorkCalendarKeyword] = useState('')
  const [workCalendarDefaultFilter, setWorkCalendarDefaultFilter] = useState('ALL')
  const [workCalendarForm, setWorkCalendarForm] = useState<WorkCalendarFormState>(emptyWorkCalendarForm)
  const [workCalendarDirty, setWorkCalendarDirty] = useState(false)
  const [workCalendarSaving, setWorkCalendarSaving] = useState(false)
  const [workCalendarActionLoading, setWorkCalendarActionLoading] = useState(false)
  const [workCalendarConfirmAction, setWorkCalendarConfirmAction] = useState<WorkCalendarConfirmAction>(null)
  const [calendarSlaPolicies, setCalendarSlaPolicies] = useState<SlaPolicy[]>([])
  const [holidaysData, setHolidaysData] = useState<HolidayListResponse | null>(null)
  const [holidaysLoading, setHolidaysLoading] = useState(false)
  const [holidaysError, setHolidaysError] = useState('')
  const [holidayMonth, setHolidayMonth] = useState('2026-10')
  const [holidayKeyword, setHolidayKeyword] = useState('')
  const [holidayForm, setHolidayForm] = useState<HolidayFormState>(emptyHolidayForm)
  const [holidayDirty, setHolidayDirty] = useState(false)
  const [holidaySaving, setHolidaySaving] = useState(false)
  const [holidayImporting, setHolidayImporting] = useState(false)
  const [calendarPreviewCreatedAt, setCalendarPreviewCreatedAt] = useState(calendarPreviewBaseTime.format('YYYY-MM-DDTHH:mm:ss'))
  const [calendarPreviewResponseHours, setCalendarPreviewResponseHours] = useState('2')
  const [calendarPreviewResolveHours, setCalendarPreviewResolveHours] = useState('16')
  const [workCalendarEnterpriseOptions, setWorkCalendarEnterpriseOptions] = useState<EnterpriseOption[]>([])
  const [workCalendarEnterpriseFilter, setWorkCalendarEnterpriseFilter] = useState('ALL')

  useEffect(() => {
    if (!token || activeMenu !== '工作日历') return
    void enterpriseApi.options().then((options) => {
      setWorkCalendarEnterpriseOptions(options)
      setWorkCalendarEnterpriseFilter((value) => value === 'ALL' && options[0] ? String(options[0].id) : value)
    }).catch(() => setWorkCalendarEnterpriseOptions([]))
  }, [activeMenu, token])

  const fetchWorkCalendarsPage = useCallback(async () => {
    if (!token || activeMenu !== '工作日历') return
    if (!canReadWorkCalendars) {
      setWorkCalendarData(null)
      setWorkCalendarError('当前账号没有工作日历管理权限')
      return
    }

    setWorkCalendarsLoading(true)
    setWorkCalendarError('')
    try {
      const data = await workCalendarApi.list({
        enterpriseId: workCalendarEnterpriseFilter === 'ALL' ? undefined : Number(workCalendarEnterpriseFilter),
        keyword: workCalendarKeyword.trim(),
        defaultCalendar: workCalendarDefaultFilter !== 'ALL' ? workCalendarDefaultFilter : undefined,
      })
      setWorkCalendarData(data)
      const selected = data.records.find((calendar) => calendar.id === workCalendarForm.id) || data.records[0] || null
      if (selected && !workCalendarDirty) {
        setWorkCalendarForm(toWorkCalendarForm(selected))
        setHolidayForm((form) => ({
          ...form,
          calendarId: String(selected.id),
        }))
      }
      if (!selected && !workCalendarDirty) {
        setWorkCalendarForm({ ...emptyWorkCalendarForm, enterpriseId: workCalendarEnterpriseFilter === 'ALL' ? '' : workCalendarEnterpriseFilter })
        setHolidayForm(emptyHolidayForm)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setWorkCalendarError(error instanceof Error ? error.message : '工作日历加载失败')
    } finally {
      setWorkCalendarsLoading(false)
    }
  }, [
    activeMenu,
    canReadWorkCalendars,
    handleAuthExpired,
    token,
    workCalendarDefaultFilter,
    workCalendarDirty,
    workCalendarForm.id,
    workCalendarKeyword,
    workCalendarEnterpriseFilter,
  ])

  useEffect(() => {
    void fetchWorkCalendarsPage()
  }, [fetchWorkCalendarsPage])

  const fetchCalendarSlaPolicies = useCallback(async () => {
    if (!token || activeMenu !== '工作日历' || !canReadSlaPolicies) return
    try {
      const data = await slaPolicyApi.list({ enterpriseId: workCalendarEnterpriseFilter === 'ALL' ? undefined : Number(workCalendarEnterpriseFilter) })
      setCalendarSlaPolicies(data.records)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setWorkCalendarError(error instanceof Error ? error.message : 'SLA 策略引用加载失败')
    }
  }, [activeMenu, canReadSlaPolicies, handleAuthExpired, token, workCalendarEnterpriseFilter])

  useEffect(() => {
    void fetchCalendarSlaPolicies()
  }, [fetchCalendarSlaPolicies])

  const fetchHolidays = useCallback(async () => {
    if (!token || activeMenu !== '工作日历') return
    if (!canReadHolidays) {
      setHolidaysData(null)
      setHolidaysError('当前账号没有节假日管理权限')
      return
    }
    if (!workCalendarForm.id) {
      setHolidaysData({ records: [], summary: { totalCount: 0 } })
      return
    }

    const month = dayjs(`${holidayMonth}-01`)
    setHolidaysLoading(true)
    setHolidaysError('')
    try {
      const data = await holidayApi.list({
        calendarId: workCalendarForm.id,
        dateFrom: month.startOf('month').format('YYYY-MM-DD'),
        dateTo: month.endOf('month').format('YYYY-MM-DD'),
        keyword: holidayKeyword.trim(),
      })
      setHolidaysData(data)
      setHolidayForm((form) => ({
        ...form,
        calendarId: String(workCalendarForm.id),
      }))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setHolidaysError(error instanceof Error ? error.message : '节假日加载失败')
    } finally {
      setHolidaysLoading(false)
    }
  }, [
    activeMenu,
    canReadHolidays,
    handleAuthExpired,
    holidayKeyword,
    holidayMonth,
    token,
    workCalendarForm.id,
  ])

  useEffect(() => {
    void fetchHolidays()
  }, [fetchHolidays])

  const resetWorkCalendarFilters = useCallback(() => {
    setWorkCalendarKeyword('')
    setWorkCalendarDefaultFilter('ALL')
  }, [])

  const updateWorkCalendarForm = useCallback((patch: Partial<WorkCalendarFormState>) => {
    setWorkCalendarForm((value) => ({ ...value, ...patch }))
    setWorkCalendarDirty(true)
    setWorkCalendarError('')
  }, [])

  const selectWorkCalendar = useCallback((calendar: WorkCalendar) => {
    setWorkCalendarForm(toWorkCalendarForm(calendar))
    setWorkCalendarDirty(false)
    setWorkCalendarError('')
    setHolidayForm({
      ...emptyHolidayForm,
      calendarId: String(calendar.id),
      holidayDate: `${holidayMonth}-01`,
    })
    setHolidayDirty(false)
  }, [holidayMonth])

  const openCreateWorkCalendar = useCallback(() => {
    setWorkCalendarForm({
      ...emptyWorkCalendarForm,
      enterpriseId: workCalendarEnterpriseFilter === 'ALL'
        ? (workCalendarEnterpriseOptions[0] ? String(workCalendarEnterpriseOptions[0].id) : '')
        : workCalendarEnterpriseFilter,
    })
    setWorkCalendarDirty(true)
    setWorkCalendarError('')
    setHolidayForm(emptyHolidayForm)
    setHolidayDirty(false)
  }, [workCalendarEnterpriseFilter, workCalendarEnterpriseOptions])

  const buildWorkCalendarPayload = useCallback(() => ({
    enterpriseId: Number(workCalendarForm.enterpriseId),
    calendarName: workCalendarForm.calendarName.trim(),
    timezone: workCalendarForm.timezone.trim() || 'Asia/Shanghai',
    workdays: [...workCalendarForm.workdays].sort((a, b) => a - b),
    workStartTime: workCalendarForm.workStartTime,
    workEndTime: workCalendarForm.workEndTime,
    defaultCalendar: workCalendarForm.defaultCalendar,
  }), [workCalendarForm])

  const saveWorkCalendar = useCallback(async () => {
    if (!token) return
    setWorkCalendarSaving(true)
    setWorkCalendarError('')
    try {
      const saved = await workCalendarApi.save(workCalendarForm.id, buildWorkCalendarPayload())
      setWorkCalendarForm(toWorkCalendarForm(saved))
      setWorkCalendarDirty(false)
      setHolidayForm((form) => ({
        ...form,
        calendarId: String(saved.id),
      }))
      await fetchWorkCalendarsPage()
      message.success('工作日历已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setWorkCalendarError(error instanceof Error ? error.message : '工作日历保存失败')
    } finally {
      setWorkCalendarSaving(false)
    }
  }, [buildWorkCalendarPayload, fetchWorkCalendarsPage, handleAuthExpired, token, workCalendarForm.id])

  const setDefaultWorkCalendar = useCallback(async (calendar: WorkCalendar) => {
    if (!token) return
    setWorkCalendarActionLoading(true)
    setWorkCalendarError('')
    try {
      const saved = await workCalendarApi.setDefault(calendar.id)
      setWorkCalendarForm(toWorkCalendarForm(saved))
      setWorkCalendarDirty(false)
      await fetchWorkCalendarsPage()
      message.success('默认工作日历已更新')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setWorkCalendarError(error instanceof Error ? error.message : '默认工作日历设置失败')
    } finally {
      setWorkCalendarActionLoading(false)
    }
  }, [fetchWorkCalendarsPage, handleAuthExpired, token])

  const openCreateHoliday = useCallback(() => {
    setHolidayForm({
      ...emptyHolidayForm,
      calendarId: workCalendarForm.id ? String(workCalendarForm.id) : '',
      holidayDate: `${holidayMonth}-01`,
    })
    setHolidayDirty(true)
    setHolidaysError('')
  }, [holidayMonth, workCalendarForm.id])

  const updateHolidayForm = useCallback((patch: Partial<HolidayFormState>) => {
    setHolidayForm((value) => ({ ...value, ...patch }))
    setHolidayDirty(true)
    setHolidaysError('')
  }, [])

  const selectHoliday = useCallback((holiday: Holiday) => {
    setHolidayForm(toHolidayForm(holiday))
    setHolidayDirty(false)
    setHolidaysError('')
  }, [])

  const buildHolidayPayload = useCallback(() => ({
    calendarId: holidayForm.calendarId ? Number(holidayForm.calendarId) : null,
    holidayDate: holidayForm.holidayDate,
    holidayName: holidayForm.holidayName.trim(),
  }), [holidayForm])

  const saveHoliday = useCallback(async () => {
    if (!token) return
    setHolidaySaving(true)
    setHolidaysError('')
    try {
      const saved = await holidayApi.save(holidayForm.id, buildHolidayPayload())
      setHolidayForm(toHolidayForm(saved))
      setHolidayDirty(false)
      await fetchHolidays()
      message.success('节假日已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setHolidaysError(error instanceof Error ? error.message : '节假日保存失败')
    } finally {
      setHolidaySaving(false)
    }
  }, [buildHolidayPayload, fetchHolidays, handleAuthExpired, holidayForm.id, token])

  const importNationalHolidays = useCallback(async () => {
    if (!token || !workCalendarForm.id) return
    const importYear = Number(holidayMonth.slice(0, 4)) || dayjs().year()
    setHolidayImporting(true)
    setHolidaysError('')
    try {
      const preset = await holidayApi.nationalPresets(importYear)
      const data = await holidayApi.list({
        calendarId: workCalendarForm.id,
        dateFrom: `${importYear}-01-01`,
        dateTo: `${importYear}-12-31`,
      })
      const existingDates = new Set(data.records.map((holiday) => holiday.holidayDate))
      const missingHolidays = preset.records.filter((holiday) => !existingDates.has(holiday.holidayDate))

      for (const holiday of missingHolidays) {
        await holidayApi.save(null, {
          calendarId: workCalendarForm.id,
          holidayDate: holiday.holidayDate,
          holidayName: holiday.holidayName,
        })
      }

      await fetchHolidays()
      if (missingHolidays.length > 0) {
        message.success(`已从${preset.sourceName}导入 ${missingHolidays.length} 天 ${importYear} 法定节假日`)
      } else {
        message.info(`${importYear} 法定节假日已存在，无需重复导入`)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setHolidaysError(error instanceof Error ? error.message : '法定节假日导入失败')
    } finally {
      setHolidayImporting(false)
    }
  }, [fetchHolidays, handleAuthExpired, holidayMonth, token, workCalendarForm.id])

  const submitWorkCalendarConfirm = useCallback(async () => {
    if (!token || !workCalendarConfirmAction) return
    setWorkCalendarActionLoading(true)
    setWorkCalendarError('')
    setHolidaysError('')
    try {
      if (workCalendarConfirmAction.type === 'delete-calendar') {
        await workCalendarApi.delete(workCalendarConfirmAction.calendar.id)
        if (workCalendarForm.id === workCalendarConfirmAction.calendar.id) {
          setWorkCalendarForm(emptyWorkCalendarForm)
          setWorkCalendarDirty(false)
        }
        await fetchWorkCalendarsPage()
        await fetchHolidays()
        message.success('工作日历已删除')
      } else {
        await holidayApi.delete(workCalendarConfirmAction.holiday.id)
        if (holidayForm.id === workCalendarConfirmAction.holiday.id) {
          openCreateHoliday()
        }
        await fetchHolidays()
        message.success('节假日已删除')
      }
      setWorkCalendarConfirmAction(null)
    } catch (error) {
      if (handleAuthExpired(error)) return
      const fallback = workCalendarConfirmAction.type === 'delete-calendar' ? '工作日历删除失败' : '节假日删除失败'
      if (workCalendarConfirmAction.type === 'delete-calendar') {
        setWorkCalendarError(error instanceof Error ? error.message : fallback)
      } else {
        setHolidaysError(error instanceof Error ? error.message : fallback)
      }
    } finally {
      setWorkCalendarActionLoading(false)
    }
  }, [
    fetchHolidays,
    fetchWorkCalendarsPage,
    handleAuthExpired,
    holidayForm.id,
    openCreateHoliday,
    token,
    workCalendarConfirmAction,
    workCalendarForm.id,
  ])

  const fetchWorkCalendarPageAll = useCallback(() => {
    void fetchWorkCalendarsPage()
    void fetchHolidays()
    void fetchCalendarSlaPolicies()
  }, [fetchCalendarSlaPolicies, fetchHolidays, fetchWorkCalendarsPage])

  const resetCalendarPreview = useCallback(() => {
    setCalendarPreviewCreatedAt(calendarPreviewBaseTime.format('YYYY-MM-DDTHH:mm:ss'))
    setCalendarPreviewResponseHours('2')
    setCalendarPreviewResolveHours('16')
  }, [])

  const workCalendarRecords = useMemo(() => workCalendarData?.records ?? [], [workCalendarData])
  const selectedCalendarForPage = useMemo(
    () => (
      workCalendarForm.id
        ? workCalendarRecords.find((calendar) => calendar.id === workCalendarForm.id) || null
        : null
    ),
    [workCalendarForm.id, workCalendarRecords],
  )
  const holidayRecords = useMemo(() => holidaysData?.records ?? [], [holidaysData])
  const workCalendarStartMinutes = parseClockMinutes(workCalendarForm.workStartTime, 9 * 60)
  const workCalendarEndMinutes = parseClockMinutes(workCalendarForm.workEndTime, 18 * 60)
  const workCalendarTimeInvalid = workCalendarStartMinutes >= workCalendarEndMinutes
  const monthCells = useMemo(
    () => buildMonthCells(holidayMonth, selectedCalendarForPage, holidayRecords),
    [holidayMonth, holidayRecords, selectedCalendarForPage],
  )
  const calendarPreviewCreatedAtValue = dayjs(calendarPreviewCreatedAt)
  const calendarPreviewResponseHoursValue = Math.max(1, Number(calendarPreviewResponseHours) || 1)
  const calendarPreviewResolveHoursValue = Math.max(1, Number(calendarPreviewResolveHours) || 1)
  const calendarSlaExample = resolveCalendarSlaExample(
    selectedCalendarForPage,
    holidayRecords,
    calendarPreviewCreatedAtValue,
    calendarPreviewResponseHoursValue,
    calendarPreviewResolveHoursValue,
  )
  const holidayDateValue = holidayForm.holidayDate ? dayjs(holidayForm.holidayDate) : null
  const holidayMonthValue = holidayMonth ? dayjs(`${holidayMonth}-01`) : null
  const holidayImportYear = Number(holidayMonth.slice(0, 4)) || dayjs().year()

  return {
    calendarPreviewCreatedAtValue,
    calendarPreviewResponseHours,
    calendarPreviewResponseHoursValue,
    calendarPreviewResolveHours,
    calendarPreviewResolveHoursValue,
    calendarSlaExample,
    calendarSlaPolicies,
    fetchCalendarSlaPolicies,
    fetchHolidays,
    fetchWorkCalendarPageAll,
    fetchWorkCalendarsPage,
    holidayDateValue,
    holidayDirty,
    holidayForm,
    holidayImportYear,
    holidayImporting,
    holidayKeyword,
    holidayMonthValue,
    holidayRecords,
    holidaySaving,
    holidaysData,
    holidaysError,
    holidaysLoading,
    importNationalHolidays,
    monthCells,
    openCreateHoliday,
    openCreateWorkCalendar,
    resetCalendarPreview,
    resetWorkCalendarFilters,
    saveHoliday,
    saveWorkCalendar,
    selectHoliday,
    selectedCalendarForPage,
    selectWorkCalendar,
    setCalendarPreviewCreatedAt,
    setCalendarPreviewResponseHours,
    setCalendarPreviewResolveHours,
    setDefaultWorkCalendar,
    setHolidayKeyword,
    setHolidayMonth,
    setWorkCalendarConfirmAction,
    setWorkCalendarDefaultFilter,
    setWorkCalendarKeyword,
    submitWorkCalendarConfirm,
    updateHolidayForm,
    updateWorkCalendarForm,
    workCalendarActionLoading,
    workCalendarConfirmAction,
    workCalendarData,
    workCalendarDefaultFilter,
    workCalendarDirty,
    workCalendarError,
    workCalendarForm,
    workCalendarKeyword,
    workCalendarSaving,
    workCalendarsLoading,
    workCalendarEnterpriseFilter,
    workCalendarEnterpriseOptions,
    setWorkCalendarEnterpriseFilter,
    workCalendarTimeInvalid,
  }
}
