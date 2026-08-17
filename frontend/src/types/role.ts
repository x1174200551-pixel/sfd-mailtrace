export type RoleDataScope = {
  resourceType: string
  scopeCode: string
  scopeDesc: string | null
}

export type ManagedRole = {
  id: number
  roleCode: string
  roleName: string
  roleDesc: string | null
  systemRole: boolean
  enabled: boolean
  sortOrder: number | null
  permissionCodes: string[]
  dataScopes: RoleDataScope[]
  userCount: number
  createdAt: string | null
  updatedAt: string | null
}

export type RoleListResponse = {
  records: ManagedRole[]
  total: number
  enabledCount: number
  systemCount: number
  customCount: number
  permissionTotal: number
  userTotal: number
}

export type PermissionTreeNode = {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: string
  moduleCode: string | null
  parentId: number | null
  children: PermissionTreeNode[]
}

export type RoleFormState = {
  roleName: string
  roleDesc: string
  enabled: boolean
  permissionCodes: string[]
  dataScopes: RoleDataScope[]
}
