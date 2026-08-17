export type TicketNumberRule = {
  enabled: boolean
  prefix: string
  dateFormat: string
  seqLength: number
  separator: string
  description: string
  todayDate: string
  dateKey: string
  usedSeq: number
  nextSeq: string
  nextTicketNo: string
  subjectPreview: string
  updatedAt: string | null
}

export type TicketRuleFormState = {
  enabled: boolean
  prefix: string
  dateFormat: string
  seqLength: number
  separator: string
  description: string
}

export type SystemGroupKey = 'ticket' | 'mail' | 'notice' | 'security'

export type SystemGroup = {
  key: SystemGroupKey
  title: string
  summary: string
  detail: string
  owner: string
}
