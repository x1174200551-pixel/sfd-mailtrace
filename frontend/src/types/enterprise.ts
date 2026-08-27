export type Enterprise = {
  id: number
  enterpriseName: string
  contactName: string | null
  contactEmail: string | null
  contactPhone: string | null
  mailboxCount: number
  ticketCount: number
  enabled: boolean
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
  remark: string
}

export type EnterpriseConfirmAction = {
  enterprise: Enterprise
  nextEnabled: boolean
} | null
