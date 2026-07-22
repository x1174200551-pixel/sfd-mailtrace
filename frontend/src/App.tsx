import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import {
  Bell,
  CalendarDays,
  ChevronDown,
  CircleCheck,
  CircleHelp,
  Clock,
  Folder,
  Home,
  Inbox,
  Layers,
  ListChecks,
  LogOut,
  Mail,
  Menu,
  MessageCircle,
  PanelLeftClose,
  PanelLeftOpen,
  PieChart,
  Search,
  Send,
  Settings,
  SlidersHorizontal,
  Timer,
  TriangleAlert,
  UserRound,
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

type MenuItem = {
  title: string
  icon: LucideIcon
  badge?: string
  tone?: 'primary' | 'warning' | 'danger' | 'new'
}

type MenuGroup = {
  title: string
  items: MenuItem[]
}

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
    items: [
      { title: '邮箱配置', icon: Settings },
      { title: '收件记录', icon: Inbox },
      { title: '发件记录', icon: Send, badge: '3', tone: 'primary' },
    ],
  },
  {
    title: 'SLA管理',
    items: [
      { title: 'SLA策略', icon: Timer },
      { title: '工作日历', icon: CalendarDays },
      { title: '超时记录', icon: TriangleAlert },
      { title: '统计报表', icon: PieChart, badge: 'NEW', tone: 'new' },
    ],
  },
  {
    title: '系统管理',
    items: [
      { title: '系统设置', icon: SlidersHorizontal },
      { title: '通知模板', icon: Bell },
      { title: '操作日志', icon: ListChecks },
    ],
  },
]

const features = [
  { mark: 'M', title: '自动收取邮件', text: 'IMAP/POP3 实时同步' },
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

async function requestApi<T>(url: string, options: RequestInit = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })
  const body = (await response.json()) as BasicResult<T>

  if (!response.ok || body.code !== 0) {
    throw new Error(body.message || `请求失败：${response.status}`)
  }

  return body.data
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
  const searchInputRef = useRef<HTMLInputElement>(null)

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
            {menuGroups.map((group) => (
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
        </main>
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
