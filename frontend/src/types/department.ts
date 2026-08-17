import type { PageResponse } from './common'
import type { ManagedUser } from './user'

export type DepartmentNode = {
  id: number
  parentId: number | null
  deptCode: string
  deptName: string
  deptDesc: string | null
  leaderUserId: number | null
  leaderDisplayName: string | null
  deptPath: string | null
  enabled: boolean
  sortOrder: number | null
  memberCount: number
  createdAt: string | null
  updatedAt: string | null
  children: DepartmentNode[]
}

export type DepartmentStats = {
  totalDepartments: number
  enabledDepartments: number
  disabledDepartments: number
  leaderCount: number
  memberCount: number
  unassignedUserCount: number
}

export type DepartmentMemberPageResponse = PageResponse<ManagedUser>

export type DepartmentFormPayload = {
  parentId?: number | null
  deptName: string
  deptDesc: string
  leaderUserId?: number | null
  enabled: boolean
}

export type DepartmentFormState = {
  parentId: number
  deptName: string
  deptDesc: string
  leaderUserId: number
  enabled: boolean
}

export type DepartmentFormMode = 'create' | 'edit'
