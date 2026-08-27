import { FolderTree, Plus, RefreshCw, TriangleAlert } from 'lucide-react'
import { DepartmentMemberPanel } from '../../components/business/department/DepartmentMemberPanel'
import { DepartmentModals } from '../../components/business/department/DepartmentModals'
import { DepartmentSidePanel } from '../../components/business/department/DepartmentSidePanel'
import { DepartmentStatsCards } from '../../components/business/department/DepartmentStatsCards'
import { DepartmentTree } from '../../components/business/department/DepartmentTree'
import { useDepartmentManage } from '../../hooks/useDepartmentManage'

type RoleOption = {
  label: string
  value: string
}

type DepartmentManagePageProps = {
  canReadDepartments: boolean
  canCreateDepartments: boolean
  canUpdateDepartments: boolean
  canEnableDepartments: boolean
  roleOptions: RoleOption[]
  onAuthExpired: (error: unknown) => boolean
}

export function DepartmentManagePage({
  canReadDepartments,
  canCreateDepartments,
  canUpdateDepartments,
  canEnableDepartments,
  roleOptions,
  onAuthExpired,
}: DepartmentManagePageProps) {
  const { state, actions } = useDepartmentManage({ canReadDepartments, onAuthExpired })
  const memberRoleOptions = [{ label: '全部角色', value: 'ALL' }, ...roleOptions]

  return (
    <section className="app-content dept-page" aria-label="组织架构管理">
      <header className="dept-topbar">
        <div className="dept-title-block">
          <h2>组织架构管理</h2>
          <span>维护部门层级、部门成员和负责人</span>
        </div>
        <div className="dept-top-actions">
          <button disabled={state.orgTreeLoading || state.orgStatsLoading} onClick={actions.refresh} type="button">
            <RefreshCw size={16} />
            刷新
          </button>
          {!state.hasRootDepartment && (
            <button className="primary-action" disabled={!canCreateDepartments} onClick={() => actions.openCreateDepartment(0)} type="button">
              <Plus size={16} />
              创建顶级部门
            </button>
          )}
        </div>
      </header>

      {state.orgError ? (
        <div className="permission-state">
          <TriangleAlert size={42} />
          <strong>加载失败</strong>
          <p>{state.orgError}</p>
          <button className="retry-button" onClick={actions.fetchOrgTree} type="button">
            <RefreshCw size={16} /> 重试
          </button>
        </div>
      ) : state.orgTree.length === 0 && !state.orgTreeLoading ? (
        <div className="permission-state">
          <FolderTree size={42} />
          <strong>暂无部门数据</strong>
          <p>请先新建部门来创建组织架构。</p>
        </div>
      ) : (
        <>
          <DepartmentStatsCards stats={state.orgStats} />

          <div className="dept-layout org-layout">
            <DepartmentTree
              loading={state.orgTreeLoading}
              onCreateChild={actions.openCreateDepartment}
              onSelect={actions.selectDepartment}
              selectedId={state.selectedDeptId}
              tree={state.orgTree}
            />

            <DepartmentMemberPanel
              canCreateDepartments={canCreateDepartments}
              canEnableDepartments={canEnableDepartments}
              canUpdateDepartments={canUpdateDepartments}
              memberKeyword={state.deptMemberKeyword}
              memberPage={state.deptMemberPage}
              memberPageSize={state.deptMemberPageSize}
              memberRoleFilter={state.deptMemberRoleFilter}
              memberRoleOptions={memberRoleOptions}
              membersData={state.deptMembersData}
              membersError={state.deptMembersError}
              membersLoading={state.deptMembersLoading}
              onCreateChild={actions.openCreateDepartment}
              onEditDepartment={actions.openEditDepartment}
              onEnableDepartment={actions.openEnableConfirm}
              onKeywordChange={(keyword) => {
                actions.setDeptMemberKeyword(keyword)
                actions.setDeptMemberPage(1)
              }}
              onMoveDepartment={actions.openMoveDepartment}
              onOpenAddMember={actions.openAddMember}
              onOpenLeader={actions.openLeaderModal}
              onPageChange={actions.setDeptMemberPage}
              onPageSizeChange={(pageSize) => {
                actions.setDeptMemberPageSize(pageSize)
                actions.setDeptMemberPage(1)
              }}
              onRemoveMember={actions.removeDeptMember}
              onRoleFilterChange={(roleCode) => {
                actions.setDeptMemberRoleFilter(roleCode)
                actions.setDeptMemberPage(1)
              }}
              onSearch={actions.fetchDeptMembers}
              onSetLeader={actions.updateDeptLeader}
              roleOptions={roleOptions}
              selectedDeptNode={state.selectedDeptNode}
            />

            <DepartmentSidePanel
              orgDeptOptions={state.orgDeptOptions}
              selectedDeptNode={state.selectedDeptNode}
            />
          </div>
        </>
      )}

      <DepartmentModals
        addDeptMemberOpen={state.addDeptMemberOpen}
        deptCandidateKeyword={state.deptCandidateKeyword}
        deptCandidatesData={state.deptCandidatesData}
        deptCandidatesLoading={state.deptCandidatesLoading}
        deptConfirmOpen={state.deptConfirmOpen}
        deptConfirmTarget={state.deptConfirmTarget}
        deptEnableSubmitting={state.deptEnableSubmitting}
        deptForm={state.deptForm}
        deptFormError={state.deptFormError}
        deptFormMode={state.deptFormMode}
        deptFormOpen={state.deptFormOpen}
        deptFormSubmitting={state.deptFormSubmitting}
        deptLeaderOpen={state.deptLeaderOpen}
        deptLeaderSubmitting={state.deptLeaderSubmitting}
        deptLeaderUserId={state.deptLeaderUserId}
        deptMemberSubmitting={state.deptMemberSubmitting}
        deptMembersData={state.deptMembersData}
        deptMoveOpen={state.deptMoveOpen}
        deptMoveParentId={state.deptMoveParentId}
        deptMoveSubmitting={state.deptMoveSubmitting}
        hasRootDepartment={state.hasRootDepartment}
        movableParentOptions={state.movableParentOptions}
        onCandidateIdsChange={actions.setSelectedCandidateIds}
        onCandidateKeywordChange={actions.setDeptCandidateKeyword}
        onCloseAddMember={() => actions.setAddDeptMemberOpen(false)}
        onCloseConfirm={() => actions.setDeptConfirmOpen(false)}
        onCloseDeptForm={() => actions.setDeptFormOpen(false)}
        onCloseLeader={() => actions.setDeptLeaderOpen(false)}
        onCloseMove={() => actions.setDeptMoveOpen(false)}
        onDeptFormChange={actions.setDeptForm}
        onDeptLeaderUserChange={actions.setDeptLeaderUserId}
        onDeptMoveParentChange={actions.setDeptMoveParentId}
        onSearchCandidates={actions.fetchDeptCandidates}
        onSubmitAddMembers={actions.submitAddDeptMembers}
        onSubmitDeptForm={actions.submitDeptForm}
        onSubmitEnable={actions.submitDeptEnable}
        onSubmitLeader={actions.submitDeptLeader}
        onSubmitMove={actions.submitDeptMove}
        orgDeptOptions={state.orgDeptOptions}
        selectedCandidateIds={state.selectedCandidateIds}
        selectedDeptNode={state.selectedDeptNode}
      />
    </section>
  )
}
