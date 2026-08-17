import { Alert, Button, Checkbox, Empty, Form, Input, List, Modal, Select, Space, Spin, Switch } from 'antd'
import { Search, TriangleAlert } from 'lucide-react'
import type { DepartmentFormMode, DepartmentFormState, DepartmentMemberPageResponse, DepartmentNode } from '../../../types/department'

type DepartmentModalsProps = {
  deptFormOpen: boolean
  deptFormMode: DepartmentFormMode
  deptFormSubmitting: boolean
  deptFormError: string
  deptForm: DepartmentFormState
  hasRootDepartment: boolean
  orgDeptOptions: DepartmentNode[]
  deptMembersData: DepartmentMemberPageResponse | null
  selectedDeptNode: DepartmentNode | null
  deptMoveOpen: boolean
  deptMoveParentId: number
  deptMoveSubmitting: boolean
  movableParentOptions: DepartmentNode[]
  addDeptMemberOpen: boolean
  deptCandidateKeyword: string
  deptCandidatesData: DepartmentMemberPageResponse | null
  deptCandidatesLoading: boolean
  selectedCandidateIds: number[]
  deptMemberSubmitting: boolean
  deptLeaderOpen: boolean
  deptLeaderUserId: number
  deptLeaderSubmitting: boolean
  deptConfirmOpen: boolean
  deptConfirmTarget: DepartmentNode | null
  deptEnableSubmitting: boolean
  onCloseDeptForm: () => void
  onDeptFormChange: (form: DepartmentFormState) => void
  onSubmitDeptForm: () => void
  onCloseMove: () => void
  onDeptMoveParentChange: (parentId: number) => void
  onSubmitMove: () => void
  onCloseAddMember: () => void
  onCandidateKeywordChange: (keyword: string) => void
  onSearchCandidates: () => void
  onCandidateIdsChange: (userIds: number[]) => void
  onSubmitAddMembers: () => void
  onCloseLeader: () => void
  onDeptLeaderUserChange: (userId: number) => void
  onSubmitLeader: () => void
  onCloseConfirm: () => void
  onSubmitEnable: () => void
}

export function DepartmentModals(props: DepartmentModalsProps) {
  return (
    <>
      <DepartmentFormModal {...props} />
      <DepartmentMoveModal {...props} />
      <AddDepartmentMemberModal {...props} />
      <SetDepartmentLeaderModal {...props} />
      <DepartmentEnableConfirmModal {...props} />
    </>
  )
}

