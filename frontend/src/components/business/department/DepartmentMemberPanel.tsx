import { Folder, Plus, PowerOff, Search, UserPlus } from 'lucide-react'
import { SwapOutlined } from '@ant-design/icons'
import { Button, Empty, Input, Pagination, Select, Space, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { DepartmentMemberPageResponse, DepartmentNode } from '../../../types/department'
import type { ManagedUser } from '../../../types/user'

type RoleOption = {
  label: string
  value: string
}

type DepartmentMemberPanelProps = {
  selectedDeptNode: DepartmentNode | null
  membersData: DepartmentMemberPageResponse | null
  membersLoading: boolean
  membersError: string
  memberKeyword: string
  memberRoleFilter: string
  memberPage: number
  memberPageSize: number
  roleOptions: RoleOption[]
  memberRoleOptions: RoleOption[]
  canCreateDepartments: boolean
  canUpdateDepartments: boolean
  canEnableDepartments: boolean
  onCreateChild: (parentId: number) => void
  onMoveDepartment: (department: DepartmentNode) => void
  onEditDepartment: (department: DepartmentNode) => void
  onEnableDepartment: (department: DepartmentNode) => void
  onOpenLeader: () => void
  onOpenAddMember: () => void
  onKeywordChange: (keyword: string) => void
  onRoleFilterChange: (roleCode: string) => void
  onSearch: () => void
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
  onSetLeader: (userId: number) => void
  onRemoveMember: (member: ManagedUser) => void
}

export function DepartmentMemberPanel({
  selectedDeptNode,
  membersData,
  membersLoading,
  membersError,
  memberKeyword,
  memberRoleFilter,
  memberPage,
  memberPageSize,
  roleOptions,
  memberRoleOptions,
  canCreateDepartments,
  canUpdateDepartments,
  canEnableDepartments,
  onCreateChild,
  onMoveDepartment,
  onEditDepartment,
  onEnableDepartment,
  onOpenLeader,
  onOpenAddMember,
  onKeywordChange,
  onRoleFilterChange,
  onSearch,
  onPageChange,
  onPageSizeChange,
  onSetLeader,
  onRemoveMember,
}: DepartmentMemberPanelProps) {
  const columns: ColumnsType<ManagedUser> = [
    {
      title: '成员',
      dataIndex: 'displayName',
      render: (_value, member) => (
        <>
          <strong>{member.displayName}</strong>
          <br />
          <span>{member.email || member.account}</span>
        </>
      ),
    },
    {
      title: '角色',
      dataIndex: 'roleCode',
      render: (roleCode: string) => <span className="org-tag">{roleOptions.find((role) => role.value === roleCode)?.label || roleCode}</span>,
    },
    {
      title: '主部门',
      dataIndex: 'departmentName',
      render: (departmentName: string | null) => departmentName || '-',
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      render: (enabled: boolean) => <span className={enabled ? 'badge-active' : 'badge-disabled'}>{enabled ? '启用' : '停用'}</span>,
    },
    {
      title: '操作',
      key: 'actions',
      align: 'right',
      render: (_value, member) => (
        <Space size={6}>
          <Button
            disabled={!canUpdateDepartments || selectedDeptNode?.leaderUserId === member.id}
            onClick={() => onSetLeader(member.id)}
            size="small"
          >
            设负责人
          </Button>
          <Button disabled={!canUpdateDepartments} onClick={() => onRemoveMember(member)} size="small">
            移出
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <section className="org-card org-member-card">
      <div className="org-card-head">
        <div>
          <div className="org-card-title"><Folder size={16} /> {selectedDeptNode?.deptName || '部门成员'}</div>
          <p>{selectedDeptNode ? `直属成员 ${selectedDeptNode.memberCount} 人` : '请选择部门'}</p>
        </div>
        {selectedDeptNode && (
          <div className="org-actions">
            <button
              aria-label="新建子部门"
              className="icon-only"
              disabled={!canCreateDepartments}
              onClick={() => onCreateChild(selectedDeptNode.id)}
              title="新建子部门"
              type="button"
            >
              <Plus size={14} />
            </button>
            <button disabled={!canUpdateDepartments} onClick={() => onMoveDepartment(selectedDeptNode)} type="button">
              <SwapOutlined /> 移动部门
            </button>
            <button disabled={!canUpdateDepartments} onClick={() => onEditDepartment(selectedDeptNode)} type="button">
              编辑
            </button>
            <button className="danger" disabled={!canEnableDepartments} onClick={() => onEnableDepartment(selectedDeptNode)} type="button">
              <PowerOff size={14} /> {selectedDeptNode.enabled ? '停用' : '启用'}
            </button>
          </div>
        )}
      </div>
      <div className="org-card-body">
        {!selectedDeptNode ? (
          <div className="org-empty">
            <Folder size={36} style={{ marginBottom: 8, color: '#d1d5db' }} />
            <p>请从左侧部门树中选择一个部门查看详情</p>
          </div>
        ) : (
          <div className="member-section">
            <div className="member-section-head">
              <h2>成员管理</h2>
              <div className="org-actions">
                <button disabled={!canUpdateDepartments || !membersData?.records.length} onClick={onOpenLeader} type="button">设置负责人</button>
                <button className="primary" disabled={!canUpdateDepartments} onClick={onOpenAddMember} type="button">
                  <UserPlus size={14} /> 添加成员
                </button>
              </div>
            </div>

            <div className="member-toolbar">
              <Input
                allowClear
                onChange={(event) => onKeywordChange(event.target.value)}
                placeholder="搜索姓名 / 账号 / 邮箱"
                prefix={<Search size={15} />}
                value={memberKeyword}
              />
              <Select
                onChange={onRoleFilterChange}
                options={memberRoleOptions}
                value={memberRoleFilter}
              />
              <Button icon={<Search size={15} />} onClick={onSearch}>查询</Button>
            </div>

            {membersError ? (
              <div className="org-empty"><p>{membersError}</p></div>
            ) : (
              <Table
                className="org-member-table"
                columns={columns}
                dataSource={membersData?.records || []}
                loading={membersLoading}
                locale={{ emptyText: <Empty description="暂无成员" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                pagination={false}
                rowKey="id"
                size="small"
              />
            )}

            <div className="org-pager">
              <span>共 {membersData?.total ?? 0} 条，当前第 {membersData?.page ?? memberPage} 页</span>
              <Pagination
                current={memberPage}
                onChange={(page, pageSize) => {
                  if (pageSize !== memberPageSize) {
                    onPageSizeChange(pageSize)
                    return
                  }
                  onPageChange(page)
                }}
                pageSize={memberPageSize}
                pageSizeOptions={[10, 20, 50]}
                showSizeChanger
                size="small"
                total={membersData?.total ?? 0}
              />
            </div>
          </div>
        )}
      </div>
    </section>
  )
}
