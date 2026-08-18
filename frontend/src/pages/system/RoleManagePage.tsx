import {
  Check,
  Loader,
  Plus,
  Power,
  PowerOff,
  RefreshCw,
  Search,
  ShieldCheck,
  UserCog,
} from 'lucide-react'
import { dataResourceLabel, dataScopeDesc, dataScopeLabel } from '../../constants/data-scopes'
import type { ManagedRole, PermissionTreeNode, RoleFormState, RoleListResponse } from '../../types/role'

type RoleManagePageProps = {
  canCreateRoles: boolean
  canEnableRoles: boolean
  canReadRoles: boolean
  canUpdateRoles: boolean
  canUpdateRolePermissions: boolean
  onCreateRole: () => void
  onFetchRoles: () => void
  onOpenUserManage: () => void
  onRoleEnabledFilterChange: (value: string) => void
  onRoleFormChange: (patch: Partial<RoleFormState>) => void
  onRoleKeywordChange: (value: string) => void
  onSelectRole: (role: ManagedRole) => void
  onSubmitRoleBase: () => void
  onSubmitRolePermissions: () => void
  onToggleRoleEnabled: (role: ManagedRole) => void
  onToggleRolePermission: (permissionCode: string, checked: boolean) => void
  onUpdateRoleScope: (resourceType: string, scopeCode: string) => void
  permissionTree: PermissionTreeNode[]
  permissionTreeLoading: boolean
  roleDraftMode: 'create' | 'edit'
  roleEnabledFilter: string
  roleForm: RoleFormState
  roleKeyword: string
  rolePermissionSaving: boolean
  roleSaving: boolean
  rolesData: RoleListResponse | null
  rolesError: string
  rolesLoading: boolean
  selectedRole: ManagedRole | null
  selectedRoleId: number | null
  selectedRoleReadonly: boolean
}

function collectPermissionNodes(nodes: PermissionTreeNode[]): PermissionTreeNode[] {
  return nodes.flatMap((node) => [node, ...collectPermissionNodes(node.children || [])])
}

const dataScopeResources = ['TICKET', 'CUSTOMER', 'DASHBOARD']
const dataScopeOptions = ['SELF', 'DEPT', 'DEPT_AND_CHILDREN', 'ALL']

