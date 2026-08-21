import { requestApi } from '../shared/api/request'
import type { TicketNumberRule, TicketRulePayload } from '../types/system-config'

export const systemConfigApi = {
  ticketNumberRule() {
    return requestApi<TicketNumberRule>('/api/v1/sys-params/ticket-number-rule')
  },

  previewTicketNumberRule(payload: TicketRulePayload) {
    return requestApi<TicketNumberRule>('/api/v1/sys-params/ticket-number-rule/preview', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  saveTicketNumberRule(payload: TicketRulePayload) {
    return requestApi<TicketNumberRule>('/api/v1/sys-params/ticket-number-rule', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },
}
