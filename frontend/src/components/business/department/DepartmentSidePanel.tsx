import dayjs from 'dayjs'
import type { DepartmentNode } from '../../../types/department'

type DepartmentSidePanelProps = {
  selectedDeptNode: DepartmentNode | null
  orgDeptOptions: DepartmentNode[]
}

export function DepartmentSidePanel({ selectedDeptNode, orgDeptOptions }: DepartmentSidePanelProps) {
  return (
    <aside className="org-side">
      <section className="org-card">
        <div className="org-card-head">
          <div className="org-card-title">部门信息</div>
          {selectedDeptNode && <span className={selectedDeptNode.enabled ? 'badge-active' : 'badge-disabled'}>{selectedDeptNode.enabled ? '启用' : '停用'}</span>}
        </div>
        <div className="org-card-body">
          {selectedDeptNode ? (
            <div className="org-info-grid">
              <div><span>上级部门</span><strong>{selectedDeptNode.parentId ? orgDeptOptions.find((dept) => dept.id === selectedDeptNode.parentId)?.deptName || '-' : '顶级部门'}</strong></div>
              <div><span>负责人</span><strong>{selectedDeptNode.leaderDisplayName || '未设置'}</strong></div>
              <div><span>成员 / 下级</span><strong>{selectedDeptNode.memberCount} 人 / {selectedDeptNode.children.length} 个</strong></div>
              <div><span>部门层级</span><strong>{selectedDeptNode.parentId ? '下级部门' : '顶级部门'}</strong></div>
              <div><span>创建时间</span><strong>{selectedDeptNode.createdAt ? dayjs(selectedDeptNode.createdAt).format('YYYY-MM-DD') : '-'}</strong></div>
              <div><span>更新时间</span><strong>{selectedDeptNode.updatedAt ? dayjs(selectedDeptNode.updatedAt).format('YYYY-MM-DD HH:mm') : '-'}</strong></div>
            </div>
          ) : (
            <p>请选择部门。</p>
          )}
        </div>
      </section>

    </aside>
  )
}