export function RoleManagePage({
  canCreateRoles,
  canEnableRoles,
  canReadRoles,
  canUpdateRoles,
  canUpdateRolePermissions,
  onCreateRole,
  onFetchRoles,
  onOpenUserManage,
  onRoleEnabledFilterChange,
  onRoleFormChange,
  onRoleKeywordChange,
  onSelectRole,
  onSubmitRoleBase,
  onSubmitRolePermissions,
  onToggleRoleEnabled,
  onToggleRolePermission,
  onUpdateRoleScope,
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
}: RoleManagePageProps) {
  const flatPermissionNodes = collectPermissionNodes(permissionTree)
  const checkedPermissionSet = new Set(roleForm.permissionCodes)
  const permissionDisabled = selectedRoleReadonly || roleSaving || rolePermissionSaving || !canUpdateRolePermissions

  function renderPermissionTree(nodes: PermissionTreeNode[]) {
    if (permissionTreeLoading) {
      return <div className="role-permission-loading">权限清单加载中...</div>
    }
    if (nodes.length === 0) {
      return <div className="role-permission-loading">暂无可配置权限</div>
    }
    return nodes.map((group) => (
      <div className="role-permission-group" key={group.permissionCode}>
        <div className="role-permission-group__head">
          <label>
            <input
              checked={checkedPermissionSet.has(group.permissionCode)}
              disabled={permissionDisabled}
              onChange={(event) => onToggleRolePermission(group.permissionCode, event.target.checked)}
              type="checkbox"
            />
            <strong>{group.permissionName}</strong>
          </label>
          <span>{group.children.length} 项</span>
        </div>
        <div className="role-permission-children">
          {group.children.map((child) => (
            <div className="role-permission-child" key={child.permissionCode}>
              <label>
                <input
                  checked={checkedPermissionSet.has(child.permissionCode)}
                  disabled={permissionDisabled}
                  onChange={(event) => onToggleRolePermission(child.permissionCode, event.target.checked)}
                  type="checkbox"
                />
                <span>{child.permissionName}</span>
              </label>
              {child.children.length > 0 && (
                <div className="role-permission-actions">
                  {child.children.map((action) => (
                    <label key={action.permissionCode}>
                      <input
                        checked={checkedPermissionSet.has(action.permissionCode)}
                        disabled={permissionDisabled}
                        onChange={(event) => onToggleRolePermission(action.permissionCode, event.target.checked)}
                        type="checkbox"
                      />
                      {action.permissionName}
                    </label>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    ))
  }

  return (
    <section className="app-content role-management-page" aria-label="角色管理">
      <header className="role-topbar">
        <div className="role-title-block">
          <h2>角色管理</h2>
          <span>配置菜单权限、操作权限和默认数据范围</span>
        </div>
        <div className="role-top-actions">
          <button disabled={rolesLoading} onClick={onFetchRoles} type="button">
            <RefreshCw size={16} />
            刷新
          </button>
          <button className="primary-action" disabled={!canCreateRoles} onClick={onCreateRole} type="button">
            <Plus size={16} />
            新建角色
          </button>
        </div>
      </header>

      {!canReadRoles ? (
        <div className="permission-state">
          <ShieldCheck size={42} />
          <strong>无角色管理权限</strong>
          <p>角色配置仅对具备系统管理权限的账号开放。</p>
        </div>
      ) : (
        <>
          <section className="user-summary-strip role-summary-strip" aria-label="角色统计">
            <div className="user-summary-item active">
              <span className="user-summary-icon"><UserCog size={17} /></span>
              <span className="user-summary-copy">
                <span>角色总数</span>
                <small>{rolesData ? `${rolesData.systemCount} 个内置，${rolesData.customCount} 个自定义` : '后台角色总量'}</small>
              </span>
              <strong>{rolesData?.total ?? '--'}</strong>
            </div>
            <div className="user-summary-item">
              <span className="user-summary-icon success"><Check size={17} /></span>
              <span className="user-summary-copy">
                <span>启用角色</span>
                <small>可被分配给用户</small>
              </span>
              <strong>{rolesData?.enabledCount ?? '--'}</strong>
            </div>
            <div className="user-summary-item">
              <span className="user-summary-icon warning"><ShieldCheck size={17} /></span>
              <span className="user-summary-copy">
                <span>权限项</span>
                <small>菜单与操作统一清单</small>
              </span>
              <strong>{(rolesData?.permissionTotal ?? flatPermissionNodes.length) || '--'}</strong>
            </div>
            <div className="user-summary-item">
              <span className="user-summary-icon info"><UserCog size={17} /></span>
              <span className="user-summary-copy">
                <span>关联用户</span>
                <small>当前已分配角色用户</small>
              </span>
              <strong>{rolesData?.userTotal ?? '--'}</strong>
            </div>
          </section>

          <div className="role-management-grid">
            <section className="role-list-panel">
              <div className="role-panel-head">
                <div>
                  <strong>角色列表</strong>
                  <span>内置角色受保护，自定义角色可编辑和启停</span>
                </div>
                <em>启用 {rolesData?.enabledCount ?? 0}</em>
              </div>
              <div className="user-toolbar compact">
                <label className="user-search">
                  <Search size={16} />
                  <input
                    onChange={(event) => onRoleKeywordChange(event.target.value)}
                    placeholder="搜索角色"
                    type="search"
                    value={roleKeyword}
                  />
                </label>
                <label>
                  <span>状态</span>
                  <select onChange={(event) => onRoleEnabledFilterChange(event.target.value)} value={roleEnabledFilter}>
                    <option value="ALL">全部状态</option>
                    <option value="true">启用</option>
                    <option value="false">停用</option>
                  </select>
                </label>
              </div>
              {rolesError && <div className="user-alert">{rolesError}</div>}
              <div className="role-list">
                {rolesLoading ? (
                  <div className="user-loading">
                    {[0, 1, 2].map((item) => <span key={item} />)}
                  </div>
                ) : rolesData && rolesData.records.length > 0 ? (
                  rolesData.records.map((role) => (
                    <article
                      className={selectedRoleId === role.id ? 'role-list-item active' : 'role-list-item'}
                      key={role.id}
                      onClick={() => onSelectRole(role)}
                    >
                      <div>
                        <strong>{role.roleName}</strong>
                        <p>{role.roleDesc || '暂无角色说明'}</p>
                      </div>
                      <div className="role-list-tags">
                        <span className={role.systemRole ? 'role-pill admin' : 'role-pill'}>{role.systemRole ? '内置' : '自定义'}</span>
                        <span className={role.enabled ? 'state-pill enabled' : 'state-pill disabled'}>{role.enabled ? '启用' : '停用'}</span>
                      </div>
                      <dl>
                        <div><dt>菜单</dt><dd>{role.permissionCodes.filter((code) => code.startsWith('menu:')).length}</dd></div>
                        <div><dt>操作</dt><dd>{role.permissionCodes.filter((code) => !code.startsWith('menu:')).length}</dd></div>
                        <div><dt>用户</dt><dd>{role.userCount}</dd></div>
                      </dl>
                    </article>
                  ))
                ) : (
                  <div className="empty-state compact">
                    <ShieldCheck size={36} />
                    <strong>暂无角色</strong>
                    <p>可新建自定义角色后配置权限。</p>
                  </div>
                )}
              </div>
            </section>

            <section className="role-editor-panel">
              <div className="role-panel-head editor">
                <div>
                  <strong>{roleDraftMode === 'create' ? '新建角色' : '编辑角色'}</strong>
                  <span>
                    {selectedRoleReadonly
                      ? '内置角色仅支持查看，不允许编辑或停用'
                      : roleDraftMode === 'create'
                        ? '可先勾选权限和数据范围，保存时一次创建并生效'
                        : '保存后，已分配该角色的用户重新获取当前用户信息后生效'}
                  </span>
                </div>
                <div className="role-editor-actions">
                  {roleDraftMode === 'edit' && selectedRole && !selectedRole.systemRole && (
                    <button disabled={!canEnableRoles || roleSaving} onClick={() => onToggleRoleEnabled(selectedRole)} type="button">
                      {selectedRole.enabled ? <PowerOff size={15} /> : <Power size={15} />}
                      {selectedRole.enabled ? '停用' : '启用'}
                    </button>
                  )}
                  <button
                    className="primary-action"
                    disabled={selectedRoleReadonly || roleSaving || !roleForm.roleName.trim() || (roleDraftMode === 'create' ? !canCreateRoles : !canUpdateRoles)}
                    onClick={onSubmitRoleBase}
                    type="button"
                  >
                    {roleSaving ? <Loader size={15} className="spin-icon" /> : <Check size={15} />}
                    {roleDraftMode === 'create' ? '创建角色' : '保存角色'}
                  </button>
                </div>
              </div>

              <div className="role-editor-form">
                <label>
                  <span>角色名称</span>
                  <input
                    disabled={selectedRoleReadonly || roleSaving}
                    onChange={(event) => onRoleFormChange({ roleName: event.target.value })}
                    placeholder="工单质检"
                    value={roleForm.roleName}
                  />
                </label>
                <label>
                  <span>角色状态</span>
                  <select
                    disabled={selectedRoleReadonly || roleSaving}
                    onChange={(event) => onRoleFormChange({ enabled: event.target.value === 'true' })}
                    value={String(roleForm.enabled)}
                  >
                    <option value="true">启用</option>
                    <option value="false">停用</option>
                  </select>
                </label>
                <label className="wide">
                  <span>角色说明</span>
                  <textarea
                    disabled={selectedRoleReadonly || roleSaving}
                    onChange={(event) => onRoleFormChange({ roleDesc: event.target.value })}
                    placeholder="查看工单、客户和附件，用于服务质量抽查"
                    value={roleForm.roleDesc}
                  />
                </label>
              </div>

              <div className="role-section-title">
                <strong>菜单与操作权限</strong>
                <span>已选 {roleForm.permissionCodes.length} 项</span>
              </div>
              <div className="role-permission-layout">
                {renderPermissionTree(permissionTree)}
              </div>

              <div className="role-section-title">
                <strong>默认数据范围</strong>
                <span>配置角色默认可见范围</span>
              </div>
              <div className="role-scope-grid">
                {dataScopeResources.map((resourceType) => {
                  const selectedScope = roleForm.dataScopes.find((scope) => scope.resourceType === resourceType)?.scopeCode || 'SELF'
                  return (
                    <div className="role-scope-card" key={resourceType}>
                      <strong>{dataResourceLabel(resourceType)}</strong>
                      <div>
                        {dataScopeOptions.map((scopeCode) => (
                          <button
                            className={selectedScope === scopeCode ? 'active' : ''}
                            disabled={permissionDisabled}
                            key={scopeCode}
                            onClick={() => onUpdateRoleScope(resourceType, scopeCode)}
                            type="button"
                          >
                            {dataScopeLabel(scopeCode)}
                          </button>
                        ))}
                      </div>
                      <p>{dataScopeDesc(resourceType, selectedScope)}</p>
                    </div>
                  )
                })}
              </div>

              <div className="role-effective-preview">
                <div><span>可见菜单</span><strong>{roleForm.permissionCodes.filter((code) => code.startsWith('menu:')).length} 项</strong></div>
                <div><span>操作权限</span><strong>{roleForm.permissionCodes.filter((code) => !code.startsWith('menu:')).length} 项</strong></div>
                <div><span>数据范围</span><strong>{roleForm.dataScopes.map((scope) => `${dataResourceLabel(scope.resourceType)} ${dataScopeLabel(scope.scopeCode)}`).join('、')}</strong></div>
                <div><span>保存影响</span><strong>{selectedRole?.userCount ?? 0} 个用户</strong></div>
              </div>

              <div className="role-editor-foot">
                <span>关联用户入口仍在用户管理</span>
                <button onClick={onOpenUserManage} type="button">
                  <UserCog size={15} />
                  去用户管理
                </button>
                <button
                  className="primary-action"
                  disabled={selectedRoleReadonly || roleDraftMode === 'create' || !canUpdateRolePermissions || rolePermissionSaving}
                  onClick={onSubmitRolePermissions}
                  type="button"
                >
                  {rolePermissionSaving ? <Loader size={15} className="spin-icon" /> : <Check size={15} />}
                  保存权限
                </button>
              </div>
            </section>
          </div>
        </>
      )}
    </section>
  )
}
