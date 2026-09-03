export type Enterprise = {
  id: number
  enterpriseName: string
  contactName: string | null
  contactEmail: string | null
  contactPhone: string | null
  mailboxCount: number
  ticketCount: number
  enabled: boolean
  feishuNotifyEnabled: boolean
  feishuGroupName: string | null
  feishuConfigured: boolean
  feishuConnectionStatus: 'UNCONFIGURED' | 'UNTESTED' | 'OK' | 'ERROR' | string
  feishuLastTestAt: string | null
  feishuLastError: string | null
  remark: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type EnterpriseOption = Pick<Enterprise, 'id' | 'enterpriseName' | 'enabled'>

export type EnterpriseListResponse = {
  records: Enterprise[]
  total: number
  page: number
  size: number
  pages: number
  totalCount: number
  enabledCount: number
  disabledCount: number
}

export type EnterpriseFormState = {
  id: number | null
  enterpriseName: string
  contactName: string
  contactEmail: string
  contactPhone: string
  enabled: boolean
  feishuNotifyEnabled: boolean
  feishuGroupName: string
  feishuWebhookUrl: string
  feishuSigningSecret: string
  clearFeishuConfig: boolean
  remark: string
}

export type FeishuGroupTestResponse = {
  accepted: boolean
  message: string
  sendLogId: number | null
}

export type EnterpriseConfirmAction = {
  enterprise: Enterprise
  nextEnabled: boolean
} | null
