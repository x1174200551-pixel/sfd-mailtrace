import { requestApi } from '../shared/api/request'
import type {
  MailFetchLogPageResponse,
  MailFetchLogStats,
  MailSendLogPageResponse,
  MailSendLogStats,
} from '../types/mail-logs'

export type MailFetchLogQuery = {
  enterpriseId?: string
  mailboxId?: string
  page: number
  size: number
  startFrom?: string
  startTo?: string
  success?: string
}

export type MailSendLogQuery = {
  enterpriseId?: string
  mailboxId?: string
  page: number
  sendStatus?: string
  sendType?: string
  size: number
  startFrom?: string
  startTo?: string
}

function toQuery(params: Record<string, string | number | boolean | null | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, String(value))
    }
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export const mailFetchLogApi = {
  list(params: MailFetchLogQuery) {
    return requestApi<MailFetchLogPageResponse>(`/api/v1/mail-fetch-logs${toQuery(params)}`)
  },

  stats() {
    return requestApi<MailFetchLogStats>('/api/v1/mail-fetch-logs/stats')
  },
}

export const mailSendLogApi = {
  list(params: MailSendLogQuery) {
    return requestApi<MailSendLogPageResponse>(`/api/v1/mail-send/logs${toQuery(params)}`)
  },

  stats() {
    return requestApi<MailSendLogStats>('/api/v1/mail-send/logs/stats')
  },

  pendingCount() {
    return requestApi<number>('/api/v1/mail-send/logs/pending-count')
  },
}
