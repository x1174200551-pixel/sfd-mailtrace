import type { SlaPolicyFormState } from '../types/sla-policy'

export const emptySlaPolicyForm: SlaPolicyFormState = {
  id: null,
  enterpriseId: '',
  policyName: '',
  enabled: true,
  defaultPolicy: false,
  responseHours: 4,
  resolveHours: '24',
  warningRemainHours: 1,
  escalateAfterBreachHours: '2',
  responseWarningNotifyEnabled: true,
  responseBreachNotifyEnabled: true,
  responseEscalationNotifyEnabled: false,
  resolveWarningNotifyEnabled: true,
  resolveBreachNotifyEnabled: true,
  resolveEscalationNotifyEnabled: false,
  calendarId: '',
}

export function hoursLabel(value: number | null | undefined, fallback = '未配置') {
  if (value == null || Number.isNaN(value)) return fallback
  return `${value} 工作小时`
}
