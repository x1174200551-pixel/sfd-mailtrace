import { Button, Card, Col, Empty, Input, Row, Segmented, Select, Space, Switch, Table, Tag, Typography, Alert } from 'antd'
import { DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import { Plus, RefreshCw, ShieldCheck } from 'lucide-react'
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

  return (
    <>
      <section className="app-content" aria-label="分配规则">
        <div className="content-title">
          <div>
            <h1>分配规则</h1>
            <p>按优先级自动匹配新工单，命中后分配给指定处理人；保存后仅影响后续新建工单。</p>
          </div>
          <div className="content-actions">
            <button disabled={rulesLoading} onClick={onFetchAssignmentRules} type="button">
              <RefreshCw size={16} />
              刷新
            </button>
            <button
              className="primary-action"
              disabled={!canCreateAssignmentRules}
              onClick={onOpenCreateRule}
              type="button"
            >
              <Plus size={16} />
              新建规则
            </button>
          </div>
        </div>

        {!canReadAssignmentRules ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无分配规则管理权限</strong>
            <p>当前账号没有分配规则查看权限；新建、编辑、启停、排序或删除由独立权限控制。</p>
          </div>
        ) : (
          <>
            <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">规则总数</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{summary?.totalCount ?? '--'}</Typography.Title>
                  <Typography.Text type="secondary">启用 {summary?.enabledCount ?? '--'} 条，停用 {summary?.disabledCount ?? '--'} 条</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">默认兜底</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{summary?.defaultCount ?? '--'}</Typography.Title>
                  <Typography.Text type="secondary">仅允许一个 DEFAULT 规则</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">当前命中测试</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{matchResult?.matched ? '已命中' : '--'}</Typography.Title>
                  <Typography.Text type="secondary">{matchResult?.ruleName || '输入邮件信息后测试'}</Typography.Text>
                </Card>
              </Col>
              <Col xs={24} md={12} xl={6}>
                <Card>
                  <Typography.Text type="secondary">未保存修改</Typography.Text>
                  <Typography.Title level={2} style={{ margin: '6px 0 0' }}>{ruleDirty ? '1' : '0'}</Typography.Title>
                  <Typography.Text type="secondary">保存前不会影响自动建单</Typography.Text>
                </Card>
              </Col>
            </Row>

            <Alert
              showIcon
              type="info"
              style={{ marginBottom: 16 }}
              title="优先级数字越小越先匹配；排序、启停和规则保存只影响后续自动建单，历史工单不会回写。"
            />

            {rulesError && (
              <Alert
                showIcon
                type="error"
                style={{ marginBottom: 16 }}
                title={rulesError}
                action={<Button size="small" onClick={onFetchAssignmentRules}>重试</Button>}
              />
            )}

            <Row gutter={[16, 16]}>
              <Col xs={24} xl={12}>
                <Card
                  title="规则列表"
                  extra={<Tag color="blue">共 {summary?.totalCount ?? 0} 条</Tag>}
                >
                  <Space wrap style={{ width: '100%', marginBottom: 16 }}>
                    <Input
                      allowClear
                      prefix={<SearchOutlined />}
                      placeholder="规则名 / 匹配值"
                      style={{ width: 220 }}
                      value={keyword}
                      onChange={(event) => onKeywordChange(event.target.value)}
                      onPressEnter={() => void onFetchAssignmentRules()}
                    />
                    <Select
                      style={{ width: 130 }}
                      value={assignmentEnabledFilter}
                      onChange={onEnabledFilterChange}
                      options={[
                        { value: 'ALL', label: '全部状态' },
                        { value: 'true', label: '启用' },
                        { value: 'false', label: '停用' },
                      ]}
                    />
                    <Select
                      style={{ width: 150 }}
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
                  </Space>

                  <Table<AssignmentRule>
                    rowKey="id"
                    size="middle"
                    loading={rulesLoading}
                    dataSource={sortedRecords}
                    pagination={false}
                    locale={{
                      emptyText: (
                        <Empty
                          description="还没有分配规则"
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                        >
                          <Button type="primary" onClick={onOpenCreateRule}>新建规则</Button>
                        </Empty>
                      ),
                    }}
                    rowClassName={(record) => record.id === form.id ? 'ant-table-row-selected' : ''}
                    onRow={(record) => ({
                      onClick: () => onSelectRule(record),
                    })}
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
                            <Button
                              size="small"
                              disabled={index === 0 || actionLoading}
                              onClick={() => void onMoveRule(record, -1)}
                            >
                              上移
                            </Button>
                            <Button
                              size="small"
                              disabled={index >= sortedRecords.length - 1 || actionLoading}
                              onClick={() => void onMoveRule(record, 1)}
                            >
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
                </Card>
              </Col>

              <Col xs={24} xl={12}>
                <Row gutter={[16, 16]}>
                  <Col span={24}>
                    <Card
                      title="规则编辑"
                      extra={
                        ruleDirty
                          ? <Tag color="orange">有未保存修改</Tag>
                          : selectedRule
                            ? <Tag color="green">已保存</Tag>
                            : <Tag>新建草稿</Tag>
                      }
                    >
                      <Row gutter={[12, 12]}>
                        <Col xs={24} md={12}>
                          <Typography.Text strong>规则名称</Typography.Text>
                          <Input
                            value={form.ruleName}
                            onChange={(event) => onUpdateForm({ ruleName: event.target.value })}
                            placeholder="VIP 售后优先"
                            style={{ marginTop: 8 }}
                          />
                        </Col>
                        <Col xs={24} md={12}>
                          <Typography.Text strong>优先级</Typography.Text>
                          <Input
                            type="number"
                            min={1}
                            max={9999}
                            value={form.priorityOrder}
                            onChange={(event) => onUpdateForm({ priorityOrder: Number(event.target.value || 1) })}
                            style={{ marginTop: 8 }}
                          />
                        </Col>
                        <Col span={24}>
                          <Typography.Text strong>匹配类型</Typography.Text>
                          <Segmented
                            block
                            style={{ marginTop: 8 }}
                            value={form.matchType}
                            onChange={(value) => onUpdateForm({ matchType: value as AssignmentRuleMatchType })}
                            options={[
                              { value: 'DEFAULT', label: '默认' },
                              { value: 'SUBJECT_KEYWORD', label: '主题关键词' },
                              { value: 'MAILBOX', label: '来源邮箱' },
                              { value: 'FROM_EMAIL', label: '客户邮箱' },
                            ]}
                          />
                        </Col>
                        <Col xs={24} md={12}>
                          <Typography.Text strong>匹配值</Typography.Text>
                          <Input
                            disabled={form.matchType === 'DEFAULT'}
                            value={form.matchValue}
                            onChange={(event) => onUpdateForm({ matchValue: event.target.value })}
                            placeholder={form.matchType === 'DEFAULT' ? '默认规则不需要匹配值' : 'VIP / support@example.com'}
                            status={form.matchType !== 'DEFAULT' && !form.matchValue.trim() ? 'error' : undefined}
                            style={{ marginTop: 8 }}
                          />
                          {form.matchType !== 'DEFAULT' && !form.matchValue.trim() && (
                            <Typography.Text type="danger" style={{ fontSize: 12 }}>
                              匹配类型不是 DEFAULT 时，匹配值不能为空。
                            </Typography.Text>
                          )}
                        </Col>
                        <Col xs={24} md={12}>
                          <Typography.Text strong>分配处理人</Typography.Text>
                          <Select
                            showSearch
                            value={form.assigneeId || undefined}
                            placeholder="选择处理人"
                            optionFilterProp="label"
                            options={assigneeOptions}
                            onChange={(value) => onUpdateForm({ assigneeId: value })}
                            style={{ width: '100%', marginTop: 8 }}
                          />
                        </Col>
                        <Col xs={24} md={12}>
                          <Typography.Text strong>启用状态</Typography.Text>
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
                          <Typography.Text strong>分配通知</Typography.Text>
                          <Select
                            value={String(form.notifyEnabled)}
                            onChange={(value) => onUpdateForm({ notifyEnabled: value === 'true' })}
                            options={[
                              { value: 'true', label: '通知处理人' },
                              { value: 'false', label: '不发送通知' },
                            ]}
                            style={{ width: '100%', marginTop: 8 }}
                          />
                        </Col>
                        <Col span={24}>
                          <Alert
                            type="info"
                            showIcon
                            title="规则预览"
                            description={`IF ${assignmentMatchTypeLabel(form.matchType)} ${form.matchType === 'DEFAULT' ? '兜底命中' : `= ${form.matchValue || '未填写'}`} THEN 分配给 ${selectedAssigneeName}`}
                          />
                        </Col>
                      </Row>
                      <Space style={{ marginTop: 16 }}>
                        <Button onClick={onOpenCreateRule}>新建草稿</Button>
                        <Button
                          type="primary"
                          loading={saving}
                          disabled={!form.ruleName.trim() || !form.assigneeId || (form.matchType !== 'DEFAULT' && !form.matchValue.trim())}
                          onClick={() => void onSaveRule()}
                        >
                          保存规则
                        </Button>
                        <Button
                          danger
                          disabled={!selectedRule}
                          icon={<DeleteOutlined />}
                          onClick={() => selectedRule && onRequestDelete(selectedRule)}
                        >
                          删除
                        </Button>
                      </Space>
                    </Card>
                  </Col>

                  <Col span={24}>
                    <Card title="测试匹配" extra={matchResult?.matched ? <Tag color="green">已命中</Tag> : <Tag>未测试</Tag>}>
                      <Row gutter={[12, 12]}>
                        <Col xs={24} md={12}>
                          <Typography.Text strong>来源邮箱</Typography.Text>
                          <Select
                            showSearch
                            value={testForm.mailboxId || undefined}
                            placeholder="选择来源邮箱"
                            optionFilterProp="label"
                            options={mailboxOptions}
                            onChange={(value) => onTestFormChange({ mailboxId: value })}
                            style={{ width: '100%', marginTop: 8 }}
                          />
                        </Col>
                        <Col xs={24} md={12}>
                          <Typography.Text strong>客户邮箱</Typography.Text>
                          <Input
                            value={testForm.fromEmail}
                            onChange={(event) => onTestFormChange({ fromEmail: event.target.value })}
                            placeholder="buyer@acme.com"
                            style={{ marginTop: 8 }}
                          />
                        </Col>
                        <Col span={24}>
                          <Typography.Text strong>邮件主题</Typography.Text>
                          <Input
                            value={testForm.subject}
                            onChange={(event) => onTestFormChange({ subject: event.target.value })}
                            placeholder="VIP 客户反馈：无法登录后台"
                            style={{ marginTop: 8 }}
                          />
                        </Col>
                      </Row>
                      <Button
                        block
                        type="primary"
                        loading={testing}
                        disabled={!testForm.mailboxId}
                        onClick={() => void onRunTest()}
                        style={{ marginTop: 16 }}
                      >
                        运行测试匹配
                      </Button>
                      {matchResult ? (
                        <Alert
                          showIcon
                          type={matchResult.matched ? 'success' : 'warning'}
                          style={{ marginTop: 16 }}
                          title={matchResult.matched ? `命中 ${matchResult.ruleName}` : '未命中分配规则'}
                          description={matchResult.matched
                            ? `${assignmentMatchTypeLabel(matchResult.matchType)} = ${matchResult.matchValue || '-'}，分配给 ${matchResult.assigneeName || matchResult.assigneeId}，${matchResult.notifyEnabled ? '通知处理人' : '不发送通知'}。`
                            : '当前输入未命中任何启用规则，自动建单会继续走默认规则或邮箱默认处理人。'}
                        />
                      ) : (
                        <Empty
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                          description="输入来源邮箱、客户邮箱和主题后测试命中结果"
                          style={{ marginTop: 16 }}
                        />
                      )}
                    </Card>
                  </Col>
                </Row>
              </Col>
            </Row>
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
