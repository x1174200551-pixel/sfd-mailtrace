import dayjs from 'dayjs'
import { weekdayNames } from '../constants/work-calendars'
import type { SlaPolicy, SlaPolicyFormState } from '../types/sla-policy'
import type { Holiday, HolidayFormState, WorkCalendar, WorkCalendarFormState } from '../types/work-calendar'

export const slaPreviewBaseTime = dayjs('2026-07-27T15:30:00')
export const calendarPreviewBaseTime = dayjs('2026-10-01T15:30:00')

export function toSlaPolicyForm(policy: SlaPolicy): SlaPolicyFormState {
  return {
    id: policy.id,
    enterpriseId: String(policy.enterpriseId),
    policyName: policy.policyName,
    enabled: policy.enabled,
    defaultPolicy: policy.defaultPolicy,
    responseHours: policy.responseHours,
    resolveHours: policy.resolveHours == null ? '' : String(policy.resolveHours),
    warningRemainHours: policy.warningRemainHours,
    escalateAfterBreachHours: policy.escalateAfterBreachHours == null ? '' : String(policy.escalateAfterBreachHours),
    responseWarningNotifyEnabled: policy.responseWarningNotifyEnabled ?? true,
    responseBreachNotifyEnabled: policy.responseBreachNotifyEnabled ?? true,
    responseEscalationNotifyEnabled: policy.responseEscalationNotifyEnabled ?? false,
    resolveWarningNotifyEnabled: policy.resolveWarningNotifyEnabled ?? true,
    resolveBreachNotifyEnabled: policy.resolveBreachNotifyEnabled ?? true,
    resolveEscalationNotifyEnabled: policy.resolveEscalationNotifyEnabled ?? false,
    calendarId: String(policy.calendarId),
  }
}

export function toWorkCalendarForm(calendar: WorkCalendar): WorkCalendarFormState {
  return {
    id: calendar.id,
    enterpriseId: String(calendar.enterpriseId),
    calendarName: calendar.calendarName,
    timezone: calendar.timezone,
    workdays: calendar.workdays,
    workStartTime: calendar.workStartTime,
    workEndTime: calendar.workEndTime,
    defaultCalendar: calendar.defaultCalendar,
  }
}

export function toHolidayForm(holiday: Holiday): HolidayFormState {
  return {
    id: holiday.id,
    calendarId: String(holiday.calendarId),
    holidayDate: holiday.holidayDate,
    holidayName: holiday.holidayName,
  }
}

export function workdayLabel(workdays?: number[]) {
  if (!workdays || workdays.length === 0) return '未配置'
  const sorted = [...workdays].sort((a, b) => a - b)
  if (sorted.join(',') === '1,2,3,4,5') return '周一至周五'
  if (sorted.join(',') === '1,2,3,4,5,6,7') return '周一至周日'
  return sorted.map((day) => weekdayNames[day - 1] || `周${day}`).join('、')
}

export function parseClockMinutes(value: string | undefined, fallback: number) {
  const [hourText, minuteText] = (value || '').split(':')
  const hour = Number(hourText)
  const minute = Number(minuteText || 0)
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return fallback
  return hour * 60 + minute
}

function calendarDayNumber(value: dayjs.Dayjs) {
  const day = value.day()
  return day === 0 ? 7 : day
}

function withClockMinutes(value: dayjs.Dayjs, minutes: number) {
  return value.hour(Math.floor(minutes / 60)).minute(minutes % 60).second(0).millisecond(0)
}

function nextWorkStart(value: dayjs.Dayjs, calendar: WorkCalendar) {
  const startMinutes = parseClockMinutes(calendar.workStartTime, 9 * 60)
  const endMinutes = parseClockMinutes(calendar.workEndTime, 18 * 60)
  let cursor = value
  for (let i = 0; i < 14; i += 1) {
    const isWorkday = calendar.workdays.includes(calendarDayNumber(cursor))
    const startAt = withClockMinutes(cursor, startMinutes)
    const endAt = withClockMinutes(cursor, endMinutes)
    if (!isWorkday || !endAt.isAfter(startAt)) {
      cursor = cursor.add(1, 'day').startOf('day')
      continue
    }
    if (cursor.isBefore(startAt)) return startAt
    if (cursor.isBefore(endAt)) return cursor
    cursor = cursor.add(1, 'day').startOf('day')
  }
  return value
}

function nextWorkStartWithHolidays(value: dayjs.Dayjs, calendar: WorkCalendar, holidayDates: Set<string>) {
  const startMinutes = parseClockMinutes(calendar.workStartTime, 9 * 60)
  const endMinutes = parseClockMinutes(calendar.workEndTime, 18 * 60)
  let cursor = value
  for (let i = 0; i < 30; i += 1) {
    const dateKey = cursor.format('YYYY-MM-DD')
    const isWorkday = calendar.workdays.includes(calendarDayNumber(cursor)) && !holidayDates.has(dateKey)
    const startAt = withClockMinutes(cursor, startMinutes)
    const endAt = withClockMinutes(cursor, endMinutes)
    if (!isWorkday || !endAt.isAfter(startAt)) {
      cursor = cursor.add(1, 'day').startOf('day')
      continue
    }
    if (cursor.isBefore(startAt)) return startAt
    if (cursor.isBefore(endAt)) return cursor
    cursor = cursor.add(1, 'day').startOf('day')
  }
  return value
}

