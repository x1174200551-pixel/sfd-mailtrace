import type { MailboxConnectionStatus, MailboxFormState, MailboxStepKey } from '../types/mailbox'

export const emptyMailboxForm: MailboxFormState = {
  id: null,
  mailboxName: '',
  emailAddress: '',
  enabled: true,
  defaultAssigneeId: '',
  imapHost: '',
  imapPort: 993,
  imapSslEnabled: true,
  imapUsername: '',
  imapPassword: '',
  imapFolder: 'INBOX',
  fetchIntervalSec: 120,
  smtpHost: '',
  smtpPort: 587,
  smtpSslEnabled: true,
  smtpUsername: '',
  smtpPassword: '',
  smtpFromName: '',
  autoReplyEnabled: true,
  autoReplyTemplateId: '',
}

export const mailboxSteps: Array<{ key: MailboxStepKey; label: string }> = [
  { key: 'basic', label: '基础信息' },
  { key: 'imap', label: '收信配置' },
  { key: 'smtp', label: '发信配置' },
  { key: 'reply', label: '自动回复' },
  { key: 'test', label: '连接测试' },
]

export function mailboxStatusLabel(status: MailboxConnectionStatus) {
  if (status === 'OK') return '正常'
  if (status === 'ERROR') return '异常'
  return '未知'
}
