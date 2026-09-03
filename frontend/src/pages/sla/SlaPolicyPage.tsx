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
import type { EnterpriseOption } from '../../types/enterprise'

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
  enabledFilter: string
  enterpriseFilter: string
  enterpriseOptions: EnterpriseOption[]
  form: SlaPolicyFormState
  keyword: string
  onCancelConfirm: () => void
  onEnabledFilterChange: (value: string) => void
  onEnterpriseFilterChange: (value: string) => void
  onFetchSlaPolicies: () => void
  onKeywordChange: (value: string) => void
  onOpenCreatePolicy: () => void
  onRequestDelete: (policy: SlaPolicy) => void
  onResetFilters: () => void
  onSavePolicy: () => void
  onSelectPolicy: (policy: SlaPolicy) => void
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
  escalationInvalid: boolean
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
  enabledFilter,
  enterpriseFilter,
  enterpriseOptions,
  form,
  keyword,
  onCancelConfirm,
  onEnabledFilterChange,
  onEnterpriseFilterChange,
  onFetchSlaPolicies,
  onKeywordChange,
  onOpenCreatePolicy,
  onRequestDelete,
  onResetFilters,
  onSavePolicy,
  onSelectPolicy,
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
  escalationInvalid,
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
      label: '建单依据',
      value: '邮箱绑定',
      detail: '不再使用全局默认策略',
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
              title={policiesError || 'SLA 只通过邮箱显式绑定用于新工单；历史工单的策略快照不回写。'}
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
                    value={enterpriseFilter}
                    onChange={onEnterpriseFilterChange}
                    options={enterpriseOptions.map((enterprise) => ({ value: String(enterprise.id), label: enterprise.enterpriseName }))}
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
                            <Switch
                              size="small"
                              checked={record.enabled}
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
                    <span>所属企业</span>
                    <Select value={form.enterpriseId || undefined} options={enterpriseOptions.map((enterprise) => ({ value: String(enterprise.id), label: enterprise.enterpriseName }))} onChange={(value) => onUpdateForm({ enterpriseId: value, calendarId: '' })} />
                  </label>
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
                  <section className="sla-form-wide sla-notification-controls" aria-label="SLA通知节点">
                    <header>
                      <div>
                        <strong>通知节点</strong>
                        <span>邮件与飞书群通知共用以下开关；关闭后该节点仍留痕，但不发送消息。</span>
                      </div>
                      <Tag color="blue">默认 4 项</Tag>
                    </header>
                    <div className="sla-notification-grid">
                      <NotificationSwitch label="首次响应预警" checked={form.responseWarningNotifyEnabled} onChange={(checked) => onUpdateForm({ responseWarningNotifyEnabled: checked })} />
                      <NotificationSwitch label="首次响应超时" checked={form.responseBreachNotifyEnabled} onChange={(checked) => onUpdateForm({ responseBreachNotifyEnabled: checked })} />
                      <NotificationSwitch label="解决预警" checked={form.resolveWarningNotifyEnabled} onChange={(checked) => onUpdateForm({ resolveWarningNotifyEnabled: checked })} />
                      <NotificationSwitch label="解决超时" checked={form.resolveBreachNotifyEnabled} onChange={(checked) => onUpdateForm({ resolveBreachNotifyEnabled: checked })} />
                    </div>
                    <details className="sla-notification-advanced">
                      <summary>高级通知（默认关闭）</summary>
                      <div className="sla-notification-grid">
                        <NotificationSwitch label="首次响应超时升级" checked={form.responseEscalationNotifyEnabled} disabled={!form.escalateAfterBreachHours.trim()} onChange={(checked) => onUpdateForm({ responseEscalationNotifyEnabled: checked })} />
                        <NotificationSwitch label="解决超时升级" checked={form.resolveEscalationNotifyEnabled} disabled={!form.escalateAfterBreachHours.trim()} onChange={(checked) => onUpdateForm({ resolveEscalationNotifyEnabled: checked })} />
                      </div>
                    </details>
                  </section>
                  <Alert
                    className="sla-form-wide"
                    type={resolveHoursInvalid || warningInvalid || escalationInvalid ? 'error' : 'info'}
                    showIcon
                    title={resolveHoursInvalid || warningInvalid || escalationInvalid ? '策略校验未通过' : '策略校验'}
                    description={
                      resolveHoursInvalid
                        ? '解决时限不能小于首次响应时限。'
                        : warningInvalid
                          ? '预警阈值必须小于首次响应时限。'
                          : escalationInvalid
                            ? '启用超时升级通知前，请先配置升级阈值。'
                            : '时间配置仅影响后续新建工单；通知开关会作用于当前尚未发送的节点与队列任务。'
                    }
                  />
                </div>

                <footer className="sla-panel-actions">
                  <Button onClick={onOpenCreatePolicy}>新建草稿</Button>
                  <Button
                    type="primary"
                    loading={saving}
                    disabled={!form.policyName.trim() || !form.calendarId || resolveHoursInvalid || warningInvalid || escalationInvalid}
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
                    { color: form.responseWarningNotifyEnabled ? 'orange' : 'gray', content: <span>首次响应预警：{form.responseWarningNotifyEnabled ? preview.responseWarningAt.format('YYYY-MM-DD HH:mm') : '已关闭'}</span> },
                    { color: form.responseBreachNotifyEnabled ? 'red' : 'gray', content: <span>首次响应超时：{form.responseBreachNotifyEnabled ? preview.responseDeadline.format('YYYY-MM-DD HH:mm') : '已关闭'}</span> },
                    { color: form.resolveWarningNotifyEnabled ? 'orange' : 'gray', content: <span>解决预警：{form.resolveWarningNotifyEnabled && preview.resolveWarningAt ? preview.resolveWarningAt.format('YYYY-MM-DD HH:mm') : form.resolveWarningNotifyEnabled ? '未配置解决时限' : '已关闭'}</span> },
                    { color: form.resolveBreachNotifyEnabled ? 'red' : 'gray', content: <span>解决超时：{form.resolveBreachNotifyEnabled && preview.resolveDeadline ? preview.resolveDeadline.format('YYYY-MM-DD HH:mm') : form.resolveBreachNotifyEnabled ? '未配置解决时限' : '已关闭'}</span> },
                    { color: 'gray', content: <span>升级通知：{form.responseEscalationNotifyEnabled || form.resolveEscalationNotifyEnabled ? '按高级配置发送' : '默认关闭'}</span> },
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

function NotificationSwitch({ label, checked, disabled = false, onChange }: {
  label: string
  checked: boolean
  disabled?: boolean
  onChange: (checked: boolean) => void
}) {
  return (
    <label className={`sla-notification-switch${disabled ? ' is-disabled' : ''}`}>
      <span>{label}</span>
      <Switch size="small" checked={checked} disabled={disabled} onChange={onChange} />
    </label>
  )
}
