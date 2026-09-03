import type { Dayjs } from 'dayjs'

export type SlaPolicy = {
  id: number
  enterpriseId: number
  policyName: string
  enabled: boolean
  defaultPolicy: boolean
  responseHours: number
  resolveHours: number | null
  warningRemainHours: number
  escalateAfterBreachHours: number | null
  responseWarningNotifyEnabled: boolean
  responseBreachNotifyEnabled: boolean
  responseEscalationNotifyEnabled: boolean
  resolveWarningNotifyEnabled: boolean
  resolveBreachNotifyEnabled: boolean
  resolveEscalationNotifyEnabled: boolean
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
  enterpriseId: string
  policyName: string
  enabled: boolean
  defaultPolicy: boolean
  responseHours: number
  resolveHours: string
  warningRemainHours: number
  escalateAfterBreachHours: string
  responseWarningNotifyEnabled: boolean
  responseBreachNotifyEnabled: boolean
  responseEscalationNotifyEnabled: boolean
  resolveWarningNotifyEnabled: boolean
  resolveBreachNotifyEnabled: boolean
  resolveEscalationNotifyEnabled: boolean
  calendarId: string
}

export type SlaPolicyConfirmAction = {
  type: 'delete'
  policy: SlaPolicy
} | null

export type SlaWorkCalendar = {
  id: number
  enterpriseId?: number
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
  responseWarningAt: Dayjs
  responseEscalationAt: Dayjs | null
  resolveWarningAt: Dayjs | null
  resolveEscalationAt: Dayjs | null
}
