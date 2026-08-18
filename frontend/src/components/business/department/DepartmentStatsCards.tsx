import { Building2, UserCheck, UserRoundCheck, UserRoundX } from 'lucide-react'
import type { DepartmentStats } from '../../../types/department'

type DepartmentStatsCardsProps = {
  stats: DepartmentStats | null
}

export function DepartmentStatsCards({ stats }: DepartmentStatsCardsProps) {
  return (
    <section className="user-summary-strip dept-summary-strip" aria-label="组织统计">
      <div className="user-summary-item active">
        <span className="user-summary-icon"><Building2 size={17} /></span>
        <span className="user-summary-copy">
          <span>部门总数</span>
          <small>启用 {stats ? stats.enabledDepartments : '--'}，停用 {stats ? stats.disabledDepartments : '--'}</small>
        </span>
        <strong>{stats ? stats.totalDepartments : '--'}</strong>
      </div>
      <div className="user-summary-item">
        <span className="user-summary-icon success"><UserRoundCheck size={17} /></span>
        <span className="user-summary-copy">
          <span>成员总数</span>
          <small>已分配主部门成员</small>
        </span>
        <strong>{stats ? stats.memberCount : '--'}</strong>
      </div>
      <div className="user-summary-item">
        <span className="user-summary-icon info"><UserCheck size={17} /></span>
        <span className="user-summary-copy">
          <span>负责人</span>
          <small>部门负责人数量</small>
        </span>
        <strong>{stats ? stats.leaderCount : '--'}</strong>
      </div>
      <div className="user-summary-item">
        <span className="user-summary-icon warning"><UserRoundX size={17} /></span>
        <span className="user-summary-copy">
          <span>未分配用户</span>
          <small>待分配主部门</small>
        </span>
        <strong>{stats ? stats.unassignedUserCount : '--'}</strong>
      </div>
    </section>
  )
}
