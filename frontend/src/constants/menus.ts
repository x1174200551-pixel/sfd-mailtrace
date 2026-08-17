import type { LucideIcon } from 'lucide-react'
import {
  Bell,
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
    title: '工作空间',
    permission: 'menu:workspace',
    items: [{ title: '工作台', icon: Home, permission: 'menu:dashboard', accessPermissions: ['dashboard:read'] }],
  },
  {
    title: '工单中心',
    permission: 'menu:ticket_center',
    items: [
      { title: '全部工单', icon: Layers, permission: 'menu:tickets', accessPermissions: ['ticket:read'] },
      { title: '客户管理', icon: Users, permission: 'menu:customers', accessPermissions: ['customer:read'] },
    ],
  },
  {
    title: '邮件管理',
    adminOnly: true,
    permission: 'menu:mail_management',
    items: [
      { title: '邮箱配置', icon: Settings, permission: 'menu:mailboxes', accessPermissions: ['menu:mailboxes', 'mailbox:read'] },
      { title: '收件记录', icon: Inbox, permission: 'menu:mail_fetch_logs', accessPermissions: ['mail_fetch_log:read'] },
      { title: '发件记录', icon: Send, tone: 'primary', permission: 'menu:mail_send_logs', accessPermissions: ['mail_send_log:read'] },
    ],
  },
  {
    title: 'SLA管理',
    adminOnly: true,
    permission: 'menu:sla_management',
    items: [
      { title: '分配规则', icon: ShieldCheck, permission: 'menu:assignment_rules', accessPermissions: ['assignment_rule:read'] },
      { title: 'SLA策略', icon: Timer, permission: 'menu:sla_policies', accessPermissions: ['sla_policy:read'] },
      { title: '工作日历', icon: CalendarDays, permission: 'menu:work_calendars', accessPermissions: ['work_calendar:read'] },
    ],
  },
  {
    title: '系统管理',
    adminOnly: true,
    permission: 'menu:system_management',
    items: [
      { title: '用户管理', icon: UserCog, adminOnly: true, permission: 'menu:users', accessPermissions: ['user:read'] },
      { title: '角色管理', icon: ShieldCheck, adminOnly: true, permission: 'menu:roles', accessPermissions: ['role:read'] },
      { title: '组织管理', icon: FolderTree, adminOnly: true, permission: 'menu:departments', accessPermissions: ['department:read'] },
      { title: '编号规则', icon: SlidersHorizontal, permission: 'menu:ticket_number_rule', accessPermissions: ['ticket_number_rule:read'] },
      { title: '通知模板', icon: Bell, permission: 'menu:notification_templates', accessPermissions: ['notification_template:read'] },
    ],
  },
]
