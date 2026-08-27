import type { LucideIcon } from 'lucide-react'
import {
  Bell,
  Building2,
  CalendarDays,
  FolderTree,
  Home,
  Inbox,
  Layers,
  Send,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Timer,
  UserCog,
  Users,
} from 'lucide-react'

export type MenuItem = {
  title: string
  icon: LucideIcon
  badge?: string
  tone?: 'primary' | 'warning' | 'danger' | 'new'
  adminOnly?: boolean
  permission?: string
  accessPermissions?: string[]
}

export type MenuGroup = {
  title: string
  items: MenuItem[]
  adminOnly?: boolean
  permission?: string
}

export const menuGroups: MenuGroup[] = [
  {
    title: '我的工作',
    permission: 'menu:workspace',
    items: [
      { title: '工作台', icon: Home, permission: 'menu:dashboard', accessPermissions: ['dashboard:read'] },
      { title: '全部工单', icon: Layers, permission: 'menu:tickets', accessPermissions: ['ticket:read'] },
      { title: '客户管理', icon: Users, permission: 'menu:customers', accessPermissions: ['customer:read'] },
    ],
  },
  {
    title: '企业配置',
    permission: 'menu:enterprise_config',
    items: [
      { title: '企业管理', icon: Building2, permission: 'menu:enterprises', accessPermissions: ['enterprise:read'] },
      { title: '邮箱配置', icon: Settings, permission: 'menu:mailboxes', accessPermissions: ['mailbox:read'] },
      { title: '通知模板', icon: Bell, permission: 'menu:notification_templates', accessPermissions: ['notification_template:read'] },
      { title: 'SLA策略', icon: Timer, permission: 'menu:sla_policies', accessPermissions: ['sla_policy:read'] },
      { title: '分配规则', icon: ShieldCheck, permission: 'menu:assignment_rules', accessPermissions: ['assignment_rule:read'] },
      { title: '工作日历', icon: CalendarDays, permission: 'menu:work_calendars', accessPermissions: ['work_calendar:read'] },
    ],
  },
  {
    title: '组织权限',
    permission: 'menu:organization_permissions',
    items: [
      { title: '组织管理', icon: FolderTree, permission: 'menu:departments', accessPermissions: ['department:read'] },
      { title: '用户管理', icon: UserCog, permission: 'menu:users', accessPermissions: ['user:read'] },
      { title: '角色管理', icon: ShieldCheck, permission: 'menu:roles', accessPermissions: ['role:read'] },
    ],
  },
  {
    title: '系统运维',
    permission: 'menu:system_operations',
    items: [
      { title: '收件记录', icon: Inbox, permission: 'menu:mail_fetch_logs', accessPermissions: ['mail_fetch_log:read'] },
      { title: '发件记录', icon: Send, tone: 'primary', permission: 'menu:mail_send_logs', accessPermissions: ['mail_send_log:read'] },
      { title: '编号规则', icon: SlidersHorizontal, permission: 'menu:ticket_number_rule', accessPermissions: ['ticket_number_rule:read'] },
    ],
  },
]
