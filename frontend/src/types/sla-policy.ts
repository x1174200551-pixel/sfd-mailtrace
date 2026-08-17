import type { Dayjs } from 'dayjs'

export type SlaPolicy = {
  id: number
  policyName: string
  enabled: boolean
  defaultPolicy: boolean
  responseHours: number
  resolveHours: number | null
  warningRemainHours: number
  escalateAfterBreachHours: number | null
  calendarId: number
  createdAt: string | null
  updatedAt: string | null
}

export type SlaPolicySummary = {
  totalCount: number
  enabledCount: number
  disabledCount: number
  defaultCount: number
}

export type SlaPolicyListResponse = {
  records: SlaPolicy[]
  summary: SlaPolicySummary
}

export type SlaPolicyFormState = {
  id: number | null
  policyName: string
  enabled: boolean
  defaultPolicy: boolean
  responseHours: number
  resolveHours: string
  warningRemainHours: number
  escalateAfterBreachHours: string
  calendarId: string
}

export type SlaPolicyConfirmAction = {
  type: 'delete'
  policy: SlaPolicy
} | null

export type SlaWorkCalendar = {
  id: number
  calendarName: string
  timezone: string
  workdays: number[]
  workStartTime: string
  workEndTime: string
  defaultCalendar: boolean
}

export type SlaPolicyPreview = {
  responseDeadline: Dayjs
  resolveDeadline: Dayjs | null
  warningAt: Dayjs
  escalateAt: Dayjs | null
}
