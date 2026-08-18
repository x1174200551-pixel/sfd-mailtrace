import { Alert, Button, Empty, Input, Select, Space, Switch, Table, Tag, Timeline, Typography } from 'antd'
import { DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import { BellRing, CalendarDays, CheckCircle2, Plus, RefreshCw, ShieldCheck, TimerReset } from 'lucide-react'
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
  const selectedCalendarText = selectedWorkCalendar
    ? `${selectedWorkCalendar.calendarName} / ${workdayLabel(selectedWorkCalendar.workdays)}`
    : '未选择日历'
  const summaryItems = [
    {
      icon: TimerReset,
      tone: 'primary',
      label: '策略总数',
      value: summary?.totalCount ?? '--',
      detail: `启用 ${summary?.enabledCount ?? '--'} / 停用 ${summary?.disabledCount ?? '--'}`,
    },
    {
      icon: CheckCircle2,
      tone: 'success',
      label: '启用策略',
      value: summary?.enabledCount ?? '--',
      detail: '参与后续新工单 SLA 计算',
    },
    {
      icon: BellRing,
      tone: 'warning',
      label: '默认策略',
      value: summary?.defaultCount ?? '--',
      detail: '默认策略必须保持启用',
    },
    {
      icon: CalendarDays,
      tone: 'primary',
      label: '工作日历',
      value: calendarCount || '--',
      detail: selectedCalendarText,
    },
  ]

  return (
    <>
      <section className="app-content sla-page" aria-label="SLA策略">
        <header className="sla-topbar">
          <div className="sla-title-block">
            <h2>SLA策略</h2>
            <span>维护首次响应、解决时限、预警阈值和升级阈值</span>
          </div>
          <div className="sla-top-actions">
            <Button disabled={policiesLoading} icon={<RefreshCw size={16} />} onClick={onFetchSlaPolicies}>
              刷新
            </Button>
            <Button
              type="primary"
              disabled={!canCreateSlaPolicies}
              icon={<Plus size={16} />}
              onClick={onOpenCreatePolicy}
            >
              新建策略
            </Button>
          </div>
        </header>

        {!canReadSlaPolicies ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无 SLA 策略管理权限</strong>
            <p>当前账号没有 SLA 策略查看权限；新建、编辑、启停、设置默认或删除由独立权限控制。</p>
          </div>
        ) : (
          <>
            <section className="sla-summary-strip" aria-label="SLA策略统计">
              {summaryItems.map((item) => {
                const Icon = item.icon
                return (
                  <div key={item.label} className={`sla-summary-item sla-summary-item--${item.tone}`}>
                    <span className="sla-summary-icon"><Icon size={17} /></span>
                    <span className="sla-summary-copy">
                      <span>{item.label}</span>
                      <small>{item.detail}</small>
                    </span>
                    <strong>{item.value}</strong>
                  </div>
                )
              })}
            </section>

            <Alert
              showIcon
              type={policiesError ? 'error' : 'info'}
              className="sla-inline-alert"
              message={policiesError || '默认策略只影响后续新建工单；历史工单已有的响应截止、解决截止不自动重算。'}
              action={policiesError ? <Button size="small" onClick={onFetchSlaPolicies}>重试</Button> : undefined}
            />

            <main className="sla-workspace">
              <section className="sla-panel sla-ledger">
                <header className="sla-panel-head">
                  <div>
                    <h3>策略列表</h3>
                    <span>按当前筛选展示策略，点击行进入编辑</span>
                  </div>
                  <Tag color="blue">{records.length} 条</Tag>
                </header>

                <section className="sla-inline-filters" aria-label="筛选条件">
                  <Input
                    allowClear
                    prefix={<SearchOutlined />}
                    placeholder="策略名称"
                    value={keyword}
                    onChange={(event) => onKeywordChange(event.target.value)}
                    onPressEnter={() => void onFetchSlaPolicies()}
                  />
                  <Select
                    value={enabledFilter}
                    onChange={onEnabledFilterChange}
                    options={[
                      { value: 'ALL', label: '全部状态' },
                      { value: 'true', label: '启用' },
                      { value: 'false', label: '停用' },
                    ]}
                  />
                  <Select
                    value={defaultFilter}
                    onChange={onDefaultFilterChange}
                    options={[
                      { value: 'ALL', label: '全部策略' },
                      { value: 'true', label: '默认策略' },
                      { value: 'false', label: '非默认' },
                    ]}
                  />
                  <Button onClick={onResetFilters}>清空筛选</Button>
                </section>

                <div className="sla-table-shell">
                  <Table<SlaPolicy>
                    rowKey="id"
                    size="middle"
                    loading={policiesLoading || workCalendarsLoading}
                    dataSource={records}
                    pagination={false}
                    locale={{
                      emptyText: (
                        <Empty description="还没有 SLA 策略" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                          <Button type="primary" onClick={onOpenCreatePolicy}>新建策略</Button>
                        </Empty>
                      ),
                    }}
                    rowClassName={(record) => record.id === form.id ? 'ant-table-row-selected' : ''}
                    onRow={(record) => ({ onClick: () => onSelectPolicy(record) })}
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
                                绑定日历：{calendar?.calendarName || '未找到日历'}
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
                </div>
              </section>

              <section className="sla-panel sla-editor-panel">
                <header className="sla-panel-head">
                  <div>
                    <h3>策略编辑</h3>
                    <span>配置截止时间、预警提醒和工作日历</span>
                  </div>
                  {policyDirty
                    ? <Tag color="orange">有未保存修改</Tag>
                    : selectedPolicy
                      ? <Tag color="green">已保存</Tag>
                      : <Tag>新建草稿</Tag>}
                </header>

                <div className="sla-form-grid">
                  <label className="sla-form-wide">
                    <span>策略名称</span>
                    <Input value={form.policyName} onChange={(event) => onUpdateForm({ policyName: event.target.value })} placeholder="VIP 客户 2 小时响应" />
                  </label>
                  <label>
                    <span>启用策略</span>
                    <Select
                      value={String(form.enabled)}
                      onChange={(value) => onUpdateForm({ enabled: value === 'true' })}
                      options={[
                        { value: 'true', label: '启用' },
                        { value: 'false', label: '停用' },
                      ]}
                    />
                  </label>
                  <label>
                    <span>默认策略</span>
                    <Select
                      value={String(form.defaultPolicy)}
                      onChange={(value) => onUpdateForm({ defaultPolicy: value === 'true' })}
                      options={[
                        { value: 'true', label: '设为默认' },
                        { value: 'false', label: '非默认' },
                      ]}
                    />
                  </label>
                  <label>
                    <span>首次响应时限</span>
                    <Input type="number" min={1} max={9999} value={form.responseHours} onChange={(event) => onUpdateForm({ responseHours: Number(event.target.value || 1) })} suffix="工作小时" />
                  </label>
                  <label>
                    <span>解决时限</span>
                    <Input
                      type="number"
                      min={1}
                      max={9999}
                      status={resolveHoursInvalid ? 'error' : undefined}
                      value={form.resolveHours}
                      onChange={(event) => onUpdateForm({ resolveHours: event.target.value })}
                      placeholder="可空"
                      suffix="工作小时"
                    />
                  </label>
                  <label>
                    <span>预警阈值</span>
                    <Input
                      type="number"
                      min={1}
                      max={9999}
                      status={warningInvalid ? 'error' : undefined}
                      value={form.warningRemainHours}
                      onChange={(event) => onUpdateForm({ warningRemainHours: Number(event.target.value || 1) })}
                      suffix="剩余工作小时"
                    />
                  </label>
                  <label>
                    <span>升级阈值</span>
                    <Input
                      type="number"
                      min={1}
                      max={9999}
                      value={form.escalateAfterBreachHours}
                      onChange={(event) => onUpdateForm({ escalateAfterBreachHours: event.target.value })}
                      placeholder="可空"
                      suffix="超时后工作小时"
                    />
                  </label>
                  <label className="sla-form-wide">
                    <span>绑定日历</span>
                    <Select
                      showSearch
                      loading={workCalendarsLoading}
                      value={form.calendarId || undefined}
                      placeholder="请选择工作日历"
                      optionFilterProp="label"
                      options={calendarOptions}
                      onChange={(value) => onUpdateForm({ calendarId: value })}
                    />
                  </label>
                  <Alert
                    className="sla-form-wide"
                    type={resolveHoursInvalid || warningInvalid ? 'error' : 'info'}
                    showIcon
                    message={resolveHoursInvalid || warningInvalid ? '策略校验未通过' : '策略校验'}
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
                </div>

                <footer className="sla-panel-actions">
                  <Button onClick={onOpenCreatePolicy}>新建草稿</Button>
                  <Button
                    type="primary"
                    loading={saving}
                    disabled={!form.policyName.trim() || !form.calendarId || resolveHoursInvalid || warningInvalid}
                    onClick={() => void onSavePolicy()}
                  >
                    保存策略
                  </Button>
                  <Button danger disabled={!selectedPolicy || selectedPolicy.defaultPolicy} icon={<DeleteOutlined />} onClick={() => selectedPolicy && onRequestDelete(selectedPolicy)}>
                    删除
                  </Button>
                </footer>
              </section>

              <aside className="sla-panel sla-preview-panel">
                <header className="sla-panel-head">
                  <div>
                    <h3>SLA 预览</h3>
                    <span>按当前表单配置估算截止时间</span>
                  </div>
                  <Tag color="blue">预览</Tag>
                </header>

                <div className="sla-preview-list">
                  <div>
                    <span>绑定日历</span>
                    <strong>{selectedWorkCalendar?.calendarName || '未选择'}</strong>
                  </div>
                  <div>
                    <span>工作时段</span>
                    <strong>{selectedWorkCalendar ? `${workdayLabel(selectedWorkCalendar.workdays)} ${selectedWorkCalendar.workStartTime}-${selectedWorkCalendar.workEndTime}` : '-'}</strong>
                  </div>
                  <div>
                    <span>建单时间</span>
                    <strong>{previewBaseTime.format('YYYY-MM-DD HH:mm')}</strong>
                  </div>
                  <div>
                    <span>首次响应截止</span>
                    <strong>{preview.responseDeadline.format('YYYY-MM-DD HH:mm')}</strong>
                  </div>
                  <div>
                    <span>解决截止</span>
                    <strong>{preview.resolveDeadline ? preview.resolveDeadline.format('YYYY-MM-DD HH:mm') : '未配置'}</strong>
                  </div>
                </div>

                <Timeline
                  className="sla-preview-timeline"
                  items={[
                    { color: 'blue', content: <span>新建工单：写入策略和截止时间</span> },
                    { color: 'orange', content: <span>即将超时：{preview.warningAt.format('YYYY-MM-DD HH:mm')}</span> },
                    { color: 'red', content: <span>升级提醒：{preview.escalateAt ? preview.escalateAt.format('YYYY-MM-DD HH:mm') : '未配置'}</span> },
                  ]}
                />
              </aside>
            </main>
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
