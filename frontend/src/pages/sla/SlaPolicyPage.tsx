import { Alert, Button, Card, Col, Descriptions, Empty, Input, Row, Select, Space, Switch, Table, Tag, Timeline, Typography } from 'antd'
import { DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import { Plus, RefreshCw, ShieldCheck } from 'lucide-react'
import { hoursLabel } from '../../constants/sla-policies'
import type {
  SlaPolicy,
  SlaPolicyConfirmAction,
  SlaPolicyFormState,
  SlaPolicyListResponse,
  SlaPolicyPreview,
  SlaWorkCalendar,
} from '../../types/sla-policy'

type SelectOption = {
  value: string
  label: string
}

type SlaPolicyPageProps = {
  actionLoading: boolean
  calendarCount: number
  calendarOptions: SelectOption[]
  canCreateSlaPolicies: boolean
  canReadSlaPolicies: boolean
  confirmAction: SlaPolicyConfirmAction
  defaultFilter: string
  enabledFilter: string
  form: SlaPolicyFormState
  keyword: string
  onCancelConfirm: () => void
  onDefaultFilterChange: (value: string) => void
  onEnabledFilterChange: (value: string) => void
  onFetchSlaPolicies: () => void
  onKeywordChange: (value: string) => void
  onOpenCreatePolicy: () => void
  onRequestDelete: (policy: SlaPolicy) => void
  onResetFilters: () => void
  onSavePolicy: () => void
  onSelectPolicy: (policy: SlaPolicy) => void
  onSetDefaultPolicy: (policy: SlaPolicy) => void
  onSubmitConfirm: () => void
  onTogglePolicy: (policy: SlaPolicy, enabled: boolean) => void
  onUpdateForm: (patch: Partial<SlaPolicyFormState>) => void
  policiesData: SlaPolicyListResponse | null
  policiesError: string
  policiesLoading: boolean
  policyDirty: boolean
  preview: SlaPolicyPreview
  previewBaseTime: { format: (template: string) => string }
  resolveHoursInvalid: boolean
  saving: boolean
  selectedPolicy: SlaPolicy | null
  selectedWorkCalendar: SlaWorkCalendar | null
  warningInvalid: boolean
  workCalendars: SlaWorkCalendar[]
  workCalendarsLoading: boolean
  workdayLabel: (workdays?: number[]) => string
}

export function SlaPolicyPage({
  actionLoading,
  calendarCount,
  calendarOptions,
  canCreateSlaPolicies,
  canReadSlaPolicies,
  confirmAction,
  defaultFilter,
  enabledFilter,
  form,
  keyword,
  onCancelConfirm,
  onDefaultFilterChange,
  onEnabledFilterChange,
  onFetchSlaPolicies,
  onKeywordChange,
  onOpenCreatePolicy,
  onRequestDelete,
  onResetFilters,
  onSavePolicy,
  onSelectPolicy,
  onSetDefaultPolicy,
  onSubmitConfirm,
  onTogglePolicy,
  onUpdateForm,
  policiesData,
  policiesError,
  policiesLoading,
  policyDirty,
  preview,
  previewBaseTime,
  resolveHoursInvalid,
  saving,
  selectedPolicy,
  selectedWorkCalendar,
  warningInvalid,
  workCalendars,
  workCalendarsLoading,
  workdayLabel,
}: SlaPolicyPageProps) {
  const records = policiesData?.records ?? []
  const summary = policiesData?.summary

  return (
    <>
      <section className="app-content" aria-label="SLA策略">
        <div className="content-title">
          <div>
            <h1>SLA策略</h1>
            <p>维护首次响应、解决时限、预警阈值和升级阈值，绑定工作日历后按工作时间计算截止时间。</p>
          </div>
          <div className="content-actions">
            <button disabled={policiesLoading} onClick={onFetchSlaPolicies} type="button">
              <RefreshCw size={16} />
              刷新
            </button>
            <button
              className="primary-action"
              disabled={!canCreateSlaPolicies}
              onClick={onOpenCreatePolicy}
              type="button"
            >
              <Plus size={16} />
              新建策略
            </button>
          </div>
        </div>

        {!canReadSlaPolicies ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无 SLA 策略管理权限</strong>
            <p>当前账号没有 SLA 策略查看权限；新建、编辑、启停、设置默认或删除由独立权限控制。</p>
          </div>
        ) : (
          <>
            <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">策略总数</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{summary?.totalCount ?? '--'}</Typography.Title>
                  <Typography.Text type="secondary">启用 {summary?.enabledCount ?? '--'} 条，停用 {summary?.disabledCount ?? '--'} 条</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">启用策略</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{summary?.enabledCount ?? '--'}</Typography.Title>
                  <Typography.Text type="secondary">仅启用策略参与新工单 SLA 计算</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">默认策略</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{summary?.defaultCount ?? '--'}</Typography.Title>
                  <Typography.Text type="secondary">默认策略必须启用，不能停用或删除</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">绑定日历</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{calendarCount || '--'}</Typography.Title>
                  <Typography.Text type="secondary">策略保存时必须选择工作日历</Typography.Text>
                </Card>
              </Col>
            </Row>

            <Alert
              showIcon
              type="info"
              style={{ marginBottom: 16 }}
              title="默认策略只影响后续新建工单；历史工单已有的响应截止、解决截止不自动重算。"
            />

            {policiesError && (
              <Alert
                showIcon
                type="error"
                style={{ marginBottom: 16 }}
                title={policiesError}
                action={<Button size="small" onClick={onFetchSlaPolicies}>重试</Button>}
              />
            )}

            <Row gutter={[16, 16]}>
              <Col xs={24} xl={10}>
                <Card title="策略列表">
                  <Space wrap style={{ width: '100%', marginBottom: 16 }}>
                    <Input
                      allowClear
                      prefix={<SearchOutlined />}
                      placeholder="策略名称"
                      style={{ width: 190 }}
                      value={keyword}
                      onChange={(event) => onKeywordChange(event.target.value)}
                      onPressEnter={() => void onFetchSlaPolicies()}
                    />
                    <Select
                      style={{ width: 126 }}
                      value={enabledFilter}
                      onChange={onEnabledFilterChange}
                      options={[
                        { value: 'ALL', label: '全部状态' },
                        { value: 'true', label: '启用' },
                        { value: 'false', label: '停用' },
                      ]}
                    />
                    <Select
                      style={{ width: 126 }}
                      value={defaultFilter}
                      onChange={onDefaultFilterChange}
                      options={[
                        { value: 'ALL', label: '全部策略' },
                        { value: 'true', label: '默认策略' },
                        { value: 'false', label: '非默认' },
                      ]}
                    />
                    <Button onClick={onResetFilters}>清空筛选</Button>
                  </Space>

                  <Table<SlaPolicy>
                    rowKey="id"
                    size="middle"
                    loading={policiesLoading || workCalendarsLoading}
                    dataSource={records}
                    pagination={false}
                    locale={{
                      emptyText: (
                        <Empty
                          description="还没有 SLA 策略"
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                        >
                          <Button type="primary" onClick={onOpenCreatePolicy}>新建策略</Button>
                        </Empty>
                      ),
                    }}
                    rowClassName={(record) => record.id === form.id ? 'ant-table-row-selected' : ''}
                    onRow={(record) => ({
                      onClick: () => onSelectPolicy(record),
                    })}
                    columns={[
                      {
                        title: '策略',
                        dataIndex: 'policyName',
                        render: (_value: string, record: SlaPolicy) => {
                          const calendar = workCalendars.find((item) => item.id === record.calendarId)
                          return (
                            <Space orientation="vertical" size={4}>
                              <Space wrap>
                                <Typography.Text strong>{record.policyName}</Typography.Text>
                                {record.defaultPolicy && <Tag color="blue">默认</Tag>}
                                <Tag color={record.enabled ? 'green' : 'default'}>{record.enabled ? '启用' : '停用'}</Tag>
                              </Space>
                              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                绑定日历：{calendar?.calendarName || `#${record.calendarId}`}
                                {calendar ? ` · ${workdayLabel(calendar.workdays)} ${calendar.workStartTime}-${calendar.workEndTime}` : ''}
                              </Typography.Text>
                              <Space wrap size={[4, 4]}>
                                <Tag>响应 {hoursLabel(record.responseHours)}</Tag>
                                <Tag>解决 {hoursLabel(record.resolveHours)}</Tag>
                                <Tag color="gold">预警 剩余 {hoursLabel(record.warningRemainHours)}</Tag>
                                <Tag color="orange">升级 {hoursLabel(record.escalateAfterBreachHours)}</Tag>
                              </Space>
                            </Space>
                          )
                        },
                      },
                      {
                        title: '操作',
                        width: 190,
                        render: (_value: unknown, record: SlaPolicy) => (
                          <Space onClick={(event) => event.stopPropagation()}>
                            <Button
                              size="small"
                              disabled={record.defaultPolicy || !record.enabled || actionLoading}
                              onClick={() => void onSetDefaultPolicy(record)}
                            >
                              默认
                            </Button>
                            <Switch
                              size="small"
                              checked={record.enabled}
                              disabled={record.defaultPolicy}
                              loading={actionLoading && form.id === record.id}
                              onChange={(checked) => void onTogglePolicy(record, checked)}
                            />
                          </Space>
                        ),
                      },
                    ]}
                  />
                </Card>
              </Col>

              <Col xs={24} xl={9}>
                <Card
                  title="新建/编辑策略"
                  extra={
                    policyDirty
                      ? <Tag color="orange">有未保存修改</Tag>
                      : selectedPolicy
                        ? <Tag color="green">已保存</Tag>
                        : <Tag>新建草稿</Tag>
                  }
                >
                  <Row gutter={[12, 12]}>
                    <Col span={24}>
                      <Typography.Text strong>策略名称</Typography.Text>
                      <Input
                        value={form.policyName}
                        onChange={(event) => onUpdateForm({ policyName: event.target.value })}
                        placeholder="VIP 客户 2 小时响应"
                        style={{ marginTop: 8 }}
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>policyName，最多 64 字。</Typography.Text>
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>启用策略</Typography.Text>
                      <Select
                        value={String(form.enabled)}
                        onChange={(value) => onUpdateForm({ enabled: value === 'true' })}
                        options={[
                          { value: 'true', label: '启用' },
                          { value: 'false', label: '停用' },
                        ]}
                        style={{ width: '100%', marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>默认策略</Typography.Text>
                      <Select
                        value={String(form.defaultPolicy)}
                        onChange={(value) => onUpdateForm({ defaultPolicy: value === 'true' })}
                        options={[
                          { value: 'true', label: '设为默认' },
                          { value: 'false', label: '非默认' },
                        ]}
                        style={{ width: '100%', marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>首次响应时限（工作小时）</Typography.Text>
                      <Input
                        type="number"
                        min={1}
                        max={9999}
                        value={form.responseHours}
                        onChange={(event) => onUpdateForm({ responseHours: Number(event.target.value || 1) })}
                        style={{ marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>解决时限（工作小时）</Typography.Text>
                      <Input
                        type="number"
                        min={1}
                        max={9999}
                        status={resolveHoursInvalid ? 'error' : undefined}
                        value={form.resolveHours}
                        onChange={(event) => onUpdateForm({ resolveHours: event.target.value })}
                        placeholder="可空"
                        style={{ marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>预警阈值（剩余工作小时）</Typography.Text>
                      <Input
                        type="number"
                        min={1}
                        max={9999}
                        status={warningInvalid ? 'error' : undefined}
                        value={form.warningRemainHours}
                        onChange={(event) => onUpdateForm({ warningRemainHours: Number(event.target.value || 1) })}
                        style={{ marginTop: 8 }}
                      />
                    </Col>
                    <Col xs={24} md={12}>
                      <Typography.Text strong>升级阈值（超时后工作小时）</Typography.Text>
                      <Input
                        type="number"
                        min={1}
                        max={9999}
                        value={form.escalateAfterBreachHours}
                        onChange={(event) => onUpdateForm({ escalateAfterBreachHours: event.target.value })}
                        placeholder="可空"
                        style={{ marginTop: 8 }}
                      />
                    </Col>
                    <Col span={24}>
                      <Typography.Text strong>绑定日历</Typography.Text>
                      <Select
                        showSearch
                        loading={workCalendarsLoading}
                        value={form.calendarId || undefined}
                        placeholder="请选择工作日历"
                        optionFilterProp="label"
                        options={calendarOptions}
                        onChange={(value) => onUpdateForm({ calendarId: value })}
                        style={{ width: '100%', marginTop: 8 }}
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>deadline 计算以所选工作日历为准。</Typography.Text>
                    </Col>
                    <Col span={24}>
                      <Alert
                        type={resolveHoursInvalid || warningInvalid ? 'error' : 'info'}
                        showIcon
                        title={resolveHoursInvalid || warningInvalid ? '策略校验未通过' : '策略校验'}
                        description={
                          resolveHoursInvalid
                            ? '解决时限不能小于首次响应时限。'
                            : warningInvalid
                              ? '预警阈值必须小于首次响应时限。'
                              : form.defaultPolicy
                                ? '默认策略必须保持启用，保存后其他策略会取消默认标记。'
                                : '保存后配置立即生效，仅影响后续新建工单。'
                        }
                      />
                    </Col>
                  </Row>

                  <Space style={{ marginTop: 16 }} wrap>
                    <Button onClick={onOpenCreatePolicy}>新建草稿</Button>
                    <Button
                      type="primary"
                      loading={saving}
                      disabled={
                        !form.policyName.trim()
                        || !form.calendarId
                        || resolveHoursInvalid
                        || warningInvalid
                      }
                      onClick={() => void onSavePolicy()}
                    >
                      保存策略
                    </Button>
                    <Button
                      danger
                      disabled={!selectedPolicy || selectedPolicy.defaultPolicy}
                      icon={<DeleteOutlined />}
                      onClick={() => selectedPolicy && onRequestDelete(selectedPolicy)}
                    >
                      删除
                    </Button>
                  </Space>
                </Card>
              </Col>

              <Col xs={24} xl={5}>
                <Card title="SLA 预览" extra={<Tag color="blue">前端估算</Tag>}>
                  <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                    <Descriptions
                      bordered
                      column={1}
                      size="small"
                      items={[
                        { key: 'calendar', label: '绑定日历', children: selectedWorkCalendar?.calendarName || '未选择' },
                        { key: 'timezone', label: '时区', children: selectedWorkCalendar?.timezone || '-' },
                        { key: 'workdays', label: '工作日', children: workdayLabel(selectedWorkCalendar?.workdays) },
                        {
                          key: 'workTime',
                          label: '工作时段',
                          children: selectedWorkCalendar ? `${selectedWorkCalendar.workStartTime}-${selectedWorkCalendar.workEndTime}` : '-',
                        },
                      ]}
                    />
                    <Descriptions
                      bordered
                      column={1}
                      size="small"
                      items={[
                        { key: 'created', label: '建单时间', children: previewBaseTime.format('YYYY-MM-DD HH:mm') },
                        { key: 'response', label: '首次响应截止', children: preview.responseDeadline.format('YYYY-MM-DD HH:mm') },
                        {
                          key: 'resolve',
                          label: '解决截止',
                          children: preview.resolveDeadline ? preview.resolveDeadline.format('YYYY-MM-DD HH:mm') : '未配置',
                        },
                      ]}
                    />
                    <Timeline
                      items={[
                        {
                          color: 'blue',
                          content: (
                            <span>新建工单：写入策略 ID 与截止时间</span>
                          ),
                        },
                        {
                          color: 'orange',
                          content: (
                            <span>即将超时：{preview.warningAt.format('YYYY-MM-DD HH:mm')}</span>
                          ),
                        },
                        {
                          color: 'red',
                          content: (
                            <span>升级提醒：{preview.escalateAt ? preview.escalateAt.format('YYYY-MM-DD HH:mm') : '未配置'}</span>
                          ),
                        },
                      ]}
                    />
                  </Space>
                </Card>
              </Col>
            </Row>
          </>
        )}
      </section>

      {confirmAction && (
        <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="sla-policy-confirm-title">
          <div className="confirm-modal">
            <h3 id="sla-policy-confirm-title">删除 SLA 策略</h3>
            <p>删除后该策略不再用于后续新工单 SLA 计算，历史工单已有的 SLA 字段保持不变。</p>
            <div className="confirm-target">
              <strong>{confirmAction.policy.policyName}</strong>
              <span>
                响应 {hoursLabel(confirmAction.policy.responseHours)}
                ，解决 {hoursLabel(confirmAction.policy.resolveHours)}
              </span>
            </div>
            <div className="user-modal__foot">
              <button disabled={actionLoading} onClick={onCancelConfirm} type="button">
                取消
              </button>
              <button className="primary-action" disabled={actionLoading} onClick={onSubmitConfirm} type="button">
                {actionLoading ? '删除中...' : '确认删除'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
