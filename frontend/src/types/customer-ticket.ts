export type CustomerTicketEmail = {
  fromAddress: string | null
  toAddress: string | null
  subject: string | null
  sentAt: string | null
  contentText: string | null
  contentHtml: string | null
}

export type CustomerTicketMessage = {
  direction: 'INBOUND' | 'OUTBOUND' | string
  fromAddress: string | null
  toAddress: string | null
  subject: string | null
  sentAt: string | null
  contentText: string | null
  contentHtml: string | null
}

export type CustomerTicketTimelineItem = {
  stage: string
  title: string
  content: string
  badge: string
  eventAt: string | null
}

export type CustomerTicketDetail = {
  ticketNo: string
  subject: string
  status: string
  statusLabel: string
  customerEmail: string
  createdAt: string
  updatedAt: string
  firstReplyAt: string | null
  closedAt: string | null
  slaResponseDeadline: string | null
  slaResolveDeadline: string | null
  slaBreached: boolean
  customerAccessExpiresAt: string | null
  email: CustomerTicketEmail | null
  messages: CustomerTicketMessage[]
  timeline: CustomerTicketTimelineItem[]
}
