import { Alert, Button, Empty, Input, Segmented, Select, Space, Switch, Table, Tag, Typography } from 'antd'
import { DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import { CheckCircle2, FlaskConical, Layers3, Plus, RefreshCw, ShieldCheck, UserCheck } from 'lucide-react'
import { assignmentMatchTypeLabel, assignmentRuleText } from '../../constants/assignment-rules'
import type {
  AssignmentRule,
  AssignmentRuleConfirmAction,
  AssignmentRuleFormState,
  AssignmentRuleListResponse,
  AssignmentRuleMatchResponse,
  AssignmentRuleMatchType,
  AssignmentRuleTestForm,
} from '../../types/assignment-rule'
import type { ManagedUser } from '../../types/user'

type SelectOption = {
  value: string
  label: string
}

type AssignmentRulePageProps = {
  actionLoading: boolean
  assigneeOptions: SelectOption[]
  assignmentAssignees: ManagedUser[]
  assignmentEnabledFilter: string
  canCreateAssignmentRules: boolean
  canReadAssignmentRules: boolean
  confirmAction: AssignmentRuleConfirmAction
  form: AssignmentRuleFormState
  keyword: string
  matchResult: AssignmentRuleMatchResponse | null
  matchTypeFilter: string
  mailboxOptions: SelectOption[]
  onCancelConfirm: () => void
  onEnabledFilterChange: (value: string) => void
  onFetchAssignmentRules: () => void
  onKeywordChange: (value: string) => void
  onMatchTypeFilterChange: (value: string) => void
  onMoveRule: (rule: AssignmentRule, direction: 1 | -1) => void
  onOpenCreateRule: () => void
  onRequestDelete: (rule: AssignmentRule) => void
  onResetFilters: () => void
  onRunTest: () => void
  onSaveRule: () => void
  onSelectRule: (rule: AssignmentRule) => void
  onSubmitConfirm: () => void
  onTestFormChange: (patch: Partial<AssignmentRuleTestForm>) => void
  onToggleRule: (rule: AssignmentRule, enabled: boolean) => void
  onUpdateForm: (patch: Partial<AssignmentRuleFormState>) => void
  ruleDirty: boolean
  rulesData: AssignmentRuleListResponse | null
  rulesError: string
  rulesLoading: boolean
  saving: boolean
  selectedRule: AssignmentRule | null
  testForm: AssignmentRuleTestForm
  testing: boolean
}

export function AssignmentRulePage({
  actionLoading,
  assigneeOptions,
  assignmentAssignees,
  assignmentEnabledFilter,
  canCreateAssignmentRules,
  canReadAssignmentRules,
  confirmAction,
  form,
  keyword,
  matchResult,
  matchTypeFilter,
  mailboxOptions,
  onCancelConfirm,
  onEnabledFilterChange,
  onFetchAssignmentRules,
  onKeywordChange,
  onMatchTypeFilterChange,
  onMoveRule,
  onOpenCreateRule,
  onRequestDelete,
  onResetFilters,
  onRunTest,
  onSaveRule,
  onSelectRule,
  onSubmitConfirm,
  onTestFormChange,
  onToggleRule,
  onUpdateForm,
  ruleDirty,
  rulesData,
  rulesError,
  rulesLoading,
  saving,
  selectedRule,
  testForm,
  testing,
}: AssignmentRulePageProps) {
  const records = rulesData?.records ?? []
  const summary = rulesData?.summary
  const sortedRecords = [...records].sort((a, b) => a.priorityOrder - b.priorityOrder || a.id - b.id)
  const selectedAssigneeName = assignmentAssignees.find((agent) => String(agent.id) === form.assigneeId)?.displayName || '未选择处理人'
  const summaryItems = [
    {
      icon: Layers3,
      tone: 'primary',
      label: '规则总数',
      value: summary?.totalCount ?? '--',
      detail: `启用 ${summary?.enabledCount ?? '--'} / 停用 ${summary?.disabledCount ?? '--'}`,
    },
    {
      icon: CheckCircle2,
      tone: 'success',
      label: '默认兜底',
      value: summary?.defaultCount ?? '--',
      detail: '仅允许一个默认规则',
    },
    {
      icon: FlaskConical,
      tone: matchResult?.matched ? 'success' : 'warning',
      label: '测试结果',
      value: matchResult?.matched ? '已命中' : '--',
      detail: matchResult?.ruleName || '输入邮件信息后测试',
    },
    {
      icon: UserCheck,
      tone: ruleDirty ? 'warning' : 'primary',
      label: '当前处理人',
      value: selectedAssigneeName,
      detail: ruleDirty ? '有未保存修改' : '保存后影响后续新工单',
    },
  ]

  return (
    <>
      <section className="app-content assignment-page" aria-label="分配规则">
        <header className="assignment-topbar">
          <div className="assignment-title-block">
            <h2>分配规则</h2>
            <span>按优先级匹配新工单，命中后自动分配给指定处理人</span>
          </div>
          <div className="assignment-top-actions">
            <Button disabled={rulesLoading} icon={<RefreshCw size={16} />} onClick={onFetchAssignmentRules}>
              刷新
            </Button>
            <Button
              type="primary"
              disabled={!canCreateAssignmentRules}
              icon={<Plus size={16} />}
              onClick={onOpenCreateRule}
            >
              新建规则
            </Button>
          </div>
        </header>

        {!canReadAssignmentRules ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无分配规则管理权限</strong>
            <p>当前账号没有分配规则查看权限；新建、编辑、启停、排序或删除由独立权限控制。</p>
          </div>
        ) : (
          <>
            <section className="assignment-summary-strip" aria-label="分配规则统计">
              {summaryItems.map((item) => {
                const Icon = item.icon
                return (
                  <div key={item.label} className={`assignment-summary-item assignment-summary-item--${item.tone}`}>
                    <span className="assignment-summary-icon"><Icon size={17} /></span>
                    <span className="assignment-summary-copy">
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
              type={rulesError ? 'error' : 'info'}
              className="assignment-inline-alert"
              message={rulesError || '优先级数字越小越先匹配；排序、启停和规则保存只影响后续自动建单，历史工单不会回写。'}
              action={rulesError ? <Button size="small" onClick={onFetchAssignmentRules}>重试</Button> : undefined}
            />

            <main className="assignment-workspace">
              <section className="assignment-panel assignment-ledger">
                <header className="assignment-panel-head">
                  <div>
                    <h3>规则列表</h3>
                    <span>共 {summary?.totalCount ?? 0} 条，按当前优先级顺序执行</span>
                  </div>
                  <Tag color="blue">当前 {sortedRecords.length} 条</Tag>
                </header>

                <section className="assignment-inline-filters" aria-label="筛选条件">
                  <Input
                    allowClear
                    prefix={<SearchOutlined />}
                    placeholder="规则名 / 匹配值"
                    value={keyword}
                    onChange={(event) => onKeywordChange(event.target.value)}
                    onPressEnter={() => void onFetchAssignmentRules()}
                  />
                  <Select
                    value={assignmentEnabledFilter}
                    onChange={onEnabledFilterChange}
                    options={[
                      { value: 'ALL', label: '全部状态' },
                      { value: 'true', label: '启用' },
                      { value: 'false', label: '停用' },
                    ]}
                  />
                  <Select
                    value={matchTypeFilter}
                    onChange={onMatchTypeFilterChange}
                    options={[
                      { value: 'ALL', label: '全部类型' },
                      { value: 'DEFAULT', label: '默认兜底' },
                      { value: 'SUBJECT_KEYWORD', label: '主题关键词' },
                      { value: 'MAILBOX', label: '来源邮箱' },
                      { value: 'FROM_EMAIL', label: '客户邮箱' },
                    ]}
                  />
                  <Button onClick={onResetFilters}>清空筛选</Button>
                </section>

                <div className="assignment-table-shell">
                  <Table<AssignmentRule>
                    rowKey="id"
                    size="middle"
                    loading={rulesLoading}
                    dataSource={sortedRecords}
                    pagination={false}
                    locale={{
                      emptyText: (
                        <Empty description="还没有分配规则" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                          <Button type="primary" onClick={onOpenCreateRule}>新建规则</Button>
                        </Empty>
                      ),
                    }}
                    rowClassName={(record) => record.id === form.id ? 'ant-table-row-selected' : ''}
                    onRow={(record) => ({ onClick: () => onSelectRule(record) })}
                    columns={[
                      {
                        title: '优先级',
                        dataIndex: 'priorityOrder',
                        width: 82,
                        render: (value: number) => <Tag color="blue">{value}</Tag>,
                      },
                      {
                        title: '规则',
                        dataIndex: 'ruleName',
                        render: (_value: string, record: AssignmentRule) => (
                          <Space orientation="vertical" size={2}>
                            <Space wrap>
                              <Typography.Text strong>{record.ruleName}</Typography.Text>
                              {record.defaultRule && <Tag color="gold">默认</Tag>}
                              <Tag color={record.enabled ? 'green' : 'default'}>{record.enabled ? '启用' : '停用'}</Tag>
                            </Space>
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              {assignmentRuleText(record)} · 分配给 {record.assigneeName || record.assigneeId}
                            </Typography.Text>
                          </Space>
                        ),
                      },
                      {
                        title: '操作',
                        width: 188,
                        render: (_value: unknown, record: AssignmentRule, index: number) => (
                          <Space onClick={(event) => event.stopPropagation()}>
                            <Button size="small" disabled={index === 0 || actionLoading} onClick={() => void onMoveRule(record, -1)}>
                              上移
                            </Button>
                            <Button size="small" disabled={index >= sortedRecords.length - 1 || actionLoading} onClick={() => void onMoveRule(record, 1)}>
                              下移
                            </Button>
                            <Switch
                              size="small"
                              checked={record.enabled}
                              loading={actionLoading && form.id === record.id}
                              onChange={(checked) => void onToggleRule(record, checked)}
                            />
                          </Space>
                        ),
                      },
                    ]}
                  />
                </div>
              </section>

              <aside className="assignment-editor-stack">
                <section className="assignment-panel assignment-editor-panel">
                  <header className="assignment-panel-head">
                    <div>
                      <h3>规则编辑</h3>
                      <span>匹配条件、处理人和通知方式</span>
                    </div>
                    {ruleDirty
                      ? <Tag color="orange">有未保存修改</Tag>
                      : selectedRule
                        ? <Tag color="green">已保存</Tag>
                        : <Tag>新建草稿</Tag>}
                  </header>

                  <div className="assignment-form-grid">
                    <label>
                      <span>规则名称</span>
                      <Input value={form.ruleName} onChange={(event) => onUpdateForm({ ruleName: event.target.value })} placeholder="VIP 售后优先" />
                    </label>
                    <label>
                      <span>优先级</span>
                      <Input type="number" min={1} max={9999} value={form.priorityOrder} onChange={(event) => onUpdateForm({ priorityOrder: Number(event.target.value || 1) })} />
                    </label>
                    <label className="assignment-form-wide">
                      <span>匹配类型</span>
                      <Segmented
                        block
                        value={form.matchType}
                        onChange={(value) => onUpdateForm({ matchType: value as AssignmentRuleMatchType })}
                        options={[
                          { value: 'DEFAULT', label: '默认' },
                          { value: 'SUBJECT_KEYWORD', label: '主题关键词' },
                          { value: 'MAILBOX', label: '来源邮箱' },
                          { value: 'FROM_EMAIL', label: '客户邮箱' },
                        ]}
                      />
                    </label>
                    <label>
                      <span>匹配值</span>
                      <Input
                        disabled={form.matchType === 'DEFAULT'}
                        value={form.matchValue}
                        onChange={(event) => onUpdateForm({ matchValue: event.target.value })}
                        placeholder={form.matchType === 'DEFAULT' ? '默认规则不需要匹配值' : 'VIP / support@example.com'}
                        status={form.matchType !== 'DEFAULT' && !form.matchValue.trim() ? 'error' : undefined}
                      />
                      {form.matchType !== 'DEFAULT' && !form.matchValue.trim() && <small className="assignment-field-error">匹配值不能为空</small>}
                    </label>
                    <label>
                      <span>分配处理人</span>
                      <Select
                        showSearch
                        value={form.assigneeId || undefined}
                        placeholder="选择处理人"
                        optionFilterProp="label"
                        options={assigneeOptions}
                        onChange={(value) => onUpdateForm({ assigneeId: value })}
                      />
                    </label>
                    <label>
                      <span>启用状态</span>
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
                      <span>分配通知</span>
                      <Select
                        value={String(form.notifyEnabled)}
                        onChange={(value) => onUpdateForm({ notifyEnabled: value === 'true' })}
                        options={[
                          { value: 'true', label: '通知处理人' },
                          { value: 'false', label: '不发送通知' },
                        ]}
                      />
                    </label>
                    <Alert
                      className="assignment-form-wide"
                      type="info"
                      showIcon
                      message="规则预览"
                      description={`IF ${assignmentMatchTypeLabel(form.matchType)} ${form.matchType === 'DEFAULT' ? '兜底命中' : `= ${form.matchValue || '未填写'}`} THEN 分配给 ${selectedAssigneeName}`}
                    />
                  </div>

                  <footer className="assignment-panel-actions">
                    <Button onClick={onOpenCreateRule}>新建草稿</Button>
                    <Button
                      type="primary"
                      loading={saving}
                      disabled={!form.ruleName.trim() || !form.assigneeId || (form.matchType !== 'DEFAULT' && !form.matchValue.trim())}
                      onClick={() => void onSaveRule()}
                    >
                      保存规则
                    </Button>
                    <Button danger disabled={!selectedRule} icon={<DeleteOutlined />} onClick={() => selectedRule && onRequestDelete(selectedRule)}>
                      删除
                    </Button>
                  </footer>
                </section>

                <section className="assignment-panel assignment-test-panel">
                  <header className="assignment-panel-head">
                    <div>
                      <h3>测试匹配</h3>
                      <span>用邮件信息预演规则命中结果</span>
                    </div>
                    {matchResult?.matched ? <Tag color="green">已命中</Tag> : <Tag>未测试</Tag>}
                  </header>
                  <div className="assignment-form-grid">
                    <label>
                      <span>来源邮箱</span>
                      <Select
                        showSearch
                        value={testForm.mailboxId || undefined}
                        placeholder="选择来源邮箱"
                        optionFilterProp="label"
                        options={mailboxOptions}
                        onChange={(value) => onTestFormChange({ mailboxId: value })}
                      />
                    </label>
                    <label>
                      <span>客户邮箱</span>
                      <Input value={testForm.fromEmail} onChange={(event) => onTestFormChange({ fromEmail: event.target.value })} placeholder="buyer@acme.com" />
                    </label>
                    <label className="assignment-form-wide">
                      <span>邮件主题</span>
                      <Input value={testForm.subject} onChange={(event) => onTestFormChange({ subject: event.target.value })} placeholder="VIP 客户反馈：无法登录后台" />
                    </label>
                  </div>
                  <Button block type="primary" loading={testing} disabled={!testForm.mailboxId} onClick={() => void onRunTest()}>
                    运行测试匹配
                  </Button>
                  {matchResult ? (
                    <Alert
                      showIcon
                      type={matchResult.matched ? 'success' : 'warning'}
                      className="assignment-match-result"
                      message={matchResult.matched ? `命中 ${matchResult.ruleName}` : '未命中分配规则'}
                      description={matchResult.matched
                        ? `${assignmentMatchTypeLabel(matchResult.matchType)} = ${matchResult.matchValue || '-'}，分配给 ${matchResult.assigneeName || matchResult.assigneeId}，${matchResult.notifyEnabled ? '通知处理人' : '不发送通知'}。`
                        : '当前输入未命中任何启用规则，自动建单会继续走默认规则或邮箱默认处理人。'}
                    />
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="输入来源邮箱、客户邮箱和主题后测试命中结果" className="assignment-empty-state" />
                  )}
                </section>
              </aside>
            </main>
          </>
        )}
      </section>

      {confirmAction && (
        <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="assignment-confirm-title">
          <div className="confirm-modal">
            <h3 id="assignment-confirm-title">删除分配规则</h3>
            <p>删除后该规则不再参与新工单自动匹配，历史工单已分配的处理人保持不变。</p>
            <div className="confirm-target">
              <strong>{confirmAction.rule.ruleName}</strong>
              <span>{assignmentRuleText(confirmAction.rule)}</span>
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
