import { Alert, Button, Tag, message } from 'antd'
import {
  CircleCheck,
  Clock,
  Folder,
  Inbox,
  Layers,
  Loader,
  LockKeyhole,
  MessageCircle,
  RefreshCw,
  Send,
  Settings,
  Timer,
  TriangleAlert,
} from 'lucide-react'
import {
  priorityBadgeClass,
  priorityBadgeText,
  statusLabel,
} from '../../constants/status'
import type { DashboardSummary, DashboardTodoListResponse } from '../../types/dashboard'

type DashboardPageProps = {
  canOpenTicketList: boolean
  dashboardError: string
  dashboardLoading: boolean
  dashboardSummary: DashboardSummary | null
  dashboardTodos: DashboardTodoListResponse | null
  dashboardUpdatedAt: string | null
  hasPermission: (permission: string) => boolean
  onFetchDashboard: () => void
  onNavigateToTickets: (status?: string, slaBreachedOnly?: boolean) => void
  onOpenTicketDetail: (ticketId: number) => void
  onSetActiveMenu: (menu: string) => void
}

function relativeTime(value: string) {
  if (!value) return '-'
  const diff = Date.now() - new Date(value).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  return `${Math.floor(hours / 24)} 天前`
}

function formatOptionalDateTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

export function DashboardPage({
  canOpenTicketList,
  dashboardError,
  dashboardLoading,
  dashboardSummary,
  dashboardTodos,
  dashboardUpdatedAt,
  hasPermission,
  onFetchDashboard,
  onNavigateToTickets,
  onOpenTicketDetail,
  onSetActiveMenu,
}: DashboardPageProps) {
  const dashboardCards = [
    {
      key: 'total',
      label: '工单总数',
      value: dashboardSummary?.totalCount,
      help: '当前权限范围',
      icon: Layers,
      tone: 'primary',
      onClick: () => onNavigateToTickets('ALL'),
    },
    {
      key: 'pending',
      label: '待分配',
      value: dashboardSummary?.pendingAssignCount,
      help: '等待处理人',
      icon: Clock,
      tone: 'warning',
      onClick: () => onNavigateToTickets('PENDING_ASSIGN'),
    },
    {
      key: 'processing',
      label: '处理中',
      value: dashboardSummary?.processingCount,
      help: '已进入处理',
      icon: Folder,
      tone: 'info',
      onClick: () => onNavigateToTickets('PROCESSING'),
    },
    {
      key: 'overdue',
      label: 'SLA 已超时',
      value: dashboardSummary?.slaOverdueCount,
      help: '优先处理',
      icon: TriangleAlert,
      tone: 'danger',
      onClick: () => onNavigateToTickets('ALL', true),
    },
    {
      key: 'closed',
      label: '今日已关闭',
      value: dashboardSummary?.closedTodayCount,
      help: '今日完成量',
      icon: CircleCheck,
      tone: 'success',
      onClick: () => onNavigateToTickets('CLOSED'),
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
      detail: '全部超时记录，优先响应',
      value: dashboardSummary?.slaOverdueCount ?? 0,
      tone: 'danger',
      icon: TriangleAlert,
      onClick: () => onNavigateToTickets('ALL', true),
    },
    {
      label: '我的待办超时',
      detail: '当前处理人名下已超时',
      value: dashboardTodos?.slaOverdueCount ?? 0,
      tone: 'danger',
      icon: Timer,
      onClick: () => onNavigateToTickets('ALL', true),
    },
    {
      label: '待分配工单',
      detail: '需要尽快指定处理人',
      value: dashboardSummary?.pendingAssignCount ?? 0,
      tone: 'warning',
      icon: Clock,
      onClick: () => onNavigateToTickets('PENDING_ASSIGN'),
    },
    {
      label: '待客户回复工单',
      detail: '跟踪客户补充进度',
      value: dashboardSummary?.waitingCustomerCount ?? 0,
      tone: 'info',
      icon: MessageCircle,
      onClick: () => onNavigateToTickets('WAITING_CUSTOMER'),
    },
  ]
  const dashboardMailEntries = [
    {
      title: '邮箱配置',
      detail: '查看连接状态',
      icon: Settings,
      permission: 'menu:mailboxes',
      onClick: () => onSetActiveMenu('邮箱配置'),
    },
    {
      title: '收件记录',
      detail: '检查拉取结果',
      icon: Inbox,
      permission: 'menu:mail_fetch_logs',
      onClick: () => onSetActiveMenu('收件记录'),
    },
    {
      title: '发件记录',
      detail: '处理失败记录',
      icon: Send,
      permission: 'menu:mail_send_logs',
      onClick: () => onSetActiveMenu('发件记录'),
    },
  ].filter((entry) => hasPermission(entry.permission))

  return (
    <section className="app-content dashboard-page" aria-label="工作台">
      <header className="dashboard-topbar">
        <div className="dashboard-title-block">
          <h1>工作台</h1>
          <span>聚焦待处理工单、SLA 风险与今日处理状态</span>
        </div>
        <div className="dashboard-top-actions">
          <button type="button" disabled>
            <Clock size={15} />
            {dashboardUpdatedAt ? `统计截至 ${dashboardUpdatedAt}` : '统计截至 -'}
          </button>
          <button className="primary" disabled={dashboardLoading} onClick={onFetchDashboard} type="button">
            {dashboardLoading ? <Loader size={15} className="spin-icon" /> : <RefreshCw size={15} />}
            刷新
          </button>
        </div>
      </header>

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
              <button onClick={onFetchDashboard} type="button">重试</button>
            </>
          )}
        </div>
      ) : (
        <>
          {dashboardError && (
            <Alert
              title="部分工作台数据加载失败"
              description={dashboardError}
              type="error"
              showIcon
              action={<Button size="small" danger onClick={onFetchDashboard}>重试</Button>}
              style={{ marginBottom: 12 }}
            />
          )}

          <section className="dashboard-summary-strip" aria-label="工单统计">
            {dashboardCards.map((card) => {
              const Icon = card.icon
              return (
                <button
                  className={`dashboard-summary-item dashboard-summary-item--${card.tone}`}
                  key={card.key}
                  onClick={card.onClick}
                  type="button"
                >
                  <span className="dashboard-summary-icon">
                    <Icon size={17} />
                  </span>
                  <span className="dashboard-summary-copy">
                    <span>{card.label}</span>
                    <small>{card.help}</small>
                  </span>
                  <strong>{dashboardLoading && dashboardSummary == null ? '--' : card.value ?? 0}</strong>
                </button>
              )
            })}
          </section>

          {dashboardLoading && !dashboardSummary && !dashboardTodos ? (
            <main className="dashboard-workspace">
              <section className="dashboard-ledger dashboard-skeleton"><span /><span /><span /></section>
              <aside className="dashboard-profile dashboard-skeleton"><span /><span /><span /></aside>
            </main>
          ) : !dashboardHasAnyData ? (
            <div className="empty-state dashboard-empty">
              <CircleCheck size={36} />
              <strong>暂无工作台数据</strong>
              <p>当前权限范围内没有工单或待办记录。可以刷新数据，或从全部工单查看历史记录。</p>
              <button onClick={onFetchDashboard} type="button">刷新</button>
            </div>
          ) : (
            <main className="dashboard-workspace">
              <section className="dashboard-ledger">
                <header className="dashboard-ledger-toolbar">
                  <div>
                    <h2>我的待办</h2>
                    <span>共 {dashboardTodos?.totalCount ?? 0} 条，按优先级和 SLA 状态处理</span>
                  </div>
                  <div>
                    <button type="button" onClick={() => onNavigateToTickets('PROCESSING')}>处理中</button>
                    <button className="primary" type="button" onClick={() => onNavigateToTickets('ALL')}>全部工单</button>
                  </div>
                </header>

                {dashboardTodoRecords.length === 0 ? (
                  <div className="dashboard-inline-empty">
                    <CircleCheck size={28} />
                    <strong>暂无待办工单</strong>
                    <small>当前没有需要你处理的工单。</small>
                  </div>
                ) : (
                  <div className="dashboard-table">
                    <div className="dashboard-table-head">
                      <span>工单</span>
                      <span>客户</span>
                      <span>状态</span>
                      <span>SLA</span>
                      <span>创建时间</span>
                    </div>
                    {dashboardTodoRecords.map((ticket) => {
                      const statusText = ticket.status === 'PENDING_ASSIGN' ? '待分配' : statusLabel(ticket.status)
                      return (
                        <button
                          className="dashboard-table-row"
                          key={ticket.id}
                          onClick={() => {
                            if (!canOpenTicketList) {
                              message.warning('当前账号没有工单列表查看权限')
                              return
                            }
                            onOpenTicketDetail(ticket.id)
                            onSetActiveMenu('全部工单')
                          }}
                          type="button"
                        >
                          <span className="ticket-cell">
                            <strong>{ticket.ticketNo}</strong>
                            <small>{ticket.subject}</small>
                          </span>
                          <span>{ticket.customerEmail}</span>
                          <span>
                            <Tag color={ticket.status === 'WAITING_CUSTOMER' ? 'green' : ticket.status === 'PROCESSING' ? 'blue' : 'orange'}>
                              {statusText}
                            </Tag>
                          </span>
                          <span className={ticket.slaBreached ? 'danger-text' : ''}>
                            {ticket.slaBreached ? '已超时' : formatOptionalDateTime(ticket.slaResponseDeadline)}
                          </span>
                          <span className="time-cell">
                            <span className={`priority-pill ${priorityBadgeClass(ticket.priority)}`}>{priorityBadgeText(ticket.priority)}</span>
                            <small>{relativeTime(ticket.createdAt)}</small>
                          </span>
                        </button>
                      )
                    })}
                  </div>
                )}

                <footer className="dashboard-pager">
                  <span>展示 {dashboardTodoRecords.length} 条</span>
                  <span>共 {dashboardTodos?.totalCount ?? 0} 条</span>
                  <strong>1</strong>
                </footer>
              </section>

              <aside className="dashboard-profile">
                <header className="dashboard-profile-head">
                  <div>
                    <strong>优先处理</strong>
                    <span>当前最需要关注的工单状态</span>
                  </div>
                  <Tag color={(dashboardSummary?.slaOverdueCount ?? 0) > 0 ? 'red' : 'green'}>
                    {(dashboardSummary?.slaOverdueCount ?? 0) > 0 ? `${dashboardSummary?.slaOverdueCount} 个超时` : '暂无超时'}
                  </Tag>
                </header>

                <section className="dashboard-profile-summary">
                  <div><span>活跃工单</span><strong>{dashboardSummary?.activeCount ?? 0}</strong></div>
                  <div><span>我的待办</span><strong>{dashboardTodos?.totalCount ?? 0}</strong></div>
                  <div><span>今日关闭</span><strong>{dashboardSummary?.closedTodayCount ?? 0}</strong></div>
                </section>

                {dashboardMailEntries.length > 0 && (
                  <section className="dashboard-profile-section dashboard-entry-panel">
                    <header>
                      <div>
                        <strong>常用入口</strong>
                        <span>邮件运行相关页面</span>
                      </div>
                    </header>
                    <div className="dashboard-entry-actions">
                      {dashboardMailEntries.map((entry) => {
                        const Icon = entry.icon
                        return (
                          <button key={entry.title} onClick={entry.onClick} type="button">
                            <Icon size={16} />
                            <span><strong>{entry.title}</strong><small>{entry.detail}</small></span>
                          </button>
                        )
                      })}
                    </div>
                  </section>
                )}

                <section className="dashboard-profile-section">
                  <header>
                    <div>
                      <strong>处理队列</strong>
                      <span>点击后进入对应工单筛选</span>
                    </div>
                  </header>
                  <div className="dashboard-risk-list">
                    {dashboardRiskItems.map((item) => {
                      const Icon = item.icon
                      return (
                        <button key={item.label} onClick={item.onClick} type="button">
                          <i className={item.tone}><Icon size={16} /></i>
                          <span><strong>{item.label}</strong><small>{item.detail}</small></span>
                          <b>{item.value}</b>
                        </button>
                      )
                    })}
                  </div>
                </section>
              </aside>
            </main>
          )}
        </>
      )}
    </section>
  )
}
