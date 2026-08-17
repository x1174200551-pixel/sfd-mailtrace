import type { Dayjs } from 'dayjs'
import type { SlaPolicy } from './sla-policy'

export type WorkCalendar = {
  id: number
  calendarName: string
  timezone: string
  workdays: number[]
  workStartTime: string
  workEndTime: string
  defaultCalendar: boolean
  createdAt: string | null
  updatedAt: string | null
}

export type WorkCalendarListResponse = {
  records: WorkCalendar[]
  summary: {
    totalCount: number
    defaultCount: number
  }
}

export type WorkCalendarFormState = {
  id: number | null
  calendarName: string
  timezone: string
  workdays: number[]
  workStartTime: string
  workEndTime: string
  defaultCalendar: boolean
}

export type Holiday = {
  id: number
  calendarId: number
  holidayDate: string
  holidayName: string
  createdAt: string | null
  updatedAt: string | null
}

export type HolidayListResponse = {
  records: Holiday[]
  summary: {
    totalCount: number
  }
}

export type NationalHolidayPresetResponse = {
  year: number
  sourceName: string
  sourceUrl: string
  supportedYears: number[]
  records: Array<{
    holidayDate: string
    holidayName: string
  }>
  makeupWorkdayDates: string[]
}

export type HolidayFormState = {
  id: number | null
  calendarId: string
  holidayDate: string
  holidayName: string
}

export type WorkCalendarConfirmAction =
  | { type: 'delete-calendar'; calendar: WorkCalendar }
  | { type: 'delete-holiday'; holiday: Holiday }
  | null

export type MonthCell = {
  date: Dayjs
  dateKey: string
  inMonth: boolean
  holidayName: string
  isWorkday: boolean
  isToday: boolean
}

export type CalendarSlaExample = {
  startAt: Dayjs
  responseDeadline: Dayjs
  resolveDeadline: Dayjs
}

export type WorkCalendarPolicy = Pick<SlaPolicy, 'id' | 'calendarId'>
