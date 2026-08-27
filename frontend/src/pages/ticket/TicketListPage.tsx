	import { Alert, Button, Empty, Input, Pagination, Select, Tag, Typography } from 'antd'
	import {
	  CircleCheck,
	  Clock3,
	  Inbox,
	  Layers,
	  Loader,
	  MessageCircle,
	  RefreshCw,
	  Search,
	  TriangleAlert,
	  UserPlus,
	} from 'lucide-react'
import {
  priorityBadgeClass,
  priorityBadgeText,
  statusLabel,
} from '../../constants/status'
import type { TicketPageResponse, TicketStats, TicketSummary } from '../../types/ticket'
import type { EnterpriseOption } from '../../types/enterprise'
import type { MailboxOption } from '../../types/mailbox'

type TicketListPageProps = {
  isAdmin: boolean
  enterpriseFilter: string
  enterpriseOptions: EnterpriseOption[]
  keyword: string
  loading: boolean
  onClearFilters: () => void
  onEnterpriseFilterChange: (value: string) => void
  onKeywordChange: (value: string) => void
  onMailboxFilterChange: (value: string) => void
  onOpenDetail: (id: number) => void
  onPageChange: (page: number, size?: number) => void
  onRefresh: () => void
  onSearch: () => void
  onSelectSlaBreached: () => void
  onStatusChange: (status: string) => void
  page: number
  pageSize: number
  mailboxFilter: string
  mailboxOptions: MailboxOption[]
  slaBreachedOnly: boolean
  stats: TicketStats | null
  statusTab: string
  ticketsData: TicketPageResponse | null
  ticketsError: string
}

