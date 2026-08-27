import { Card, Table, Select, Button, Drawer, Tag, Row, Col, DatePicker, Empty, Typography } from 'antd'
import { CloseOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { MailboxOption } from '../../types/mailbox'
import type { MailSendLog, MailSendLogPageResponse, MailSendLogStats } from '../../types/mail-logs'
import type { EnterpriseOption } from '../../types/enterprise'

type MailSendLogPageProps = {
  detail: MailSendLog | null
  error: string
  enterpriseFilter: string
  enterprises: EnterpriseOption[]
  loading: boolean
  mailboxFilter: string
  mailboxes: MailboxOption[]
  onClearFilters: () => void
  onEnterpriseFilterChange: (value: string) => void
  onDetailChange: (detail: MailSendLog | null) => void
  onMailboxFilterChange: (value: string) => void
  onPageChange: (page: number, size: number) => void
  onQuery: () => void
  onRefresh: () => void
  onStatusFilterChange: (value: string) => void
  onTimeRangeChange: (startFrom: string, startTo: string) => void
  onTypeFilterChange: (value: string) => void
  page: number
  pageSize: number
  records: MailSendLogPageResponse | null
  startFrom: string
  startTo: string
  stats: MailSendLogStats | null
  statusFilter: string
  typeFilter: string
}

const sendTypeLabels: Record<string, string> = {
  TEST: '测试',
  AUTO_REPLY: '自动回执',
  ASSIGN_NOTIFY: '分配通知',
  AGENT_REPLY: '客服回复',
}

export function MailSendLogPage({
  detail,
  error,
  enterpriseFilter,
  enterprises,
  loading,
  mailboxFilter,
  mailboxes,
  onClearFilters,
  onEnterpriseFilterChange,
  onDetailChange,
  onMailboxFilterChange,
  onPageChange,
  onQuery,
  onRefresh,
  onStatusFilterChange,
  onTimeRangeChange,
  onTypeFilterChange,
  page,
  pageSize,
  records,
  startFrom,
  startTo,
  stats,
  statusFilter,
  typeFilter,
}: MailSendLogPageProps) {
  return (
    <div style={{ padding: 24, overflow: 'auto', height: '100%' }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, marginBottom: 18 }}>
        <h1 style={{ margin: 0, fontSize: 28, fontWeight: 800, letterSpacing: '-0.03em' }}>发送日志</h1>
        <Button icon={<ReloadOutlined />} onClick={onRefresh} loading={loading}>刷新数据</Button>
      </div>

      {stats && (
        <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
          {[
            { label: '发送次数', value: stats.totalCount, sub: '含测试与自动发送', icon: 'S', cls: '#2563eb', bg: '#eff6ff' },
            { label: '成功任务', value: stats.successCount, sub: 'SMTP 发送正常', icon: 'E', cls: '#10b981', bg: '#ecfdf5' },
            { label: '失败任务', value: stats.failCount, sub: '可查看原因并重试', icon: 'D', cls: '#ef4444', bg: '#fef2f2' },
            { label: '发送中', value: records ? records.total - stats.successCount - stats.failCount : 0, sub: '待发送与重试中', icon: 'N', cls: '#f59e0b', bg: '#fffbeb' },
          ].map((card) => (
            <Col key={card.label} xs={24} sm={12} lg={6}>
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

      {error && (
        <Card size="small" style={{ marginBottom: 16, borderColor: '#ffccc7', background: '#fff2f0' }}>
          <div style={{ textAlign: 'center', padding: '8px 0' }}>
            <div style={{ fontWeight: 600, marginBottom: 4 }}>日志查询失败</div>
            <Typography.Text type="secondary">{error}</Typography.Text>
            <div style={{ marginTop: 8 }}><Button size="small" onClick={onRefresh}>重新加载</Button></div>
          </div>
        </Card>
      )}

      <div style={{ border: '1px solid #e5e7eb', borderRadius: 16, background: '#fff', boxShadow: '0 12px 30px rgba(15,23,42,0.06)', overflow: 'hidden' }}>
        <div style={{ padding: '14px 18px', borderBottom: '1px solid #f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <strong style={{ fontSize: 15 }}>SMTP 发送任务记录</strong>
          {records && <span style={{ display: 'inline-flex', alignItems: 'center', borderRadius: 999, padding: '4px 10px', background: '#eff6ff', color: '#2563eb', fontSize: 12, fontWeight: 800 }}>共 {records.total} 条</span>}
        </div>

        <div style={{ padding: '14px 18px', borderBottom: '1px solid #f1f5f9', display: 'grid', gridTemplateColumns: '1.4fr 1.4fr 1.2fr 1.2fr 2.3fr auto', gap: 10, alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>企业</span>
            <Select value={enterpriseFilter} onChange={onEnterpriseFilterChange} style={{ width: '100%' }} options={[{ value: 'ALL', label: '全部企业' }, ...enterprises.map((item) => ({ value: String(item.id), label: item.enterpriseName }))]} />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>邮箱</span>
            <Select value={mailboxFilter || undefined} onChange={(value) => onMailboxFilterChange(value || '')} placeholder="全部邮箱" allowClear style={{ width: '100%' }} options={mailboxes.map((mailbox) => ({ value: String(mailbox.id), label: mailbox.mailboxName }))} />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>类型</span>
            <Select value={typeFilter === 'ALL' ? undefined : typeFilter} onChange={(value) => onTypeFilterChange(value ?? 'ALL')} placeholder="全部类型" allowClear style={{ width: '100%' }} options={[{ value: 'TEST', label: '测试' }, { value: 'AUTO_REPLY', label: '自动回执' }, { value: 'ASSIGN_NOTIFY', label: '分配通知' }, { value: 'AGENT_REPLY', label: '客服回复' }]} />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>状态</span>
            <Select value={statusFilter === 'ALL' ? undefined : statusFilter} onChange={(value) => onStatusFilterChange(value ?? 'ALL')} placeholder="全部状态" allowClear style={{ width: '100%' }} options={[{ value: 'SUCCESS', label: '成功' }, { value: 'FAILED', label: '失败' }, { value: 'PENDING', label: '待发送' }, { value: 'RETRYING', label: '重试中' }]} />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>时间</span>
            <DatePicker.RangePicker
              showTime={{ format: 'HH:mm' }}
              format="YYYY-MM-DD HH:mm"
              style={{ width: '100%' }}
              value={startFrom && startTo ? [dayjs(startFrom), dayjs(startTo)] : null}
              onChange={(dates) => onTimeRangeChange(dates?.[0]?.format('YYYY-MM-DDTHH:mm:ss') || '', dates?.[1]?.format('YYYY-MM-DDTHH:mm:ss') || '')}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
            <Button onClick={onClearFilters}>清空筛选</Button>
            <Button type="primary" icon={<SearchOutlined />} onClick={onQuery}>查询</Button>
          </div>
        </div>

        <Table<MailSendLog>
          rowKey="id"
          dataSource={records?.records}
          loading={loading}
          locale={{ emptyText: <Empty description="未找到发送日志，请检查邮件发送配置是否已启用。" /> }}
          pagination={{
            current: page,
            pageSize,
            total: records?.total || 0,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20'],
            showTotal: (total) => `共 ${total} 条`,
            onChange: onPageChange,
          }}
          onRow={(record) => ({ onClick: () => onDetailChange(record), style: { cursor: 'pointer' } })}
          columns={[
            { title: '#', width: 50, render: (_: unknown, __: unknown, index: number) => (page - 1) * pageSize + index + 1 },
            { title: '企业', dataIndex: 'enterpriseId', width: 100, render: (value: number) => enterprises.find((item) => item.id === value)?.enterpriseName || `#${value}` },
            { title: '发送时间', dataIndex: 'createdAt', render: (value: string) => value?.replace('T', ' ').slice(0, 16) || '-', width: 150 },
            { title: '收件人', dataIndex: 'toAddress', width: 180 },
            { title: '主题', dataIndex: 'subject', ellipsis: true, width: 250 },
            { title: '类型', dataIndex: 'sendType', width: 90, render: (value: string) => sendTypeLabels[value] || value },
            { title: '模板', dataIndex: 'templateId', width: 110, render: (value: number | null, record) => value ? `${record.templateType || '模板'} #${value}` : '-' },
            { title: '状态', dataIndex: 'sendStatus', width: 80, render: renderSendStatus },
            { title: '重试', dataIndex: 'retryCount', width: 60, render: (value: number, record) => `${value}/${record.maxRetry}` },
            { title: '错误信息', dataIndex: 'errorMessage', ellipsis: true, width: 200, render: (value: string) => value ? <Typography.Text type="danger" style={{ fontSize: 12 }}>{value}</Typography.Text> : '' },
            { title: '操作', width: 60, render: (_value: unknown, record) => <Button type="link" size="small" onClick={(event) => { event.stopPropagation(); onDetailChange(record) }}>详情</Button> },
          ]}
          scroll={{ x: 1000 }}
          size="middle"
        />
      </div>

      <Drawer
        title={<span style={{ fontSize: 16, fontWeight: 700 }}>发送任务详情</span>}
        placement="right"
        size={520}
        onClose={() => onDetailChange(null)}
        open={Boolean(detail)}
        extra={<Button size="small" onClick={() => onDetailChange(null)} icon={<CloseOutlined />}>关闭</Button>}
      >
        {detail && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#fafafa', borderRadius: 10, padding: '14px 18px', border: '1px solid #f0f0f0' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ fontSize: 12, color: '#8c8c8c', fontWeight: 500 }}>任务编号</span>
                <span style={{ fontWeight: 700, fontSize: 15, color: '#262626' }}>{detail.id}</span>
                <span style={{ width: 1, height: 20, background: '#e8e8e8' }} />
                {renderSendStatus(detail.sendStatus)}
              </div>
              <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                {sendTypeLabels[detail.sendType] || detail.sendType}
              </span>
            </div>
            <Card size="small" title={<span style={{ fontSize: 13, fontWeight: 600 }}>发送信息</span>} styles={{ body: { padding: '12px 16px' } }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                {[
                  ['收件人', detail.toAddress],
                  ['邮件主题', detail.subject],
                  ['发送类型', sendTypeLabels[detail.sendType] || detail.sendType],
                  ['创建时间', detail.createdAt?.replace('T', ' ').slice(0, 16) || '-'],
                  ['发送时间', detail.sentAt?.replace('T', ' ').slice(0, 16) || '-'],
                  ['重试次数', `${detail.retryCount}/${detail.maxRetry}`],
                ].map(([label, value]) => (
                  <div key={label}><div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 2 }}>{label}</div><div style={{ fontWeight: 600, fontSize: 13 }}>{value}</div></div>
                ))}
              </div>
            </Card>
            {detail.contentBody && (
              <Card size="small" title={<span style={{ fontSize: 13, fontWeight: 600 }}>邮件正文</span>} styles={{ body: { padding: '12px 16px', background: '#fafafa' } }}>
                <pre style={{ fontSize: 12, whiteSpace: 'pre-wrap', maxHeight: 300, overflow: 'auto', margin: 0, color: '#262626', lineHeight: 1.6 }}>{detail.contentBody}</pre>
              </Card>
            )}
            {detail.errorMessage && (
              <Card size="small" styles={{ header: { background: '#fff2f0', borderBottom: '1px solid #ffccc7' }, body: { padding: '12px 16px', background: '#fff2f0' } }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}><span style={{ color: '#ff4d4f', fontWeight: 700, fontSize: 13 }}>错误详情</span></div>
                <pre style={{ fontSize: 12, whiteSpace: 'pre-wrap', maxHeight: 200, overflow: 'auto', margin: 0, color: '#cf1322', background: '#fff', borderRadius: 6, padding: 10, border: '1px solid #ffccc7', lineHeight: 1.6 }}>{detail.errorMessage}</pre>
              </Card>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}

function renderSendStatus(value: string) {
  if (value === 'SUCCESS') return <Tag color="success">成功</Tag>
  if (value === 'FAILED') return <Tag color="error">失败</Tag>
  if (value === 'PENDING') return <Tag color="processing">待发</Tag>
  return <Tag color="warning">重试中</Tag>
}
