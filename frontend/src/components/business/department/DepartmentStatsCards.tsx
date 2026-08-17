import type { DepartmentStats } from '../../../types/department'

type DepartmentStatsCardsProps = {
  stats: DepartmentStats | null
}

export function DepartmentStatsCards({ stats }: DepartmentStatsCardsProps) {
  return (
    <div className="user-metrics">
      <div className="user-metric">
        <span>部门总数</span>
        <strong>{stats ? stats.totalDepartments : '--'}</strong>
        <small>启用 {stats ? stats.enabledDepartments : '--'}，停用 {stats ? stats.disabledDepartments : '--'}</small>
      </div>
      <div className="user-metric">
        <span>成员总数</span>
        <strong>{stats ? stats.memberCount : '--'}</strong>
        <small>已分配主部门成员</small>
      </div>
      <div className="user-metric">
        <span>负责人</span>
        <strong>{stats ? stats.leaderCount : '--'}</strong>
        <small>部门负责人数量</small>
      </div>
      <div className="user-metric">
        <span>未分配用户</span>
        <strong>{stats ? stats.unassignedUserCount : '--'}</strong>
        <small>待分配主部门</small>
      </div>
    </div>
  )
}
