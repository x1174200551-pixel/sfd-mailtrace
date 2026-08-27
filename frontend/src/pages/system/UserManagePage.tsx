import type { FormEvent } from 'react'
import {
  Check,
  Building2,
  Edit3,
  LockKeyhole,
  Mail,
  Plus,
  Power,
  PowerOff,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  Users,
  X,
} from 'lucide-react'
import { getRoleProfile, roleLabel, roleProfiles } from '../../constants/roles'
import type { DepartmentNode } from '../../types/department'
import type { ManagedUser, RoleCode, UserConfirmAction, UserFormMode, UserFormState, UserPageResponse } from '../../types/user'
import type { UserDataGrantForm } from '../../types/user'
import type { EnterpriseOption } from '../../types/enterprise'
import type { MailboxOption } from '../../types/mailbox'

type RoleOption = {
  label: string
  value: RoleCode
}

type UserManagePageProps = {
  actionLoading: boolean
  canCreateUsers: boolean
  canEnableUsers: boolean
  canReadUsers: boolean
  canResetUserPassword: boolean
  canUpdateUsers: boolean
  confirmAction: UserConfirmAction
  departmentOptions: DepartmentNode[]
  departmentsError: string
  onCloseConfirm: () => void
  onCloseUserForm: () => void
  onFetchUsers: () => void
  onOpenCreateUser: () => void
  onOpenEditUser: (user: ManagedUser) => void
  onOpenEnabledConfirm: (user: ManagedUser) => void
  onOpenResetConfirm: (user: ManagedUser) => void
  onResetUserFilters: () => void
  onSubmitConfirmAction: () => void
  onSubmitUserForm: (event: FormEvent<HTMLFormElement>) => void
  onUserEnabledFilterChange: (value: string) => void
  onUserFormChange: (patch: Partial<UserFormState>) => void
  onUserKeywordChange: (value: string) => void
  onUserPageChange: (page: number) => void
  onUserPageSizeChange: (size: number) => void
  onUserRoleChange: (roleCode: string) => void
  onUserRoleFilterChange: (value: string) => void
  roleOptions: RoleOption[]
  rolesPermissionTotal?: number
  userEnabledFilter: string
  userForm: UserFormState
  userFormError: string
  userFormMode: UserFormMode
  userFormOpen: boolean
  userFormSubmitting: boolean
  userKeyword: string
  userPage: number
  userPageSize: number
  userRoleFilter: string
  usersData: UserPageResponse | null
  usersError: string
  usersLoading: boolean
  userGrantEnterpriseOptions: EnterpriseOption[]
  userGrantForm: UserDataGrantForm
  userGrantLoading: boolean
  userGrantMailboxOptions: MailboxOption[]
  onToggleUserGrantEnterprise: (enterpriseId: number) => void
  onToggleUserGrantMailbox: (mailboxId: number) => void
}

