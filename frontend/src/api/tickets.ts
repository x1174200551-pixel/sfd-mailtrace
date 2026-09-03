import { requestApi } from '../shared/api/request'
import type {
  TicketAttachment,
  TicketDetail,
  TicketPageResponse,
  TicketStats,
  TicketUploadedFile,
} from '../types/ticket'

export type TicketListQuery = {
  enterpriseId?: number
  keyword?: string
  mailboxId?: number
  page: number
  size: number
  slaBreached?: boolean
  status?: string
}

export type TicketReplyPayload = {
  attachments: TicketUploadedFile[]
  content: string
  htmlContent: string
  internal: boolean
  replyTemplateId?: number | null
}

export type TicketReplyTemplate = {
  defaultTemplate: boolean
  id: number
  templateName: string
}

export type TicketReplyPreviewPayload = {
  content: string
  htmlContent: string
  replyTemplateId?: number | null
}

export type TicketReplyPreview = {
  contentHtml: string | null
  contentText: string
  contentType: string
  fromAddress: string
  subject: string
  templateId: number
  templateName: string
  templateSource: 'SELECTED' | 'MAILBOX_DEFAULT'
  toAddress: string
}

export type TicketAssignPayload = {
  assigneeId: number
  notifyAssignee: boolean
  reason: string | null
}

export type TicketClosePayload = {
  reason: string | null
}

export type TicketPriorityPayload = {
  priority: string
  reason: string | null
}

export type TicketStatusPayload = {
  reason: string | null
  status: string
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

export const ticketApi = {
  list(params: TicketListQuery) {
    return requestApi<TicketPageResponse>(`/api/v1/tickets${toQuery(params)}`)
  },

  stats() {
    return requestApi<TicketStats>('/api/v1/tickets/stats')
  },

  detail(ticketId: number) {
    return requestApi<TicketDetail>(`/api/v1/tickets/${ticketId}`)
  },

  attachments(ticketId: number) {
    return requestApi<TicketAttachment[]>(`/api/v1/tickets/${ticketId}/attachments`)
  },

  reply(ticketId: number, payload: TicketReplyPayload) {
    return requestApi<TicketDetail>(`/api/v1/tickets/${ticketId}/reply`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  replyTemplates(ticketId: number) {
    return requestApi<TicketReplyTemplate[]>(`/api/v1/tickets/${ticketId}/reply-templates`)
  },

  previewReply(ticketId: number, payload: TicketReplyPreviewPayload) {
    return requestApi<TicketReplyPreview>(`/api/v1/tickets/${ticketId}/reply-preview`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  assign(ticketId: number, payload: TicketAssignPayload) {
    return requestApi<void>(`/api/v1/tickets/${ticketId}/assign`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  close(ticketId: number, payload: TicketClosePayload) {
    return requestApi<void>(`/api/v1/tickets/${ticketId}/close`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updatePriority(ticketId: number, payload: TicketPriorityPayload) {
    return requestApi<void>(`/api/v1/tickets/${ticketId}/priority`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    })
  },

  updateStatus(ticketId: number, payload: TicketStatusPayload) {
    return requestApi<void>(`/api/v1/tickets/${ticketId}/status`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    })
  },

  updateRemark(ticketId: number, remark: string) {
    return requestApi<void>(`/api/v1/tickets/${ticketId}/remark`, {
      method: 'PATCH',
      body: JSON.stringify({ remark }),
    })
  },

  claim(ticketId: number) {
    return requestApi<void>(`/api/v1/tickets/${ticketId}/claim`, {
      method: 'POST',
    })
  },

  deleteAttachment(ticketId: number, attachmentId: number) {
    return requestApi<void>(`/api/v1/tickets/${ticketId}/attachments/${attachmentId}`, {
      method: 'DELETE',
    })
  },

  uploadFile(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return requestApi<TicketUploadedFile>('/api/v1/files/upload', {
      method: 'POST',
      body: formData,
    })
  },
}
