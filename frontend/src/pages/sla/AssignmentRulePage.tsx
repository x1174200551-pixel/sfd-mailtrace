import { useState } from 'react'
import { Alert, Button, Empty, Input, Segmented, Select, Space, Switch, Table, Tag, Typography } from 'antd'
import { DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import { CheckCircle2, FlaskConical, Layers3, Plus, RefreshCw, ShieldCheck, UserCheck } from 'lucide-react'
import { assignmentMatchTypeLabel, assignmentRuleText } from '../../constants/assignment-rules'
import type {
  AssignmentRule,
  AssignmentRuleConfirmAction,
  AssignmentRuleFormState,
  AssignmentRuleGroup,
  AssignmentRuleGroupFormState,
  AssignmentRuleGroupListResponse,
  AssignmentRuleListResponse,
  AssignmentRuleMatchResponse,
  AssignmentRuleMatchType,
  AssignmentRuleTestForm,
} from '../../types/assignment-rule'
import type { ManagedUser } from '../../types/user'
import type { EnterpriseOption } from '../../types/enterprise'

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
  enterpriseOptions: EnterpriseOption[]
  groupForm: AssignmentRuleGroupFormState
  groupSaving: boolean
  groupsData: AssignmentRuleGroupListResponse | null
  groupsLoading: boolean
  confirmAction: AssignmentRuleConfirmAction
  form: AssignmentRuleFormState
  keyword: string
  matchResult: AssignmentRuleMatchResponse | null
  matchTypeFilter: string
  mailboxOptions: SelectOption[]
  onCancelConfirm: () => void
  onEnabledFilterChange: (value: string) => void
  onFetchAssignmentRules: () => void
  onFetchAssignmentGroups: () => void
  onKeywordChange: (value: string) => void
  onMatchTypeFilterChange: (value: string) => void
  onMoveRule: (rule: AssignmentRule, direction: 1 | -1) => void
  onOpenCreateRule: () => void
  onOpenCreateGroup: () => void
  onRequestDelete: (rule: AssignmentRule) => void
  onResetFilters: () => void
  onRunTest: () => void
  onSaveRule: () => void
  onSaveGroup: () => void
  onSelectGroup: (group: AssignmentRuleGroup) => void
  onSelectRule: (rule: AssignmentRule) => void
  onSubmitConfirm: () => void
  onTestFormChange: (patch: Partial<AssignmentRuleTestForm>) => void
  onToggleRule: (rule: AssignmentRule, enabled: boolean) => void
  onToggleGroup: (group: AssignmentRuleGroup) => void
  onGroupFormChange: (patch: Partial<AssignmentRuleGroupFormState>) => void
  onEnterpriseChange: (value: string) => void
  onUpdateForm: (patch: Partial<AssignmentRuleFormState>) => void
  ruleDirty: boolean
  rulesData: AssignmentRuleListResponse | null
  rulesError: string
  rulesLoading: boolean
  saving: boolean
  selectedEnterpriseId: string
  selectedGroupId: number | null
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
  enterpriseOptions,
  groupForm,
  groupSaving,
  groupsData,
  groupsLoading,
  confirmAction,
  form,
  keyword,
  matchResult,
  matchTypeFilter,
  mailboxOptions,
  onCancelConfirm,
  onEnabledFilterChange,
  onFetchAssignmentRules,
  onFetchAssignmentGroups,
  onKeywordChange,
  onMatchTypeFilterChange,
  onMoveRule,
  onOpenCreateRule,
  onOpenCreateGroup,
  onRequestDelete,
  onResetFilters,
  onRunTest,
  onSaveRule,
  onSaveGroup,
  onSelectGroup,
  onSelectRule,
  onSubmitConfirm,
  onTestFormChange,
  onToggleRule,
  onToggleGroup,
  onGroupFormChange,
  onEnterpriseChange,
  onUpdateForm,
  ruleDirty,
  rulesData,
  rulesError,
  rulesLoading,
  saving,
  selectedEnterpriseId,
  selectedGroupId,
  selectedRule,
  testForm,
  testing,
}: AssignmentRulePageProps) {
  const records = rulesData?.records ?? []
  const summary = rulesData?.summary
  const sortedRecords = [...records].sort((a, b) => a.priorityOrder - b.priorityOrder || a.id - b.id)
  const selectedAssigneeName = assignmentAssignees.find((agent) => String(agent.id) === form.assigneeId)?.displayName || '未选择处理人'
  const [activeInspector, setActiveInspector] = useState<'editor' | 'test'>('editor')
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
      label: '启用规则',
      value: summary?.enabledCount ?? '--',
      detail: `停用 ${summary?.disabledCount ?? '--'} 条`,
    },
    {
      icon: UserCheck,
      tone: 'primary',
      label: '规则组',
      value: groupsData?.totalCount ?? '--',
      detail: '按企业隔离分配策略',
    },
    {
      icon: FlaskConical,
      tone: matchResult?.matched ? 'success' : 'warning',
      label: '最近测试',
      value: matchResult?.matched ? '已命中' : '--',
      detail: matchResult?.ruleName || '尚未运行匹配测试',
    },
  ]

  function openRuleDraft() {
    setActiveInspector('editor')
    onOpenCreateRule()
  }

  function selectRule(record: AssignmentRule) {
    setActiveInspector('editor')
    onSelectRule(record)
  }

  return (
    <>
      <section className="app-content assignment-page" aria-label="分配规则">
        <header className="assignment-topbar">
          <div className="assignment-title-block">
            <h2>分配规则</h2>
            <span>按优先级匹配新工单，命中后自动分配给指定处理人</span>
          </div>
          <div className="assignment-top-actions">
            <Select
              className="assignment-enterprise-select"
              value={selectedEnterpriseId || undefined}
              placeholder="选择企业"
              onChange={onEnterpriseChange}
              options={enterpriseOptions.map((enterprise) => ({ value: String(enterprise.id), label: enterprise.enterpriseName }))}
            />
            <Button disabled={rulesLoading} icon={<RefreshCw size={16} />} onClick={onFetchAssignmentRules}>
              刷新
            </Button>
            <Button
              type="primary"
              disabled={!canCreateAssignmentRules}
              icon={<Plus size={16} />}
              onClick={openRuleDraft}
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
              title={rulesError || '优先级数字越小越先匹配；排序、启停和规则保存只影响后续自动建单，历史工单不会回写。'}
              action={rulesError ? <Button size="small" onClick={onFetchAssignmentRules}>重试</Button> : undefined}
            />

            <main className="assignment-workspace">
              <section className="assignment-panel assignment-group-panel">
                <header className="assignment-panel-head">
                  <div><h3>规则组</h3><span>{groupsData?.totalCount ?? 0} 个企业规则组</span></div>
                  <Button size="small" icon={<Plus size={14} />} onClick={onOpenCreateGroup}>新建</Button>
                </header>
                <div className="assignment-group-list">
                  {groupsLoading ? <span>加载中...</span> : groupsData?.records.length ? groupsData.records.map((group) => (
                    <button className={selectedGroupId === group.id ? 'active' : ''} key={group.id} onClick={() => onSelectGroup(group)} type="button">
                      <span><strong>{group.groupName}</strong><small>{group.remark || '暂无备注'}</small></span>
                      <Tag color={group.enabled ? 'green' : 'default'}>{group.enabled ? '启用' : '停用'}</Tag>
                    </button>
                  )) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无规则组" />}
                </div>
                <div className="assignment-group-form">
                  <div className="assignment-group-form-head">
                    <strong>{groupForm.id ? '当前组设置' : '新建规则组'}</strong>
                    <small>先选择规则组，再维护组内规则</small>
                  </div>
                  <label><span>规则组名称</span><Input value={groupForm.groupName} onChange={(event) => onGroupFormChange({ groupName: event.target.value })} placeholder="例如：客服一组" /></label>
                  <label><span>备注</span><Input.TextArea rows={3} value={groupForm.remark} onChange={(event) => onGroupFormChange({ remark: event.target.value })} /></label>
                  <Space wrap>
                    <Button type="primary" loading={groupSaving} disabled={!groupForm.groupName.trim()} onClick={onSaveGroup}>保存</Button>
                    {groupForm.id && <Button disabled={groupSaving} onClick={() => {
                      const group = groupsData?.records.find((item) => item.id === groupForm.id)
                      if (group) onToggleGroup(group)
                    }}>{groupForm.enabled ? '停用' : '启用'}</Button>}
                    <Button size="small" onClick={onFetchAssignmentGroups}>刷新</Button>
                  </Space>
                </div>
              </section>

              <section className="assignment-stage">
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
                    tableLayout="fixed"
                    loading={rulesLoading}
                    dataSource={sortedRecords}
                    pagination={false}
                    locale={{
                      emptyText: (
                        <Empty description="还没有分配规则" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                          <Button type="primary" onClick={openRuleDraft}>新建规则</Button>
                        </Empty>
                      ),
                    }}
                    rowClassName={(record) => record.id === form.id ? 'ant-table-row-selected' : ''}
                    onRow={(record) => ({ onClick: () => selectRule(record) })}
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
                          <div className="assignment-rule-cell">
                            <Space wrap size={6}>
                              <Typography.Text strong>{record.ruleName}</Typography.Text>
                              <Tag color={record.enabled ? 'green' : 'default'}>{record.enabled ? '启用' : '停用'}</Tag>
                            </Space>
                            <Typography.Text className="assignment-rule-description" type="secondary">
                              {assignmentRuleText(record)} · 分配给 {record.assigneeName || record.assigneeId}
                            </Typography.Text>
                          </div>
                        ),
                      },
                      {
                        title: '操作',
                        width: 176,
                        render: (_value: unknown, record: AssignmentRule, index: number) => (
                          <Space className="assignment-row-actions" size={6} onClick={(event) => event.stopPropagation()}>
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
                  <nav className="assignment-inspector-tabs" aria-label="规则操作" role="tablist">
                    <button
                      aria-selected={activeInspector === 'editor'}
                      className={activeInspector === 'editor' ? 'active' : ''}
                      onClick={() => setActiveInspector('editor')}
                      role="tab"
                      type="button"
                    >
                      规则配置
                    </button>
                    <button
                      aria-selected={activeInspector === 'test'}
                      className={activeInspector === 'test' ? 'active' : ''}
                      onClick={() => setActiveInspector('test')}
                      role="tab"
                      type="button"
                    >
                      测试验证
                    </button>
                  </nav>

                  {activeInspector === 'editor' ? (
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

                      <div className="assignment-form-body">
                        <section className="assignment-form-section">
                          <div className="assignment-section-title"><strong>基础信息</strong><span>用于识别规则和确定执行顺序</span></div>
                          <div className="assignment-form-grid">
                            <label>
                              <span>规则名称</span>
                              <Input value={form.ruleName} onChange={(event) => onUpdateForm({ ruleName: event.target.value })} placeholder="VIP 售后优先" />
                            </label>
                            <label>
                              <span>优先级</span>
                              <Input type="number" min={1} max={9999} value={form.priorityOrder} onChange={(event) => onUpdateForm({ priorityOrder: Number(event.target.value || 1) })} />
                            </label>
                          </div>
                        </section>

                        <section className="assignment-form-section">
                          <div className="assignment-section-title"><strong>匹配条件</strong><span>新邮件满足条件后进入本规则</span></div>
                          <div className="assignment-form-grid">
                            <label className="assignment-form-wide">
                              <span>匹配类型</span>
                              <Segmented
                                block
                                value={form.matchType}
                                onChange={(value) => onUpdateForm({ matchType: value as AssignmentRuleMatchType })}
                                options={[
                                  { value: 'SUBJECT_KEYWORD', label: '主题关键词' },
                                  { value: 'MAILBOX', label: '来源邮箱' },
                                  { value: 'FROM_EMAIL', label: '客户邮箱' },
                                ]}
                              />
                            </label>
                            <label className="assignment-form-wide">
                              <span>匹配值</span>
                              <Input
                                value={form.matchValue}
                                onChange={(event) => onUpdateForm({ matchValue: event.target.value })}
                                placeholder="VIP / support@example.com"
                                status={!form.matchValue.trim() ? 'error' : undefined}
                              />
                              {!form.matchValue.trim() && <small className="assignment-field-error">匹配值不能为空</small>}
                            </label>
                          </div>
                        </section>

                        <section className="assignment-form-section">
                          <div className="assignment-section-title"><strong>执行动作</strong><span>设置命中后的处理人和通知方式</span></div>
                          <div className="assignment-form-grid">
                            <label className="assignment-form-wide">
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
                              className="assignment-form-wide assignment-rule-preview"
                              type="info"
                              showIcon
                              title="规则预览"
                              description={`IF ${assignmentMatchTypeLabel(form.matchType)} = ${form.matchValue || '未填写'} THEN 分配给 ${selectedAssigneeName}`}
                            />
                          </div>
                        </section>
                      </div>

                  <footer className="assignment-panel-actions">
                    <Button onClick={openRuleDraft}>新建草稿</Button>
                    <Button
                      type="primary"
                      loading={saving}
                      disabled={!selectedGroupId || !form.ruleName.trim() || !form.assigneeId || !form.matchValue.trim()}
                      onClick={() => void onSaveRule()}
                    >
                      保存规则
                    </Button>
                    <Button danger disabled={!selectedRule} icon={<DeleteOutlined />} onClick={() => selectedRule && onRequestDelete(selectedRule)}>
                      删除
                    </Button>
                  </footer>
                    </section>

                  ) : (
                    <section className="assignment-panel assignment-test-panel">
                  <header className="assignment-panel-head">
                    <div>
                      <h3>测试匹配</h3>
                      <span>用邮件信息预演规则命中结果</span>
                    </div>
                    {matchResult?.matched ? <Tag color="green">已命中</Tag> : <Tag>未测试</Tag>}
                  </header>
                  <div className="assignment-test-copy">
                    <strong>用真实邮件条件验证当前规则顺序</strong>
                    <span>测试只返回命中结果，不会创建工单或修改历史数据。</span>
                  </div>
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
                      title={matchResult.matched ? `命中 ${matchResult.ruleName}` : '未命中分配规则'}
                      description={matchResult.matched
                        ? `${assignmentMatchTypeLabel(matchResult.matchType)} = ${matchResult.matchValue || '-'}，分配给 ${matchResult.assigneeName || matchResult.assigneeId}，${matchResult.notifyEnabled ? '通知处理人' : '不发送通知'}。`
                        : '当前输入未命中任何启用规则，将按邮箱未命中策略保持待分配，或显式使用默认处理人。'}
                    />
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="输入来源邮箱、客户邮箱和主题后测试命中结果" className="assignment-empty-state" />
                  )}
                    </section>
                  )}
                </aside>
              </section>
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