function relativeTicketTime(value: string) {
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

function ticketStatusClass(ticket: TicketSummary) {
  if (ticket.status === 'WAITING_CUSTOMER') return 'waiting'
  if (ticket.status === 'CLOSED') return 'closed'
  return ticket.slaBreached ? 'overdue' : 'processing'
}

export function TicketListPage({
  enterpriseFilter,
  enterpriseOptions,
  isAdmin,
  keyword,
  loading,
  onClearFilters,
  onEnterpriseFilterChange,
  onKeywordChange,
  onMailboxFilterChange,
  onOpenDetail,
  onPageChange,
  onRefresh,
  onSearch,
  onSelectSlaBreached,
  onStatusChange,
  page,
  pageSize,
  mailboxFilter,
  mailboxOptions,
  slaBreachedOnly,
  stats,
  statusTab,
  ticketsData,
  ticketsError,
}: TicketListPageProps) {
  const total = ticketsData?.total ?? stats?.totalCount ?? 0
  const currentRecords = ticketsData?.records ?? []
  const statusOptions = [
    { key: 'ALL', label: '全部', value: ticketsData?.total ?? stats?.totalCount ?? 0 },
    { key: 'PENDING_ASSIGN', label: '待分配', value: stats?.pendingAssignCount ?? '-' },
    { key: 'PROCESSING', label: '处理中', value: stats?.processingCount ?? '-' },
    { key: 'WAITING_CUSTOMER', label: '待客户回复', value: stats?.waitingCustomerCount ?? '-' },
    { key: 'CLOSED', label: '已关闭', value: '-' },
  ]
  const statsTabs = [
    { key: 'ALL', label: '全部工单', value: stats?.totalCount ?? ticketsData?.total ?? 0, Icon: Layers, tone: 'primary', onClick: () => onStatusChange('ALL') },
    { key: 'PENDING_ASSIGN', label: '待分配', value: stats?.pendingAssignCount ?? '-', Icon: UserPlus, tone: 'warning', onClick: () => onStatusChange('PENDING_ASSIGN') },
    { key: 'PROCESSING', label: '处理中', value: stats?.processingCount ?? '-', Icon: Loader, tone: 'info', onClick: () => onStatusChange('PROCESSING') },
    { key: 'WAITING_CUSTOMER', label: '待客户回复', value: stats?.waitingCustomerCount ?? '-', Icon: MessageCircle, tone: 'success', onClick: () => onStatusChange('WAITING_CUSTOMER') },
    { key: 'SLA_OVERDUE', label: 'SLA 已超时', value: stats?.slaOverdueCount ?? '-', Icon: TriangleAlert, tone: 'danger', onClick: onSelectSlaBreached },
    { key: 'CLOSED_TODAY', label: '今日已关闭', value: stats?.closedTodayCount ?? '-', Icon: CircleCheck, tone: 'success', onClick: () => onStatusChange('CLOSED') },
  ]
  const pageQualityStats = [
    { key: 'link', label: '疑似断链', value: currentRecords.filter((ticket) => ticket.linkSuspect).length, detail: '需要核对邮件关联' },
    { key: 'unassigned', label: '未分配', value: currentRecords.filter((ticket) => !ticket.assigneeName).length, detail: '可能影响响应时效' },
    { key: 'unreplied', label: '未回复', value: currentRecords.filter((ticket) => !ticket.hasReplied).length, detail: '还没有人工回复记录' },
  ]
  const warningRecords = currentRecords
    .filter((ticket) => ticket.slaBreached || ticket.priority === 'URGENT' || !ticket.assigneeName || ticket.linkSuspect)
    .slice(0, 4)
  const actionSuggestions = [
    {
      key: 'overdue',
      title: (stats?.slaOverdueCount ?? 0) > 0 ? '先处理 SLA 已超时工单' : '当前没有超时工单',
      detail: (stats?.slaOverdueCount ?? 0) > 0 ? '建议优先进入超时视图，避免继续扩大风险。' : '可以继续关注处理中和待客户回复工单。',
      tone: (stats?.slaOverdueCount ?? 0) > 0 ? 'danger' : 'success',
    },
    {
      key: 'pending',
      title: (stats?.pendingAssignCount ?? 0) > 0 ? '待分配工单需要指定处理人' : '待分配队列为空',
      detail: (stats?.pendingAssignCount ?? 0) > 0 ? '分配后才能进入明确责任人的处理链路。' : '当前分配链路暂时正常。',
      tone: (stats?.pendingAssignCount ?? 0) > 0 ? 'warning' : 'success',
    },
    {
      key: 'waiting',
      title: (stats?.waitingCustomerCount ?? 0) > 0 ? '待客户回复工单可做跟进' : '暂无待客户回复压力',
      detail: (stats?.waitingCustomerCount ?? 0) > 0 ? '可根据等待时间决定是否补发提醒。' : '客户等待队列较轻。',
      tone: 'info',
    },
  ]
  const activeFilterText = slaBreachedOnly
    ? 'SLA 已超时'
    : statusOptions.find((item) => item.key === statusTab)?.label ?? '全部'

  return (
    <section className="app-content tickets-page" aria-label="全部工单">
      <header className="tickets-topbar">
        <div className="tickets-title-block">
          <h2>全部工单</h2>
          <span>
            当前筛选共 {total} 个工单
            {!isAdmin ? '，当前账号仅查看授权范围内数据' : '，统计为当前权限范围口径'}
          </span>
        </div>
        <div className="tickets-top-actions">
          <Button icon={<RefreshCw size={15} />} onClick={onRefresh} loading={loading}>刷新</Button>
        </div>
      </header>

      <section className="tickets-summary-strip" aria-label="工单统计">
        {statsTabs.map(({ Icon, key, label, onClick, tone, value }) => {
          const active = key === 'SLA_OVERDUE' ? slaBreachedOnly : statusTab === key && !slaBreachedOnly
          return (
            <button
              className={`tickets-summary-item tickets-summary-item--${tone} ${active ? 'active' : ''}`}
              key={key}
              onClick={onClick}
              type="button"
            >
              <span className="tickets-summary-icon"><Icon size={17} /></span>
              <span className="tickets-summary-copy">
                <span>{label}</span>
                <small>{key === 'SLA_OVERDUE' ? '点击筛选超时' : '点击筛选状态'}</small>
              </span>
              <strong>{value}</strong>
            </button>
          )
        })}
      </section>

      <main className="tickets-workspace">
        <section className="tickets-ledger">
          <header className="tickets-ledger-toolbar">
            <div>
              <h3>工单列表</h3>
              <span>当前视图：{activeFilterText}</span>
            </div>
            <div>
              <Select
                size="small"
                defaultValue="newest"
                style={{ width: 116 }}
                options={[
                  { value: 'newest', label: '最新更新' },
                  { value: 'priority', label: '优先级' },
                  { value: 'created', label: '创建时间' },
                ]}
              />
            </div>
          </header>

        <section className="tickets-inline-filters" aria-label="筛选条件">
            <div className="tickets-scope-filters">
              <Select
                aria-label="企业筛选"
                size="small"
                value={enterpriseFilter}
                onChange={onEnterpriseFilterChange}
                options={[{ value: 'ALL', label: '全部企业' }, ...enterpriseOptions.map((item) => ({ value: String(item.id), label: item.enterpriseName }))]}
              />
              <Select
                aria-label="邮箱筛选"
                size="small"
                value={mailboxFilter}
                onChange={onMailboxFilterChange}
                options={[{ value: 'ALL', label: '全部邮箱' }, ...mailboxOptions.map((item) => ({ value: String(item.id), label: item.mailboxName }))]}
              />
            </div>
            <label className="tickets-search-box">
              <Search size={14} />
              <Input
                allowClear
                variant="borderless"
                placeholder="搜索工单号、主题、客户"
                size="small"
                value={keyword}
                onChange={(event) => onKeywordChange(event.target.value)}
                onPressEnter={onSearch}
              />
            </label>

            <div className="tickets-filter-chips" aria-label="工单状态">
              {statusOptions.map((item) => {
                const active = statusTab === item.key && !slaBreachedOnly
                return (
                  <button
                    className={active ? 'active' : ''}
                    key={item.key}
                    onClick={() => onStatusChange(item.key)}
                    type="button"
                  >
                    <span>{item.label}</span>
                    <b>{item.value}</b>
                  </button>
                )
              })}
              <button className={slaBreachedOnly ? 'active danger' : ''} onClick={onSelectSlaBreached} type="button">
                <span>SLA 已超时</span>
                <b>{stats?.slaOverdueCount ?? '-'}</b>
              </button>
            </div>

            <button className="tickets-reset-filter" type="button" onClick={onClearFilters}>重置</button>
          </section>

          <div className="tickets-table-shell">
            {ticketsError && <Alert title={ticketsError} type="error" showIcon className="tickets-inline-alert" />}

            <div className="tickets-table">
              <div className="tickets-table-head">
                <span>工单</span>
                <span>企业 / 邮箱</span>
                <span>客户</span>
                <span>处理人</span>
                <span>状态</span>
                <span>SLA</span>
                <span>创建时间</span>
              </div>

              {loading && !ticketsData ? (
                <div className="tickets-table-state"><Typography.Text type="secondary">加载中...</Typography.Text></div>
              ) : !ticketsData || currentRecords.length === 0 ? (
                <div className="tickets-table-state"><Empty description="暂无工单" /></div>
              ) : (
                currentRecords.map((ticket) => (
                  <button key={ticket.id} className="tickets-table-row" onClick={() => onOpenDetail(ticket.id)} type="button">
                    <span className="ticket-main-cell">
                      <span className={`priority-pill ${priorityBadgeClass(ticket.priority)}`}>{priorityBadgeText(ticket.priority)}</span>
                      <span>
                        <strong>{ticket.ticketNo}</strong>
                        <small>{ticket.subject}</small>
                      </span>
                      {ticket.linkSuspect && <Tag color="warning">疑似断链</Tag>}
                    </span>
                    <span className="ticket-muted-cell">
                      <strong>{ticket.enterpriseName || `企业 #${ticket.enterpriseId}`}</strong>
                      <small>{ticket.mailboxName || `邮箱 #${ticket.mailboxId}`}</small>
                    </span>
                    <span className="ticket-muted-cell">
                      <strong>{ticket.customerEmail}</strong>
                      <small>客户档案</small>
                    </span>
                    <span>
                      {ticket.assigneeName ? (
                        <span className="ticket-assignee"><i>{ticket.assigneeName[0]}</i>{ticket.assigneeName}</span>
                      ) : (
                        <span className="ticket-unassigned">未分配</span>
                      )}
                    </span>
                    <span><span className={`ticket-status-tag ${ticketStatusClass(ticket)}`}>{statusLabel(ticket.status)}</span></span>
                    <span className={ticket.slaBreached ? 'ticket-sla-overdue' : 'ticket-sla-ok'}>
                      {ticket.slaBreached ? '已超时' : formatOptionalDateTime(ticket.slaResponseDeadline)}
                    </span>
                    <span className="ticket-time-cell">{relativeTicketTime(ticket.createdAt)}</span>
                  </button>
                ))
              )}
            </div>
          </div>

          {ticketsData && currentRecords.length > 0 && (
            <footer className="tickets-pager">
              <span>展示 {currentRecords.length} 条</span>
              <Pagination
                current={page}
                pageSize={pageSize}
                total={ticketsData.total}
                showSizeChanger
                pageSizeOptions={[10, 20, 50]}
                showTotal={(count) => `共 ${count} 条`}
                onChange={onPageChange}
                size="small"
              />
            </footer>
          )}
        </section>

        <aside className="tickets-profile">
          <header className="tickets-profile-head">
            <div>
              <strong>处理辅助</strong>
              <span>基于当前列表给出可执行信息</span>
            </div>
            <Tag color={slaBreachedOnly ? 'red' : 'blue'}>{activeFilterText}</Tag>
          </header>

          <section className="tickets-profile-section">
            <header>
              <div>
                <strong>当前页预警</strong>
                <span>从当前列表中自动挑出高风险项</span>
              </div>
            </header>
            {warningRecords.length > 0 ? (
              <div className="tickets-warning-list">
                {warningRecords.map((ticket) => (
                  <button key={ticket.id} onClick={() => onOpenDetail(ticket.id)} type="button">
                    <i className={ticket.slaBreached ? 'danger' : ticket.priority === 'URGENT' ? 'warning' : 'info'}>
                      {ticket.slaBreached ? <TriangleAlert size={16} /> : !ticket.assigneeName ? <UserPlus size={16} /> : <Inbox size={16} />}
                    </i>
                    <span>
                      <strong>{ticket.ticketNo}</strong>
                      <small>{ticket.slaBreached ? 'SLA 已超时' : !ticket.assigneeName ? '未分配处理人' : ticket.linkSuspect ? '疑似断链' : '紧急优先级'}</small>
                    </span>
                  </button>
                ))}
              </div>
            ) : (
              <div className="tickets-empty-note">当前页没有明显风险项。</div>
            )}
          </section>

          <section className="tickets-profile-section">
            <header>
              <div>
                <strong>数据质量</strong>
                <span>基于当前页列表检查</span>
              </div>
            </header>
            <div className="tickets-quality-grid">
              {pageQualityStats.map((item) => (
                <div key={item.key}>
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                  <small>{item.detail}</small>
                </div>
              ))}
            </div>
          </section>

          <section className="tickets-profile-section">
            <header>
              <div>
                <strong>处理建议</strong>
                <span>基于全局统计判断</span>
              </div>
            </header>
            <div className="tickets-suggestion-list">
              {actionSuggestions.map((item) => (
                <div className={item.tone} key={item.key}>
                  <strong>{item.title}</strong>
                  <span>{item.detail}</span>
                </div>
              ))}
            </div>
          </section>

          <section className="tickets-profile-section">
            <header>
              <div>
                <strong>数据范围</strong>
                <span>由当前账号权限决定</span>
              </div>
            </header>
            <div className="tickets-scope-note">
              <Inbox size={16} />
              <span>{isAdmin ? '管理员可查看当前权限范围内全部工单。' : '非管理员仅查看授权范围内工单。'}</span>
            </div>
            <div className="tickets-scope-note">
              <Clock3 size={16} />
              <span>列表按当前接口返回结果展示，排序控件仅作为视图选择。</span>
            </div>
          </section>
        </aside>
      </main>
    </section>
  )
}
