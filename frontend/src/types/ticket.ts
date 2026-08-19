export type TicketSummary = {
  id: number
  ticketNo: string
  subject: string
  status: string
  priority: string
  customerEmail: string
  assigneeId: number | null
  assigneeName: string | null
  mailboxId: number
  mailboxName: string | null
  linkSuspect: boolean
  hasReplied: boolean
  createdAt: string
  slaResponseDeadline: string | null
  slaBreached: boolean
}

export type TicketPageResponse = {
  records: TicketSummary[]
  total: number
  page: number
  size: number
  pages: number
}

export type TicketStats = {
  totalCount: number
  activeCount: number
  pendingAssignCount: number
  processingCount: number
  waitingCustomerCount: number
  closedTodayCount: number
  slaOverdueCount: number
}

export type TicketEvent = {
  id: number
  eventType: string
  eventContent: string
  operator: string
  eventAt: string
}

export type TicketMessage = {
  id: number
  direction?: string | null
  messageDirection?: string | null
  fromAddress?: string | null
  toAddress?: string | null
  toAddresses?: string | null
  ccAddresses?: string | null
  bccAddresses?: string | null
  displayName?: string | null
  sentAt?: string | null
  createdAt?: string | null
  contentText?: string | null
  contentHtml?: string | null
  rawHeaders?: string | null
  rawEmlObjectKey?: string | null
  rawEmlSize?: number | null
  contentBody?: string | null
}

export type TicketDetail = {
  id: number
  ticketNo: string
  subject: string
  status: string
  priority: string
  customerEmail: string
  mailboxId: number
  mailboxName: string | null
  assigneeId: number | null
  assigneeName: string | null
  linkSuspect: boolean
  createdAt: string
  updatedAt: string
  firstReplyAt: string | null
  closedAt: string | null
  slaResponseDeadline: string | null
  slaResolveDeadline: string | null
  slaBreached: boolean
  remark: string | null
  messages: TicketMessage[]
  events: TicketEvent[]
}

export type TicketAttachment = {
  id: number
  messageId: number | null
  fileName: string
  fileSize: number
  contentType: string | null
  downloadUrl: string | null
  isInline?: boolean | null
  contentId?: string | null
  uploadedBy: string | null
  createdAt: string
}

export type TicketUploadedFile = {
  objectKey: string
  fileName: string
  fileSize: number
  contentType: string
}
