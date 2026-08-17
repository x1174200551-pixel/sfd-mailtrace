import { useCallback, useMemo } from 'react'
import { menuGroups } from '../constants/menus'
import { getRouteByTitle } from '../router/routes'
import type { CurrentUser } from '../types/auth'

type PermissionNode = {
  adminOnly?: boolean
  permission?: string
}

function normalizeCodeList(values?: string[]) {
  return new Set((values ?? []).map((value) => value.trim()).filter(Boolean))
}

export function hasRole(currentUser: CurrentUser | null, roleCode: string) {
  const normalized = roleCode.trim().toUpperCase()
  if (!normalized || !currentUser) return false
  return normalizeCodeList(currentUser.roles).has(normalized) || currentUser.roleCode === normalized
}

export function hasUserPermission(currentUser: CurrentUser | null, permissionCode: string) {
  if (!currentUser || !permissionCode.trim()) return false
  const permissions = normalizeCodeList(currentUser.permissions)
  if (hasRole(currentUser, 'ADMIN')) {
    return permissions.size === 0 || permissions.has(permissionCode) || permissionCode.startsWith('role:')
  }
  if (permissions.size > 0) {
    return permissions.has(permissionCode)
  }
  return currentUser.roleCode === 'ADMIN'
}

export function usePermission(currentUser: CurrentUser | null) {
  const isAdmin = useMemo(() => hasRole(currentUser, 'ADMIN'), [currentUser])
  const isAgent = useMemo(() => hasRole(currentUser, 'AGENT'), [currentUser])

  const hasPermission = useCallback(
    (permissionCode: string) => hasUserPermission(currentUser, permissionCode),
    [currentUser],
  )

  const canAccessMenuNode = useCallback(
    (node: PermissionNode) => {
      if (node.permission) return hasPermission(node.permission)
      return !node.adminOnly || isAdmin
    },
    [hasPermission, isAdmin],
  )

  const canAccessPage = useCallback(
    (title: string) => {
      const route = getRouteByTitle(title)
      if (!route) return true
      if (route.menuPermission && !hasPermission(route.menuPermission)) return false
      if (route.accessPermissions.length === 0) return true
      return route.accessPermissions.some((permission) => hasPermission(permission))
    },
    [hasPermission],
  )

  const visibleMenuGroups = useMemo(
    () => menuGroups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => canAccessMenuNode(item) && canAccessPage(item.title)),
      }))
      .filter((group) => group.items.length > 0),
    [canAccessMenuNode, canAccessPage],
  )

  const firstVisibleMenuTitle = visibleMenuGroups.flatMap((group) => group.items)[0]?.title ?? ''

  return {
    canAccessMenuNode,
    canAccessPage,
    firstVisibleMenuTitle,
    hasPermission,
    hasRole: (roleCode: string) => hasRole(currentUser, roleCode),
    isAdmin,
    isAgent,
    visibleMenuGroups,
  }
}
