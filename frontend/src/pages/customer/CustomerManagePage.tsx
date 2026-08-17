import { Alert, Button, Card, Col, Descriptions, Empty, Input, Pagination, Row, Table, Tag, Typography, message } from 'antd'
import { RefreshCw, ShieldCheck } from 'lucide-react'
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

  return (
    <section className="app-content customer-page" aria-label="客户管理">
      <div className="content-title">
        <div>
          <h1>客户管理</h1>
          <p>按客户邮箱聚合历史工单和客户档案；第一版仅提供只读检索、详情和关联工单查看。</p>
        </div>
        <div className="content-actions">
          <button disabled={customersLoading} onClick={onFetchCustomers} type="button">
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
              <small>按当前页 ticketCount 统计。</small>
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
              title={customersError}
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              action={<Button size="small" onClick={onFetchCustomers}>重试</Button>}
            />
          )}

          <Row gutter={[16, 16]}>
            <Col xs={24} xl={9}>
              <Card title="客户列表" extra={<Tag color="blue">只读列表</Tag>} className="customer-card">
                <Input.Search
                  allowClear
                  placeholder="搜索邮箱或展示名"
                  value={customerKeyword}
                  onChange={(event) => onKeywordChange(event.target.value)}
                  onSearch={onSearchCustomers}
                  style={{ marginBottom: 14 }}
                />

                <div className="customer-list">
                  {customersLoading && !customersData ? (
                    <div className="customer-state">
                      <Typography.Text type="secondary">客户列表加载中...</Typography.Text>
                    </div>
                  ) : records.length === 0 ? (
                    <div className="customer-state">
                      <Empty description="暂无客户记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
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
                    onChange={onPageChange}
                    style={{ marginTop: 14 }}
                  />
                )}
              </Card>
            </Col>

            <Col xs={24} xl={15}>
              <Card title="客户详情" extra={<Tag color="green">只读详情</Tag>} className="customer-card">
                {customerDetailLoading && !selectedCustomer ? (
                  <div className="customer-state">
                    <Typography.Text type="secondary">客户详情加载中...</Typography.Text>
                  </div>
                ) : customerDetailError ? (
                  <Alert
                    title={customerDetailError}
                    type="error"
                    showIcon
                    action={<Button size="small" onClick={onFetchCustomerDetail}>重试</Button>}
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
                      title={selectedCustomer.remark?.trim() || '暂无客户备注'}
                      description="备注第一版仅展示，不提供新建或编辑客户入口。"
                    />

                    <div className="customer-section-head">
                      <div>
                        <strong>关联工单</strong>
                        <span>按客户邮箱从工单列表查询</span>
                      </div>
                      <Button size="small" loading={customerTicketsLoading} onClick={onFetchCustomerTickets}>
                        刷新工单
                      </Button>
                    </div>

                    {customerTicketsError && (
                      <Alert title={customerTicketsError} type="error" showIcon style={{ marginBottom: 12 }} />
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
                              onClick={() => onOpenTicket(record, selectedCustomer.email)}
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
  )
}
