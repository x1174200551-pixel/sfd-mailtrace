export type MailFetchLog = {
  id: number
  enterpriseId: number
  mailboxId: number
  mailboxName: string | null
  emailAddress: string | null
  triggerType: string
  startedAt: string
  finishedAt: string | null
  success: boolean
  fetchedCount: number
  createdTicketCount: number
  linkedCount: number
  errorMessage: string | null
  createdAt: string
}

export type MailFetchLogPageResponse = {
  records: MailFetchLog[]
  total: number
  page: number
  size: number
  pages: number
}

export type MailFetchLogStats = {
  totalCount: number
  successCount: number
  failCount: number
  totalCreatedTickets: number
}

export type MailSendLog = {
  id: number
  ticketId: number | null
  enterpriseId: number
  mailboxId: number | null
  sendType: string
  templateId: number | null
  templateType: string | null
  toAddress: string
  subject: string
  contentBody: string | null
  sendStatus: string
  retryCount: number
  maxRetry: number
  errorMessage: string | null
  sentAt: string | null
  createdAt: string
}

export type MailSendLogPageResponse = {
  records: MailSendLog[]
  total: number
  page: number
  size: number
  pages: number
}

export type MailSendLogStats = {
  totalCount: number
  successCount: number
  failCount: number
}
