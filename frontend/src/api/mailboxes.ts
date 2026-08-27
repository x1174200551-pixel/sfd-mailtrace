import { requestApi } from '../shared/api/request'
import type { Mailbox, MailboxConnectionTestResponse, MailboxOption, MailboxPageResponse } from '../types/mailbox'

export type MailboxQuery = {
  enterpriseId?: number
  keyword?: string
  page: number
  size: number
  status?: string
}

export type MailboxPayload = {
  enterpriseId: number
  autoReplyEnabled: boolean
  autoReplyTemplateId: number | null
  assignmentNotifyTemplateId: number | null
  agentReplyTemplateId: number | null
  slaWarningTemplateId: number | null
  slaBreachTemplateId: number | null
  defaultAssigneeId: number | null
  slaPolicyId: number | null
  assignmentRuleGroupId: number | null
  assignmentFallbackType: 'NONE' | 'DEFAULT_ASSIGNEE'
  emailAddress: string
  enabled: boolean
  fetchIntervalSec: number
  imapFolder: string
  imapHost: string
  imapPassword: string
  imapPort: number
  imapSslEnabled: boolean
  imapUsername: string
  mailboxName: string
  smtpFromName: string
  smtpHost: string
  smtpPassword: string
  smtpPort: number
  smtpSslEnabled: boolean
  smtpUsername: string
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

export const mailboxApi = {
  list(params: MailboxQuery) {
    return requestApi<MailboxPageResponse>(`/api/v1/mailboxes${toQuery(params)}`)
  },

  options(enterpriseId?: number, operationalOnly = false) {
    return requestApi<MailboxOption[]>(`/api/v1/mailboxes/options${toQuery({ enterpriseId, operationalOnly })}`)
  },

  save(mailboxId: number | null, payload: MailboxPayload) {
    return requestApi<Mailbox>(mailboxId ? `/api/v1/mailboxes/${mailboxId}` : '/api/v1/mailboxes', {
      method: mailboxId ? 'PUT' : 'POST',
      body: JSON.stringify(payload),
    })
  },

  testExisting(mailboxId: number, testType: string) {
    return requestApi<MailboxConnectionTestResponse>(
      `/api/v1/mailboxes/${mailboxId}/test-connection${toQuery({ testType })}`,
      { method: 'POST' },
    )
  },

  testDraft(payload: MailboxPayload, testType: string) {
    return requestApi<MailboxConnectionTestResponse>('/api/v1/mailboxes/test-connection', {
      method: 'POST',
      body: JSON.stringify({ ...payload, testType }),
    })
  },

  setEnabled(mailboxId: number, enabled: boolean) {
    return requestApi<Mailbox>(`/api/v1/mailboxes/${mailboxId}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },

  delete(mailboxId: number) {
    return requestApi<void>(`/api/v1/mailboxes/${mailboxId}`, {
      method: 'DELETE',
    })
  },
}
