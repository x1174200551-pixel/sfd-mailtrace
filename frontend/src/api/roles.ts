import { requestApi } from '../shared/api/request'
import type { ManagedRole, PermissionTreeNode, RoleDataScope, RoleListResponse } from '../types/role'

export type RoleQuery = {
  enabled?: string
  keyword?: string
}

export type RoleBasePayload = {
  enabled: boolean
  roleDesc: string
  roleName: string
}

export type RolePermissionPayload = {
  dataScopes: RoleDataScope[]
  permissionCodes: string[]
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

export const roleApi = {
  list(params: RoleQuery = {}) {
    return requestApi<RoleListResponse>(`/api/v1/roles${toQuery(params)}`)
  },

  permissions() {
    return requestApi<PermissionTreeNode[]>('/api/v1/roles/permissions')
  },

  create(payload: RoleBasePayload) {
    return requestApi<ManagedRole>('/api/v1/roles', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  update(roleId: number, payload: RoleBasePayload) {
    return requestApi<ManagedRole>(`/api/v1/roles/${roleId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  updatePermissions(roleId: number, payload: RolePermissionPayload) {
    return requestApi<ManagedRole>(`/api/v1/roles/${roleId}/permissions`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  setEnabled(roleId: number, enabled: boolean) {
    return requestApi<ManagedRole>(`/api/v1/roles/${roleId}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },
}