function DepartmentFormModal({
  deptFormOpen,
  deptFormMode,
  deptFormSubmitting,
  deptFormError,
  deptForm,
  hasRootDepartment,
  orgDeptOptions,
  deptMembersData,
  onCloseDeptForm,
  onDeptFormChange,
  onSubmitDeptForm,
}: DepartmentModalsProps) {
  return (
    <Modal
      cancelText="取消"
      confirmLoading={deptFormSubmitting}
      okButtonProps={{ disabled: !deptForm.deptName.trim() }}
      okText="保存"
      onCancel={onCloseDeptForm}
      onOk={onSubmitDeptForm}
      open={deptFormOpen}
      title={deptFormMode === 'create' ? '新建部门' : '编辑部门'}
      width={640}
    >
      <Form className="department-modal-form" layout="vertical">
        {deptFormError && <Alert title={deptFormError} showIcon type="error" />}
        {deptFormMode === 'create' && (
          <Form.Item label="上级部门">
            <Select
              onChange={(parentId) => onDeptFormChange({ ...deptForm, parentId })}
              options={[
                ...(!hasRootDepartment ? [{ label: '顶级部门', value: 0 }] : []),
                ...orgDeptOptions.map((department) => ({ label: department.deptName, value: department.id })),
              ]}
              value={deptForm.parentId}
            />
          </Form.Item>
        )}
        <Form.Item label="部门名称" required>
          <Input
            onChange={(event) => onDeptFormChange({ ...deptForm, deptName: event.target.value })}
            placeholder="例如：华北小组"
            value={deptForm.deptName}
          />
        </Form.Item>
        <Form.Item label="负责人">
          <Select
            onChange={(leaderUserId) => onDeptFormChange({ ...deptForm, leaderUserId })}
            options={[
              { label: '暂不设置', value: 0 },
              ...(deptMembersData?.records || []).map((member) => ({ label: member.displayName, value: member.id })),
            ]}
            value={deptForm.leaderUserId}
          />
        </Form.Item>
        <Form.Item label="状态">
          <Switch
            checked={deptForm.enabled}
            checkedChildren="启用"
            onChange={(enabled) => onDeptFormChange({ ...deptForm, enabled })}
            unCheckedChildren="停用"
          />
        </Form.Item>
        <Form.Item label="部门说明">
          <Input.TextArea
            onChange={(event) => onDeptFormChange({ ...deptForm, deptDesc: event.target.value })}
            placeholder="可选填写部门职责描述"
            rows={3}
            value={deptForm.deptDesc}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function DepartmentMoveModal({
  deptMoveOpen,
  selectedDeptNode,
  deptMoveParentId,
  deptMoveSubmitting,
  movableParentOptions,
  onCloseMove,
  onDeptMoveParentChange,
  onSubmitMove,
}: DepartmentModalsProps) {
  return (
    <Modal
      cancelText="取消"
      confirmLoading={deptMoveSubmitting}
      okText="确认移动"
      onCancel={onCloseMove}
      onOk={onSubmitMove}
      open={deptMoveOpen && Boolean(selectedDeptNode)}
      title="移动部门"
      width={520}
    >
      <Form className="department-modal-form" layout="vertical">
        <Alert title={selectedDeptNode ? `当前部门：${selectedDeptNode.deptName}` : '请选择部门'} showIcon type="info" />
        <Form.Item label="上级部门">
          <Select
            onChange={onDeptMoveParentChange}
            options={[
              ...(selectedDeptNode?.parentId == null ? [{ label: '顶级部门', value: 0 }] : []),
              ...movableParentOptions.map((department) => ({ label: department.deptName, value: department.id })),
            ]}
            value={deptMoveParentId}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function AddDepartmentMemberModal({
  addDeptMemberOpen,
  selectedDeptNode,
  deptCandidateKeyword,
  deptCandidatesData,
  deptCandidatesLoading,
  selectedCandidateIds,
  deptMemberSubmitting,
  onCloseAddMember,
  onCandidateKeywordChange,
  onSearchCandidates,
  onCandidateIdsChange,
  onSubmitAddMembers,
}: DepartmentModalsProps) {
  return (
    <Modal
      cancelText="取消"
      confirmLoading={deptMemberSubmitting}
      okButtonProps={{ disabled: selectedCandidateIds.length === 0 }}
      okText="确认添加"
      onCancel={onCloseAddMember}
      onOk={onSubmitAddMembers}
      open={addDeptMemberOpen && Boolean(selectedDeptNode)}
      title="添加成员"
      width={720}
    >
      <Space className="department-member-picker" orientation="vertical" size={12}>
        <Alert title={selectedDeptNode ? `加入部门：${selectedDeptNode.deptName}` : '请选择部门'} showIcon type="info" />
        <Input
          allowClear
          onChange={(event) => onCandidateKeywordChange(event.target.value)}
          onPressEnter={onSearchCandidates}
          placeholder="搜索姓名 / 账号 / 邮箱"
          prefix={<Search size={15} />}
          suffix={<Button size="small" type="link" onClick={onSearchCandidates}>查询</Button>}
          value={deptCandidateKeyword}
        />
        <Spin spinning={deptCandidatesLoading}>
          <List
            className="candidate-list antd-candidate-list"
            dataSource={deptCandidatesData?.records || []}
            locale={{ emptyText: <Empty description="暂无可添加成员" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
            renderItem={(candidate) => {
              const checked = selectedCandidateIds.includes(candidate.id)
              return (
                <List.Item className="candidate-row">
                  <Checkbox
                    checked={checked}
                    onChange={(event) => onCandidateIdsChange(
                      event.target.checked
                        ? [...selectedCandidateIds, candidate.id]
                        : selectedCandidateIds.filter((id) => id !== candidate.id),
                    )}
                  >
                    <span className="candidate-main">
                      <strong>{candidate.displayName}</strong>
                      <span>{candidate.email || candidate.account} / {candidate.departmentName || '未分配部门'}</span>
                    </span>
                  </Checkbox>
                  <span className={checked ? 'org-tag blue' : 'org-tag green'}>{checked ? '已选择' : '可添加'}</span>
                </List.Item>
              )
            }}
          />
        </Spin>
        <span className="modal-hint">已选择 {selectedCandidateIds.length} 人</span>
      </Space>
    </Modal>
  )
}

function SetDepartmentLeaderModal({
  deptLeaderOpen,
  selectedDeptNode,
  deptLeaderUserId,
  deptLeaderSubmitting,
  deptMembersData,
  onCloseLeader,
  onDeptLeaderUserChange,
  onSubmitLeader,
}: DepartmentModalsProps) {
  return (
    <Modal
      cancelText="取消"
      confirmLoading={deptLeaderSubmitting}
      okButtonProps={{ disabled: !deptLeaderUserId }}
      okText="保存"
      onCancel={onCloseLeader}
      onOk={onSubmitLeader}
      open={deptLeaderOpen && Boolean(selectedDeptNode)}
      title="设置负责人"
      width={520}
    >
      <Form className="department-modal-form" layout="vertical">
        <Alert title={selectedDeptNode ? `当前部门：${selectedDeptNode.deptName}` : '请选择部门'} showIcon type="info" />
        <Form.Item label="负责人">
          <Select
            onChange={onDeptLeaderUserChange}
            options={[
              { label: '请选择负责人', value: 0 },
              ...(deptMembersData?.records || []).map((member) => ({
                label: `${member.displayName} / ${member.email || member.account}`,
                value: member.id,
              })),
            ]}
            value={deptLeaderUserId}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function DepartmentEnableConfirmModal({
  deptConfirmOpen,
  deptConfirmTarget,
  deptEnableSubmitting,
  onCloseConfirm,
  onSubmitEnable,
}: DepartmentModalsProps) {
  const actionText = deptConfirmTarget?.enabled ? '停用' : '启用'

  return (
    <Modal
      cancelText="取消"
      confirmLoading={deptEnableSubmitting}
      okButtonProps={{ danger: deptConfirmTarget?.enabled }}
      okText={`确认${actionText}`}
      onCancel={onCloseConfirm}
      onOk={onSubmitEnable}
      open={deptConfirmOpen && Boolean(deptConfirmTarget)}
      title={`确认${actionText}`}
      width={520}
    >
      {deptConfirmTarget && (
        <div className="department-confirm-body">
          <p>
            <TriangleAlert size={18} />
            确认{actionText}部门 <strong>「{deptConfirmTarget.deptName}」</strong> 吗？
          </p>
          <Alert
            title={deptConfirmTarget.enabled ? '停用后，该部门及下级部门在组织树中不可见。' : '启用后，该部门在组织树中恢复显示。'}
            showIcon
            type={deptConfirmTarget.enabled ? 'warning' : 'info'}
          />
          <p className="modal-hint">
            {deptConfirmTarget.enabled
              ? '部门成员在用户管理中仍可查看，但不再归属该部门。'
              : '部门成员重新归属该部门，恢复数据范围权限。'}
          </p>
        </div>
      )}
    </Modal>
  )
}
