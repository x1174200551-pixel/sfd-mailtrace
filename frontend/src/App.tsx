import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import TiptapRichEditor from './TiptapRichEditor'
import {
  Card, Table, Select, Button, Drawer, Tag, Row, Col, Modal,
  DatePicker, Empty, Typography, Alert, Input, Pagination,
  Tabs, Timeline, Descriptions, Space, Avatar, Segmented, Switch, Checkbox, message,
} from 'antd'
import {
  ArrowLeftOutlined,
  CloseCircleOutlined,
  CloseOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EllipsisOutlined,
  FileTextOutlined,
  FlagOutlined,
  PaperClipOutlined,
  ReloadOutlined,
  SearchOutlined,
  SendOutlined,
  StarTwoTone,
  SwapOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  Bell,
  CalendarDays,
  Check,
  ChevronDown,
  CircleCheck,
  CircleHelp,
  Clock,
  Edit3,
  Folder,
  Home,
  Inbox,
  Layers,
  Loader,
  LogOut,
  LockKeyhole,
  Mail,
  Menu,
  MessageCircle,
  PanelLeftClose,
  PanelLeftOpen,
  Plus,
  Power,
  PowerOff,
  RefreshCw,
  RotateCcw,
  Search,
  Send,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Timer,
  TriangleAlert,
  UserCog,
  UserPlus,
  UserRound,
  Users,
  X,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import './App.css'

type BasicResult<T> = {
  code: number
  message: string
  data: T
}

type CurrentUser = {
  id: number
  account: string
  displayName: string
  email: string
  roleCode: string
  roles?: string[]
  permissions?: string[]
  dataScopes?: Record<string, string[]>
}

type LoginResponse = {
  token: string
  tokenType: string
  expiresIn: number
  user: CurrentUser
}

type ModalState = {
  title: string
  text: string
} | null

class ApiError extends Error {
  status: number
  code?: number

  constructor(message: string, status: number, code?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

type MenuItem = {
  title: string
  icon: LucideIcon
  badge?: string
  tone?: 'primary' | 'warning' | 'danger' | 'new'
  adminOnly?: boolean
  permission?: string
}

type MenuGroup = {
  title: string
  items: MenuItem[]
  adminOnly?: boolean
  permission?: string
}

type RoleCode = string

type ManagedUser = {
  id: number
  account: string
  displayName: string
  email: string
  roleCode: RoleCode
  roleCodes?: string[]
  enabled: boolean
  lastLoginAt: string | null
  createdAt: string | null
  updatedAt: string | null
}

type UserSummary = {
  totalUsers: number
  enabledUsers: number
  disabledUsers: number
  adminUsers: number
  agentUsers: number
}

type UserPageResponse = {
  records: ManagedUser[]
  total: number
  page: number
  size: number
  pages: number
  summary: UserSummary
}

type UserFormState = {
  account: string
  displayName: string
  email: string
  roleCode: RoleCode
  roleCodes: string[]
  password: string
  enabled: boolean
}

type UserFormMode = 'create' | 'edit'

type UserConfirmAction = {
  title: string
  text: string
  actionLabel: string
  user: ManagedUser
  type: 'enable' | 'disable' | 'reset'
} | null

type RoleDataScope = {
  resourceType: string
  scopeCode: string
  scopeDesc: string | null
}

type ManagedRole = {
  id: number
  roleCode: string
  roleName: string
  roleDesc: string | null
  systemRole: boolean
  enabled: boolean
  sortOrder: number | null
  permissionCodes: string[]
  dataScopes: RoleDataScope[]
  userCount: number
  createdAt: string | null
  updatedAt: string | null
}

type RoleListResponse = {
  records: ManagedRole[]
  total: number
  enabledCount: number
  systemCount: number
  customCount: number
  permissionTotal: number
  userTotal: number
}

type PermissionTreeNode = {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: string
  moduleCode: string | null
  parentId: number | null
  children: PermissionTreeNode[]
}

type RoleFormState = {
  roleName: string
  roleDesc: string
  enabled: boolean
  permissionCodes: string[]
  dataScopes: RoleDataScope[]
}

type TemplateVariable = {
  key: string
  label: string
  sampleValue: string
}

type NotificationTemplate = {
  id: number
  templateCode: string
  templateName: string
  subjectTpl: string
  contentTpl: string
  enabled: boolean
  updatedAt: string | null
}

type NotificationTemplateSummary = {
  totalTemplates: number
  enabledTemplates: number
  disabledTemplates: number
  availableVariables: number
}

type NotificationTemplateListResponse = {
  records: NotificationTemplate[]
  summary: NotificationTemplateSummary
  variables: TemplateVariable[]
}

type TemplateFormState = {
  id: number | null
  templateCode: string
  templateName: string
  subjectTpl: string
  contentTpl: string
  enabled: boolean
}

type TemplatePreviewResponse = {
  subject: string
  content: string
}

type TicketNumberRule = {
  enabled: boolean
  prefix: string
  dateFormat: string
  seqLength: number
  separator: string
  description: string
  todayDate: string
  dateKey: string
  usedSeq: number
  nextSeq: string
  nextTicketNo: string
  subjectPreview: string
  updatedAt: string | null
}

type TicketRuleFormState = {
  enabled: boolean
  prefix: string
  dateFormat: string
  seqLength: number
  separator: string
  description: string
}

type SystemGroupKey = 'ticket' | 'mail' | 'notice' | 'security'

type MailboxConnectionStatus = 'UNKNOWN' | 'OK' | 'ERROR'
type MailboxStepKey = 'basic' | 'imap' | 'smtp' | 'reply' | 'test'

type Mailbox = {
  id: number
  mailboxName: string
  emailAddress: string
  enabled: boolean
  defaultAssigneeId: number | null
  defaultAssigneeName: string | null
  imapHost: string
  imapPort: number
  imapSslEnabled: boolean
  imapUsername: string
  imapFolder: string
  fetchIntervalSec: number
  smtpHost: string
  smtpPort: number
  smtpSslEnabled: boolean
  smtpUsername: string
  smtpFromName: string | null
  autoReplyEnabled: boolean
  autoReplyTemplateId: number | null
  lastFetchAt: string | null
  connectionStatus: MailboxConnectionStatus
  createdAt: string | null
  updatedAt: string | null
}

type MailboxSummary = {
  totalMailboxes: number
  enabledMailboxes: number
  disabledMailboxes: number
  okMailboxes: number
  errorMailboxes: number
  unknownMailboxes: number
}

type MailboxPageResponse = {
  records: Mailbox[]
  total: number
  page: number
  size: number
  pages: number
  summary: MailboxSummary
}

type MailboxFormState = {
  id: number | null
  mailboxName: string
  emailAddress: string
  enabled: boolean
  defaultAssigneeId: string
  imapHost: string
  imapPort: number
  imapSslEnabled: boolean
  imapUsername: string
  imapPassword: string
  imapFolder: string
  fetchIntervalSec: number
  smtpHost: string
  smtpPort: number
  smtpSslEnabled: boolean
  smtpUsername: string
  smtpPassword: string
  smtpFromName: string
  autoReplyEnabled: boolean
  autoReplyTemplateId: string
}

type MailboxConnectionTestResponse = {
  success: boolean
  connectionStatus: MailboxConnectionStatus
  imapSuccess: boolean
  imapMessage: string
  smtpSuccess: boolean
  smtpMessage: string
  testedAt: string
}

type MailboxConfirmAction = {
  title: string
  text: string
  actionLabel: string
  type: 'enable' | 'disable' | 'delete'
  mailbox: Mailbox
} | null

// ---- 拉取日志类型 ----

type MailFetchLog = {
  id: number
  mailboxId: number
  mailboxName: string | null
  emailAddress: string | null
  triggerType: string
  startedAt: string
  finishedAt: string | null
  success: boolean
  fetchedCount: number
  createdTicketCount: number
  linkedCount: number
  errorMessage: string | null
  createdAt: string
}

type MailFetchLogPageResponse = {
  records: MailFetchLog[]
  total: number
  page: number
  size: number
  pages: number
}

type MailFetchLogStats = {
  totalCount: number
  successCount: number
  failCount: number
  totalCreatedTickets: number
}

// ---- 工单类型 ----
type TicketSummary = {
  id: number
  ticketNo: string
  subject: string
  status: string
  priority: string
  customerEmail: string
  assigneeId: number | null
  assigneeName: string | null
  mailboxId: number
  mailboxName: string | null
  linkSuspect: boolean
  hasReplied: boolean
  slaResponseDeadline: string | null
  slaBreached: boolean
  createdAt: string
}

type TicketPageResponse = {
  records: TicketSummary[]
  total: number
  page: number
  size: number
  pages: number
}

type DashboardSummary = {
  totalCount: number
  pendingAssignCount: number
  processingCount: number
  waitingCustomerCount: number
  slaOverdueCount: number
  closedTodayCount: number
  activeCount: number
}

type DashboardTodoListResponse = {
  records: TicketSummary[]
  totalCount: number
  processingCount: number
  waitingCustomerCount: number
  slaOverdueCount: number
  limit: number
}

type CustomerReadonly = {
  id: number | null
  email: string
  displayName: string | null
  lastMailAt: string | null
  ticketCount: number
  remark: string | null
  createdAt: string | null
}

type CustomerPageResponse = {
  records: CustomerReadonly[]
  total: number
  page: number
  size: number
  pages: number
}

type TicketEvent = {
  id: number
  eventType: string
  eventContent: string
  operator: string
  eventAt: string
}

type TicketDetail = {
  id: number
  ticketNo: string
  subject: string
  status: string
  priority: string
  mailboxId: number
  mailboxName: string | null
  customerEmail: string
  assigneeId: number | null
  assigneeName: string | null
  linkSuspect: boolean
  slaBreached: boolean
  slaResponseDeadline: string | null
  slaResolveDeadline: string | null
  firstReplyAt: string | null
  closedAt: string | null
  remark: string | null
  createdAt: string
  updatedAt: string
  messages: any[]
  events: TicketEvent[]
}

type TicketAttachment = {
  id: number
  messageId: number | null
  fileName: string
  fileSize: number
  contentType: string | null
  downloadUrl: string | null
  uploadedBy: string | null
  createdAt: string
}

type UploadedFile = {
  objectKey: string
  fileName: string
  fileSize: number
  contentType: string
}

function relativeTime(t: string) {
  if (!t) return '-'
  const diff = Date.now() - new Date(t).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  return `${Math.floor(hours / 24)} 天前`
}

function formatFileSize(bytes: number) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + units[i]
}

const statusLabels: Record<string, string> = {
  PENDING_ASSIGN: '待处理',
  PROCESSING: '处理中',
  WAITING_CUSTOMER: '待客户回复',
  CLOSED: '已关闭',
  CANCELLED: '已取消',
}

function statusLabel(s: string) { return statusLabels[s] || s }
function priorityLabel(s: string) { return ({ LOW: '低', NORMAL: '普通', HIGH: '高', URGENT: '紧急' })[s] || s }

function priorityOptionLabel(s: string) {
  return ({ URGENT: 'P1 - 紧急', HIGH: 'P2 - 高', NORMAL: 'P3 - 普通', LOW: 'P4 - 低' } as Record<string, string>)[s] || s
}

function priorityBadgeText(s: string) {
  return ({ URGENT: 'P1', HIGH: 'P2', NORMAL: 'P3', LOW: 'P4' } as Record<string, string>)[s] || 'P3'
}

function priorityBadgeClass(s: string) {
  return ({ URGENT: 'p1', HIGH: 'p2', NORMAL: 'p3', LOW: 'p4' } as Record<string, string>)[s] || 'p3'
}

function dataResourceLabel(resourceType: string) {
  return ({ TICKET: '工单数据', CUSTOMER: '客户数据', DASHBOARD: '工作台数据' } as Record<string, string>)[resourceType] || resourceType
}

function dataScopeLabel(scopeCode: string) {
  return ({ ALL: '全部范围', SELF: '自己范围' } as Record<string, string>)[scopeCode] || scopeCode
}

function dataScopeDesc(resourceType: string, scopeCode: string) {
  const scope = dataScopeLabel(scopeCode)
  if (resourceType === 'TICKET') return scopeCode === 'ALL' ? '可查看全部工单' : '自己负责工单 + 未分配池'
  if (resourceType === 'CUSTOMER') return scopeCode === 'ALL' ? '全部客户聚合数据' : '自己可见工单关联客户'
  if (resourceType === 'DASHBOARD') return scopeCode === 'ALL' ? '全部工作台统计数据' : '自己负责工单 + 未分配池统计'
  return scope
}

function normalizeRoleCodes(primaryRoleCode: string, roleCodes: string[]) {
  const result = new Set<string>()
  const primary = toRoleCode(primaryRoleCode)
  if (primary) result.add(primary)
  roleCodes.map(toRoleCode).filter(Boolean).forEach((code) => result.add(code))
  return Array.from(result)
}

function collectPermissionNodes(nodes: PermissionTreeNode[]): PermissionTreeNode[] {
  return nodes.flatMap((node) => [node, ...collectPermissionNodes(node.children || [])])
}

function formatTicketEventContent(content: string) {
  return Object.entries(statusLabels).reduce(
    (text, [status, label]) => text.replaceAll(status, label),
    content,
  )
}

function getVisibleTicketEvents(events: TicketEvent[]) {
  const hasClosedEvent = events.some(ev => ev.eventType === 'CLOSED')
  return events
    .filter(ev => !(hasClosedEvent && ev.eventType === 'STATUS_CHANGED' && formatTicketEventContent(ev.eventContent).includes('→ 已关闭')))
    .map(ev => ({ ...ev, eventContent: formatTicketEventContent(ev.eventContent) }))
}

function isTerminalTicket(status: string) {
  return status === 'CLOSED' || status === 'CANCELLED'
}

function customerDisplayName(customer: CustomerReadonly | null) {
  return customer?.displayName?.trim() || customer?.email || '-'
}

function customerInitial(customer: CustomerReadonly | null) {
  const text = customerDisplayName(customer)
  return text === '-' ? '?' : text.slice(0, 1).toUpperCase()
}

function formatCustomerDate(value: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

/** 将 HTML 转文本并保留换行 */
function htmlToText(html: string): string {
  return html
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<\/div>/gi, '\n')
    .replace(/<\/li>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

/** 获取消息正文文本 */
function msgBodyText(msg: any): string {
  if (msg.contentText) return msg.contentText
  if (msg.contentHtml) return htmlToText(msg.contentHtml)
  if (msg.contentBody) return msg.contentBody
  return '(无内容)'
}

type MailSendLog = {
  id: number
  ticketId: number | null
  mailboxId: number | null
  sendType: string
  toAddress: string
  subject: string
  contentBody: string | null
  sendStatus: string
  retryCount: number
  maxRetry: number
  errorMessage: string | null
  sentAt: string | null
  createdAt: string
}

type MailSendLogPageResponse = {
  records: MailSendLog[]
  total: number
  page: number
  size: number
  pages: number
}

type AssignmentRuleMatchType = 'DEFAULT' | 'SUBJECT_KEYWORD' | 'MAILBOX' | 'FROM_EMAIL'

type AssignmentRule = {
  id: number
  ruleName: string
  enabled: boolean
  priorityOrder: number
  defaultRule: boolean
  matchType: AssignmentRuleMatchType
  matchValue: string | null
  assigneeId: number
  assigneeName: string | null
  notifyEnabled: boolean
  createdAt: string | null
  updatedAt: string | null
}

type AssignmentRuleSummary = {
  totalCount: number
  enabledCount: number
  disabledCount: number
  defaultCount: number
}

type AssignmentRuleListResponse = {
  records: AssignmentRule[]
  summary: AssignmentRuleSummary
}

type AssignmentRuleFormState = {
  id: number | null
  ruleName: string
  enabled: boolean
  priorityOrder: number
  defaultRule: boolean
  matchType: AssignmentRuleMatchType
  matchValue: string
  assigneeId: string
  notifyEnabled: boolean
}

type AssignmentRuleTestForm = {
  mailboxId: string
  subject: string
  fromEmail: string
}

type AssignmentRuleMatchResponse = {
  matched: boolean
  ruleId: number | null
  ruleName: string | null
  matchType: AssignmentRuleMatchType | null
  matchValue: string | null
  assigneeId: number | null
  assigneeName: string | null
  notifyEnabled: boolean | null
}

type AssignmentRuleConfirmAction = {
  type: 'delete'
  rule: AssignmentRule
} | null

type SlaPolicy = {
  id: number
  policyName: string
  enabled: boolean
  defaultPolicy: boolean
  responseHours: number
  resolveHours: number | null
  warningRemainHours: number
  escalateAfterBreachHours: number | null
  calendarId: number
  createdAt: string | null
  updatedAt: string | null
}

type SlaPolicySummary = {
  totalCount: number
  enabledCount: number
  disabledCount: number
  defaultCount: number
}

type SlaPolicyListResponse = {
  records: SlaPolicy[]
  summary: SlaPolicySummary
}

type SlaPolicyFormState = {
  id: number | null
  policyName: string
  enabled: boolean
  defaultPolicy: boolean
  responseHours: number
  resolveHours: string
  warningRemainHours: number
  escalateAfterBreachHours: string
  calendarId: string
}

type WorkCalendar = {
  id: number
  calendarName: string
  timezone: string
  workdays: number[]
  workStartTime: string
  workEndTime: string
  defaultCalendar: boolean
  createdAt: string | null
  updatedAt: string | null
}

type WorkCalendarListResponse = {
  records: WorkCalendar[]
  summary: {
    totalCount: number
    defaultCount: number
  }
}

type WorkCalendarFormState = {
  id: number | null
  calendarName: string
  timezone: string
  workdays: number[]
  workStartTime: string
  workEndTime: string
  defaultCalendar: boolean
}

type Holiday = {
  id: number
  calendarId: number
  holidayDate: string
  holidayName: string
  createdAt: string | null
  updatedAt: string | null
}

type HolidayListResponse = {
  records: Holiday[]
  summary: {
    totalCount: number
  }
}

type NationalHolidayPresetResponse = {
  year: number
  sourceName: string
  sourceUrl: string
  supportedYears: number[]
  records: Array<{
    holidayDate: string
    holidayName: string
  }>
  makeupWorkdayDates: string[]
}

type HolidayFormState = {
  id: number | null
  calendarId: string
  holidayDate: string
  holidayName: string
}

type SlaPolicyConfirmAction = {
  type: 'delete'
  policy: SlaPolicy
} | null

type WorkCalendarConfirmAction =
  | { type: 'delete-calendar'; calendar: WorkCalendar }
  | { type: 'delete-holiday'; holiday: Holiday }
  | null

const TOKEN_KEY = 'mailtrace_token'
const USER_KEY = 'mailtrace_user'
const REMEMBER_KEY = 'mailtrace_remember'

const menuGroups: MenuGroup[] = [
  {
    title: '工作空间',
    permission: 'menu:workspace',
    items: [{ title: '工作台', icon: Home, permission: 'menu:dashboard' }],
  },
  {
    title: '工单中心',
    permission: 'menu:ticket_center',
    items: [
      { title: '全部工单', icon: Layers, permission: 'menu:tickets' },
      { title: '客户管理', icon: Users, permission: 'menu:customers' },
    ],
  },
  {
    title: '邮件管理',
    adminOnly: true,
    permission: 'menu:mail_management',
    items: [
      { title: '邮箱配置', icon: Settings, permission: 'menu:mailboxes' },
      { title: '收件记录', icon: Inbox, permission: 'menu:mail_fetch_logs' },
      { title: '发件记录', icon: Send, tone: 'primary', permission: 'menu:mail_send_logs' },
    ],
  },
  {
    title: 'SLA管理',
    adminOnly: true,
    permission: 'menu:sla_management',
    items: [
      { title: '分配规则', icon: ShieldCheck, permission: 'menu:assignment_rules' },
      { title: 'SLA策略', icon: Timer, permission: 'menu:sla_policies' },
      { title: '工作日历', icon: CalendarDays, permission: 'menu:work_calendars' },
    ],
  },
  {
    title: '系统管理',
    adminOnly: true,
    permission: 'menu:system_management',
    items: [
      { title: '用户管理', icon: UserCog, adminOnly: true, permission: 'menu:users' },
      { title: '角色管理', icon: ShieldCheck, adminOnly: true, permission: 'menu:roles' },
      { title: '编号规则', icon: SlidersHorizontal, permission: 'menu:ticket_number_rule' },
      { title: '通知模板', icon: Bell, permission: 'menu:notification_templates' },
    ],
  },
]

const builtInRoleOptions: Array<{ label: string; value: RoleCode }> = [
  { label: '管理员', value: 'ADMIN' },
  { label: '客服处理人', value: 'AGENT' },
]

const roleProfiles: Record<RoleCode, {
  title: string
  subtitle: string
  menuScope: string
  dataScope: string
  permissionCount: number
  actions: string[]
}> = {
  ADMIN: {
    title: '管理员',
    subtitle: '拥有全部后台配置、工单处理和系统维护权限',
    menuScope: '全部菜单',
    dataScope: '全部范围',
    permissionCount: 83,
    actions: ['用户管理', '邮箱配置', '分配规则', 'SLA 策略', '工单处理', '系统配置'],
  },
  AGENT: {
    title: '客服处理人',
    subtitle: '处理自己负责和未分配池内的工单，查看相关客户数据',
    menuScope: '工作台、全部工单、客户管理',
    dataScope: '自己范围',
    permissionCount: 20,
    actions: ['查看工单', '领取工单', '回复客户', '内部备注', '转派工单', '查看客户'],
  },
}

const emptyUserForm: UserFormState = {
  account: '',
  displayName: '',
  email: '',
  roleCode: 'AGENT',
  roleCodes: ['AGENT'],
  password: '',
  enabled: true,
}

const emptyRoleForm: RoleFormState = {
  roleName: '',
  roleDesc: '',
  enabled: true,
  permissionCodes: [],
  dataScopes: [
    { resourceType: 'TICKET', scopeCode: 'SELF', scopeDesc: '自己负责工单 + 未分配池' },
    { resourceType: 'CUSTOMER', scopeCode: 'SELF', scopeDesc: '自己可见工单关联客户' },
    { resourceType: 'DASHBOARD', scopeCode: 'SELF', scopeDesc: '自己负责工单 + 未分配池统计' },
  ],
}

const emptyTemplateForm: TemplateFormState = {
  id: null,
  templateCode: '',
  templateName: '',
  subjectTpl: '',
  contentTpl: '',
  enabled: true,
}

const emptyTicketRuleForm: TicketRuleFormState = {
  enabled: true,
  prefix: 'TCK',
  dateFormat: 'yyyyMMdd',
  seqLength: 4,
  separator: '-',
  description: '客户来信自动建单时生成唯一工单号；邮件线程关联会优先匹配主题中的工单号。',
}

const emptyMailboxForm: MailboxFormState = {
  id: null,
  mailboxName: '',
  emailAddress: '',
  enabled: true,
  defaultAssigneeId: '',
  imapHost: '',
  imapPort: 993,
  imapSslEnabled: true,
  imapUsername: '',
  imapPassword: '',
  imapFolder: 'INBOX',
  fetchIntervalSec: 120,
  smtpHost: '',
  smtpPort: 587,
  smtpSslEnabled: true,
  smtpUsername: '',
  smtpPassword: '',
  smtpFromName: '',
  autoReplyEnabled: true,
  autoReplyTemplateId: '',
}

const emptyAssignmentRuleForm: AssignmentRuleFormState = {
  id: null,
  ruleName: '',
  enabled: true,
  priorityOrder: 100,
  defaultRule: false,
  matchType: 'SUBJECT_KEYWORD',
  matchValue: '',
  assigneeId: '',
  notifyEnabled: true,
}

const emptyAssignmentRuleTestForm: AssignmentRuleTestForm = {
  mailboxId: '',
  subject: '',
  fromEmail: '',
}

const emptySlaPolicyForm: SlaPolicyFormState = {
  id: null,
  policyName: '',
  enabled: true,
  defaultPolicy: false,
  responseHours: 4,
  resolveHours: '24',
  warningRemainHours: 1,
  escalateAfterBreachHours: '2',
  calendarId: '',
}

const emptyWorkCalendarForm: WorkCalendarFormState = {
  id: null,
  calendarName: '',
  timezone: 'Asia/Shanghai',
  workdays: [1, 2, 3, 4, 5],
  workStartTime: '09:00',
  workEndTime: '18:00',
  defaultCalendar: false,
}

const emptyHolidayForm: HolidayFormState = {
  id: null,
  calendarId: '',
  holidayDate: dayjs().format('YYYY-MM-DD'),
  holidayName: '',
}

const slaPreviewBaseTime = dayjs('2026-07-27T15:30:00')
const calendarPreviewBaseTime = dayjs('2026-10-01T15:30:00')
const weekdayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const mailboxSteps: Array<{ key: MailboxStepKey; label: string }> = [
  { key: 'basic', label: '基础信息' },
  { key: 'imap', label: '收信配置' },
  { key: 'smtp', label: '发信配置' },
  { key: 'reply', label: '自动回复' },
  { key: 'test', label: '连接测试' },
]

const systemGroups: Array<{
  key: SystemGroupKey
  title: string
  summary: string
  detail: string
  owner: string
}> = [
  {
    key: 'ticket',
    title: '工单编号规则',
    summary: '前缀、日期格式、流水位数',
    detail: '业务人员可在此维护工单号生成规则，保存后仅影响后续新建工单。',
    owner: '业务可配',
  },
  {
    key: 'mail',
    title: '邮件处理策略',
    summary: '重试次数、拉取间隔等由管理员维护',
    detail: '邮件轮询、重试、附件限制等参数会影响后台任务稳定性，当前页面仅展示边界，不开放业务录入。',
    owner: '运维维护',
  },
  {
    key: 'notice',
    title: '通知与提醒',
    summary: 'SLA 预警、分配通知开关',
    detail: '通知内容已在通知模板中维护，提醒策略后续会随 SLA 模块单独设计，不在编号规则页混合编辑。',
    owner: '后续设计',
  },
  {
    key: 'security',
    title: '安全与审计',
    summary: '会话超时、操作日志保留',
    detail: '会话、安全和审计保留属于系统级策略，后续按管理员能力单独设计，不由业务人员直接填写参数。',
    owner: '管理员维护',
  },
]

const templateScenes: Record<string, string> = {
  AUTO_REPLY: '客户来信自动建单',
  ASSIGN_NOTIFY: '工单分配处理人',
  AGENT_REPLY: '处理人回复客户',
  SLA_WARNING: 'SLA 即将超时',
  SLA_BREACH: 'SLA 已超时',
}

const features = [
  { mark: 'M', title: '自动收取邮件', text: 'IMAP 实时同步' },
  { mark: 'T', title: '自动生成工单', text: '智能解析，快速建单' },
  { mark: 'U', title: '分配处理人', text: '按规则自动分配' },
  { mark: 'S', title: 'SLA 监控', text: '超时提醒，保障服务' },
]

function readStoredSession() {
  const rememberText = localStorage.getItem(REMEMBER_KEY)
  const remember = rememberText == null ? true : rememberText === 'true'
  const token = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || ''
  const userText = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY)
  let user: CurrentUser | null = null

  if (userText) {
    try {
      user = JSON.parse(userText) as CurrentUser
    } catch {
      user = null
    }
  }

  return { remember, token, user }
}

function storeSession(payload: LoginResponse, remember: boolean) {
  const persistentStore = remember ? localStorage : sessionStorage
  const volatileStore = remember ? sessionStorage : localStorage

  volatileStore.removeItem(TOKEN_KEY)
  volatileStore.removeItem(USER_KEY)
  persistentStore.setItem(TOKEN_KEY, payload.token)
  persistentStore.setItem(USER_KEY, JSON.stringify(payload.user))
  localStorage.setItem(REMEMBER_KEY, String(remember))
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(REMEMBER_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

function readStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || ''
}

async function requestApi<T>(url: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers)
  const isFormData = options.body instanceof FormData
  if (!headers.has('Content-Type') && !isFormData) {
    headers.set('Content-Type', 'application/json')
  }
  if (!headers.has('Authorization') && !url.includes('/auth/login')) {
    const storedToken = readStoredToken()
    if (storedToken) {
      headers.set('Authorization', `Bearer ${storedToken}`)
    }
  }

  const response = await fetch(url, {
    ...options,
    headers: {
      ...Object.fromEntries(headers.entries()),
    },
  })
  const body = (await response.json().catch(() => ({
    code: response.status,
    message: `请求失败：${response.status}`,
    data: null,
  }))) as BasicResult<T>

  if (!response.ok || body.code !== 0) {
    throw new ApiError(body.message || `请求失败：${response.status}`, response.status, body.code)
  }

  return body.data
}

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` }
}

function formatDateTime(value: string | null) {
  if (!value) return '未登录'
  return value.replace('T', ' ').slice(0, 16)
}

function formatOptionalDateTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatSyncTime(value: string | null) {
  if (!value) return '未同步'
  return value.replace('T', ' ').slice(0, 16)
}

function roleLabel(roleCode: string) {
  return roleProfiles[roleCode]?.title || roleCode || '-'
}

function getRoleProfile(roleCode: string) {
  return roleProfiles[roleCode] || {
    title: roleLabel(roleCode),
    subtitle: '自定义业务角色',
    menuScope: '按角色权限配置',
    dataScope: '按默认数据范围',
    permissionCount: 0,
    actions: ['按权限清单生效'],
  }
}

function normalizeCodeList(values?: string[]) {
  return new Set((values ?? []).map((value) => value.trim()).filter(Boolean))
}

function userHasRole(currentUser: CurrentUser | null, roleCode: string) {
  const normalized = roleCode.trim().toUpperCase()
  if (!normalized || !currentUser) return false
  return normalizeCodeList(currentUser.roles).has(normalized) || currentUser.roleCode === normalized
}

function userHasPermission(currentUser: CurrentUser | null, permissionCode: string) {
  if (!currentUser || !permissionCode.trim()) return false
  const permissions = normalizeCodeList(currentUser.permissions)
  if (userHasRole(currentUser, 'ADMIN')) {
    return permissions.size === 0 || permissions.has(permissionCode) || permissionCode.startsWith('role:')
  }
  if (permissions.size > 0) {
    return permissions.has(permissionCode)
  }
  return currentUser.roleCode === 'ADMIN'
}

function templateSceneLabel(templateCode: string) {
  return templateScenes[templateCode] || '自定义通知场景'
}

function formatDuration(start: string, end: string) {
  const diff = new Date(end).getTime() - new Date(start).getTime()
  if (diff < 0) return '-'
  const sec = Math.round(diff / 1000)
  if (sec < 60) return `${sec}s`
  if (sec < 3600) return `${Math.floor(sec / 60)}m${sec % 60}s`
  return `${Math.floor(sec / 3600)}h${Math.floor((sec % 3600) / 60)}m`
}

function toRoleCode(value: string): RoleCode {
  return value.trim().toUpperCase()
}

function toTemplateForm(template: NotificationTemplate): TemplateFormState {
  return {
    id: template.id,
    templateCode: template.templateCode,
    templateName: template.templateName,
    subjectTpl: template.subjectTpl,
    contentTpl: template.contentTpl,
    enabled: template.enabled,
  }
}

function toTicketRuleForm(rule: TicketNumberRule): TicketRuleFormState {
  return {
    enabled: rule.enabled,
    prefix: rule.prefix,
    dateFormat: rule.dateFormat,
    seqLength: rule.seqLength,
    separator: rule.separator,
    description: rule.description,
  }
}

function toMailboxForm(mailbox: Mailbox): MailboxFormState {
  return {
    id: mailbox.id,
    mailboxName: mailbox.mailboxName,
    emailAddress: mailbox.emailAddress,
    enabled: mailbox.enabled,
    defaultAssigneeId: mailbox.defaultAssigneeId == null ? '' : String(mailbox.defaultAssigneeId),
    imapHost: mailbox.imapHost,
    imapPort: mailbox.imapPort,
    imapSslEnabled: mailbox.imapSslEnabled,
    imapUsername: mailbox.imapUsername,
    imapPassword: '',
    imapFolder: mailbox.imapFolder,
    fetchIntervalSec: mailbox.fetchIntervalSec,
    smtpHost: mailbox.smtpHost,
    smtpPort: mailbox.smtpPort,
    smtpSslEnabled: mailbox.smtpSslEnabled,
    smtpUsername: mailbox.smtpUsername,
    smtpPassword: '',
    smtpFromName: mailbox.smtpFromName || '',
    autoReplyEnabled: mailbox.autoReplyEnabled,
    autoReplyTemplateId: mailbox.autoReplyTemplateId == null ? '' : String(mailbox.autoReplyTemplateId),
  }
}

function toAssignmentRuleForm(rule: AssignmentRule): AssignmentRuleFormState {
  return {
    id: rule.id,
    ruleName: rule.ruleName,
    enabled: rule.enabled,
    priorityOrder: rule.priorityOrder,
    defaultRule: rule.defaultRule,
    matchType: rule.matchType,
    matchValue: rule.matchValue || '',
    assigneeId: String(rule.assigneeId),
    notifyEnabled: rule.notifyEnabled,
  }
}

function toSlaPolicyForm(policy: SlaPolicy): SlaPolicyFormState {
  return {
    id: policy.id,
    policyName: policy.policyName,
    enabled: policy.enabled,
    defaultPolicy: policy.defaultPolicy,
    responseHours: policy.responseHours,
    resolveHours: policy.resolveHours == null ? '' : String(policy.resolveHours),
    warningRemainHours: policy.warningRemainHours,
    escalateAfterBreachHours: policy.escalateAfterBreachHours == null ? '' : String(policy.escalateAfterBreachHours),
    calendarId: String(policy.calendarId),
  }
}

function toWorkCalendarForm(calendar: WorkCalendar): WorkCalendarFormState {
  return {
    id: calendar.id,
    calendarName: calendar.calendarName,
    timezone: calendar.timezone,
    workdays: calendar.workdays,
    workStartTime: calendar.workStartTime,
    workEndTime: calendar.workEndTime,
    defaultCalendar: calendar.defaultCalendar,
  }
}

function toHolidayForm(holiday: Holiday): HolidayFormState {
  return {
    id: holiday.id,
    calendarId: String(holiday.calendarId),
    holidayDate: holiday.holidayDate,
    holidayName: holiday.holidayName,
  }
}

function mailboxStatusLabel(status: MailboxConnectionStatus) {
  if (status === 'OK') return '正常'
  if (status === 'ERROR') return '异常'
  return '未知'
}

function secondsLabel(value: number) {
  if (value % 60 === 0) return `${value / 60} 分钟`
  return `${value} 秒`
}

function assignmentMatchTypeLabel(value: string | null) {
  return ({
    DEFAULT: '默认兜底',
    SUBJECT_KEYWORD: '主题关键词',
    MAILBOX: '来源邮箱',
    FROM_EMAIL: '客户邮箱',
  } as Record<string, string>)[value || ''] || value || '-'
}

function assignmentRuleText(rule: AssignmentRule) {
  if (rule.matchType === 'DEFAULT') return 'DEFAULT · 未命中其他规则时兜底'
  return `${assignmentMatchTypeLabel(rule.matchType)} = ${rule.matchValue || '-'}`
}

function workdayLabel(workdays?: number[]) {
  if (!workdays || workdays.length === 0) return '未配置'
  const sorted = [...workdays].sort((a, b) => a - b)
  if (sorted.join(',') === '1,2,3,4,5') return '周一至周五'
  if (sorted.join(',') === '1,2,3,4,5,6,7') return '周一至周日'
  return sorted.map((day) => weekdayNames[day - 1] || `周${day}`).join('、')
}

function hoursLabel(value: number | null | undefined, fallback = '未配置') {
  if (value == null || Number.isNaN(value)) return fallback
  return `${value} 工作小时`
}

function parseClockMinutes(value: string | undefined, fallback: number) {
  const [hourText, minuteText] = (value || '').split(':')
  const hour = Number(hourText)
  const minute = Number(minuteText || 0)
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return fallback
  return hour * 60 + minute
}

function calendarDayNumber(value: dayjs.Dayjs) {
  const day = value.day()
  return day === 0 ? 7 : day
}

function withClockMinutes(value: dayjs.Dayjs, minutes: number) {
  return value.hour(Math.floor(minutes / 60)).minute(minutes % 60).second(0).millisecond(0)
}

function nextWorkStart(value: dayjs.Dayjs, calendar: WorkCalendar) {
  const startMinutes = parseClockMinutes(calendar.workStartTime, 9 * 60)
  const endMinutes = parseClockMinutes(calendar.workEndTime, 18 * 60)
  let cursor = value
  for (let i = 0; i < 14; i += 1) {
    const isWorkday = calendar.workdays.includes(calendarDayNumber(cursor))
    const startAt = withClockMinutes(cursor, startMinutes)
    const endAt = withClockMinutes(cursor, endMinutes)
    if (!isWorkday || !endAt.isAfter(startAt)) {
      cursor = cursor.add(1, 'day').startOf('day')
      continue
    }
    if (cursor.isBefore(startAt)) return startAt
    if (cursor.isBefore(endAt)) return cursor
    cursor = cursor.add(1, 'day').startOf('day')
  }
  return value
}

function nextWorkStartWithHolidays(value: dayjs.Dayjs, calendar: WorkCalendar, holidayDates: Set<string>) {
  const startMinutes = parseClockMinutes(calendar.workStartTime, 9 * 60)
  const endMinutes = parseClockMinutes(calendar.workEndTime, 18 * 60)
  let cursor = value
  for (let i = 0; i < 30; i += 1) {
    const dateKey = cursor.format('YYYY-MM-DD')
    const isWorkday = calendar.workdays.includes(calendarDayNumber(cursor)) && !holidayDates.has(dateKey)
    const startAt = withClockMinutes(cursor, startMinutes)
    const endAt = withClockMinutes(cursor, endMinutes)
    if (!isWorkday || !endAt.isAfter(startAt)) {
      cursor = cursor.add(1, 'day').startOf('day')
      continue
    }
    if (cursor.isBefore(startAt)) return startAt
    if (cursor.isBefore(endAt)) return cursor
    cursor = cursor.add(1, 'day').startOf('day')
  }
  return value
}

function addWorkHours(value: dayjs.Dayjs, hours: number, calendar: WorkCalendar | null) {
  if (!calendar) return value.add(hours, 'hour')
  const endMinutes = parseClockMinutes(calendar.workEndTime, 18 * 60)
  let remainingMinutes = Math.max(0, Math.round(hours * 60))
  let cursor = nextWorkStart(value, calendar)

  for (let i = 0; i < 100 && remainingMinutes > 0; i += 1) {
    const endAt = withClockMinutes(cursor, endMinutes)
    const availableMinutes = Math.max(0, endAt.diff(cursor, 'minute'))
    if (remainingMinutes <= availableMinutes) {
      return cursor.add(remainingMinutes, 'minute')
    }
    remainingMinutes -= availableMinutes
    cursor = nextWorkStart(cursor.add(1, 'day').startOf('day'), calendar)
  }

  return cursor
}

function addWorkHoursWithHolidays(value: dayjs.Dayjs, hours: number, calendar: WorkCalendar | null, holidayDates: Set<string>) {
  if (!calendar) return value.add(hours, 'hour')
  const endMinutes = parseClockMinutes(calendar.workEndTime, 18 * 60)
  let remainingMinutes = Math.max(0, Math.round(hours * 60))
  let cursor = nextWorkStartWithHolidays(value, calendar, holidayDates)

  for (let i = 0; i < 120 && remainingMinutes > 0; i += 1) {
    const endAt = withClockMinutes(cursor, endMinutes)
    const availableMinutes = Math.max(0, endAt.diff(cursor, 'minute'))
    if (remainingMinutes <= availableMinutes) {
      return cursor.add(remainingMinutes, 'minute')
    }
    remainingMinutes -= availableMinutes
    cursor = nextWorkStartWithHolidays(cursor.add(1, 'day').startOf('day'), calendar, holidayDates)
  }

  return cursor
}

function resolveSlaPreview(form: SlaPolicyFormState, calendar: WorkCalendar | null) {
  const responseHours = Math.max(1, Number(form.responseHours) || 1)
  const resolveHours = form.resolveHours.trim() ? Math.max(1, Number(form.resolveHours) || responseHours) : null
  const warningHours = Math.max(1, Number(form.warningRemainHours) || 1)
  const escalateHours = form.escalateAfterBreachHours.trim()
    ? Math.max(1, Number(form.escalateAfterBreachHours) || 1)
    : null
  const responseDeadline = addWorkHours(slaPreviewBaseTime, responseHours, calendar)
  const resolveDeadline = resolveHours == null ? null : addWorkHours(slaPreviewBaseTime, resolveHours, calendar)
  const warningAt = resolveDeadline
    ? addWorkHours(resolveDeadline, -warningHours, null)
    : addWorkHours(responseDeadline, -warningHours, null)
  const escalateAt = resolveDeadline && escalateHours != null ? addWorkHours(resolveDeadline, escalateHours, calendar) : null

  return {
    responseDeadline,
    resolveDeadline,
    warningAt,
    escalateAt,
  }
}

function resolveCalendarSlaExample(
  calendar: WorkCalendar | null,
  holidays: Holiday[],
  createdAt: dayjs.Dayjs,
  responseHours: number,
  resolveHours: number,
) {
  const holidayDates = new Set(holidays.map((holiday) => holiday.holidayDate))
  const startAt = calendar ? nextWorkStartWithHolidays(createdAt, calendar, holidayDates) : createdAt
  return {
    startAt,
    responseDeadline: addWorkHoursWithHolidays(createdAt, responseHours, calendar, holidayDates),
    resolveDeadline: addWorkHoursWithHolidays(createdAt, resolveHours, calendar, holidayDates),
  }
}

function buildMonthCells(month: string, calendar: WorkCalendar | null, holidays: Holiday[]) {
  const monthStart = dayjs(`${month}-01`)
  const gridStart = monthStart.subtract(calendarDayNumber(monthStart) - 1, 'day')
  const holidayMap = new Map(holidays.map((holiday) => [holiday.holidayDate, holiday.holidayName]))

  return Array.from({ length: 42 }, (_value, index) => {
    const date = gridStart.add(index, 'day')
    const dateKey = date.format('YYYY-MM-DD')
    const holidayName = holidayMap.get(dateKey) || ''
    const inMonth = date.isSame(monthStart, 'month')
    const isWorkday = Boolean(calendar?.workdays.includes(calendarDayNumber(date))) && !holidayName
    return {
      date,
      dateKey,
      inMonth,
      holidayName,
      isWorkday,
      isToday: date.isSame(dayjs(), 'day'),
    }
  })
}

function App() {
  const initialSession = useMemo(readStoredSession, [])
  const [account, setAccount] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState<boolean>(initialSession.remember)
  const [showPassword, setShowPassword] = useState(false)
  const [token, setToken] = useState(initialSession.token)
  const [user, setUser] = useState<CurrentUser | null>(initialSession.user)
  const [formError, setFormError] = useState('')
  const [accountError, setAccountError] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [checkingSession, setCheckingSession] = useState(Boolean(initialSession.token))
  const [modal, setModal] = useState<ModalState>(null)
  const [activeMenu, setActiveMenu] = useState('工作台')
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [userKeyword, setUserKeyword] = useState('')
  const [userRoleFilter, setUserRoleFilter] = useState('ALL')
  const [userEnabledFilter, setUserEnabledFilter] = useState('ALL')
  const [userPage, setUserPage] = useState(1)
  const [userPageSize, setUserPageSize] = useState(10)
  const [usersData, setUsersData] = useState<UserPageResponse | null>(null)
  const [usersLoading, setUsersLoading] = useState(false)
  const [usersError, setUsersError] = useState('')
  const [userFormMode, setUserFormMode] = useState<UserFormMode>('create')
  const [userFormOpen, setUserFormOpen] = useState(false)
  const [userFormSubmitting, setUserFormSubmitting] = useState(false)
  const [userFormError, setUserFormError] = useState('')
  const [userForm, setUserForm] = useState<UserFormState>(emptyUserForm)
  const [editingUser, setEditingUser] = useState<ManagedUser | null>(null)
  const [confirmAction, setConfirmAction] = useState<UserConfirmAction>(null)
  const [actionLoading, setActionLoading] = useState(false)
  const [roleKeyword, setRoleKeyword] = useState('')
  const [roleEnabledFilter, setRoleEnabledFilter] = useState('ALL')
  const [rolesData, setRolesData] = useState<RoleListResponse | null>(null)
  const [rolesLoading, setRolesLoading] = useState(false)
  const [rolesError, setRolesError] = useState('')
  const [permissionTree, setPermissionTree] = useState<PermissionTreeNode[]>([])
  const [permissionTreeLoading, setPermissionTreeLoading] = useState(false)
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null)
  const [roleForm, setRoleForm] = useState<RoleFormState>(emptyRoleForm)
  const [roleDraftMode, setRoleDraftMode] = useState<'create' | 'edit'>('edit')
  const [roleSaving, setRoleSaving] = useState(false)
  const [rolePermissionSaving, setRolePermissionSaving] = useState(false)
  const [templateKeyword, setTemplateKeyword] = useState('')
  const [templatesData, setTemplatesData] = useState<NotificationTemplateListResponse | null>(null)
  const [templatesLoading, setTemplatesLoading] = useState(false)
  const [templatesError, setTemplatesError] = useState('')
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null)
  const [templateForm, setTemplateForm] = useState<TemplateFormState>(emptyTemplateForm)
  const [templateDraftMode, setTemplateDraftMode] = useState(false)
  const [templateDirty, setTemplateDirty] = useState(false)
  const [templateSaving, setTemplateSaving] = useState(false)
  const [templateConfirmOpen, setTemplateConfirmOpen] = useState(false)
  const [templatePreview, setTemplatePreview] = useState<TemplatePreviewResponse | null>(null)
  const [templatePreviewLoading, setTemplatePreviewLoading] = useState(false)
  // ---- 工单列表 ----
  const [ticketStatusTab, setTicketStatusTab] = useState('ALL')
  const [ticketSlaBreachedOnly, setTicketSlaBreachedOnly] = useState(false)
  const [ticketKeyword, setTicketKeyword] = useState('')
  const [ticketPage, setTicketPage] = useState(1)
  const [ticketPageSize] = useState(20)
  const [ticketsData, setTicketsData] = useState<TicketPageResponse | null>(null)
  const [ticketsLoading, setTicketsLoading] = useState(false)
  const [ticketsError, setTicketsError] = useState('')
  const [ticketDetail, setTicketDetail] = useState<TicketDetail | null>(null)
  const [ticketDetailTab, setTicketDetailTab] = useState('mail')
  const [showTicketDetailPage, setShowTicketDetailPage] = useState(false)
  const [msgFilter, setMsgFilter] = useState('ALL')
  const [msgSortAsc, setMsgSortAsc] = useState(true)

  // ---- 附件 ----
  const [ticketAttachments, setTicketAttachments] = useState<TicketAttachment[]>([])

  // ---- 工单操作 ----
  const [replyContent, setReplyContent] = useState('')
  const [replyHtml, setReplyHtml] = useState('')
  const [replySending, setReplySending] = useState(false)
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([])
  const [uploadingFile, setUploadingFile] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [assignModalOpen, setAssignModalOpen] = useState(false)
  const [assignUsers, setAssignUsers] = useState<ManagedUser[]>([])
  const [assignUserId, setAssignUserId] = useState<number | null>(null)
  const [assignReason, setAssignReason] = useState('')
  const [assignNotifyAssignee, setAssignNotifyAssignee] = useState(true)
  const [assignSending, setAssignSending] = useState(false)
  const [claimSending, setClaimSending] = useState(false)
  const [closeModalOpen, setCloseModalOpen] = useState(false)
  const [closeReason, setCloseReason] = useState('')
  const [closeConfirmed, setCloseConfirmed] = useState(false)
  const [closeSending, setCloseSending] = useState(false)
  const [remarkDraft, setRemarkDraft] = useState('')
  const [priorityModalOpen, setPriorityModalOpen] = useState(false)
  const [priorityValue, setPriorityValue] = useState('NORMAL')
  const [priorityReason, setPriorityReason] = useState('')
  const [prioritySending, setPrioritySending] = useState(false)
  const [statusModalOpen, setStatusModalOpen] = useState(false)
  const [statusValue, setStatusValue] = useState('PROCESSING')
  const [statusReason, setStatusReason] = useState('')
  const [statusSending, setStatusSending] = useState(false)
  const [ticketStats, setTicketStats] = useState<any>(null)
  const [dashboardSummary, setDashboardSummary] = useState<DashboardSummary | null>(null)
  const [dashboardTodos, setDashboardTodos] = useState<DashboardTodoListResponse | null>(null)
  const [dashboardLoading, setDashboardLoading] = useState(false)
  const [dashboardError, setDashboardError] = useState('')
  const [dashboardUpdatedAt, setDashboardUpdatedAt] = useState<string | null>(null)
  const [ticketRule, setTicketRule] = useState<TicketNumberRule | null>(null)
  const [ticketRuleForm, setTicketRuleForm] = useState<TicketRuleFormState>(emptyTicketRuleForm)
  const [ticketRuleDirty, setTicketRuleDirty] = useState(false)
  const [ticketRuleLoading, setTicketRuleLoading] = useState(false)
  const [ticketRuleSaving, setTicketRuleSaving] = useState(false)
  const [ticketRulePreviewLoading, setTicketRulePreviewLoading] = useState(false)
  const [ticketRuleError, setTicketRuleError] = useState('')
  const [ticketRuleMessage, setTicketRuleMessage] = useState('')
  const [ticketRuleConfirmOpen, setTicketRuleConfirmOpen] = useState(false)
  const [customerKeyword, setCustomerKeyword] = useState('')
  const [customerPage, setCustomerPage] = useState(1)
  const [customerPageSize, setCustomerPageSize] = useState(20)
  const [customersData, setCustomersData] = useState<CustomerPageResponse | null>(null)
  const [customersLoading, setCustomersLoading] = useState(false)
  const [customersError, setCustomersError] = useState('')
  const [selectedCustomerEmail, setSelectedCustomerEmail] = useState('')
  const [customerDetail, setCustomerDetail] = useState<CustomerReadonly | null>(null)
  const [customerDetailLoading, setCustomerDetailLoading] = useState(false)
  const [customerDetailError, setCustomerDetailError] = useState('')
  const [customerTicketsData, setCustomerTicketsData] = useState<TicketPageResponse | null>(null)
  const [customerTicketsLoading, setCustomerTicketsLoading] = useState(false)
  const [customerTicketsError, setCustomerTicketsError] = useState('')
  const [activeSystemGroup, setActiveSystemGroup] = useState<SystemGroupKey>('ticket')
  const [mailboxKeyword, setMailboxKeyword] = useState('')
  const [mailboxStatusFilter, setMailboxStatusFilter] = useState('ALL')
  const [mailboxPage, setMailboxPage] = useState(1)
  const [mailboxPageSize, setMailboxPageSize] = useState(10)
  const [mailboxesData, setMailboxesData] = useState<MailboxPageResponse | null>(null)
  const [mailboxesLoading, setMailboxesLoading] = useState(false)
  const [mailboxesError, setMailboxesError] = useState('')
  const [mailboxAssignees, setMailboxAssignees] = useState<ManagedUser[]>([])
  const [activeMailboxStep, setActiveMailboxStep] = useState<MailboxStepKey>('basic')
  const [mailboxForm, setMailboxForm] = useState<MailboxFormState>(emptyMailboxForm)
  const [mailboxDirty, setMailboxDirty] = useState(false)
  const [mailboxSaving, setMailboxSaving] = useState(false)
  const [mailboxTesting, setMailboxTesting] = useState(false)
  const [mailboxTestResult, setMailboxTestResult] = useState<MailboxConnectionTestResponse | null>(null)
  const [mailboxConfirmAction, setMailboxConfirmAction] = useState<MailboxConfirmAction>(null)
  const [mailboxActionLoading, setMailboxActionLoading] = useState(false)
  const [fetchLogMailboxFilter, setFetchLogMailboxFilter] = useState('')
  const [fetchLogSuccessFilter, setFetchLogSuccessFilter] = useState('ALL')
  const [fetchLogStartFrom, setFetchLogStartFrom] = useState('')
  const [fetchLogStartTo, setFetchLogStartTo] = useState('')
  const [fetchLogPage, setFetchLogPage] = useState(1)
  const [fetchLogPageSize, setFetchLogPageSize] = useState(10)
  const [fetchLogsData, setFetchLogsData] = useState<MailFetchLogPageResponse | null>(null)
  const [fetchLogsLoading, setFetchLogsLoading] = useState(false)
  const [fetchLogsError, setFetchLogsError] = useState('')
  const [fetchLogDetail, setFetchLogDetail] = useState<MailFetchLog | null>(null)
  const [fetchLogStats, setFetchLogStats] = useState<MailFetchLogStats | null>(null)

  // ---- 发送日志 ----
  const [sendLogPage, setSendLogPage] = useState(1)
  const [sendLogPageSize, setSendLogPageSize] = useState(10)
  const [sendLogMailboxFilter, setSendLogMailboxFilter] = useState('')
  const [sendLogTypeFilter, setSendLogTypeFilter] = useState('ALL')
  const [sendLogStatusFilter, setSendLogStatusFilter] = useState('ALL')
  const [sendLogStartFrom, setSendLogStartFrom] = useState('')
  const [sendLogStartTo, setSendLogStartTo] = useState('')
  const [sendLogsData, setSendLogsData] = useState<MailSendLogPageResponse | null>(null)
  const [sendLogsLoading, setSendLogsLoading] = useState(false)
  const [sendLogsError, setSendLogsError] = useState('')
  const [sendLogDetail, setSendLogDetail] = useState<MailSendLog | null>(null)
  const [sendLogStats, setSendLogStats] = useState<{ totalCount: number; successCount: number; failCount: number } | null>(null)
  const [sendPendingCount, setSendPendingCount] = useState(0)
  const [assignmentRulesData, setAssignmentRulesData] = useState<AssignmentRuleListResponse | null>(null)
  const [assignmentRulesLoading, setAssignmentRulesLoading] = useState(false)
  const [assignmentRulesError, setAssignmentRulesError] = useState('')
  const [assignmentKeyword, setAssignmentKeyword] = useState('')
  const [assignmentEnabledFilter, setAssignmentEnabledFilter] = useState('ALL')
  const [assignmentMatchTypeFilter, setAssignmentMatchTypeFilter] = useState('ALL')
  const [assignmentForm, setAssignmentForm] = useState<AssignmentRuleFormState>(emptyAssignmentRuleForm)
  const [assignmentRuleDirty, setAssignmentRuleDirty] = useState(false)
  const [assignmentSaving, setAssignmentSaving] = useState(false)
  const [assignmentActionLoading, setAssignmentActionLoading] = useState(false)
  const [assignmentConfirmAction, setAssignmentConfirmAction] = useState<AssignmentRuleConfirmAction>(null)
  const [assignmentAssignees, setAssignmentAssignees] = useState<ManagedUser[]>([])
  const [assignmentTestForm, setAssignmentTestForm] = useState<AssignmentRuleTestForm>(emptyAssignmentRuleTestForm)
  const [assignmentTesting, setAssignmentTesting] = useState(false)
  const [assignmentMatchResult, setAssignmentMatchResult] = useState<AssignmentRuleMatchResponse | null>(null)
  const [slaPoliciesData, setSlaPoliciesData] = useState<SlaPolicyListResponse | null>(null)
  const [slaPoliciesLoading, setSlaPoliciesLoading] = useState(false)
  const [slaPoliciesError, setSlaPoliciesError] = useState('')
  const [slaPolicyKeyword, setSlaPolicyKeyword] = useState('')
  const [slaPolicyEnabledFilter, setSlaPolicyEnabledFilter] = useState('ALL')
  const [slaPolicyDefaultFilter, setSlaPolicyDefaultFilter] = useState('ALL')
  const [slaPolicyForm, setSlaPolicyForm] = useState<SlaPolicyFormState>(emptySlaPolicyForm)
  const [slaPolicyDirty, setSlaPolicyDirty] = useState(false)
  const [slaPolicySaving, setSlaPolicySaving] = useState(false)
  const [slaPolicyActionLoading, setSlaPolicyActionLoading] = useState(false)
  const [slaPolicyConfirmAction, setSlaPolicyConfirmAction] = useState<SlaPolicyConfirmAction>(null)
  const [workCalendars, setWorkCalendars] = useState<WorkCalendar[]>([])
  const [workCalendarsLoading, setWorkCalendarsLoading] = useState(false)
  const [workCalendarData, setWorkCalendarData] = useState<WorkCalendarListResponse | null>(null)
  const [workCalendarError, setWorkCalendarError] = useState('')
  const [workCalendarKeyword, setWorkCalendarKeyword] = useState('')
  const [workCalendarDefaultFilter, setWorkCalendarDefaultFilter] = useState('ALL')
  const [workCalendarForm, setWorkCalendarForm] = useState<WorkCalendarFormState>(emptyWorkCalendarForm)
  const [workCalendarDirty, setWorkCalendarDirty] = useState(false)
  const [workCalendarSaving, setWorkCalendarSaving] = useState(false)
  const [workCalendarActionLoading, setWorkCalendarActionLoading] = useState(false)
  const [workCalendarConfirmAction, setWorkCalendarConfirmAction] = useState<WorkCalendarConfirmAction>(null)
  const [calendarSlaPolicies, setCalendarSlaPolicies] = useState<SlaPolicy[]>([])
  const [holidaysData, setHolidaysData] = useState<HolidayListResponse | null>(null)
  const [holidaysLoading, setHolidaysLoading] = useState(false)
  const [holidaysError, setHolidaysError] = useState('')
  const [holidayMonth, setHolidayMonth] = useState('2026-10')
  const [holidayKeyword, setHolidayKeyword] = useState('')
  const [holidayForm, setHolidayForm] = useState<HolidayFormState>(emptyHolidayForm)
  const [holidayDirty, setHolidayDirty] = useState(false)
  const [holidaySaving, setHolidaySaving] = useState(false)
  const [holidayImporting, setHolidayImporting] = useState(false)
  const [calendarPreviewCreatedAt, setCalendarPreviewCreatedAt] = useState(calendarPreviewBaseTime.format('YYYY-MM-DDTHH:mm:ss'))
  const [calendarPreviewResponseHours, setCalendarPreviewResponseHours] = useState('2')
  const [calendarPreviewResolveHours, setCalendarPreviewResolveHours] = useState('16')

  const [mailboxes, setMailboxes] = useState<Mailbox[]>([])
  const searchInputRef = useRef<HTMLInputElement>(null)
  const templateContentRef = useRef<HTMLTextAreaElement>(null)
  const selectedRoleIdRef = useRef<number | null>(null)
  const roleDraftModeRef = useRef<'create' | 'edit'>('edit')
  const hasPermission = useCallback((permissionCode: string) => userHasPermission(user, permissionCode), [user])
  const isAdmin = userHasRole(user, 'ADMIN')
  const isAgent = userHasRole(user, 'AGENT')
  const canReadCustomers = hasPermission('customer:read')
  const canReadUsers = hasPermission('user:read')
  const canCreateUsers = hasPermission('user:create')
  const canUpdateUsers = hasPermission('user:update')
  const canEnableUsers = hasPermission('user:enable')
  const canResetUserPassword = hasPermission('user:reset_password')
  const canReadRoles = hasPermission('role:read')
  const canCreateRoles = hasPermission('role:create')
  const canUpdateRoles = hasPermission('role:update')
  const canEnableRoles = hasPermission('role:enable')
  const canUpdateRolePermissions = hasPermission('role:permission_update')
  const canAccessMenuNode = useCallback(
    (adminOnly?: boolean, permission?: string) => {
      if (permission) return hasPermission(permission)
      return !adminOnly || isAdmin
    },
    [hasPermission, isAdmin],
  )
  const isCurrentTicketUnassigned = ticketDetail?.assigneeId == null
  const isCurrentTicketTerminal = ticketDetail ? isTerminalTicket(ticketDetail.status) : false
  const canOperateCurrentTicket = !!ticketDetail && !isCurrentTicketTerminal
    && (isAdmin || ticketDetail.assigneeId === user?.id)
  const canClaimCurrentTicket = !!ticketDetail && !isCurrentTicketTerminal
    && isCurrentTicketUnassigned && (isAdmin || isAgent)
  const activeSystemGroupConfig = systemGroups.find((group) => group.key === activeSystemGroup) || systemGroups[0]
  const visibleMenuGroups = useMemo(
    () =>
      menuGroups
        .filter((group) => canAccessMenuNode(group.adminOnly, group.permission))
        .map((group) => ({
          ...group,
          items: group.items.filter((item) => canAccessMenuNode(item.adminOnly, item.permission)),
        }))
        .filter((group) => group.items.length > 0),
    [canAccessMenuNode],
  )
  const roleSelectOptions = useMemo(
    () => {
      const options = rolesData?.records.map((role) => ({ label: role.roleName, value: role.roleCode })) ?? []
      return options.length > 0 ? options : builtInRoleOptions
    },
    [rolesData],
  )
  const selectedRole = useMemo(
    () => rolesData?.records.find((role) => role.id === selectedRoleId) ?? null,
    [rolesData, selectedRoleId],
  )
  const selectedRoleReadonly = roleDraftMode === 'edit' && Boolean(selectedRole?.systemRole)
  const flatPermissionNodes = useMemo(() => collectPermissionNodes(permissionTree), [permissionTree])
  const checkedPermissionSet = useMemo(() => new Set(roleForm.permissionCodes), [roleForm.permissionCodes])

  useEffect(() => {
    if (!user || visibleMenuGroups.length === 0) return
    const visibleItems = visibleMenuGroups.flatMap((group) => group.items)
    if (visibleItems.some((item) => item.title === activeMenu)) return
    setActiveMenu(visibleItems[0]?.title ?? '工作台')
  }, [activeMenu, user, visibleMenuGroups])

  const handleAuthExpired = useCallback((error: unknown) => {
    if (!(error instanceof ApiError) || (error.status !== 401 && error.code !== 40102)) {
      return false
    }

    clearSession()
    setToken('')
    setUser(null)
    setAccount('')
    setPassword('')
    setFormError(error.message || '登录状态已失效，请重新登录')
    return true
  }, [])

  useEffect(() => {
    if (!token) {
      setCheckingSession(false)
      return
    }

    let active = true
    requestApi<CurrentUser>('/api/v1/auth/me', {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((currentUser) => {
        if (!active) return
        setUser(currentUser)
      })
      .catch(() => {
        if (!active) return
        clearSession()
        setToken('')
        setUser(null)
      })
      .finally(() => {
        if (active) setCheckingSession(false)
      })

    return () => {
      active = false
    }
  }, [token])

  useEffect(() => {
    function focusSearch(event: KeyboardEvent) {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        searchInputRef.current?.focus()
      }
    }

    document.addEventListener('keydown', focusSearch)
    return () => document.removeEventListener('keydown', focusSearch)
  }, [])

  useEffect(() => {
    if (!user) return
    const activeExists = visibleMenuGroups.some((group) => group.items.some((item) => item.title === activeMenu))
    if (!activeExists) {
      setActiveMenu('工作台')
    }
  }, [activeMenu, user, visibleMenuGroups])

  const hydrateRoleForm = useCallback((role: ManagedRole) => {
    setRoleForm({
      roleName: role.roleName,
      roleDesc: role.roleDesc || '',
      enabled: role.enabled,
      permissionCodes: role.permissionCodes || [],
      dataScopes: role.dataScopes?.length ? role.dataScopes : emptyRoleForm.dataScopes,
    })
    roleDraftModeRef.current = 'edit'
    selectedRoleIdRef.current = role.id
    setRoleDraftMode('edit')
    setSelectedRoleId(role.id)
  }, [])

  const fetchRoles = useCallback(async (preferredRoleId?: number) => {
    if (!token || (activeMenu !== '角色管理' && activeMenu !== '用户管理')) return
    if (!canReadRoles) {
      if (activeMenu === '角色管理') {
        setRolesData(null)
        setRolesError('当前账号没有角色管理权限')
      }
      return
    }
    const params = new URLSearchParams()
    if (roleKeyword.trim()) params.set('keyword', roleKeyword.trim())
    if (roleEnabledFilter !== 'ALL') params.set('enabled', roleEnabledFilter)
    setRolesLoading(true)
    setRolesError('')
    try {
      const data = await requestApi<RoleListResponse>(`/api/v1/roles?${params.toString()}`, {
        headers: authHeaders(token),
      })
      setRolesData(data)
      const targetRoleId = preferredRoleId ?? selectedRoleIdRef.current
      const current = data.records.find((role) => role.id === targetRoleId)
      if (activeMenu === '角色管理') {
        if (current) {
          hydrateRoleForm(current)
        } else if (!preferredRoleId && roleDraftModeRef.current !== 'create' && data.records[0]) {
          hydrateRoleForm(data.records[0])
        } else if (!preferredRoleId && roleDraftModeRef.current !== 'create') {
          selectedRoleIdRef.current = null
          setSelectedRoleId(null)
          setRoleForm(emptyRoleForm)
        }
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '角色列表加载失败')
    } finally {
      setRolesLoading(false)
    }
  }, [
    activeMenu,
    canReadRoles,
    handleAuthExpired,
    hydrateRoleForm,
    roleEnabledFilter,
    roleKeyword,
    token,
  ])

  const fetchPermissionTree = useCallback(async () => {
    if (!token || activeMenu !== '角色管理' || !canReadRoles) return
    setPermissionTreeLoading(true)
    try {
      const data = await requestApi<PermissionTreeNode[]>('/api/v1/roles/permissions', {
        headers: authHeaders(token),
      })
      setPermissionTree(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '权限树加载失败')
    } finally {
      setPermissionTreeLoading(false)
    }
  }, [activeMenu, canReadRoles, handleAuthExpired, token])

  useEffect(() => {
    if (activeMenu === '角色管理' || activeMenu === '用户管理') {
      void fetchRoles()
    }
  }, [activeMenu, fetchRoles])

  useEffect(() => {
    if (activeMenu === '角色管理') {
      void fetchPermissionTree()
    }
  }, [activeMenu, fetchPermissionTree])

  const fetchUsers = useCallback(async () => {
    if (!token || activeMenu !== '用户管理') return
    if (!canReadUsers) {
      setUsersData(null)
      setUsersError('当前账号没有用户管理权限')
      return
    }

    const params = new URLSearchParams({
      page: String(userPage),
      size: String(userPageSize),
    })
    if (userKeyword.trim()) params.set('keyword', userKeyword.trim())
    if (userRoleFilter !== 'ALL') params.set('roleCode', userRoleFilter)
    if (userEnabledFilter !== 'ALL') params.set('enabled', userEnabledFilter)

    setUsersLoading(true)
    setUsersError('')
    try {
      const data = await requestApi<UserPageResponse>(`/api/v1/users?${params.toString()}`, {
        headers: authHeaders(token),
      })
      setUsersData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setUsersError(error instanceof Error ? error.message : '用户列表加载失败')
    } finally {
      setUsersLoading(false)
    }
  }, [activeMenu, canReadUsers, handleAuthExpired, token, userEnabledFilter, userKeyword, userPage, userPageSize, userRoleFilter])

  useEffect(() => {
    void fetchUsers()
  }, [fetchUsers])

  const fetchTemplates = useCallback(async () => {
    if (!token || activeMenu !== '通知模板') return
    if (!isAdmin) {
      setTemplatesData(null)
      setTemplatesError('当前账号没有通知模板管理权限')
      return
    }

    const params = new URLSearchParams()
    if (templateKeyword.trim()) params.set('keyword', templateKeyword.trim())

    setTemplatesLoading(true)
    setTemplatesError('')
    try {
      const data = await requestApi<NotificationTemplateListResponse>(
        `/api/v1/notification-templates${params.toString() ? `?${params.toString()}` : ''}`,
        { headers: authHeaders(token) },
      )
      setTemplatesData(data)
      if (templateDraftMode) {
        return
      }
      const selected = data.records.find((template) => template.id === selectedTemplateId) || data.records[0] || null
      if (selected) {
        setSelectedTemplateId(selected.id)
        setTemplateForm(toTemplateForm(selected))
        setTemplateDirty(false)
        setTemplatePreview(null)
      } else {
        setSelectedTemplateId(null)
        setTemplateForm(emptyTemplateForm)
        setTemplateDirty(false)
        setTemplatePreview(null)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTemplatesError(error instanceof Error ? error.message : '模板列表加载失败')
    } finally {
      setTemplatesLoading(false)
    }
  }, [activeMenu, handleAuthExpired, isAdmin, selectedTemplateId, templateDraftMode, templateKeyword, token])

  useEffect(() => {
    void fetchTemplates()
  }, [fetchTemplates])

  const fetchTicketRule = useCallback(async () => {
    if (!token || activeMenu !== '编号规则') return
    if (!isAdmin) {
      setTicketRule(null)
      setTicketRuleError('当前账号没有编号规则管理权限')
      return
    }

    setTicketRuleLoading(true)
    setTicketRuleError('')
    setTicketRuleMessage('')
    try {
      const data = await requestApi<TicketNumberRule>('/api/v1/sys-params/ticket-number-rule', {
        headers: authHeaders(token),
      })
      setTicketRule(data)
      setTicketRuleForm(toTicketRuleForm(data))
      setTicketRuleDirty(false)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketRuleError(error instanceof Error ? error.message : '编号规则加载失败')
    } finally {
      setTicketRuleLoading(false)
    }
  }, [activeMenu, handleAuthExpired, isAdmin, token])

  useEffect(() => {
    void fetchTicketRule()
  }, [fetchTicketRule])

  const fetchMailboxes = useCallback(async () => {
    if (!token || activeMenu !== '邮箱配置') return
    if (!isAdmin) {
      setMailboxesData(null)
      setMailboxesError('当前账号没有邮箱配置管理权限')
      return
    }

    const params = new URLSearchParams({
      page: String(mailboxPage),
      size: String(mailboxPageSize),
    })
    if (mailboxKeyword.trim()) params.set('keyword', mailboxKeyword.trim())
    if (mailboxStatusFilter !== 'ALL') params.set('status', mailboxStatusFilter)

    setMailboxesLoading(true)
    setMailboxesError('')
    try {
      const data = await requestApi<MailboxPageResponse>(`/api/v1/mailboxes?${params.toString()}`, {
        headers: authHeaders(token),
      })
      setMailboxesData(data)
      const selected =
        data.records.find((mailbox) => mailbox.id === mailboxForm.id) ||
        data.records[0] ||
        null
      if (selected && !mailboxDirty) {
        setMailboxForm(toMailboxForm(selected))
        setMailboxTestResult(null)
      }
      if (!selected && !mailboxDirty) {
        setMailboxForm(emptyMailboxForm)
        setMailboxTestResult(null)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxesError(error instanceof Error ? error.message : '邮箱列表加载失败')
    } finally {
      setMailboxesLoading(false)
    }
  }, [
    activeMenu,
    handleAuthExpired,
    isAdmin,
    mailboxDirty,
    mailboxForm.id,
    mailboxKeyword,
    mailboxPage,
    mailboxPageSize,
    mailboxStatusFilter,
    token,
  ])

  useEffect(() => {
    void fetchMailboxes()
  }, [fetchMailboxes])

  const fetchMailboxAssignees = useCallback(async () => {
    if (!token || activeMenu !== '邮箱配置' || !isAdmin) return

    try {
      const data = await requestApi<UserPageResponse>('/api/v1/users?page=1&size=100&roleCode=AGENT&enabled=true', {
        headers: authHeaders(token),
      })
      setMailboxAssignees(data.records)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxAssignees([])
    }
  }, [activeMenu, handleAuthExpired, isAdmin, token])

  useEffect(() => {
    void fetchMailboxAssignees()
  }, [fetchMailboxAssignees])

  // ---- 拉取日志 ----
  const fetchFetchLogs = useCallback(async () => {
    if (!token || activeMenu !== '收件记录') return

    const params = new URLSearchParams({
      page: String(fetchLogPage),
      size: String(fetchLogPageSize),
    })
    if (fetchLogMailboxFilter) params.set('mailboxId', fetchLogMailboxFilter)
    if (fetchLogSuccessFilter !== 'ALL') params.set('success', fetchLogSuccessFilter)
    if (fetchLogStartFrom) params.set('startFrom', fetchLogStartFrom)
    if (fetchLogStartTo) params.set('startTo', fetchLogStartTo)

    setFetchLogsLoading(true)
    setFetchLogsError('')
    try {
      const data = await requestApi<MailFetchLogPageResponse>(`/api/v1/mail-fetch-logs?${params.toString()}`, {
        headers: authHeaders(token),
      })
      setFetchLogsData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      const msg = error instanceof ApiError ? error.message : '加载拉取日志失败'
      setFetchLogsError(msg)
    } finally {
      setFetchLogsLoading(false)
    }
  }, [token, activeMenu, fetchLogPage, fetchLogPageSize, fetchLogMailboxFilter,
      fetchLogSuccessFilter, fetchLogStartFrom, fetchLogStartTo, handleAuthExpired])

  useEffect(() => {
    if (activeMenu === '收件记录') {
      void fetchFetchLogs()
      void fetchFetchLogStats()
    }
  }, [fetchFetchLogs, activeMenu])

  const fetchFetchLogStats = useCallback(async () => {
    if (!token) return
    try {
      const data = await requestApi<MailFetchLogStats>('/api/v1/mail-fetch-logs/stats', {
        headers: authHeaders(token),
      })
      setFetchLogStats(data)
    } catch { /* stats 加载失败不影响主列表 */ }
  }, [token])

  // ---- 发送日志 ----
  const fetchSendLogs = useCallback(async () => {
    if (!token || activeMenu !== '发件记录') return
    const params = new URLSearchParams({
      page: String(sendLogPage),
      size: String(sendLogPageSize),
    })
    if (sendLogMailboxFilter) params.set('mailboxId', sendLogMailboxFilter)
    if (sendLogTypeFilter !== 'ALL') params.set('sendType', sendLogTypeFilter)
    if (sendLogStatusFilter !== 'ALL') params.set('sendStatus', sendLogStatusFilter)
    if (sendLogStartFrom) params.set('startFrom', sendLogStartFrom)
    if (sendLogStartTo) params.set('startTo', sendLogStartTo)

    setSendLogsLoading(true)
    setSendLogsError('')
    try {
      const data = await requestApi<MailSendLogPageResponse>(`/api/v1/mail-send/logs?${params.toString()}`, {
        headers: authHeaders(token),
      })
      setSendLogsData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSendLogsError(error instanceof ApiError ? error.message : '加载发送日志失败')
    } finally {
      setSendLogsLoading(false)
    }
  }, [token, activeMenu, sendLogPage, sendLogPageSize, sendLogMailboxFilter, sendLogTypeFilter, sendLogStatusFilter, sendLogStartFrom, sendLogStartTo, handleAuthExpired])

  const fetchSendLogStats = useCallback(async () => {
    if (!token) return
    try {
      const data = await requestApi<{ totalCount: number; successCount: number; failCount: number }>('/api/v1/mail-send/logs/stats', { headers: authHeaders(token) })
      setSendLogStats(data)
    } catch { /* ignore */ }
  }, [token])

  const fetchSendPendingCount = useCallback(async () => {
    if (!token) return
    try {
      const count = await requestApi<number>('/api/v1/mail-send/logs/pending-count', { headers: authHeaders(token) })
      setSendPendingCount(count)
    } catch { /* ignore */ }
  }, [token])

  useEffect(() => {
    if (activeMenu === '发件记录') {
      void fetchSendLogs()
      void fetchSendLogStats()
    }
  }, [fetchSendLogs, activeMenu])

  // 全局拉取待处理数量（菜单角标）
  useEffect(() => {
    if (token) void fetchSendPendingCount()
  }, [token, fetchSendPendingCount])

  const fetchAssignmentRules = useCallback(async () => {
    if (!token || activeMenu !== '分配规则') return
    if (!isAdmin) {
      setAssignmentRulesData(null)
      setAssignmentRulesError('当前账号没有分配规则管理权限')
      return
    }

    const params = new URLSearchParams()
    if (assignmentKeyword.trim()) params.set('keyword', assignmentKeyword.trim())
    if (assignmentEnabledFilter !== 'ALL') params.set('enabled', assignmentEnabledFilter)
    if (assignmentMatchTypeFilter !== 'ALL') params.set('matchType', assignmentMatchTypeFilter)

    setAssignmentRulesLoading(true)
    setAssignmentRulesError('')
    try {
      const query = params.toString()
      const data = await requestApi<AssignmentRuleListResponse>(
        `/api/v1/assignment-rules${query ? `?${query}` : ''}`,
        { headers: authHeaders(token) },
      )
      setAssignmentRulesData(data)
      const selected = data.records.find((rule) => rule.id === assignmentForm.id) || data.records[0] || null
      if (selected && !assignmentRuleDirty) {
        setAssignmentForm(toAssignmentRuleForm(selected))
        setAssignmentMatchResult(null)
      }
      if (!selected && !assignmentRuleDirty) {
        setAssignmentForm(emptyAssignmentRuleForm)
        setAssignmentMatchResult(null)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '分配规则加载失败')
    } finally {
      setAssignmentRulesLoading(false)
    }
  }, [
    activeMenu,
    assignmentEnabledFilter,
    assignmentForm.id,
    assignmentKeyword,
    assignmentMatchTypeFilter,
    assignmentRuleDirty,
    handleAuthExpired,
    isAdmin,
    token,
  ])

  useEffect(() => {
    void fetchAssignmentRules()
  }, [fetchAssignmentRules])

  const fetchSlaPolicies = useCallback(async () => {
    if (!token || activeMenu !== 'SLA策略') return
    if (!isAdmin) {
      setSlaPoliciesData(null)
      setSlaPoliciesError('当前账号没有 SLA 策略管理权限')
      return
    }

    const params = new URLSearchParams()
    if (slaPolicyKeyword.trim()) params.set('keyword', slaPolicyKeyword.trim())
    if (slaPolicyEnabledFilter !== 'ALL') params.set('enabled', slaPolicyEnabledFilter)
    if (slaPolicyDefaultFilter !== 'ALL') params.set('defaultPolicy', slaPolicyDefaultFilter)

    setSlaPoliciesLoading(true)
    setSlaPoliciesError('')
    try {
      const query = params.toString()
      const data = await requestApi<SlaPolicyListResponse>(
        `/api/v1/sla-policies${query ? `?${query}` : ''}`,
        { headers: authHeaders(token) },
      )
      setSlaPoliciesData(data)
      const selected = data.records.find((policy) => policy.id === slaPolicyForm.id) || data.records[0] || null
      if (selected && !slaPolicyDirty) {
        setSlaPolicyForm(toSlaPolicyForm(selected))
      }
      if (!selected && !slaPolicyDirty) {
        setSlaPolicyForm({
          ...emptySlaPolicyForm,
          calendarId: workCalendars[0] ? String(workCalendars[0].id) : '',
        })
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : 'SLA 策略加载失败')
    } finally {
      setSlaPoliciesLoading(false)
    }
  }, [
    activeMenu,
    handleAuthExpired,
    isAdmin,
    slaPolicyDefaultFilter,
    slaPolicyDirty,
    slaPolicyEnabledFilter,
    slaPolicyForm.id,
    slaPolicyKeyword,
    token,
    workCalendars,
  ])

  useEffect(() => {
    void fetchSlaPolicies()
  }, [fetchSlaPolicies])

  const fetchWorkCalendarsForSla = useCallback(async () => {
    if (!token || activeMenu !== 'SLA策略' || !isAdmin) return
    setWorkCalendarsLoading(true)
    try {
      const data = await requestApi<WorkCalendarListResponse>('/api/v1/work-calendars', {
        headers: authHeaders(token),
      })
      setWorkCalendars(data.records)
      setSlaPolicyForm((form) => {
        if (form.calendarId || !data.records[0]) return form
        return { ...form, calendarId: String(data.records[0].id) }
      })
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : '工作日历加载失败')
    } finally {
      setWorkCalendarsLoading(false)
    }
  }, [activeMenu, handleAuthExpired, isAdmin, token])

  useEffect(() => {
    void fetchWorkCalendarsForSla()
  }, [fetchWorkCalendarsForSla])

  const fetchWorkCalendarsPage = useCallback(async () => {
    if (!token || activeMenu !== '工作日历') return
    if (!isAdmin) {
      setWorkCalendarData(null)
      setWorkCalendarError('当前账号没有工作日历管理权限')
      return
    }

    const params = new URLSearchParams()
    if (workCalendarKeyword.trim()) params.set('keyword', workCalendarKeyword.trim())
    if (workCalendarDefaultFilter !== 'ALL') params.set('defaultCalendar', workCalendarDefaultFilter)

    setWorkCalendarsLoading(true)
    setWorkCalendarError('')
    try {
      const query = params.toString()
      const data = await requestApi<WorkCalendarListResponse>(
        `/api/v1/work-calendars${query ? `?${query}` : ''}`,
        { headers: authHeaders(token) },
      )
      setWorkCalendarData(data)
      setWorkCalendars(data.records)
      const selected = data.records.find((calendar) => calendar.id === workCalendarForm.id) || data.records[0] || null
      if (selected && !workCalendarDirty) {
        setWorkCalendarForm(toWorkCalendarForm(selected))
        setHolidayForm((form) => ({
          ...form,
          calendarId: String(selected.id),
        }))
      }
      if (!selected && !workCalendarDirty) {
        setWorkCalendarForm(emptyWorkCalendarForm)
        setHolidayForm(emptyHolidayForm)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setWorkCalendarError(error instanceof Error ? error.message : '工作日历加载失败')
    } finally {
      setWorkCalendarsLoading(false)
    }
  }, [
    activeMenu,
    handleAuthExpired,
    isAdmin,
    token,
    workCalendarDefaultFilter,
    workCalendarDirty,
    workCalendarForm.id,
    workCalendarKeyword,
  ])

  useEffect(() => {
    void fetchWorkCalendarsPage()
  }, [fetchWorkCalendarsPage])

  const fetchCalendarSlaPolicies = useCallback(async () => {
    if (!token || activeMenu !== '工作日历' || !isAdmin) return
    try {
      const data = await requestApi<SlaPolicyListResponse>('/api/v1/sla-policies', {
        headers: authHeaders(token),
      })
      setCalendarSlaPolicies(data.records)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setWorkCalendarError(error instanceof Error ? error.message : 'SLA 策略引用加载失败')
    }
  }, [activeMenu, handleAuthExpired, isAdmin, token])

  useEffect(() => {
    void fetchCalendarSlaPolicies()
  }, [fetchCalendarSlaPolicies])

  const fetchHolidays = useCallback(async () => {
    if (!token || activeMenu !== '工作日历') return
    if (!isAdmin) {
      setHolidaysData(null)
      setHolidaysError('当前账号没有节假日管理权限')
      return
    }
    if (!workCalendarForm.id) {
      setHolidaysData({ records: [], summary: { totalCount: 0 } })
      return
    }

    const month = dayjs(`${holidayMonth}-01`)
    const params = new URLSearchParams({
      calendarId: String(workCalendarForm.id),
      dateFrom: month.startOf('month').format('YYYY-MM-DD'),
      dateTo: month.endOf('month').format('YYYY-MM-DD'),
    })
    if (holidayKeyword.trim()) params.set('keyword', holidayKeyword.trim())

    setHolidaysLoading(true)
    setHolidaysError('')
    try {
      const data = await requestApi<HolidayListResponse>(`/api/v1/holidays?${params.toString()}`, {
        headers: authHeaders(token),
      })
      setHolidaysData(data)
      setHolidayForm((form) => ({
        ...form,
        calendarId: String(workCalendarForm.id),
      }))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setHolidaysError(error instanceof Error ? error.message : '节假日加载失败')
    } finally {
      setHolidaysLoading(false)
    }
  }, [
    activeMenu,
    handleAuthExpired,
    holidayKeyword,
    holidayMonth,
    isAdmin,
    token,
    workCalendarForm.id,
  ])

  useEffect(() => {
    void fetchHolidays()
  }, [fetchHolidays])

  const fetchAssignmentAssignees = useCallback(async () => {
    if (!token || activeMenu !== '分配规则' || !isAdmin) return
    try {
      const data = await requestApi<UserPageResponse>('/api/v1/users?page=1&size=100&roleCode=AGENT&enabled=true', {
        headers: authHeaders(token),
      })
      setAssignmentAssignees(data.records)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentAssignees([])
    }
  }, [activeMenu, handleAuthExpired, isAdmin, token])

  useEffect(() => {
    void fetchAssignmentAssignees()
  }, [fetchAssignmentAssignees])

  const fetchCustomers = useCallback(async () => {
    if (!token || activeMenu !== '客户管理') return
    if (!canReadCustomers) {
      setCustomersData(null)
      setCustomersError('当前账号没有客户查看权限')
      setSelectedCustomerEmail('')
      return
    }

    const params = new URLSearchParams({
      page: String(customerPage),
      size: String(customerPageSize),
    })
    if (customerKeyword.trim()) params.set('keyword', customerKeyword.trim())

    setCustomersLoading(true)
    setCustomersError('')
    try {
      const data = await requestApi<CustomerPageResponse>(`/api/v1/customers?${params.toString()}`, {
        headers: authHeaders(token),
      })
      setCustomersData(data)
      setSelectedCustomerEmail((current) => {
        if (current && data.records.some((customer) => customer.email === current)) return current
        return data.records[0]?.email || ''
      })
    } catch (error) {
      if (handleAuthExpired(error)) return
      setCustomersError(error instanceof Error ? error.message : '客户列表加载失败')
    } finally {
      setCustomersLoading(false)
    }
  }, [
    activeMenu,
    canReadCustomers,
    customerKeyword,
    customerPage,
    customerPageSize,
    handleAuthExpired,
    token,
  ])

  useEffect(() => {
    void fetchCustomers()
  }, [fetchCustomers])

  const fetchCustomerDetail = useCallback(async () => {
    if (!token || activeMenu !== '客户管理' || !canReadCustomers) return
    if (!selectedCustomerEmail) {
      setCustomerDetail(null)
      setCustomerDetailError('')
      return
    }

    setCustomerDetailLoading(true)
    setCustomerDetailError('')
    try {
      const data = await requestApi<CustomerReadonly>(
        `/api/v1/customers/${encodeURIComponent(selectedCustomerEmail)}`,
        { headers: authHeaders(token) },
      )
      setCustomerDetail(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setCustomerDetail(null)
      setCustomerDetailError(error instanceof Error ? error.message : '客户详情加载失败')
    } finally {
      setCustomerDetailLoading(false)
    }
  }, [activeMenu, canReadCustomers, handleAuthExpired, selectedCustomerEmail, token])

  useEffect(() => {
    void fetchCustomerDetail()
  }, [fetchCustomerDetail])

  const fetchCustomerTickets = useCallback(async () => {
    if (!token || activeMenu !== '客户管理' || !canReadCustomers || !selectedCustomerEmail) {
      setCustomerTicketsData(null)
      return
    }

    const params = new URLSearchParams({
      page: '1',
      size: '5',
      keyword: selectedCustomerEmail,
    })

    setCustomerTicketsLoading(true)
    setCustomerTicketsError('')
    try {
      const data = await requestApi<TicketPageResponse>(`/api/v1/tickets?${params.toString()}`, {
        headers: authHeaders(token),
      })
      setCustomerTicketsData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setCustomerTicketsError(error instanceof Error ? error.message : '关联工单加载失败')
    } finally {
      setCustomerTicketsLoading(false)
    }
  }, [activeMenu, canReadCustomers, handleAuthExpired, selectedCustomerEmail, token])

  useEffect(() => {
    void fetchCustomerTickets()
  }, [fetchCustomerTickets])

  // ---- 工作台 ----
  const fetchDashboard = useCallback(async () => {
    if (!token || activeMenu !== '工作台') return
    setDashboardLoading(true)
    setDashboardError('')
    try {
      const [summary, todos] = await Promise.all([
        requestApi<DashboardSummary>('/api/v1/dashboard/summary', { headers: authHeaders(token) }),
        requestApi<DashboardTodoListResponse>('/api/v1/dashboard/my-todos?limit=5', { headers: authHeaders(token) }),
      ])
      setDashboardSummary(summary)
      setDashboardTodos(todos)
      setDashboardUpdatedAt(dayjs().format('YYYY-MM-DD HH:mm'))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setDashboardError(error instanceof ApiError ? error.message : '加载工作台数据失败')
    } finally {
      setDashboardLoading(false)
    }
  }, [activeMenu, handleAuthExpired, token])

  useEffect(() => {
    if (activeMenu === '工作台') {
      void fetchDashboard()
    }
  }, [activeMenu, fetchDashboard])

  const navigateToTickets = useCallback((status: string = 'ALL', slaBreachedOnly = false) => {
    setTicketStatusTab(status)
    setTicketSlaBreachedOnly(slaBreachedOnly)
    setTicketKeyword('')
    setTicketPage(1)
    setShowTicketDetailPage(false)
    setActiveMenu('全部工单')
  }, [])

  // ---- 工单列表 ----
  const fetchTickets = useCallback(async () => {
    if (!token || activeMenu !== '全部工单') return
    const params = new URLSearchParams({ page: String(ticketPage), size: String(ticketPageSize) })
    if (ticketStatusTab !== 'ALL') params.set('status', ticketStatusTab)
    if (ticketSlaBreachedOnly) params.set('slaBreached', 'true')
    if (ticketKeyword.trim()) params.set('keyword', ticketKeyword.trim())

    setTicketsLoading(true)
    setTicketsError('')
    try {
      const data = await requestApi<TicketPageResponse>(`/api/v1/tickets?${params.toString()}`, { headers: authHeaders(token) })
      setTicketsData(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketsError(error instanceof ApiError ? error.message : '加载工单失败')
    } finally {
      setTicketsLoading(false)
    }
  }, [token, activeMenu, ticketPage, ticketPageSize, ticketStatusTab, ticketSlaBreachedOnly, ticketKeyword, handleAuthExpired])

  const handleBackToList = useCallback(() => {
    setShowTicketDetailPage(false)
    setTicketDetail(null)
    setTicketDetailTab('mail')
    setMsgFilter('ALL')
    setMsgSortAsc(false)
    setTicketAttachments([])
  }, [])

  const handleOpenDetail = useCallback(async (id: number) => {
    setShowTicketDetailPage(true)
    setTicketDetailTab('mail')
    if (!token) return
    try {
      const data = await requestApi<TicketDetail>(`/api/v1/tickets/${id}`, { headers: authHeaders(token) })
      setTicketDetail(data)
      setRemarkDraft(data.remark || '')
      // 加载附件列表
      try {
        const atts = await requestApi<TicketAttachment[]>(`/api/v1/tickets/${id}/attachments`, { headers: authHeaders(token) })
        setTicketAttachments(atts || [])
      } catch { setTicketAttachments([]) }
    } catch (error) {
      if (handleAuthExpired(error)) return
    }
  }, [token, handleAuthExpired])

  /** 重新加载工单详情（操作后刷新） */
  const reloadTicketDetail = useCallback(async () => {
    if (!token || !ticketDetail) return
    try {
      const data = await requestApi<TicketDetail>(`/api/v1/tickets/${ticketDetail.id}`, { headers: authHeaders(token) })
      setTicketDetail(data)
      setRemarkDraft(data.remark || '')
      // 刷新附件列表
      try {
        const atts = await requestApi<TicketAttachment[]>(`/api/v1/tickets/${ticketDetail.id}/attachments`, { headers: authHeaders(token) })
        setTicketAttachments(atts || [])
      } catch { /* ignore */ }
    } catch (error) {
      if (handleAuthExpired(error)) return
    }
  }, [token, ticketDetail, handleAuthExpired])

  /** 获取可用处理人列表 */
  const fetchAgentUsers = useCallback(async () => {
    if (!token) return
    try {
      const data = await requestApi<UserPageResponse>('/api/v1/users?page=1&size=200&enabled=true', { headers: authHeaders(token) })
      setAssignUsers(data.records)
    } catch { setAssignUsers([]) }
  }, [token])

  /** 回复客户 */
  const handleReply = useCallback(async () => {
    const content = replyContent.trim()
    const html = replyHtml.trim()
    if (!token || !ticketDetail || !canOperateCurrentTicket || (!content && !html)) return
    setReplySending(true)
    try {
      const attInfos = uploadedFiles.map(f => ({ objectKey: f.objectKey, fileName: f.fileName, fileSize: f.fileSize, contentType: f.contentType }))
      await requestApi(`/api/v1/tickets/${ticketDetail.id}/reply`, {
        method: 'POST',
        headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: content || html, htmlContent: html || content, internal: false, attachments: attInfos }),
      })
      setReplyContent('')
      setReplyHtml('')
      setUploadedFiles([])
      await reloadTicketDetail()
      void fetchTickets()
      message.success('回复已发送')
    } catch (error: any) {
      message.error(error?.message || '回复发送失败')
      if (handleAuthExpired(error)) return
    } finally {
      setReplySending(false)
    }
  }, [token, ticketDetail, canOperateCurrentTicket, replyContent, replyHtml, uploadedFiles, reloadTicketDetail, fetchTickets, handleAuthExpired])

  /** 转派 */
  const handleAssign = useCallback(async () => {
    if (!token || !ticketDetail || !canOperateCurrentTicket || !assignUserId) return
    setAssignSending(true)
    try {
      await requestApi(`/api/v1/tickets/${ticketDetail.id}/assign`, {
        method: 'POST',
        headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
        body: JSON.stringify({
          assigneeId: assignUserId,
          reason: assignReason.trim() || null,
          notifyAssignee: assignNotifyAssignee,
        }),
      })
      setAssignModalOpen(false)
      setAssignUserId(null)
      setAssignReason('')
      setAssignNotifyAssignee(true)
      await reloadTicketDetail()
      void fetchTickets()
      message.success('工单已转派')
    } catch (error: any) {
      message.error(error?.message || '转派失败')
      if (handleAuthExpired(error)) return
    } finally {
      setAssignSending(false)
    }
  }, [token, ticketDetail, canOperateCurrentTicket, assignUserId, assignReason, assignNotifyAssignee, reloadTicketDetail, fetchTickets, handleAuthExpired])

  /** 关闭工单 */
  const handleClose = useCallback(async () => {
    if (!token || !ticketDetail || !canOperateCurrentTicket) return
    setCloseSending(true)
    try {
      await requestApi(`/api/v1/tickets/${ticketDetail.id}/close`, {
        method: 'POST',
        headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: closeReason.trim() || null }),
      })
      setCloseModalOpen(false)
      setCloseReason('')
      setCloseConfirmed(false)
      await reloadTicketDetail()
      void fetchTickets()
      message.success('工单已关闭')
    } catch (error: any) {
      message.error(error?.message || '关闭失败')
      if (handleAuthExpired(error)) return
    } finally {
      setCloseSending(false)
    }
  }, [token, ticketDetail, canOperateCurrentTicket, closeReason, reloadTicketDetail, fetchTickets, handleAuthExpired])

  /** 上传附件（通用上传，不关联工单） */
  const handleUploadFile = useCallback(async (file: File) => {
    if (!token) return
    setUploadingFile(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const result = await requestApi<UploadedFile>('/api/v1/files/upload', {
        method: 'POST',
        headers: { ...authHeaders(token) },
        body: formData,
      })
      setUploadedFiles(prev => [...prev, result])
      message.success(`"${file.name}" 上传成功`)
    } catch (error: any) {
      const msg = error?.message || '上传失败'
      message.error(msg)
      if (handleAuthExpired(error)) return
    } finally {
      setUploadingFile(false)
    }
  }, [token, handleAuthExpired])

  /** 移除已上传的附件 */
  const handleRemoveFile = useCallback((objectKey: string) => {
    setUploadedFiles(prev => prev.filter(f => f.objectKey !== objectKey))
  }, [])

  /** 删除附件 */
  const handleDeleteAttachment = useCallback(async (attachmentId: number) => {
    if (!token || !ticketDetail || !canOperateCurrentTicket) return
    try {
      await requestApi(`/api/v1/tickets/${ticketDetail.id}/attachments/${attachmentId}`, {
        method: 'DELETE',
        headers: authHeaders(token),
      })
      setTicketAttachments(prev => prev.filter(a => a.id !== attachmentId))
      message.success('附件已删除')
    } catch (error: any) {
      message.error(error?.message || '附件删除失败')
      if (handleAuthExpired(error)) return
    }
  }, [token, ticketDetail, canOperateCurrentTicket, handleAuthExpired])

  /** 修改优先级 */
  const handlePriority = useCallback(async () => {
    if (!token || !ticketDetail || !canOperateCurrentTicket) return
    setPrioritySending(true)
    try {
      await requestApi(`/api/v1/tickets/${ticketDetail.id}/priority`, {
        method: 'PATCH',
        headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
        body: JSON.stringify({ priority: priorityValue, reason: priorityReason.trim() || null }),
      })
      setPriorityModalOpen(false)
      setPriorityReason('')
      await reloadTicketDetail()
      void fetchTickets()
      message.success('优先级已修改')
    } catch (error: any) {
      message.error(error?.message || '修改优先级失败')
      if (handleAuthExpired(error)) return
    } finally {
      setPrioritySending(false)
    }
  }, [token, ticketDetail, canOperateCurrentTicket, priorityValue, priorityReason, reloadTicketDetail, fetchTickets, handleAuthExpired])

  /** 修改状态 */
  const handleStatusChange = useCallback(async () => {
    if (!token || !ticketDetail || !canOperateCurrentTicket) return
    setStatusSending(true)
    try {
      await requestApi(`/api/v1/tickets/${ticketDetail.id}/status`, {
        method: 'PATCH',
        headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: statusValue, reason: statusReason.trim() || null }),
      })
      setStatusModalOpen(false)
      setStatusReason('')
      await reloadTicketDetail()
      void fetchTickets()
      message.success('状态已修改')
    } catch (error: any) {
      message.error(error?.message || '修改状态失败')
      if (handleAuthExpired(error)) return
    } finally {
      setStatusSending(false)
    }
  }, [token, ticketDetail, canOperateCurrentTicket, statusValue, statusReason, reloadTicketDetail, fetchTickets, handleAuthExpired])

  const fetchTicketStats = useCallback(async () => {
    if (!token) return
    try {
      const data = await requestApi<any>('/api/v1/tickets/stats', { headers: authHeaders(token) })
      setTicketStats(data)
    } catch { /* stats failure is non-critical */ }
  }, [token])

  /** 领取未分配工单 */
  const handleClaimTicket = useCallback(async () => {
    if (!token || !ticketDetail) return
    setClaimSending(true)
    try {
      await requestApi(`/api/v1/tickets/${ticketDetail.id}/claim`, {
        method: 'POST',
        headers: authHeaders(token),
      })
      await reloadTicketDetail()
      void fetchTickets()
      void fetchTicketStats()
      message.success('工单已领取')
    } catch (error: any) {
      message.error(error?.message || '领取失败')
      if (handleAuthExpired(error)) return
    } finally {
      setClaimSending(false)
    }
  }, [token, ticketDetail, reloadTicketDetail, fetchTickets, fetchTicketStats, handleAuthExpired])

  useEffect(() => {
    if (activeMenu === '全部工单') { void fetchTickets(); void fetchTicketStats() }
  }, [fetchTickets, fetchTicketStats, activeMenu])

  // 加载邮箱列表用于下拉筛选
  const fetchMailboxList = useCallback(async () => {
    if (!token) return
    try {
      const data = await requestApi<MailboxPageResponse>('/api/v1/mailboxes?page=1&size=100', {
        headers: authHeaders(token),
      })
      setMailboxes(data.records)
    } catch { /* ignore */ }
  }, [token])

  useEffect(() => {
    if (activeMenu === '收件记录' || activeMenu === '发件记录' || activeMenu === '分配规则') {
      void fetchMailboxList()
    }
  }, [fetchMailboxList, activeMenu])

  function resetUserFilters() {
    setUserKeyword('')
    setUserRoleFilter('ALL')
    setUserEnabledFilter('ALL')
    setUserPage(1)
  }

  function resetMailboxFilters() {
    setMailboxKeyword('')
    setMailboxStatusFilter('ALL')
    setMailboxPage(1)
  }

  function updateMailboxForm(patch: Partial<MailboxFormState>) {
    setMailboxForm((value) => ({ ...value, ...patch }))
    setMailboxDirty(true)
    setMailboxTestResult(null)
    setMailboxesError('')
  }

  function selectMailbox(mailbox: Mailbox) {
    setMailboxForm(toMailboxForm(mailbox))
    setMailboxDirty(false)
    setMailboxTestResult(null)
    setMailboxesError('')
    setActiveMailboxStep('basic')
  }

  function openCreateMailbox() {
    setMailboxForm(emptyMailboxForm)
    setMailboxDirty(true)
    setMailboxTestResult(null)
    setMailboxesError('')
    setActiveMailboxStep('basic')
  }

  function moveMailboxStep(direction: 1 | -1) {
    const currentIndex = mailboxSteps.findIndex((step) => step.key === activeMailboxStep)
    const nextIndex = Math.min(Math.max(currentIndex + direction, 0), mailboxSteps.length - 1)
    setActiveMailboxStep(mailboxSteps[nextIndex].key)
  }

  function buildMailboxPayload() {
    return {
      mailboxName: mailboxForm.mailboxName,
      emailAddress: mailboxForm.emailAddress,
      enabled: mailboxForm.enabled,
      defaultAssigneeId: mailboxForm.defaultAssigneeId ? Number(mailboxForm.defaultAssigneeId) : null,
      imapHost: mailboxForm.imapHost,
      imapPort: Number(mailboxForm.imapPort),
      imapSslEnabled: mailboxForm.imapSslEnabled,
      imapUsername: mailboxForm.imapUsername,
      imapPassword: mailboxForm.imapPassword,
      imapFolder: mailboxForm.imapFolder,
      fetchIntervalSec: Number(mailboxForm.fetchIntervalSec),
      smtpHost: mailboxForm.smtpHost,
      smtpPort: Number(mailboxForm.smtpPort),
      smtpSslEnabled: mailboxForm.smtpSslEnabled,
      smtpUsername: mailboxForm.smtpUsername,
      smtpPassword: mailboxForm.smtpPassword,
      smtpFromName: mailboxForm.smtpFromName,
      autoReplyEnabled: mailboxForm.autoReplyEnabled,
      autoReplyTemplateId: mailboxForm.autoReplyTemplateId ? Number(mailboxForm.autoReplyTemplateId) : null,
    }
  }

  async function saveMailbox() {
    if (!token) return
    setMailboxSaving(true)
    setMailboxesError('')
    try {
      const saved = await requestApi<Mailbox>(
        mailboxForm.id ? `/api/v1/mailboxes/${mailboxForm.id}` : '/api/v1/mailboxes',
        {
          method: mailboxForm.id ? 'PUT' : 'POST',
          headers: authHeaders(token),
          body: JSON.stringify(buildMailboxPayload()),
        },
      )
      setMailboxForm(toMailboxForm(saved))
      setMailboxDirty(false)
      await fetchMailboxes()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxesError(error instanceof Error ? error.message : '邮箱配置保存失败')
    } finally {
      setMailboxSaving(false)
    }
  }

  async function testMailboxConnection(testType = 'ALL') {
    if (!token) return
    setMailboxTesting(true)
    setMailboxesError('')
    try {
      const data = mailboxForm.id && !mailboxDirty
        ? await requestApi<MailboxConnectionTestResponse>(`/api/v1/mailboxes/${mailboxForm.id}/test-connection?testType=${testType}`, {
            method: 'POST',
            headers: authHeaders(token),
          })
        : await requestApi<MailboxConnectionTestResponse>('/api/v1/mailboxes/test-connection', {
            method: 'POST',
            headers: authHeaders(token),
            body: JSON.stringify({ ...buildMailboxPayload(), testType }),
          })
      setMailboxTestResult(data)
      if (mailboxForm.id && !mailboxDirty) {
        await fetchMailboxes()
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxesError(error instanceof Error ? error.message : '连接测试失败')
    } finally {
      setMailboxTesting(false)
    }
  }

  function openMailboxConfirm(mailbox: Mailbox, type: 'enable' | 'disable' | 'delete') {
    if (type === 'delete') {
      setMailboxConfirmAction({
        mailbox,
        type,
        title: '删除邮箱配置',
        text: '删除后该邮箱不再参与拉取，历史邮件、工单和发送记录保留。',
        actionLabel: '确认删除',
      })
      return
    }
    setMailboxConfirmAction({
      mailbox,
      type,
      title: type === 'enable' ? '启用邮箱配置' : '停用邮箱配置',
      text: type === 'enable' ? '启用后后台任务可继续拉取该邮箱。' : '停用后后台任务将不再拉取该邮箱。',
      actionLabel: type === 'enable' ? '确认启用' : '确认停用',
    })
  }

  async function submitMailboxConfirm() {
    if (!token || !mailboxConfirmAction) return
    setMailboxActionLoading(true)
    setMailboxesError('')
    try {
      if (mailboxConfirmAction.type === 'delete') {
        await requestApi<void>(`/api/v1/mailboxes/${mailboxConfirmAction.mailbox.id}`, {
          method: 'DELETE',
          headers: authHeaders(token),
        })
        if (mailboxForm.id === mailboxConfirmAction.mailbox.id) {
          setMailboxForm(emptyMailboxForm)
          setMailboxDirty(false)
        }
      } else {
        await requestApi<Mailbox>(`/api/v1/mailboxes/${mailboxConfirmAction.mailbox.id}/enabled`, {
          method: 'PATCH',
          headers: authHeaders(token),
          body: JSON.stringify({ enabled: mailboxConfirmAction.type === 'enable' }),
        })
      }
      setMailboxConfirmAction(null)
      await fetchMailboxes()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxesError(error instanceof Error ? error.message : '邮箱操作失败')
    } finally {
      setMailboxActionLoading(false)
    }
  }

  function resetAssignmentFilters() {
    setAssignmentKeyword('')
    setAssignmentEnabledFilter('ALL')
    setAssignmentMatchTypeFilter('ALL')
  }

  function updateAssignmentForm(patch: Partial<AssignmentRuleFormState>) {
    setAssignmentForm((value) => {
      const next = { ...value, ...patch }
      if (patch.matchType === 'DEFAULT') {
        next.defaultRule = true
        next.matchValue = ''
      } else if (patch.matchType) {
        next.defaultRule = false
      }
      return next
    })
    setAssignmentRuleDirty(true)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }

  function selectAssignmentRule(rule: AssignmentRule) {
    setAssignmentForm(toAssignmentRuleForm(rule))
    setAssignmentRuleDirty(false)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }

  function openCreateAssignmentRule() {
    setAssignmentForm({
      ...emptyAssignmentRuleForm,
      priorityOrder: (assignmentRulesData?.records.length ?? 0) * 10 + 10,
      assigneeId: assignmentAssignees[0] ? String(assignmentAssignees[0].id) : '',
    })
    setAssignmentRuleDirty(true)
    setAssignmentMatchResult(null)
    setAssignmentRulesError('')
  }

  function buildAssignmentRulePayload() {
    return {
      ruleName: assignmentForm.ruleName.trim(),
      enabled: assignmentForm.enabled,
      priorityOrder: Number(assignmentForm.priorityOrder),
      defaultRule: assignmentForm.matchType === 'DEFAULT',
      matchType: assignmentForm.matchType,
      matchValue: assignmentForm.matchType === 'DEFAULT' ? '' : assignmentForm.matchValue.trim(),
      assigneeId: assignmentForm.assigneeId ? Number(assignmentForm.assigneeId) : null,
      notifyEnabled: assignmentForm.notifyEnabled,
    }
  }

  async function saveAssignmentRule() {
    if (!token) return
    setAssignmentSaving(true)
    setAssignmentRulesError('')
    try {
      const saved = await requestApi<AssignmentRule>(
        assignmentForm.id ? `/api/v1/assignment-rules/${assignmentForm.id}` : '/api/v1/assignment-rules',
        {
          method: assignmentForm.id ? 'PUT' : 'POST',
          headers: authHeaders(token),
          body: JSON.stringify(buildAssignmentRulePayload()),
        },
      )
      setAssignmentForm(toAssignmentRuleForm(saved))
      setAssignmentRuleDirty(false)
      await fetchAssignmentRules()
      message.success('分配规则已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '分配规则保存失败')
    } finally {
      setAssignmentSaving(false)
    }
  }

  async function toggleAssignmentRule(rule: AssignmentRule, enabled: boolean) {
    if (!token) return
    setAssignmentActionLoading(true)
    setAssignmentRulesError('')
    try {
      const saved = await requestApi<AssignmentRule>(`/api/v1/assignment-rules/${rule.id}/enabled`, {
        method: 'PATCH',
        headers: authHeaders(token),
        body: JSON.stringify({ enabled }),
      })
      if (assignmentForm.id === saved.id) {
        setAssignmentForm(toAssignmentRuleForm(saved))
        setAssignmentRuleDirty(false)
      }
      await fetchAssignmentRules()
      message.success(enabled ? '规则已启用' : '规则已停用')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '规则启停失败')
    } finally {
      setAssignmentActionLoading(false)
    }
  }

  async function moveAssignmentRule(rule: AssignmentRule, direction: 1 | -1) {
    if (!token || !assignmentRulesData) return
    const records = [...assignmentRulesData.records].sort((a, b) => a.priorityOrder - b.priorityOrder || a.id - b.id)
    const index = records.findIndex((item) => item.id === rule.id)
    const targetIndex = index + direction
    if (index < 0 || targetIndex < 0 || targetIndex >= records.length) return
    const current = records[index]
    const target = records[targetIndex]
    records[index] = { ...target, priorityOrder: current.priorityOrder }
    records[targetIndex] = { ...current, priorityOrder: target.priorityOrder }

    setAssignmentActionLoading(true)
    setAssignmentRulesError('')
    try {
      await requestApi<AssignmentRule[]>('/api/v1/assignment-rules/sort', {
        method: 'PUT',
        headers: authHeaders(token),
        body: JSON.stringify({
          rules: records.map((item) => ({ id: item.id, priorityOrder: item.priorityOrder })),
        }),
      })
      await fetchAssignmentRules()
      message.success('规则排序已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '规则排序失败')
    } finally {
      setAssignmentActionLoading(false)
    }
  }

  async function runAssignmentRuleTest() {
    if (!token) return
    setAssignmentTesting(true)
    setAssignmentRulesError('')
    try {
      const mailbox = mailboxes.find((item) => String(item.id) === assignmentTestForm.mailboxId)
      const result = await requestApi<AssignmentRuleMatchResponse>('/api/v1/assignment-rules/test-match', {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify({
          mailboxId: assignmentTestForm.mailboxId ? Number(assignmentTestForm.mailboxId) : null,
          mailboxAddress: mailbox?.emailAddress || '',
          subject: assignmentTestForm.subject.trim(),
          fromEmail: assignmentTestForm.fromEmail.trim(),
        }),
      })
      setAssignmentMatchResult(result)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '测试匹配失败')
    } finally {
      setAssignmentTesting(false)
    }
  }

  async function submitAssignmentConfirm() {
    if (!token || !assignmentConfirmAction) return
    setAssignmentActionLoading(true)
    setAssignmentRulesError('')
    try {
      await requestApi<void>(`/api/v1/assignment-rules/${assignmentConfirmAction.rule.id}`, {
        method: 'DELETE',
        headers: authHeaders(token),
      })
      if (assignmentForm.id === assignmentConfirmAction.rule.id) {
        setAssignmentForm(emptyAssignmentRuleForm)
        setAssignmentRuleDirty(false)
      }
      setAssignmentConfirmAction(null)
      await fetchAssignmentRules()
      message.success('分配规则已删除')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setAssignmentRulesError(error instanceof Error ? error.message : '分配规则删除失败')
    } finally {
      setAssignmentActionLoading(false)
    }
  }

  function resetSlaPolicyFilters() {
    setSlaPolicyKeyword('')
    setSlaPolicyEnabledFilter('ALL')
    setSlaPolicyDefaultFilter('ALL')
  }

  function updateSlaPolicyForm(patch: Partial<SlaPolicyFormState>) {
    setSlaPolicyForm((value) => {
      const next = { ...value, ...patch }
      if (patch.defaultPolicy === true) {
        next.enabled = true
      }
      if (patch.enabled === false && value.defaultPolicy) {
        next.defaultPolicy = false
      }
      return next
    })
    setSlaPolicyDirty(true)
    setSlaPoliciesError('')
  }

  function selectSlaPolicy(policy: SlaPolicy) {
    setSlaPolicyForm(toSlaPolicyForm(policy))
    setSlaPolicyDirty(false)
    setSlaPoliciesError('')
  }

  function openCreateSlaPolicy() {
    setSlaPolicyForm({
      ...emptySlaPolicyForm,
      calendarId: workCalendars[0] ? String(workCalendars[0].id) : '',
    })
    setSlaPolicyDirty(true)
    setSlaPoliciesError('')
  }

  function buildSlaPolicyPayload() {
    return {
      policyName: slaPolicyForm.policyName.trim(),
      enabled: slaPolicyForm.enabled,
      defaultPolicy: slaPolicyForm.defaultPolicy,
      responseHours: Number(slaPolicyForm.responseHours),
      resolveHours: slaPolicyForm.resolveHours.trim() ? Number(slaPolicyForm.resolveHours) : null,
      warningRemainHours: Number(slaPolicyForm.warningRemainHours),
      escalateAfterBreachHours: slaPolicyForm.escalateAfterBreachHours.trim()
        ? Number(slaPolicyForm.escalateAfterBreachHours)
        : null,
      calendarId: slaPolicyForm.calendarId ? Number(slaPolicyForm.calendarId) : null,
    }
  }

  async function saveSlaPolicy() {
    if (!token) return
    setSlaPolicySaving(true)
    setSlaPoliciesError('')
    try {
      const saved = await requestApi<SlaPolicy>(
        slaPolicyForm.id ? `/api/v1/sla-policies/${slaPolicyForm.id}` : '/api/v1/sla-policies',
        {
          method: slaPolicyForm.id ? 'PUT' : 'POST',
          headers: authHeaders(token),
          body: JSON.stringify(buildSlaPolicyPayload()),
        },
      )
      setSlaPolicyForm(toSlaPolicyForm(saved))
      setSlaPolicyDirty(false)
      await fetchSlaPolicies()
      message.success('SLA 策略已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : 'SLA 策略保存失败')
    } finally {
      setSlaPolicySaving(false)
    }
  }

  async function toggleSlaPolicy(policy: SlaPolicy, enabled: boolean) {
    if (!token) return
    setSlaPolicyActionLoading(true)
    setSlaPoliciesError('')
    try {
      const saved = await requestApi<SlaPolicy>(`/api/v1/sla-policies/${policy.id}/enabled`, {
        method: 'PATCH',
        headers: authHeaders(token),
        body: JSON.stringify({ enabled }),
      })
      if (slaPolicyForm.id === saved.id) {
        setSlaPolicyForm(toSlaPolicyForm(saved))
        setSlaPolicyDirty(false)
      }
      await fetchSlaPolicies()
      message.success(enabled ? 'SLA 策略已启用' : 'SLA 策略已停用')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : 'SLA 策略启停失败')
    } finally {
      setSlaPolicyActionLoading(false)
    }
  }

  async function setDefaultSlaPolicy(policy: SlaPolicy) {
    if (!token) return
    setSlaPolicyActionLoading(true)
    setSlaPoliciesError('')
    try {
      const saved = await requestApi<SlaPolicy>(`/api/v1/sla-policies/${policy.id}/default`, {
        method: 'PATCH',
        headers: authHeaders(token),
        body: JSON.stringify({ defaultPolicy: true }),
      })
      setSlaPolicyForm(toSlaPolicyForm(saved))
      setSlaPolicyDirty(false)
      await fetchSlaPolicies()
      message.success('默认 SLA 策略已更新')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : '默认策略设置失败')
    } finally {
      setSlaPolicyActionLoading(false)
    }
  }

  async function submitSlaPolicyConfirm() {
    if (!token || !slaPolicyConfirmAction) return
    setSlaPolicyActionLoading(true)
    setSlaPoliciesError('')
    try {
      await requestApi<void>(`/api/v1/sla-policies/${slaPolicyConfirmAction.policy.id}`, {
        method: 'DELETE',
        headers: authHeaders(token),
      })
      if (slaPolicyForm.id === slaPolicyConfirmAction.policy.id) {
        setSlaPolicyForm({
          ...emptySlaPolicyForm,
          calendarId: workCalendars[0] ? String(workCalendars[0].id) : '',
        })
        setSlaPolicyDirty(false)
      }
      setSlaPolicyConfirmAction(null)
      await fetchSlaPolicies()
      message.success('SLA 策略已删除')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setSlaPoliciesError(error instanceof Error ? error.message : 'SLA 策略删除失败')
    } finally {
      setSlaPolicyActionLoading(false)
    }
  }

  function resetWorkCalendarFilters() {
    setWorkCalendarKeyword('')
    setWorkCalendarDefaultFilter('ALL')
  }

  function updateWorkCalendarForm(patch: Partial<WorkCalendarFormState>) {
    setWorkCalendarForm((value) => ({ ...value, ...patch }))
    setWorkCalendarDirty(true)
    setWorkCalendarError('')
  }

  function selectWorkCalendar(calendar: WorkCalendar) {
    setWorkCalendarForm(toWorkCalendarForm(calendar))
    setWorkCalendarDirty(false)
    setWorkCalendarError('')
    setHolidayForm({
      ...emptyHolidayForm,
      calendarId: String(calendar.id),
      holidayDate: `${holidayMonth}-01`,
    })
    setHolidayDirty(false)
  }

  function openCreateWorkCalendar() {
    setWorkCalendarForm(emptyWorkCalendarForm)
    setWorkCalendarDirty(true)
    setWorkCalendarError('')
    setHolidayForm(emptyHolidayForm)
    setHolidayDirty(false)
  }

  function buildWorkCalendarPayload() {
    return {
      calendarName: workCalendarForm.calendarName.trim(),
      timezone: workCalendarForm.timezone.trim() || 'Asia/Shanghai',
      workdays: [...workCalendarForm.workdays].sort((a, b) => a - b),
      workStartTime: workCalendarForm.workStartTime,
      workEndTime: workCalendarForm.workEndTime,
      defaultCalendar: workCalendarForm.defaultCalendar,
    }
  }

  async function saveWorkCalendar() {
    if (!token) return
    setWorkCalendarSaving(true)
    setWorkCalendarError('')
    try {
      const saved = await requestApi<WorkCalendar>(
        workCalendarForm.id ? `/api/v1/work-calendars/${workCalendarForm.id}` : '/api/v1/work-calendars',
        {
          method: workCalendarForm.id ? 'PUT' : 'POST',
          headers: authHeaders(token),
          body: JSON.stringify(buildWorkCalendarPayload()),
        },
      )
      setWorkCalendarForm(toWorkCalendarForm(saved))
      setWorkCalendarDirty(false)
      setHolidayForm((form) => ({
        ...form,
        calendarId: String(saved.id),
      }))
      await fetchWorkCalendarsPage()
      message.success('工作日历已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setWorkCalendarError(error instanceof Error ? error.message : '工作日历保存失败')
    } finally {
      setWorkCalendarSaving(false)
    }
  }

  async function setDefaultWorkCalendar(calendar: WorkCalendar) {
    if (!token) return
    setWorkCalendarActionLoading(true)
    setWorkCalendarError('')
    try {
      const saved = await requestApi<WorkCalendar>(`/api/v1/work-calendars/${calendar.id}/default`, {
        method: 'PATCH',
        headers: authHeaders(token),
        body: JSON.stringify({ defaultCalendar: true }),
      })
      setWorkCalendarForm(toWorkCalendarForm(saved))
      setWorkCalendarDirty(false)
      await fetchWorkCalendarsPage()
      message.success('默认工作日历已更新')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setWorkCalendarError(error instanceof Error ? error.message : '默认工作日历设置失败')
    } finally {
      setWorkCalendarActionLoading(false)
    }
  }

  function openCreateHoliday() {
    setHolidayForm({
      ...emptyHolidayForm,
      calendarId: workCalendarForm.id ? String(workCalendarForm.id) : '',
      holidayDate: `${holidayMonth}-01`,
    })
    setHolidayDirty(true)
    setHolidaysError('')
  }

  function updateHolidayForm(patch: Partial<HolidayFormState>) {
    setHolidayForm((value) => ({ ...value, ...patch }))
    setHolidayDirty(true)
    setHolidaysError('')
  }

  function selectHoliday(holiday: Holiday) {
    setHolidayForm(toHolidayForm(holiday))
    setHolidayDirty(false)
    setHolidaysError('')
  }

  function buildHolidayPayload() {
    return {
      calendarId: holidayForm.calendarId ? Number(holidayForm.calendarId) : null,
      holidayDate: holidayForm.holidayDate,
      holidayName: holidayForm.holidayName.trim(),
    }
  }

  async function saveHoliday() {
    if (!token) return
    setHolidaySaving(true)
    setHolidaysError('')
    try {
      const saved = await requestApi<Holiday>(
        holidayForm.id ? `/api/v1/holidays/${holidayForm.id}` : '/api/v1/holidays',
        {
          method: holidayForm.id ? 'PUT' : 'POST',
          headers: authHeaders(token),
          body: JSON.stringify(buildHolidayPayload()),
        },
      )
      setHolidayForm(toHolidayForm(saved))
      setHolidayDirty(false)
      await fetchHolidays()
      message.success('节假日已保存')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setHolidaysError(error instanceof Error ? error.message : '节假日保存失败')
    } finally {
      setHolidaySaving(false)
    }
  }

  async function importNationalHolidays() {
    if (!token || !workCalendarForm.id) return
    const importYear = Number(holidayMonth.slice(0, 4)) || dayjs().year()
    setHolidayImporting(true)
    setHolidaysError('')
    try {
      const preset = await requestApi<NationalHolidayPresetResponse>(
        `/api/v1/holidays/national-presets?year=${importYear}`,
        { headers: authHeaders(token) },
      )
      const params = new URLSearchParams({
        calendarId: String(workCalendarForm.id),
        dateFrom: `${importYear}-01-01`,
        dateTo: `${importYear}-12-31`,
      })
      const data = await requestApi<HolidayListResponse>(`/api/v1/holidays?${params.toString()}`, {
        headers: authHeaders(token),
      })
      const existingDates = new Set(data.records.map((holiday) => holiday.holidayDate))
      const missingHolidays = preset.records.filter((holiday) => !existingDates.has(holiday.holidayDate))

      for (const holiday of missingHolidays) {
        await requestApi<Holiday>('/api/v1/holidays', {
          method: 'POST',
          headers: authHeaders(token),
          body: JSON.stringify({
            calendarId: workCalendarForm.id,
            holidayDate: holiday.holidayDate,
            holidayName: holiday.holidayName,
          }),
        })
      }

      await fetchHolidays()
      if (missingHolidays.length > 0) {
        message.success(`已从${preset.sourceName}导入 ${missingHolidays.length} 天 ${importYear} 法定节假日`)
      } else {
        message.info(`${importYear} 法定节假日已存在，无需重复导入`)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setHolidaysError(error instanceof Error ? error.message : '法定节假日导入失败')
    } finally {
      setHolidayImporting(false)
    }
  }

  async function submitWorkCalendarConfirm() {
    if (!token || !workCalendarConfirmAction) return
    setWorkCalendarActionLoading(true)
    setWorkCalendarError('')
    setHolidaysError('')
    try {
      if (workCalendarConfirmAction.type === 'delete-calendar') {
        await requestApi<void>(`/api/v1/work-calendars/${workCalendarConfirmAction.calendar.id}`, {
          method: 'DELETE',
          headers: authHeaders(token),
        })
        if (workCalendarForm.id === workCalendarConfirmAction.calendar.id) {
          setWorkCalendarForm(emptyWorkCalendarForm)
          setWorkCalendarDirty(false)
        }
        await fetchWorkCalendarsPage()
        await fetchHolidays()
        message.success('工作日历已删除')
      } else {
        await requestApi<void>(`/api/v1/holidays/${workCalendarConfirmAction.holiday.id}`, {
          method: 'DELETE',
          headers: authHeaders(token),
        })
        if (holidayForm.id === workCalendarConfirmAction.holiday.id) {
          openCreateHoliday()
        }
        await fetchHolidays()
        message.success('节假日已删除')
      }
      setWorkCalendarConfirmAction(null)
    } catch (error) {
      if (handleAuthExpired(error)) return
      const fallback = workCalendarConfirmAction.type === 'delete-calendar' ? '工作日历删除失败' : '节假日删除失败'
      if (workCalendarConfirmAction.type === 'delete-calendar') {
        setWorkCalendarError(error instanceof Error ? error.message : fallback)
      } else {
        setHolidaysError(error instanceof Error ? error.message : fallback)
      }
    } finally {
      setWorkCalendarActionLoading(false)
    }
  }

  function openCreateUser() {
    setUserFormMode('create')
    setEditingUser(null)
    setUserForm(emptyUserForm)
    setUserFormError('')
    setUserFormOpen(true)
  }

  function openCreateRole() {
    roleDraftModeRef.current = 'create'
    selectedRoleIdRef.current = null
    setRoleDraftMode('create')
    setSelectedRoleId(null)
    setRoleForm(emptyRoleForm)
    setRolesError('')
  }

  function selectRole(role: ManagedRole) {
    hydrateRoleForm(role)
    setRolesError('')
  }

  function toggleRolePermission(permissionCode: string, checked: boolean) {
    setRoleForm((value) => {
      const next = new Set(value.permissionCodes)
      if (checked) {
        next.add(permissionCode)
      } else {
        next.delete(permissionCode)
      }
      return { ...value, permissionCodes: Array.from(next) }
    })
  }

  function updateRoleScope(resourceType: string, scopeCode: string) {
    setRoleForm((value) => ({
      ...value,
      dataScopes: ['TICKET', 'CUSTOMER', 'DASHBOARD'].map((resource) => {
        const nextScope = resource === resourceType ? scopeCode : (value.dataScopes.find((scope) => scope.resourceType === resource)?.scopeCode || 'SELF')
        return {
          resourceType: resource,
          scopeCode: nextScope,
          scopeDesc: dataScopeDesc(resource, nextScope),
        }
      }),
    }))
  }

  async function submitRoleBase() {
    if (!token) return
    setRoleSaving(true)
    setRolesError('')
    try {
      let saved: ManagedRole
      if (roleDraftMode === 'create') {
        saved = await requestApi<ManagedRole>('/api/v1/roles', {
          method: 'POST',
          headers: authHeaders(token),
          body: JSON.stringify({
            roleName: roleForm.roleName,
            roleDesc: roleForm.roleDesc,
            enabled: roleForm.enabled,
          }),
        })
        if (roleForm.permissionCodes.length > 0) {
          saved = await requestApi<ManagedRole>(`/api/v1/roles/${saved.id}/permissions`, {
            method: 'PUT',
            headers: authHeaders(token),
            body: JSON.stringify({
              permissionCodes: roleForm.permissionCodes,
              dataScopes: roleForm.dataScopes,
            }),
          })
        }
      } else if (selectedRole) {
        saved = await requestApi<ManagedRole>(`/api/v1/roles/${selectedRole.id}`, {
          method: 'PUT',
          headers: authHeaders(token),
          body: JSON.stringify({
            roleName: roleForm.roleName,
            roleDesc: roleForm.roleDesc,
            enabled: roleForm.enabled,
          }),
        })
      } else {
        return
      }
      roleDraftModeRef.current = 'edit'
      selectedRoleIdRef.current = saved.id
      setRoleDraftMode('edit')
      setSelectedRoleId(saved.id)
      hydrateRoleForm(saved)
      await fetchRoles(saved.id)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '角色保存失败')
    } finally {
      setRoleSaving(false)
    }
  }

  async function submitRolePermissions() {
    if (!token || !selectedRole) return
    setRolePermissionSaving(true)
    setRolesError('')
    try {
      const saved = await requestApi<ManagedRole>(`/api/v1/roles/${selectedRole.id}/permissions`, {
        method: 'PUT',
        headers: authHeaders(token),
        body: JSON.stringify({
          permissionCodes: roleForm.permissionCodes,
          dataScopes: roleForm.dataScopes,
        }),
      })
      hydrateRoleForm(saved)
      await fetchRoles()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '角色权限保存失败')
    } finally {
      setRolePermissionSaving(false)
    }
  }

  async function toggleRoleEnabled(role: ManagedRole) {
    if (!token || role.systemRole || !canEnableRoles) return
    setRoleSaving(true)
    setRolesError('')
    try {
      const saved = await requestApi<ManagedRole>(`/api/v1/roles/${role.id}/enabled`, {
        method: 'PATCH',
        headers: authHeaders(token),
        body: JSON.stringify({ enabled: !role.enabled }),
      })
      hydrateRoleForm(saved)
      await fetchRoles()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setRolesError(error instanceof Error ? error.message : '角色启停失败')
    } finally {
      setRoleSaving(false)
    }
  }

  function openEditUser(nextUser: ManagedUser) {
    setUserFormMode('edit')
    setEditingUser(nextUser)
    const assignedRoleCodes = normalizeRoleCodes(nextUser.roleCode, nextUser.roleCodes || [])
    setUserForm({
      account: nextUser.account,
      displayName: nextUser.displayName,
      email: nextUser.email,
      roleCode: nextUser.roleCode,
      roleCodes: assignedRoleCodes,
      password: '',
      enabled: nextUser.enabled,
    })
    setUserFormError('')
    setUserFormOpen(true)
  }

  async function submitUserForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) return
    setUserFormSubmitting(true)
    setUserFormError('')
    try {
      if (userFormMode === 'create') {
        await requestApi<ManagedUser>('/api/v1/users', {
          method: 'POST',
          headers: authHeaders(token),
          body: JSON.stringify(userForm),
        })
        setUserPage(1)
      } else if (editingUser) {
        await requestApi<ManagedUser>(`/api/v1/users/${editingUser.id}`, {
          method: 'PUT',
          headers: authHeaders(token),
          body: JSON.stringify({
            displayName: userForm.displayName,
            email: userForm.email,
            roleCode: userForm.roleCode,
            roleCodes: normalizeRoleCodes(userForm.roleCode, userForm.roleCodes),
            enabled: userForm.enabled,
          }),
        })
      }
      setUserFormOpen(false)
      await fetchUsers()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setUserFormError(error instanceof Error ? error.message : '保存失败，请稍后重试')
    } finally {
      setUserFormSubmitting(false)
    }
  }

  function openEnabledConfirm(nextUser: ManagedUser) {
    setConfirmAction({
      user: nextUser,
      type: nextUser.enabled ? 'disable' : 'enable',
      title: nextUser.enabled ? '停用用户确认' : '启用用户确认',
      text: nextUser.enabled
        ? '停用后该账号将无法登录系统，历史工单归属和操作日志仍保留。'
        : '启用后该账号可重新登录系统并处理工单。',
      actionLabel: nextUser.enabled ? '确认停用' : '确认启用',
    })
  }

  function openResetConfirm(nextUser: ManagedUser) {
    setConfirmAction({
      user: nextUser,
      type: 'reset',
      title: '重置密码确认',
      text: '确认后将把该用户密码重置为临时密码 Mail@2026，请管理员线下告知用户登录后及时修改。',
      actionLabel: '确认重置',
    })
  }

  async function submitConfirmAction() {
    if (!token || !confirmAction) return
    setActionLoading(true)
    setUsersError('')
    try {
      if (confirmAction.type === 'reset') {
        await requestApi<void>(`/api/v1/users/${confirmAction.user.id}/reset-password`, {
          method: 'POST',
          headers: authHeaders(token),
          body: JSON.stringify({ password: 'Mail@2026' }),
        })
      } else {
        await requestApi<ManagedUser>(`/api/v1/users/${confirmAction.user.id}/enabled`, {
          method: 'PATCH',
          headers: authHeaders(token),
          body: JSON.stringify({ enabled: confirmAction.type === 'enable' }),
        })
      }
      setConfirmAction(null)
      await fetchUsers()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setUsersError(error instanceof Error ? error.message : '操作失败，请稍后重试')
    } finally {
      setActionLoading(false)
    }
  }

  function selectTemplate(template: NotificationTemplate) {
    setTemplateDraftMode(false)
    setSelectedTemplateId(template.id)
    setTemplateForm(toTemplateForm(template))
    setTemplateDirty(false)
    setTemplatePreview(null)
    setTemplatesError('')
  }

  function openCreateTemplate() {
    setTemplateDraftMode(true)
    setSelectedTemplateId(null)
    setTemplateKeyword('')
    setTemplateForm({
      id: null,
      templateCode: '',
      templateName: '自定义通知模板',
      subjectTpl: '通知：{ticket_no}',
      contentTpl: '您好，工单 {ticket_no} 有新的通知。\n\n工单主题：{subject}',
      enabled: true,
    })
    setTemplateDirty(true)
    setTemplatePreview(null)
    setTemplatesError('')
  }

  function updateTemplateForm(patch: Partial<TemplateFormState>) {
    setTemplateForm((value) => ({ ...value, ...patch }))
    setTemplateDirty(true)
    setTemplatePreview(null)
  }

  function updateTicketRuleForm(patch: Partial<TicketRuleFormState>) {
    setTicketRuleForm((value) => ({ ...value, ...patch }))
    setTicketRuleDirty(true)
    setTicketRuleMessage('')
  }

  function insertVariable(variableKey: string) {
    const textarea = templateContentRef.current
    if (!textarea) {
      updateTemplateForm({ contentTpl: `${templateForm.contentTpl}${variableKey}` })
      return
    }
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const contentTpl = `${templateForm.contentTpl.slice(0, start)}${variableKey}${templateForm.contentTpl.slice(end)}`
    updateTemplateForm({ contentTpl })
    window.requestAnimationFrame(() => {
      textarea.focus()
      textarea.selectionStart = start + variableKey.length
      textarea.selectionEnd = start + variableKey.length
    })
  }

  async function previewTemplate() {
    if (!token) return
    setTemplatePreviewLoading(true)
    setTemplatesError('')
    try {
      const data = await requestApi<TemplatePreviewResponse>('/api/v1/notification-templates/preview', {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify({
          subjectTpl: templateForm.subjectTpl,
          contentTpl: templateForm.contentTpl,
        }),
      })
      setTemplatePreview(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTemplatesError(error instanceof Error ? error.message : '预览生成失败')
    } finally {
      setTemplatePreviewLoading(false)
    }
  }

  async function saveTemplate() {
    if (!token) return
    setTemplateSaving(true)
    setTemplatesError('')
    try {
      const saved = await requestApi<NotificationTemplate>(
        templateForm.id ? `/api/v1/notification-templates/${templateForm.id}` : '/api/v1/notification-templates',
        {
          method: templateForm.id ? 'PUT' : 'POST',
          headers: authHeaders(token),
          body: JSON.stringify({
            templateCode: templateForm.templateCode,
            templateName: templateForm.templateName,
            subjectTpl: templateForm.subjectTpl,
            contentTpl: templateForm.contentTpl,
            enabled: templateForm.enabled,
          }),
        },
      )
      setSelectedTemplateId(saved.id)
      setTemplateDraftMode(false)
      setTemplateForm(toTemplateForm(saved))
      setTemplateDirty(false)
      setTemplateConfirmOpen(false)
      await fetchTemplates()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTemplatesError(error instanceof Error ? error.message : '模板保存失败')
    } finally {
      setTemplateSaving(false)
    }
  }

  async function previewTicketRule() {
    if (!token) return
    setTicketRulePreviewLoading(true)
    setTicketRuleError('')
    setTicketRuleMessage('')
    try {
      const data = await requestApi<TicketNumberRule>('/api/v1/sys-params/ticket-number-rule/preview', {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify(ticketRuleForm),
      })
      setTicketRule(data)
      setTicketRuleForm(toTicketRuleForm(data))
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketRuleError(error instanceof Error ? error.message : '规则预览失败')
    } finally {
      setTicketRulePreviewLoading(false)
    }
  }

  async function saveTicketRule() {
    if (!token) return
    setTicketRuleSaving(true)
    setTicketRuleError('')
    setTicketRuleMessage('')
    try {
      const data = await requestApi<TicketNumberRule>('/api/v1/sys-params/ticket-number-rule', {
        method: 'PUT',
        headers: authHeaders(token),
        body: JSON.stringify(ticketRuleForm),
      })
      setTicketRule(data)
      setTicketRuleForm(toTicketRuleForm(data))
      setTicketRuleDirty(false)
      setTicketRuleConfirmOpen(false)
      setTicketRuleMessage('编号规则已保存，后续新建工单将使用当前规则。')
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTicketRuleError(error instanceof Error ? error.message : '编号规则保存失败')
    } finally {
      setTicketRuleSaving(false)
    }
  }

  function resetTicketRule() {
    updateTicketRuleForm(emptyTicketRuleForm)
    setTicketRuleMessage('已恢复默认规则，保存前可先生成预览确认。')
  }

  function validateForm() {
    const normalizedAccount = account.trim()
    const normalizedPassword = password.trim()
    const nextAccountError = normalizedAccount ? '' : '请输入账号或邮箱'
    const nextPasswordError = normalizedPassword ? '' : '请输入密码'

    setAccountError(nextAccountError)
    setPasswordError(nextPasswordError)

    if (nextAccountError || nextPasswordError) {
      setFormError('请输入账号和密码后再登录')
      return false
    }

    return true
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError('')

    if (!validateForm()) return

    setSubmitting(true)
    try {
      const payload = await requestApi<LoginResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          account: account.trim(),
          password: password.trim(),
          rememberMe,
        }),
      })
      storeSession(payload, rememberMe)
      setToken(payload.token)
      setUser(payload.user)
      setPassword('')
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '登录失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleLogout() {
    try {
      if (token) {
        await requestApi<void>('/api/v1/auth/logout', {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        })
      }
    } finally {
      clearSession()
      setToken('')
      setUser(null)
      setAccount('')
      setPassword('')
      setFormError('')
    }
  }

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
              disabled={selectedRoleReadonly || roleSaving || rolePermissionSaving || !canUpdateRolePermissions}
              onChange={(event) => toggleRolePermission(group.permissionCode, event.target.checked)}
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
                  disabled={selectedRoleReadonly || roleSaving || rolePermissionSaving || !canUpdateRolePermissions}
                  onChange={(event) => toggleRolePermission(child.permissionCode, event.target.checked)}
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
                        disabled={selectedRoleReadonly || roleSaving || rolePermissionSaving || !canUpdateRolePermissions}
                        onChange={(event) => toggleRolePermission(action.permissionCode, event.target.checked)}
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

  if (checkingSession) {
    return (
      <main className="session-check">
        <div className="session-check__panel">
          <div className="brand-mark">M</div>
          <p>正在恢复登录状态...</p>
        </div>
      </main>
    )
  }

  if (user) {
    const userInitial = user.displayName.trim().charAt(0) || user.account.trim().charAt(0).toUpperCase() || 'U'
    const activeMailboxStepIndex = mailboxSteps.findIndex((step) => step.key === activeMailboxStep)
    const mailboxRecords = mailboxesData?.records ?? []
    const activeMailboxCount = mailboxRecords.filter((mailbox) => mailbox.enabled).length
    const mailboxTaskRows = mailboxRecords.slice(0, 3).map((mailbox, index) => {
      const taskFailed = mailbox.connectionStatus === 'ERROR'
      return {
        id: mailbox.id,
        mailboxName: mailbox.mailboxName,
        status: !mailbox.enabled ? '暂停' : taskFailed ? '失败' : '运行中',
        statusClass: !mailbox.enabled ? 'status-unknown' : taskFailed ? 'status-error' : 'status-ok',
        taskType: taskFailed ? '连接测试' : '拉取新邮件',
        startTime: mailbox.lastFetchAt ? mailbox.lastFetchAt.replace('T', ' ').slice(11, 19) : `10:${String(32 - index).padStart(2, '0')}:00`,
        nextRun: !mailbox.enabled ? '停用' : taskFailed ? '手动重试' : secondsLabel(mailbox.fetchIntervalSec),
        progress: taskFailed ? '认证失败' : `${Math.max(45, 82 - index * 14)}%`,
      }
    })
    const mailboxLogRows = mailboxRecords.slice(0, 3).map((mailbox, index) => {
      const failed = mailbox.connectionStatus === 'ERROR'
      const count = failed ? 0 : Math.max(0, 20 - index * 5)
      return {
        id: mailbox.id,
        time: mailbox.lastFetchAt ? mailbox.lastFetchAt.replace('T', ' ').slice(11, 19) : `10:${String(32 - index).padStart(2, '0')}:20`,
        mailboxName: mailbox.mailboxName,
        action: '拉取邮件',
        result: failed ? '失败' : '成功',
        resultClass: failed ? 'status-error' : 'status-ok',
        count: `${count} 封`,
        relatedTickets: failed ? '连接失败' : count > 0 ? `TCK-${new Date().toISOString().slice(0, 10).replaceAll('-', '')}-${String(index * 20 + 1).padStart(3, '0')} 起` : '无新增',
      }
    })
    const todayReceivedCount = mailboxLogRows.reduce((total, log) => total + Number(log.count.replace(/\D/g, '') || 0), 0)
    const todayTicketCount = Math.max(0, todayReceivedCount - (mailboxesData?.summary.errorMailboxes ?? 0))
    const mailboxRowColors = ['#2563eb', '#f97316', '#10b981', '#8b5cf6', '#ef4444', '#14b8a6']
    const mailboxDailyCount = (mailbox: Mailbox, index: number) => (mailbox.connectionStatus === 'ERROR' ? 0 : Math.max(0, 20 - index * 5))
    const assignmentRecords = assignmentRulesData?.records ?? []
    const assignmentSummary = assignmentRulesData?.summary
    const sortedAssignmentRecords = [...assignmentRecords].sort((a, b) => a.priorityOrder - b.priorityOrder || a.id - b.id)
    const selectedAssignmentRule = assignmentForm.id
      ? assignmentRecords.find((rule) => rule.id === assignmentForm.id) || null
      : null
    const assignmentMailboxOptions = mailboxes.map((mailbox) => ({
      value: String(mailbox.id),
      label: `${mailbox.mailboxName} ${mailbox.emailAddress}`,
    }))
    const assignmentAssigneeOptions = assignmentAssignees.map((agent) => ({
      value: String(agent.id),
      label: `${agent.displayName} / ${agent.email}`,
    }))
    const slaPolicyRecords = slaPoliciesData?.records ?? []
    const slaPolicySummary = slaPoliciesData?.summary
    const selectedSlaPolicy = slaPolicyForm.id
      ? slaPolicyRecords.find((policy) => policy.id === slaPolicyForm.id) || null
      : null
    const selectedWorkCalendar = workCalendars.find((calendar) => String(calendar.id) === slaPolicyForm.calendarId) || null
    const slaCalendarOptions = workCalendars.map((calendar) => ({
      value: String(calendar.id),
      label: `${calendar.calendarName} / ${calendar.timezone} / ${workdayLabel(calendar.workdays)} ${calendar.workStartTime}-${calendar.workEndTime}`,
    }))
    const slaPreview = resolveSlaPreview(slaPolicyForm, selectedWorkCalendar)
    const slaCalendarCount = new Set(slaPolicyRecords.map((policy) => policy.calendarId)).size
    const slaResolveHoursInvalid = Boolean(
      slaPolicyForm.resolveHours.trim()
      && Number(slaPolicyForm.resolveHours) < Number(slaPolicyForm.responseHours),
    )
    const slaWarningInvalid = Number(slaPolicyForm.warningRemainHours) >= Number(slaPolicyForm.responseHours)
    const workCalendarRecords = workCalendarData?.records ?? []
    const workCalendarSummary = workCalendarData?.summary
    const selectedCalendarForPage = workCalendarForm.id
      ? workCalendarRecords.find((calendar) => calendar.id === workCalendarForm.id) || null
      : null
    const holidayRecords = holidaysData?.records ?? []
    const selectedCalendarPolicyCount = selectedCalendarForPage
      ? calendarSlaPolicies.filter((policy) => policy.calendarId === selectedCalendarForPage.id).length
      : 0
    const totalCalendarPolicyCount = calendarSlaPolicies.length
    const selectedCalendarHolidayCount = holidayRecords.filter((holiday) => holiday.calendarId === selectedCalendarForPage?.id).length
    const workCalendarStartMinutes = parseClockMinutes(workCalendarForm.workStartTime, 9 * 60)
    const workCalendarEndMinutes = parseClockMinutes(workCalendarForm.workEndTime, 18 * 60)
    const workCalendarTimeInvalid = workCalendarStartMinutes >= workCalendarEndMinutes
    const workCalendarDeleteBlockedReason = selectedCalendarForPage?.defaultCalendar
      ? '默认日历不可删除'
      : selectedCalendarPolicyCount > 0
        ? `已被 ${selectedCalendarPolicyCount} 条 SLA 策略引用`
        : selectedCalendarHolidayCount > 0
          ? `当前月份已配置 ${selectedCalendarHolidayCount} 个节假日`
          : ''
    const monthCells = buildMonthCells(holidayMonth, selectedCalendarForPage, holidayRecords)
    const calendarPreviewCreatedAtValue = dayjs(calendarPreviewCreatedAt)
    const calendarPreviewResponseHoursValue = Math.max(1, Number(calendarPreviewResponseHours) || 1)
    const calendarPreviewResolveHoursValue = Math.max(1, Number(calendarPreviewResolveHours) || 1)
    const calendarSlaExample = resolveCalendarSlaExample(
      selectedCalendarForPage,
      holidayRecords,
      calendarPreviewCreatedAtValue,
      calendarPreviewResponseHoursValue,
      calendarPreviewResolveHoursValue,
    )
    const holidayDateValue = holidayForm.holidayDate ? dayjs(holidayForm.holidayDate) : null
    const holidayMonthValue = holidayMonth ? dayjs(`${holidayMonth}-01`) : null
    const holidayImportYear = Number(holidayMonth.slice(0, 4)) || dayjs().year()
    const customerRecords = customersData?.records ?? []
    const selectedCustomer = customerDetail || customerRecords.find((customer) => customer.email === selectedCustomerEmail) || null
    const customerRemarkCount = customerRecords.filter((customer) => customer.remark?.trim()).length
    const customerWithTicketCount = customerRecords.filter((customer) => customer.ticketCount > 0).length
    const recentCustomerCount = customerRecords.filter((customer) => (
      customer.lastMailAt ? dayjs().diff(dayjs(customer.lastMailAt), 'day') <= 7 : false
    )).length
    const dashboardCards = [
      {
        key: 'total',
        label: '工单总数',
        value: dashboardSummary?.totalCount,
        help: '当前权限范围内总量',
        icon: Layers,
        tone: 'blue',
        onClick: () => navigateToTickets('ALL'),
      },
      {
        key: 'pending',
        label: '待分配工单',
        value: dashboardSummary?.pendingAssignCount,
        help: '等待分配处理人',
        icon: Clock,
        tone: 'amber',
        onClick: () => navigateToTickets('PENDING_ASSIGN'),
      },
      {
        key: 'processing',
        label: '处理中工单',
        value: dashboardSummary?.processingCount,
        help: '已分配并处理中',
        icon: Folder,
        tone: 'blue',
        onClick: () => navigateToTickets('PROCESSING'),
      },
      {
        key: 'waiting',
        label: '待客户回复',
        value: dashboardSummary?.waitingCustomerCount,
        help: '等待客户补充信息',
        icon: MessageCircle,
        tone: 'green',
        onClick: () => navigateToTickets('WAITING_CUSTOMER'),
      },
      {
        key: 'overdue',
        label: 'SLA 已超时',
        value: dashboardSummary?.slaOverdueCount,
        help: '需要优先处理',
        icon: TriangleAlert,
        tone: 'red',
        onClick: () => navigateToTickets('ALL', true),
      },
      {
        key: 'closed',
        label: '今日已关闭',
        value: dashboardSummary?.closedTodayCount,
        help: '已解决并关闭',
        icon: CircleCheck,
        tone: 'green',
        onClick: () => navigateToTickets('CLOSED'),
      },
    ]
    const dashboardTodoRecords = dashboardTodos?.records ?? []
    const dashboardHasAnyData = Boolean(
      dashboardSummary && (
        dashboardSummary.totalCount > 0
        || dashboardSummary.activeCount > 0
        || dashboardTodoRecords.length > 0
      ),
    )
    const dashboardRiskItems = [
      {
        label: 'SLA 已超时工单',
        detail: '进入全部工单并筛选超时记录',
        value: dashboardSummary?.slaOverdueCount ?? 0,
        tone: 'red',
        icon: TriangleAlert,
        onClick: () => navigateToTickets('ALL', true),
      },
      {
        label: '待分配工单',
        detail: '进入全部工单并筛选待分配',
        value: dashboardSummary?.pendingAssignCount ?? 0,
        tone: 'amber',
        icon: Clock,
        onClick: () => navigateToTickets('PENDING_ASSIGN'),
      },
      {
        label: '待客户回复工单',
        detail: '进入全部工单并筛选待客户回复',
        value: dashboardSummary?.waitingCustomerCount ?? 0,
        tone: 'blue',
        icon: MessageCircle,
        onClick: () => navigateToTickets('WAITING_CUSTOMER'),
      },
    ]
    const dashboardMailEntries = [
      {
        title: '邮箱配置',
        detail: '查看连接状态',
        icon: Settings,
        permission: 'menu:mailboxes',
        onClick: () => setActiveMenu('邮箱配置'),
      },
      {
        title: '收件记录',
        detail: '检查拉取结果',
        icon: Inbox,
        permission: 'menu:mail_fetch_logs',
        onClick: () => setActiveMenu('收件记录'),
      },
      {
        title: '发件记录',
        detail: '处理失败记录',
        icon: Send,
        permission: 'menu:mail_send_logs',
        onClick: () => setActiveMenu('发件记录'),
      },
    ].filter((entry) => hasPermission(entry.permission))

    return (
      <div className={sidebarCollapsed ? 'app-workspace shell-collapsed' : 'app-workspace'}>
        <aside className={sidebarCollapsed ? 'app-sidebar collapsed' : 'app-sidebar'} aria-label="左侧菜单">
          <div className="app-logo">
            <span className="app-logo__mark">
              <Mail size={18} strokeWidth={2.4} />
            </span>
            {!sidebarCollapsed && <strong>邮件工单系统</strong>}
          </div>

          <nav className="sidebar-nav" aria-label="主导航">
            {visibleMenuGroups.map((group) => (
              <section className="sidebar-group" key={group.title}>
                {!sidebarCollapsed && <h2>{group.title}</h2>}
                {group.items.map((item) => {
                  const Icon = item.icon
                  return (
                    <button
                      className={activeMenu === item.title ? 'sidebar-item active' : 'sidebar-item'}
                      key={item.title}
                      onClick={() => {
                        setActiveMenu(item.title)
                        setProfileOpen(false)
                        setNotificationsOpen(false)
                      }}
                      title={sidebarCollapsed ? item.title : undefined}
                      type="button"
                    >
                      <span className="sidebar-item__main">
                        <Icon size={18} strokeWidth={2.2} />
                        {!sidebarCollapsed && <span>{item.title}</span>}
                      </span>
                      {!sidebarCollapsed && (item.badge || (item.title === '发件记录' && sendPendingCount > 0)) && (
                        <span className={item.tone ? `menu-badge ${item.tone}` : 'menu-badge'}>
                          {item.title === '发件记录' ? sendPendingCount : item.badge}
                        </span>
                      )}
                    </button>
                  )
                })}
              </section>
            ))}
          </nav>

          <button
            className="sidebar-collapse"
            onClick={() => setSidebarCollapsed((value) => !value)}
            type="button"
          >
            {sidebarCollapsed ? <PanelLeftOpen size={17} /> : <PanelLeftClose size={17} />}
            {!sidebarCollapsed && <span>收起菜单</span>}
          </button>
        </aside>

        <main className="app-main">
          <header className="app-topbar">
            <div className="breadcrumb">
              <Menu size={18} />
              <span>邮件工单</span>
              <i>/</i>
              <strong>{activeMenu}</strong>
            </div>

            <label className="global-search">
              <Search size={16} />
              <input
                onChange={(event) => setSearchKeyword(event.target.value)}
                placeholder="搜索工单、客户、邮件内容或功能"
                ref={searchInputRef}
                type="search"
                value={searchKeyword}
              />
              <span className="shortcut-key">⌘</span>
              <span className="shortcut-key">K</span>
            </label>

            <div className="topbar-actions">
              <button
                aria-label="帮助"
                className="icon-button"
                onClick={() =>
                  setModal({
                    title: '帮助',
                    text: '帮助文档入口已固定在顶部栏。后续接入操作手册后，这里打开帮助抽屉或文档中心。',
                  })
                }
                title="帮助"
                type="button"
              >
                <CircleHelp size={20} />
              </button>

              <div className="popover-wrap">
                <button
                  aria-expanded={notificationsOpen}
                  aria-label="通知"
                  className="icon-button has-dot"
                  onClick={() => {
                    setNotificationsOpen((value) => !value)
                    setProfileOpen(false)
                  }}
                  title="通知"
                  type="button"
                >
                  <Bell size={20} />
                  <span />
                </button>
                {notificationsOpen && (
                  <div className="topbar-popover notifications-panel">
                    <strong>系统消息</strong>
                    <button type="button">有 2 个工单已超时</button>
                    <button type="button">有 6 个工单即将超时</button>
                    <button type="button">有 3 个客户新回复待处理</button>
                  </div>
                )}
              </div>

              <div className="profile-area">
                <button
                  aria-expanded={profileOpen}
                  className="profile-button"
                  onClick={() => {
                    setProfileOpen((value) => !value)
                    setNotificationsOpen(false)
                  }}
                  type="button"
                >
                  <span className="profile-avatar">{userInitial}</span>
                  <span className="profile-text">
                    <strong>{user.displayName}</strong>
                    <small>{roleLabel(user.roleCode)}</small>
                  </span>
                  <ChevronDown size={15} />
                </button>
                {profileOpen && (
                  <div className="topbar-popover profile-menu">
                    <div className="profile-summary">
                      <span className="profile-avatar">{userInitial}</span>
                      <div>
                        <strong>{user.displayName}</strong>
                        <small>{user.email}</small>
                      </div>
                    </div>
                    <button type="button">
                      <UserRound size={16} />
                      个人信息
                    </button>
                    <button type="button" onClick={handleLogout}>
                      <LogOut size={16} />
                      退出登录
                    </button>
                  </div>
                )}
              </div>
            </div>
          </header>

          {activeMenu === '工作台' ? (
            <section className="app-content dashboard-page" aria-label="工作台">
              <div className="content-title dashboard-title">
                <div>
                  <h1>工作台</h1>
                  <p>查看工单处理状态、SLA 风险以及邮件运行入口</p>
                </div>
                <div className="content-actions">
                  <button type="button" disabled>
                    <Clock size={16} />
                    {dashboardUpdatedAt ? `统计截至 ${dashboardUpdatedAt}` : '统计截至 -'}
                  </button>
                  <button disabled={dashboardLoading} onClick={() => void fetchDashboard()} type="button">
                    {dashboardLoading ? <Loader size={16} className="spin-icon" /> : <RefreshCw size={16} />}
                    刷新数据
                  </button>
                </div>
              </div>

              {dashboardError && !dashboardSummary && !dashboardTodos ? (
                <div className="permission-state dashboard-error-state">
                  {dashboardError.includes('仅管理员') || dashboardError.includes('权限') ? (
                    <>
                      <LockKeyhole size={36} />
                      <strong>无工作台查看权限</strong>
                      <p>请联系管理员开通工作台或相关数据权限。</p>
                    </>
                  ) : (
                    <>
                      <TriangleAlert size={36} />
                      <strong>工作台统计加载失败</strong>
                      <p>{dashboardError}</p>
                      <button onClick={() => void fetchDashboard()} type="button">重试</button>
                    </>
                  )}
                </div>
              ) : (
                <>
                  {dashboardError && (
                    <Alert
                      message="部分工作台数据加载失败"
                      description={dashboardError}
                      type="error"
                      showIcon
                      action={<Button size="small" danger onClick={() => void fetchDashboard()}>重试</Button>}
                      style={{ marginBottom: 16 }}
                    />
                  )}

                  <div className="dashboard-metrics">
                    {dashboardCards.map((card) => {
                      const Icon = card.icon
                      return (
                        <button className="dashboard-metric" key={card.key} onClick={card.onClick} type="button">
                          <span className="dashboard-metric__label">{card.label}</span>
                          <strong>{dashboardLoading && dashboardSummary == null ? '--' : card.value ?? 0}</strong>
                          <small>{card.help}</small>
                          <i className={`dashboard-metric__icon ${card.tone}`}>
                            <Icon size={19} />
                          </i>
                        </button>
                      )
                    })}
                  </div>

                  {dashboardLoading && !dashboardSummary && !dashboardTodos ? (
                    <div className="dashboard-grid">
                      <div className="dashboard-panel dashboard-skeleton"><span /><span /><span /></div>
                      <div className="dashboard-panel dashboard-skeleton"><span /><span /><span /></div>
                      <div className="dashboard-panel dashboard-skeleton"><span /><span /><span /></div>
                    </div>
                  ) : !dashboardHasAnyData ? (
                    <div className="empty-state dashboard-empty">
                      <CircleCheck size={36} />
                      <strong>暂无工作台数据</strong>
                      <p>当前权限范围内没有工单或待办记录。可以刷新数据，或从全部工单查看历史记录。</p>
                      <button onClick={() => void fetchDashboard()} type="button">刷新</button>
                    </div>
                  ) : (
                    <>
                      <div className="dashboard-grid">
                        <section className="dashboard-panel">
                          <div className="dashboard-panel__head">
                            <div>
                              <h2>SLA 风险摘要</h2>
                              <p>第一版展示已超时风险和活跃工单</p>
                            </div>
                            <Tag color={(dashboardSummary?.slaOverdueCount ?? 0) > 0 ? 'red' : 'green'}>
                              {(dashboardSummary?.slaOverdueCount ?? 0) > 0 ? `${dashboardSummary?.slaOverdueCount} 个超时` : '暂无超时'}
                            </Tag>
                          </div>
                          <button className="dashboard-risk-card red" onClick={() => navigateToTickets('ALL', true)} type="button">
                            <span>
                              <strong>SLA 已超时</strong>
                              <small>进入全部工单并筛选超时记录</small>
                            </span>
                            <b>{dashboardSummary?.slaOverdueCount ?? 0}</b>
                          </button>
                          <div className="dashboard-mini-grid">
                            <div><small>今日已关闭</small><strong>{dashboardSummary?.closedTodayCount ?? 0}</strong></div>
                            <div><small>活跃工单</small><strong>{dashboardSummary?.activeCount ?? 0}</strong></div>
                          </div>
                        </section>

                        {dashboardMailEntries.length > 0 && (
                          <section className="dashboard-panel">
                            <div className="dashboard-panel__head">
                              <div>
                                <h2>邮箱运行入口</h2>
                                <p>快捷进入当前账号可访问的邮件管理页面</p>
                              </div>
                              <Tag color="green">入口</Tag>
                            </div>
                            <div className="dashboard-entry-grid">
                              {dashboardMailEntries.map((entry) => {
                                const Icon = entry.icon
                                return (
                                  <button key={entry.title} onClick={entry.onClick} type="button">
                                    <Icon size={18} /><span>{entry.title}</span><small>{entry.detail}</small>
                                  </button>
                                )
                              })}
                            </div>
                          </section>
                        )}

                        <section className="dashboard-panel">
                          <div className="dashboard-panel__head">
                            <div>
                              <h2>风险入口</h2>
                              <p>通过已有工单列表筛选处理</p>
                            </div>
                            <Tag color="blue">第一版跳转</Tag>
                          </div>
                          <div className="dashboard-risk-list">
                            {dashboardRiskItems.map((item) => {
                              const Icon = item.icon
                              return (
                                <button key={item.label} onClick={item.onClick} type="button">
                                  <i className={item.tone}><Icon size={17} /></i>
                                  <span><strong>{item.label}</strong><small>{item.detail}</small></span>
                                  <b>{item.value}</b>
                                </button>
                              )
                            })}
                          </div>
                        </section>
                      </div>

                      <div className="dashboard-lower-grid">
                        <section className="dashboard-panel dashboard-todos">
                          <div className="dashboard-panel__head">
                            <div>
                              <h2>我的待办 <span>({dashboardTodos?.totalCount ?? 0})</span></h2>
                              <p>按当前登录处理人过滤，SLA 近的优先</p>
                            </div>
                            <button className="link-button" onClick={() => navigateToTickets('ALL')} type="button">全部</button>
                          </div>
                          {dashboardTodoRecords.length === 0 ? (
                            <div className="dashboard-inline-empty">
                              <CircleCheck size={28} />
                              <strong>暂无待办工单</strong>
                              <small>当前没有需要你处理的工单。</small>
                            </div>
                          ) : (
                            <div className="dashboard-todo-list">
                              {dashboardTodoRecords.map((ticket) => {
                                const statusText = ticket.status === 'PENDING_ASSIGN' ? '待分配' : statusLabel(ticket.status)
                                return (
                                  <button key={ticket.id} onClick={() => { void handleOpenDetail(ticket.id); setActiveMenu('全部工单') }} type="button">
                                    <div className="dashboard-todo__top">
                                      <span className={`priority-pill ${priorityBadgeClass(ticket.priority)}`}>{priorityBadgeText(ticket.priority)}</span>
                                      <small>{relativeTime(ticket.createdAt)}</small>
                                    </div>
                                    <strong>{ticket.ticketNo}</strong>
                                    <span>{ticket.subject}</span>
                                    <small>客户：{ticket.customerEmail}</small>
                                    <div className="dashboard-todo__foot">
                                      <Tag color={ticket.status === 'WAITING_CUSTOMER' ? 'green' : ticket.status === 'PROCESSING' ? 'blue' : 'orange'}>
                                        {statusText}
                                      </Tag>
                                      <small className={ticket.slaBreached ? 'danger-text' : ''}>
                                        SLA：{ticket.slaBreached ? '已超时' : formatOptionalDateTime(ticket.slaResponseDeadline)}
                                      </small>
                                    </div>
                                  </button>
                                )
                              })}
                            </div>
                          )}
                        </section>

                        <section className="dashboard-panel">
                          <div className="dashboard-panel__head">
                            <div>
                              <h2>处理中工单入口</h2>
                              <p>从全部工单筛选处理中状态</p>
                            </div>
                            <button className="link-button" onClick={() => navigateToTickets('PROCESSING')} type="button">全部</button>
                          </div>
                          <button className="dashboard-processing-card" onClick={() => navigateToTickets('PROCESSING')} type="button">
                            <span><strong>处理中工单</strong><small>进入全部工单并筛选处理状态</small></span>
                            <b>{dashboardSummary?.processingCount ?? 0}</b>
                          </button>
                          <div className="dashboard-mini-grid">
                            <div><small>SLA 已超时</small><strong className="danger-text">{dashboardSummary?.slaOverdueCount ?? 0}</strong></div>
                            <div><small>今日已关闭</small><strong className="success-text">{dashboardSummary?.closedTodayCount ?? 0}</strong></div>
                          </div>
                        </section>

                        <section className="dashboard-panel">
                          <div className="dashboard-panel__head">
                            <div>
                              <h2>我的待办摘要</h2>
                              <p>按当前处理人统计</p>
                            </div>
                          </div>
                          <div className="dashboard-summary-grid">
                            <div><small>我的待办</small><strong>{dashboardTodos?.totalCount ?? 0}</strong></div>
                            <div className="red"><small>SLA 已超时</small><strong>{dashboardTodos?.slaOverdueCount ?? 0}</strong></div>
                          </div>
                          <div className="dashboard-risk-list compact">
                            <button onClick={() => navigateToTickets('PROCESSING')} type="button">
                              <i className="blue" /><span><strong>处理中</strong></span><b>{dashboardTodos?.processingCount ?? 0}</b>
                            </button>
                            <button onClick={() => navigateToTickets('WAITING_CUSTOMER')} type="button">
                              <i className="green" /><span><strong>待客户回复</strong></span><b>{dashboardTodos?.waitingCustomerCount ?? 0}</b>
                            </button>
                          </div>
                        </section>
                      </div>

                      <section className="dashboard-panel dashboard-activity">
                        <div className="dashboard-panel__head">
                          <div>
                            <h2>最近活动</h2>
                            <p>后续由操作日志沉淀活动流；第一版先保留入口，不阻断工作台上线。</p>
                          </div>
                          <Tag color="orange">后续扩展</Tag>
                        </div>
                        <div className="dashboard-extension-grid">
                          <div><Mail size={18} /><strong>邮件事件</strong><small>收到客户邮件、发送失败、自动回复等后续合并展示。</small></div>
                          <div><Folder size={18} /><strong>工单事件</strong><small>创建、分配、打开、关闭等关键动作后续排序展示。</small></div>
                          <div><Timer size={18} /><strong>SLA 事件</strong><small>预警、超时、恢复等事件后续与提醒任务统一展示。</small></div>
                        </div>
                      </section>
                    </>
                  )}
                </>
              )}
            </section>
          ) : activeMenu === '全部工单' ? (
            showTicketDetailPage && ticketDetail ? (
              /* ===== 全屏工单详情页 (PG-15) ===== */
              <div className="ticket-detail-page">
                {/* 返回标题栏 */}
                <div className="detail-topbar">
                  <Button type="text" icon={<ArrowLeftOutlined />} onClick={handleBackToList} className="detail-back-btn">
                    返回列表
                  </Button>
                  <h2>工单详情</h2>
                </div>

                <div className="detail-body">
                  {/* === 左侧主内容 === */}
                  <div className="detail-main">
                    {/* 工单头部 */}
                    <div className="detail-header-card">
                      <div className="detail-header-top">
                        <div className="detail-header-left">
                          <span className={`priority-pill ${priorityBadgeClass(ticketDetail.priority)}`}>
                            {priorityBadgeText(ticketDetail.priority)}
                          </span>
                          <span style={{ fontSize: 14, fontWeight: 500, color: ticketDetail.priority === 'URGENT' ? '#dc2626' : ticketDetail.priority === 'HIGH' ? '#d97706' : '#6b7280', marginRight: 8 }}>
                            {priorityLabel(ticketDetail.priority)}
                          </span>
                          <StarTwoTone twoToneColor="#f59e0b" style={{ fontSize: 16 }} />
                          <span className="detail-ticket-no">{ticketDetail.ticketNo}</span>
                        </div>
                        <div className="detail-header-actions">
                          {canClaimCurrentTicket && (
                            <Button
                              type="primary"
                              size="small"
                              icon={<UserPlus size={14} />}
                              loading={claimSending}
                              onClick={() => void handleClaimTicket()}
                            >
                              领取工单
                            </Button>
                          )}
                          <Button
                            size="small"
                            icon={<SwapOutlined />}
                            disabled={!canOperateCurrentTicket}
                            onClick={() => {
                              setAssignUserId(ticketDetail.assigneeId)
                              setAssignReason('')
                              setAssignNotifyAssignee(true)
                              setAssignModalOpen(true)
                              void fetchAgentUsers()
                            }}
                          >
                            转派
                          </Button>
                          <Button size="small" icon={<FlagOutlined />}
                            disabled={!canOperateCurrentTicket}
                            onClick={() => {
                              setPriorityValue(ticketDetail.priority)
                              setPriorityReason('')
                              setPriorityModalOpen(true)
                            }}>修改优先级</Button>
                          <Button
                            size="small"
                            disabled={!canOperateCurrentTicket}
                            onClick={() => {
                              setStatusValue(ticketDetail.status === 'PROCESSING' ? 'CANCELLED' : 'PROCESSING')
                              setStatusReason('')
                              setStatusModalOpen(true)
                            }}
                          >
                            修改状态
                          </Button>
                          <Button
                            size="small"
                            icon={<CloseCircleOutlined />}
                            disabled={!canOperateCurrentTicket}
                            onClick={() => {
                              setCloseReason('')
                              setCloseConfirmed(false)
                              setCloseModalOpen(true)
                            }}
                          >
                            关闭工单
                          </Button>
                          <Button size="small" icon={<EllipsisOutlined />} disabled={!canOperateCurrentTicket}>更多</Button>
                        </div>
                      </div>
                      <h1 className="detail-subject">{ticketDetail.subject}</h1>
                      <div className="detail-meta">
                        <span>来源：<b>{ticketDetail.mailboxName || '客户邮件'}</b></span>
                        <span>创建时间：<b>{dayjs(ticketDetail.createdAt).format('YYYY-MM-DD HH:mm')}</b></span>
                        <span>更新时间：<b>{dayjs(ticketDetail.updatedAt).format('YYYY-MM-DD HH:mm')}</b></span>
                        <Tag color={ticketDetail.slaBreached ? 'red' : ticketDetail.status === 'CLOSED' ? 'default' : 'blue'}>
                          {statusLabel(ticketDetail.status)}
                        </Tag>
                        {ticketDetail.linkSuspect && <Tag color="warning" style={{ marginLeft: 4 }}>疑似断链</Tag>}
                      </div>
                    </div>

                    {/* Tabs + 内容区 */}
                    <div className="detail-content-card">
                      <Tabs
                        activeKey={ticketDetailTab}
                        onChange={setTicketDetailTab}
                        items={[
                          {
                            key: 'mail',
                            label: '邮件会话',
                            children: (
                              <div className="detail-mail-conversation">
                                {/* 消息筛选栏 */}
                                {ticketDetail.messages && ticketDetail.messages.length > 0 && (
                                  <div className="msg-filter-bar">
                                    <div className="msg-filter-left">
                                      <Segmented
                                        size="small"
                                        value={msgFilter}
                                        onChange={(val) => setMsgFilter(val as string)}
                                        options={(() => {
                                          const msgs = ticketDetail.messages
                                          const total = msgs.filter((m: any) => (m.direction || m.messageDirection) !== 'INTERNAL').length
                                          const inbound = msgs.filter((m: any) => (m.direction || m.messageDirection) === 'INBOUND').length
                                          const outbound = msgs.filter((m: any) => (m.direction || m.messageDirection) === 'OUTBOUND').length
                                          return [
                                            { label: `全部邮件 (${total})`, value: 'ALL' },
                                            { label: `客户 (${inbound})`, value: 'INBOUND' },
                                            { label: `客服 (${outbound})`, value: 'OUTBOUND' },
                                          ]
                                        })()}
                                      />
                                    </div>
                                    <div className="msg-filter-right" onClick={() => setMsgSortAsc(!msgSortAsc)}>
                                      <span>排序：</span>
                                      <span style={{ fontWeight: 500, color: '#1f2937', cursor: 'pointer' }}>
                                        {msgSortAsc ? '时间升序' : '时间降序'}
                                        <span style={{ marginLeft: 4, fontSize: 11, color: '#9ca3af' }}>
                                          {msgSortAsc ? '↑' : '↓'}
                                        </span>
                                      </span>
                                    </div>
                                  </div>
                                )}

                                {/* 邮件消息列表 */}
                                {(!ticketDetail.messages || ticketDetail.messages.length === 0) ? (
                                  <Empty description="暂无邮件消息" style={{ padding: '40px 0' }} />
                                ) : (
                                  [...ticketDetail.messages]
                                    .filter((msg: any) => {
                                      const dir = msg.direction || msg.messageDirection
                                      return msgFilter === 'ALL' ? dir !== 'INTERNAL' : dir === msgFilter
                                    })
                                    .sort((a: any, b: any) => {
                                      const ta = a.sentAt || a.createdAt
                                      const tb = b.sentAt || b.createdAt
                                      if (!ta || !tb) return 0
                                      return msgSortAsc
                                        ? new Date(ta).getTime() - new Date(tb).getTime()
                                        : new Date(tb).getTime() - new Date(ta).getTime()
                                    })
                                    .map((msg: any) => {
                                      const dir = msg.direction || msg.messageDirection
                                      const isAgent = dir === 'OUTBOUND'
                                      const isAuto = false // auto-reply messages not yet persisted as ticket messages
                                      const displayName = msg.displayName || (msg.fromAddress ? msg.fromAddress.split('@')[0] : '')
                                      const firstChar = displayName ? displayName[0].toUpperCase() : isAuto ? 'S' : 'C'
                                      return (
                                        <div key={msg.id} className={`msg-card ${isAuto ? 'msg-system' : isAgent ? 'msg-agent' : 'msg-customer'}`}>
                                          <div className="msg-avatar">
                                            <div className={`msg-avatar-circle ${isAuto ? 'avatar-system' : isAgent ? 'avatar-agent' : 'avatar-customer'}`}>
                                              {firstChar}
                                            </div>
                                          </div>
                                          <div className="msg-content">
                                            <div className="msg-header">
                                              <div className="msg-header-left">
                                                <span className="msg-from">{msg.fromAddress || '系统'}</span>
                                                <span className={`msg-badge ${isAuto ? 'badge-system' : isAgent ? 'badge-agent' : 'badge-customer'}`}>
                                                  {isAuto ? '系统' : isAgent ? '客服' : '客户'}
                                                </span>
                                              </div>
                                              <span className="msg-time">{msg.sentAt ? dayjs(msg.sentAt).format('YYYY-MM-DD HH:mm') : ''}</span>
                                            </div>
                                            {msg.toAddress && (
                                              <div className="msg-to">收件人：{msg.toAddress}</div>
                                            )}
                                        <div className="msg-body">
                                          {msgBodyText(msg)}
                                        </div>
                                        {msg.id && (() => {
                                          const msgAtts = ticketAttachments.filter(a => a.messageId === msg.id)
                                          if (msgAtts.length === 0) return null
                                          return (
                                            <div className="msg-attachments">
                                              {msgAtts.map((att) => (
                                                <div key={att.id} className="msg-attachment-item">
                                                  <span className="msg-attachment-icon">📎</span>
                                                  <a href={att.downloadUrl || undefined} target="_blank" rel="noopener noreferrer"
                                                    className="msg-attachment-link">{att.fileName}</a>
                                                  <span className="msg-attachment-size">({formatFileSize(att.fileSize)})</span>
                                                </div>
                                              ))}
                                            </div>
                                          )
                                        })()}
                                          </div>
                                        </div>
                                      )
                                    })
                                )}

                                {/* 回复编辑器 */}
                                <div className="detail-editor">
                                  <div style={{ fontSize: 13, fontWeight: 600, color: '#1f2937', marginBottom: 8 }}>回复客户</div>
                                  {!canOperateCurrentTicket && !isCurrentTicketTerminal && (
                                    <Alert
                                      type="info"
                                      showIcon
                                      message={isCurrentTicketUnassigned ? '领取后即可处理该工单' : '当前账号不可操作该工单'}
                                      style={{ marginBottom: 10 }}
                                    />
                                  )}
                                  <TiptapRichEditor
                                    placeholder="请输入回复内容（将发送邮件给客户）..."
                                    disabled={!canOperateCurrentTicket}
                                    onUpdate={(html, text) => { setReplyHtml(html); setReplyContent(text) }}
                                  />
                                  {uploadedFiles.length > 0 && (
                                    <div style={{ margin: '6px 0', display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                                      {uploadedFiles.map(f => (
                                        <Tag key={f.objectKey} closable onClose={() => handleRemoveFile(f.objectKey)}
                                          style={{ fontSize: 12, margin: 0 }}>
                                          📎 {f.fileName} ({formatFileSize(f.fileSize)})
                                        </Tag>
                                      ))}
                                    </div>
                                  )}
                                  <div className="detail-editor-actions">
                                    <div>
                                        <input type="file" ref={fileInputRef} style={{ display: 'none' }}
                                          onChange={(e) => { const f = e.target.files?.[0]; if (f) { void handleUploadFile(f); e.target.value = '' } }} />
                                      <Button type="text" icon={<PaperClipOutlined />}
                                        loading={uploadingFile}
                                        disabled={!canOperateCurrentTicket}
                                        onClick={() => fileInputRef.current?.click()}>添加附件</Button>
                                      <Button type="text" icon={<FileTextOutlined />} disabled>插入模板</Button>
                                    </div>
                                    <Space>
                                      <Button disabled>保存草稿</Button>
                                      <Button type="primary" icon={<SendOutlined />} onClick={() => void handleReply()}
                                        loading={replySending} disabled={!canOperateCurrentTicket || (!replyContent.trim() && !replyHtml.trim())}>
                                        发送邮件
                                      </Button>
                                    </Space>
                                  </div>
                                </div>
                              </div>
                            ),
                          },
                          {
                            key: 'log',
                            label: '工单日志',
                            children: getVisibleTicketEvents(ticketDetail.events).length > 0 ? (
                              <Timeline
                                items={getVisibleTicketEvents(ticketDetail.events).map(ev => ({
                                  color: 'blue',
                                  children: (
                                    <div>
                                      <div style={{ fontWeight: 500, color: '#1f2937' }}>{ev.eventContent}</div>
                                      <div style={{ fontSize: 12, color: '#9ca3af' }}>{ev.operator} · {dayjs(ev.eventAt).format('YYYY-MM-DD HH:mm')}</div>
                                    </div>
                                  ),
                                }))}
                              />
                            ) : (
                              <Empty description="暂无工单日志" style={{ padding: '40px 0' }} />
                            ),
                          },
                          {
                            key: 'customer',
                            label: '客户信息',
                            children: (
                              <Descriptions column={1} size="small" style={{ padding: '16px 0' }}>
                                <Descriptions.Item label="客户邮箱">{ticketDetail.customerEmail}</Descriptions.Item>
                                <Descriptions.Item label="来源邮箱">{ticketDetail.mailboxName || `#${ticketDetail.mailboxId}`}</Descriptions.Item>
                              </Descriptions>
                            ),
                          },
                          {
                            key: 'sla',
                            label: 'SLA',
                            children: (
                              <Descriptions column={1} size="small" style={{ padding: '16px 0' }}>
                                <Descriptions.Item label="SLA状态">
                                  <Tag color={ticketDetail.slaBreached ? 'red' : 'green'}>
                                    {ticketDetail.slaBreached ? '已超时' : '正常'}
                                  </Tag>
                                </Descriptions.Item>
                                <Descriptions.Item label="首次响应截止">
                                  {ticketDetail.slaResponseDeadline ? dayjs(ticketDetail.slaResponseDeadline).format('YYYY-MM-DD HH:mm') : '-'}
                                </Descriptions.Item>
                                <Descriptions.Item label="解决截止">
                                  {ticketDetail.slaResolveDeadline ? dayjs(ticketDetail.slaResolveDeadline).format('YYYY-MM-DD HH:mm') : '-'}
                                </Descriptions.Item>
                              </Descriptions>
                            ),
                          },
                          {
                            key: 'attachment',
                            label: `附件 (${ticketAttachments.length})`,
                            children: (
                              <div className="detail-attachments">
                                {ticketAttachments.length === 0 ? (
                                  <Empty description="暂无附件" style={{ padding: '40px 0' }} />
                                ) : (
                                  <div className="attachment-grid">
                                    {ticketAttachments.map((att) => (
                                      <div key={att.id} className="attachment-card">
                                        <div className="attachment-icon">
                                          {att.contentType?.startsWith('image/') ? '🖼' : '📄'}
                                        </div>
                                        <div className="attachment-info">
                                          <div className="attachment-name" title={att.fileName}>{att.fileName}</div>
                                          <div className="attachment-meta">
                                            {formatFileSize(att.fileSize)}
                                            {att.contentType && <span> · {att.contentType}</span>}
                                          </div>
                                        </div>
                                        <div className="attachment-actions">
                                          <Button type="link" size="small" icon={<DownloadOutlined />}
                                            href={att.downloadUrl || undefined} target="_blank" />
                                          <Button type="link" size="small" danger icon={<DeleteOutlined />}
                                            disabled={!canOperateCurrentTicket}
                                            onClick={() => { if (window.confirm('确认删除此附件？')) void handleDeleteAttachment(att.id) }} />
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            ),
                          },
                        ]}
                      />
                    </div>
                  </div>

                  {/* === 右侧信息面板 === */}
                  <aside className="detail-sidebar">
                    {/* 工单信息 */}
                    <Card size="small" title="工单信息">
                      <Descriptions column={1} size="small">
                        <Descriptions.Item label="负责人">
                          <Space>
                            {ticketDetail.assigneeName ? <Avatar size="small" style={{ backgroundColor: '#10b981' }}>{ticketDetail.assigneeName[0]}</Avatar> : null}
                            {ticketDetail.assigneeName || '未分配'}
                          </Space>
                        </Descriptions.Item>
                        <Descriptions.Item label="客户">{ticketDetail.customerEmail}</Descriptions.Item>
                        <Descriptions.Item label="优先级">{priorityLabel(ticketDetail.priority)}</Descriptions.Item>
                        <Descriptions.Item label="状态">{statusLabel(ticketDetail.status)}</Descriptions.Item>
                        <Descriptions.Item label="来源">{ticketDetail.mailboxName || '客户邮件'}</Descriptions.Item>
                        <Descriptions.Item label="备注">
                          <Input.TextArea rows={2} size="small" value={remarkDraft}
                            onChange={e => setRemarkDraft(e.target.value)}
                            disabled={!canOperateCurrentTicket}
                            placeholder="点击添加备注..." style={{ fontSize: 12 }}
                            onBlur={(e) => {
                              if (!canOperateCurrentTicket) return
                              const val = e.target.value.trim()
                              if (val !== (ticketDetail.remark || '')) {
                                requestApi(`/api/v1/tickets/${ticketDetail.id}/remark`, {
                                  method: 'PATCH',
                                  headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
                                  body: JSON.stringify({ remark: val }),
                                }).then(() => { message.success('备注已保存'); void reloadTicketDetail() })
                                  .catch((e: any) => message.error(e?.message || '备注保存失败'))
                              }
                            }}
                          />
                        </Descriptions.Item>
                      </Descriptions>
                    </Card>

                    {/* SLA 信息 */}
                    <Card size="small" title="SLA 信息" extra={
                      <Tag color={ticketDetail.slaBreached ? 'red' : 'green'}>
                        {ticketDetail.slaBreached ? '已超时' : '正常'}
                      </Tag>
                    }>
                      <Descriptions column={1} size="small">
                        <Descriptions.Item label="首次响应截止">
                          {ticketDetail.slaResponseDeadline ? dayjs(ticketDetail.slaResponseDeadline).format('YYYY-MM-DD HH:mm') : '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="解决截止">
                          {ticketDetail.slaResolveDeadline ? dayjs(ticketDetail.slaResolveDeadline).format('YYYY-MM-DD HH:mm') : '-'}
                        </Descriptions.Item>
                      </Descriptions>
                    </Card>

                    {/* 工单生命周期 */}
                    <Card size="small" title="工单生命周期">
                      {getVisibleTicketEvents(ticketDetail.events).length > 0 ? (
                        <Timeline
                          items={getVisibleTicketEvents(ticketDetail.events).map(ev => ({
                            color: 'blue',
                            children: (
                              <div>
                                <div style={{ fontWeight: 500, color: '#1f2937', fontSize: 13 }}>{ev.eventContent}</div>
                                <div style={{ fontSize: 11, color: '#9ca3af' }}>{dayjs(ev.eventAt).format('YYYY-MM-DD HH:mm')}</div>
                              </div>
                            ),
                          }))}
                        />
                      ) : (
                        <Empty description="暂无记录" />
                      )}
                    </Card>
                  </aside>
                </div>
              </div>
            ) : (
              /* ===== 工单列表页 (PG-14) ===== */
              <div className="tickets-page">
                {/* 标题 */}
                <div className="tickets-header">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <h2>全部工单</h2>
                      <div className="tickets-header-sub">
                        当前筛选共 {ticketsData?.total ?? ticketStats?.totalCount ?? 0} 个工单，统计卡片展示当前权限范围口径
                        {!isAdmin ? '；当前仅显示自己负责和未分配工单' : ''}
                      </div>
                    </div>
                    <div className="tickets-header-actions">
                      <Button icon={<ReloadOutlined />} onClick={() => { void fetchTickets(); void fetchTicketStats() }} loading={ticketsLoading}>刷新</Button>
                      <Button>导出</Button>
                    </div>
                  </div>
                  {/* 统计标签栏 — 纯数据展示，不做交互 */}
                  <div className="tickets-stat-tabs">
                    {[
                      { label: '全部工单', val: ticketStats?.totalCount ?? ticketsData?.total ?? 0, Icon: Layers, iconCls: 'total' },
                      { label: '待分配', val: ticketStats?.pendingAssignCount ?? '-', Icon: UserPlus, iconCls: 'pending' },
                      { label: '处理中', val: ticketStats?.processingCount ?? '-', Icon: Loader, iconCls: 'processing' },
                      { label: '待客户回复', val: ticketStats?.waitingCustomerCount ?? '-', Icon: MessageCircle, iconCls: 'waiting' },
                      { label: '已超时', val: ticketStats?.slaOverdueCount ?? '-', Icon: TriangleAlert, iconCls: 'sla-overdue' },
                      { label: '今日已关闭', val: ticketStats?.closedTodayCount ?? '-', Icon: CircleCheck, iconCls: 'closed' },
                    ].map(tab => {
                      const { Icon, iconCls } = tab
                      return (
                        <div key={tab.label} className="tickets-stat-tab">
                          <div className="stat-tab-info">
                            <div className="label">{tab.label}</div>
                            <div className="value">{tab.val}</div>
                          </div>
                          <div className={`stat-tab-icon ${iconCls}`}>
                            <Icon size={16} />
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>

                {/* 列表内容 */}
                <div className="tickets-body">
                  {/* === 左侧筛选 === */}
                  <div className="tickets-filter">
                    <Input.Search placeholder="搜索工单号、主题、客户..." allowClear size="small"
                      value={ticketKeyword} onChange={e => { setTicketKeyword(e.target.value); setTicketPage(1) }}
                      onSearch={() => { setTicketPage(1); void fetchTickets() }} style={{ marginBottom: 16 }} />

                    <div style={{ marginBottom: 20 }}>
                      <div className="tickets-filter-title">我的视图</div>
                      {[
                        { icon: '★', label: '我的待处理', count: ticketStats?.processingCount ?? 0, color: '#f59e0b' },
                        { icon: '◎', label: '我关注的', count: '-', color: '#9ca3af' },
                        { icon: '◷', label: '最近更新', count: '-', color: '#9ca3af' },
                      ].map(item => (
                        <div key={item.label} className="tickets-filter-item">
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <span style={{ color: item.color, fontSize: 12 }}>{item.icon}</span>
                            <span style={{ fontWeight: item.label === '我的待处理' ? 500 : 400 }}>{item.label}</span>
                          </div>
                          <span className="count">{item.count}</span>
                        </div>
                      ))}
                    </div>

                    <div style={{ marginBottom: 20 }}>
                      <div className="tickets-filter-title">工单状态</div>
                      {[{ k: 'ALL', v: '全部' }, { k: 'PENDING_ASSIGN', v: '待分配' }, { k: 'PROCESSING', v: '处理中' }, { k: 'WAITING_CUSTOMER', v: '待客户回复' }, { k: 'CLOSED', v: '已关闭' }].map(item => {
                        const isStatusActive = ticketStatusTab === item.k && !ticketSlaBreachedOnly
                        return (
                          <div key={item.k} className={`tickets-filter-item ${isStatusActive ? 'active' : ''}`}
                            onClick={() => { setTicketStatusTab(item.k); setTicketSlaBreachedOnly(false); setTicketPage(1) }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                              <span style={{
                                width: 14, height: 14, borderRadius: 3,
                                border: isStatusActive ? '2px solid #2563eb' : '2px solid #d1d5db',
                                background: isStatusActive ? '#2563eb' : 'transparent',
                                display: 'inline-block'
                              }} />
                              <span>{item.v}</span>
                            </div>
                            <span className="count">{item.k === 'ALL' ? ticketsData?.total ?? '-' : ticketsData?.records?.filter(r => r.status === item.k).length ?? '-'}</span>
                          </div>
                        )
                      })}
                    </div>

                    <div style={{ marginBottom: 20 }}>
                      <div className="tickets-filter-title">优先级</div>
                      {[
                        { k: 'URGENT', v: '紧急', cls: 'p1', n: 'P1' },
                        { k: 'HIGH', v: '高', cls: 'p2', n: 'P2' },
                        { k: 'NORMAL', v: '普通', cls: 'p3', n: 'P3' },
                      ].map(p => (
                        <div key={p.k} className="tickets-filter-item">
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <span style={{ width: 14, height: 14, borderRadius: 3, border: '2px solid #d1d5db', display: 'inline-block' }} />
                            <span className={`priority-pill ${p.cls}`}>{p.n}</span>
                            <span>{p.v}</span>
                          </div>
                          <span className="count">{ticketsData?.records?.filter(r => r.priority === p.k).length ?? '-'}</span>
                        </div>
                      ))}
                    </div>

                    <div style={{ marginBottom: 20 }}>
                      <div className="tickets-filter-title">SLA状态</div>
                      {[
                        { v: '即将超时', color: '#f59e0b', count: '-', active: false, onClick: undefined },
                        { v: '已超时', color: '#ef4444', count: ticketStats?.slaOverdueCount ?? '-', active: ticketSlaBreachedOnly, onClick: () => navigateToTickets('ALL', true) },
                        { v: '正常', color: '#10b981', count: '-', active: false, onClick: undefined },
                      ].map(item => (
                        <div key={item.v} className={`tickets-filter-item ${item.active ? 'active' : ''}`} onClick={item.onClick}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                            <span style={{ width: 8, height: 8, borderRadius: '50%', background: item.color, display: 'inline-block' }} />
                            <span>{item.v}</span>
                          </div>
                          <span className="count">{item.count}</span>
                        </div>
                      ))}
                    </div>

                    <div style={{ marginBottom: 20 }}>
                      <div className="tickets-filter-title">负责人</div>
                      <div style={{ position: 'relative', marginBottom: 8 }}>
                        <Input.Search placeholder="搜索负责人" size="small" />
                      </div>
                    </div>

                    {ticketsError && <Alert message={ticketsError} type="error" showIcon style={{ marginBottom: 12 }} />}
                    <Button block size="small" onClick={() => { setTicketKeyword(''); setTicketStatusTab('ALL'); setTicketSlaBreachedOnly(false); setTicketPage(1) }}>清空筛选</Button>
                  </div>

                  {/* === 中间工单列表 === */}
                  <div className="tickets-list-panel">
                    <div className="tickets-list-head">
                      <div className="sort-label">
                        <span>排序：</span>
                        <Select size="small" variant="borderless" defaultValue="newest" style={{ width: 110 }}
                          options={[{ value: 'newest', label: '最新更新' }, { value: 'priority', label: '优先级' }, { value: 'created', label: '创建时间' }]} />
                      </div>
                      <div>{ticketsData && <span style={{ fontSize: 12, color: '#9ca3af' }}>共 {ticketsData.total} 条</span>}</div>
                    </div>
                    <div className="tickets-list-body">
                      {ticketsLoading && !ticketsData ? (
                        <div style={{ padding: 60, textAlign: 'center' }}><Typography.Text type="secondary">加载中...</Typography.Text></div>
                      ) : !ticketsData || ticketsData.records.length === 0 ? (
                        <div style={{ padding: 60, textAlign: 'center' }}><Empty description="暂无工单" /></div>
                      ) : (
                        ticketsData.records.map(ticket => {
                          const pClass = priorityBadgeClass(ticket.priority)
                          const stClass = ticket.status === 'WAITING_CUSTOMER' ? 'waiting' : ticket.status === 'CLOSED' ? 'closed' : ticket.slaBreached ? 'overdue' : 'processing'
                          return (
                            <div key={ticket.id} className="ticket-row"
                              onClick={() => { void handleOpenDetail(ticket.id) }}>
                              <div style={{ flexShrink: 0, marginRight: 12, paddingTop: 2 }}>
                                <span className={`priority-pill ${pClass}`}>{priorityBadgeText(ticket.priority)}</span>
                              </div>
                              <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 3 }}>
                                  <span className="ticket-no">{ticket.ticketNo}</span>
                                  <span className="ticket-subject">{ticket.subject}</span>
                                  {ticket.linkSuspect && <Tag color="warning" style={{ fontSize: 11, lineHeight: '18px', height: 20 }}>疑似断链</Tag>}
                                </div>
                                <div className="ticket-customer" style={{ marginBottom: 4 }}>{ticket.customerEmail}</div>
                                <div><span className="ticket-source-tag">{ticket.mailboxName || '客户邮件'}</span></div>
                              </div>
                              <div style={{ flexShrink: 0, marginLeft: 12, textAlign: 'right' }}>
                                <div className="ticket-time">{relativeTime(ticket.createdAt)}</div>
                                <div style={{ textAlign: 'right' }}>
                                  <div className={ticket.slaBreached ? 'ticket-sla-overdue' : 'ticket-sla-ok'} style={{ marginBottom: 4 }}>
                                    {ticket.slaBreached ? 'SLA已超时' : 'SLA正常'}
                                  </div>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'flex-end' }}>
                                    {ticket.assigneeName && <div className="assignee-avatar">{ticket.assigneeName[0]}</div>}
                                    {!ticket.assigneeName && <span style={{ fontSize: 12, color: '#9ca3af' }}>未分配</span>}
                                    <span className={`ticket-status-tag ${stClass}`}>{statusLabel(ticket.status)}</span>
                                  </div>
                                </div>
                              </div>
                            </div>
                          )
                        })
                      )}
                    </div>
                    {ticketsData && ticketsData.records.length > 0 && (
                      <div className="tickets-list-foot">
                        <Pagination
                          current={ticketPage}
                          pageSize={ticketPageSize}
                          total={ticketsData.total}
                          showSizeChanger
                          pageSizeOptions={[10, 20, 50]}
                          showTotal={(total) => `共 ${total} 条`}
                          onChange={(page) => { setTicketPage(page) }}
                          size="small"
                        />
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )
          ) : activeMenu === '客户管理' ? (
            <section className="app-content customer-page" aria-label="客户管理">
              <div className="content-title">
                <div>
                  <h1>客户管理</h1>
                  <p>按客户邮箱聚合历史工单和客户档案；第一版仅提供只读检索、详情和关联工单查看。</p>
                </div>
                <div className="content-actions">
                  <button disabled={customersLoading} onClick={() => void fetchCustomers()} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                </div>
              </div>

              {!canReadCustomers ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无客户查看权限</strong>
                  <p>客户只读页面仅允许管理员和客服处理人访问。</p>
                </div>
              ) : (
                <>
                  <div className="user-metrics customer-metrics">
                    <div className="user-metric">
                      <span>客户总数</span>
                      <strong>{customersData?.total ?? '-'}</strong>
                      <small>合并客户档案和历史工单邮箱。</small>
                    </div>
                    <div className="user-metric">
                      <span>当前页有工单客户</span>
                      <strong>{customerWithTicketCount}</strong>
                      <small>按当前页 `ticketCount` 统计。</small>
                    </div>
                    <div className="user-metric">
                      <span>7 天内来信</span>
                      <strong>{recentCustomerCount}</strong>
                      <small>按当前页最近来信时间估算。</small>
                    </div>
                    <div className="user-metric">
                      <span>备注覆盖</span>
                      <strong>{customerRemarkCount}</strong>
                      <small>备注只展示，不提供编辑入口。</small>
                    </div>
                  </div>

                  {customersError && (
                    <Alert
                      message={customersError}
                      type="error"
                      showIcon
                      style={{ marginBottom: 16 }}
                      action={<Button size="small" onClick={() => void fetchCustomers()}>重试</Button>}
                    />
                  )}

                  <Row gutter={[16, 16]}>
                    <Col xs={24} xl={9}>
                      <Card
                        title="客户列表"
                        extra={<Tag color="blue">只读列表</Tag>}
                        className="customer-card"
                      >
                        <Input.Search
                          allowClear
                          placeholder="搜索邮箱或展示名"
                          value={customerKeyword}
                          onChange={(event) => {
                            setCustomerKeyword(event.target.value)
                            setCustomerPage(1)
                          }}
                          onSearch={() => {
                            setCustomerPage(1)
                            void fetchCustomers()
                          }}
                          style={{ marginBottom: 14 }}
                        />

                        <div className="customer-list">
                          {customersLoading && !customersData ? (
                            <div className="customer-state">
                              <Typography.Text type="secondary">客户列表加载中...</Typography.Text>
                            </div>
                          ) : customerRecords.length === 0 ? (
                            <div className="customer-state">
                              <Empty description="暂无客户记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                            </div>
                          ) : (
                            customerRecords.map((customer) => {
                              const active = customer.email === selectedCustomerEmail
                              return (
                                <button
                                  key={customer.email}
                                  className={active ? 'customer-row active' : 'customer-row'}
                                  onClick={() => setSelectedCustomerEmail(customer.email)}
                                  type="button"
                                >
                                  <span className="customer-avatar">{customerInitial(customer)}</span>
                                  <span className="customer-row__main">
                                    <span className="customer-row__title">
                                      <strong>{customerDisplayName(customer)}</strong>
                                      {customer.lastMailAt && <Tag color="green">近期来信</Tag>}
                                      {customer.remark?.trim() && <Tag color="gold">有备注</Tag>}
                                    </span>
                                    <span className="customer-row__email">{customer.email}</span>
                                    <span className="customer-row__meta">
                                      最近 {formatCustomerDate(customer.lastMailAt)}
                                    </span>
                                  </span>
                                  <span className="customer-row__count">
                                    <strong>{customer.ticketCount}</strong>
                                    <small>工单</small>
                                  </span>
                                </button>
                              )
                            })
                          )}
                        </div>

                        {customersData && customersData.total > 0 && (
                          <Pagination
                            current={customerPage}
                            pageSize={customerPageSize}
                            total={customersData.total}
                            showSizeChanger
                            pageSizeOptions={[10, 20, 50, 100]}
                            size="small"
                            showTotal={(total) => `共 ${total} 位客户`}
                            onChange={(page, size) => {
                              setCustomerPage(page)
                              setCustomerPageSize(size)
                            }}
                            style={{ marginTop: 14 }}
                          />
                        )}
                      </Card>
                    </Col>

                    <Col xs={24} xl={15}>
                      <Card
                        title="客户详情"
                        extra={<Tag color="green">只读详情</Tag>}
                        className="customer-card"
                      >
                        {customerDetailLoading && !selectedCustomer ? (
                          <div className="customer-state">
                            <Typography.Text type="secondary">客户详情加载中...</Typography.Text>
                          </div>
                        ) : customerDetailError ? (
                          <Alert
                            message={customerDetailError}
                            type="error"
                            showIcon
                            action={<Button size="small" onClick={() => void fetchCustomerDetail()}>重试</Button>}
                          />
                        ) : !selectedCustomer ? (
                          <div className="customer-state">
                            <Empty description="请选择客户" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                          </div>
                        ) : (
                          <>
                            <div className="customer-detail-head">
                              <span className="customer-detail-avatar">{customerInitial(selectedCustomer)}</span>
                              <div>
                                <Typography.Title level={4} style={{ margin: 0 }}>
                                  {customerDisplayName(selectedCustomer)}
                                </Typography.Title>
                                <Typography.Text type="secondary">{selectedCustomer.email}</Typography.Text>
                              </div>
                              <Button
                                size="small"
                                onClick={() => {
                                  void navigator.clipboard?.writeText(selectedCustomer.email)
                                  message.success('客户邮箱已复制')
                                }}
                              >
                                复制邮箱
                              </Button>
                            </div>

                            <Descriptions
                              bordered
                              size="small"
                              column={{ xs: 1, md: 2 }}
                              items={[
                                { key: 'id', label: '客户ID', children: selectedCustomer.id ?? '仅工单聚合，暂无档案 ID' },
                                { key: 'lastMailAt', label: '最近来信', children: formatCustomerDate(selectedCustomer.lastMailAt) },
                                { key: 'ticketCount', label: '关联工单数', children: `${selectedCustomer.ticketCount} 条` },
                                { key: 'createdAt', label: '创建时间', children: formatCustomerDate(selectedCustomer.createdAt) },
                              ]}
                            />

                            <Alert
                              style={{ marginTop: 16 }}
                              type="info"
                              showIcon
                              message={selectedCustomer.remark?.trim() || '暂无客户备注'}
                              description="备注第一版仅展示，不提供新建或编辑客户入口。"
                            />

                            <div className="customer-section-head">
                              <div>
                                <strong>关联工单</strong>
                                <span>按客户邮箱从工单列表查询</span>
                              </div>
                              <Button
                                size="small"
                                loading={customerTicketsLoading}
                                onClick={() => void fetchCustomerTickets()}
                              >
                                刷新工单
                              </Button>
                            </div>

                            {customerTicketsError && (
                              <Alert message={customerTicketsError} type="error" showIcon style={{ marginBottom: 12 }} />
                            )}

                            <Table<TicketSummary>
                              rowKey="id"
                              size="small"
                              loading={customerTicketsLoading}
                              dataSource={customerTicketsData?.records ?? []}
                              pagination={false}
                              locale={{
                                emptyText: <Empty description="暂无关联工单" image={Empty.PRESENTED_IMAGE_SIMPLE} />,
                              }}
                              columns={[
                                {
                                  title: '工单号',
                                  dataIndex: 'ticketNo',
                                  width: 150,
                                  render: (value: string, record) => (
                                    <Button
                                      type="link"
                                      size="small"
                                      onClick={() => {
                                        setTicketKeyword(selectedCustomer.email)
                                        setActiveMenu('全部工单')
                                        void handleOpenDetail(record.id)
                                      }}
                                      style={{ padding: 0 }}
                                    >
                                      {value}
                                    </Button>
                                  ),
                                },
                                { title: '主题', dataIndex: 'subject', ellipsis: true },
                                {
                                  title: '状态',
                                  dataIndex: 'status',
                                  width: 112,
                                  render: (value: string, record) => {
                                    const cls = value === 'WAITING_CUSTOMER' ? 'waiting' : value === 'CLOSED' ? 'closed' : record.slaBreached ? 'overdue' : 'processing'
                                    return <span className={`ticket-status-tag ${cls}`}>{statusLabel(value)}</span>
                                  },
                                },
                                {
                                  title: 'SLA',
                                  dataIndex: 'slaBreached',
                                  width: 92,
                                  render: (value: boolean) => <Tag color={value ? 'red' : 'green'}>{value ? '已超时' : '正常'}</Tag>,
                                },
                                {
                                  title: '创建时间',
                                  dataIndex: 'createdAt',
                                  width: 140,
                                  render: (value: string) => formatCustomerDate(value),
                                },
                              ]}
                            />
                          </>
                        )}
                      </Card>
                    </Col>
                  </Row>
                </>
              )}
            </section>
          ) : activeMenu === '分配规则' ? (
            <section className="app-content" aria-label="分配规则">
              <div className="content-title">
                <div>
                  <h1>分配规则</h1>
                  <p>按优先级自动匹配新工单，命中后分配给指定处理人；保存后仅影响后续新建工单。</p>
                </div>
                <div className="content-actions">
                  <button disabled={assignmentRulesLoading} onClick={fetchAssignmentRules} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                  <button className="primary-action" onClick={openCreateAssignmentRule} type="button">
                    <Plus size={16} />
                    新建规则
                  </button>
                </div>
              </div>

              {!isAdmin ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无分配规则管理权限</strong>
                  <p>非管理员不可新建、编辑、启停、排序或删除自动分配规则。</p>
                </div>
              ) : (
                <>
                  <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">规则总数</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{assignmentSummary?.totalCount ?? '--'}</Typography.Title>
                        <Typography.Text type="secondary">启用 {assignmentSummary?.enabledCount ?? '--'} 条，停用 {assignmentSummary?.disabledCount ?? '--'} 条</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">默认兜底</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{assignmentSummary?.defaultCount ?? '--'}</Typography.Title>
                        <Typography.Text type="secondary">仅允许一个 DEFAULT 规则</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">当前命中测试</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{assignmentMatchResult?.matched ? '已命中' : '--'}</Typography.Title>
                        <Typography.Text type="secondary">{assignmentMatchResult?.ruleName || '输入邮件信息后测试'}</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">未保存修改</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{assignmentRuleDirty ? '1' : '0'}</Typography.Title>
                        <Typography.Text type="secondary">保存前不会影响自动建单</Typography.Text>
                      </Card>
                    </Col>
                  </Row>

                  <Alert
                    showIcon
                    type="info"
                    style={{ marginBottom: 16 }}
                    message="优先级数字越小越先匹配；排序、启停和规则保存只影响后续自动建单，历史工单不会回写。"
                  />

                  {assignmentRulesError && (
                    <Alert
                      showIcon
                      type="error"
                      style={{ marginBottom: 16 }}
                      message={assignmentRulesError}
                      action={<Button size="small" onClick={fetchAssignmentRules}>重试</Button>}
                    />
                  )}

                  <Row gutter={[16, 16]}>
                    <Col xs={24} xl={12}>
                      <Card
                        title="规则列表"
                        extra={<Tag color="blue">共 {assignmentSummary?.totalCount ?? 0} 条</Tag>}
                      >
                        <Space wrap style={{ width: '100%', marginBottom: 16 }}>
                          <Input
                            allowClear
                            prefix={<SearchOutlined />}
                            placeholder="规则名 / 匹配值"
                            style={{ width: 220 }}
                            value={assignmentKeyword}
                            onChange={(event) => setAssignmentKeyword(event.target.value)}
                            onPressEnter={() => void fetchAssignmentRules()}
                          />
                          <Select
                            style={{ width: 130 }}
                            value={assignmentEnabledFilter}
                            onChange={setAssignmentEnabledFilter}
                            options={[
                              { value: 'ALL', label: '全部状态' },
                              { value: 'true', label: '启用' },
                              { value: 'false', label: '停用' },
                            ]}
                          />
                          <Select
                            style={{ width: 150 }}
                            value={assignmentMatchTypeFilter}
                            onChange={setAssignmentMatchTypeFilter}
                            options={[
                              { value: 'ALL', label: '全部类型' },
                              { value: 'DEFAULT', label: '默认兜底' },
                              { value: 'SUBJECT_KEYWORD', label: '主题关键词' },
                              { value: 'MAILBOX', label: '来源邮箱' },
                              { value: 'FROM_EMAIL', label: '客户邮箱' },
                            ]}
                          />
                          <Button onClick={resetAssignmentFilters}>清空筛选</Button>
                        </Space>

                        <Table<AssignmentRule>
                          rowKey="id"
                          size="middle"
                          loading={assignmentRulesLoading}
                          dataSource={sortedAssignmentRecords}
                          pagination={false}
                          locale={{
                            emptyText: (
                              <Empty
                                description="还没有分配规则"
                                image={Empty.PRESENTED_IMAGE_SIMPLE}
                              >
                                <Button type="primary" onClick={openCreateAssignmentRule}>新建规则</Button>
                              </Empty>
                            ),
                          }}
                          rowClassName={(record) => record.id === assignmentForm.id ? 'ant-table-row-selected' : ''}
                          onRow={(record) => ({
                            onClick: () => selectAssignmentRule(record),
                          })}
                          columns={[
                            {
                              title: '优先级',
                              dataIndex: 'priorityOrder',
                              width: 82,
                              render: (value: number) => <Tag color="blue">{value}</Tag>,
                            },
                            {
                              title: '规则',
                              dataIndex: 'ruleName',
                              render: (_value: string, record: AssignmentRule) => (
                                <Space direction="vertical" size={2}>
                                  <Space wrap>
                                    <Typography.Text strong>{record.ruleName}</Typography.Text>
                                    {record.defaultRule && <Tag color="gold">默认</Tag>}
                                    <Tag color={record.enabled ? 'green' : 'default'}>{record.enabled ? '启用' : '停用'}</Tag>
                                  </Space>
                                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                    {assignmentRuleText(record)} · 分配给 {record.assigneeName || record.assigneeId}
                                  </Typography.Text>
                                </Space>
                              ),
                            },
                            {
                              title: '操作',
                              width: 188,
                              render: (_value: unknown, record: AssignmentRule, index: number) => (
                                <Space onClick={(event) => event.stopPropagation()}>
                                  <Button
                                    size="small"
                                    disabled={index === 0 || assignmentActionLoading}
                                    onClick={() => void moveAssignmentRule(record, -1)}
                                  >
                                    上移
                                  </Button>
                                  <Button
                                    size="small"
                                    disabled={index >= sortedAssignmentRecords.length - 1 || assignmentActionLoading}
                                    onClick={() => void moveAssignmentRule(record, 1)}
                                  >
                                    下移
                                  </Button>
                                  <Switch
                                    size="small"
                                    checked={record.enabled}
                                    loading={assignmentActionLoading && assignmentForm.id === record.id}
                                    onChange={(checked) => void toggleAssignmentRule(record, checked)}
                                  />
                                </Space>
                              ),
                            },
                          ]}
                        />

                      </Card>
                    </Col>

                    <Col xs={24} xl={12}>
                      <Row gutter={[16, 16]}>
                        <Col span={24}>
                          <Card
                            title="规则编辑"
                            extra={
                              assignmentRuleDirty
                                ? <Tag color="orange">有未保存修改</Tag>
                                : selectedAssignmentRule
                                  ? <Tag color="green">已保存</Tag>
                                  : <Tag>新建草稿</Tag>
                            }
                          >
                            <Row gutter={[12, 12]}>
                              <Col xs={24} md={12}>
                                <Typography.Text strong>规则名称</Typography.Text>
                                <Input
                                  value={assignmentForm.ruleName}
                                  onChange={(event) => updateAssignmentForm({ ruleName: event.target.value })}
                                  placeholder="VIP 售后优先"
                                  style={{ marginTop: 8 }}
                                />
                              </Col>
                              <Col xs={24} md={12}>
                                <Typography.Text strong>优先级</Typography.Text>
                                <Input
                                  type="number"
                                  min={1}
                                  max={9999}
                                  value={assignmentForm.priorityOrder}
                                  onChange={(event) => updateAssignmentForm({ priorityOrder: Number(event.target.value || 1) })}
                                  style={{ marginTop: 8 }}
                                />
                              </Col>
                              <Col span={24}>
                                <Typography.Text strong>匹配类型</Typography.Text>
                                <Segmented
                                  block
                                  style={{ marginTop: 8 }}
                                  value={assignmentForm.matchType}
                                  onChange={(value) => updateAssignmentForm({ matchType: value as AssignmentRuleMatchType })}
                                  options={[
                                    { value: 'DEFAULT', label: '默认' },
                                    { value: 'SUBJECT_KEYWORD', label: '主题关键词' },
                                    { value: 'MAILBOX', label: '来源邮箱' },
                                    { value: 'FROM_EMAIL', label: '客户邮箱' },
                                  ]}
                                />
                              </Col>
                              <Col xs={24} md={12}>
                                <Typography.Text strong>匹配值</Typography.Text>
                                <Input
                                  disabled={assignmentForm.matchType === 'DEFAULT'}
                                  value={assignmentForm.matchValue}
                                  onChange={(event) => updateAssignmentForm({ matchValue: event.target.value })}
                                  placeholder={assignmentForm.matchType === 'DEFAULT' ? '默认规则不需要匹配值' : 'VIP / support@example.com'}
                                  status={assignmentForm.matchType !== 'DEFAULT' && !assignmentForm.matchValue.trim() ? 'error' : undefined}
                                  style={{ marginTop: 8 }}
                                />
                                {assignmentForm.matchType !== 'DEFAULT' && !assignmentForm.matchValue.trim() && (
                                  <Typography.Text type="danger" style={{ fontSize: 12 }}>
                                    匹配类型不是 DEFAULT 时，匹配值不能为空。
                                  </Typography.Text>
                                )}
                              </Col>
                              <Col xs={24} md={12}>
                                <Typography.Text strong>分配处理人</Typography.Text>
                                <Select
                                  showSearch
                                  value={assignmentForm.assigneeId || undefined}
                                  placeholder="选择处理人"
                                  optionFilterProp="label"
                                  options={assignmentAssigneeOptions}
                                  onChange={(value) => updateAssignmentForm({ assigneeId: value })}
                                  style={{ width: '100%', marginTop: 8 }}
                                />
                              </Col>
                              <Col xs={24} md={12}>
                                <Typography.Text strong>启用状态</Typography.Text>
                                <Select
                                  value={String(assignmentForm.enabled)}
                                  onChange={(value) => updateAssignmentForm({ enabled: value === 'true' })}
                                  options={[
                                    { value: 'true', label: '启用' },
                                    { value: 'false', label: '停用' },
                                  ]}
                                  style={{ width: '100%', marginTop: 8 }}
                                />
                              </Col>
                              <Col xs={24} md={12}>
                                <Typography.Text strong>分配通知</Typography.Text>
                                <Select
                                  value={String(assignmentForm.notifyEnabled)}
                                  onChange={(value) => updateAssignmentForm({ notifyEnabled: value === 'true' })}
                                  options={[
                                    { value: 'true', label: '通知处理人' },
                                    { value: 'false', label: '不发送通知' },
                                  ]}
                                  style={{ width: '100%', marginTop: 8 }}
                                />
                              </Col>
                              <Col span={24}>
                                <Alert
                                  type="info"
                                  showIcon
                                  message="规则预览"
                                  description={`IF ${assignmentMatchTypeLabel(assignmentForm.matchType)} ${assignmentForm.matchType === 'DEFAULT' ? '兜底命中' : `= ${assignmentForm.matchValue || '未填写'}`} THEN 分配给 ${assignmentAssignees.find((agent) => String(agent.id) === assignmentForm.assigneeId)?.displayName || '未选择处理人'}`}
                                />
                              </Col>
                            </Row>
                            <Space style={{ marginTop: 16 }}>
                              <Button onClick={openCreateAssignmentRule}>新建草稿</Button>
                              <Button
                                type="primary"
                                loading={assignmentSaving}
                                disabled={!assignmentForm.ruleName.trim() || !assignmentForm.assigneeId || (assignmentForm.matchType !== 'DEFAULT' && !assignmentForm.matchValue.trim())}
                                onClick={() => void saveAssignmentRule()}
                              >
                                保存规则
                              </Button>
                              <Button
                                danger
                                disabled={!selectedAssignmentRule}
                                icon={<DeleteOutlined />}
                                onClick={() => selectedAssignmentRule && setAssignmentConfirmAction({ type: 'delete', rule: selectedAssignmentRule })}
                              >
                                删除
                              </Button>
                            </Space>
                          </Card>
                        </Col>

                        <Col span={24}>
                          <Card title="测试匹配" extra={assignmentMatchResult?.matched ? <Tag color="green">已命中</Tag> : <Tag>未测试</Tag>}>
                            <Row gutter={[12, 12]}>
                              <Col xs={24} md={12}>
                                <Typography.Text strong>来源邮箱</Typography.Text>
                                <Select
                                  showSearch
                                  value={assignmentTestForm.mailboxId || undefined}
                                  placeholder="选择来源邮箱"
                                  optionFilterProp="label"
                                  options={assignmentMailboxOptions}
                                  onChange={(value) => setAssignmentTestForm((form) => ({ ...form, mailboxId: value }))}
                                  style={{ width: '100%', marginTop: 8 }}
                                />
                              </Col>
                              <Col xs={24} md={12}>
                                <Typography.Text strong>客户邮箱</Typography.Text>
                                <Input
                                  value={assignmentTestForm.fromEmail}
                                  onChange={(event) => setAssignmentTestForm((form) => ({ ...form, fromEmail: event.target.value }))}
                                  placeholder="buyer@acme.com"
                                  style={{ marginTop: 8 }}
                                />
                              </Col>
                              <Col span={24}>
                                <Typography.Text strong>邮件主题</Typography.Text>
                                <Input
                                  value={assignmentTestForm.subject}
                                  onChange={(event) => setAssignmentTestForm((form) => ({ ...form, subject: event.target.value }))}
                                  placeholder="VIP 客户反馈：无法登录后台"
                                  style={{ marginTop: 8 }}
                                />
                              </Col>
                            </Row>
                            <Button
                              block
                              type="primary"
                              loading={assignmentTesting}
                              disabled={!assignmentTestForm.mailboxId}
                              onClick={() => void runAssignmentRuleTest()}
                              style={{ marginTop: 16 }}
                            >
                              运行测试匹配
                            </Button>
                            {assignmentMatchResult ? (
                              <Alert
                                showIcon
                                type={assignmentMatchResult.matched ? 'success' : 'warning'}
                                style={{ marginTop: 16 }}
                                message={assignmentMatchResult.matched ? `命中 ${assignmentMatchResult.ruleName}` : '未命中分配规则'}
                                description={assignmentMatchResult.matched
                                  ? `${assignmentMatchTypeLabel(assignmentMatchResult.matchType)} = ${assignmentMatchResult.matchValue || '-'}，分配给 ${assignmentMatchResult.assigneeName || assignmentMatchResult.assigneeId}，${assignmentMatchResult.notifyEnabled ? '通知处理人' : '不发送通知'}。`
                                  : '当前输入未命中任何启用规则，自动建单会继续走默认规则或邮箱默认处理人。'}
                              />
                            ) : (
                              <Empty
                                image={Empty.PRESENTED_IMAGE_SIMPLE}
                                description="输入来源邮箱、客户邮箱和主题后测试命中结果"
                                style={{ marginTop: 16 }}
                              />
                            )}
                          </Card>
                        </Col>
                      </Row>
                    </Col>
                  </Row>
                </>
              )}
            </section>
          ) : activeMenu === 'SLA策略' ? (
            <section className="app-content" aria-label="SLA策略">
              <div className="content-title">
                <div>
                  <h1>SLA策略</h1>
                  <p>维护首次响应、解决时限、预警阈值和升级阈值，绑定工作日历后按工作时间计算截止时间。</p>
                </div>
                <div className="content-actions">
                  <button disabled={slaPoliciesLoading} onClick={fetchSlaPolicies} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                  <button className="primary-action" onClick={openCreateSlaPolicy} type="button">
                    <Plus size={16} />
                    新建策略
                  </button>
                </div>
              </div>

              {!isAdmin ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无 SLA 策略管理权限</strong>
                  <p>非管理员不可新建、编辑、启停、设置默认或删除 SLA 策略。</p>
                </div>
              ) : (
                <>
                  <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">策略总数</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{slaPolicySummary?.totalCount ?? '--'}</Typography.Title>
                        <Typography.Text type="secondary">启用 {slaPolicySummary?.enabledCount ?? '--'} 条，停用 {slaPolicySummary?.disabledCount ?? '--'} 条</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">启用策略</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{slaPolicySummary?.enabledCount ?? '--'}</Typography.Title>
                        <Typography.Text type="secondary">仅启用策略参与新工单 SLA 计算</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">默认策略</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{slaPolicySummary?.defaultCount ?? '--'}</Typography.Title>
                        <Typography.Text type="secondary">默认策略必须启用，不能停用或删除</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">绑定日历</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{slaCalendarCount || '--'}</Typography.Title>
                        <Typography.Text type="secondary">策略保存时必须选择工作日历</Typography.Text>
                      </Card>
                    </Col>
                  </Row>

                  <Alert
                    showIcon
                    type="info"
                    style={{ marginBottom: 16 }}
                    message="默认策略只影响后续新建工单；历史工单已有的响应截止、解决截止不自动重算。"
                  />

                  {slaPoliciesError && (
                    <Alert
                      showIcon
                      type="error"
                      style={{ marginBottom: 16 }}
                      message={slaPoliciesError}
                      action={<Button size="small" onClick={fetchSlaPolicies}>重试</Button>}
                    />
                  )}

                  <Row gutter={[16, 16]}>
                    <Col xs={24} xl={10}>
                      <Card title="策略列表">
                        <Space wrap style={{ width: '100%', marginBottom: 16 }}>
                          <Input
                            allowClear
                            prefix={<SearchOutlined />}
                            placeholder="策略名称"
                            style={{ width: 190 }}
                            value={slaPolicyKeyword}
                            onChange={(event) => setSlaPolicyKeyword(event.target.value)}
                            onPressEnter={() => void fetchSlaPolicies()}
                          />
                          <Select
                            style={{ width: 126 }}
                            value={slaPolicyEnabledFilter}
                            onChange={setSlaPolicyEnabledFilter}
                            options={[
                              { value: 'ALL', label: '全部状态' },
                              { value: 'true', label: '启用' },
                              { value: 'false', label: '停用' },
                            ]}
                          />
                          <Select
                            style={{ width: 126 }}
                            value={slaPolicyDefaultFilter}
                            onChange={setSlaPolicyDefaultFilter}
                            options={[
                              { value: 'ALL', label: '全部策略' },
                              { value: 'true', label: '默认策略' },
                              { value: 'false', label: '非默认' },
                            ]}
                          />
                          <Button onClick={resetSlaPolicyFilters}>清空筛选</Button>
                        </Space>

                        <Table<SlaPolicy>
                          rowKey="id"
                          size="middle"
                          loading={slaPoliciesLoading || workCalendarsLoading}
                          dataSource={slaPolicyRecords}
                          pagination={false}
                          locale={{
                            emptyText: (
                              <Empty
                                description="还没有 SLA 策略"
                                image={Empty.PRESENTED_IMAGE_SIMPLE}
                              >
                                <Button type="primary" onClick={openCreateSlaPolicy}>新建策略</Button>
                              </Empty>
                            ),
                          }}
                          rowClassName={(record) => record.id === slaPolicyForm.id ? 'ant-table-row-selected' : ''}
                          onRow={(record) => ({
                            onClick: () => selectSlaPolicy(record),
                          })}
                          columns={[
                            {
                              title: '策略',
                              dataIndex: 'policyName',
                              render: (_value: string, record: SlaPolicy) => {
                                const calendar = workCalendars.find((item) => item.id === record.calendarId)
                                return (
                                  <Space direction="vertical" size={4}>
                                    <Space wrap>
                                      <Typography.Text strong>{record.policyName}</Typography.Text>
                                      {record.defaultPolicy && <Tag color="blue">默认</Tag>}
                                      <Tag color={record.enabled ? 'green' : 'default'}>{record.enabled ? '启用' : '停用'}</Tag>
                                    </Space>
                                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                      绑定日历：{calendar?.calendarName || `#${record.calendarId}`}
                                      {calendar ? ` · ${workdayLabel(calendar.workdays)} ${calendar.workStartTime}-${calendar.workEndTime}` : ''}
                                    </Typography.Text>
                                    <Space wrap size={[4, 4]}>
                                      <Tag>响应 {hoursLabel(record.responseHours)}</Tag>
                                      <Tag>解决 {hoursLabel(record.resolveHours)}</Tag>
                                      <Tag color="gold">预警 剩余 {hoursLabel(record.warningRemainHours)}</Tag>
                                      <Tag color="orange">升级 {hoursLabel(record.escalateAfterBreachHours)}</Tag>
                                    </Space>
                                  </Space>
                                )
                              },
                            },
                            {
                              title: '操作',
                              width: 190,
                              render: (_value: unknown, record: SlaPolicy) => (
                                <Space onClick={(event) => event.stopPropagation()}>
                                  <Button
                                    size="small"
                                    disabled={record.defaultPolicy || !record.enabled || slaPolicyActionLoading}
                                    onClick={() => void setDefaultSlaPolicy(record)}
                                  >
                                    默认
                                  </Button>
                                  <Switch
                                    size="small"
                                    checked={record.enabled}
                                    disabled={record.defaultPolicy}
                                    loading={slaPolicyActionLoading && slaPolicyForm.id === record.id}
                                    onChange={(checked) => void toggleSlaPolicy(record, checked)}
                                  />
                                </Space>
                              ),
                            },
                          ]}
                        />

                      </Card>
                    </Col>

                    <Col xs={24} xl={9}>
                      <Card
                        title="新建/编辑策略"
                        extra={
                          slaPolicyDirty
                            ? <Tag color="orange">有未保存修改</Tag>
                            : selectedSlaPolicy
                              ? <Tag color="green">已保存</Tag>
                              : <Tag>新建草稿</Tag>
                        }
                      >
                        <Row gutter={[12, 12]}>
                          <Col span={24}>
                            <Typography.Text strong>策略名称</Typography.Text>
                            <Input
                              value={slaPolicyForm.policyName}
                              onChange={(event) => updateSlaPolicyForm({ policyName: event.target.value })}
                              placeholder="VIP 客户 2 小时响应"
                              style={{ marginTop: 8 }}
                            />
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>policyName，最多 64 字。</Typography.Text>
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>启用策略</Typography.Text>
                            <Select
                              value={String(slaPolicyForm.enabled)}
                              onChange={(value) => updateSlaPolicyForm({ enabled: value === 'true' })}
                              options={[
                                { value: 'true', label: '启用' },
                                { value: 'false', label: '停用' },
                              ]}
                              style={{ width: '100%', marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>默认策略</Typography.Text>
                            <Select
                              value={String(slaPolicyForm.defaultPolicy)}
                              onChange={(value) => updateSlaPolicyForm({ defaultPolicy: value === 'true' })}
                              options={[
                                { value: 'true', label: '设为默认' },
                                { value: 'false', label: '非默认' },
                              ]}
                              style={{ width: '100%', marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>首次响应时限（工作小时）</Typography.Text>
                            <Input
                              type="number"
                              min={1}
                              max={9999}
                              value={slaPolicyForm.responseHours}
                              onChange={(event) => updateSlaPolicyForm({ responseHours: Number(event.target.value || 1) })}
                              style={{ marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>解决时限（工作小时）</Typography.Text>
                            <Input
                              type="number"
                              min={1}
                              max={9999}
                              status={slaResolveHoursInvalid ? 'error' : undefined}
                              value={slaPolicyForm.resolveHours}
                              onChange={(event) => updateSlaPolicyForm({ resolveHours: event.target.value })}
                              placeholder="可空"
                              style={{ marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>预警阈值（剩余工作小时）</Typography.Text>
                            <Input
                              type="number"
                              min={1}
                              max={9999}
                              status={slaWarningInvalid ? 'error' : undefined}
                              value={slaPolicyForm.warningRemainHours}
                              onChange={(event) => updateSlaPolicyForm({ warningRemainHours: Number(event.target.value || 1) })}
                              style={{ marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>升级阈值（超时后工作小时）</Typography.Text>
                            <Input
                              type="number"
                              min={1}
                              max={9999}
                              value={slaPolicyForm.escalateAfterBreachHours}
                              onChange={(event) => updateSlaPolicyForm({ escalateAfterBreachHours: event.target.value })}
                              placeholder="可空"
                              style={{ marginTop: 8 }}
                            />
                          </Col>
                          <Col span={24}>
                            <Typography.Text strong>绑定日历</Typography.Text>
                            <Select
                              showSearch
                              loading={workCalendarsLoading}
                              value={slaPolicyForm.calendarId || undefined}
                              placeholder="请选择工作日历"
                              optionFilterProp="label"
                              options={slaCalendarOptions}
                              onChange={(value) => updateSlaPolicyForm({ calendarId: value })}
                              style={{ width: '100%', marginTop: 8 }}
                            />
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>deadline 计算以所选工作日历为准。</Typography.Text>
                          </Col>
                          <Col span={24}>
                            <Alert
                              type={slaResolveHoursInvalid || slaWarningInvalid ? 'error' : 'info'}
                              showIcon
                              message={slaResolveHoursInvalid || slaWarningInvalid ? '策略校验未通过' : '策略校验'}
                              description={
                                slaResolveHoursInvalid
                                  ? '解决时限不能小于首次响应时限。'
                                  : slaWarningInvalid
                                    ? '预警阈值必须小于首次响应时限。'
                                    : slaPolicyForm.defaultPolicy
                                      ? '默认策略必须保持启用，保存后其他策略会取消默认标记。'
                                      : '保存后配置立即生效，仅影响后续新建工单。'
                              }
                            />
                          </Col>
                        </Row>

                        <Space style={{ marginTop: 16 }} wrap>
                          <Button onClick={openCreateSlaPolicy}>新建草稿</Button>
                          <Button
                            type="primary"
                            loading={slaPolicySaving}
                            disabled={
                              !slaPolicyForm.policyName.trim()
                              || !slaPolicyForm.calendarId
                              || slaResolveHoursInvalid
                              || slaWarningInvalid
                            }
                            onClick={() => void saveSlaPolicy()}
                          >
                            保存策略
                          </Button>
                          <Button
                            danger
                            disabled={!selectedSlaPolicy || selectedSlaPolicy.defaultPolicy}
                            icon={<DeleteOutlined />}
                            onClick={() => selectedSlaPolicy && setSlaPolicyConfirmAction({ type: 'delete', policy: selectedSlaPolicy })}
                          >
                            删除
                          </Button>
                        </Space>
                      </Card>
                    </Col>

                    <Col xs={24} xl={5}>
                      <Card title="SLA 预览" extra={<Tag color="blue">前端估算</Tag>}>
                        <Space direction="vertical" size={12} style={{ width: '100%' }}>
                          <Descriptions
                            bordered
                            column={1}
                            size="small"
                            items={[
                              { key: 'calendar', label: '绑定日历', children: selectedWorkCalendar?.calendarName || '未选择' },
                              { key: 'timezone', label: '时区', children: selectedWorkCalendar?.timezone || '-' },
                              { key: 'workdays', label: '工作日', children: workdayLabel(selectedWorkCalendar?.workdays) },
                              {
                                key: 'workTime',
                                label: '工作时段',
                                children: selectedWorkCalendar ? `${selectedWorkCalendar.workStartTime}-${selectedWorkCalendar.workEndTime}` : '-',
                              },
                            ]}
                          />
                          <Descriptions
                            bordered
                            column={1}
                            size="small"
                            items={[
                              { key: 'created', label: '建单时间', children: slaPreviewBaseTime.format('YYYY-MM-DD HH:mm') },
                              { key: 'response', label: '首次响应截止', children: slaPreview.responseDeadline.format('YYYY-MM-DD HH:mm') },
                              {
                                key: 'resolve',
                                label: '解决截止',
                                children: slaPreview.resolveDeadline ? slaPreview.resolveDeadline.format('YYYY-MM-DD HH:mm') : '未配置',
                              },
                            ]}
                          />
                          <Timeline
                            items={[
                              {
                                color: 'blue',
                                children: (
                                  <span>新建工单：写入策略 ID 与截止时间</span>
                                ),
                              },
                              {
                                color: 'orange',
                                children: (
                                  <span>即将超时：{slaPreview.warningAt.format('YYYY-MM-DD HH:mm')}</span>
                                ),
                              },
                              {
                                color: 'red',
                                children: (
                                  <span>升级提醒：{slaPreview.escalateAt ? slaPreview.escalateAt.format('YYYY-MM-DD HH:mm') : '未配置'}</span>
                                ),
                              },
                            ]}
                          />
                        </Space>
                      </Card>
                    </Col>
                  </Row>
                </>
              )}
            </section>
          ) : activeMenu === '工作日历' ? (
            <section className="app-content" aria-label="工作日历">
              <div className="content-title">
                <div>
                  <h1>工作日历</h1>
                  <p>维护 SLA 计算使用的工作日、工作时段和节假日；策略绑定后按该日历计算响应与解决截止时间。</p>
                </div>
                <div className="content-actions">
                  <button disabled={workCalendarsLoading || holidaysLoading} onClick={() => { void fetchWorkCalendarsPage(); void fetchHolidays(); void fetchCalendarSlaPolicies() }} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                  <button className="primary-action" onClick={openCreateWorkCalendar} type="button">
                    <Plus size={16} />
                    新建日历
                  </button>
                  <button onClick={openCreateHoliday} type="button">
                    <Plus size={16} />
                    新增节假日
                  </button>
                </div>
              </div>

              {!isAdmin ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无工作日历管理权限</strong>
                  <p>非管理员不可新建、编辑、设置默认、删除工作日历或维护节假日。</p>
                </div>
              ) : (
                <>
                  <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">日历总数</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{workCalendarSummary?.totalCount ?? '--'}</Typography.Title>
                        <Typography.Text type="secondary">工作日历可被 SLA 策略绑定</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">默认日历</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{workCalendarSummary?.defaultCount ?? '--'}</Typography.Title>
                        <Typography.Text type="secondary">默认日历不可直接删除</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">本月节假日</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{holidaysData?.summary.totalCount ?? '--'}</Typography.Title>
                        <Typography.Text type="secondary">按所选日历和月份统计</Typography.Text>
                      </Card>
                    </Col>
                    <Col xs={24} md={12} xl={6}>
                      <Card>
                        <Typography.Text type="secondary">绑定策略</Typography.Text>
                        <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{totalCalendarPolicyCount || '--'}</Typography.Title>
                        <Typography.Text type="secondary">由 SLA 策略列表按日历派生</Typography.Text>
                      </Card>
                    </Col>
                  </Row>

                  <Alert
                    showIcon
                    type="info"
                    style={{ marginBottom: 16 }}
                    message="工作日历和节假日保存后只影响后续 SLA 计算；历史工单已有响应截止和解决截止不自动重算。"
                  />

                  {workCalendarError && (
                    <Alert
                      showIcon
                      type="error"
                      style={{ marginBottom: 16 }}
                      message={workCalendarError}
                      action={<Button size="small" onClick={fetchWorkCalendarsPage}>重试</Button>}
                    />
                  )}

                  {holidaysError && (
                    <Alert
                      showIcon
                      type="error"
                      style={{ marginBottom: 16 }}
                      message={holidaysError}
                      action={<Button size="small" onClick={fetchHolidays}>重试</Button>}
                    />
                  )}

                  <Row gutter={[16, 16]}>
                    <Col xs={24} xl={8}>
                      <Card title="日历列表">
                        <Space wrap style={{ width: '100%', marginBottom: 16 }}>
                          <Input
                            allowClear
                            prefix={<SearchOutlined />}
                            placeholder="日历名称"
                            style={{ width: 180 }}
                            value={workCalendarKeyword}
                            onChange={(event) => setWorkCalendarKeyword(event.target.value)}
                            onPressEnter={() => void fetchWorkCalendarsPage()}
                          />
                          <Select
                            style={{ width: 126 }}
                            value={workCalendarDefaultFilter}
                            onChange={setWorkCalendarDefaultFilter}
                            options={[
                              { value: 'ALL', label: '全部日历' },
                              { value: 'true', label: '默认日历' },
                              { value: 'false', label: '非默认' },
                            ]}
                          />
                          <Button onClick={resetWorkCalendarFilters}>清空筛选</Button>
                        </Space>

                        <Table<WorkCalendar>
                          rowKey="id"
                          size="middle"
                          loading={workCalendarsLoading}
                          dataSource={workCalendarRecords}
                          pagination={false}
                          locale={{
                            emptyText: (
                              <Empty description="还没有工作日历" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                                <Button type="primary" onClick={openCreateWorkCalendar}>新建日历</Button>
                              </Empty>
                            ),
                          }}
                          rowClassName={(record) => record.id === workCalendarForm.id ? 'ant-table-row-selected' : ''}
                          onRow={(record) => ({
                            onClick: () => selectWorkCalendar(record),
                          })}
                          columns={[
                            {
                              title: '日历',
                              dataIndex: 'calendarName',
                              render: (_value: string, record: WorkCalendar) => {
                                const policyCount = calendarSlaPolicies.filter((policy) => policy.calendarId === record.id).length
                                return (
                                  <Space direction="vertical" size={4}>
                                    <Space wrap>
                                      <Typography.Text strong>{record.calendarName}</Typography.Text>
                                      {record.defaultCalendar && <Tag color="blue">默认</Tag>}
                                      {policyCount > 0 && <Tag color="gold">绑定 {policyCount} 条策略</Tag>}
                                    </Space>
                                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                      {record.timezone} · {workdayLabel(record.workdays)} · {record.workStartTime}-{record.workEndTime}
                                    </Typography.Text>
                                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                      更新：{record.updatedAt ? dayjs(record.updatedAt).format('YYYY-MM-DD HH:mm') : '-'}
                                    </Typography.Text>
                                  </Space>
                                )
                              },
                            },
                            {
                              title: '操作',
                              width: 92,
                              render: (_value: unknown, record: WorkCalendar) => (
                                <Space onClick={(event) => event.stopPropagation()}>
                                  <Button
                                    size="small"
                                    disabled={record.defaultCalendar || workCalendarActionLoading}
                                    onClick={() => void setDefaultWorkCalendar(record)}
                                  >
                                    默认
                                  </Button>
                                </Space>
                              ),
                            },
                          ]}
                        />
                      </Card>
                    </Col>

                    <Col xs={24} xl={8}>
                      <Card
                        title="日历编辑"
                        extra={
                          workCalendarDirty
                            ? <Tag color="orange">有未保存修改</Tag>
                            : selectedCalendarForPage
                              ? <Tag color="green">已保存</Tag>
                              : <Tag>新建草稿</Tag>
                        }
                      >
                        <Row gutter={[12, 12]}>
                          <Col span={24}>
                            <Typography.Text strong>日历名称</Typography.Text>
                            <Input
                              value={workCalendarForm.calendarName}
                              onChange={(event) => updateWorkCalendarForm({ calendarName: event.target.value })}
                              placeholder="客服工作日历"
                              style={{ marginTop: 8 }}
                            />
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>calendarName，最多 64 字。</Typography.Text>
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>时区</Typography.Text>
                            <Select
                              showSearch
                              value={workCalendarForm.timezone}
                              onChange={(value) => updateWorkCalendarForm({ timezone: value })}
                              options={[
                                { value: 'Asia/Shanghai', label: 'Asia/Shanghai' },
                                { value: 'Asia/Singapore', label: 'Asia/Singapore' },
                                { value: 'UTC', label: 'UTC' },
                              ]}
                              style={{ width: '100%', marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>默认日历</Typography.Text>
                            <Select
                              value={String(workCalendarForm.defaultCalendar)}
                              onChange={(value) => updateWorkCalendarForm({ defaultCalendar: value === 'true' })}
                              options={[
                                { value: 'true', label: '设为默认' },
                                { value: 'false', label: '非默认' },
                              ]}
                              style={{ width: '100%', marginTop: 8 }}
                            />
                          </Col>
                          <Col span={24}>
                            <Typography.Text strong>工作日</Typography.Text>
                            <div style={{ marginTop: 8 }}>
                              <Checkbox.Group
                                value={workCalendarForm.workdays}
                                options={weekdayNames.map((label, index) => ({ label, value: index + 1 }))}
                                onChange={(values) => updateWorkCalendarForm({ workdays: values.map(Number).sort((a, b) => a - b) })}
                              />
                            </div>
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>workdays，1=周一，7=周日，至少选择一天。</Typography.Text>
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>工作开始时间</Typography.Text>
                            <Input
                              type="time"
                              value={workCalendarForm.workStartTime}
                              onChange={(event) => updateWorkCalendarForm({ workStartTime: event.target.value })}
                              style={{ marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>工作结束时间</Typography.Text>
                            <Input
                              type="time"
                              status={workCalendarTimeInvalid ? 'error' : undefined}
                              value={workCalendarForm.workEndTime}
                              onChange={(event) => updateWorkCalendarForm({ workEndTime: event.target.value })}
                              style={{ marginTop: 8 }}
                            />
                          </Col>
                          <Col span={24}>
                            <Alert
                              showIcon
                              type={workCalendarTimeInvalid || workCalendarForm.workdays.length === 0 ? 'error' : 'info'}
                              message={workCalendarTimeInvalid || workCalendarForm.workdays.length === 0 ? '日历校验未通过' : '保存影响范围'}
                              description={
                                workCalendarTimeInvalid
                                  ? '工作开始时间必须早于工作结束时间。'
                                  : workCalendarForm.workdays.length === 0
                                    ? '至少选择一个工作日。'
                                    : '保存后只影响后续 SLA 计算，历史工单已有截止时间保持不变。'
                              }
                            />
                          </Col>
                        </Row>

                        <Space style={{ marginTop: 16 }} wrap>
                          <Button onClick={openCreateWorkCalendar}>新建草稿</Button>
                          <Button
                            type="primary"
                            loading={workCalendarSaving}
                            disabled={!workCalendarForm.calendarName.trim() || workCalendarForm.workdays.length === 0 || workCalendarTimeInvalid}
                            onClick={() => void saveWorkCalendar()}
                          >
                            保存日历
                          </Button>
                          <Button
                            danger
                            disabled={!selectedCalendarForPage || Boolean(workCalendarDeleteBlockedReason)}
                            icon={<DeleteOutlined />}
                            onClick={() => selectedCalendarForPage && setWorkCalendarConfirmAction({ type: 'delete-calendar', calendar: selectedCalendarForPage })}
                          >
                            删除日历
                          </Button>
                          {workCalendarDeleteBlockedReason && <Tag color="orange">{workCalendarDeleteBlockedReason}</Tag>}
                        </Space>
                      </Card>
                    </Col>

                    <Col xs={24} xl={8}>
                      <Card
                        title="节假日"
                        extra={
                          <Button
                            size="small"
                            loading={holidayImporting}
                            disabled={!workCalendarForm.id || holidaysLoading}
                            onClick={() => void importNationalHolidays()}
                          >
                            导入 {holidayImportYear} 法定节假日
                          </Button>
                        }
                      >
                        <Space wrap style={{ width: '100%', marginBottom: 16 }}>
                          <DatePicker
                            picker="month"
                            value={holidayMonthValue}
                            onChange={(value) => {
                              if (value) setHolidayMonth(value.format('YYYY-MM'))
                            }}
                            allowClear={false}
                          />
                          <Input
                            allowClear
                            placeholder="节假日名称"
                            style={{ width: 150 }}
                            value={holidayKeyword}
                            onChange={(event) => setHolidayKeyword(event.target.value)}
                            onPressEnter={() => void fetchHolidays()}
                          />
                          <Button onClick={openCreateHoliday}>新增</Button>
                        </Space>
                        <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12, marginBottom: 12 }}>
                          快捷导入从三方节假日接口获取所选年份放假日期；补班日当前不单独建模。
                        </Typography.Text>

                        <Table<Holiday>
                          rowKey="id"
                          size="small"
                          loading={holidaysLoading}
                          dataSource={holidayRecords}
                          pagination={false}
                          locale={{
                            emptyText: (
                              <Empty description="当前月份暂无节假日" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                                <Button type="primary" onClick={openCreateHoliday}>新增节假日</Button>
                              </Empty>
                            ),
                          }}
                          onRow={(record) => ({
                            onClick: () => selectHoliday(record),
                          })}
                          columns={[
                            { title: '日期', dataIndex: 'holidayDate', width: 112 },
                            { title: '名称', dataIndex: 'holidayName' },
                            {
                              title: '操作',
                              width: 120,
                              render: (_value: unknown, record: Holiday) => (
                                <Space onClick={(event) => event.stopPropagation()}>
                                  <Button size="small" onClick={() => selectHoliday(record)}>编辑</Button>
                                  <Button
                                    size="small"
                                    danger
                                    disabled={workCalendarActionLoading}
                                    onClick={() => setWorkCalendarConfirmAction({ type: 'delete-holiday', holiday: record })}
                                  >
                                    删除
                                  </Button>
                                </Space>
                              ),
                            },
                          ]}
                        />

                        <Card size="small" title="新增/编辑节假日" style={{ marginTop: 16 }}>
                          <Space direction="vertical" size={10} style={{ width: '100%' }}>
                            <DatePicker
                              value={holidayDateValue}
                              onChange={(value) => updateHolidayForm({ holidayDate: value ? value.format('YYYY-MM-DD') : '' })}
                              style={{ width: '100%' }}
                              allowClear={false}
                            />
                            <Input
                              value={holidayForm.holidayName}
                              onChange={(event) => updateHolidayForm({ holidayName: event.target.value })}
                              placeholder="国庆节"
                            />
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>字段按当前后端：holidayDate + holidayName。</Typography.Text>
                            <Space wrap>
                              <Button onClick={openCreateHoliday}>新建草稿</Button>
                              <Button
                                type="primary"
                                loading={holidaySaving}
                                disabled={!holidayForm.calendarId || !holidayForm.holidayDate || !holidayForm.holidayName.trim()}
                                onClick={() => void saveHoliday()}
                              >
                                保存节假日
                              </Button>
                              {holidayDirty && <Tag color="orange">有未保存修改</Tag>}
                            </Space>
                          </Space>
                        </Card>
                      </Card>
                    </Col>
                  </Row>

                  <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                    <Col xs={24} xl={14}>
                      <Card
                        title="月历预览"
                        extra={
                          <DatePicker
                            picker="month"
                            size="small"
                            value={holidayMonthValue}
                            onChange={(value) => {
                              if (value) setHolidayMonth(value.format('YYYY-MM'))
                            }}
                            allowClear={false}
                            style={{ width: 128 }}
                          />
                        }
                      >
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, minmax(0, 1fr))', gap: 8 }}>
                          {weekdayNames.map((name) => (
                            <Typography.Text key={name} type="secondary" style={{ textAlign: 'center', fontSize: 12, fontWeight: 700 }}>
                              {name}
                            </Typography.Text>
                          ))}
                          {monthCells.map((cell) => (
                            <div
                              key={cell.dateKey}
                              style={{
                                minHeight: 58,
                                border: cell.isToday ? '2px solid #2563eb' : cell.isWorkday ? '1px solid #bfdbfe' : '1px solid #e5e7eb',
                                borderRadius: 8,
                                padding: 8,
                                background: !cell.inMonth ? '#f8fafc' : cell.holidayName ? '#fff7ed' : cell.isWorkday ? '#dbeafe' : '#fff',
                                color: !cell.inMonth ? '#94a3b8' : '#111827',
                              }}
                            >
                              <Typography.Text strong>{cell.date.date()}</Typography.Text>
                              <div style={{ marginTop: 4 }}>
                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                  {cell.holidayName || (cell.isWorkday && selectedCalendarForPage ? `${selectedCalendarForPage.workStartTime}-${selectedCalendarForPage.workEndTime}` : weekdayNames[calendarDayNumber(cell.date) - 1])}
                                </Typography.Text>
                              </div>
                            </div>
                          ))}
                        </div>
                      </Card>
                    </Col>
                    <Col xs={24} xl={10}>
                      <Card
                        title="SLA 计算示例"
                        extra={(
                          <Space size={8}>
                            <Tag color="green">工作小时</Tag>
                            <Button
                              size="small"
                              onClick={() => {
                                setCalendarPreviewCreatedAt(calendarPreviewBaseTime.format('YYYY-MM-DDTHH:mm:ss'))
                                setCalendarPreviewResponseHours('2')
                                setCalendarPreviewResolveHours('16')
                              }}
                            >
                              重置
                            </Button>
                          </Space>
                        )}
                      >
                        <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                          <Col xs={24} md={12}>
                            <Typography.Text strong>建单时间</Typography.Text>
                            <DatePicker
                              showTime={{ format: 'HH:mm' }}
                              value={calendarPreviewCreatedAtValue}
                              onChange={(value) => {
                                setCalendarPreviewCreatedAt((value || calendarPreviewBaseTime).format('YYYY-MM-DDTHH:mm:ss'))
                              }}
                              format="YYYY-MM-DD HH:mm"
                              allowClear={false}
                              style={{ width: '100%', marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={12} md={6}>
                            <Typography.Text strong>响应小时</Typography.Text>
                            <Input
                              type="number"
                              min={1}
                              step={0.5}
                              value={calendarPreviewResponseHours}
                              onChange={(event) => setCalendarPreviewResponseHours(event.target.value)}
                              style={{ marginTop: 8 }}
                            />
                          </Col>
                          <Col xs={12} md={6}>
                            <Typography.Text strong>解决小时</Typography.Text>
                            <Input
                              type="number"
                              min={1}
                              step={0.5}
                              value={calendarPreviewResolveHours}
                              onChange={(event) => setCalendarPreviewResolveHours(event.target.value)}
                              style={{ marginTop: 8 }}
                            />
                          </Col>
                        </Row>
                        <Descriptions
                          bordered
                          size="small"
                          column={1}
                          items={[
                            { key: 'calendar', label: '工作日历', children: selectedCalendarForPage?.calendarName || '未选择' },
                            { key: 'created', label: '建单时间', children: calendarPreviewCreatedAtValue.format('YYYY-MM-DD HH:mm') },
                            { key: 'start', label: '起算时间', children: calendarSlaExample.startAt.format('YYYY-MM-DD HH:mm') },
                            { key: 'response', label: `${calendarPreviewResponseHoursValue} 工作小时响应截止`, children: calendarSlaExample.responseDeadline.format('YYYY-MM-DD HH:mm') },
                            { key: 'resolve', label: `${calendarPreviewResolveHoursValue} 工作小时解决截止`, children: calendarSlaExample.resolveDeadline.format('YYYY-MM-DD HH:mm') },
                          ]}
                        />
                        <Alert
                          showIcon
                          type="success"
                          style={{ marginTop: 16 }}
                          message="实际截止时间以后端 SlaDeadlineService 写入结果为准。"
                        />
                      </Card>
                    </Col>
                  </Row>
                </>
              )}
            </section>
          ) : activeMenu === '角色管理' ? (
            <section className="app-content role-management-page" aria-label="角色管理">
              <div className="content-title">
                <div>
                  <h1>角色管理</h1>
                  <p>创建业务角色，配置菜单权限、操作权限和默认数据范围；用户分配入口仍在用户管理。</p>
                </div>
                <div className="content-actions">
                  <button disabled={rolesLoading} onClick={() => void fetchRoles()} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                  <button className="primary-action" disabled={!canCreateRoles} onClick={openCreateRole} type="button">
                    <Plus size={16} />
                    新建角色
                  </button>
                </div>
              </div>

              {!canReadRoles ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无角色管理权限</strong>
                  <p>角色配置仅对具备系统管理权限的账号开放。</p>
                </div>
              ) : (
                <>
                  <div className="user-metrics">
                    <div className="user-metric">
                      <span>角色总数</span>
                      <strong>{rolesData?.total ?? '--'}</strong>
                      <small>{rolesData ? `${rolesData.systemCount} 个内置，${rolesData.customCount} 个自定义` : '后台角色总量'}</small>
                    </div>
                    <div className="user-metric">
                      <span>启用角色</span>
                      <strong>{rolesData?.enabledCount ?? '--'}</strong>
                      <small>可被分配给用户</small>
                    </div>
                    <div className="user-metric">
                      <span>权限项</span>
                      <strong>{(rolesData?.permissionTotal ?? flatPermissionNodes.length) || '--'}</strong>
                      <small>菜单与操作统一清单</small>
                    </div>
                    <div className="user-metric">
                      <span>关联用户</span>
                      <strong>{rolesData?.userTotal ?? '--'}</strong>
                      <small>当前已分配角色用户</small>
                    </div>
                  </div>

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
                            onChange={(event) => setRoleKeyword(event.target.value)}
                            placeholder="搜索角色"
                            type="search"
                            value={roleKeyword}
                          />
                        </label>
                        <label>
                          <span>状态</span>
                          <select onChange={(event) => setRoleEnabledFilter(event.target.value)} value={roleEnabledFilter}>
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
                              onClick={() => selectRole(role)}
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
                            <button disabled={!canEnableRoles || roleSaving} onClick={() => toggleRoleEnabled(selectedRole)} type="button">
                              {selectedRole.enabled ? <PowerOff size={15} /> : <Power size={15} />}
                              {selectedRole.enabled ? '停用' : '启用'}
                            </button>
                          )}
                          <button
                            className="primary-action"
                            disabled={selectedRoleReadonly || roleSaving || !roleForm.roleName.trim() || (roleDraftMode === 'create' ? !canCreateRoles : !canUpdateRoles)}
                            onClick={submitRoleBase}
                            type="button"
                          >
                            {roleSaving ? <Loader size={15} className="spin-icon" /> : <Check size={15} />}
                            {roleDraftMode === 'create' ? '创建角色' : '保存角色'}
                          </button>
                        </div>
                      </div>

                      <div className="role-protect-note">
                        当前阶段只开放自定义角色配置；管理员、客服处理人等内置角色可查看，不允许删除或改成不可用。
                      </div>

                      <div className="role-editor-form">
                        <label>
                          <span>角色名称</span>
                          <input
                            disabled={selectedRoleReadonly || roleSaving}
                            onChange={(event) => setRoleForm((value) => ({ ...value, roleName: event.target.value }))}
                            placeholder="工单质检"
                            value={roleForm.roleName}
                          />
                        </label>
                        <label>
                          <span>角色状态</span>
                          <select
                            disabled={selectedRoleReadonly || roleSaving}
                            onChange={(event) => setRoleForm((value) => ({ ...value, enabled: event.target.value === 'true' }))}
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
                            onChange={(event) => setRoleForm((value) => ({ ...value, roleDesc: event.target.value }))}
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
                        <span>当前阶段不支持指定邮箱和客户标签</span>
                      </div>
                      <div className="role-scope-grid">
                        {['TICKET', 'CUSTOMER', 'DASHBOARD'].map((resourceType) => {
                          const selectedScope = roleForm.dataScopes.find((scope) => scope.resourceType === resourceType)?.scopeCode || 'SELF'
                          return (
                            <div className="role-scope-card" key={resourceType}>
                              <strong>{dataResourceLabel(resourceType)}</strong>
                              <div>
                                {['SELF', 'ALL'].map((scopeCode) => (
                                  <button
                                    className={selectedScope === scopeCode ? 'active' : ''}
                                    disabled={selectedRoleReadonly || roleSaving || rolePermissionSaving || !canUpdateRolePermissions}
                                    key={scopeCode}
                                    onClick={() => updateRoleScope(resourceType, scopeCode)}
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
                        <button onClick={() => setActiveMenu('用户管理')} type="button">
                          <UserCog size={15} />
                          去用户管理
                        </button>
                        <button
                          className="primary-action"
                          disabled={selectedRoleReadonly || roleDraftMode === 'create' || !canUpdateRolePermissions || rolePermissionSaving}
                          onClick={submitRolePermissions}
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
          ) : activeMenu === '用户管理' ? (
            <section className="app-content user-page" aria-label="用户管理">
              <div className="content-title">
                <div>
                  <h1>用户管理</h1>
                  <p>维护后台账号、角色和启停状态；菜单、按钮和数据范围由权限清单统一生效。</p>
                </div>
                <div className="content-actions">
                  <button disabled={usersLoading} onClick={fetchUsers} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                  <button className="primary-action" disabled={!canCreateUsers} onClick={openCreateUser} type="button">
                    <Plus size={16} />
                    新建用户
                  </button>
                </div>
              </div>

              {!canReadUsers ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无用户管理权限</strong>
                  <p>非管理员仅可查看自己的个人信息，用户管理入口对处理人隐藏。</p>
                </div>
              ) : (
                <>
                  <div className="user-metrics">
                    <div className="user-metric">
                      <span>用户总数</span>
                      <strong>{usersData?.summary.totalUsers ?? '--'}</strong>
                      <small>后台账号总量</small>
                    </div>
                    <div className="user-metric">
                      <span>管理员</span>
                      <strong>{usersData?.summary.adminUsers ?? '--'}</strong>
                      <small>全部菜单和数据范围</small>
                    </div>
                    <div className="user-metric">
                      <span>处理人</span>
                      <strong>{usersData?.summary.agentUsers ?? '--'}</strong>
                      <small>自己负责和未分配池</small>
                    </div>
                    <div className="user-metric">
                      <span>权限项</span>
                      <strong>{rolesData?.permissionTotal ?? roleProfiles.ADMIN.permissionCount}</strong>
                      <small>当前内置权限清单</small>
                    </div>
                  </div>

                  <div className="role-overview">
                    {roleSelectOptions.slice(0, 4).map((role) => {
                      const profile = getRoleProfile(role.value)
                      return (
                        <div className={role.value === 'ADMIN' ? 'role-card admin' : 'role-card'} key={role.value}>
                          <div className="role-card__head">
                            <span className={role.value === 'ADMIN' ? 'role-pill admin' : 'role-pill'}>
                              {profile.title}
                            </span>
                            <strong>{profile.permissionCount} 项权限</strong>
                          </div>
                          <p>{profile.subtitle}</p>
                          <dl>
                            <div>
                              <dt>菜单范围</dt>
                              <dd>{profile.menuScope}</dd>
                            </div>
                            <div>
                              <dt>数据范围</dt>
                              <dd>{profile.dataScope}</dd>
                            </div>
                          </dl>
                        </div>
                      )
                    })}
                  </div>

                  <div className="user-toolbar">
                    <label className="user-search">
                      <Search size={16} />
                      <input
                        onChange={(event) => {
                          setUserKeyword(event.target.value)
                          setUserPage(1)
                        }}
                        placeholder="搜索账号、姓名、邮箱"
                        type="search"
                        value={userKeyword}
                      />
                    </label>
                    <label>
                      <span>角色</span>
                      <select
                        onChange={(event) => {
                          setUserRoleFilter(event.target.value)
                          setUserPage(1)
                        }}
                        value={userRoleFilter}
                      >
                        <option value="ALL">全部角色</option>
                        {roleSelectOptions.map((role) => (
                          <option key={role.value} value={role.value}>
                            {role.label}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label>
                      <span>状态</span>
                      <select
                        onChange={(event) => {
                          setUserEnabledFilter(event.target.value)
                          setUserPage(1)
                        }}
                        value={userEnabledFilter}
                      >
                        <option value="ALL">全部状态</option>
                        <option value="true">启用</option>
                        <option value="false">停用</option>
                      </select>
                    </label>
                    <button onClick={resetUserFilters} type="button">
                      <RotateCcw size={15} />
                      清空筛选
                    </button>
                  </div>

                  {usersError && <div className="user-alert">{usersError}</div>}

                  <div className="user-table-panel">
                    {usersLoading ? (
                      <div className="user-loading">
                        {[0, 1, 2, 3].map((item) => (
                          <span key={item} />
                        ))}
                      </div>
                    ) : usersData && usersData.records.length > 0 ? (
                      <table className="user-table">
                        <thead>
                          <tr>
                            <th>账号</th>
                            <th>姓名</th>
                            <th>邮箱</th>
                            <th>角色</th>
                            <th>菜单范围</th>
                            <th>数据范围</th>
                            <th>状态</th>
                            <th>最近登录</th>
                            <th>操作</th>
                          </tr>
                        </thead>
                        <tbody>
                          {usersData.records.map((managedUser) => (
                            <tr key={managedUser.id}>
                              <td>
                                <strong>{managedUser.account}</strong>
                                <small>ID {managedUser.id}</small>
                              </td>
                              <td>{managedUser.displayName}</td>
                              <td>{managedUser.email}</td>
                              <td>
                                <div className="role-pill-list">
                                  {normalizeRoleCodes(managedUser.roleCode, managedUser.roleCodes || []).map((roleCode) => (
                                    <span className={roleCode === 'ADMIN' ? 'role-pill admin' : 'role-pill'} key={roleCode}>
                                      {roleSelectOptions.find((role) => role.value === roleCode)?.label || roleLabel(roleCode)}
                                    </span>
                                  ))}
                                </div>
                              </td>
                              <td>{(managedUser.roleCodes?.length || 0) > 1 ? '按多角色合并' : getRoleProfile(managedUser.roleCode).menuScope}</td>
                              <td>{(managedUser.roleCodes?.length || 0) > 1 ? '按数据范围合并' : getRoleProfile(managedUser.roleCode).dataScope}</td>
                              <td>
                                <span className={managedUser.enabled ? 'state-pill enabled' : 'state-pill disabled'}>
                                  {managedUser.enabled ? '启用' : '停用'}
                                </span>
                              </td>
                              <td>{formatDateTime(managedUser.lastLoginAt)}</td>
                              <td>
                                <div className="user-ops">
                                  <button disabled={!canUpdateUsers} onClick={() => openEditUser(managedUser)} type="button">
                                    <Edit3 size={14} />
                                    编辑用户
                                  </button>
                                  <button disabled={!canResetUserPassword} onClick={() => openResetConfirm(managedUser)} type="button">
                                    <LockKeyhole size={14} />
                                    重置密码
                                  </button>
                                  <button
                                    className={managedUser.enabled ? 'danger' : 'success'}
                                    disabled={!canEnableUsers}
                                    onClick={() => openEnabledConfirm(managedUser)}
                                    type="button"
                                  >
                                    {managedUser.enabled ? <PowerOff size={14} /> : <Power size={14} />}
                                    {managedUser.enabled ? '停用' : '启用'}
                                  </button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    ) : (
                      <div className="empty-state">
                        <Users size={42} />
                        <strong>未找到用户</strong>
                        <p>可清空筛选后重新查询，或直接新建用户。</p>
                        <div>
                          <button onClick={resetUserFilters} type="button">清空筛选</button>
                          <button className="primary-action" disabled={!canCreateUsers} onClick={openCreateUser} type="button">新建用户</button>
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="user-pagination">
                    <span>
                      共 {usersData?.total ?? 0} 条，每页
                      <select
                        onChange={(event) => {
                          setUserPageSize(Number(event.target.value))
                          setUserPage(1)
                        }}
                        value={userPageSize}
                      >
                        <option value={10}>10</option>
                        <option value={20}>20</option>
                        <option value={50}>50</option>
                      </select>
                      条
                    </span>
                    <div>
                      <button disabled={userPage <= 1} onClick={() => setUserPage((value) => value - 1)} type="button">
                        上一页
                      </button>
                      <strong>
                        {usersData?.page ?? userPage} / {Math.max(usersData?.pages ?? 1, 1)}
                      </strong>
                      <button
                        disabled={!usersData || userPage >= Math.max(usersData.pages, 1)}
                        onClick={() => setUserPage((value) => value + 1)}
                        type="button"
                      >
                        下一页
                      </button>
                    </div>
                  </div>
                </>
              )}
            </section>
          ) : activeMenu === '邮箱配置' ? (
            <section className="app-content mailbox-page" aria-label="邮箱配置">
              <div className="content-title">
                <div>
                  <h1>邮箱管理中心</h1>
                </div>
                <div className="content-actions">
                  <button disabled={mailboxesLoading} onClick={fetchMailboxes} type="button">
                    <RefreshCw size={16} />
                    刷新数据
                  </button>
                  <button className="primary-action" onClick={openCreateMailbox} type="button">
                    <Plus size={16} />
                    新增邮箱
                  </button>
                </div>
              </div>

              {!isAdmin ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无邮箱配置管理权限</strong>
                  <p>邮箱账号、密码和连接测试仅管理员可维护，处理人入口隐藏。</p>
                </div>
              ) : (
                <>
                  <div className="user-metrics">
                    <div className="user-metric">
                      <div>
                        <span>邮箱总数</span>
                        <strong>{mailboxesData?.summary.totalMailboxes ?? '--'}</strong>
                        <small>已配置客服邮箱</small>
                      </div>
                      <i><Mail size={18} /></i>
                    </div>
                    <div className="user-metric">
                      <div>
                        <span>在线邮箱</span>
                        <strong>{mailboxesData?.summary.enabledMailboxes ?? '--'}</strong>
                        <small>IMAP/SMTP 正常</small>
                      </div>
                      <i className="green"><CircleCheck size={18} /></i>
                    </div>
                    <div className="user-metric">
                      <div>
                        <span>今日收件邮件</span>
                        <strong>{mailboxesData ? todayReceivedCount : '--'}</strong>
                        <small>最近拉取统计</small>
                      </div>
                      <i><Inbox size={18} /></i>
                    </div>
                    <div className="user-metric">
                      <div>
                        <span>今日自动建单</span>
                        <strong>{mailboxesData ? todayTicketCount : '--'}</strong>
                        <small>按拉取结果估算</small>
                      </div>
                      <i className="green"><Check size={18} /></i>
                    </div>
                    <div className="user-metric">
                      <div>
                        <span>异常邮箱</span>
                        <strong>{mailboxesData?.summary.errorMailboxes ?? '--'}</strong>
                        <small>连接测试失败</small>
                      </div>
                      <i className="red"><TriangleAlert size={18} /></i>
                    </div>
                    <div className="user-metric">
                      <div>
                        <span>同步任务</span>
                        <strong>{mailboxesData ? activeMailboxCount : '--'}</strong>
                        <small>{activeMailboxCount > 0 ? '正在运行' : '暂无运行任务'}</small>
                      </div>
                      <i className="orange"><RefreshCw size={18} /></i>
                    </div>
                  </div>

                  <div className="user-toolbar mailbox-toolbar">
                    <label className="user-search">
                      <Search size={16} />
                      <input
                        onChange={(event) => {
                          setMailboxKeyword(event.target.value)
                          setMailboxPage(1)
                        }}
                        placeholder="搜索邮箱名称、地址或服务器"
                        type="search"
                        value={mailboxKeyword}
                      />
                    </label>
                    <label>
                      <span>状态</span>
                      <select
                        onChange={(event) => {
                          setMailboxStatusFilter(event.target.value)
                          setMailboxPage(1)
                        }}
                        value={mailboxStatusFilter}
                      >
                        <option value="ALL">全部状态</option>
                        <option value="OK">连接正常</option>
                        <option value="ERROR">连接异常</option>
                        <option value="UNKNOWN">未测试</option>
                        <option value="DISABLED">已停用</option>
                      </select>
                    </label>
                    <button onClick={resetMailboxFilters} type="button">
                      <RotateCcw size={15} />
                      清空筛选
                    </button>
                  </div>

                  {mailboxesError && <div className="user-alert">{mailboxesError}</div>}

                  <div className="mailbox-layout">
                    <section className="mailbox-panel mailbox-list-panel">
                      <div className="mailbox-panel__head">
                        <div className="mailbox-head-copy">
                          <strong>邮箱账号管理</strong>
                        </div>
                        <span className="template-code-pill">{mailboxesLoading ? '加载中' : `${mailboxesData?.total ?? 0} 条`}</span>
                      </div>

                      {mailboxesLoading ? (
                        <div className="user-loading">
                          {[0, 1, 2, 3, 4].map((item) => (
                            <span key={item} />
                          ))}
                        </div>
                      ) : mailboxesData && mailboxesData.records.length > 0 ? (
                        <div className="mailbox-table-wrap">
                          <table className="user-table mailbox-table">
                            <thead>
                              <tr>
                                <th>邮箱名称 / 地址</th>
                                <th>收发服务器</th>
                                <th>状态 / 规则</th>
                                <th>今日收件</th>
                                <th>最近同步</th>
                                <th>操作</th>
                              </tr>
                            </thead>
                            <tbody>
                              {mailboxesData.records.map((mailbox, index) => (
                                <tr
                                  aria-selected={mailboxForm.id === mailbox.id}
                                  className={mailboxForm.id === mailbox.id ? 'mailbox-selectable-row selected-row' : 'mailbox-selectable-row'}
                                  key={mailbox.id}
                                  onClick={() => selectMailbox(mailbox)}
                                  onKeyDown={(event) => {
                                    if (event.key === 'Enter' || event.key === ' ') {
                                      event.preventDefault()
                                      selectMailbox(mailbox)
                                    }
                                  }}
                                  tabIndex={0}
                                >
                                  <td>
                                    <div className="mailbox-name-cell">
                                      <span style={{ background: mailboxRowColors[index % mailboxRowColors.length] }}>
                                        {mailbox.mailboxName.trim().charAt(0).toUpperCase() || 'M'}
                                      </span>
                                      <div>
                                        <strong>{mailbox.mailboxName}</strong>
                                        <small>{mailbox.emailAddress}</small>
                                      </div>
                                    </div>
                                  </td>
                                  <td>
                                    <div className="mailbox-protocols">
                                      <span className="state-pill status-unknown">IMAP</span>
                                      <span className="state-pill status-unknown">SMTP</span>
                                    </div>
                                    <small>{mailbox.imapHost}:{mailbox.imapPort} / {mailbox.smtpHost}:{mailbox.smtpPort}</small>
                                  </td>
                                  <td>
                                    <div className="mailbox-status-line">
                                      <i className={mailbox.connectionStatus === 'ERROR' ? 'red' : mailbox.connectionStatus === 'UNKNOWN' ? 'gray' : ''} />
                                      <strong className={mailbox.connectionStatus === 'ERROR' ? 'mailbox-mini-error' : ''}>
                                        {mailbox.enabled ? mailboxStatusLabel(mailbox.connectionStatus) : '停用'}
                                      </strong>
                                    </div>
                                    <small>{mailbox.enabled ? '客服中心 / 客服组' : '已停用 / 不拉取'}</small>
                                  </td>
                                  <td>
                                    <strong className={mailbox.connectionStatus === 'ERROR' ? 'mailbox-mini-error' : 'mailbox-strong-blue'}>
                                      {mailboxDailyCount(mailbox, index)}
                                    </strong>
                                  </td>
                                  <td>{formatSyncTime(mailbox.lastFetchAt)}</td>
                                  <td>
                                    <div className="user-ops mailbox-ops" onClick={(event) => event.stopPropagation()}>
                                      <button
                                        className={mailbox.enabled ? 'danger' : 'success'}
                                        onClick={() => openMailboxConfirm(mailbox, mailbox.enabled ? 'disable' : 'enable')}
                                        type="button"
                                      >
                                        {mailbox.enabled ? '停用' : '启用'}
                                      </button>
                                      <button className="danger" onClick={() => openMailboxConfirm(mailbox, 'delete')} type="button">
                                        删除
                                      </button>
                                    </div>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      ) : (
                        <div className="empty-state compact">
                          <Mail size={38} />
                          <strong>未找到邮箱</strong>
                          <p>可清空筛选重新查询，或新增第一个客服邮箱。</p>
                          <div>
                            <button onClick={resetMailboxFilters} type="button">清空筛选</button>
                            <button className="primary-action" onClick={openCreateMailbox} type="button">新增邮箱</button>
                          </div>
                        </div>
                      )}

                      <div className="user-pagination mailbox-pagination">
                        <span>
                          共 {mailboxesData?.total ?? 0} 条，每页
                          <select
                            onChange={(event) => {
                              setMailboxPageSize(Number(event.target.value))
                              setMailboxPage(1)
                            }}
                            value={mailboxPageSize}
                          >
                            <option value={10}>10</option>
                            <option value={20}>20</option>
                            <option value={50}>50</option>
                          </select>
                          条
                        </span>
                        <div>
                          <button disabled={mailboxPage <= 1} onClick={() => setMailboxPage((value) => value - 1)} type="button">
                            上一页
                          </button>
                          <strong>
                            {mailboxesData?.page ?? mailboxPage} / {Math.max(mailboxesData?.pages ?? 1, 1)}
                          </strong>
                          <button
                            disabled={!mailboxesData || mailboxPage >= Math.max(mailboxesData.pages, 1)}
                            onClick={() => setMailboxPage((value) => value + 1)}
                            type="button"
                          >
                            下一页
                          </button>
                        </div>
                      </div>
                    </section>

                    <section className="mailbox-panel mailbox-editor-panel">
                      <div className="mailbox-panel__head">
                        <div className="template-editor-title">
                          <strong>{mailboxForm.id ? '编辑邮箱配置' : '新增邮箱配置'}</strong>
                          <span className={mailboxDirty ? 'template-code-pill dirty' : 'template-code-pill'}>
                            {mailboxForm.id ? (mailboxDirty ? '未保存' : '已保存') : '草稿'}
                          </span>
                        </div>
                      </div>

                      <div className="mailbox-editor">
                        <div className="mailbox-step-tabs" role="tablist" aria-label="邮箱配置步骤">
                          {mailboxSteps.map((step) => (
                            <button
                              aria-selected={activeMailboxStep === step.key}
                              className={activeMailboxStep === step.key ? 'mailbox-step active' : 'mailbox-step'}
                              key={step.key}
                              onClick={() => setActiveMailboxStep(step.key)}
                              role="tab"
                              type="button"
                            >
                              <span>{step.label}</span>
                            </button>
                          ))}
                        </div>

                        <div className="mailbox-step-body">
                          {activeMailboxStep === 'basic' && (
                            <div className="mailbox-form-grid">
                              <label>
                                <span><b className="required">*</b> 邮箱名称</span>
                                <input
                                  onChange={(event) => updateMailboxForm({ mailboxName: event.target.value })}
                                  placeholder="例如 客服支持邮箱"
                                  value={mailboxForm.mailboxName}
                                />
                              </label>
                              <label>
                                <span><b className="required">*</b> 邮箱地址</span>
                                <input
                                  onChange={(event) => updateMailboxForm({ emailAddress: event.target.value })}
                                  placeholder="support@example.com"
                                  type="email"
                                  value={mailboxForm.emailAddress}
                                />
                              </label>
                              <label>
                                <span>默认处理人</span>
                                <select
                                  onChange={(event) => updateMailboxForm({ defaultAssigneeId: event.target.value })}
                                  value={mailboxForm.defaultAssigneeId}
                                >
                                  <option value="">按分配规则处理</option>
                                  {mailboxAssignees.map((assignee) => (
                                    <option key={assignee.id} value={assignee.id}>
                                      {assignee.displayName} / {assignee.account}
                                    </option>
                                  ))}
                                </select>
                                <small>为空时后续按自动分配规则选择处理人。</small>
                              </label>
                              <label>
                                <span>启用状态</span>
                                <button
                                  className={mailboxForm.enabled ? 'template-switch enabled' : 'template-switch'}
                                  onClick={() => updateMailboxForm({ enabled: !mailboxForm.enabled })}
                                  type="button"
                                >
                                  <span>{mailboxForm.enabled ? '启用后参与邮箱拉取' : '停用后不拉取邮件'}</span>
                                  <i />
                                </button>
                              </label>
                            </div>
                          )}

                          {activeMailboxStep === 'imap' && (
                            <>
                              <div className="mailbox-form-grid">
                                <label>
                                  <span><b className="required">*</b> IMAP 服务器</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ imapHost: event.target.value })}
                                    placeholder="imap.example.com"
                                    value={mailboxForm.imapHost}
                                  />
                                </label>
                                <label>
                                  <span><b className="required">*</b> 端口</span>
                                  <input
                                    inputMode="numeric"
                                    onChange={(event) => updateMailboxForm({ imapPort: Number(event.target.value.replace(/\D/g, '') || 0) })}
                                    placeholder="993"
                                    value={mailboxForm.imapPort}
                                  />
                                </label>
                                <label>
                                  <span>SSL</span>
                                  <button
                                    className={mailboxForm.imapSslEnabled ? 'template-switch enabled' : 'template-switch'}
                                    onClick={() => updateMailboxForm({ imapSslEnabled: !mailboxForm.imapSslEnabled })}
                                    type="button"
                                  >
                                    <span>{mailboxForm.imapSslEnabled ? '启用' : '关闭'}</span>
                                    <i />
                                  </button>
                                </label>
                                <label>
                                  <span>收件文件夹</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ imapFolder: event.target.value })}
                                    placeholder="INBOX"
                                    value={mailboxForm.imapFolder}
                                  />
                                </label>
                                <label>
                                  <span><b className="required">*</b> IMAP 账号</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ imapUsername: event.target.value })}
                                    placeholder="通常为邮箱地址"
                                    value={mailboxForm.imapUsername}
                                  />
                                </label>
                                <label>
                                  <span><b className="required">*</b> 密码 / 授权码</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ imapPassword: event.target.value })}
                                    placeholder={mailboxForm.id ? '为空则不修改' : '新建时必填'}
                                    type="password"
                                    value={mailboxForm.imapPassword}
                                  />
                                </label>
                              </div>
                            </>
                          )}

                          {activeMailboxStep === 'smtp' && (
                            <>
                              <div className="mailbox-form-grid">
                                <label>
                                  <span><b className="required">*</b> SMTP 服务器</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ smtpHost: event.target.value })}
                                    placeholder="smtp.example.com"
                                    value={mailboxForm.smtpHost}
                                  />
                                </label>
                                <label>
                                  <span><b className="required">*</b> 端口</span>
                                  <input
                                    inputMode="numeric"
                                    onChange={(event) => updateMailboxForm({ smtpPort: Number(event.target.value.replace(/\D/g, '') || 0) })}
                                    placeholder="587"
                                    value={mailboxForm.smtpPort}
                                  />
                                </label>
                                <label>
                                  <span>SSL/TLS</span>
                                  <button
                                    className={mailboxForm.smtpSslEnabled ? 'template-switch enabled' : 'template-switch'}
                                    onClick={() => updateMailboxForm({ smtpSslEnabled: !mailboxForm.smtpSslEnabled })}
                                    type="button"
                                  >
                                    <span>{mailboxForm.smtpSslEnabled ? '启用' : '关闭'}</span>
                                    <i />
                                  </button>
                                </label>
                                <label>
                                  <span>发件人显示名</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ smtpFromName: event.target.value })}
                                    placeholder="客服支持中心"
                                    value={mailboxForm.smtpFromName}
                                  />
                                </label>
                                <label>
                                  <span><b className="required">*</b> SMTP 账号</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ smtpUsername: event.target.value })}
                                    placeholder="通常为邮箱地址"
                                    value={mailboxForm.smtpUsername}
                                  />
                                </label>
                                <label>
                                  <span><b className="required">*</b> 密码 / 授权码</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ smtpPassword: event.target.value })}
                                    placeholder={mailboxForm.id ? '为空则不修改' : '新建时必填'}
                                    type="password"
                                    value={mailboxForm.smtpPassword}
                                  />
                                </label>
                              </div>
                            </>
                          )}

                          {activeMailboxStep === 'reply' && (
                            <>
                              <div className="mailbox-form-grid">
                                <label>
                                  <span>拉取频率（秒）</span>
                                  <input
                                    min={60}
                                    max={1800}
                                    onChange={(event) => updateMailboxForm({ fetchIntervalSec: Number(event.target.value || 120) })}
                                    type="number"
                                    value={mailboxForm.fetchIntervalSec}
                                  />
                                  <small>建议 60-1800 秒，过短会增加邮箱服务压力。</small>
                                </label>
                                <label>
                                  <span>自动回执</span>
                                  <button
                                    className={mailboxForm.autoReplyEnabled ? 'template-switch enabled' : 'template-switch'}
                                    onClick={() => updateMailboxForm({ autoReplyEnabled: !mailboxForm.autoReplyEnabled })}
                                    type="button"
                                  >
                                    <span>{mailboxForm.autoReplyEnabled ? '启用自动回执' : '不发送自动回执'}</span>
                                    <i />
                                  </button>
                                </label>
                                <label>
                                  <span>回执模板</span>
                                  <select
                                    onChange={(event) => updateMailboxForm({ autoReplyTemplateId: event.target.value })}
                                    value={mailboxForm.autoReplyTemplateId}
                                  >
                                    <option value="">使用默认自动回执模板</option>
                                  </select>
                                  <small>后续如需多模板选择，再接通知模板下拉。</small>
                                </label>
                              </div>
                            </>
                          )}

                          {activeMailboxStep === 'test' && (
                            <div className="mailbox-test-panel">
                              <div className="mailbox-test-grid">
                                <div>
                                  <span>收信服务</span>
                                  <strong>{mailboxForm.imapHost || '未填写'}:{mailboxForm.imapPort || '-'}</strong>
                                  <small>{mailboxForm.imapUsername || 'IMAP 账号未填写'} / {mailboxForm.imapFolder || 'INBOX'}</small>
                                </div>
                                <div>
                                  <span>发信服务</span>
                                  <strong>{mailboxForm.smtpHost || '未填写'}:{mailboxForm.smtpPort || '-'}</strong>
                                  <small>{mailboxForm.smtpUsername || 'SMTP 账号未填写'} / {mailboxForm.smtpFromName || '默认发件人'}</small>
                                </div>
                                <div>
                                  <span>处理策略</span>
                                  <strong>{mailboxForm.fetchIntervalSec || 120} 秒拉取</strong>
                                  <small>{mailboxForm.autoReplyEnabled ? '启用自动回执' : '关闭自动回执'}</small>
                                </div>
                              </div>

                              {mailboxTestResult ? (
                                <div className={mailboxTestResult.success ? 'mailbox-test-result ok' : 'mailbox-test-result error'}>
                                  <strong>{mailboxTestResult.success ? '连接测试通过' : '连接测试未通过'}</strong>
                                  <span>{mailboxTestResult.imapMessage}</span>
                                  <span>{mailboxTestResult.smtpMessage}</span>
                                </div>
                              ) : (
                                <div className="mailbox-test-empty">
                                  保存前建议完成收信和发信测试；编辑时密码为空会沿用原授权码。
                                </div>
                              )}
                            </div>
                          )}
                        </div>

                        <div className="mailbox-actions">
                          <span>{mailboxForm.id ? '编辑时密码为空会沿用原授权码。' : '新建邮箱保存前需填写收信和发信授权码。'}</span>
                          <div>
                            <button disabled={activeMailboxStepIndex <= 0} onClick={() => moveMailboxStep(-1)} type="button">
                              上一步
                            </button>
                            {activeMailboxStep === 'test' ? (
                              <>
                                <button disabled={mailboxTesting} onClick={() => void testMailboxConnection('IMAP')} type="button">
                                  测试收信
                                </button>
                                <button disabled={mailboxTesting} onClick={() => void testMailboxConnection('SMTP')} type="button">
                                  测试发信
                                </button>
                                <button disabled={mailboxTesting} onClick={() => void testMailboxConnection('ALL')} type="button">
                                  {mailboxTesting ? '测试中...' : '测试全部'}
                                </button>
                              </>
                            ) : (
                              <button onClick={() => moveMailboxStep(1)} type="button">
                                下一步
                              </button>
                            )}
                            <button className="primary-action" disabled={mailboxSaving} onClick={() => void saveMailbox()} type="button">
                              <Check size={16} />
                              {mailboxSaving ? '保存中...' : '保存配置'}
                            </button>
                          </div>
                        </div>
                      </div>
                    </section>
                  </div>

                  <div className="mailbox-below">
                    <section className="mailbox-panel mailbox-mini-panel">
                      <div className="mailbox-panel__head">
                        <strong>同步任务状态</strong>
                        <button className="template-head-action" type="button">全部任务</button>
                      </div>
                      {mailboxTaskRows.length > 0 ? (
                        <div className="mailbox-mini-table-wrap">
                          <table className="mailbox-mini-table">
                            <thead>
                              <tr>
                                <th>邮箱账号</th>
                                <th>状态</th>
                                <th>任务类型</th>
                                <th>开始时间</th>
                                <th>下次执行</th>
                                <th>进度</th>
                              </tr>
                            </thead>
                            <tbody>
                              {mailboxTaskRows.map((task) => (
                                <tr key={task.id}>
                                  <td>{task.mailboxName}</td>
                                  <td>
                                    <span className={`state-pill ${task.statusClass}`}>{task.status}</span>
                                  </td>
                                  <td>{task.taskType}</td>
                                  <td>{task.startTime}</td>
                                  <td>{task.nextRun}</td>
                                  <td>
                                    {task.status === '失败' ? (
                                      <span className="mailbox-mini-error">{task.progress}</span>
                                    ) : (
                                      <div className="mailbox-progress" aria-label={`任务进度 ${task.progress}`}>
                                        <span style={{ width: task.progress }} />
                                      </div>
                                    )}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      ) : (
                        <div className="mailbox-mini-empty">暂无同步任务，新增并启用邮箱后展示后台拉取状态。</div>
                      )}
                    </section>

                    <section className="mailbox-panel mailbox-mini-panel">
                      <div className="mailbox-panel__head">
                        <strong>邮件拉取日志</strong>
                        <button className="template-head-action" type="button">查看全部</button>
                      </div>
                      {mailboxLogRows.length > 0 ? (
                        <div className="mailbox-mini-table-wrap">
                          <table className="mailbox-mini-table">
                            <thead>
                              <tr>
                                <th>时间</th>
                                <th>邮箱账号</th>
                                <th>动作</th>
                                <th>结果</th>
                                <th>数量</th>
                                <th>关联工单</th>
                              </tr>
                            </thead>
                            <tbody>
                              {mailboxLogRows.map((log) => (
                                <tr key={log.id}>
                                  <td>{log.time}</td>
                                  <td>{log.mailboxName}</td>
                                  <td>{log.action}</td>
                                  <td>
                                    <span className={`state-pill ${log.resultClass}`}>{log.result}</span>
                                  </td>
                                  <td>{log.count}</td>
                                  <td>{log.relatedTickets}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      ) : (
                        <div className="mailbox-mini-empty">暂无拉取日志，后续拉取任务执行后展示最近记录。</div>
                      )}
                    </section>
                  </div>
                </>
              )}
            </section>
          ) : activeMenu === '收件记录' ? (
            <div style={{ padding: '24px', overflow: 'auto', height: '100%' }}>
              {/* 标题 + 刷新 */}
              <Row justify="space-between" align="middle" style={{ marginBottom: 20 }}>
                <Typography.Title level={4} style={{ margin: 0 }}>拉取日志</Typography.Title>
                <Button icon={<ReloadOutlined />} onClick={() => void fetchFetchLogs()} loading={fetchLogsLoading}>刷新数据</Button>
              </Row>

              {/* 统计卡片 */}
              {fetchLogStats && (
                <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
                  {[
                    { label: '拉取次数', value: fetchLogStats.totalCount, sub: '含定时与手动触发' },
                    { label: '成功任务', value: fetchLogStats.successCount, sub: 'IMAP 连接与解析正常' },
                    { label: '失败任务', value: fetchLogStats.failCount, sub: '可查看原因并重试' },
                    { label: '新建工单', value: fetchLogStats.totalCreatedTickets, sub: '由成功拉取任务创建' },
                  ].map((card, i) => (
                    <Col key={i} xs={24} sm={12} lg={6}>
                      <Card size="small" styles={{ body: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12, padding: '14px 16px' } }}>
                        <div style={{ flex: 1 }}>
                          <div style={{ fontSize: 13, color: '#8c8c8c', fontWeight: 600 }}>{card.label}</div>
                          <div style={{ fontSize: 28, fontWeight: 800, lineHeight: 1.2, marginTop: 6 }}>{card.value}</div>
                          <div style={{ fontSize: 12, color: '#bfbfbf', marginTop: 4 }}>{card.sub}</div>
                        </div>
                        <div style={{
                          width: 36, height: 36, borderRadius: 10,
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          fontWeight: 900, fontSize: 16,
                          background: ['#eff6ff', '#ecfdf5', '#fef2f2', '#fffbeb'][i],
                          color: ['#2563eb', '#10b981', '#ef4444', '#f59e0b'][i],
                        }}>
                          {['F','O','E','T'][i]}
                        </div>
                      </Card>
                    </Col>
                  ))}
                </Row>
              )}

              {/* 筛选栏 */}
              <Card size="small" style={{ marginBottom: 16 }}>
                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1.5fr 3fr auto', alignItems: 'center', gap: 16 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>邮箱</span>
                    <Select value={fetchLogMailboxFilter || undefined} onChange={v => { setFetchLogMailboxFilter(v || ''); setFetchLogPage(1) }} placeholder="全部邮箱" allowClear style={{ width: '100%' }} options={mailboxes.map(m => ({ value: String(m.id), label: m.mailboxName }))} />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>结果</span>
                    <Select value={fetchLogSuccessFilter === 'ALL' ? undefined : fetchLogSuccessFilter} onChange={v => { setFetchLogSuccessFilter(v ?? 'ALL'); setFetchLogPage(1) }} placeholder="全部结果" allowClear style={{ width: '100%' }} options={[{ value: 'true', label: '成功' }, { value: 'false', label: '失败' }]} />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>时间</span>
                    <DatePicker.RangePicker
                      showTime={{ format: 'HH:mm' }}
                      format="YYYY-MM-DD HH:mm"
                      style={{ width: '100%' }}
                      value={fetchLogStartFrom && fetchLogStartTo ? [dayjs(fetchLogStartFrom), dayjs(fetchLogStartTo)] : null}
                      onChange={(dates) => {
                        setFetchLogStartFrom(dates?.[0]?.format('YYYY-MM-DDTHH:mm:ss') || '')
                        setFetchLogStartTo(dates?.[1]?.format('YYYY-MM-DDTHH:mm:ss') || '')
                        setFetchLogPage(1)
                      }}
                    />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                    <Button onClick={() => { setFetchLogMailboxFilter(''); setFetchLogSuccessFilter('ALL'); setFetchLogStartFrom(''); setFetchLogStartTo(''); setFetchLogPage(1) }}>清空筛选</Button>
                    <Button type="primary" icon={<SearchOutlined />} onClick={() => { setFetchLogPage(1); void fetchFetchLogs() }}>查询</Button>
                  </div>
                </div>
              </Card>

              {/* 错误提示 */}
              {fetchLogsError && (
                <Card size="small" style={{ marginBottom: 16, borderColor: '#ffccc7', background: '#fff2f0' }}>
                  <div style={{ textAlign: 'center', padding: '8px 0' }}>
                    <div style={{ fontWeight: 600, marginBottom: 4 }}>日志查询失败</div>
                    <Typography.Text type="secondary">{fetchLogsError}</Typography.Text>
                    <div style={{ marginTop: 8 }}><Button size="small" onClick={() => void fetchFetchLogs()}>重新加载</Button></div>
                  </div>
                </Card>
              )}

              {/* 表格 */}
              <Card size="small" title={<span style={{ fontSize: 14 }}>IMAP 拉取任务记录</span>} extra={fetchLogsData && <Tag color="blue">共 {fetchLogsData.total} 条</Tag>}>
                <Table<MailFetchLog>
                  rowKey="id"
                  dataSource={fetchLogsData?.records}
                  loading={fetchLogsLoading}
                  locale={{ emptyText: <Empty description="未找到拉取日志" /> }}
                  pagination={{
                    current: fetchLogPage,
                    pageSize: fetchLogPageSize,
                    total: fetchLogsData?.total || 0,
                    showSizeChanger: true,
                    pageSizeOptions: ['10', '20'],
                    showTotal: (total) => `共 ${total} 条`,
                    onChange: (page, size) => { setFetchLogPage(page); setFetchLogPageSize(size) },
                  }}
                  onRow={(record) => ({
                    onClick: () => setFetchLogDetail(record),
                    style: { cursor: 'pointer' },
                  })}
                  columns={[
                    { title: '#', width: 50, render: (_: unknown, __: unknown, index: number) => (fetchLogPage - 1) * fetchLogPageSize + index + 1 },
                    { title: '开始时间', dataIndex: 'startedAt', render: (v: string) => formatDateTime(v), width: 160 },
                    { title: '邮箱地址', dataIndex: 'emailAddress', render: (v: string) => v || '-', width: 180 },
                    { title: '邮箱名称', dataIndex: 'mailboxName', render: (v: string) => v || '-', width: 120 },
                    { title: '结果', dataIndex: 'success', width: 80, render: (v: boolean) => v ? <Tag color="success">成功</Tag> : <Tag color="error">失败</Tag> },
                    { title: '拉取数', dataIndex: 'fetchedCount', width: 70 },
                    { title: '新建工单', dataIndex: 'createdTicketCount', width: 80 },
                    { title: '关联工单', dataIndex: 'linkedCount', width: 80 },
                    { title: '耗时', width: 80, render: (_, r) => r.finishedAt ? formatDuration(r.startedAt, r.finishedAt) : '-' },
                    { title: '错误摘要', dataIndex: 'errorMessage', width: 200, ellipsis: true, render: (v: string) => v ? <Typography.Text type="danger" style={{ fontSize: 12 }}>{v}</Typography.Text> : '' },
                    { title: '操作', width: 60, render: (_, r) => <Button type="link" size="small" onClick={e => { e.stopPropagation(); setFetchLogDetail(r) }}>详情</Button> },
                  ]}
                  scroll={{ x: 1050 }}
                  size="middle"
                />
              </Card>

              {/* 详情抽屉 */}
              <Drawer
                title={<span style={{ fontSize: 16, fontWeight: 700 }}>拉取任务详情</span>}
                placement="right"
                width={520}
                onClose={() => setFetchLogDetail(null)}
                open={!!fetchLogDetail}
                extra={<Button size="small" onClick={() => setFetchLogDetail(null)} icon={<CloseOutlined />}>关闭</Button>}
              >
                {fetchLogDetail && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                    {/* 状态头部 */}
                    <div style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      background: '#fafafa', borderRadius: 10, padding: '14px 18px',
                      border: '1px solid #f0f0f0',
                    }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <span style={{ fontSize: 12, color: '#8c8c8c', fontWeight: 500 }}>任务编号</span>
                        <span style={{ fontWeight: 700, fontSize: 15, color: '#262626' }}>{fetchLogDetail.id}</span>
                        <span style={{ width: 1, height: 20, background: '#e8e8e8' }} />
                        <Tag style={{ margin: 0 }} color={fetchLogDetail.success ? 'success' : 'error'}>
                          {fetchLogDetail.success ? '成功' : '失败'}
                        </Tag>
                      </div>
                      <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                        {fetchLogDetail.triggerType === 'SCHEDULED' ? '⏱ 定时任务' : '👤 手动触发'}
                      </span>
                    </div>

                    {/* 邮箱信息 */}
                    <Card size="small" title={<span style={{ fontSize: 13, fontWeight: 600 }}>📬 邮箱信息</span>}
                      styles={{ body: { padding: '12px 16px' } }}>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                        {[
                          ['邮箱地址', fetchLogDetail.emailAddress || '-'],
                          ['邮箱名称', fetchLogDetail.mailboxName || '-'],
                          ['触发方式', fetchLogDetail.triggerType === 'SCHEDULED' ? '定时任务' : '手动触发'],
                          ['开始时间', formatDateTime(fetchLogDetail.startedAt)],
                          ['结束时间', fetchLogDetail.finishedAt ? formatDateTime(fetchLogDetail.finishedAt) : '-'],
                          ['耗时', fetchLogDetail.finishedAt ? formatDuration(fetchLogDetail.startedAt, fetchLogDetail.finishedAt) : '-'],
                        ].map(([label, value]) => (
                          <div key={label}>
                            <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 2 }}>{label}</div>
                            <div style={{ fontWeight: 600, fontSize: 13 }}>{value}</div>
                          </div>
                        ))}
                      </div>
                    </Card>

                    {/* 执行结果 */}
                    <Card size="small" title={<span style={{ fontSize: 13, fontWeight: 600 }}>📊 执行结果</span>}
                      styles={{ body: { padding: '12px 16px' } }}>
                      <Row gutter={[12, 12]}>
                        <Col span={8}>
                          <div style={{ background: '#f6ffed', borderRadius: 8, padding: '10px 14px', textAlign: 'center' }}>
                            <div style={{ fontSize: 22, fontWeight: 800, color: '#52c41a' }}>{fetchLogDetail.fetchedCount}</div>
                            <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 2 }}>拉取邮件数</div>
                          </div>
                        </Col>
                        <Col span={8}>
                          <div style={{ background: '#e6f7ff', borderRadius: 8, padding: '10px 14px', textAlign: 'center' }}>
                            <div style={{ fontSize: 22, fontWeight: 800, color: '#1890ff' }}>{fetchLogDetail.createdTicketCount}</div>
                            <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 2 }}>新建工单</div>
                          </div>
                        </Col>
                        <Col span={8}>
                          <div style={{ background: '#fff7e6', borderRadius: 8, padding: '10px 14px', textAlign: 'center' }}>
                            <div style={{ fontSize: 22, fontWeight: 800, color: '#fa8c16' }}>{fetchLogDetail.linkedCount}</div>
                            <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 2 }}>关联工单</div>
                          </div>
                        </Col>
                      </Row>
                    </Card>

                    {/* 错误详情 */}
                    {fetchLogDetail.errorMessage && (
                      <Card size="small"
                        styles={{
                          header: { background: '#fff2f0', borderBottom: '1px solid #ffccc7' },
                          body: { padding: '12px 16px', background: '#fff2f0' },
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                          <span style={{ color: '#ff4d4f', fontWeight: 700, fontSize: 13 }}>⚠ 错误详情</span>
                        </div>
                        <pre style={{
                          fontSize: 12, whiteSpace: 'pre-wrap', maxHeight: 200, overflow: 'auto',
                          margin: 0, color: '#cf1322', background: '#fff', borderRadius: 6,
                          padding: 10, border: '1px solid #ffccc7', lineHeight: 1.6,
                        }}>{fetchLogDetail.errorMessage}</pre>
                      </Card>
                    )}
                  </div>
                )}
              </Drawer>
            </div>
          ) : activeMenu === '发件记录' ? (
            <div style={{ padding: 24, overflow: 'auto', height: '100%' }}>
              {/* 页面标题 + 刷新 */}
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, marginBottom: 18 }}>
                <h1 style={{ margin: 0, fontSize: 28, fontWeight: 800, letterSpacing: '-0.03em' }}>发送日志</h1>
                <Button icon={<ReloadOutlined />} onClick={() => { void fetchSendLogs(); void fetchSendLogStats() }} loading={sendLogsLoading}>刷新数据</Button>
              </div>

              {/* 统计卡片 */}
              {sendLogStats && (
                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                  {[
                    { label: '发送次数', value: sendLogStats.totalCount, sub: '含测试与自动发送', icon: 'S', cls: '#2563eb', bg: '#eff6ff' },
                    { label: '成功任务', value: sendLogStats.successCount, sub: 'SMTP 发送正常', icon: 'E', cls: '#10b981', bg: '#ecfdf5' },
                    { label: '失败任务', value: sendLogStats.failCount, sub: '可查看原因并重试', icon: 'D', cls: '#ef4444', bg: '#fef2f2' },
                    { label: '发送中', value: sendLogsData ? sendLogsData.total - sendLogStats.successCount - sendLogStats.failCount : 0, sub: '待发送与重试中', icon: 'N', cls: '#f59e0b', bg: '#fffbeb' },
                  ].map((card, i) => (
                    <Col key={i} xs={24} sm={12} lg={6}>
                      <Card size="small" styles={{ body: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12, padding: '14px 16px' } }}>
                        <div style={{ flex: 1 }}>
                          <span style={{ fontSize: 13, color: '#4b5563', fontWeight: 650 }}>{card.label}</span>
                          <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: '-0.04em', marginTop: 8 }}>{card.value}</div>
                          <small style={{ display: 'block', marginTop: 6, color: '#9ca3af', fontSize: 12 }}>{card.sub}</small>
                        </div>
                        <div style={{ width: 36, height: 36, borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 900, fontSize: 16, background: card.bg, color: card.cls }}>{card.icon}</div>
                      </Card>
                    </Col>
                  ))}
                </Row>
              )}

              {/* 错误提示 */}
              {sendLogsError && (
                <Card size="small" style={{ marginBottom: 16, borderColor: '#ffccc7', background: '#fff2f0' }}>
                  <div style={{ textAlign: 'center', padding: '8px 0' }}>
                    <div style={{ fontWeight: 600, marginBottom: 4 }}>日志查询失败</div>
                    <Typography.Text type="secondary">{sendLogsError}</Typography.Text>
                    <div style={{ marginTop: 8 }}><Button size="small" onClick={() => void fetchSendLogs()}>重新加载</Button></div>
                  </div>
                </Card>
              )}

              {/* 面板 */}
              <div style={{ border: '1px solid #e5e7eb', borderRadius: 16, background: '#fff', boxShadow: '0 12px 30px rgba(15,23,42,0.06)', overflow: 'hidden' }}>
                {/* 面板头 */}
                <div style={{ padding: '14px 18px', borderBottom: '1px solid #f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <strong style={{ fontSize: 15 }}>SMTP 发送任务记录</strong>
                  {sendLogsData && <span style={{ display: 'inline-flex', alignItems: 'center', borderRadius: 999, padding: '4px 10px', background: '#eff6ff', color: '#2563eb', fontSize: 12, fontWeight: 800 }}>共 {sendLogsData.total} 条</span>}
                </div>

                {/* 筛选栏 */}
                <div style={{ padding: '14px 18px', borderBottom: '1px solid #f1f5f9', display: 'grid', gridTemplateColumns: '2fr 1.5fr 1.5fr 3fr auto auto', gap: 12, alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>邮箱</span>
                    <Select value={sendLogMailboxFilter || undefined} onChange={v => { setSendLogMailboxFilter(v || ''); setSendLogPage(1) }} placeholder="全部邮箱" allowClear style={{ width: '100%' }} options={mailboxes.map(m => ({ value: String(m.id), label: m.mailboxName }))} />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>类型</span>
                    <Select value={sendLogTypeFilter === 'ALL' ? undefined : sendLogTypeFilter} onChange={v => { setSendLogTypeFilter(v ?? 'ALL'); setSendLogPage(1) }} placeholder="全部类型" allowClear style={{ width: '100%' }} options={[{ value: 'TEST', label: '测试' }, { value: 'AUTO_REPLY', label: '自动回执' }, { value: 'ASSIGN_NOTIFY', label: '分配通知' }, { value: 'AGENT_REPLY', label: '客服回复' }]} />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>状态</span>
                    <Select value={sendLogStatusFilter === 'ALL' ? undefined : sendLogStatusFilter} onChange={v => { setSendLogStatusFilter(v ?? 'ALL'); setSendLogPage(1) }} placeholder="全部状态" allowClear style={{ width: '100%' }} options={[{ value: 'SUCCESS', label: '成功' }, { value: 'FAILED', label: '失败' }, { value: 'PENDING', label: '待发送' }, { value: 'RETRYING', label: '重试中' }]} />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>时间</span>
                    <DatePicker.RangePicker
                      showTime={{ format: 'HH:mm' }} format="YYYY-MM-DD HH:mm" style={{ width: '100%' }}
                      value={sendLogStartFrom && sendLogStartTo ? [dayjs(sendLogStartFrom), dayjs(sendLogStartTo)] : null}
                      onChange={(dates) => { setSendLogStartFrom(dates?.[0]?.format('YYYY-MM-DDTHH:mm:ss') || ''); setSendLogStartTo(dates?.[1]?.format('YYYY-MM-DDTHH:mm:ss') || ''); setSendLogPage(1) }} />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                    <Button onClick={() => { setSendLogMailboxFilter(''); setSendLogTypeFilter('ALL'); setSendLogStatusFilter('ALL'); setSendLogStartFrom(''); setSendLogStartTo(''); setSendLogPage(1) }}>清空筛选</Button>
                    <Button type="primary" icon={<SearchOutlined />} onClick={() => { setSendLogPage(1); void fetchSendLogs() }}>查询</Button>
                  </div>
                </div>

                {/* 表格 */}
                <Table<MailSendLog>
                  rowKey="id"
                  dataSource={sendLogsData?.records}
                  loading={sendLogsLoading}
                  locale={{ emptyText: <Empty description="未找到发送日志，请检查邮件发送配置是否已启用。" /> }}
                  pagination={{
                    current: sendLogPage,
                    pageSize: sendLogPageSize,
                    total: sendLogsData?.total || 0,
                    showSizeChanger: true,
                    pageSizeOptions: ['10', '20'],
                    showTotal: (total) => `共 ${total} 条`,
                    onChange: (page, size) => { setSendLogPage(page); setSendLogPageSize(size) },
                  }}
                  onRow={(record) => ({ onClick: () => setSendLogDetail(record), style: { cursor: 'pointer' } })}
                  columns={[
                    { title: '#', width: 50, render: (_: unknown, __: unknown, index: number) => (sendLogPage - 1) * sendLogPageSize + index + 1 },
                    { title: '发送时间', dataIndex: 'createdAt', render: (v: string) => v?.replace('T', ' ').slice(0, 16) || '-', width: 150 },
                    { title: '收件人', dataIndex: 'toAddress', width: 180 },
                    { title: '主题', dataIndex: 'subject', ellipsis: true, width: 250 },
                    { title: '类型', dataIndex: 'sendType', width: 90, render: (v: string) => {
                      const labels: Record<string, string> = { 'TEST': '测试', 'AUTO_REPLY': '自动回执', 'ASSIGN_NOTIFY': '分配通知', 'AGENT_REPLY': '客服回复' }
                      return labels[v] || v
                    }},
                    { title: '状态', dataIndex: 'sendStatus', width: 80, render: (v: string) => {
                      if (v === 'SUCCESS') return <Tag color="success">成功</Tag>
                      if (v === 'FAILED') return <Tag color="error">失败</Tag>
                      if (v === 'PENDING') return <Tag color="processing">待发</Tag>
                      return <Tag color="warning">重试中</Tag>
                    }},
                    { title: '重试', dataIndex: 'retryCount', width: 60, render: (v: number, r) => `${v}/${r.maxRetry}` },
                    { title: '错误信息', dataIndex: 'errorMessage', ellipsis: true, width: 200, render: (v: string) => v ? <Typography.Text type="danger" style={{ fontSize: 12 }}>{v}</Typography.Text> : '' },
                    { title: '操作', width: 60, render: (_, r) => <Button type="link" size="small" onClick={e => { e.stopPropagation(); setSendLogDetail(r) }}>详情</Button> },
                  ]}
                  scroll={{ x: 1000 }}
                  size="middle"
                />
              </div>

              {/* 详情抽屉 */}
              <Drawer title={<span style={{ fontSize: 16, fontWeight: 700 }}>发送任务详情</span>}
                placement="right" width={520} onClose={() => setSendLogDetail(null)} open={!!sendLogDetail}
                extra={<Button size="small" onClick={() => setSendLogDetail(null)} icon={<CloseOutlined />}>关闭</Button>}>
                {sendLogDetail && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#fafafa', borderRadius: 10, padding: '14px 18px', border: '1px solid #f0f0f0' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <span style={{ fontSize: 12, color: '#8c8c8c', fontWeight: 500 }}>任务编号</span>
                        <span style={{ fontWeight: 700, fontSize: 15, color: '#262626' }}>{sendLogDetail.id}</span>
                        <span style={{ width: 1, height: 20, background: '#e8e8e8' }} />
                        {sendLogDetail.sendStatus === 'SUCCESS' ? <Tag color="success">成功</Tag> : sendLogDetail.sendStatus === 'FAILED' ? <Tag color="error">失败</Tag> : sendLogDetail.sendStatus === 'PENDING' ? <Tag color="processing">待发</Tag> : <Tag color="warning">重试中</Tag>}
                      </div>
                      <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                        {sendLogDetail.sendType === 'TEST' ? '📧 测试发送' : sendLogDetail.sendType === 'AUTO_REPLY' ? '🤖 自动回执' : sendLogDetail.sendType === 'ASSIGN_NOTIFY' ? '📢 分配通知' : '💬 客服回复'}
                      </span>
                    </div>
                    <Card size="small" title={<span style={{ fontSize: 13, fontWeight: 600 }}>📬 发送信息</span>} styles={{ body: { padding: '12px 16px' } }}>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                        {[
                          ['收件人', sendLogDetail.toAddress],
                          ['邮件主题', sendLogDetail.subject],
                          ['发送类型', ({ 'TEST': '测试', 'AUTO_REPLY': '自动回执', 'ASSIGN_NOTIFY': '分配通知', 'AGENT_REPLY': '客服回复' }[sendLogDetail.sendType] || sendLogDetail.sendType)],
                          ['创建时间', sendLogDetail.createdAt?.replace('T', ' ').slice(0, 16) || '-'],
                          ['发送时间', sendLogDetail.sentAt?.replace('T', ' ').slice(0, 16) || '-'],
                          ['重试次数', `${sendLogDetail.retryCount}/${sendLogDetail.maxRetry}`],
                        ].map(([label, value]) => (
                          <div key={label}><div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 2 }}>{label}</div><div style={{ fontWeight: 600, fontSize: 13 }}>{value}</div></div>
                        ))}
                      </div>
                    </Card>
                    {sendLogDetail.contentBody && (
                      <Card size="small" title={<span style={{ fontSize: 13, fontWeight: 600 }}>📄 邮件正文</span>} styles={{ body: { padding: '12px 16px', background: '#fafafa' } }}>
                        <pre style={{ fontSize: 12, whiteSpace: 'pre-wrap', maxHeight: 300, overflow: 'auto', margin: 0, color: '#262626', lineHeight: 1.6 }}>{sendLogDetail.contentBody}</pre>
                      </Card>
                    )}
                    {sendLogDetail.errorMessage && (
                      <Card size="small" styles={{ header: { background: '#fff2f0', borderBottom: '1px solid #ffccc7' }, body: { padding: '12px 16px', background: '#fff2f0' } }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}><span style={{ color: '#ff4d4f', fontWeight: 700, fontSize: 13 }}>⚠ 错误详情</span></div>
                        <pre style={{ fontSize: 12, whiteSpace: 'pre-wrap', maxHeight: 200, overflow: 'auto', margin: 0, color: '#cf1322', background: '#fff', borderRadius: 6, padding: 10, border: '1px solid #ffccc7', lineHeight: 1.6 }}>{sendLogDetail.errorMessage}</pre>
                      </Card>
                    )}
                  </div>
                )}
              </Drawer>
            </div>
          ) : activeMenu === '编号规则' ? (
            <section className="app-content system-page" aria-label="编号规则配置">
              <div className="content-title">
                <div>
                  <h1>编号规则配置</h1>
                  <p>维护业务可理解的工单号生成规则；系统技术参数由管理员或运维在后台维护。</p>
                </div>
                <div className="content-actions">
                  <button disabled={ticketRuleLoading} onClick={fetchTicketRule} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                  <button
                    className="primary-action"
                    disabled={activeSystemGroup !== 'ticket' || !ticketRuleDirty || ticketRuleSaving}
                    onClick={() => setTicketRuleConfirmOpen(true)}
                    type="button"
                  >
                    <Check size={16} />
                    保存修改
                  </button>
                </div>
              </div>

              {!isAdmin ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无编号规则管理权限</strong>
                  <p>非管理员只读自己的工作信息，编号规则配置入口对处理人隐藏。</p>
                </div>
              ) : (
                <>
                  <div className="user-metrics">
                    <div className="user-metric">
                      <span>规则数量</span>
                      <strong>{ticketRule ? 1 : '--'}</strong>
                      <small>当前启用工单编号规则</small>
                    </div>
                    <div className="user-metric">
                      <span>编号规则</span>
                      <strong>{ticketRuleForm.enabled ? 1 : 0}</strong>
                      <small>当前前缀 {ticketRuleForm.prefix || '--'}</small>
                    </div>
                    <div className="user-metric">
                      <span>未保存变更</span>
                      <strong>{ticketRuleDirty ? 1 : 0}</strong>
                      <small>{ticketRuleDirty ? '规则待确认' : '暂无待保存内容'}</small>
                    </div>
                    <div className="user-metric">
                      <span>当前流水</span>
                      <strong>{ticketRule?.usedSeq ?? '--'}</strong>
                      <small>下一号 {ticketRule?.nextSeq ?? '--'}</small>
                    </div>
                  </div>

                  <div className={ticketRuleError ? 'system-alert danger' : 'system-alert'}>
                    <span>
                      {ticketRuleError ||
                        ticketRuleMessage ||
                        (activeSystemGroup === 'ticket'
                          ? '编号规则影响后续新建工单，历史工单号不会回写变更。'
                          : `${activeSystemGroupConfig.title}当前不开放业务编辑，仅展示维护边界。`)}
                    </span>
                    <button
                      onClick={previewTicketRule}
                      disabled={activeSystemGroup !== 'ticket' || ticketRulePreviewLoading}
                      type="button"
                    >
                      {ticketRulePreviewLoading ? '生成中...' : '生成预览'}
                    </button>
                  </div>

                  <div className="system-layout">
                    <aside className="system-panel system-groups">
                      <div className="system-panel__head">
                        <strong>配置分组</strong>
                        <span className="template-code-pill">业务可配</span>
                      </div>
                      {systemGroups.map((group) => (
                        <button
                          aria-pressed={activeSystemGroup === group.key}
                          className={activeSystemGroup === group.key ? 'system-group active' : 'system-group'}
                          key={group.key}
                          onClick={() => {
                            setActiveSystemGroup(group.key)
                            setTicketRuleError('')
                            setTicketRuleMessage('')
                          }}
                          type="button"
                        >
                          <strong>{group.title}</strong>
                          <small>{group.summary}</small>
                        </button>
                      ))}
                    </aside>

                    <section className="system-panel system-editor">
                      <div className="system-panel__head">
                        <strong>{activeSystemGroupConfig.title}</strong>
                        <span className={ticketRuleDirty && activeSystemGroup === 'ticket' ? 'template-code-pill dirty' : 'template-code-pill'}>
                          {activeSystemGroup === 'ticket' ? (ticketRuleDirty ? '未保存' : '已保存') : activeSystemGroupConfig.owner}
                        </span>
                      </div>
                      {activeSystemGroup !== 'ticket' ? (
                        <div className="system-readonly">
                          <ShieldCheck size={38} />
                          <strong>{activeSystemGroupConfig.title}不在当前页编辑</strong>
                          <p>{activeSystemGroupConfig.detail}</p>
                          <div className="readonly-facts">
                            <span>当前状态</span>
                            <strong>{activeSystemGroupConfig.owner}</strong>
                          </div>
                        </div>
                      ) : ticketRuleLoading ? (
                        <div className="user-loading">
                          {[0, 1, 2, 3].map((item) => (
                            <span key={item} />
                          ))}
                        </div>
                      ) : (
                        <div className="system-form">
                          <label>
                            <span>启用状态</span>
                            <select
                              onChange={(event) => updateTicketRuleForm({ enabled: event.target.value === 'true' })}
                              value={String(ticketRuleForm.enabled)}
                            >
                              <option value="true">启用</option>
                              <option value="false">停用</option>
                            </select>
                            <small>停用后使用默认规则 TCK-yyyyMMdd-0001。</small>
                          </label>
                          <label>
                            <span>工单前缀</span>
                            <input
                              maxLength={8}
                              onChange={(event) =>
                                updateTicketRuleForm({
                                  prefix: event.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''),
                                })
                              }
                              value={ticketRuleForm.prefix}
                            />
                            <small>建议使用 2-8 位大写英文或数字。</small>
                          </label>
                          <label>
                            <span>日期格式</span>
                            <select
                              onChange={(event) => updateTicketRuleForm({ dateFormat: event.target.value })}
                              value={ticketRuleForm.dateFormat}
                            >
                              <option value="yyyyMMdd">yyyyMMdd</option>
                              <option value="yyyyMM">yyyyMM</option>
                              <option value="yyyy">yyyy</option>
                            </select>
                            <small>变更后新工单按新日期维度取流水。</small>
                          </label>
                          <label>
                            <span>流水位数</span>
                            <input
                              max={8}
                              min={3}
                              onChange={(event) =>
                                updateTicketRuleForm({ seqLength: Number(event.target.value || 4) })
                              }
                              type="number"
                              value={ticketRuleForm.seqLength}
                            />
                            <small>示例：4 位生成 0001，6 位生成 000001。</small>
                          </label>
                          <label>
                            <span>分隔符</span>
                            <select
                              onChange={(event) => updateTicketRuleForm({ separator: event.target.value })}
                              value={ticketRuleForm.separator}
                            >
                              <option value="-">短横线 -</option>
                              <option value="">无分隔符</option>
                              <option value="_">下划线 _</option>
                            </select>
                            <small>建议保留短横线，便于邮件主题识别。</small>
                          </label>
                          <label className="full">
                            <span>参数说明</span>
                            <textarea
                              onChange={(event) => updateTicketRuleForm({ description: event.target.value })}
                              value={ticketRuleForm.description}
                            />
                          </label>
                          <div className="system-token-row">
                            <span>{'{prefix}'}</span>
                            <span>{`{${ticketRuleForm.dateFormat}}`}</span>
                            <span>{'{seq}'}</span>
                            <span>{ticketRuleForm.separator || '无分隔符'}</span>
                          </div>
                          <div className="system-actions">
                            <span>保存前会校验格式合法性和下一号预览。</span>
                            <div>
                              <button onClick={resetTicketRule} type="button">恢复默认</button>
                              <button
                                className="primary-action"
                                disabled={activeSystemGroup !== 'ticket'}
                                onClick={() => setTicketRuleConfirmOpen(true)}
                                type="button"
                              >
                                保存规则
                              </button>
                            </div>
                          </div>
                        </div>
                      )}
                    </section>

                    <aside className="system-side">
                      {activeSystemGroup === 'ticket' ? (
                        <>
                          <section className="system-panel">
                            <div className="system-panel__head">
                              <strong>规则预览</strong>
                              <span className="state-pill enabled">可用</span>
                            </div>
                            <div className="rule-preview-card">
                              <span>下一工单号</span>
                              <strong>{ticketRule?.nextTicketNo || '点击生成预览'}</strong>
                            </div>
                            <div className="rule-preview-list">
                              <div><span>今日日期</span><strong>{ticketRule?.todayDate || '--'}</strong></div>
                              <div><span>当前日期维度</span><strong>{ticketRule?.dateKey || '--'}</strong></div>
                              <div><span>当前已用流水</span><strong>{ticketRule?.usedSeq ?? '--'}</strong></div>
                              <div><span>主题匹配样例</span><strong>{ticketRule?.subjectPreview || '--'}</strong></div>
                            </div>
                          </section>

                          <section className="system-panel">
                            <div className="system-panel__head">
                              <strong>发布检查</strong>
                              <span className="template-code-pill">自动校验</span>
                            </div>
                            <div className="system-check-list">
                              <div><Check size={16} /><span><strong>规则格式合法</strong><small>前缀、日期和流水片段均可解析。</small></span></div>
                              <div><Check size={16} /><span><strong>下一号可预览</strong><small>预览编号按当前日期维度生成。</small></span></div>
                              <div><TriangleAlert size={16} /><span><strong>影响新工单</strong><small>保存后仅影响后续自动建单。</small></span></div>
                            </div>
                          </section>
                        </>
                      ) : (
                        <section className="system-panel">
                          <div className="system-panel__head">
                            <strong>分组说明</strong>
                            <span className="template-code-pill">{activeSystemGroupConfig.owner}</span>
                          </div>
                          <div className="system-readonly side">
                            <ShieldCheck size={34} />
                            <strong>{activeSystemGroupConfig.title}</strong>
                            <p>{activeSystemGroupConfig.detail}</p>
                          </div>
                        </section>
                      )}
                    </aside>
                  </div>

                </>
              )}
            </section>
          ) : activeMenu === '通知模板' ? (
            <section className="app-content template-page" aria-label="通知模板">
              <div className="content-title">
                <div>
                  <h1>通知模板</h1>
                  <p>维护自动回执、分配通知、处理人回复和 SLA 提醒模板；支持变量插入、预览和保存。</p>
                </div>
                <div className="content-actions">
                  <button disabled={templatesLoading} onClick={fetchTemplates} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                </div>
              </div>

              {!isAdmin ? (
                <div className="permission-state">
                  <ShieldCheck size={42} />
                  <strong>无通知模板管理权限</strong>
                  <p>非管理员只读自己的工作信息，通知模板编辑入口对处理人隐藏。</p>
                </div>
              ) : (
                <>
                  <div className="user-metrics">
                    <div className="user-metric">
                      <span>模板总数</span>
                      <strong>{templatesData?.summary.totalTemplates ?? '--'}</strong>
                      <small>按业务场景唯一编码</small>
                    </div>
                    <div className="user-metric">
                      <span>启用模板</span>
                      <strong>{templatesData?.summary.enabledTemplates ?? '--'}</strong>
                      <small>可参与自动通知</small>
                    </div>
                    <div className="user-metric">
                      <span>停用模板</span>
                      <strong>{templatesData?.summary.disabledTemplates ?? '--'}</strong>
                      <small>保留配置但不发送</small>
                    </div>
                    <div className="user-metric">
                      <span>可用变量</span>
                      <strong>{templatesData?.summary.availableVariables ?? '--'}</strong>
                      <small>工单、客户、处理人信息</small>
                    </div>
                  </div>

                  {templatesError && <div className="user-alert">{templatesError}</div>}

                  <div className="template-layout">
                    <aside className="template-panel template-list-panel">
                      <div className="template-panel__head">
                        <strong>模板列表</strong>
                        <button className="template-head-action primary" onClick={openCreateTemplate} type="button">
                          <Plus size={14} />
                          新建
                        </button>
                      </div>
                      <label className="template-search">
                        <Search size={15} />
                        <input
                          onChange={(event) => setTemplateKeyword(event.target.value)}
                          placeholder="搜索模板名称或编码"
                          type="search"
                          value={templateKeyword}
                        />
                      </label>

                      {templatesLoading ? (
                        <div className="user-loading">
                          {[0, 1, 2, 3, 4].map((item) => (
                            <span key={item} />
                          ))}
                        </div>
                      ) : templatesData && templatesData.records.length > 0 ? (
                        <div className="template-list">
                          {templatesData.records.map((template) => (
                            <button
                              className={selectedTemplateId === template.id ? 'template-item active' : 'template-item'}
                              key={template.id}
                              onClick={() => selectTemplate(template)}
                              type="button"
                            >
                              <span className="template-item__top">
                                <strong>{template.templateName}</strong>
                                <i className={template.enabled ? 'state-pill enabled' : 'state-pill disabled'}>
                                  {template.enabled ? '启用' : '停用'}
                                </i>
                              </span>
                              <code>{template.templateCode}</code>
                              <small>{templateSceneLabel(template.templateCode)}</small>
                            </button>
                          ))}
                        </div>
                      ) : (
                        <div className="empty-state compact">
                          <Bell size={34} />
                          <strong>未找到模板</strong>
                          <p>可清空搜索后重新查询。</p>
                          <button onClick={() => setTemplateKeyword('')} type="button">清空搜索</button>
                        </div>
                      )}
                    </aside>

                    <section className="template-panel template-editor-panel">
                      <div className="template-panel__head">
                        <div className="template-editor-title">
                          <strong>{templateForm.id ? '模板编辑' : '新建模板'}</strong>
                          <span className={templateDirty ? 'template-code-pill dirty' : 'template-code-pill'}>
                            {templateDirty ? '未保存' : '已保存'}
                          </span>
                        </div>
                        <button
                          className="template-head-action primary"
                          disabled={!templateDirty || templateSaving}
                          onClick={() => setTemplateConfirmOpen(true)}
                          type="button"
                        >
                          <Check size={14} />
                          {templateForm.id ? '保存' : '创建'}
                        </button>
                      </div>
                      <div className="template-editor">
                        <div className="template-form-grid">
                          <label>
                            <span>模板编码</span>
                            <input
                              disabled={Boolean(templateForm.id)}
                              onChange={(event) =>
                                updateTemplateForm({ templateCode: event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })
                              }
                              placeholder="例如 CUSTOM_NOTICE"
                              value={templateForm.templateCode}
                            />
                            <small>编码唯一；新建时可填写，保存后不可修改。</small>
                          </label>
                          <label>
                            <span>模板名称</span>
                            <input
                              onChange={(event) => updateTemplateForm({ templateName: event.target.value })}
                              value={templateForm.templateName}
                            />
                          </label>
                          <label>
                            <span>发送场景</span>
                            <input disabled value={templateSceneLabel(templateForm.templateCode)} />
                          </label>
                          <label>
                            <span>启用状态</span>
                            <button
                              className={templateForm.enabled ? 'template-switch enabled' : 'template-switch'}
                              onClick={() => updateTemplateForm({ enabled: !templateForm.enabled })}
                              type="button"
                            >
                              <span>{templateForm.enabled ? '启用后参与自动通知' : '停用后不参与发送'}</span>
                              <i />
                            </button>
                          </label>
                          <label className="full">
                            <span>邮件主题</span>
                            <input
                              onChange={(event) => updateTemplateForm({ subjectTpl: event.target.value })}
                              value={templateForm.subjectTpl}
                            />
                            <small>主题可插入变量，保存前需校验变量格式。</small>
                          </label>
                          <label className="full">
                            <span>邮件正文</span>
                            <div className="template-toolbar">
                              <button type="button">B</button>
                              <button type="button">I</button>
                              <button type="button">列表</button>
                              <button type="button">链接</button>
                              <button type="button">撤销</button>
                              <button type="button">重做</button>
                            </div>
                            <textarea
                              onChange={(event) => updateTemplateForm({ contentTpl: event.target.value })}
                              ref={templateContentRef}
                              value={templateForm.contentTpl}
                            />
                            <small>第一版可降级为纯文本编辑；工具栏保留交互位，保存时写入正文模板。</small>
                          </label>
                        </div>
                      </div>
                    </section>

                    <aside className="template-side">
                      <section className="template-panel">
                        <div className="template-panel__head">
                          <strong>变量面板</strong>
                          <span className="template-code-pill">点击插入</span>
                        </div>
                        <div className="template-vars">
                          {(templatesData?.variables || []).map((variable) => (
                            <button key={variable.key} onClick={() => insertVariable(variable.key)} type="button">
                              <code>{variable.key}</code>
                              <span>{variable.label}</span>
                              <small>示例：{variable.sampleValue}</small>
                            </button>
                          ))}
                        </div>
                      </section>

                      <section className="template-panel">
                        <div className="template-panel__head">
                          <strong>预览</strong>
                          <button disabled={templatePreviewLoading} onClick={previewTemplate} type="button">
                            {templatePreviewLoading ? '生成中...' : '生成预览'}
                          </button>
                        </div>
                        <div className="template-preview">
                          <div className="mail-subject">
                            {templatePreview?.subject || '点击生成预览后显示邮件主题'}
                          </div>
                          <div className="mail-body">
                            {templatePreview?.content || '模板正文预览会使用系统默认示例数据进行变量替换。'}
                          </div>
                        </div>
                      </section>
                    </aside>
                  </div>

                </>
              )}
            </section>
          ) : (
            <section className="app-content" aria-label="菜单内容区">
              <div className="content-title">
                <div>
                  <h1>{activeMenu}</h1>
                  <p>全局左侧菜单和右上角个人中心已固定；该区域后续按当前菜单对应原型逐项开发。</p>
                </div>
                <div className="content-actions">
                  <button type="button">
                    <CalendarDays size={16} />
                    2026-07-22
                  </button>
                  <button type="button">
                    <Settings size={16} />
                    刷新数据
                  </button>
                </div>
              </div>

              <div className="workspace-placeholder">
                <div className="placeholder-card">
                  <span>当前账号</span>
                  <strong>{user.account}</strong>
                  <small>{user.email}</small>
                </div>
                <div className="placeholder-card">
                  <span>当前菜单</span>
                  <strong>{activeMenu}</strong>
                  <small>{searchKeyword ? `搜索关键字：${searchKeyword}` : '等待开发具体内容'}</small>
                </div>
                <div className="placeholder-card">
                  <span>开发边界</span>
                  <strong>AppShell 固定</strong>
                  <small>后续只替换内容区</small>
                </div>
              </div>
            </section>
          )}
        </main>

        {userFormOpen && (
          <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="user-form-title">
            <form className="user-modal" onSubmit={submitUserForm}>
              <div className="user-modal__head">
                <h2 id="user-form-title">{userFormMode === 'create' ? '新建用户' : '编辑用户'}</h2>
                <button aria-label="关闭" onClick={() => setUserFormOpen(false)} type="button">
                  <X size={18} />
                </button>
              </div>
              {userFormError && <div className="user-alert">{userFormError}</div>}
              <div className="user-modal__body">
                <label>
                  <span>登录账号</span>
                  <input
                    disabled={userFormMode === 'edit' || userFormSubmitting}
                    onChange={(event) => setUserForm((value) => ({ ...value, account: event.target.value }))}
                    placeholder="agent01"
                    value={userForm.account}
                  />
                  <small>账号保存后不可修改，需保持唯一。</small>
                </label>
                <label>
                  <span>姓名</span>
                  <input
                    disabled={userFormSubmitting}
                    onChange={(event) => setUserForm((value) => ({ ...value, displayName: event.target.value }))}
                    placeholder="客服一号"
                    value={userForm.displayName}
                  />
                </label>
                <label>
                  <span>邮箱</span>
                  <input
                    disabled={userFormSubmitting}
                    onChange={(event) => setUserForm((value) => ({ ...value, email: event.target.value }))}
                    placeholder="agent01@ntn.fziot"
                    type="email"
                    value={userForm.email}
                  />
                </label>
                <label>
                  <span>角色</span>
                  <select
                    disabled={userFormSubmitting}
                    onChange={(event) => {
                      const nextRole = toRoleCode(event.target.value)
                      setUserForm((value) => ({
                        ...value,
                        roleCode: nextRole,
                        roleCodes: normalizeRoleCodes(nextRole, value.roleCodes),
                      }))
                    }}
                    value={userForm.roleCode}
                  >
                    {roleSelectOptions.map((role) => (
                      <option key={role.value} value={role.value}>{role.label}</option>
                    ))}
                  </select>
                </label>
                <div className="user-role-checks">
                  <span>附加角色</span>
                  <div>
                    {roleSelectOptions.map((role) => (
                      <label key={role.value}>
                        <input
                          checked={normalizeRoleCodes(userForm.roleCode, userForm.roleCodes).includes(role.value)}
                          disabled={userFormSubmitting || role.value === userForm.roleCode}
                          onChange={(event) => {
                            setUserForm((value) => {
                              const next = new Set(normalizeRoleCodes(value.roleCode, value.roleCodes))
                              if (event.target.checked) {
                                next.add(role.value)
                              } else {
                                next.delete(role.value)
                              }
                              next.add(value.roleCode)
                              return { ...value, roleCodes: Array.from(next) }
                            })
                          }}
                          type="checkbox"
                        />
                        {role.label}
                      </label>
                    ))}
                  </div>
                  <small>主角色用于列表展示；实际菜单、按钮和数据范围按全部角色合并。</small>
                </div>
                <div className="role-preview">
                  <span>角色生效预览</span>
                  <strong>
                    {normalizeRoleCodes(userForm.roleCode, userForm.roleCodes)
                      .map((roleCode) => roleSelectOptions.find((role) => role.value === roleCode)?.label || roleLabel(roleCode))
                      .join('、')}
                  </strong>
                  <small>{normalizeRoleCodes(userForm.roleCode, userForm.roleCodes).length > 1 ? '菜单、按钮和数据范围按多角色合并' : getRoleProfile(userForm.roleCode).dataScope}；保存后刷新当前用户信息即可按新权限生效。</small>
                  <div>
                    {normalizeRoleCodes(userForm.roleCode, userForm.roleCodes).slice(0, 4).map((roleCode) => (
                      <em key={roleCode}>{roleSelectOptions.find((role) => role.value === roleCode)?.label || roleLabel(roleCode)}</em>
                    ))}
                  </div>
                </div>
                {userFormMode === 'create' && (
                  <label>
                    <span>初始密码</span>
                    <input
                      disabled={userFormSubmitting}
                      onChange={(event) => setUserForm((value) => ({ ...value, password: event.target.value }))}
                      placeholder="至少 6 位"
                      type="password"
                      value={userForm.password}
                    />
                  </label>
                )}
                <label>
                  <span>状态</span>
                  <select
                    disabled={userFormSubmitting}
                    onChange={(event) =>
                      setUserForm((value) => ({ ...value, enabled: event.target.value === 'true' }))
                    }
                    value={String(userForm.enabled)}
                  >
                    <option value="true">启用</option>
                    <option value="false">停用</option>
                  </select>
                </label>
              </div>
              <div className="user-modal__foot">
                <button disabled={userFormSubmitting} onClick={() => setUserFormOpen(false)} type="button">
                  取消
                </button>
                <button className="primary-action" disabled={userFormSubmitting} type="submit">
                  <Check size={16} />
                  {userFormSubmitting ? '保存中...' : userFormMode === 'create' ? '保存并创建' : '保存修改'}
                </button>
              </div>
            </form>
          </div>
        )}

        {confirmAction && (
          <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
            <div className="confirm-modal">
              <h3 id="confirm-title">{confirmAction.title}</h3>
              <p>{confirmAction.text}</p>
              <div className="confirm-target">
                <strong>{confirmAction.user.displayName}</strong>
                <span>{confirmAction.user.account} / {confirmAction.user.email}</span>
              </div>
              <div className="user-modal__foot">
                <button disabled={actionLoading} onClick={() => setConfirmAction(null)} type="button">取消</button>
                <button className="primary-action" disabled={actionLoading} onClick={submitConfirmAction} type="button">
                  {actionLoading ? '处理中...' : confirmAction.actionLabel}
                </button>
              </div>
            </div>
          </div>
        )}

        {templateConfirmOpen && (
          <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="template-confirm-title">
            <div className="confirm-modal">
              <h3 id="template-confirm-title">{templateForm.id ? '保存模板确认' : '新建模板确认'}</h3>
              <p>
                {templateForm.id
                  ? '保存后新产生的自动回执和通知将使用当前模板内容，历史已发送邮件不受影响。'
                  : '创建后模板会进入列表，启用状态由当前开关决定。'}
              </p>
              <div className="confirm-target">
                <strong>{templateForm.templateName || '未命名模板'}</strong>
                <span>{templateForm.templateCode || '未选择模板'}</span>
              </div>
              <div className="user-modal__foot">
                <button disabled={templateSaving} onClick={() => setTemplateConfirmOpen(false)} type="button">取消</button>
                <button className="primary-action" disabled={templateSaving} onClick={saveTemplate} type="button">
                  {templateSaving ? '保存中...' : templateForm.id ? '确认保存' : '确认创建'}
                </button>
              </div>
            </div>
          </div>
        )}

        {mailboxConfirmAction && (
          <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="mailbox-confirm-title">
            <div className="confirm-modal">
              <h3 id="mailbox-confirm-title">{mailboxConfirmAction.title}</h3>
              <p>{mailboxConfirmAction.text}</p>
              <div className="confirm-target">
                <strong>{mailboxConfirmAction.mailbox.mailboxName}</strong>
                <span>{mailboxConfirmAction.mailbox.emailAddress}</span>
              </div>
              <div className="user-modal__foot">
                <button disabled={mailboxActionLoading} onClick={() => setMailboxConfirmAction(null)} type="button">
                  取消
                </button>
                <button className="primary-action" disabled={mailboxActionLoading} onClick={submitMailboxConfirm} type="button">
                  {mailboxActionLoading ? '处理中...' : mailboxConfirmAction.actionLabel}
                </button>
              </div>
            </div>
          </div>
        )}

        {assignmentConfirmAction && (
          <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="assignment-confirm-title">
            <div className="confirm-modal">
              <h3 id="assignment-confirm-title">删除分配规则</h3>
              <p>删除后该规则不再参与新工单自动匹配，历史工单已分配的处理人保持不变。</p>
              <div className="confirm-target">
                <strong>{assignmentConfirmAction.rule.ruleName}</strong>
                <span>{assignmentRuleText(assignmentConfirmAction.rule)}</span>
              </div>
              <div className="user-modal__foot">
                <button disabled={assignmentActionLoading} onClick={() => setAssignmentConfirmAction(null)} type="button">
                  取消
                </button>
                <button className="primary-action" disabled={assignmentActionLoading} onClick={submitAssignmentConfirm} type="button">
                  {assignmentActionLoading ? '删除中...' : '确认删除'}
                </button>
              </div>
            </div>
          </div>
        )}

        {slaPolicyConfirmAction && (
          <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="sla-policy-confirm-title">
            <div className="confirm-modal">
              <h3 id="sla-policy-confirm-title">删除 SLA 策略</h3>
              <p>删除后该策略不再用于后续新工单 SLA 计算，历史工单已有的 SLA 字段保持不变。</p>
              <div className="confirm-target">
                <strong>{slaPolicyConfirmAction.policy.policyName}</strong>
                <span>
                  响应 {hoursLabel(slaPolicyConfirmAction.policy.responseHours)}
                  ，解决 {hoursLabel(slaPolicyConfirmAction.policy.resolveHours)}
                </span>
              </div>
              <div className="user-modal__foot">
                <button disabled={slaPolicyActionLoading} onClick={() => setSlaPolicyConfirmAction(null)} type="button">
                  取消
                </button>
                <button className="primary-action" disabled={slaPolicyActionLoading} onClick={submitSlaPolicyConfirm} type="button">
                  {slaPolicyActionLoading ? '删除中...' : '确认删除'}
                </button>
              </div>
            </div>
          </div>
        )}

        {workCalendarConfirmAction && (
          <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="work-calendar-confirm-title">
            <div className="confirm-modal">
              <h3 id="work-calendar-confirm-title">
                {workCalendarConfirmAction.type === 'delete-calendar' ? '删除工作日历' : '删除节假日'}
              </h3>
              <p>
                {workCalendarConfirmAction.type === 'delete-calendar'
                  ? '删除后该日历不再用于后续 SLA 计算，历史工单已有截止时间保持不变。'
                  : '删除后该日期不再作为后续 SLA 计算的节假日，历史工单已有截止时间保持不变。'}
              </p>
              <div className="confirm-target">
                <strong>
                  {workCalendarConfirmAction.type === 'delete-calendar'
                    ? workCalendarConfirmAction.calendar.calendarName
                    : workCalendarConfirmAction.holiday.holidayName}
                </strong>
                <span>
                  {workCalendarConfirmAction.type === 'delete-calendar'
                    ? `${workdayLabel(workCalendarConfirmAction.calendar.workdays)} · ${workCalendarConfirmAction.calendar.workStartTime}-${workCalendarConfirmAction.calendar.workEndTime}`
                    : workCalendarConfirmAction.holiday.holidayDate}
                </span>
              </div>
              <div className="user-modal__foot">
                <button disabled={workCalendarActionLoading} onClick={() => setWorkCalendarConfirmAction(null)} type="button">
                  取消
                </button>
                <button className="primary-action" disabled={workCalendarActionLoading} onClick={submitWorkCalendarConfirm} type="button">
                  {workCalendarActionLoading ? '删除中...' : '确认删除'}
                </button>
              </div>
            </div>
          </div>
        )}

        {ticketRuleConfirmOpen && (
          <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="ticket-rule-confirm-title">
            <div className="confirm-modal">
              <h3 id="ticket-rule-confirm-title">保存编号规则确认</h3>
              <p>保存后系统会用新规则生成后续工单号，历史工单号不受影响。请确认规则预览无误。</p>
              <div className="confirm-target">
                <strong>{ticketRule?.nextTicketNo || `${ticketRuleForm.prefix || 'TCK'} 规则`}</strong>
                <span>影响范围：后续客户来信自动建单、自动回执、主题工单号匹配。</span>
              </div>
              <div className="user-modal__foot">
                <button disabled={ticketRuleSaving} onClick={() => setTicketRuleConfirmOpen(false)} type="button">
                  取消
                </button>
                <button className="primary-action" disabled={ticketRuleSaving} onClick={saveTicketRule} type="button">
                  {ticketRuleSaving ? '保存中...' : '确认保存'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 转派弹窗 */}
        <Modal
          title="转派工单"
          open={assignModalOpen}
          onCancel={() => {
            setAssignModalOpen(false)
            setAssignUserId(null)
            setAssignReason('')
            setAssignNotifyAssignee(true)
          }}
          onOk={() => void handleAssign()}
          confirmLoading={assignSending}
          okText="确认转派"
          cancelText="取消"
          okButtonProps={{ disabled: !canOperateCurrentTicket || !assignUserId }}
        >
          <div style={{ padding: '12px 0', display: 'grid', gap: 14 }}>
            <Alert
              type="info"
              showIcon
              message={ticketDetail ? `${ticketDetail.ticketNo} / ${ticketDetail.subject}` : '当前工单'}
              description={`当前处理人：${ticketDetail?.assigneeName || '未分配'}`}
            />
            <div>
              <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>选择处理人</div>
            <Select
              style={{ width: '100%' }}
              placeholder="请选择处理人"
              value={assignUserId}
              onChange={setAssignUserId}
              disabled={!canOperateCurrentTicket}
              showSearch
              optionFilterProp="label"
              options={assignUsers
                .filter(u => u.enabled)
                .map(u => ({
                  label: `${u.displayName} (${u.account}${u.email ? ` / ${u.email}` : ''})`,
                  value: u.id,
                }))}
            />
            </div>
            <div>
              <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>转派原因</div>
              <Input.TextArea
                value={assignReason}
                onChange={(event) => setAssignReason(event.target.value)}
                disabled={!canOperateCurrentTicket}
                maxLength={200}
                rows={3}
                showCount
                placeholder="填写转派原因，保存后会写入工单日志"
              />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}>
              <div>
                <div style={{ fontSize: 13, fontWeight: 600, color: '#1f2937' }}>通知新处理人</div>
                <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>开启后会发送分配通知邮件；关闭时仅更新工单。</div>
              </div>
              <Switch checked={assignNotifyAssignee} onChange={setAssignNotifyAssignee} disabled={!canOperateCurrentTicket} />
            </div>
          </div>
        </Modal>

        {/* 修改优先级弹窗 */}
        <Modal
          title="修改优先级"
          open={priorityModalOpen}
          onCancel={() => {
            setPriorityModalOpen(false)
            setPriorityReason('')
          }}
          onOk={() => void handlePriority()}
          confirmLoading={prioritySending}
          okText="确认修改"
          cancelText="取消"
          okButtonProps={{ disabled: !canOperateCurrentTicket || !ticketDetail || priorityValue === ticketDetail.priority }}
        >
          <div style={{ padding: '12px 0', display: 'grid', gap: 14 }}>
            <Alert
              type="info"
              showIcon
              message={ticketDetail ? `${ticketDetail.ticketNo} / ${ticketDetail.subject}` : '当前工单'}
              description={`当前优先级：${ticketDetail ? priorityOptionLabel(ticketDetail.priority) : '-'}`}
            />
            <div>
              <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>目标优先级</div>
              <Select
                style={{ width: '100%' }}
                value={priorityValue}
                onChange={setPriorityValue}
                disabled={!canOperateCurrentTicket}
                options={[
                  { label: 'P1 - 紧急', value: 'URGENT' },
                  { label: 'P2 - 高', value: 'HIGH' },
                  { label: 'P3 - 普通', value: 'NORMAL' },
                  { label: 'P4 - 低', value: 'LOW' },
                ]}
              />
            </div>
            <div>
              <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>变更说明</div>
              <Input.TextArea
                value={priorityReason}
                onChange={(event) => setPriorityReason(event.target.value)}
                disabled={!canOperateCurrentTicket}
                maxLength={200}
                rows={3}
                showCount
                placeholder="填写优先级调整原因，保存后会写入工单日志"
              />
            </div>
          </div>
        </Modal>

        {/* 修改状态弹窗 */}
        <Modal
          title="修改状态"
          open={statusModalOpen}
          onCancel={() => {
            setStatusModalOpen(false)
            setStatusReason('')
          }}
          onOk={() => void handleStatusChange()}
          confirmLoading={statusSending}
          okText="确认修改"
          cancelText="取消"
          okButtonProps={{ disabled: !canOperateCurrentTicket || !ticketDetail || statusValue === ticketDetail.status }}
        >
          <div style={{ padding: '12px 0', display: 'grid', gap: 14 }}>
            <Alert
              type="warning"
              showIcon
              message={ticketDetail ? `${ticketDetail.ticketNo} / ${ticketDetail.subject}` : '当前工单'}
              description="状态变更会写入生命周期。关闭工单请使用专用关闭确认弹窗；待客户回复由对外回复自动流转。"
            />
            <div>
              <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>目标状态</div>
              <Select
                style={{ width: '100%' }}
                value={statusValue}
                onChange={setStatusValue}
                disabled={!canOperateCurrentTicket}
                options={[
                  { label: '处理中', value: 'PROCESSING' },
                  { label: '已取消', value: 'CANCELLED' },
                ]}
              />
            </div>
            <div>
              <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>变更说明</div>
              <Input.TextArea
                value={statusReason}
                onChange={(event) => setStatusReason(event.target.value)}
                disabled={!canOperateCurrentTicket}
                maxLength={200}
                rows={3}
                showCount
                placeholder="填写状态调整原因，保存后会写入工单日志"
              />
            </div>
          </div>
        </Modal>

        {/* 关闭确认弹窗 */}
        <Modal
          title="关闭工单"
          open={closeModalOpen}
          onCancel={() => {
            setCloseModalOpen(false)
            setCloseReason('')
            setCloseConfirmed(false)
          }}
          onOk={() => void handleClose()}
          confirmLoading={closeSending}
          okText="确认关闭"
          cancelText="取消"
          okButtonProps={{ danger: true, disabled: !canOperateCurrentTicket || !closeConfirmed }}
        >
          <div style={{ padding: '12px 0', display: 'grid', gap: 14 }}>
            <Alert
              type="warning"
              showIcon
              message={ticketDetail ? `${ticketDetail.ticketNo} / ${ticketDetail.subject}` : '当前工单'}
              description="关闭后工单状态会变为已关闭，并写入关闭时间和生命周期事件。客户后续追信将默认关联原单并转回处理中。"
            />
            <div>
              <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>关闭说明</div>
              <Input.TextArea
                value={closeReason}
                onChange={(event) => setCloseReason(event.target.value)}
                disabled={!canOperateCurrentTicket}
                maxLength={200}
                rows={3}
                showCount
                placeholder="填写关闭原因或处理结论，保存后会写入工单日志"
              />
            </div>
            <Checkbox checked={closeConfirmed} disabled={!canOperateCurrentTicket} onChange={(event) => setCloseConfirmed(event.target.checked)}>
              我确认该工单已处理完成，可以关闭
            </Checkbox>
          </div>
        </Modal>
      </div>
    )
  }

  return (
    <main className="login-page">
      <section className="login-intro" aria-label="产品介绍">
        <div className="brand">
          <span className="brand-mark">M</span>
          <span>
            <h1>邮件工单系统</h1>
            <p>企业级邮件工单管理平台</p>
          </span>
        </div>

        <div className="hero-copy">
          <h2>
            让邮件沟通<span>更高效</span>
          </h2>
          <p>集中管理客户邮件，自动生成工单，智能分配处理，全流程跟踪，提升团队协作效率与客户满意度。</p>
        </div>

        <div className="workflow" aria-label="系统能力">
          {features.map((feature) => (
            <div className="feature" key={feature.title}>
              <span className="feature-mark">{feature.mark}</span>
              <span>
                <strong>{feature.title}</strong>
                <small>{feature.text}</small>
              </span>
            </div>
          ))}
          <div className="screen-preview" aria-hidden="true">
            <div className="screen-bar">
              <span />
              <span />
              <span />
            </div>
            <div className="screen-body">
              <div className="mini-sidebar">
                <span />
                <span />
                <span />
                <span />
                <span />
              </div>
              <div className="mini-content">
                <div className="mini-search" />
                {[0, 1, 2, 3].map((item) => (
                  <div className="mini-row" key={item}>
                    <span />
                    <i />
                    <b />
                  </div>
                ))}
              </div>
            </div>
          </div>
          <div className="shield" aria-hidden="true">OK</div>
        </div>
      </section>

      <section className="login-panel" aria-label="登录表单">
        <form className="form-box" onSubmit={handleSubmit} noValidate>
          <div className="form-header">
            <h2>欢迎登录</h2>
            <p>请输入您的账号和密码登录系统</p>
          </div>

          {formError && (
            <div className="alert" role="alert">
              {formError}
            </div>
          )}

          <label className="field">
            <span>账号</span>
            <input
              aria-invalid={Boolean(accountError)}
              autoComplete="username"
              className={accountError ? 'invalid' : ''}
              disabled={submitting}
              onChange={(event) => {
                setAccount(event.target.value)
                setAccountError('')
                setFormError('')
              }}
              placeholder="请输入账号 / 邮箱"
              type="text"
              value={account}
            />
            {accountError && <small>{accountError}</small>}
          </label>

          <label className="field">
            <span>密码</span>
            <div className="password-field">
              <input
                aria-invalid={Boolean(passwordError)}
                autoComplete="current-password"
                className={passwordError ? 'invalid' : ''}
                disabled={submitting}
                onChange={(event) => {
                  setPassword(event.target.value)
                  setPasswordError('')
                  setFormError('')
                }}
                placeholder="请输入密码"
                type={showPassword ? 'text' : 'password'}
                value={password}
              />
              <button
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
                disabled={submitting}
                onClick={() => setShowPassword((value) => !value)}
                title="显示/隐藏密码"
                type="button"
              >
                {showPassword ? '隐藏' : '显示'}
              </button>
            </div>
            {passwordError && <small>{passwordError}</small>}
          </label>

          <div className="form-row">
            <label className="remember">
              <input
                checked={rememberMe}
                disabled={submitting}
                onChange={(event) => setRememberMe(event.target.checked)}
                type="checkbox"
              />
              <span>记住我</span>
            </label>
            <button
              className="link-button"
              onClick={() =>
                setModal({
                  title: '忘记密码',
                  text: '请联系系统管理员重置密码。为保护邮箱与工单数据安全，第一版不开放自助找回入口。',
                })
              }
              type="button"
            >
              忘记密码?
            </button>
          </div>

          <button className="login-button" disabled={submitting} type="submit">
            {submitting ? '登录中...' : '登录'}
          </button>

          <p className="helper">
            首次无账号？
            <button
              onClick={() =>
                setModal({
                  title: '首次无账号',
                  text: '后台账号由管理员统一创建。请联系管理员开通账号并分配角色后再登录。',
                })
              }
              type="button"
            >
              请联系管理员创建账号
            </button>
          </p>
        </form>
      </section>

      <footer>© 2026 邮件工单系统. All rights reserved.</footer>

      {modal && (
        <div className="modal-mask" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <div className="modal">
            <h3 id="modal-title">{modal.title}</h3>
            <p>{modal.text}</p>
            <div className="modal-actions">
              <button type="button" onClick={() => setModal(null)}>
                取消
              </button>
              <button className="primary" type="button" onClick={() => setModal(null)}>
                我知道了
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}

export default App
