import { requestApi } from '../shared/api/request'
import type {
  NotificationTemplate,
  NotificationTemplateListResponse,
  TemplatePreviewResponse,
} from '../types/notification-template'

export type NotificationTemplateQuery = {
  keyword?: string
}

export type NotificationTemplatePreviewPayload = {
  contentTpl: string
  subjectTpl: string
}

export type NotificationTemplateSavePayload = {
  contentTpl: string
  enabled: boolean
  subjectTpl: string
  templateCode: string
  templateName: string
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

export const notificationTemplateApi = {
  list(params: NotificationTemplateQuery = {}) {
    return requestApi<NotificationTemplateListResponse>(`/api/v1/notification-templates${toQuery(params)}`)
  },

  preview(payload: NotificationTemplatePreviewPayload) {
    return requestApi<TemplatePreviewResponse>('/api/v1/notification-templates/preview', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  save(templateId: number | null, payload: NotificationTemplateSavePayload) {
    return requestApi<NotificationTemplate>(
      templateId ? `/api/v1/notification-templates/${templateId}` : '/api/v1/notification-templates',
      {
        method: templateId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      },
    )
  },
}
