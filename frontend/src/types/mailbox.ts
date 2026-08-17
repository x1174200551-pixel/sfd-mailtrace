export type MailboxConnectionStatus = 'UNKNOWN' | 'OK' | 'ERROR'
export type MailboxStepKey = 'basic' | 'imap' | 'smtp' | 'reply' | 'test'

export type Mailbox = {
  id: number
  mailboxName: string
  emailAddress: string
  enabled: boolean
  defaultAssigneeId: number | null
  defaultAssigneeName: string | null
  imapHost: string
  imapPort: number
  imapSslEnabled: boolean
  imapUsername: string
  imapFolder: string
  fetchIntervalSec: number
  smtpHost: string
  smtpPort: number
  smtpSslEnabled: boolean
  smtpUsername: string
  smtpFromName: string | null
  autoReplyEnabled: boolean
  autoReplyTemplateId: number | null
  lastFetchAt: string | null
  connectionStatus: MailboxConnectionStatus
  createdAt: string | null
  updatedAt: string | null
}

export type MailboxSummary = {
  totalMailboxes: number
  enabledMailboxes: number
  disabledMailboxes: number
  okMailboxes: number
  errorMailboxes: number
  unknownMailboxes: number
}

export type MailboxPageResponse = {
  records: Mailbox[]
  total: number
  page: number
  size: number
  pages: number
  summary: MailboxSummary
}

export type MailboxFormState = {
  id: number | null
  mailboxName: string
  emailAddress: string
  enabled: boolean
  defaultAssigneeId: string
  imapHost: string
  imapPort: number
  imapSslEnabled: boolean
  imapUsername: string
  imapPassword: string
  imapFolder: string
  fetchIntervalSec: number
  smtpHost: string
  smtpPort: number
  smtpSslEnabled: boolean
  smtpUsername: string
  smtpPassword: string
  smtpFromName: string
  autoReplyEnabled: boolean
  autoReplyTemplateId: string
}

export type MailboxConnectionTestResponse = {
  success: boolean
  connectionStatus: MailboxConnectionStatus
  imapSuccess: boolean
  imapMessage: string
  smtpSuccess: boolean
  smtpMessage: string
  testedAt: string
}

export type MailboxConfirmAction = {
  title: string
  text: string
  actionLabel: string
  type: 'enable' | 'disable' | 'delete'
  mailbox: Mailbox
} | null
