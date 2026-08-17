import type {
  DepartmentFormPayload,
  DepartmentMemberPageResponse,
  DepartmentNode,
  DepartmentStats,
} from '../types/department'
import type { ManagedUser } from '../types/user'
import { requestApi } from '../shared/api/request'

export type DepartmentListParams = {
  enabled?: boolean
}

export type DepartmentMemberQuery = {
  keyword?: string
  roleCode?: string
  page: number
  size: number
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

export const departmentApi = {
  list(params: DepartmentListParams = {}) {
    return requestApi<DepartmentNode[]>(`/api/v1/departments${toQuery(params)}`)
  },

  stats() {
    return requestApi<DepartmentStats>('/api/v1/departments/stats')
  },

  members(departmentId: number, params: DepartmentMemberQuery) {
    return requestApi<DepartmentMemberPageResponse>(`/api/v1/departments/${departmentId}/members${toQuery(params)}`)
  },

  memberCandidates(departmentId: number, params: DepartmentMemberQuery) {
    return requestApi<DepartmentMemberPageResponse>(`/api/v1/departments/${departmentId}/member-candidates${toQuery(params)}`)
  },

  create(payload: DepartmentFormPayload) {
    return requestApi<DepartmentNode>('/api/v1/departments', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  update(departmentId: number, payload: Omit<DepartmentFormPayload, 'parentId'>) {
    return requestApi<DepartmentNode>(`/api/v1/departments/${departmentId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  move(departmentId: number, parentId: number | null) {
    return requestApi<DepartmentNode>(`/api/v1/departments/${departmentId}/parent`, {
      method: 'PATCH',
      body: JSON.stringify({ parentId }),
    })
  },

  addMembers(departmentId: number, userIds: number[]) {
    return requestApi<ManagedUser[]>(`/api/v1/departments/${departmentId}/members`, {
      method: 'POST',
      body: JSON.stringify({ userIds }),
    })
  },

  removeMember(departmentId: number, userId: number) {
    return requestApi<void>(`/api/v1/departments/${departmentId}/members/${userId}`, {
      method: 'DELETE',
    })
  },

  updateLeader(departmentId: number, leaderUserId: number) {
    return requestApi<DepartmentNode>(`/api/v1/departments/${departmentId}/leader`, {
      method: 'PATCH',
      body: JSON.stringify({ leaderUserId }),
    })
  },

  setEnabled(departmentId: number, enabled: boolean) {
    return requestApi<DepartmentNode>(`/api/v1/departments/${departmentId}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    })
  },
}
