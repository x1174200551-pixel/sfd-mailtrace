export type CustomerReadonly = {
  id: number | null
  enterpriseId: number
  enterpriseName: string | null
  email: string
  displayName: string | null
  remark: string | null
  ticketCount: number
  lastMailAt: string | null
  createdAt: string | null
}

export type CustomerPageResponse = {
  records: CustomerReadonly[]
  total: number
  page: number
  size: number
  pages: number
}
