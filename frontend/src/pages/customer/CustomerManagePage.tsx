import { Pagination, message } from 'antd'
import { Clock3, Copy, Inbox, Mail, RefreshCw, Search, ShieldCheck, TicketCheck, UserRound, UsersRound } from 'lucide-react'
import { statusLabel } from '../../constants/status'
import type { CustomerPageResponse, CustomerReadonly } from '../../types/customer'
import type { TicketPageResponse, TicketSummary } from '../../types/ticket'

type CustomerManagePageProps = {
  canReadCustomers: boolean
  customerDetail: CustomerReadonly | null
  customerDetailError: string
  customerDetailLoading: boolean
  customerKeyword: string
  customerPage: number
  customerPageSize: number
  customerTicketsData: TicketPageResponse | null
  customerTicketsError: string
  customerTicketsLoading: boolean
  customersData: CustomerPageResponse | null
  customersError: string
  customersLoading: boolean
  onFetchCustomerDetail: () => void
  onFetchCustomerTickets: () => void
  onFetchCustomers: () => void
  onKeywordChange: (value: string) => void
  onOpenTicket: (ticket: TicketSummary, customerEmail: string) => void
  onPageChange: (page: number, size: number) => void
  onSearchCustomers: () => void
  onSelectCustomer: (email: string) => void
  selectedCustomerEmail: string
}

function customerDisplayName(customer: CustomerReadonly | null) {
  return customer?.displayName?.trim() || customer?.email || '-'
}

function customerInitial(customer: CustomerReadonly | null) {
  const text = customerDisplayName(customer)
  return text === '-' ? 'C' : text.charAt(0).toUpperCase()
}

