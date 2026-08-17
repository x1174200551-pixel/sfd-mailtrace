import type { FormEvent, ReactNode, RefObject } from 'react'
import {
  Bell,
  ChevronDown,
  CircleHelp,
  LogOut,
  Mail,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  Search,
  UserRound,
} from 'lucide-react'
import { roleLabel } from '../../constants/roles'
import type { MenuGroup } from '../../constants/menus'
import type { CurrentUser } from '../../types/auth'

export type AppShellSearchResult = {
  groupTitle: string
  title: string
}

type AppShellProps = {
  activeMenu: string
  children: ReactNode
  notificationsOpen: boolean
  onGlobalSearch: () => void
  onHelp: () => void
  onLogout: () => void
  onMenuChange: (menu: string) => void
  onNotificationsOpenChange: (open: boolean) => void
  onProfileOpenChange: (open: boolean) => void
  onSearchResultSelect: (title: string) => void
  onSearchKeywordChange: (keyword: string) => void
  onSidebarCollapsedChange: (collapsed: boolean) => void
  profileOpen: boolean
  searchInputRef: RefObject<HTMLInputElement | null>
  searchKeyword: string
  searchResults: AppShellSearchResult[]
  sendPendingCount: number
  sidebarCollapsed: boolean
  user: CurrentUser
  visibleMenuGroups: MenuGroup[]
}

export function AppShell({
  activeMenu,
  children,
  notificationsOpen,
  onGlobalSearch,
  onHelp,
  onLogout,
  onMenuChange,
  onNotificationsOpenChange,
  onProfileOpenChange,
  onSearchResultSelect,
  onSearchKeywordChange,
  onSidebarCollapsedChange,
  profileOpen,
  searchInputRef,
  searchKeyword,
  searchResults,
  sendPendingCount,
  sidebarCollapsed,
  user,
  visibleMenuGroups,
}: AppShellProps) {
  const userInitial = user.displayName.trim().charAt(0) || user.account.trim().charAt(0).toUpperCase() || 'U'

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onGlobalSearch()
  }

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
                      onMenuChange(item.title)
                      onProfileOpenChange(false)
                      onNotificationsOpenChange(false)
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
          onClick={() => onSidebarCollapsedChange(!sidebarCollapsed)}
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

          <form className="global-search-wrap" onSubmit={handleSearchSubmit} role="search">
            <label className="global-search">
              <Search size={16} />
              <input
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault()
                    onGlobalSearch()
                  }
                }}
                onChange={(event) => onSearchKeywordChange(event.target.value)}
                placeholder="搜索菜单、工单号、客户邮箱或邮件内容"
                ref={searchInputRef}
                type="search"
                value={searchKeyword}
              />
              <span className="shortcut-key">⌘</span>
              <span className="shortcut-key">K</span>
            </label>
            {searchResults.length > 0 && (
              <div className="global-search-results">
                {searchResults.map((item) => (
                  <button key={`${item.groupTitle}-${item.title}`} onClick={() => onSearchResultSelect(item.title)} type="button">
                    <span>{item.title}</span>
                    <small>{item.groupTitle}</small>
                  </button>
                ))}
              </div>
            )}
          </form>

          <div className="topbar-actions">
            <button
              aria-label="帮助"
              className="icon-button"
              onClick={onHelp}
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
                  onNotificationsOpenChange(!notificationsOpen)
                  onProfileOpenChange(false)
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
                  onProfileOpenChange(!profileOpen)
                  onNotificationsOpenChange(false)
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
                  <button type="button" onClick={onLogout}>
                    <LogOut size={16} />
                    退出登录
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {children}
      </main>
    </div>
  )
}