export function UserManagePage({
  actionLoading,
  canCreateUsers,
  canEnableUsers,
  canReadUsers,
  canResetUserPassword,
  canUpdateUsers,
  confirmAction,
  departmentOptions,
  departmentsError,
  onCloseConfirm,
  onCloseUserForm,
  onFetchUsers,
  onOpenCreateUser,
  onOpenEditUser,
  onOpenEnabledConfirm,
  onOpenResetConfirm,
  onResetUserFilters,
  onSubmitConfirmAction,
  onSubmitUserForm,
  onUserEnabledFilterChange,
  onUserFormChange,
  onUserKeywordChange,
  onUserPageChange,
  onUserPageSizeChange,
  onUserRoleChange,
  onUserRoleFilterChange,
  roleOptions,
  rolesPermissionTotal,
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
  onToggleUserGrantEnterprise,
  onToggleUserGrantMailbox,
}: UserManagePageProps) {
  const selectedDepartmentName = departmentOptions.find((department) => department.id === userForm.departmentId)?.deptName || '默认部门'
  const selectedRoleLabel = roleOptions.find((role) => role.value === userForm.roleCode)?.label || roleLabel(userForm.roleCode)

  return (
    <>
      <section className="app-content user-page" aria-label="用户管理">
        <header className="user-topbar">
          <div className="user-title-block">
            <h2>用户管理</h2>
            <span>维护后台账号、角色、主部门和启停状态</span>
          </div>
          <div className="user-top-actions">
            <button disabled={usersLoading} onClick={onFetchUsers} type="button">
              <RefreshCw size={16} />
              刷新
            </button>
            <button className="primary-action" disabled={!canCreateUsers} onClick={onOpenCreateUser} type="button">
              <Plus size={16} />
              新建用户
            </button>
          </div>
        </header>

        {!canReadUsers ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无用户管理权限</strong>
            <p>非管理员仅可查看自己的个人信息，用户管理入口对处理人隐藏。</p>
          </div>
        ) : (
          <>
            <section className="user-summary-strip" aria-label="用户统计">
              <div className="user-summary-item active">
                <span className="user-summary-icon"><Users size={17} /></span>
                <span className="user-summary-copy">
                  <span>用户总数</span>
                  <small>后台账号总量</small>
                </span>
                <strong>{usersData?.summary.totalUsers ?? '--'}</strong>
              </div>
              <div className="user-summary-item">
                <span className="user-summary-icon success"><ShieldCheck size={17} /></span>
                <span className="user-summary-copy">
                  <span>管理员</span>
                  <small>全部菜单和数据范围</small>
                </span>
                <strong>{usersData?.summary.adminUsers ?? '--'}</strong>
              </div>
              <div className="user-summary-item">
                <span className="user-summary-icon info"><Check size={17} /></span>
                <span className="user-summary-copy">
                  <span>处理人</span>
                  <small>自己负责和未分配池</small>
                </span>
                <strong>{usersData?.summary.agentUsers ?? '--'}</strong>
              </div>
              <div className="user-summary-item">
                <span className="user-summary-icon warning"><LockKeyhole size={17} /></span>
                <span className="user-summary-copy">
                  <span>权限项</span>
                  <small>当前内置权限清单</small>
                </span>
                <strong>{rolesPermissionTotal ?? roleProfiles.ADMIN.permissionCount}</strong>
              </div>
            </section>

            <div className="user-toolbar">
              <label className="user-search">
                <Search size={16} />
                <input
                  onChange={(event) => onUserKeywordChange(event.target.value)}
                  placeholder="搜索账号、姓名、邮箱"
                  type="search"
                  value={userKeyword}
                />
              </label>
              <label>
                <span>角色</span>
                <select onChange={(event) => onUserRoleFilterChange(event.target.value)} value={userRoleFilter}>
                  <option value="ALL">全部角色</option>
                  {roleOptions.map((role) => (
                    <option key={role.value} value={role.value}>
                      {role.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>状态</span>
                <select onChange={(event) => onUserEnabledFilterChange(event.target.value)} value={userEnabledFilter}>
                  <option value="ALL">全部状态</option>
                  <option value="true">启用</option>
                  <option value="false">停用</option>
                </select>
              </label>
              <button onClick={onResetUserFilters} type="button">
                <RotateCcw size={15} />
                清空筛选
              </button>
            </div>

            {usersError && <div className="user-alert">{usersError}</div>}

            <div className="user-table-panel">
              {usersLoading ? (
                <div className="user-loading">
                  {[0, 1, 2, 3].map((item) => (
                    <span key={item} />
                  ))}
                </div>
              ) : usersData && usersData.records.length > 0 ? (
                <table className="user-table">
                  <thead>
                    <tr>
                      <th>账号</th>
                      <th>姓名</th>
                      <th>邮箱</th>
                      <th>角色</th>
                      <th>主部门</th>
                      <th>菜单范围</th>
                      <th>数据授权</th>
                      <th>状态</th>
                      <th>最近登录</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {usersData.records.map((managedUser) => {
                      const profile = getRoleProfile(managedUser.roleCode)
                      return (
                        <tr key={managedUser.id}>
                          <td>
                            <strong>{managedUser.account}</strong>
                            <small>ID {managedUser.id}</small>
                          </td>
                          <td>{managedUser.displayName}</td>
                          <td>{managedUser.email}</td>
                          <td>
                            <div className="role-pill-list">
                              <span className={managedUser.roleCode === 'ADMIN' ? 'role-pill admin' : 'role-pill'}>
                                {roleOptions.find((role) => role.value === managedUser.roleCode)?.label || roleLabel(managedUser.roleCode)}
                              </span>
                            </div>
                          </td>
                          <td>{managedUser.departmentName || '默认部门'}</td>
                          <td>{profile.menuScope}</td>
                          <td>{managedUser.roleCode === 'ADMIN' ? '全部数据可见' : '按企业 / 邮箱授权'}</td>
                          <td>
                            <span className={managedUser.enabled ? 'state-pill enabled' : 'state-pill disabled'}>
                              {managedUser.enabled ? '启用' : '停用'}
                            </span>
                          </td>
                          <td>{formatDateTime(managedUser.lastLoginAt)}</td>
                          <td>
                            <div className="user-ops">
                              <button disabled={!canUpdateUsers} onClick={() => onOpenEditUser(managedUser)} type="button">
                                <Edit3 size={14} />
                                编辑用户
                              </button>
                              <button disabled={!canResetUserPassword} onClick={() => onOpenResetConfirm(managedUser)} type="button">
                                <LockKeyhole size={14} />
                                重置密码
                              </button>
                              <button
                                className={managedUser.enabled ? 'danger' : 'success'}
                                disabled={!canEnableUsers}
                                onClick={() => onOpenEnabledConfirm(managedUser)}
                                type="button"
                              >
                                {managedUser.enabled ? <PowerOff size={14} /> : <Power size={14} />}
                                {managedUser.enabled ? '停用' : '启用'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              ) : (
                <div className="empty-state">
                  <Users size={42} />
                  <strong>未找到用户</strong>
                  <p>可清空筛选后重新查询，或直接新建用户。</p>
                  <div>
                    <button onClick={onResetUserFilters} type="button">清空筛选</button>
                    <button className="primary-action" disabled={!canCreateUsers} onClick={onOpenCreateUser} type="button">新建用户</button>
                  </div>
                </div>
              )}
            </div>

            <div className="user-pagination">
              <span>
                共 {usersData?.total ?? 0} 条，每页
                <select onChange={(event) => onUserPageSizeChange(Number(event.target.value))} value={userPageSize}>
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
                条
              </span>
              <div>
                <button disabled={userPage <= 1} onClick={() => onUserPageChange(userPage - 1)} type="button">
                  上一页
                </button>
                <strong>
                  {usersData?.page ?? userPage} / {Math.max(usersData?.pages ?? 1, 1)}
                </strong>
                <button
                  disabled={!usersData || userPage >= Math.max(usersData.pages, 1)}
                  onClick={() => onUserPageChange(userPage + 1)}
                  type="button"
                >
                  下一页
                </button>
              </div>
            </div>
          </>
        )}
      </section>

      {userFormOpen && (
        <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="user-form-title">
          <form className="user-modal" onSubmit={onSubmitUserForm}>
            <div className="user-modal__head">
              <h2 id="user-form-title">{userFormMode === 'create' ? '新建用户' : '编辑用户'}</h2>
              <button aria-label="关闭" onClick={onCloseUserForm} type="button">
                <X size={18} />
              </button>
            </div>
            {userFormError && <div className="user-alert">{userFormError}</div>}
            <div className="user-modal__body">
              <label>
                <span>登录账号</span>
                <input
                  disabled={userFormMode === 'edit' || userFormSubmitting}
                  onChange={(event) => onUserFormChange({ account: event.target.value })}
                  placeholder="agent01"
                  value={userForm.account}
                />
                <small>账号保存后不可修改，需保持唯一。</small>
              </label>
              <label>
                <span>姓名</span>
                <input
                  disabled={userFormSubmitting}
                  onChange={(event) => onUserFormChange({ displayName: event.target.value })}
                  placeholder="客服一号"
                  value={userForm.displayName}
                />
              </label>
              <label>
                <span>邮箱</span>
                <input
                  disabled={userFormSubmitting}
                  onChange={(event) => onUserFormChange({ email: event.target.value })}
                  placeholder="agent01@ntn.fziot"
                  type="email"
                  value={userForm.email}
                />
              </label>
              <label>
                <span>角色</span>
                <select disabled={userFormSubmitting} onChange={(event) => onUserRoleChange(event.target.value)} value={userForm.roleCode}>
                  {roleOptions.map((role) => (
                    <option key={role.value} value={role.value}>{role.label}</option>
                  ))}
                </select>
              </label>
              <label>
                <span>主部门</span>
                <select
                  disabled={userFormSubmitting || departmentOptions.length === 0}
                  onChange={(event) => onUserFormChange({ departmentId: Number(event.target.value) || null })}
                  value={userForm.departmentId ?? ''}
                >
                  {departmentOptions.map((department) => (
                    <option key={department.id} value={department.id}>{department.deptName}</option>
                  ))}
                </select>
                <small>{departmentsError || '用于组织归属、成员管理和负责人识别。'}</small>
              </label>
              <div className="role-preview">
                <span>角色生效预览</span>
                <strong>{selectedRoleLabel}</strong>
                <small>一个用户只绑定一个角色和一个主部门；菜单与操作权限由角色决定，业务数据由下方企业/邮箱授权决定。</small>
                <div>
                  <em>{selectedDepartmentName}</em>
                  <em>{selectedRoleLabel}</em>
                </div>
              </div>
              <section className="user-grant-editor">
                <div className="user-grant-head">
                  <div><strong>数据授权</strong><small>企业授权自动包含该企业未来新增邮箱；单邮箱授权只包含勾选项。</small></div>
                  <span>{userForm.roleCode === 'ADMIN' ? '全部数据可见' : `已选 ${userGrantForm.enterpriseIds.length} 个企业 / ${userGrantForm.mailboxIds.length} 个邮箱`}</span>
                </div>
                {userForm.roleCode === 'ADMIN' ? (
                  <div className="user-grant-admin"><ShieldCheck size={20} /><div><strong>管理员无需逐条授权</strong><small>管理员始终可见所有企业、邮箱及历史数据。</small></div></div>
                ) : userGrantLoading ? (
                  <div className="user-grant-loading">正在加载数据授权...</div>
                ) : (
                  <div className="user-grant-groups">
                    {userGrantEnterpriseOptions.map((enterprise) => {
                      const enterpriseSelected = userGrantForm.enterpriseIds.includes(enterprise.id)
                      const enterpriseMailboxes = userGrantMailboxOptions.filter((mailbox) => mailbox.enterpriseId === enterprise.id)
                      return (
                        <article key={enterprise.id}>
                          <label className="user-grant-enterprise">
                            <input checked={enterpriseSelected} disabled={userFormSubmitting} onChange={() => onToggleUserGrantEnterprise(enterprise.id)} type="checkbox" />
                            <Building2 size={16} />
                            <strong>{enterprise.enterpriseName}</strong>
                            <small>{enterpriseSelected ? '已授权整个企业' : '可单独选择邮箱'}</small>
                          </label>
                          <div className="user-grant-mailboxes">
                            {enterpriseMailboxes.length ? enterpriseMailboxes.map((mailbox) => (
                              <label key={mailbox.id}>
                                <input checked={enterpriseSelected || userGrantForm.mailboxIds.includes(mailbox.id)} disabled={userFormSubmitting || enterpriseSelected} onChange={() => onToggleUserGrantMailbox(mailbox.id)} type="checkbox" />
                                <Mail size={14} /><span>{mailbox.mailboxName}</span><small>{mailbox.emailAddress}</small>
                              </label>
                            )) : <small>该企业暂无邮箱</small>}
                          </div>
                        </article>
                      )
                    })}
                    {userGrantEnterpriseOptions.length === 0 && <div className="user-grant-loading">暂无可授权企业</div>}
                  </div>
                )}
              </section>
              {userFormMode === 'create' && (
                <label>
                  <span>初始密码</span>
                  <input
                    disabled={userFormSubmitting}
                    onChange={(event) => onUserFormChange({ password: event.target.value })}
                    placeholder="至少 6 位"
                    type="password"
                    value={userForm.password}
                  />
                </label>
              )}
              <label>
                <span>状态</span>
                <select
                  disabled={userFormSubmitting}
                  onChange={(event) => onUserFormChange({ enabled: event.target.value === 'true' })}
                  value={String(userForm.enabled)}
                >
                  <option value="true">启用</option>
                  <option value="false">停用</option>
                </select>
              </label>
            </div>
            <div className="user-modal__foot">
              <button disabled={userFormSubmitting} onClick={onCloseUserForm} type="button">
                取消
              </button>
              <button className="primary-action" disabled={userFormSubmitting || userGrantLoading} type="submit">
                <Check size={16} />
                {userFormSubmitting ? '保存中...' : userFormMode === 'create' ? '保存并创建' : '保存修改'}
              </button>
            </div>
          </form>
        </div>
      )}

      {confirmAction && (
        <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
          <div className="confirm-modal">
            <h3 id="confirm-title">{confirmAction.title}</h3>
            <p>{confirmAction.text}</p>
            <div className="confirm-target">
              <strong>{confirmAction.user.displayName}</strong>
              <span>{confirmAction.user.account} / {confirmAction.user.email}</span>
            </div>
            <div className="user-modal__foot">
              <button disabled={actionLoading} onClick={onCloseConfirm} type="button">取消</button>
              <button className="primary-action" disabled={actionLoading} onClick={onSubmitConfirmAction} type="button">
                {actionLoading ? '处理中...' : confirmAction.actionLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function formatDateTime(value: string | null) {
  if (!value) return '未登录'
  return value.replace('T', ' ').slice(0, 16)
}
