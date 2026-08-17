import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { roleApi } from '../api/roles'
import { dataScopeDesc } from '../constants/data-scopes'
import { emptyRoleForm } from '../constants/roles'
import type { ManagedRole, PermissionTreeNode, RoleFormState, RoleListResponse } from '../types/role'

type UseRoleManagementParams = {
  activeMenu: string
  canEnableRoles: boolean
  canReadRoles: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

export function useRoleManagement({
  activeMenu,
  canEnableRoles,
  canReadRoles,
  handleAuthExpired,
  token,
}: UseRoleManagementParams) {
  const [roleKeyword, setRoleKeyword] = useState('')
  const [roleEnabledFilter, setRoleEnabledFilter] = useState('ALL')
  const [rolesData, setRolesData] = useState<RoleListResponse | null>(null)
  const [rolesLoading, setRolesLoading] = useState(false)
  const [rolesError, setRolesError] = useState('')
  const [permissionTree, setPermissionTree] = useState<PermissionTreeNode[]>([])
  const [permissionTreeLoading, setPermissionTreeLoading] = useState(false)
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null)
  const [roleForm, setRoleForm] = useState<RoleFormState>(emptyRoleForm)
  const [roleDraftMode, setRoleDraftMode] = useState<'create' | 'edit'>('edit')
  const [roleSaving, setRoleSaving] = useState(false)
  const [rolePermissionSaving, setRolePermissionSaving] = useState(false)

  const selectedRoleIdRef = useRef<number | null>(null)
  const roleDraftModeRef = useRef<'create' | 'edit'>('edit')

  const selectedRole = useMemo(
    () => rolesData?.records.find((role) => role.id === selectedRoleId) ?? null,
    [rolesData, selectedRoleId],
  )
  const selectedRoleReadonly = roleDraftMode === 'edit' && Boolean(selectedRole?.systemRole)

  const hydrateRoleForm = useCallback((role: ManagedRole) => {
    setRoleForm({
      roleName: role.roleName,
      roleDesc: role.roleDesc || '',
      enabled: role.enabled,
      permissionCodes: role.permissionCodes || [],
      dataScopes: role.dataScopes?.length ? role.dataScopes : emptyRoleForm.dataScopes,
    })
    roleDraftModeRef.current = 'edit'
    selectedRoleIdRef.current = role.id
    setRoleDraftMode('edit')
    setSelectedRoleId(role.id)
  }, [])

  const fetchRoles = useCallback(async (preferredRoleId?: number) => {
    if (!token || (activeMenu !== '角色管理' && activeMenu !== '用户管理')) return
    if (!canReadRoles) {
      if (activeMenu === '角色管理') {
        setRolesData(null)
        setRolesError('当前账号没有角色管理权限')
      }
      return
    }
    setRolesLoading(true)
    setRolesError('')
    try {
      const data = await roleApi.list({
        enabled: roleEnabledFilter !== 'ALL' ? roleEnabledFilter : undefined,
        keyword: roleKeyword.trim(),
      })
      setRolesData(data)
      const targetRoleId = preferredRoleId ?? selectedRoleIdRef.current
      const current = data.records.find((role) => role.id === targetRoleId)
      if (activeMenu === '角色管理') {
        if (current) {
          hydrateRoleForm(current)
        } else if (!preferredRoleId && roleDraftModeRef.current !== 'create' && data.records[0]) {
          hydrateRoleForm(data.records[0])
        } else if (!preferredRoleId && roleDraftModeRef.current !== 'create') {
          selectedRoleIdRef.current = null
          setSelectedRoleId(null)
          setRoleForm(emptyRoleForm)
        }
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '角色列表加载失败')
    } finally {
      setRolesLoading(false)
    }
  }, [activeMenu, canReadRoles, handleAuthExpired, hydrateRoleForm, roleEnabledFilter, roleKeyword, token])

  const fetchPermissionTree = useCallback(async () => {
    if (!token || activeMenu !== '角色管理' || !canReadRoles) return
    setPermissionTreeLoading(true)
    try {
      const data = await roleApi.permissions()
      setPermissionTree(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '权限树加载失败')
    } finally {
      setPermissionTreeLoading(false)
    }
  }, [activeMenu, canReadRoles, handleAuthExpired, token])

  useEffect(() => {
    if (activeMenu === '角色管理' || activeMenu === '用户管理') {
      void fetchRoles()
    }
  }, [activeMenu, fetchRoles])

  useEffect(() => {
    if (activeMenu === '角色管理') {
      void fetchPermissionTree()
    }
  }, [activeMenu, fetchPermissionTree])

  const openCreateRole = useCallback(() => {
    roleDraftModeRef.current = 'create'
    selectedRoleIdRef.current = null
    setRoleDraftMode('create')
    setSelectedRoleId(null)
    setRoleForm(emptyRoleForm)
    setRolesError('')
  }, [])

  const selectRole = useCallback((role: ManagedRole) => {
    hydrateRoleForm(role)
    setRolesError('')
  }, [hydrateRoleForm])

  const updateRoleForm = useCallback((patch: Partial<RoleFormState>) => {
    setRoleForm((value) => ({ ...value, ...patch }))
  }, [])

  const toggleRolePermission = useCallback((permissionCode: string, checked: boolean) => {
    setRoleForm((value) => {
      const next = new Set(value.permissionCodes)
      if (checked) {
        next.add(permissionCode)
      } else {
        next.delete(permissionCode)
      }
      return { ...value, permissionCodes: Array.from(next) }
    })
  }, [])

  const updateRoleScope = useCallback((resourceType: string, scopeCode: string) => {
    setRoleForm((value) => ({
      ...value,
      dataScopes: ['TICKET', 'CUSTOMER', 'DASHBOARD'].map((resource) => {
        const nextScope = resource === resourceType ? scopeCode : (value.dataScopes.find((scope) => scope.resourceType === resource)?.scopeCode || 'SELF')
        return {
          resourceType: resource,
          scopeCode: nextScope,
          scopeDesc: dataScopeDesc(resource, nextScope),
        }
      }),
    }))
  }, [])

  const submitRoleBase = useCallback(async () => {
    if (!token) return
    setRoleSaving(true)
    setRolesError('')
    try {
      let saved: ManagedRole
      if (roleDraftMode === 'create') {
        saved = await roleApi.create({
          roleName: roleForm.roleName,
          roleDesc: roleForm.roleDesc,
          enabled: roleForm.enabled,
        })
        if (roleForm.permissionCodes.length > 0) {
          saved = await roleApi.updatePermissions(saved.id, {
            permissionCodes: roleForm.permissionCodes,
            dataScopes: roleForm.dataScopes,
          })
        }
      } else if (selectedRole) {
        saved = await roleApi.update(selectedRole.id, {
          roleName: roleForm.roleName,
          roleDesc: roleForm.roleDesc,
          enabled: roleForm.enabled,
        })
      } else {
        return
      }
      roleDraftModeRef.current = 'edit'
      selectedRoleIdRef.current = saved.id
      setRoleDraftMode('edit')
      setSelectedRoleId(saved.id)
      hydrateRoleForm(saved)
      await fetchRoles(saved.id)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '角色保存失败')
    } finally {
      setRoleSaving(false)
    }
  }, [fetchRoles, handleAuthExpired, hydrateRoleForm, roleDraftMode, roleForm, selectedRole, token])

  const submitRolePermissions = useCallback(async () => {
    if (!token || !selectedRole) return
    setRolePermissionSaving(true)
    setRolesError('')
    try {
      const saved = await roleApi.updatePermissions(selectedRole.id, {
        permissionCodes: roleForm.permissionCodes,
        dataScopes: roleForm.dataScopes,
      })
      hydrateRoleForm(saved)
      await fetchRoles()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '角色权限保存失败')
    } finally {
      setRolePermissionSaving(false)
    }
  }, [fetchRoles, handleAuthExpired, hydrateRoleForm, roleForm.dataScopes, roleForm.permissionCodes, selectedRole, token])

  const toggleRoleEnabled = useCallback(async (role: ManagedRole) => {
    if (!token || role.systemRole || !canEnableRoles) return
    setRoleSaving(true)
    setRolesError('')
    try {
      const saved = await roleApi.setEnabled(role.id, !role.enabled)
      hydrateRoleForm(saved)
      await fetchRoles()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '角色启停失败')
    } finally {
      setRoleSaving(false)
    }
  }, [canEnableRoles, fetchRoles, handleAuthExpired, hydrateRoleForm, token])

  return {
    fetchRoles,
    openCreateRole,
    permissionTree,
    permissionTreeLoading,
    roleDraftMode,
    roleEnabledFilter,
    roleForm,
    roleKeyword,
    rolePermissionSaving,
    roleSaving,
    rolesData,
    rolesError,
    rolesLoading,
    selectedRole,
    selectedRoleId,
    selectedRoleReadonly,
    selectRole,
    setRoleEnabledFilter,
    setRoleKeyword,
    submitRoleBase,
    submitRolePermissions,
    toggleRoleEnabled,
    toggleRolePermission,
    updateRoleForm,
    updateRoleScope,
  }
}
