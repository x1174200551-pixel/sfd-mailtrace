import { requestApi } from '../shared/api/request'
import type { CustomerTicketDetail } from '../types/customer-ticket'

export const customerTicketApi = {
  verify(ticketNo: string, accessCode: string) {
    return requestApi<CustomerTicketDetail>(`/api/v1/customer-tickets/${encodeURIComponent(ticketNo)}/verify`, {
      method: 'POST',
      skipAuth: true,
      body: JSON.stringify({ accessCode }),
    })
  },
}