function addWorkHours(value: dayjs.Dayjs, hours: number, calendar: WorkCalendar | null) {
  if (!calendar) return value.add(hours, 'hour')
  const endMinutes = parseClockMinutes(calendar.workEndTime, 18 * 60)
  let remainingMinutes = Math.max(0, Math.round(hours * 60))
  let cursor = nextWorkStart(value, calendar)

  for (let i = 0; i < 100 && remainingMinutes > 0; i += 1) {
    const endAt = withClockMinutes(cursor, endMinutes)
    const availableMinutes = Math.max(0, endAt.diff(cursor, 'minute'))
    if (remainingMinutes <= availableMinutes) {
      return cursor.add(remainingMinutes, 'minute')
    }
    remainingMinutes -= availableMinutes
    cursor = nextWorkStart(cursor.add(1, 'day').startOf('day'), calendar)
  }

  return cursor
}

function addWorkHoursWithHolidays(value: dayjs.Dayjs, hours: number, calendar: WorkCalendar | null, holidayDates: Set<string>) {
  if (!calendar) return value.add(hours, 'hour')
  const endMinutes = parseClockMinutes(calendar.workEndTime, 18 * 60)
  let remainingMinutes = Math.max(0, Math.round(hours * 60))
  let cursor = nextWorkStartWithHolidays(value, calendar, holidayDates)

  for (let i = 0; i < 120 && remainingMinutes > 0; i += 1) {
    const endAt = withClockMinutes(cursor, endMinutes)
    const availableMinutes = Math.max(0, endAt.diff(cursor, 'minute'))
    if (remainingMinutes <= availableMinutes) {
      return cursor.add(remainingMinutes, 'minute')
    }
    remainingMinutes -= availableMinutes
    cursor = nextWorkStartWithHolidays(cursor.add(1, 'day').startOf('day'), calendar, holidayDates)
  }

  return cursor
}

export function resolveSlaPreview(form: SlaPolicyFormState, calendar: WorkCalendar | null) {
  const responseHours = Math.max(1, Number(form.responseHours) || 1)
  const resolveHours = form.resolveHours.trim() ? Math.max(1, Number(form.resolveHours) || responseHours) : null
  const warningHours = Math.max(1, Number(form.warningRemainHours) || 1)
  const escalateHours = form.escalateAfterBreachHours.trim()
    ? Math.max(1, Number(form.escalateAfterBreachHours) || 1)
    : null
  const responseDeadline = addWorkHours(slaPreviewBaseTime, responseHours, calendar)
  const resolveDeadline = resolveHours == null ? null : addWorkHours(slaPreviewBaseTime, resolveHours, calendar)
  const responseWarningAt = responseDeadline.subtract(warningHours, 'hour')
  const resolveWarningAt = resolveDeadline ? resolveDeadline.subtract(warningHours, 'hour') : null
  const responseEscalationAt = escalateHours == null ? null : addWorkHours(responseDeadline, escalateHours, calendar)
  const resolveEscalationAt = resolveDeadline && escalateHours != null
    ? addWorkHours(resolveDeadline, escalateHours, calendar)
    : null

  return {
    responseDeadline,
    resolveDeadline,
    responseWarningAt,
    responseEscalationAt,
    resolveWarningAt,
    resolveEscalationAt,
  }
}

export function resolveCalendarSlaExample(
  calendar: WorkCalendar | null,
  holidays: Holiday[],
  createdAt: dayjs.Dayjs,
  responseHours: number,
  resolveHours: number,
) {
  const holidayDates = new Set(holidays.map((holiday) => holiday.holidayDate))
  const startAt = calendar ? nextWorkStartWithHolidays(createdAt, calendar, holidayDates) : createdAt
  return {
    startAt,
    responseDeadline: addWorkHoursWithHolidays(createdAt, responseHours, calendar, holidayDates),
    resolveDeadline: addWorkHoursWithHolidays(createdAt, resolveHours, calendar, holidayDates),
  }
}

export function buildMonthCells(month: string, calendar: WorkCalendar | null, holidays: Holiday[]) {
  const monthStart = dayjs(`${month}-01`)
  const gridStart = monthStart.subtract(calendarDayNumber(monthStart) - 1, 'day')
  const holidayMap = new Map(holidays.map((holiday) => [holiday.holidayDate, holiday.holidayName]))

  return Array.from({ length: 42 }, (_value, index) => {
    const date = gridStart.add(index, 'day')
    const dateKey = date.format('YYYY-MM-DD')
    const holidayName = holidayMap.get(dateKey) || ''
    const inMonth = date.isSame(monthStart, 'month')
    const isWorkday = Boolean(calendar?.workdays.includes(calendarDayNumber(date))) && !holidayName
    return {
      date,
      dateKey,
      inMonth,
      holidayName,
      isWorkday,
      isToday: date.isSame(dayjs(), 'day'),
    }
  })
}
