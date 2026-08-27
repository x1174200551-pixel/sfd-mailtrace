import { requestApi } from '../shared/api/request'
import type { ManagedUser, RoleCode, UserDataGrantDetail, UserPageResponse } from '../types/user'

export type UserQuery = {
  enabled?: string | boolean
  keyword?: string
  page: number
  roleCode?: string
  size: number
}

export type UserCreatePayload = {
  account: string
  departmentId: number | null
  displayName: string
  email: string
  enabled: boolean
  enterpriseIds: number[]
  mailboxIds: number[]
  password: string
  roleCode: RoleCode
}

export type UserUpdatePayload = {
  departmentId: number | null
  displayName: string
  email: string
  enabled: boolean
  enterpriseIds: number[]
  mailboxIds: number[]
  roleCode: RoleCode
}

export type AssignableUserQuery = {
  assignmentRuleGroupId?: number
  enterpriseId?: number
  mailboxId?: number
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

export const userApi = {
  list(params: UserQuery) {
    return requestApi<UserPageResponse>(`/api/v1/users${toQuery(params)}`)
  },

  assignableOptions(params: AssignableUserQuery) {
    return requestApi<ManagedUser[]>(`/api/v1/users/options${toQuery(params)}`)
  },

  create(payload: UserCreatePayload) {
    return requestApi<ManagedUser>('/api/v1/users', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  update(userId: number, payload: UserUpdatePayload) {
    return requestApi<ManagedUser>(`/api/v1/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  resetPassword(userId: number, password: string) {
    return requestApi<void>(`/api/v1/users/${userId}/reset-password`, {
      method: 'POST',
      body: JSON.stringify({ password }),
    })
  },

  setEnabled(userId: number, enabled: boolean) {
    return requestApi<ManagedUser>(`/api/v1/users/${userId}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },

  getDataGrants(userId: number) {
    return requestApi<UserDataGrantDetail>(`/api/v1/users/${userId}/data-grants`)
  },

  saveDataGrants(userId: number, enterpriseIds: number[], mailboxIds: number[]) {
    return requestApi<UserDataGrantDetail>(`/api/v1/users/${userId}/data-grants`, {
      method: 'PUT',
      body: JSON.stringify({ enterpriseIds, mailboxIds }),
    })
  },
}
