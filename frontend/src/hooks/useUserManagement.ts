import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { userApi } from '../api/users'
import { emptyUserForm } from '../constants/roles'
import type { ManagedUser, RoleCode, UserConfirmAction, UserFormMode, UserFormState, UserPageResponse } from '../types/user'

type UseUserManagementParams = {
  activeMenu: string
  canReadUsers: boolean
  defaultDepartmentId: number | null
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

function toRoleCode(value: string): RoleCode {
  return value.trim().toUpperCase()
}

function normalizeRoleCodes(primaryRoleCode: string, roleCodes: string[]) {
  const primary = toRoleCode(primaryRoleCode)
  return primary ? [primary] : roleCodes.map(toRoleCode).filter(Boolean).slice(0, 1)
}

export function useUserManagement({
  activeMenu,
  canReadUsers,
  defaultDepartmentId,
  handleAuthExpired,
  token,
}: UseUserManagementParams) {
  const [userKeyword, setUserKeyword] = useState('')
  const [userRoleFilter, setUserRoleFilter] = useState('ALL')
  const [userEnabledFilter, setUserEnabledFilter] = useState('ALL')
  const [userPage, setUserPage] = useState(1)
  const [userPageSize, setUserPageSize] = useState(10)
  const [usersData, setUsersData] = useState<UserPageResponse | null>(null)
  const [usersLoading, setUsersLoading] = useState(false)
  const [usersError, setUsersError] = useState('')
  const [userFormMode, setUserFormMode] = useState<UserFormMode>('create')
  const [userFormOpen, setUserFormOpen] = useState(false)
  const [userFormSubmitting, setUserFormSubmitting] = useState(false)
  const [userFormError, setUserFormError] = useState('')
  const [userForm, setUserForm] = useState<UserFormState>(emptyUserForm)
  const [editingUser, setEditingUser] = useState<ManagedUser | null>(null)
  const [confirmAction, setConfirmAction] = useState<UserConfirmAction>(null)
  const [actionLoading, setActionLoading] = useState(false)

  const fetchUsers = useCallback(async () => {
    if (!token || activeMenu !== '用户管理') return
    if (!canReadUsers) {
      setUsersData(null)
      setUsersError('当前账号没有用户管理权限')
      return
    }

    setUsersLoading(true)
    setUsersError('')
    try {
      const data = await userApi.list({
        enabled: userEnabledFilter !== 'ALL' ? userEnabledFilter : undefined,
        keyword: userKeyword.trim(),
        page: userPage,
        roleCode: userRoleFilter !== 'ALL' ? userRoleFilter : undefined,
        size: userPageSize,
      })
      setUsersData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setUsersError(error instanceof Error ? error.message : '用户列表加载失败')
    } finally {
      setUsersLoading(false)
    }
  }, [activeMenu, canReadUsers, handleAuthExpired, token, userEnabledFilter, userKeyword, userPage, userPageSize, userRoleFilter])

  useEffect(() => {
    void fetchUsers()
  }, [fetchUsers])

  const resetUserFilters = useCallback(() => {
    setUserKeyword('')
    setUserRoleFilter('ALL')
    setUserEnabledFilter('ALL')
    setUserPage(1)
  }, [])

  const openCreateUser = useCallback(() => {
    setUserFormMode('create')
    setEditingUser(null)
    setUserForm({ ...emptyUserForm, departmentId: defaultDepartmentId })
    setUserFormError('')
    setUserFormOpen(true)
  }, [defaultDepartmentId])

  const openEditUser = useCallback((nextUser: ManagedUser) => {
    setUserFormMode('edit')
    setEditingUser(nextUser)
    const assignedRoleCodes = normalizeRoleCodes(nextUser.roleCode, nextUser.roleCodes || [])
    setUserForm({
      account: nextUser.account,
      displayName: nextUser.displayName,
      email: nextUser.email,
      roleCode: nextUser.roleCode,
      roleCodes: assignedRoleCodes.slice(0, 1),
      departmentId: nextUser.departmentId ?? defaultDepartmentId,
      password: '',
      enabled: nextUser.enabled,
    })
    setUserFormError('')
    setUserFormOpen(true)
  }, [defaultDepartmentId])

  const updateUserForm = useCallback((patch: Partial<UserFormState>) => {
    setUserForm((value) => ({ ...value, ...patch }))
  }, [])

  const changeUserRole = useCallback((roleCode: string) => {
    const nextRole = toRoleCode(roleCode)
    setUserForm((value) => ({
      ...value,
      roleCode: nextRole,
      roleCodes: normalizeRoleCodes(nextRole, []),
    }))
  }, [])

  const submitUserForm = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    setUserFormSubmitting(true)
    setUserFormError('')
    try {
      if (userFormMode === 'create') {
        await userApi.create({
          ...userForm,
          roleCodes: normalizeRoleCodes(userForm.roleCode, userForm.roleCodes),
        })
        setUserPage(1)
      } else if (editingUser) {
        await userApi.update(editingUser.id, {
          displayName: userForm.displayName,
          email: userForm.email,
          roleCode: userForm.roleCode,
          roleCodes: normalizeRoleCodes(userForm.roleCode, userForm.roleCodes),
          departmentId: userForm.departmentId,
          enabled: userForm.enabled,
        })
      }
      setUserFormOpen(false)
      await fetchUsers()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setUserFormError(error instanceof Error ? error.message : '保存失败，请稍后重试')
    } finally {
      setUserFormSubmitting(false)
    }
  }, [editingUser, fetchUsers, handleAuthExpired, token, userForm, userFormMode])

  const openEnabledConfirm = useCallback((nextUser: ManagedUser) => {
    setConfirmAction({
      user: nextUser,
      type: nextUser.enabled ? 'disable' : 'enable',
      title: nextUser.enabled ? '停用用户确认' : '启用用户确认',
      text: nextUser.enabled
        ? '停用后该账号将无法登录系统，历史工单归属和操作日志仍保留。'
        : '启用后该账号可重新登录系统并处理工单。',
      actionLabel: nextUser.enabled ? '确认停用' : '确认启用',
    })
  }, [])

  const openResetConfirm = useCallback((nextUser: ManagedUser) => {
    setConfirmAction({
      user: nextUser,
      type: 'reset',
      title: '重置密码确认',
      text: '确认后将把该用户密码重置为临时密码 Mail@2026，请管理员线下告知用户登录后及时修改。',
      actionLabel: '确认重置',
    })
  }, [])

  const submitConfirmAction = useCallback(async () => {
    if (!token || !confirmAction) return
    setActionLoading(true)
    setUsersError('')
    try {
      if (confirmAction.type === 'reset') {
        await userApi.resetPassword(confirmAction.user.id, 'Mail@2026')
      } else {
        await userApi.setEnabled(confirmAction.user.id, confirmAction.type === 'enable')
      }
      setConfirmAction(null)
      await fetchUsers()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setUsersError(error instanceof Error ? error.message : '操作失败，请稍后重试')
    } finally {
      setActionLoading(false)
    }
  }, [confirmAction, fetchUsers, handleAuthExpired, token])

  const changeUserEnabledFilter = useCallback((value: string) => {
    setUserEnabledFilter(value)
    setUserPage(1)
  }, [])

  const changeUserKeyword = useCallback((value: string) => {
    setUserKeyword(value)
    setUserPage(1)
  }, [])

  const changeUserPageSize = useCallback((size: number) => {
    setUserPageSize(size)
    setUserPage(1)
  }, [])

  const changeUserRoleFilter = useCallback((value: string) => {
    setUserRoleFilter(value)
    setUserPage(1)
  }, [])

  return {
    actionLoading,
    changeUserEnabledFilter,
    changeUserKeyword,
    changeUserPageSize,
    changeUserRole,
    changeUserRoleFilter,
    closeConfirm: () => setConfirmAction(null),
    closeUserForm: () => setUserFormOpen(false),
    confirmAction,
    fetchUsers,
    openCreateUser,
    openEditUser,
    openEnabledConfirm,
    openResetConfirm,
    resetUserFilters,
    setUserPage,
    submitConfirmAction,
    submitUserForm,
    updateUserForm,
    userEnabledFilter,
    userForm,
    userFormError,
    userFormMode,
    userFormOpen,
    userFormSubmitting,
    userKeyword,
    userPage,
    userPageSize,
    userRoleFilter,
    usersData,
    usersError,
    usersLoading,
  }
}
