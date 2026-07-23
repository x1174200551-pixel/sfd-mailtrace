import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
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
  ListChecks,
  LogOut,
  LockKeyhole,
  Mail,
  Menu,
  MessageCircle,
  PanelLeftClose,
  PanelLeftOpen,
  PieChart,
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
  UserRound,
  Users,
  X,
  Trash2,
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
}

type MenuGroup = {
  title: string
  items: MenuItem[]
  adminOnly?: boolean
}

type RoleCode = 'ADMIN' | 'AGENT'

type ManagedUser = {
  id: number
  account: string
  displayName: string
  email: string
  roleCode: RoleCode
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

const TOKEN_KEY = 'mailtrace_token'
const USER_KEY = 'mailtrace_user'
const REMEMBER_KEY = 'mailtrace_remember'

const menuGroups: MenuGroup[] = [
  {
    title: '工作空间',
    items: [{ title: '工作台', icon: Home }],
  },
  {
    title: '工单中心',
    items: [
      { title: '全部工单', icon: Layers },
      { title: '我的工单', icon: Folder },
      { title: '待处理', icon: Clock, badge: '39', tone: 'danger' },
      { title: '待客户回复', icon: MessageCircle, badge: '15', tone: 'warning' },
      { title: '已关闭', icon: CircleCheck },
    ],
  },
  {
    title: '邮件管理',
    adminOnly: true,
    items: [
      { title: '邮箱配置', icon: Settings },
      { title: '收件记录', icon: Inbox },
      { title: '发件记录', icon: Send, badge: '3', tone: 'primary' },
    ],
  },
  {
    title: 'SLA管理',
    adminOnly: true,
    items: [
      { title: 'SLA策略', icon: Timer },
      { title: '工作日历', icon: CalendarDays },
      { title: '超时记录', icon: TriangleAlert },
      { title: '统计报表', icon: PieChart, badge: 'NEW', tone: 'new' },
    ],
  },
  {
    title: '系统管理',
    adminOnly: true,
    items: [
      { title: '用户管理', icon: UserCog, adminOnly: true },
      { title: '编号规则', icon: SlidersHorizontal },
      { title: '通知模板', icon: Bell },
      { title: '操作日志', icon: ListChecks },
    ],
  },
]

const roleOptions: Array<{ label: string; value: RoleCode }> = [
  { label: '管理员', value: 'ADMIN' },
  { label: '客服处理人', value: 'AGENT' },
]

const emptyUserForm: UserFormState = {
  account: '',
  displayName: '',
  email: '',
  roleCode: 'AGENT',
  password: '',
  enabled: true,
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

const mailboxSteps: Array<{ key: MailboxStepKey; label: string; description: string }> = [
  { key: 'basic', label: '基础信息', description: '邮箱名称、地址、默认处理人和启用状态' },
  { key: 'imap', label: '收信配置', description: 'IMAP 服务器、账号、授权码和收件文件夹' },
  { key: 'smtp', label: '发信配置', description: 'SMTP 服务器、账号、授权码和发件人' },
  { key: 'reply', label: '自动回复', description: '拉取频率、自动回执和回执模板' },
  { key: 'test', label: '连接测试', description: '保存前建议完成收信和发信连接测试' },
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
  if (!headers.has('Content-Type')) {
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

function formatSyncTime(value: string | null) {
  if (!value) return '未同步'
  return value.replace('T', ' ').slice(0, 16)
}

function roleLabel(roleCode: string) {
  return roleCode === 'ADMIN' ? '管理员' : '客服处理人'
}

function templateSceneLabel(templateCode: string) {
  return templateScenes[templateCode] || '自定义通知场景'
}

function toRoleCode(value: string): RoleCode {
  return value === 'ADMIN' ? 'ADMIN' : 'AGENT'
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

function mailboxStatusLabel(status: MailboxConnectionStatus) {
  if (status === 'OK') return '正常'
  if (status === 'ERROR') return '异常'
  return '未知'
}

function secondsLabel(value: number) {
  if (value % 60 === 0) return `${value / 60} 分钟`
  return `${value} 秒`
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
  const [ticketRule, setTicketRule] = useState<TicketNumberRule | null>(null)
  const [ticketRuleForm, setTicketRuleForm] = useState<TicketRuleFormState>(emptyTicketRuleForm)
  const [ticketRuleDirty, setTicketRuleDirty] = useState(false)
  const [ticketRuleLoading, setTicketRuleLoading] = useState(false)
  const [ticketRuleSaving, setTicketRuleSaving] = useState(false)
  const [ticketRulePreviewLoading, setTicketRulePreviewLoading] = useState(false)
  const [ticketRuleError, setTicketRuleError] = useState('')
  const [ticketRuleMessage, setTicketRuleMessage] = useState('')
  const [ticketRuleConfirmOpen, setTicketRuleConfirmOpen] = useState(false)
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
  const searchInputRef = useRef<HTMLInputElement>(null)
  const templateContentRef = useRef<HTMLTextAreaElement>(null)
  const isAdmin = user?.roleCode === 'ADMIN'
  const activeSystemGroupConfig = systemGroups.find((group) => group.key === activeSystemGroup) || systemGroups[0]
  const visibleMenuGroups = useMemo(
    () =>
      menuGroups
        .filter((group) => !group.adminOnly || isAdmin)
        .map((group) => ({
          ...group,
          items: group.items.filter((item) => !item.adminOnly || isAdmin),
        }))
        .filter((group) => group.items.length > 0),
    [isAdmin],
  )

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

  const fetchUsers = useCallback(async () => {
    if (!token || activeMenu !== '用户管理') return
    if (!isAdmin) {
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
  }, [activeMenu, handleAuthExpired, isAdmin, token, userEnabledFilter, userKeyword, userPage, userPageSize, userRoleFilter])

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

  function openCreateUser() {
    setUserFormMode('create')
    setEditingUser(null)
    setUserForm(emptyUserForm)
    setUserFormError('')
    setUserFormOpen(true)
  }

  function openEditUser(nextUser: ManagedUser) {
    setUserFormMode('edit')
    setEditingUser(nextUser)
    setUserForm({
      account: nextUser.account,
      displayName: nextUser.displayName,
      email: nextUser.email,
      roleCode: nextUser.roleCode,
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
    const activeMailboxStepConfig = mailboxSteps[activeMailboxStepIndex] || mailboxSteps[0]

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
                      {!sidebarCollapsed && item.badge && (
                        <span className={item.tone ? `menu-badge ${item.tone}` : 'menu-badge'}>
                          {item.badge}
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
                    <small>{user.roleCode === 'ADMIN' ? '系统管理员' : '客服处理人员'}</small>
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

          {activeMenu === '用户管理' ? (
            <section className="app-content user-page" aria-label="用户管理">
              <div className="content-title">
                <div>
                  <h1>用户管理</h1>
                  <p>维护后台账号、角色和启停状态；管理员可创建处理人并重置密码。</p>
                </div>
                <div className="content-actions">
                  <button disabled={usersLoading} onClick={fetchUsers} type="button">
                    <RefreshCw size={16} />
                    刷新
                  </button>
                  <button className="primary-action" onClick={openCreateUser} type="button">
                    <Plus size={16} />
                    新建用户
                  </button>
                </div>
              </div>

              {!isAdmin ? (
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
                      <span>启用账号</span>
                      <strong>{usersData?.summary.enabledUsers ?? '--'}</strong>
                      <small>可正常登录系统</small>
                    </div>
                    <div className="user-metric">
                      <span>停用账号</span>
                      <strong>{usersData?.summary.disabledUsers ?? '--'}</strong>
                      <small>不可登录，保留历史归属</small>
                    </div>
                    <div className="user-metric">
                      <span>管理员</span>
                      <strong>{usersData?.summary.adminUsers ?? '--'}</strong>
                      <small>拥有系统管理权限</small>
                    </div>
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
                        {roleOptions.map((role) => (
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
                                <span className={managedUser.roleCode === 'ADMIN' ? 'role-pill admin' : 'role-pill'}>
                                  {roleLabel(managedUser.roleCode)}
                                </span>
                              </td>
                              <td>
                                <span className={managedUser.enabled ? 'state-pill enabled' : 'state-pill disabled'}>
                                  {managedUser.enabled ? '启用' : '停用'}
                                </span>
                              </td>
                              <td>{formatDateTime(managedUser.lastLoginAt)}</td>
                              <td>
                                <div className="user-ops">
                                  <button onClick={() => openEditUser(managedUser)} type="button">
                                    <Edit3 size={14} />
                                    编辑
                                  </button>
                                  <button onClick={() => openResetConfirm(managedUser)} type="button">
                                    <LockKeyhole size={14} />
                                    重置密码
                                  </button>
                                  <button
                                    className={managedUser.enabled ? 'danger' : 'success'}
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
                          <button className="primary-action" onClick={openCreateUser} type="button">新建用户</button>
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
                  <h1>邮箱配置</h1>
                  <p>维护客服邮箱的 IMAP 收信、SMTP 发信、拉取频率和自动回执开关；一期仅支持 IMAP 收信。</p>
                </div>
                <div className="content-actions">
                  <button disabled={mailboxesLoading} onClick={fetchMailboxes} type="button">
                    <RefreshCw size={16} />
                    刷新
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
                      <span>邮箱总数</span>
                      <strong>{mailboxesData?.summary.totalMailboxes ?? '--'}</strong>
                      <small>已配置客服邮箱</small>
                    </div>
                    <div className="user-metric">
                      <span>启用邮箱</span>
                      <strong>{mailboxesData?.summary.enabledMailboxes ?? '--'}</strong>
                      <small>参与后台拉取任务</small>
                    </div>
                    <div className="user-metric">
                      <span>连接正常</span>
                      <strong>{mailboxesData?.summary.okMailboxes ?? '--'}</strong>
                      <small>最近测试通过</small>
                    </div>
                    <div className="user-metric">
                      <span>连接异常</span>
                      <strong>{mailboxesData?.summary.errorMailboxes ?? '--'}</strong>
                      <small>需检查账号或服务器</small>
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
                        <strong>邮箱列表</strong>
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
                                <th>邮箱</th>
                                <th>收发服务器</th>
                                <th>状态</th>
                                <th>拉取频率</th>
                                <th>最近同步</th>
                                <th>操作</th>
                              </tr>
                            </thead>
                            <tbody>
                              {mailboxesData.records.map((mailbox) => (
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
                                    <strong>{mailbox.mailboxName}</strong>
                                    <small>{mailbox.emailAddress}</small>
                                  </td>
                                  <td>
                                    <strong>{mailbox.imapHost}:{mailbox.imapPort}</strong>
                                    <small>{mailbox.smtpHost}:{mailbox.smtpPort}</small>
                                  </td>
                                  <td>
                                    <div className="mailbox-status-cell">
                                      <span className={`state-pill status-${mailbox.connectionStatus.toLowerCase()}`}>
                                        {mailboxStatusLabel(mailbox.connectionStatus)}
                                      </span>
                                      <span className={mailbox.enabled ? 'state-pill enabled' : 'state-pill disabled'}>
                                        {mailbox.enabled ? '启用' : '停用'}
                                      </span>
                                    </div>
                                  </td>
                                  <td>{secondsLabel(mailbox.fetchIntervalSec)}</td>
                                  <td>{formatSyncTime(mailbox.lastFetchAt)}</td>
                                  <td>
                                    <div className="user-ops" onClick={(event) => event.stopPropagation()}>
                                      <button onClick={() => selectMailbox(mailbox)} type="button">
                                        <Edit3 size={14} />
                                        编辑
                                      </button>
                                      <button
                                        className={mailbox.enabled ? 'danger' : 'success'}
                                        onClick={() => openMailboxConfirm(mailbox, mailbox.enabled ? 'disable' : 'enable')}
                                        type="button"
                                      >
                                        {mailbox.enabled ? <PowerOff size={14} /> : <Power size={14} />}
                                        {mailbox.enabled ? '停用' : '启用'}
                                      </button>
                                      <button className="danger" onClick={() => openMailboxConfirm(mailbox, 'delete')} type="button">
                                        <Trash2 size={14} />
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
                        <button className="template-head-action" onClick={openCreateMailbox} type="button">
                          <Plus size={14} />
                          新增
                        </button>
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

                        <div className="mailbox-step-summary">
                          <strong>{activeMailboxStepConfig.label}</strong>
                          <span>{activeMailboxStepConfig.description}</span>
                        </div>

                        <div className="mailbox-step-body">
                          {activeMailboxStep === 'basic' && (
                            <div className="mailbox-form-grid">
                              <label>
                                <span>邮箱名称</span>
                                <input
                                  onChange={(event) => updateMailboxForm({ mailboxName: event.target.value })}
                                  placeholder="例如 客服支持邮箱"
                                  value={mailboxForm.mailboxName}
                                />
                              </label>
                              <label>
                                <span>邮箱地址</span>
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
                              <div className="mailbox-section-title compact">
                                <strong>收信 IMAP</strong>
                                <small>一期通过 IMAP 拉取客户来信并自动建单</small>
                              </div>
                              <div className="mailbox-form-grid">
                                <label>
                                  <span>IMAP 服务器</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ imapHost: event.target.value })}
                                    placeholder="imap.example.com"
                                    value={mailboxForm.imapHost}
                                  />
                                </label>
                                <label>
                                  <span>IMAP 端口</span>
                                  <input
                                    min={1}
                                    max={65535}
                                    onChange={(event) => updateMailboxForm({ imapPort: Number(event.target.value || 993) })}
                                    type="number"
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
                                  <span>IMAP 账号</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ imapUsername: event.target.value })}
                                    placeholder="通常为邮箱地址"
                                    value={mailboxForm.imapUsername}
                                  />
                                </label>
                                <label>
                                  <span>IMAP 密码/授权码</span>
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
                              <div className="mailbox-section-title compact">
                                <strong>发信 SMTP</strong>
                                <small>用于后续处理人回复、自动回执和系统通知发信</small>
                              </div>
                              <div className="mailbox-form-grid">
                                <label>
                                  <span>SMTP 服务器</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ smtpHost: event.target.value })}
                                    placeholder="smtp.example.com"
                                    value={mailboxForm.smtpHost}
                                  />
                                </label>
                                <label>
                                  <span>SMTP 端口</span>
                                  <input
                                    min={1}
                                    max={65535}
                                    onChange={(event) => updateMailboxForm({ smtpPort: Number(event.target.value || 587) })}
                                    type="number"
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
                                  <span>SMTP 账号</span>
                                  <input
                                    onChange={(event) => updateMailboxForm({ smtpUsername: event.target.value })}
                                    placeholder="通常为邮箱地址"
                                    value={mailboxForm.smtpUsername}
                                  />
                                </label>
                                <label>
                                  <span>SMTP 密码/授权码</span>
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
                              <div className="mailbox-section-title compact">
                                <strong>自动回复</strong>
                                <small>拉取频率和自动回执仅影响后续新邮件</small>
                              </div>
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
                </>
              )}
            </section>
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
                    placeholder="agent01@sfonda.local"
                    type="email"
                    value={userForm.email}
                  />
                </label>
                <label>
                  <span>角色</span>
                  <select
                    disabled={userFormSubmitting}
                    onChange={(event) => setUserForm((value) => ({ ...value, roleCode: toRoleCode(event.target.value) }))}
                    value={userForm.roleCode}
                  >
                    {roleOptions.map((role) => (
                      <option key={role.value} value={role.value}>{role.label}</option>
                    ))}
                  </select>
                </label>
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
