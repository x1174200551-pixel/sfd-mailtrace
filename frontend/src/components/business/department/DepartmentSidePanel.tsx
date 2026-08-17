import dayjs from 'dayjs'
import type { DepartmentNode, DepartmentStats } from '../../../types/department'

type DepartmentSidePanelProps = {
  selectedDeptNode: DepartmentNode | null
  orgDeptOptions: DepartmentNode[]
  stats: DepartmentStats | null
  selectedDeptTotalMemberCount: number
}

export function DepartmentSidePanel({ selectedDeptNode, orgDeptOptions, stats, selectedDeptTotalMemberCount }: DepartmentSidePanelProps) {
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

      <section className="org-card">
        <div className="org-card-head">
          <div className="org-card-title">数据权限范围</div>
          <span className="org-tag blue">预览</span>
        </div>
        <div className="org-card-body">
          <div className="scope-list">
            <div className="scope-card"><div><strong>全部数据</strong><span className="org-tag orange">{stats?.memberCount ?? 0} 人</span></div><p>可查看当前工作空间内所有部门成员相关数据。</p></div>
            <div className="scope-card"><div><strong>仅本人</strong><span className="org-tag">1 人</span></div><p>只能查看自己负责或参与处理的数据。</p></div>
            <div className="scope-card"><div><strong>仅本部门</strong><span className="org-tag blue">{selectedDeptNode?.memberCount ?? 0} 人</span></div><p>只能查看当前部门直属成员相关数据。</p></div>
            <div className="scope-card"><div><strong>本部门及下级部门</strong><span className="org-tag green">{selectedDeptTotalMemberCount} 人</span></div><p>可查看当前部门及下级部门相关数据。</p></div>
          </div>
        </div>
      </section>
    </aside>
  )
}
