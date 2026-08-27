import { menuGroups } from '../constants/menus'

export type AppRouteMeta = {
  title: string
  groupTitle: string
  groupPermission?: string
  menuPermission?: string
  accessPermissions: string[]
  adminOnly?: boolean
}

export const appRoutes: AppRouteMeta[] = menuGroups.flatMap((group) => (
  group.items.map((item) => ({
    title: item.title,
    groupTitle: group.title,
    groupPermission: group.permission,
    menuPermission: item.permission,
    accessPermissions: item.accessPermissions || [],
    adminOnly: item.adminOnly ?? group.adminOnly,
  }))
))

export function getRouteByTitle(title: string) {
  return appRoutes.find((route) => route.title === title) || null
}
