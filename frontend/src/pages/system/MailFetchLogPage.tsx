import { Card, Table, Select, Button, Drawer, Tag, Row, Col, DatePicker, Empty, Typography } from 'antd'
import { CloseOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { Mailbox } from '../../types/mailbox'
import type { MailFetchLog, MailFetchLogPageResponse, MailFetchLogStats } from '../../types/mail-logs'

type MailFetchLogPageProps = {
  detail: MailFetchLog | null
  error: string
  loading: boolean
  mailboxFilter: string
  mailboxes: Mailbox[]
  onClearFilters: () => void
  onDetailChange: (detail: MailFetchLog | null) => void
  onMailboxFilterChange: (value: string) => void
  onPageChange: (page: number, size: number) => void
  onQuery: () => void
  onRefresh: () => void
  onSuccessFilterChange: (value: string) => void
  onTimeRangeChange: (startFrom: string, startTo: string) => void
  page: number
  pageSize: number
  records: MailFetchLogPageResponse | null
  startFrom: string
  startTo: string
  stats: MailFetchLogStats | null
  successFilter: string
}

export function MailFetchLogPage({
  detail,
  error,
  loading,
  mailboxFilter,
  mailboxes,
  onClearFilters,
  onDetailChange,
  onMailboxFilterChange,
  onPageChange,
  onQuery,
  onRefresh,
  onSuccessFilterChange,
  onTimeRangeChange,
  page,
  pageSize,
  records,
  startFrom,
  startTo,
  stats,
  successFilter,
}: MailFetchLogPageProps) {
  return (
    <div style={{ padding: '24px', overflow: 'auto', height: '100%' }}>
      <Row justify="space-between" align="middle" style={{ marginBottom: 20 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>拉取日志</Typography.Title>
        <Button icon={<ReloadOutlined />} onClick={onRefresh} loading={loading}>刷新数据</Button>
      </Row>

      {stats && (
        <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
          {[
            { label: '拉取次数', value: stats.totalCount, sub: '含定时与手动触发' },
            { label: '成功任务', value: stats.successCount, sub: 'IMAP 连接与解析正常' },
            { label: '失败任务', value: stats.failCount, sub: '可查看原因并重试' },
            { label: '新建工单', value: stats.totalCreatedTickets, sub: '由成功拉取任务创建' },
          ].map((card, i) => (
            <Col key={card.label} xs={24} sm={12} lg={6}>
              <Card size="small" styles={{ body: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12, padding: '14px 16px' } }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 13, color: '#8c8c8c', fontWeight: 600 }}>{card.label}</div>
                  <div style={{ fontSize: 28, fontWeight: 800, lineHeight: 1.2, marginTop: 6 }}>{card.value}</div>
                  <div style={{ fontSize: 12, color: '#bfbfbf', marginTop: 4 }}>{card.sub}</div>
                </div>
                <div style={{
                  width: 36, height: 36, borderRadius: 10,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontWeight: 900, fontSize: 16,
                  background: ['#eff6ff', '#ecfdf5', '#fef2f2', '#fffbeb'][i],
                  color: ['#2563eb', '#10b981', '#ef4444', '#f59e0b'][i],
                }}>
                  {['F', 'O', 'E', 'T'][i]}
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <Card size="small" style={{ marginBottom: 16 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1.5fr 3fr auto', alignItems: 'center', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>邮箱</span>
            <Select
              value={mailboxFilter || undefined}
              onChange={(value) => onMailboxFilterChange(value || '')}
              placeholder="全部邮箱"
              allowClear
              style={{ width: '100%' }}
              options={mailboxes.map((mailbox) => ({ value: String(mailbox.id), label: mailbox.mailboxName }))}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>结果</span>
            <Select
              value={successFilter === 'ALL' ? undefined : successFilter}
              onChange={(value) => onSuccessFilterChange(value ?? 'ALL')}
              placeholder="全部结果"
              allowClear
              style={{ width: '100%' }}
              options={[{ value: 'true', label: '成功' }, { value: 'false', label: '失败' }]}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 13, color: '#595959', whiteSpace: 'nowrap', fontWeight: 500 }}>时间</span>
            <DatePicker.RangePicker
              showTime={{ format: 'HH:mm' }}
              format="YYYY-MM-DD HH:mm"
              style={{ width: '100%' }}
              value={startFrom && startTo ? [dayjs(startFrom), dayjs(startTo)] : null}
              onChange={(dates) => {
                onTimeRangeChange(
                  dates?.[0]?.format('YYYY-MM-DDTHH:mm:ss') || '',
                  dates?.[1]?.format('YYYY-MM-DDTHH:mm:ss') || '',
                )
              }}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
            <Button onClick={onClearFilters}>清空筛选</Button>
            <Button type="primary" icon={<SearchOutlined />} onClick={onQuery}>查询</Button>
          </div>
        </div>
      </Card>

      {error && (
        <Card size="small" style={{ marginBottom: 16, borderColor: '#ffccc7', background: '#fff2f0' }}>
          <div style={{ textAlign: 'center', padding: '8px 0' }}>
            <div style={{ fontWeight: 600, marginBottom: 4 }}>日志查询失败</div>
            <Typography.Text type="secondary">{error}</Typography.Text>
            <div style={{ marginTop: 8 }}><Button size="small" onClick={onRefresh}>重新加载</Button></div>
          </div>
        </Card>
      )}

      <Card size="small" title={<span style={{ fontSize: 14 }}>IMAP 拉取任务记录</span>} extra={records && <Tag color="blue">共 {records.total} 条</Tag>}>
        <Table<MailFetchLog>
          rowKey="id"
          dataSource={records?.records}
          loading={loading}
          locale={{ emptyText: <Empty description="未找到拉取日志" /> }}
          pagination={{
            current: page,
            pageSize,
            total: records?.total || 0,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20'],
            showTotal: (total) => `共 ${total} 条`,
            onChange: onPageChange,
          }}
          onRow={(record) => ({
            onClick: () => onDetailChange(record),
            style: { cursor: 'pointer' },
          })}
          columns={[
            { title: '#', width: 50, render: (_: unknown, __: unknown, index: number) => (page - 1) * pageSize + index + 1 },
            { title: '开始时间', dataIndex: 'startedAt', render: (value: string) => formatDateTime(value), width: 160 },
            { title: '邮箱地址', dataIndex: 'emailAddress', render: (value: string) => value || '-', width: 180 },
            { title: '邮箱名称', dataIndex: 'mailboxName', render: (value: string) => value || '-', width: 120 },
            { title: '结果', dataIndex: 'success', width: 80, render: (value: boolean) => value ? <Tag color="success">成功</Tag> : <Tag color="error">失败</Tag> },
            { title: '拉取数', dataIndex: 'fetchedCount', width: 70 },
            { title: '新建工单', dataIndex: 'createdTicketCount', width: 80 },
            { title: '关联工单', dataIndex: 'linkedCount', width: 80 },
            { title: '耗时', width: 80, render: (_value: unknown, record) => record.finishedAt ? formatDuration(record.startedAt, record.finishedAt) : '-' },
            { title: '错误摘要', dataIndex: 'errorMessage', width: 200, ellipsis: true, render: (value: string) => value ? <Typography.Text type="danger" style={{ fontSize: 12 }}>{value}</Typography.Text> : '' },
            { title: '操作', width: 60, render: (_value: unknown, record) => <Button type="link" size="small" onClick={(event) => { event.stopPropagation(); onDetailChange(record) }}>详情</Button> },
          ]}
          scroll={{ x: 1050 }}
          size="middle"
        />
      </Card>

      <Drawer
        title={<span style={{ fontSize: 16, fontWeight: 700 }}>拉取任务详情</span>}
        placement="right"
        size={520}
        onClose={() => onDetailChange(null)}
        open={Boolean(detail)}
        extra={<Button size="small" onClick={() => onDetailChange(null)} icon={<CloseOutlined />}>关闭</Button>}
      >
        {detail && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              background: '#fafafa', borderRadius: 10, padding: '14px 18px',
              border: '1px solid #f0f0f0',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ fontSize: 12, color: '#8c8c8c', fontWeight: 500 }}>任务编号</span>
                <span style={{ fontWeight: 700, fontSize: 15, color: '#262626' }}>{detail.id}</span>
                <span style={{ width: 1, height: 20, background: '#e8e8e8' }} />
                <Tag style={{ margin: 0 }} color={detail.success ? 'success' : 'error'}>
                  {detail.success ? '成功' : '失败'}
                </Tag>
              </div>
              <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                {detail.triggerType === 'SCHEDULED' ? '定时任务' : '手动触发'}
              </span>
            </div>

            <Card size="small" title={<span style={{ fontSize: 13, fontWeight: 600 }}>邮箱信息</span>} styles={{ body: { padding: '12px 16px' } }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                {[
                  ['邮箱地址', detail.emailAddress || '-'],
                  ['邮箱名称', detail.mailboxName || '-'],
                  ['触发方式', detail.triggerType === 'SCHEDULED' ? '定时任务' : '手动触发'],
                  ['开始时间', formatDateTime(detail.startedAt)],
                  ['结束时间', detail.finishedAt ? formatDateTime(detail.finishedAt) : '-'],
                  ['耗时', detail.finishedAt ? formatDuration(detail.startedAt, detail.finishedAt) : '-'],
                ].map(([label, value]) => (
                  <div key={label}>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 2 }}>{label}</div>
                    <div style={{ fontWeight: 600, fontSize: 13 }}>{value}</div>
                  </div>
                ))}
              </div>
            </Card>

            <Card size="small" title={<span style={{ fontSize: 13, fontWeight: 600 }}>执行结果</span>} styles={{ body: { padding: '12px 16px' } }}>
              <Row gutter={[12, 12]}>
                <Col span={8}>
                  <div style={{ background: '#f6ffed', borderRadius: 8, padding: '10px 14px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 800, color: '#52c41a' }}>{detail.fetchedCount}</div>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 2 }}>拉取邮件数</div>
                  </div>
                </Col>
                <Col span={8}>
                  <div style={{ background: '#e6f7ff', borderRadius: 8, padding: '10px 14px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 800, color: '#1890ff' }}>{detail.createdTicketCount}</div>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 2 }}>新建工单</div>
                  </div>
                </Col>
                <Col span={8}>
                  <div style={{ background: '#fff7e6', borderRadius: 8, padding: '10px 14px', textAlign: 'center' }}>
                    <div style={{ fontSize: 22, fontWeight: 800, color: '#fa8c16' }}>{detail.linkedCount}</div>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 2 }}>关联工单</div>
                  </div>
                </Col>
              </Row>
            </Card>

            {detail.errorMessage && (
              <Card size="small" styles={{ header: { background: '#fff2f0', borderBottom: '1px solid #ffccc7' }, body: { padding: '12px 16px', background: '#fff2f0' } }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                  <span style={{ color: '#ff4d4f', fontWeight: 700, fontSize: 13 }}>错误详情</span>
                </div>
                <pre style={{ fontSize: 12, whiteSpace: 'pre-wrap', maxHeight: 200, overflow: 'auto', margin: 0, color: '#cf1322', background: '#fff', borderRadius: 6, padding: 10, border: '1px solid #ffccc7', lineHeight: 1.6 }}>{detail.errorMessage}</pre>
              </Card>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}

function formatDateTime(value: string | null) {
  if (!value) return '未登录'
  return value.replace('T', ' ').slice(0, 16)
}

function formatDuration(start: string, end: string) {
  const diff = new Date(end).getTime() - new Date(start).getTime()
  if (diff < 0) return '-'
  const sec = Math.round(diff / 1000)
  if (sec < 60) return `${sec}s`
  if (sec < 3600) return `${Math.floor(sec / 60)}m${sec % 60}s`
  return `${Math.floor(sec / 3600)}h${Math.floor((sec % 3600) / 60)}m`
}
