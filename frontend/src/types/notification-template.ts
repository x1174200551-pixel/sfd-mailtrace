export type TemplateVariable = {
  key: string
  label: string
  sampleValue: string
}

export type NotificationTemplate = {
  id: number
  templateCode: string
  templateName: string
  subjectTpl: string
  contentTpl: string
  enabled: boolean
  updatedAt: string | null
}

export type NotificationTemplateSummary = {
  totalTemplates: number
  enabledTemplates: number
  disabledTemplates: number
  availableVariables: number
}

export type NotificationTemplateListResponse = {
  records: NotificationTemplate[]
  summary: NotificationTemplateSummary
  variables: TemplateVariable[]
}

export type TemplateFormState = {
  id: number | null
  templateCode: string
  templateName: string
  subjectTpl: string
  contentTpl: string
  enabled: boolean
}

export type TemplatePreviewResponse = {
  subject: string
  content: string
}
