import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Drawer, Dropdown, Empty, Input, Modal, Segmented, Select, Space, Switch, Table, Tag, Typography } from 'antd'
import { DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import {
  ArrowDown,
  ArrowUp,
  Beaker,
  Bell,
  CheckCircle2,
  CircleOff,
  MoreHorizontal,
  Pencil,
  Plus,
  RefreshCw,
  Settings2,
  ShieldCheck,
  SlidersHorizontal,
  Users,
  Workflow,
} from 'lucide-react'
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
  onDiscardRuleChanges: () => void
  onEnabledFilterChange: (value: string) => void
  onFetchAssignmentRules: () => void
  onKeywordChange: (value: string) => void
  onMatchTypeFilterChange: (value: string) => void
  onMoveRule: (rule: AssignmentRule, direction: 1 | -1) => void
  onOpenCreateRule: () => void
  onOpenCreateGroup: () => void
  onRequestDelete: (rule: AssignmentRule) => void
  onResetFilters: () => void
  onRunTest: () => void
  onSaveRule: () => Promise<boolean>
  onSaveGroup: () => Promise<boolean>
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

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function isDefaultRule(rule: AssignmentRule) {
  return rule.defaultRule || rule.matchType === 'DEFAULT'
}

function humanRuleCondition(rule: AssignmentRule) {
  if (isDefaultRule(rule)) return '未命中其他规则时执行默认分配'
  if (rule.matchType === 'SUBJECT_KEYWORD') return `主题包含“${rule.matchValue || '-'}”`
  if (rule.matchType === 'MAILBOX') return `来源邮箱为“${rule.matchValue || '-'}”`
  return `客户邮箱为“${rule.matchValue || '-'}”`
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
  onDiscardRuleChanges,
  onEnabledFilterChange,
  onFetchAssignmentRules,
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
  const [ruleDrawerOpen, setRuleDrawerOpen] = useState(false)
  const [testDrawerOpen, setTestDrawerOpen] = useState(false)
  const [groupModalOpen, setGroupModalOpen] = useState(false)
  const [sortingMode, setSortingMode] = useState(false)

  const records = useMemo(() => rulesData?.records ?? [], [rulesData?.records])
  const summary = rulesData?.summary
  const orderedRecords = useMemo(() => records
    .filter((record) => !isDefaultRule(record))
    .sort((a, b) => a.priorityOrder - b.priorityOrder || a.id - b.id), [records])
  const defaultRecords = useMemo(() => records.filter(isDefaultRule), [records])
  const displayRecords = useMemo(() => [...orderedRecords, ...defaultRecords], [defaultRecords, orderedRecords])
  const selectedGroup = groupsData?.records.find((group) => group.id === selectedGroupId) ?? null
  const selectedEnterprise = enterpriseOptions.find((enterprise) => String(enterprise.id) === selectedEnterpriseId) ?? null
  const selectedAssigneeName = assignmentAssignees.find((agent) => String(agent.id) === form.assigneeId)?.displayName || '未选择处理人'
  const defaultForm = form.defaultRule || form.matchType === 'DEFAULT'
  const emailInvalid = form.matchType === 'FROM_EMAIL' && Boolean(form.matchValue.trim()) && !EMAIL_PATTERN.test(form.matchValue.trim())
  const formValid = Boolean(selectedGroupId && form.ruleName.trim() && form.assigneeId && (defaultForm || form.matchValue.trim()) && !emailInvalid)
  const filtersActive = Boolean(keyword.trim() || assignmentEnabledFilter !== 'ALL' || matchTypeFilter !== 'ALL')
  const mailboxRuleOptions = useMemo(() => {
    if (form.matchType !== 'MAILBOX' || !form.matchValue || mailboxOptions.some((option) => option.value === form.matchValue)) return mailboxOptions
    return [{ value: form.matchValue, label: `当前配置：${form.matchValue}` }, ...mailboxOptions]
  }, [form.matchType, form.matchValue, mailboxOptions])

  useEffect(() => {
    if (!ruleDirty) return undefined
    const guard = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', guard)
    return () => window.removeEventListener('beforeunload', guard)
  }, [ruleDirty])

  function afterDirtyCheck(action: () => void) {
    if (!ruleDirty) {
      action()
      return
    }
    Modal.confirm({
      title: '放弃未保存的修改？',
      content: '当前规则还有未保存内容，继续操作后这些修改将丢失。',
      okText: '放弃修改',
      cancelText: '继续编辑',
      okButtonProps: { danger: true },
      onOk: () => {
        onDiscardRuleChanges()
        action()
      },
    })
  }

  function openRuleDraft() {
    if (!selectedGroupId) return
    afterDirtyCheck(() => {
      onOpenCreateRule()
      setRuleDrawerOpen(true)
    })
  }

  function openRule(record: AssignmentRule) {
    if (form.id === record.id) {
      setRuleDrawerOpen(true)
      return
    }
    afterDirtyCheck(() => {
      onSelectRule(record)
      setRuleDrawerOpen(true)
    })
  }

  function closeRuleDrawer() {
    if (!ruleDirty) {
      if (!form.id) onDiscardRuleChanges()
      setRuleDrawerOpen(false)
      return
    }
    afterDirtyCheck(() => setRuleDrawerOpen(false))
  }

  function changeEnterprise(value: string) {
    if (value === selectedEnterpriseId) return
    afterDirtyCheck(() => {
      onEnterpriseChange(value)
      setSortingMode(false)
      setRuleDrawerOpen(false)
    })
  }

  function selectGroup(group: AssignmentRuleGroup) {
    if (group.id === selectedGroupId) return
    afterDirtyCheck(() => {
      onSelectGroup(group)
      setSortingMode(false)
      setRuleDrawerOpen(false)
    })
  }

  function openCreateGroup() {
    onOpenCreateGroup()
    setGroupModalOpen(true)
  }

  function openEditGroup(group: AssignmentRuleGroup) {
    onGroupFormChange({
      id: group.id,
      enterpriseId: String(group.enterpriseId),
      groupName: group.groupName,
      enabled: group.enabled,
      remark: group.remark || '',
    })
    setGroupModalOpen(true)
  }

  function requestToggleGroup(group: AssignmentRuleGroup) {
    if (!group.enabled) {
      void onToggleGroup(group)
      return
    }
    Modal.confirm({
      title: `停用“${group.groupName}”？`,
      content: '停用后，该规则组内规则不会再参与后续自动分配，历史工单不受影响。',
      okText: '确认停用',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () => onToggleGroup(group),
    })
  }

  async function saveGroup() {
    if (await onSaveGroup()) setGroupModalOpen(false)
  }

  async function saveRule() {
    if (await onSaveRule()) setRuleDrawerOpen(false)
  }

  function updateMatchType(value: AssignmentRuleMatchType) {
    onUpdateForm({ matchType: value, matchValue: '' })
  }

  function renderMatchValueField() {
    if (form.matchType === 'DEFAULT') {
      return (
        <Alert
          showIcon
          type="info"
          title="无需填写匹配内容"
          description="前面的规则都未命中时，系统执行这条默认分配规则。每个规则组只能有一条默认规则。"
        />
      )
    }
    if (form.matchType === 'MAILBOX') {
      return (
        <Select
          showSearch
          value={form.matchValue || undefined}
          placeholder="选择接收邮件的邮箱"
          optionFilterProp="label"
          options={mailboxRuleOptions}
          onChange={(value) => onUpdateForm({ matchValue: value })}
        />
      )
    }
    if (form.matchType === 'FROM_EMAIL') {
      return (
        <Input
          type="email"
          value={form.matchValue}
          onChange={(event) => onUpdateForm({ matchValue: event.target.value })}
          placeholder="例如：vip@example.com"
          status={emailInvalid || !form.matchValue.trim() ? 'error' : undefined}
        />
      )
    }
    return (
      <Input
        value={form.matchValue}
        onChange={(event) => onUpdateForm({ matchValue: event.target.value })}
        placeholder="例如：VIP、退款、紧急"
        status={!form.matchValue.trim() ? 'error' : undefined}
      />
    )
  }

  return (
    <>
      <section className="app-content assignment-page assignment-page-v2" aria-label="分配规则">
        <header className="assignment-v2-header">
          <div className="assignment-v2-heading">
            <div className="assignment-v2-title-row">
              <h2>分配规则</h2>
              {selectedGroup && <Tag color={selectedGroup.enabled ? 'green' : 'default'}>{selectedGroup.enabled ? '规则组运行中' : '规则组已停用'}</Tag>}
            </div>
            <p>
              {selectedEnterprise?.enterpriseName || '请选择企业'}
              <span>/</span>
              {selectedGroup?.groupName || '请选择规则组'}
            </p>
          </div>
          <div className="assignment-v2-actions">
            <label className="assignment-v2-enterprise">
              <span>当前企业</span>
              <Select
                value={selectedEnterpriseId || undefined}
                placeholder="选择企业"
                onChange={changeEnterprise}
                options={enterpriseOptions.map((enterprise) => ({ value: String(enterprise.id), label: enterprise.enterpriseName }))}
              />
            </label>
            <Button aria-label="刷新分配规则" disabled={rulesLoading} icon={<RefreshCw size={15} />} onClick={onFetchAssignmentRules}>刷新</Button>
            <Button icon={<Beaker size={15} />} disabled={!selectedGroupId} onClick={() => setTestDrawerOpen(true)}>测试规则</Button>
            <Button
              type="primary"
              disabled={!canCreateAssignmentRules || !selectedGroupId || !selectedGroup?.enabled}
              icon={<Plus size={15} />}
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
            <section className="assignment-healthbar" aria-label="当前规则运行概况">
              <div><Workflow size={16} /><span>规则组</span><strong>{groupsData?.totalCount ?? 0}</strong></div>
              <div><CheckCircle2 size={16} /><span>启用规则</span><strong>{summary?.enabledCount ?? 0}</strong></div>
              <div><CircleOff size={16} /><span>停用规则</span><strong>{summary?.disabledCount ?? 0}</strong></div>
              <div className={matchResult?.matched ? 'success' : ''}>
                <Beaker size={16} /><span>最近测试</span><strong>{matchResult?.matched ? `命中 ${matchResult.ruleName}` : '尚未测试'}</strong>
              </div>
              <p><SlidersHorizontal size={15} />按列表顺序从上到下匹配，调整和保存只影响后续新工单。</p>
            </section>

            {rulesError && (
              <Alert showIcon type="error" className="assignment-v2-error" title={rulesError} action={<Button size="small" onClick={onFetchAssignmentRules}>重试</Button>} />
            )}

            <main className="assignment-v2-workspace">
              <aside className="assignment-v2-groups">
                <header>
                  <div><strong>规则组</strong><span>按业务场景组织规则</span></div>
                  <Button aria-label="新建规则组" size="small" type="text" icon={<Plus size={15} />} disabled={!selectedEnterpriseId} onClick={openCreateGroup} />
                </header>
                <div className="assignment-v2-group-list">
                  {groupsLoading ? (
                    <div className="assignment-v2-loading">加载规则组...</div>
                  ) : groupsData?.records.length ? groupsData.records.map((group) => (
                    <div className={`assignment-v2-group ${selectedGroupId === group.id ? 'active' : ''}`} key={group.id}>
                      <button type="button" onClick={() => selectGroup(group)}>
                        <span className="assignment-v2-group-icon"><Users size={15} /></span>
                        <span className="assignment-v2-group-copy">
                          <strong>{group.groupName}</strong>
                          <small>{group.remark || '暂无规则组说明'}</small>
                        </span>
                      </button>
                      <Dropdown
                        trigger={['click']}
                        menu={{
                          items: [
                            { key: 'edit', label: '编辑规则组', icon: <Pencil size={14} /> },
                            { key: 'toggle', label: group.enabled ? '停用规则组' : '启用规则组', danger: group.enabled },
                          ],
                          onClick: ({ key, domEvent }) => {
                            domEvent.stopPropagation()
                            if (key === 'edit') openEditGroup(group)
                            if (key === 'toggle') requestToggleGroup(group)
                          },
                        }}
                      >
                        <Button aria-label={`${group.groupName}更多操作`} size="small" type="text" icon={<MoreHorizontal size={16} />} onClick={(event) => event.stopPropagation()} />
                      </Dropdown>
                      <span className={`assignment-v2-group-status ${group.enabled ? 'enabled' : ''}`} title={group.enabled ? '启用' : '停用'} />
                    </div>
                  )) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无规则组">
                      <Button size="small" type="primary" onClick={openCreateGroup}>新建规则组</Button>
                    </Empty>
                  )}
                </div>
                {selectedGroup && (
                  <footer>
                    <div><span>当前规则组</span><strong>{selectedGroup.groupName}</strong></div>
                    <Button size="small" icon={<Settings2 size={14} />} onClick={() => openEditGroup(selectedGroup)}>设置</Button>
                  </footer>
                )}
              </aside>

              <section className="assignment-v2-ledger">
                <header className="assignment-v2-ledger-head">
                  <div>
                    <strong>规则清单</strong>
                    <span>{selectedGroup ? `共 ${summary?.totalCount ?? 0} 条规则，列表靠前的规则优先匹配` : '请先选择规则组'}</span>
                  </div>
                  <Space size={8}>
                    {sortingMode && <Tag color="purple">排序模式</Tag>}
                    <Button size="small" icon={<SlidersHorizontal size={14} />} disabled={orderedRecords.length < 2} onClick={() => setSortingMode((value) => !value)}>
                      {sortingMode ? '完成排序' : '调整顺序'}
                    </Button>
                  </Space>
                </header>

                <section className="assignment-v2-filters" aria-label="筛选规则">
                  <Input allowClear prefix={<SearchOutlined />} placeholder="搜索规则名称或匹配内容" value={keyword} onChange={(event) => onKeywordChange(event.target.value)} />
                  <Select value={assignmentEnabledFilter} onChange={onEnabledFilterChange} options={[
                    { value: 'ALL', label: '全部状态' },
                    { value: 'true', label: '仅看启用' },
                    { value: 'false', label: '仅看停用' },
                  ]} />
                  <Select value={matchTypeFilter} onChange={onMatchTypeFilterChange} options={[
                    { value: 'ALL', label: '全部匹配方式' },
                    { value: 'DEFAULT', label: '默认分配' },
                    { value: 'SUBJECT_KEYWORD', label: '主题关键词' },
                    { value: 'MAILBOX', label: '来源邮箱' },
                    { value: 'FROM_EMAIL', label: '客户邮箱' },
                  ]} />
                  {filtersActive && <Button size="small" type="link" onClick={onResetFilters}>清空筛选</Button>}
                </section>

                <div className="assignment-v2-table">
                  <Table<AssignmentRule>
                    rowKey="id"
                    size="middle"
                    tableLayout="fixed"
                    loading={rulesLoading}
                    dataSource={displayRecords}
                    pagination={false}
                    scroll={{ x: 760 }}
                    locale={{
                      emptyText: (
                        <Empty description={selectedGroupId ? '当前规则组还没有规则' : '请先在左侧选择规则组'} image={Empty.PRESENTED_IMAGE_SIMPLE}>
                          {selectedGroupId && <Button type="primary" onClick={openRuleDraft}>新建第一条规则</Button>}
                        </Empty>
                      ),
                    }}
                    onRow={(record) => ({ onClick: () => !sortingMode && openRule(record) })}
                    columns={[
                      {
                        title: '顺序',
                        width: 72,
                        render: (_value, record) => {
                          const normalIndex = orderedRecords.findIndex((item) => item.id === record.id)
                          const defaultIndex = defaultRecords.findIndex((item) => item.id === record.id)
                          return <span className="assignment-v2-order">{normalIndex >= 0 ? normalIndex + 1 : orderedRecords.length + defaultIndex + 1}</span>
                        },
                      },
                      {
                        title: '匹配条件',
                        width: 320,
                        render: (_value, record) => (
                          <div className="assignment-v2-rule-copy">
                            <strong>{record.ruleName}</strong>
                            <span>{humanRuleCondition(record)}</span>
                          </div>
                        ),
                      },
                      {
                        title: '分配结果',
                        width: 210,
                        render: (_value, record) => (
                          <div className="assignment-v2-result-copy">
                            <strong title={record.assigneeName || `用户 ${record.assigneeId}`}>{record.assigneeName || `用户 ${record.assigneeId}`}</strong>
                            <span><Bell size={13} />{record.notifyEnabled ? '通知处理人' : '不发送通知'}</span>
                          </div>
                        ),
                      },
                      {
                        title: '状态',
                        width: 92,
                        render: (_value, record) => (
                          <span className="assignment-v2-status" onClick={(event) => event.stopPropagation()}>
                            <Switch size="small" aria-label={`${record.enabled ? '停用' : '启用'}${record.ruleName}`} checked={record.enabled} loading={actionLoading && form.id === record.id} onChange={(checked) => void onToggleRule(record, checked)} />
                            {record.enabled ? '启用' : '停用'}
                          </span>
                        ),
                      },
                      {
                        title: '操作',
                        width: sortingMode ? 104 : 144,
                        fixed: 'right',
                        render: (_value, record) => {
                          const index = orderedRecords.findIndex((item) => item.id === record.id)
                          if (sortingMode) {
                            return isDefaultRule(record) ? <Typography.Text type="secondary">固定最后</Typography.Text> : (
                              <Space size={4} onClick={(event) => event.stopPropagation()}>
                                <Button aria-label={`上移${record.ruleName}`} size="small" type="text" icon={<ArrowUp size={15} />} disabled={index <= 0 || actionLoading} onClick={() => void onMoveRule(record, -1)} />
                                <Button aria-label={`下移${record.ruleName}`} size="small" type="text" icon={<ArrowDown size={15} />} disabled={index < 0 || index >= orderedRecords.length - 1 || actionLoading} onClick={() => void onMoveRule(record, 1)} />
                              </Space>
                            )
                          }
                          return (
                            <Space size={2} onClick={(event) => event.stopPropagation()}>
                              <Button size="small" type="link" icon={<Pencil size={14} />} onClick={() => openRule(record)}>编辑</Button>
                              <Button danger size="small" type="link" icon={<DeleteOutlined />} onClick={() => onRequestDelete(record)}>删除</Button>
                            </Space>
                          )
                        },
                      },
                    ]}
                  />
                </div>
              </section>
            </main>
          </>
        )}
      </section>

      <Drawer
        rootClassName="assignment-v2-drawer"
        title={selectedRule ? '编辑分配规则' : '新建分配规则'}
        extra={ruleDirty ? <Tag color="orange">未保存</Tag> : selectedRule ? <Tag color="green">已保存</Tag> : null}
        width={580}
        open={ruleDrawerOpen}
        onClose={closeRuleDrawer}
        footer={(
          <div className="assignment-v2-drawer-footer">
            <div>{selectedRule && <Button danger type="text" icon={<DeleteOutlined />} onClick={() => onRequestDelete(selectedRule)}>删除规则</Button>}</div>
            <Space>
              <Button onClick={closeRuleDrawer}>取消</Button>
              <Button type="primary" loading={saving} disabled={!formValid} onClick={() => void saveRule()}>保存规则</Button>
            </Space>
          </div>
        )}
      >
        <div className="assignment-v2-drawer-body">
          <>
              <section className="assignment-v2-form-section">
                <header><span>1</span><div><strong>规则信息</strong><small>为运营人员提供清晰、可识别的规则名称</small></div></header>
                <div className="assignment-v2-form-grid">
                  <label className="wide"><span>规则名称</span><Input value={form.ruleName} onChange={(event) => onUpdateForm({ ruleName: event.target.value })} placeholder="例如：VIP 客户优先分配" /></label>
                  <label><span>执行顺序</span><Input type="number" min={1} max={9999} value={form.priorityOrder} onChange={(event) => onUpdateForm({ priorityOrder: Number(event.target.value || 1) })} /><small>数字越小越先执行，也可以在列表中调整。</small></label>
                  <label className="assignment-v2-switch-field"><span>规则状态</span><div><Switch checked={form.enabled} onChange={(checked) => onUpdateForm({ enabled: checked })} /><strong>{form.enabled ? '启用' : '停用'}</strong></div></label>
                </div>
              </section>

              <section className="assignment-v2-form-section">
                <header><span>2</span><div><strong>匹配条件</strong><small>新邮件满足以下条件时执行本规则</small></div></header>
                <div className="assignment-v2-form-grid one-column">
                  <label><span>匹配方式</span><Segmented block value={form.matchType} onChange={(value) => updateMatchType(value as AssignmentRuleMatchType)} options={[
                    { value: 'DEFAULT', label: '默认分配' },
                    { value: 'SUBJECT_KEYWORD', label: '主题关键词' },
                    { value: 'MAILBOX', label: '来源邮箱' },
                    { value: 'FROM_EMAIL', label: '客户邮箱' },
                  ]} /></label>
                  {!defaultForm && <label>
                    <span>{form.matchType === 'SUBJECT_KEYWORD' ? '关键词' : form.matchType === 'MAILBOX' ? '来源邮箱' : '客户邮箱'}</span>
                    {renderMatchValueField()}
                    {!form.matchValue.trim() && <small className="assignment-field-error">请填写匹配内容</small>}
                    {emailInvalid && <small className="assignment-field-error">请输入有效的邮箱地址</small>}
                  </label>}
                  {defaultForm && renderMatchValueField()}
                </div>
              </section>

              <section className="assignment-v2-form-section">
                <header><span>3</span><div><strong>分配结果</strong><small>设置命中规则后的处理人与通知方式</small></div></header>
                <div className="assignment-v2-form-grid one-column">
                  <label><span>分配处理人</span><Select showSearch value={form.assigneeId || undefined} placeholder="选择负责处理工单的客服" optionFilterProp="label" options={assigneeOptions} onChange={(value) => onUpdateForm({ assigneeId: value })} /></label>
                  <label className="assignment-v2-notify-field">
                    <div><Bell size={17} /><span><strong>通知处理人</strong><small>工单分配成功后，按照配置的通知渠道提醒客服。</small></span></div>
                    <Switch checked={form.notifyEnabled} onChange={(checked) => onUpdateForm({ notifyEnabled: checked })} />
                  </label>
                </div>
              </section>

              <Alert className="assignment-v2-preview" type="info" showIcon title="规则预览" description={defaultForm
                ? `其他规则均未命中时，默认分配给${selectedAssigneeName}${form.notifyEnabled ? '，并通知处理人' : ''}。`
                : `${assignmentMatchTypeLabel(form.matchType)}“${form.matchValue || '未填写'}”时，分配给${selectedAssigneeName}${form.notifyEnabled ? '，并通知处理人' : ''}。`} />
            </>
        </div>
      </Drawer>

      <Drawer rootClassName="assignment-v2-drawer" title="测试分配规则" width={540} open={testDrawerOpen} onClose={() => setTestDrawerOpen(false)} extra={matchResult?.matched ? <Tag color="green">已命中</Tag> : <Tag>不会创建工单</Tag>}>
        <div className="assignment-v2-test">
          <Alert showIcon type="info" title="使用一封模拟邮件验证当前规则顺序，不会创建工单或修改数据。" />
          <label><span>来源邮箱</span><Select showSearch value={testForm.mailboxId || undefined} placeholder="选择接收邮件的邮箱" optionFilterProp="label" options={mailboxOptions} onChange={(value) => onTestFormChange({ mailboxId: value })} /></label>
          <label><span>客户邮箱</span><Input value={testForm.fromEmail} onChange={(event) => onTestFormChange({ fromEmail: event.target.value })} placeholder="buyer@example.com" /></label>
          <label><span>邮件主题</span><Input.TextArea rows={3} value={testForm.subject} onChange={(event) => onTestFormChange({ subject: event.target.value })} placeholder="例如：VIP 客户反馈无法登录后台" /></label>
          <Button block type="primary" loading={testing} disabled={!testForm.mailboxId} icon={<Beaker size={15} />} onClick={() => void onRunTest()}>运行测试</Button>
          {matchResult ? (
            <section className={`assignment-v2-test-result ${matchResult.matched ? 'matched' : ''}`}>
              <header><CheckCircle2 size={20} /><div><strong>{matchResult.matched ? `命中：${matchResult.ruleName}` : '未命中任何规则'}</strong><span>{matchResult.matched ? '系统将按照以下结果自动分配' : '系统将按邮箱配置的兜底策略处理'}</span></div></header>
              {matchResult.matched && <div>
                <p><span>命中原因</span><strong>{assignmentMatchTypeLabel(matchResult.matchType)} = {matchResult.matchValue || '-'}</strong></p>
                <p><span>最终处理人</span><strong>{matchResult.assigneeName || matchResult.assigneeId}</strong></p>
                <p><span>通知方式</span><strong>{matchResult.notifyEnabled ? '通知处理人' : '不发送通知'}</strong></p>
              </div>}
            </section>
          ) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="填写模拟邮件后运行测试" />}
        </div>
      </Drawer>

      <Modal title={groupForm.id ? '编辑规则组' : '新建规则组'} open={groupModalOpen} onCancel={() => setGroupModalOpen(false)} footer={(
        <Space><Button onClick={() => setGroupModalOpen(false)}>取消</Button><Button type="primary" loading={groupSaving} disabled={!groupForm.groupName.trim()} onClick={() => void saveGroup()}>保存规则组</Button></Space>
      )}>
        <div className="assignment-v2-group-form">
          <Alert type="info" showIcon title="规则组用于组织同一企业下不同业务场景的分配策略。" />
          <label><span>规则组名称</span><Input value={groupForm.groupName} onChange={(event) => onGroupFormChange({ groupName: event.target.value })} placeholder="例如：售后客服组" /></label>
          <label><span>规则组说明</span><Input.TextArea rows={3} value={groupForm.remark} onChange={(event) => onGroupFormChange({ remark: event.target.value })} placeholder="说明该规则组适用的业务范围" /></label>
          <label className="assignment-v2-group-enable"><span><strong>启用规则组</strong><small>停用后组内规则不再参与后续自动分配。</small></span><Switch checked={groupForm.enabled} onChange={(checked) => onGroupFormChange({ enabled: checked })} /></label>
        </div>
      </Modal>

      {confirmAction && (
        <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="assignment-confirm-title">
          <div className="confirm-modal">
            <h3 id="assignment-confirm-title">删除分配规则</h3>
            <p>删除后该规则不再参与新工单自动匹配，历史工单已分配的处理人保持不变。</p>
            <div className="confirm-target"><strong>{confirmAction.rule.ruleName}</strong><span>{assignmentRuleText(confirmAction.rule)}</span></div>
            <div className="user-modal__foot">
              <button disabled={actionLoading} onClick={onCancelConfirm} type="button">取消</button>
              <button className="primary-action" disabled={actionLoading} onClick={onSubmitConfirm} type="button">{actionLoading ? '删除中...' : '确认删除'}</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
