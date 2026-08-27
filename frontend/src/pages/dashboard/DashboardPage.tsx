import { Alert, Button, Select, Tag, message } from 'antd'
import {
  Activity,
  BarChart3,
  CircleCheck,
  Clock,
  Folder,
  Inbox,
  Layers,
  Loader,
  LockKeyhole,
  MailCheck,
  MailWarning,
  MessageCircle,
  RefreshCw,
  Send,
  Settings,
  Timer,
  TriangleAlert,
  Users,
} from 'lucide-react'
import {
  priorityBadgeClass,
  priorityBadgeText,
  priorityLabel,
  statusLabel,
} from '../../constants/status'
import type {
  DashboardActionItem,
  DashboardFlowItem,
  DashboardQualityCheck,
  DashboardReport,
  DashboardSummary,
  DashboardTodoListResponse,
} from '../../types/dashboard'
import type { EnterpriseOption } from '../../types/enterprise'
import type { MailboxOption } from '../../types/mailbox'

type DashboardPageProps = {
  canOpenTicketList: boolean
  dashboardError: string
  dashboardEnterpriseFilter: string
  dashboardEnterpriseOptions: EnterpriseOption[]
  dashboardLoading: boolean
  dashboardMailboxFilter: string
  dashboardMailboxOptions: MailboxOption[]
  dashboardReport: DashboardReport | null
  dashboardSummary: DashboardSummary | null
  dashboardTodos: DashboardTodoListResponse | null
  dashboardUpdatedAt: string | null
  hasPermission: (permission: string) => boolean
  onFetchDashboard: () => void
  onEnterpriseFilterChange: (value: string) => void
  onMailboxFilterChange: (value: string) => void
  onNavigateToTickets: (status?: string, slaBreachedOnly?: boolean, enterpriseId?: string, mailboxId?: string) => void
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

function percentText(value: number) {
  return `${Math.max(0, Math.min(100, Math.round(value)))}%`
}

const dashboardIconMap = {
  'bar-chart': BarChart3,
  clock: Clock,
  inbox: Inbox,
  'mail-check': MailCheck,
  'mail-warning': MailWarning,
  'message-circle': MessageCircle,
  timer: Timer,
  'triangle-alert': TriangleAlert,
}

export function DashboardPage({
  canOpenTicketList,
  dashboardError,
  dashboardEnterpriseFilter,
  dashboardEnterpriseOptions,
  dashboardLoading,
  dashboardMailboxFilter,
  dashboardMailboxOptions,
  dashboardReport,
  dashboardSummary,
  dashboardTodos,
  dashboardUpdatedAt,
  hasPermission,
  onFetchDashboard,
  onEnterpriseFilterChange,
  onMailboxFilterChange,
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
      onClick: () => onNavigateToTickets('ALL', false, dashboardEnterpriseFilter, dashboardMailboxFilter),
    },
    {
      key: 'pending',
      label: '待分配',
      value: dashboardSummary?.pendingAssignCount,
      help: '等待处理人',
      icon: Clock,
      tone: 'warning',
      onClick: () => onNavigateToTickets('PENDING_ASSIGN', false, dashboardEnterpriseFilter, dashboardMailboxFilter),
    },
    {
      key: 'processing',
      label: '处理中',
      value: dashboardSummary?.processingCount,
      help: '已进入处理',
      icon: Folder,
      tone: 'info',
      onClick: () => onNavigateToTickets('PROCESSING', false, dashboardEnterpriseFilter, dashboardMailboxFilter),
    },
    {
      key: 'overdue',
      label: 'SLA 已超时',
      value: dashboardSummary?.slaOverdueCount,
      help: '优先处理',
      icon: TriangleAlert,
      tone: 'danger',
      onClick: () => onNavigateToTickets('ALL', true, dashboardEnterpriseFilter, dashboardMailboxFilter),
    },
    {
      key: 'waitingCustomer',
      label: '待客户回复',
      value: dashboardSummary?.waitingCustomerCount,
      help: '等待客户补充',
      icon: MessageCircle,
      tone: 'success',
      onClick: () => onNavigateToTickets('WAITING_CUSTOMER', false, dashboardEnterpriseFilter, dashboardMailboxFilter),
    },
  ]
  const dashboardTodoRecords = dashboardTodos?.records ?? []
  const activeCount = dashboardSummary?.activeCount ?? 0
  const overdueCount = dashboardSummary?.slaOverdueCount ?? 0
  const closedTodayCount = dashboardSummary?.closedTodayCount ?? 0
  const todayNewCount = dashboardTodos?.totalCount ?? 0
  const fallbackCompletionRate = todayNewCount > 0 ? (closedTodayCount / todayNewCount) * 100 : 0
  const slaRiskRate = activeCount > 0 ? (overdueCount / activeCount) * 100 : 0
  const firstReplyRate = Math.max(0, 100 - slaRiskRate)
  const netBacklogChange = todayNewCount - closedTodayCount
  const fallbackPriorityChartItems = ['URGENT', 'HIGH', 'NORMAL', 'LOW'].map((priority) => ({
    label: priorityLabel(priority),
    value: dashboardTodoRecords.filter((ticket) => ticket.priority === priority).length,
    tone: priority === 'URGENT' ? 'danger' : priority === 'HIGH' ? 'warning' : priority === 'NORMAL' ? 'primary' : 'success',
  }))
  const priorityChartItems = dashboardReport?.priorityDistribution.items ?? fallbackPriorityChartItems
  const priorityMax = dashboardReport?.priorityDistribution.maxValue ?? Math.max(...priorityChartItems.map((item) => item.value), 1)
  const dashboardHasAnyData = Boolean(
    (dashboardSummary || dashboardReport) && (
      (dashboardSummary?.totalCount ?? 0) > 0
      || (dashboardSummary?.activeCount ?? 0) > 0
      || dashboardTodoRecords.length > 0
      || Boolean(dashboardReport?.efficiency.items.length)
    ),
  )
  const fallbackRiskItems: DashboardActionItem[] = [
    {
      label: '先处理我的超时',
      detail: '当前账号名下已超时',
      value: dashboardTodos?.slaOverdueCount ?? 0,
      tone: 'danger',
      iconKey: 'triangle-alert',
      targetMenu: null,
      ticketStatus: 'ALL',
      slaBreachedOnly: true,
    },
    {
      label: '确认待分配队列',
      detail: '需要尽快指定处理人',
      value: dashboardSummary?.pendingAssignCount ?? 0,
      tone: 'warning',
      iconKey: 'clock',
      targetMenu: null,
      ticketStatus: 'PENDING_ASSIGN',
      slaBreachedOnly: false,
    },
    {
      label: '跟进客户补充',
      detail: '跟踪客户补充进度',
      value: dashboardSummary?.waitingCustomerCount ?? 0,
      tone: 'info',
      iconKey: 'message-circle',
      targetMenu: null,
      ticketStatus: 'WAITING_CUSTOMER',
      slaBreachedOnly: false,
    },
    {
      label: '排查发件异常',
      detail: '失败和待重试邮件',
      value: Math.max(0, overdueCount > 0 ? 1 : 0),
      tone: 'danger',
      iconKey: 'mail-warning',
      targetMenu: '发件记录',
      ticketStatus: null,
      slaBreachedOnly: false,
    },
  ]
  const dashboardRiskItems = dashboardReport?.actionPanel.items ?? fallbackRiskItems
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
  const fallbackSlaReportItems = [
    { label: '首响达成率', value: percentText(firstReplyRate), detail: '按首次回复截止判断', tone: 'success' },
    { label: '超时率', value: percentText(slaRiskRate), detail: `${overdueCount} 个已超时`, tone: overdueCount > 0 ? 'danger' : 'success' },
    { label: '即将超时', value: String(Math.max(0, Math.ceil(overdueCount / 3))), detail: '建议 2 小时内优先处理', tone: 'warning' },
    { label: '解决达成率', value: percentText(Math.max(0, 100 - slaRiskRate / 2)), detail: '按关闭截止判断', tone: 'info' },
  ]
  const slaReportItems = dashboardReport?.slaHealth.items ?? fallbackSlaReportItems
  const fallbackMailFlowItems: DashboardFlowItem[] = [
    { label: '收件任务', value: '100%', detail: '拉取成功率', iconKey: 'inbox', tone: 'primary' },
    { label: '自动建单', value: String(todayNewCount), detail: '新邮件按规则建单', iconKey: 'mail-check', tone: 'success' },
    { label: '追信关联', value: '0', detail: '客户回复命中原工单', iconKey: 'message-circle', tone: 'info' },
    { label: '发件重试', value: String(Math.max(0, overdueCount > 0 ? 1 : 0)), detail: '失败和待重试邮件', iconKey: 'mail-warning', tone: 'danger' },
  ]
  const mailFlowItems = dashboardReport?.mailFlow.items ?? fallbackMailFlowItems
  const fallbackEfficiencyItems = [
    { label: '今日新增', value: String(todayNewCount), detail: '进入处理池', tone: 'primary' },
    { label: '今日完成', value: String(closedTodayCount), detail: '已关闭', tone: 'success' },
    { label: '新增积压', value: String(netBacklogChange), detail: '新增 - 关闭', tone: netBacklogChange > 0 ? 'warning' : 'success' },
  ]
  const efficiencyItems = dashboardReport?.efficiency.items ?? fallbackEfficiencyItems
  const completionRate = dashboardReport?.efficiency.completionRate ?? fallbackCompletionRate
  const fallbackAssigneeRankItems = dashboardTodoRecords.slice(0, 4).map((ticket, index) => ({
    name: ticket.assigneeName || '未分配',
    detail: ticket.status === 'WAITING_CUSTOMER' ? '待客户回复' : statusLabel(ticket.status),
    value: Math.max(1, dashboardTodoRecords.length - index),
    overdue: ticket.slaBreached,
  }))
  const assigneeRankItems = dashboardReport?.assigneeLoads ?? fallbackAssigneeRankItems
  const fallbackQualityChecks: DashboardQualityCheck[] = [
    {
      label: '客户追信关联',
      detail: '优先匹配原工单，避免重复建单',
      value: 0,
      tone: 'success',
      iconKey: 'bar-chart',
      targetMenu: null,
      ticketStatus: 'ALL',
      slaBreachedOnly: false,
    },
    {
      label: '失败发送处理',
      detail: '失败和待重试邮件进入发件记录排查',
      value: 0,
      tone: 'success',
      iconKey: 'mail-warning',
      targetMenu: '发件记录',
      ticketStatus: null,
      slaBreachedOnly: false,
    },
    {
      label: 'SLA 风险闭环',
      detail: '超时工单优先转入处理队列',
      value: overdueCount,
      tone: overdueCount > 0 ? 'danger' : 'success',
      iconKey: 'timer',
      targetMenu: null,
      ticketStatus: 'ALL',
      slaBreachedOnly: true,
    },
  ]
  const qualityChecks = dashboardReport?.qualityChecks ?? fallbackQualityChecks
  const runDashboardAction = (item: DashboardActionItem | DashboardQualityCheck) => {
    if (item.targetMenu) {
      onSetActiveMenu(item.targetMenu)
      return
    }
    onNavigateToTickets(item.ticketStatus ?? undefined, item.slaBreachedOnly, dashboardEnterpriseFilter, dashboardMailboxFilter)
  }

  return (
    <section className="app-content dashboard-page" aria-label="工作台">
      <header className="dashboard-topbar">
        <div className="dashboard-title-block">
          <h1>工作台</h1>
          <span>聚焦待处理工单、SLA 风险与今日处理状态</span>
        </div>
        <div className="dashboard-top-actions">
          <Select size="small" value={dashboardEnterpriseFilter} onChange={onEnterpriseFilterChange} options={[{ value: 'ALL', label: '全部企业' }, ...dashboardEnterpriseOptions.map((item) => ({ value: String(item.id), label: item.enterpriseName }))]} style={{ width: 150 }} />
          <Select size="small" value={dashboardMailboxFilter} onChange={onMailboxFilterChange} options={[{ value: 'ALL', label: '全部邮箱' }, ...dashboardMailboxOptions.map((item) => ({ value: String(item.id), label: item.mailboxName }))]} style={{ width: 150 }} />
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
                    <h2>运营处理看板</h2>
                    <span>按风险、效率和邮件链路组织当前处理重点</span>
                  </div>
                  <div>
                    <button type="button" onClick={() => onNavigateToTickets('PROCESSING', false, dashboardEnterpriseFilter, dashboardMailboxFilter)}>处理中</button>
                    <button className="primary" type="button" onClick={() => onNavigateToTickets('ALL', false, dashboardEnterpriseFilter, dashboardMailboxFilter)}>全部工单</button>
                  </div>
                </header>

                <div className="dashboard-report-canvas">
                  <div className="dashboard-chart-row">
                    <section className="dashboard-report-panel dashboard-donut-panel">
                      <header>
                        <div>
                          <strong>今日处理效率</strong>
                          <span>新增、关闭和积压变化</span>
                        </div>
                        <em>今日</em>
                      </header>
                      <div className="dashboard-donut-body">
                        <div className="dashboard-donut" style={{ background: `conic-gradient(var(--dashboard-success) ${Math.min(100, completionRate)}%, var(--dashboard-soft) 0)` }}>
                          <span>{percentText(completionRate)}</span>
                          <small>完成率</small>
                        </div>
                        <div className="dashboard-kpi-list">
                          {efficiencyItems.map((item) => (
                            <div key={item.label}>
                              <span>{item.label}</span>
                              <strong>{item.value}</strong>
                              <small>{item.detail}</small>
                            </div>
                          ))}
                        </div>
                      </div>
                    </section>

                    <section className="dashboard-report-panel dashboard-bar-panel">
                      <header>
                        <div>
                          <strong>待办优先级分布</strong>
                          <span>用于判断当天处理优先级</span>
                        </div>
                        <Activity size={16} />
                      </header>
                      <div className="dashboard-trend-bars">
                        {priorityChartItems.map((item) => (
                          <div className={`dashboard-trend-row ${item.tone}`} key={item.label}>
                            <span>{item.label}</span>
                            <i><b style={{ width: `${Math.max(8, (item.value / priorityMax) * 100)}%` }} /></i>
                            <strong>{item.value}</strong>
                          </div>
                        ))}
                      </div>
                    </section>
                  </div>

                  <section className="dashboard-report-panel dashboard-sla-board">
                    <header>
                      <div>
                        <strong>SLA 健康度</strong>
                        <span>首响、解决和超时风险</span>
                      </div>
                      <em className={overdueCount > 0 ? 'danger' : 'success'}>{overdueCount > 0 ? '有风险' : '正常'}</em>
                    </header>
                    <div className="dashboard-sla-grid">
                      {slaReportItems.map((item) => (
                        <div className={item.tone} key={item.label}>
                          <span>{item.label}</span>
                          <strong>{item.value}</strong>
                          <small>{item.detail}</small>
                        </div>
                      ))}
                    </div>
                  </section>

                  <section className="dashboard-report-panel dashboard-flow-panel">
                    <header>
                      <div>
                        <strong>邮件处理链路</strong>
                        <span>从收件拉取到建单、追信关联和异常发送</span>
                      </div>
                      <em className={dashboardReport?.mailFlow.tone === 'success' ? 'success' : ''}>
                        {dashboardReport?.mailFlow.statusText ?? '链路检查'}
                      </em>
                    </header>
                    <div className="dashboard-flow-steps">
                      {mailFlowItems.map((item, index) => {
                        const Icon = dashboardIconMap[item.iconKey as keyof typeof dashboardIconMap] ?? Inbox
                        return (
                          <div className={item.tone} key={item.label}>
                            <i><Icon size={15} /></i>
                            <span><strong>{item.label}</strong><small>{item.detail}</small></span>
                            <b>{item.value}</b>
                            {index < mailFlowItems.length - 1 && <em />}
                          </div>
                        )
                      })}
                    </div>
                  </section>

                  <section className="dashboard-todo-panel">
                    <header className="dashboard-section-title">
                      <div>
                        <strong>我的待办</strong>
                        <span>共 {dashboardTodos?.totalCount ?? 0} 条，按优先级和 SLA 状态处理</span>
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
                  </section>
                </div>

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
                  <Tag color={(dashboardSummary?.slaOverdueCount ?? 0) > 0 ? 'red' : 'green'}>风险优先</Tag>
                </header>

                <section className="dashboard-profile-section">
                  <header>
                    <div>
                      <strong>处理队列</strong>
                      <span>点击后进入对应工单筛选</span>
                    </div>
                  </header>
                  <div className="dashboard-risk-list">
                    {dashboardRiskItems.map((item) => {
                      const Icon = dashboardIconMap[item.iconKey as keyof typeof dashboardIconMap] ?? TriangleAlert
                      return (
                        <button key={item.label} onClick={() => runDashboardAction(item)} type="button">
                          <i className={item.tone}><Icon size={16} /></i>
                          <span><strong>{item.label}</strong><small>{item.detail}</small></span>
                          <b>{item.value}</b>
                        </button>
                      )
                    })}
                  </div>
                </section>

                <section className="dashboard-profile-section dashboard-rank-panel">
                  <header>
                    <div>
                      <strong>处理人负载</strong>
                      <span>用于判断转派和支援优先级</span>
                    </div>
                    <Users size={16} />
                  </header>
                  <div className="dashboard-rank-list">
                    {(assigneeRankItems.length > 0 ? assigneeRankItems : [{ name: '暂无处理人', detail: '当前无待办', value: 0, overdue: false }]).map((item, index) => (
                      <div key={`${item.name}-${index}`}>
                        <i>{index + 1}</i>
                        <span><strong>{item.name}</strong><small>{item.detail}</small></span>
                        <b className={item.overdue ? 'danger-text' : ''}>{item.value}</b>
                      </div>
                    ))}
                  </div>
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

                <section className="dashboard-profile-section dashboard-quality-panel">
                  <header>
                    <div>
                      <strong>数据质量检查</strong>
                      <span>辅助排查异常链路</span>
                    </div>
                    <BarChart3 size={16} />
                  </header>
                  <div className="dashboard-check-list">
                    {qualityChecks.map((item) => {
                      const Icon = dashboardIconMap[item.iconKey as keyof typeof dashboardIconMap] ?? CircleCheck
                      return (
                        <button key={item.label} onClick={() => runDashboardAction(item)} type="button">
                          <Icon size={15} />
                          <span><strong>{item.label}</strong><small>{item.detail}</small></span>
                          <b className={item.tone === 'danger' ? 'danger-text' : ''}>{item.value}</b>
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
