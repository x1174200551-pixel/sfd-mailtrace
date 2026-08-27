import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { userApi } from '../api/users'
import { enterpriseApi } from '../api/enterprises'
import { mailboxApi } from '../api/mailboxes'
import { emptyUserForm } from '../constants/roles'
import type { ManagedUser, RoleCode, UserConfirmAction, UserDataGrantForm, UserFormMode, UserFormState, UserPageResponse } from '../types/user'
import type { EnterpriseOption } from '../types/enterprise'
import type { MailboxOption } from '../types/mailbox'

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
  const [userGrantForm, setUserGrantForm] = useState<UserDataGrantForm>({ allDataVisible: false, enterpriseIds: [], mailboxIds: [] })
  const [userGrantLoading, setUserGrantLoading] = useState(false)
  const [userGrantEnterpriseOptions, setUserGrantEnterpriseOptions] = useState<EnterpriseOption[]>([])
  const [userGrantMailboxOptions, setUserGrantMailboxOptions] = useState<MailboxOption[]>([])
  const userGrantRequestSequence = useRef(0)

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

  useEffect(() => {
    if (!token || activeMenu !== '用户管理' || !canReadUsers) return
    void Promise.all([enterpriseApi.options(), mailboxApi.options()])
      .then(([enterprises, mailboxes]) => {
        setUserGrantEnterpriseOptions(enterprises)
        setUserGrantMailboxOptions(mailboxes)
      })
      .catch((error) => {
        if (handleAuthExpired(error)) return
        setUserGrantEnterpriseOptions([])
        setUserGrantMailboxOptions([])
      })
  }, [activeMenu, canReadUsers, handleAuthExpired, token])

  const resetUserFilters = useCallback(() => {
    setUserKeyword('')
    setUserRoleFilter('ALL')
    setUserEnabledFilter('ALL')
    setUserPage(1)
  }, [])

  const openCreateUser = useCallback(() => {
    userGrantRequestSequence.current += 1
    setUserFormMode('create')
    setEditingUser(null)
    setUserForm({ ...emptyUserForm, departmentId: defaultDepartmentId })
    setUserGrantForm({ allDataVisible: false, enterpriseIds: [], mailboxIds: [] })
    setUserGrantLoading(false)
    setUserFormError('')
    setUserFormOpen(true)
  }, [defaultDepartmentId])

  const openEditUser = useCallback(async (nextUser: ManagedUser) => {
    const requestSequence = userGrantRequestSequence.current + 1
    userGrantRequestSequence.current = requestSequence
    setUserFormMode('edit')
    setEditingUser(nextUser)
    setUserForm({
      account: nextUser.account,
      displayName: nextUser.displayName,
      email: nextUser.email,
      roleCode: nextUser.roleCode,
      departmentId: nextUser.departmentId ?? defaultDepartmentId,
      password: '',
      enabled: nextUser.enabled,
    })
    setUserGrantForm({ allDataVisible: false, enterpriseIds: [], mailboxIds: [] })
    setUserFormError('')
    setUserGrantLoading(true)
    setUserFormOpen(true)
    try {
      const detail = await userApi.getDataGrants(nextUser.id)
      if (requestSequence !== userGrantRequestSequence.current) return
      setUserGrantForm({
        allDataVisible: detail.allDataVisible,
        enterpriseIds: detail.grants.flatMap((grant) => grant.grantType === 'ENTERPRISE' && grant.enterpriseId ? [grant.enterpriseId] : []),
        mailboxIds: detail.grants.flatMap((grant) => grant.grantType === 'MAILBOX' && grant.mailboxId ? [grant.mailboxId] : []),
      })
    } catch (error) {
      if (requestSequence !== userGrantRequestSequence.current) return
      if (!handleAuthExpired(error)) setUserFormError(error instanceof Error ? error.message : '用户数据授权加载失败')
    } finally {
      if (requestSequence === userGrantRequestSequence.current) setUserGrantLoading(false)
    }
  }, [defaultDepartmentId, handleAuthExpired])

  const closeUserForm = useCallback(() => {
    userGrantRequestSequence.current += 1
    setUserGrantLoading(false)
    setUserFormOpen(false)
  }, [])

  const updateUserForm = useCallback((patch: Partial<UserFormState>) => {
    setUserForm((value) => ({ ...value, ...patch }))
  }, [])

  const changeUserRole = useCallback((roleCode: string) => {
    const nextRole = toRoleCode(roleCode)
    setUserForm((value) => ({
      ...value,
      roleCode: nextRole,
    }))
    setUserGrantForm((value) => ({ ...value, allDataVisible: nextRole === 'ADMIN' }))
  }, [])

  const submitUserForm = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    if (userGrantLoading) {
      setUserFormError('用户数据授权仍在加载，请稍后保存')
      return
    }
    setUserFormSubmitting(true)
    setUserFormError('')
    try {
      if (userForm.roleCode !== 'ADMIN' && userGrantForm.enterpriseIds.length === 0 && userGrantForm.mailboxIds.length === 0) {
        setUserFormError('普通用户至少选择一个可见企业或单邮箱')
        return
      }
      if (userFormMode === 'create') {
        await userApi.create({
          ...userForm,
          enterpriseIds: userGrantForm.enterpriseIds,
          mailboxIds: userGrantForm.mailboxIds,
        })
        setUserPage(1)
      } else if (editingUser) {
        await userApi.update(editingUser.id, {
          displayName: userForm.displayName,
          email: userForm.email,
          roleCode: userForm.roleCode,
          departmentId: userForm.departmentId,
          enabled: userForm.enabled,
          enterpriseIds: userGrantForm.enterpriseIds,
          mailboxIds: userGrantForm.mailboxIds,
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
  }, [editingUser, fetchUsers, handleAuthExpired, token, userForm, userFormMode, userGrantForm, userGrantLoading])

  const toggleUserGrantEnterprise = useCallback((enterpriseId: number) => {
    setUserGrantForm((value) => {
      const selected = value.enterpriseIds.includes(enterpriseId)
      const nextEnterpriseIds = selected ? value.enterpriseIds.filter((id) => id !== enterpriseId) : [...value.enterpriseIds, enterpriseId]
      const mailboxIds = selected
        ? value.mailboxIds
        : value.mailboxIds.filter((mailboxId) => userGrantMailboxOptions.find((mailbox) => mailbox.id === mailboxId)?.enterpriseId !== enterpriseId)
      return { ...value, enterpriseIds: nextEnterpriseIds, mailboxIds }
    })
  }, [userGrantMailboxOptions])

  const toggleUserGrantMailbox = useCallback((mailboxId: number) => {
    setUserGrantForm((value) => ({
      ...value,
      mailboxIds: value.mailboxIds.includes(mailboxId)
        ? value.mailboxIds.filter((id) => id !== mailboxId)
        : [...value.mailboxIds, mailboxId],
    }))
  }, [])

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
    closeUserForm,
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
    toggleUserGrantEnterprise,
    toggleUserGrantMailbox,
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
    userGrantEnterpriseOptions,
    userGrantForm,
    userGrantLoading,
    userGrantMailboxOptions,
  }
}
