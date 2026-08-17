import { Alert, Button, Empty, Input, Pagination, Select, Tag, Typography } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { CircleCheck, Layers, Loader, MessageCircle, TriangleAlert, UserPlus } from 'lucide-react'
import {
  priorityBadgeClass,
  priorityBadgeText,
  statusLabel,
} from '../../constants/status'
import type { TicketPageResponse, TicketStats, TicketSummary } from '../../types/ticket'

type TicketListPageProps = {
  isAdmin: boolean
  keyword: string
  loading: boolean
  onClearFilters: () => void
  onKeywordChange: (value: string) => void
  onOpenDetail: (id: number) => void
  onPageChange: (page: number, size?: number) => void
  onRefresh: () => void
  onSearch: () => void
  onSelectSlaBreached: () => void
  onStatusChange: (status: string) => void
  page: number
  pageSize: number
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

function ticketStatusClass(ticket: TicketSummary) {
  if (ticket.status === 'WAITING_CUSTOMER') return 'waiting'
  if (ticket.status === 'CLOSED') return 'closed'
  return ticket.slaBreached ? 'overdue' : 'processing'
}

export function TicketListPage({
  isAdmin,
  keyword,
  loading,
  onClearFilters,
  onKeywordChange,
  onOpenDetail,
  onPageChange,
  onRefresh,
  onSearch,
  onSelectSlaBreached,
  onStatusChange,
  page,
  pageSize,
  slaBreachedOnly,
  stats,
  statusTab,
  ticketsData,
  ticketsError,
}: TicketListPageProps) {
  const total = ticketsData?.total ?? stats?.totalCount ?? 0
  const statusOptions = [
    { key: 'ALL', label: '全部' },
    { key: 'PENDING_ASSIGN', label: '待分配' },
    { key: 'PROCESSING', label: '处理中' },
    { key: 'WAITING_CUSTOMER', label: '待客户回复' },
    { key: 'CLOSED', label: '已关闭' },
  ]
  const priorityOptions = [
    { key: 'URGENT', label: '紧急', className: 'p1', badge: 'P1' },
    { key: 'HIGH', label: '高', className: 'p2', badge: 'P2' },
    { key: 'NORMAL', label: '普通', className: 'p3', badge: 'P3' },
  ]
  const statsTabs = [
    { label: '全部工单', value: stats?.totalCount ?? ticketsData?.total ?? 0, Icon: Layers, iconClass: 'total' },
    { label: '待分配', value: stats?.pendingAssignCount ?? '-', Icon: UserPlus, iconClass: 'pending' },
    { label: '处理中', value: stats?.processingCount ?? '-', Icon: Loader, iconClass: 'processing' },
    { label: '待客户回复', value: stats?.waitingCustomerCount ?? '-', Icon: MessageCircle, iconClass: 'waiting' },
    { label: '已超时', value: stats?.slaOverdueCount ?? '-', Icon: TriangleAlert, iconClass: 'sla-overdue' },
    { label: '今日已关闭', value: stats?.closedTodayCount ?? '-', Icon: CircleCheck, iconClass: 'closed' },
  ]

  return (
    <div className="tickets-page">
      <div className="tickets-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h2>全部工单</h2>
            <div className="tickets-header-sub">
              当前筛选共 {total} 个工单，统计卡片展示当前权限范围口径
              {!isAdmin ? '；当前仅显示自己负责和未分配工单' : ''}
            </div>
          </div>
          <div className="tickets-header-actions">
            <Button icon={<ReloadOutlined />} onClick={onRefresh} loading={loading}>刷新</Button>
            <Button>导出</Button>
          </div>
        </div>

        <div className="tickets-stat-tabs">
          {statsTabs.map(({ Icon, iconClass, label, value }) => (
            <div key={label} className="tickets-stat-tab">
              <div className="stat-tab-info">
                <div className="label">{label}</div>
                <div className="value">{value}</div>
              </div>
              <div className={`stat-tab-icon ${iconClass}`}>
                <Icon size={16} />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="tickets-body">
        <div className="tickets-filter">
          <Input.Search
            allowClear
            placeholder="搜索工单号、主题、客户..."
            size="small"
            value={keyword}
            onChange={(event) => onKeywordChange(event.target.value)}
            onSearch={onSearch}
            style={{ marginBottom: 16 }}
          />

          <div style={{ marginBottom: 20 }}>
            <div className="tickets-filter-title">我的视图</div>
            {[
              { icon: '★', label: '我的待处理', count: stats?.processingCount ?? 0, color: '#f59e0b' },
              { icon: '◎', label: '我关注的', count: '-', color: '#9ca3af' },
              { icon: '◷', label: '最近更新', count: '-', color: '#9ca3af' },
            ].map((item) => (
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
            {statusOptions.map((item) => {
              const active = statusTab === item.key && !slaBreachedOnly
              return (
                <div key={item.key} className={`tickets-filter-item ${active ? 'active' : ''}`} onClick={() => onStatusChange(item.key)}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span
                      style={{
                        width: 14,
                        height: 14,
                        borderRadius: 3,
                        border: active ? '2px solid #2563eb' : '2px solid #d1d5db',
                        background: active ? '#2563eb' : 'transparent',
                        display: 'inline-block',
                      }}
                    />
                    <span>{item.label}</span>
                  </div>
                  <span className="count">
                    {item.key === 'ALL' ? ticketsData?.total ?? '-' : ticketsData?.records?.filter((ticket) => ticket.status === item.key).length ?? '-'}
                  </span>
                </div>
              )
            })}
          </div>

          <div style={{ marginBottom: 20 }}>
            <div className="tickets-filter-title">优先级</div>
            {priorityOptions.map((priority) => (
              <div key={priority.key} className="tickets-filter-item">
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ width: 14, height: 14, borderRadius: 3, border: '2px solid #d1d5db', display: 'inline-block' }} />
                  <span className={`priority-pill ${priority.className}`}>{priority.badge}</span>
                  <span>{priority.label}</span>
                </div>
                <span className="count">{ticketsData?.records?.filter((ticket) => ticket.priority === priority.key).length ?? '-'}</span>
              </div>
            ))}
          </div>

          <div style={{ marginBottom: 20 }}>
            <div className="tickets-filter-title">SLA状态</div>
            {[
              { label: '即将超时', color: '#f59e0b', count: '-', active: false, onClick: undefined },
              { label: '已超时', color: '#ef4444', count: stats?.slaOverdueCount ?? '-', active: slaBreachedOnly, onClick: onSelectSlaBreached },
              { label: '正常', color: '#10b981', count: '-', active: false, onClick: undefined },
            ].map((item) => (
              <div key={item.label} className={`tickets-filter-item ${item.active ? 'active' : ''}`} onClick={item.onClick}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ width: 8, height: 8, borderRadius: '50%', background: item.color, display: 'inline-block' }} />
                  <span>{item.label}</span>
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

          {ticketsError && <Alert title={ticketsError} type="error" showIcon style={{ marginBottom: 12 }} />}
          <Button block size="small" onClick={onClearFilters}>清空筛选</Button>
        </div>

        <div className="tickets-list-panel">
          <div className="tickets-list-head">
            <div className="sort-label">
              <span>排序：</span>
              <Select
                size="small"
                variant="borderless"
                defaultValue="newest"
                style={{ width: 110 }}
                options={[
                  { value: 'newest', label: '最新更新' },
                  { value: 'priority', label: '优先级' },
                  { value: 'created', label: '创建时间' },
                ]}
              />
            </div>
            <div>{ticketsData && <span style={{ fontSize: 12, color: '#9ca3af' }}>共 {ticketsData.total} 条</span>}</div>
          </div>

          <div className="tickets-list-body">
            {loading && !ticketsData ? (
              <div style={{ padding: 60, textAlign: 'center' }}><Typography.Text type="secondary">加载中...</Typography.Text></div>
            ) : !ticketsData || ticketsData.records.length === 0 ? (
              <div style={{ padding: 60, textAlign: 'center' }}><Empty description="暂无工单" /></div>
            ) : (
              ticketsData.records.map((ticket) => (
                <div key={ticket.id} className="ticket-row" onClick={() => onOpenDetail(ticket.id)}>
                  <div style={{ flexShrink: 0, marginRight: 12, paddingTop: 2 }}>
                    <span className={`priority-pill ${priorityBadgeClass(ticket.priority)}`}>{priorityBadgeText(ticket.priority)}</span>
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
                    <div className="ticket-time">{relativeTicketTime(ticket.createdAt)}</div>
                    <div style={{ textAlign: 'right' }}>
                      <div className={ticket.slaBreached ? 'ticket-sla-overdue' : 'ticket-sla-ok'} style={{ marginBottom: 4 }}>
                        {ticket.slaBreached ? 'SLA已超时' : 'SLA正常'}
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'flex-end' }}>
                        {ticket.assigneeName && <div className="assignee-avatar">{ticket.assigneeName[0]}</div>}
                        {!ticket.assigneeName && <span style={{ fontSize: 12, color: '#9ca3af' }}>未分配</span>}
                        <span className={`ticket-status-tag ${ticketStatusClass(ticket)}`}>{statusLabel(ticket.status)}</span>
                      </div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {ticketsData && ticketsData.records.length > 0 && (
            <div className="tickets-list-foot">
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
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
