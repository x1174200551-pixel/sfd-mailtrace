import type { PageResponse } from './common'

export type RoleCode = string

export type ManagedUser = {
  id: number
  account: string
  displayName: string
  email: string
  roleCode: RoleCode
  roleCodes?: string[]
  departmentId?: number | null
  departmentName?: string | null
  departmentPath?: string | null
  enabled: boolean
  lastLoginAt: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type UserSummary = {
  totalUsers: number
  enabledUsers: number
  disabledUsers: number
  adminUsers: number
  agentUsers: number
}

export type UserPageResponse = PageResponse<ManagedUser> & {
  summary: UserSummary
}

export type UserFormState = {
  account: string
  displayName: string
  email: string
  roleCode: RoleCode
  roleCodes: string[]
  departmentId: number | null
  password: string
  enabled: boolean
}

export type UserFormMode = 'create' | 'edit'

export type UserConfirmAction = {
  title: string
  text: string
  actionLabel: string
  user: ManagedUser
  type: 'enable' | 'disable' | 'reset'
} | null