function formatCustomerDate(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function ticketStatusClass(ticket: TicketSummary) {
  if (ticket.slaBreached) return 'overdue'
  if (ticket.status === 'WAITING_CUSTOMER') return 'waiting'
  if (ticket.status === 'CLOSED') return 'closed'
  return 'processing'
}

export function CustomerManagePage({
  canReadCustomers,
  customerDetail,
  customerDetailError,
  customerDetailLoading,
  customerKeyword,
  customerPage,
  customerPageSize,
  customerTicketsData,
  customerTicketsError,
  customerTicketsLoading,
  customersData,
  customersError,
  customersLoading,
  onFetchCustomerDetail,
  onFetchCustomerTickets,
  onFetchCustomers,
  onKeywordChange,
  onOpenTicket,
  onPageChange,
  onSearchCustomers,
  onSelectCustomer,
  selectedCustomerEmail,
}: CustomerManagePageProps) {
  const records = customersData?.records ?? []
  const selectedCustomer = customerDetail || records.find((customer) => customer.email === selectedCustomerEmail) || null
  const customerRemarkCount = records.filter((customer) => customer.remark?.trim()).length
  const customerWithTicketCount = records.filter((customer) => customer.ticketCount > 0).length
  const recentCustomerCount = records.filter((customer) => (
    customer.lastMailAt ? Date.now() - new Date(customer.lastMailAt).getTime() <= 7 * 24 * 60 * 60 * 1000 : false
  )).length
  const customerTickets = customerTicketsData?.records ?? []
  const breachedTicketCount = customerTickets.filter((ticket) => ticket.slaBreached).length
  const waitingTicketCount = customerTickets.filter((ticket) => ticket.status === 'WAITING_CUSTOMER').length
  const unassignedTicketCount = customerTickets.filter((ticket) => !ticket.assigneeName).length

  return (
    <section className="app-content customer-page" aria-label="客户管理">
      <header className="customer-topbar">
        <div className="customer-title-block">
          <h2>客户管理</h2>
          <span>共 {customersData?.total ?? '-'} 位客户</span>
        </div>
        <div className="customer-topbar-actions">
          <button className="ghost-btn" disabled={customersLoading} onClick={onFetchCustomers} type="button">
            <RefreshCw size={16} />
            刷新
          </button>
        </div>
      </header>

      {!canReadCustomers ? (
        <div className="permission-state">
          <ShieldCheck size={42} />
          <strong>无客户查看权限</strong>
          <p>客户只读页面仅允许管理员和客服处理人访问。</p>
        </div>
      ) : (
        <>
          <section className="customer-summary-strip" aria-label="客户统计">
            <div className="customer-summary-item active">
              <span className="customer-summary-icon"><UsersRound size={17} /></span>
              <span className="customer-summary-copy">
                <span>客户总数</span>
                <small>当前权限范围内客户邮箱</small>
              </span>
              <strong>{customersData?.total ?? '-'}</strong>
            </div>
            <div className="customer-summary-item">
              <span className="customer-summary-icon info"><TicketCheck size={17} /></span>
              <span className="customer-summary-copy">
                <span>有工单客户</span>
                <small>当前页存在历史工单</small>
              </span>
              <strong>{customerWithTicketCount}</strong>
            </div>
            <div className="customer-summary-item">
              <span className="customer-summary-icon success"><Clock3 size={17} /></span>
              <span className="customer-summary-copy">
                <span>7 天内来信</span>
                <small>近期活跃客户</small>
              </span>
              <strong>{recentCustomerCount}</strong>
            </div>
            <div className="customer-summary-item">
              <span className="customer-summary-icon warning"><Mail size={17} /></span>
              <span className="customer-summary-copy">
                <span>备注覆盖</span>
                <small>已有客户备注记录</small>
              </span>
              <strong>{customerRemarkCount}</strong>
            </div>
          </section>

          {customersError && <div className="user-alert customer-alert">{customersError}</div>}

          <div className="customer-layout">
            <section className="customer-list-panel customer-ledger-panel">
              <header className="customer-panel-head ledger-style">
                <div>
                  <strong>客户列表</strong>
                  <span>只读客户档案</span>
                </div>
                <span className="state-pill enabled">{records.length} 条</span>
              </header>

              <div className="user-toolbar customer-toolbar">
                <label className="user-search">
                  <Search size={16} />
                  <input
                    onChange={(event) => onKeywordChange(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') onSearchCustomers()
                    }}
                    placeholder="搜索邮箱或展示名"
                    type="search"
                    value={customerKeyword}
                  />
                </label>
                <button disabled={customersLoading} onClick={onSearchCustomers} type="button">
                  <Search size={15} />
                  查询
                </button>
              </div>

              <div className="customer-list">
                {customersLoading && !customersData ? (
                  <div className="user-loading">
                    {[0, 1, 2, 3].map((item) => <span key={item} />)}
                  </div>
                ) : records.length === 0 ? (
                  <div className="empty-state customer-empty">
                    <UserRound size={42} />
                    <strong>暂无客户记录</strong>
                    <p>可调整搜索条件后重新查询。</p>
                  </div>
                ) : (
                  records.map((customer) => {
                    const active = customer.email === selectedCustomerEmail
                    return (
                      <button
                        key={customer.email}
                        className={active ? 'customer-row active' : 'customer-row'}
                        onClick={() => onSelectCustomer(customer.email)}
                        type="button"
                      >
                        <span className="customer-avatar">{customerInitial(customer)}</span>
                        <span className="customer-row__main">
                          <span className="customer-row__title">
                            <strong>{customerDisplayName(customer)}</strong>
                            {customer.lastMailAt && <em>近期来信</em>}
                            {customer.remark?.trim() && <em className="note">有备注</em>}
                          </span>
                          <span className="customer-row__email">{customer.email}</span>
                          <span className="customer-row__meta">最近 {formatCustomerDate(customer.lastMailAt)}</span>
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
                <footer className="tickets-pager customer-pagination">
                  <span>展示 {records.length} 条</span>
                  <Pagination
                    current={customersData.page ?? customerPage}
                    onChange={onPageChange}
                    pageSize={customerPageSize}
                    pageSizeOptions={[10, 20, 50]}
                    showSizeChanger
                    showTotal={(count) => `共 ${count} 条`}
                    size="small"
                    total={customersData.total}
                  />
                </footer>
              )}
            </section>

            <section className="customer-detail-panel customer-content-panel">
              <header className="customer-panel-head profile-style">
                <div>
                  <strong>客户详情</strong>
                  <span>{selectedCustomer ? selectedCustomer.email : '请选择左侧客户'}</span>
                </div>
                {selectedCustomer && <span className="state-pill enabled">只读</span>}
              </header>

              {customerDetailLoading && !selectedCustomer ? (
                <div className="user-loading">
                  {[0, 1, 2].map((item) => <span key={item} />)}
                </div>
              ) : customerDetailError ? (
                <div className="permission-state customer-error-state">
                  <ShieldCheck size={38} />
                  <strong>客户详情加载失败</strong>
                  <p>{customerDetailError}</p>
                  <button className="retry-button" onClick={onFetchCustomerDetail} type="button">
                    <RefreshCw size={16} /> 重试
                  </button>
                </div>
              ) : !selectedCustomer ? (
                <div className="empty-state customer-empty">
                  <UserRound size={42} />
                  <strong>请选择客户</strong>
                  <p>选择客户后可查看档案信息和关联工单。</p>
                </div>
              ) : (
                <>
                  <div className="customer-detail-head">
                    <span className="customer-detail-avatar">{customerInitial(selectedCustomer)}</span>
                    <div>
                      <strong>{customerDisplayName(selectedCustomer)}</strong>
                      <span>{selectedCustomer.email}</span>
                    </div>
                    <button
                      onClick={() => {
                        void navigator.clipboard?.writeText(selectedCustomer.email)
                        message.success('客户邮箱已复制')
                      }}
                      type="button"
                    >
                      <Copy size={15} />
                      复制邮箱
                    </button>
                  </div>

                  <div className="customer-info-grid">
                    <div>
                      <span>客户来源</span>
                      <strong>{selectedCustomer.id ? '客户档案' : '工单聚合'}</strong>
                    </div>
                    <div>
                      <span>最近来信</span>
                      <strong>{formatCustomerDate(selectedCustomer.lastMailAt)}</strong>
                    </div>
                    <div>
                      <span>关联工单</span>
                      <strong>{selectedCustomer.ticketCount} 条</strong>
                    </div>
                    <div>
                      <span>创建时间</span>
                      <strong>{formatCustomerDate(selectedCustomer.createdAt)}</strong>
                    </div>
                  </div>

                  <section className="customer-note-panel">
                    <span>客户备注</span>
                    <strong>{selectedCustomer.remark?.trim() || '暂无客户备注'}</strong>
                  </section>

                  <div className="customer-section-head">
                    <div>
                      <strong>关联工单</strong>
                      <span>按客户邮箱从工单列表查询</span>
                    </div>
                    <button disabled={customerTicketsLoading} onClick={onFetchCustomerTickets} type="button">
                      <RefreshCw size={15} />
                      {customerTicketsLoading ? '刷新中' : '刷新工单'}
                    </button>
                  </div>

                  {customerTicketsError && <div className="user-alert customer-alert">{customerTicketsError}</div>}

                  <div className="customer-ticket-table">
                    {customerTicketsLoading && customerTickets.length === 0 ? (
                      <div className="user-loading">
                        {[0, 1, 2].map((item) => <span key={item} />)}
                      </div>
                    ) : customerTickets.length === 0 ? (
                      <div className="empty-state customer-empty">
                        <TicketCheck size={42} />
                        <strong>暂无关联工单</strong>
                        <p>该客户当前没有可展示的工单记录。</p>
                      </div>
                    ) : (
                      <table className="user-table customer-ticket-list">
                        <thead>
                          <tr>
                            <th>工单</th>
                            <th>状态</th>
                            <th>SLA</th>
                            <th>创建时间</th>
                          </tr>
                        </thead>
                        <tbody>
                          {customerTickets.map((ticket) => (
                            <tr key={ticket.id}>
                              <td>
                                <button className="customer-ticket-link" onClick={() => onOpenTicket(ticket, selectedCustomer.email)} type="button">
                                  {ticket.ticketNo}
                                </button>
                                <small>{ticket.subject}</small>
                              </td>
                              <td><span className={`ticket-status-tag ${ticketStatusClass(ticket)}`}>{statusLabel(ticket.status)}</span></td>
                              <td>
                                <span className={ticket.slaBreached ? 'customer-sla danger' : 'customer-sla'}>
                                  {ticket.slaBreached ? '已超时' : '正常'}
                                </span>
                              </td>
                              <td>{formatCustomerDate(ticket.createdAt)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>
                </>
              )}
            </section>

            <aside className="customer-side-panel customer-profile-panel">
              <header className="customer-panel-head profile-style">
                <div>
                  <strong>处理辅助</strong>
                  <span>基于当前客户上下文</span>
                </div>
              </header>

              <section className="customer-side-section">
                <strong>客户画像</strong>
                <div className="customer-profile-list">
                  <div>
                    <Mail size={16} />
                    <span>邮箱</span>
                    <b>{selectedCustomer?.email || '-'}</b>
                  </div>
                  <div>
                    <Inbox size={16} />
                    <span>关联工单</span>
                    <b>{selectedCustomer?.ticketCount ?? '-'}</b>
                  </div>
                  <div>
                    <TicketCheck size={16} />
                    <span>最近来信</span>
                    <b>{formatCustomerDate(selectedCustomer?.lastMailAt ?? null)}</b>
                  </div>
                </div>
              </section>

              <section className="customer-side-section">
                <strong>关联工单质量</strong>
                <div className="customer-quality-grid">
                  <div>
                    <span>SLA 超时</span>
                    <b>{breachedTicketCount}</b>
                  </div>
                  <div>
                    <span>待客户回复</span>
                    <b>{waitingTicketCount}</b>
                  </div>
                  <div>
                    <span>未分配</span>
                    <b>{unassignedTicketCount}</b>
                  </div>
                </div>
              </section>

              <section className="customer-side-section">
                <strong>处理建议</strong>
                <div className="customer-suggestion-list">
                  <div className={breachedTicketCount > 0 ? 'danger' : ''}>
                    <span>{breachedTicketCount > 0 ? '优先处理超时工单' : '暂无超时风险'}</span>
                    <small>{breachedTicketCount > 0 ? '建议先进入关联工单处理 SLA 风险。' : '当前客户关联工单 SLA 状态正常。'}</small>
                  </div>
                  <div>
                    <span>{selectedCustomer?.remark?.trim() ? '查看客户备注' : '暂无备注沉淀'}</span>
                    <small>{selectedCustomer?.remark?.trim() || '后续可考虑补充客户标签和备注维护能力。'}</small>
                  </div>
                </div>
              </section>
            </aside>
          </div>
        </>
      )}
    </section>
  )
}
