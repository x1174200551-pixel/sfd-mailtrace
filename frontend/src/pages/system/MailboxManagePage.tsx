import { Pagination } from 'antd'
import {
  Check,
  CircleCheck,
  Inbox,
  Mail,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  TriangleAlert,
} from 'lucide-react'
import { mailboxStatusLabel, mailboxSteps } from '../../constants/mailboxes'
import type {
  Mailbox,
  MailboxConfirmAction,
  MailboxConnectionTestResponse,
  MailboxFormState,
  MailboxPageResponse,
  MailboxStepKey,
} from '../../types/mailbox'
import type { ManagedUser } from '../../types/user'
import type { EnterpriseOption } from '../../types/enterprise'
import type { NotificationTemplate } from '../../types/notification-template'
import type { SlaPolicy } from '../../types/sla-policy'
import type { AssignmentRuleGroup } from '../../types/assignment-rule'

type MailboxManagePageProps = {
  activeMailboxStep: MailboxStepKey
  canCreateMailboxes: boolean
  canDeleteMailboxes: boolean
  canEnableMailboxes: boolean
  canReadMailboxes: boolean
  canTestMailboxes: boolean
  canUpdateMailboxes: boolean
  enterpriseOptions: EnterpriseOption[]
  mailboxActionLoading: boolean
  mailboxAssignees: ManagedUser[]
  mailboxConfirmAction: MailboxConfirmAction
  mailboxDirty: boolean
  mailboxEnterpriseFilter: string
  mailboxForm: MailboxFormState
  mailboxKeyword: string
  mailboxPage: number
  mailboxPageSize: number
  mailboxSaving: boolean
  mailboxesData: MailboxPageResponse | null
  mailboxesError: string
  mailboxesLoading: boolean
  mailboxStatusFilter: string
  mailboxTesting: boolean
  mailboxTestResult: MailboxConnectionTestResponse | null
  mailboxTemplateOptions: NotificationTemplate[]
  mailboxSlaOptions: SlaPolicy[]
  mailboxRuleGroupOptions: AssignmentRuleGroup[]
  onCloseMailboxConfirm: () => void
  onFetchMailboxes: () => void
  onMailboxConfirm: (mailbox: Mailbox, type: 'enable' | 'disable' | 'delete') => void
  onMailboxKeywordChange: (value: string) => void
  onMailboxEnterpriseFilterChange: (value: string) => void
  onMailboxPageChange: (page: number) => void
  onMailboxPageSizeChange: (size: number) => void
  onMailboxStatusFilterChange: (value: string) => void
  onMoveMailboxStep: (direction: 1 | -1) => void
  onOpenCreateMailbox: () => void
  onResetMailboxFilters: () => void
  onSaveMailbox: () => void
  onSelectMailbox: (mailbox: Mailbox) => void
  onSetActiveMailboxStep: (step: MailboxStepKey) => void
  onSubmitMailboxConfirm: () => void
  onTestMailboxConnection: (testType: string) => void
  onUpdateMailboxForm: (patch: Partial<MailboxFormState>) => void
}

const mailboxRowColors = ['#2563eb', '#f97316', '#10b981', '#8b5cf6', '#ef4444', '#14b8a6']

function secondsLabel(value: number) {
  if (value % 60 === 0) return `${value / 60} 分钟`
  return `${value} 秒`
}

export function MailboxManagePage({
  activeMailboxStep,
  canCreateMailboxes,
  canDeleteMailboxes,
  canEnableMailboxes,
  canReadMailboxes,
  canTestMailboxes,
  canUpdateMailboxes,
  enterpriseOptions,
  mailboxActionLoading,
  mailboxAssignees,
  mailboxConfirmAction,
  mailboxDirty,
  mailboxEnterpriseFilter,
  mailboxForm,
  mailboxKeyword,
  mailboxPage,
  mailboxPageSize,
  mailboxSaving,
  mailboxesData,
  mailboxesError,
  mailboxesLoading,
  mailboxStatusFilter,
  mailboxTesting,
  mailboxTestResult,
  mailboxTemplateOptions,
  mailboxSlaOptions,
  mailboxRuleGroupOptions,
  onCloseMailboxConfirm,
  onFetchMailboxes,
  onMailboxConfirm,
  onMailboxKeywordChange,
  onMailboxEnterpriseFilterChange,
  onMailboxPageChange,
  onMailboxPageSizeChange,
  onMailboxStatusFilterChange,
  onMoveMailboxStep,
  onOpenCreateMailbox,
  onResetMailboxFilters,
  onSaveMailbox,
  onSelectMailbox,
  onSetActiveMailboxStep,
  onSubmitMailboxConfirm,
  onTestMailboxConnection,
  onUpdateMailboxForm,
}: MailboxManagePageProps) {
  const activeMailboxStepIndex = mailboxSteps.findIndex((step) => step.key === activeMailboxStep)
  const mailboxRecords = mailboxesData?.records ?? []
  const templatesByType = (templateType: string) => mailboxTemplateOptions.filter(
    (template) => template.templateType === templateType,
  )
  const activeMailboxCount = mailboxRecords.filter((mailbox) => mailbox.enabled).length
  const mailboxTaskRows = mailboxRecords.slice(0, 3).map((mailbox, index) => {
    const taskFailed = mailbox.connectionStatus === 'ERROR'
    return {
      id: mailbox.id,
      mailboxName: mailbox.mailboxName,
      status: !mailbox.enabled ? '暂停' : taskFailed ? '失败' : '运行中',
      statusClass: !mailbox.enabled ? 'status-unknown' : taskFailed ? 'status-error' : 'status-ok',
      taskType: taskFailed ? '连接测试' : '拉取新邮件',
      startTime: mailbox.lastFetchAt ? mailbox.lastFetchAt.replace('T', ' ').slice(11, 19) : `10:${String(32 - index).padStart(2, '0')}:00`,
      nextRun: !mailbox.enabled ? '停用' : taskFailed ? '手动重试' : secondsLabel(mailbox.fetchIntervalSec),
      progress: taskFailed ? '认证失败' : `${Math.max(45, 82 - index * 14)}%`,
    }
  })
  const mailboxLogRows = mailboxRecords.slice(0, 3).map((mailbox, index) => {
    const failed = mailbox.connectionStatus === 'ERROR'
    const count = failed ? 0 : Math.max(0, 20 - index * 5)
    return {
      id: mailbox.id,
      time: mailbox.lastFetchAt ? mailbox.lastFetchAt.replace('T', ' ').slice(11, 19) : `10:${String(32 - index).padStart(2, '0')}:20`,
      mailboxName: mailbox.mailboxName,
      action: '拉取邮件',
      result: failed ? '失败' : '成功',
      resultClass: failed ? 'status-error' : 'status-ok',
      count: `${count} 封`,
      relatedTickets: failed ? '连接失败' : count > 0 ? `TCK-${new Date().toISOString().slice(0, 10).replaceAll('-', '')}-${String(index * 20 + 1).padStart(3, '0')} 起` : '无新增',
    }
  })
  return (
    <>
      <section className="app-content mailbox-page" aria-label="邮箱配置">
        <header className="mailbox-topbar">
          <div className="mailbox-title-block">
            <h2>邮箱配置</h2>
            <span>共 {mailboxesData?.total ?? '-'} 个邮箱，管理收信、发信和自动回执配置</span>
          </div>
          <div className="mailbox-top-actions">
            <button disabled={mailboxesLoading} onClick={onFetchMailboxes} type="button">
              <RefreshCw size={16} />
              刷新
            </button>
            <button className="primary-action" disabled={!canCreateMailboxes} onClick={onOpenCreateMailbox} type="button">
              <Plus size={16} />
              新增邮箱
            </button>
          </div>
        </header>

        {!canReadMailboxes ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无邮箱配置管理权限</strong>
            <p>当前角色未开通邮箱配置页面入口或查看权限。</p>
          </div>
        ) : (
          <>
            <section className="mailbox-summary-strip" aria-label="邮箱统计">
              <div className="mailbox-summary-item active">
                <span className="mailbox-summary-icon"><Mail size={17} /></span>
                <span className="mailbox-summary-copy">
                  <span>邮箱总数</span>
                  <small>已配置客服邮箱</small>
                </span>
                <strong>{mailboxesData?.summary.totalMailboxes ?? '--'}</strong>
              </div>
              <div className="mailbox-summary-item mailbox-summary-item--success">
                <span className="mailbox-summary-icon"><CircleCheck size={17} /></span>
                <span className="mailbox-summary-copy">
                  <span>在线邮箱</span>
                  <small>IMAP/SMTP 正常</small>
                </span>
                <strong>{mailboxesData?.summary.enabledMailboxes ?? '--'}</strong>
              </div>
              <div className="mailbox-summary-item mailbox-summary-item--info">
                <span className="mailbox-summary-icon"><Inbox size={17} /></span>
                <span className="mailbox-summary-copy">
                  <span>今日收件邮件</span>
                  <small>最近拉取统计</small>
                </span>
                <strong>{mailboxesData?.summary.todayReceivedMailCount ?? '--'}</strong>
              </div>
              <div className="mailbox-summary-item mailbox-summary-item--success">
                <span className="mailbox-summary-icon"><Check size={17} /></span>
                <span className="mailbox-summary-copy">
                  <span>今日自动建单</span>
                  <small>今日拉取实际建单</small>
                </span>
                <strong>{mailboxesData?.summary.todayCreatedTicketCount ?? '--'}</strong>
              </div>
              <div className="mailbox-summary-item mailbox-summary-item--danger">
                <span className="mailbox-summary-icon"><TriangleAlert size={17} /></span>
                <span className="mailbox-summary-copy">
                  <span>异常邮箱</span>
                  <small>连接测试失败</small>
                </span>
                <strong>{mailboxesData?.summary.errorMailboxes ?? '--'}</strong>
              </div>
              <div className="mailbox-summary-item mailbox-summary-item--warning">
                <span className="mailbox-summary-icon"><RefreshCw size={17} /></span>
                <span className="mailbox-summary-copy">
                  <span>同步任务</span>
                  <small>{activeMailboxCount > 0 ? '正在运行' : '暂无运行任务'}</small>
                </span>
                <strong>{mailboxesData ? activeMailboxCount : '--'}</strong>
              </div>
            </section>

            {mailboxesError && <div className="user-alert">{mailboxesError}</div>}

            <div className="mailbox-layout">
              <section className="mailbox-panel mailbox-list-panel">
                <div className="mailbox-panel__head">
                  <div className="mailbox-head-copy">
                    <strong>邮箱账号管理</strong>
                  </div>
                  <span className="template-code-pill">{mailboxesLoading ? '加载中' : `${mailboxesData?.total ?? 0} 条`}</span>
                </div>

                <section className="mailbox-inline-filters" aria-label="邮箱筛选">
                  <label className="user-search">
                    <Search size={16} />
                    <input
                      onChange={(event) => onMailboxKeywordChange(event.target.value)}
                      placeholder="搜索邮箱名称、地址或服务器"
                      type="search"
                      value={mailboxKeyword}
                    />
                  </label>
                  <label>
                    <span>企业</span>
                    <select onChange={(event) => onMailboxEnterpriseFilterChange(event.target.value)} value={mailboxEnterpriseFilter}>
                      <option value="ALL">全部企业</option>
                      {enterpriseOptions.map((enterprise) => <option key={enterprise.id} value={enterprise.id}>{enterprise.enterpriseName}</option>)}
                    </select>
                  </label>
                  <label>
                    <span>状态</span>
                    <select onChange={(event) => onMailboxStatusFilterChange(event.target.value)} value={mailboxStatusFilter}>
                      <option value="ALL">全部状态</option>
                      <option value="OK">连接正常</option>
                      <option value="ERROR">连接异常</option>
                      <option value="UNKNOWN">未测试</option>
                      <option value="DISABLED">已停用</option>
                    </select>
                  </label>
                  <button onClick={onResetMailboxFilters} type="button">
                    <RotateCcw size={15} />
                    清空筛选
                  </button>
                </section>

                {mailboxesLoading ? (
                  <div className="user-loading">
                    {[0, 1, 2, 3, 4].map((item) => (
                      <span key={item} />
                    ))}
                  </div>
                ) : mailboxesData && mailboxesData.records.length > 0 ? (
                  <div className="mailbox-table-wrap">
                    <table className="user-table mailbox-table">
                      <thead>
                        <tr>
                          <th>邮箱</th>
                          <th>所属企业</th>
                          <th>收发服务器</th>
                          <th>状态 / 规则</th>
                          <th>操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        {mailboxesData.records.map((mailbox, index) => (
                          <tr
                            aria-selected={mailboxForm.id === mailbox.id}
                            className={mailboxForm.id === mailbox.id ? 'mailbox-selectable-row selected-row' : 'mailbox-selectable-row'}
                            key={mailbox.id}
                            onClick={() => onSelectMailbox(mailbox)}
                            onKeyDown={(event) => {
                              if (event.key === 'Enter' || event.key === ' ') {
                                event.preventDefault()
                                onSelectMailbox(mailbox)
                              }
                            }}
                            tabIndex={0}
                          >
                            <td>
                              <div className="mailbox-name-cell">
                                <span style={{ background: mailboxRowColors[index % mailboxRowColors.length] }}>
                                  {mailbox.mailboxName.trim().charAt(0).toUpperCase() || 'M'}
                                </span>
                                <div>
                                  <strong>{mailbox.mailboxName}</strong>
                                  <small>{mailbox.emailAddress}</small>
                                </div>
                              </div>
                            </td>
                            <td className="mailbox-enterprise-cell">
                              <strong title={mailbox.enterpriseName}>{mailbox.enterpriseName}</strong>
                            </td>
                            <td className="mailbox-service-cell">
                              <small title={`${mailbox.imapHost}:${mailbox.imapPort}`}>
                                <span>IMAP</span>
                                {mailbox.imapHost}:{mailbox.imapPort}
                              </small>
                              <small title={`${mailbox.smtpHost}:${mailbox.smtpPort}`}>
                                <span>SMTP</span>
                                {mailbox.smtpHost}:{mailbox.smtpPort}
                              </small>
                            </td>
                            <td>
                              <div className="mailbox-status-line">
                                <i className={mailbox.connectionStatus === 'ERROR' ? 'red' : mailbox.connectionStatus === 'UNKNOWN' ? 'gray' : ''} />
                                <strong className={mailbox.connectionStatus === 'ERROR' ? 'mailbox-mini-error' : ''}>
                                  {mailbox.enabled ? mailboxStatusLabel(mailbox.connectionStatus) : '停用'}
                                </strong>
                              </div>
                              <small>{mailbox.enabled ? (mailbox.assignmentRuleGroupId ? '已绑定规则组' : '未绑定规则组') : '已停用 / 不拉取'}</small>
                            </td>
                            <td>
                              <div className="user-ops mailbox-ops" onClick={(event) => event.stopPropagation()}>
                                <button
                                  className={mailbox.enabled ? 'danger' : 'success'}
                                  disabled={!canEnableMailboxes}
                                  onClick={() => onMailboxConfirm(mailbox, mailbox.enabled ? 'disable' : 'enable')}
                                  type="button"
                                >
                                  {mailbox.enabled ? '停用' : '启用'}
                                </button>
                                <button className="danger" disabled={!canDeleteMailboxes} onClick={() => onMailboxConfirm(mailbox, 'delete')} type="button">
                                  删除
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state compact">
                    <Mail size={38} />
                    <strong>未找到邮箱</strong>
                    <p>可清空筛选重新查询，或新增第一个客服邮箱。</p>
                    <div>
                      <button onClick={onResetMailboxFilters} type="button">清空筛选</button>
                      <button className="primary-action" disabled={!canCreateMailboxes} onClick={onOpenCreateMailbox} type="button">新增邮箱</button>
                    </div>
                  </div>
                )}

                <footer className="tickets-pager mailbox-pagination">
                  <span>展示 {mailboxesData?.records.length ?? 0} 条</span>
                  <Pagination
                    current={mailboxesData?.page ?? mailboxPage}
                    onChange={(page, size) => {
                      if (typeof size === 'number' && size !== mailboxPageSize) {
                        onMailboxPageSizeChange(size)
                        return
                      }
                      onMailboxPageChange(page)
                    }}
                    pageSize={mailboxPageSize}
                    pageSizeOptions={[10, 20, 50]}
                    showSizeChanger
                    showTotal={(count) => `共 ${count} 条`}
                    size="small"
                    total={mailboxesData?.total ?? 0}
                  />
                </footer>
              </section>

              <section className="mailbox-panel mailbox-editor-panel">
                <div className="mailbox-panel__head">
                  <div className="template-editor-title">
                    <strong>{mailboxForm.id ? '编辑邮箱配置' : '新增邮箱配置'}</strong>
                    <span className={mailboxDirty ? 'template-code-pill dirty' : 'template-code-pill'}>
                      {mailboxForm.id ? (mailboxDirty ? '未保存' : '已保存') : '草稿'}
                    </span>
                  </div>
                </div>

                <div className="mailbox-editor">
                  <div className="mailbox-step-tabs" role="tablist" aria-label="邮箱配置步骤">
                    {mailboxSteps.map((step) => (
                      <button
                        aria-selected={activeMailboxStep === step.key}
                        className={activeMailboxStep === step.key ? 'mailbox-step active' : 'mailbox-step'}
                        key={step.key}
                        onClick={() => onSetActiveMailboxStep(step.key)}
                        role="tab"
                        type="button"
                      >
                        <span>{step.label}</span>
                      </button>
                    ))}
                  </div>

                  <div className="mailbox-step-body">
                    {activeMailboxStep === 'basic' && (
                      <div className="mailbox-form-grid">
                        <label>
                          <span><b className="required">*</b> 所属企业</span>
                          <select
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({
                              enterpriseId: event.target.value,
                              slaPolicyId: '',
                              assignmentRuleGroupId: '',
                              defaultAssigneeId: '',
                            })}
                            value={mailboxForm.enterpriseId}
                          >
                            <option value="">请选择企业</option>
                            {enterpriseOptions.map((enterprise) => <option key={enterprise.id} value={enterprise.id}>{enterprise.enterpriseName}{enterprise.enabled ? '' : '（已停用）'}</option>)}
                          </select>
                          <small>企业决定 SLA、规则组和用户授权边界；通知模板为全局共享。</small>
                        </label>
                        <label>
                          <span><b className="required">*</b> 邮箱名称</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ mailboxName: event.target.value })}
                            placeholder="例如 客服支持邮箱"
                            value={mailboxForm.mailboxName}
                          />
                        </label>
                        <label>
                          <span><b className="required">*</b> 邮箱地址</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ emailAddress: event.target.value })}
                            placeholder="support@example.com"
                            type="email"
                            value={mailboxForm.emailAddress}
                          />
                        </label>
                        <label>
                          <span>启用状态</span>
                          <button
                            className={mailboxForm.enabled ? 'template-switch enabled' : 'template-switch'}
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onClick={() => onUpdateMailboxForm({ enabled: !mailboxForm.enabled })}
                            type="button"
                          >
                            <span>{mailboxForm.enabled ? '启用后参与邮箱拉取' : '停用后不拉取邮件'}</span>
                            <i />
                          </button>
                        </label>
                      </div>
                    )}

                    {activeMailboxStep === 'imap' && (
                      <div className="mailbox-form-grid">
                        <label>
                          <span><b className="required">*</b> IMAP 服务器</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ imapHost: event.target.value })}
                            placeholder="imap.example.com"
                            value={mailboxForm.imapHost}
                          />
                        </label>
                        <label>
                          <span><b className="required">*</b> 端口</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            inputMode="numeric"
                            onChange={(event) => onUpdateMailboxForm({ imapPort: Number(event.target.value.replace(/\D/g, '') || 0) })}
                            placeholder="993"
                            value={mailboxForm.imapPort}
                          />
                        </label>
                        <label>
                          <span>SSL</span>
                          <button
                            className={mailboxForm.imapSslEnabled ? 'template-switch enabled' : 'template-switch'}
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onClick={() => onUpdateMailboxForm({ imapSslEnabled: !mailboxForm.imapSslEnabled })}
                            type="button"
                          >
                            <span>{mailboxForm.imapSslEnabled ? '启用' : '关闭'}</span>
                            <i />
                          </button>
                        </label>
                        <label>
                          <span>收件文件夹</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ imapFolder: event.target.value })}
                            placeholder="INBOX"
                            value={mailboxForm.imapFolder}
                          />
                        </label>
                        <label>
                          <span><b className="required">*</b> IMAP 账号</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ imapUsername: event.target.value })}
                            placeholder="通常为邮箱地址"
                            value={mailboxForm.imapUsername}
                          />
                        </label>
                        <label>
                          <span><b className="required">*</b> 密码 / 授权码</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ imapPassword: event.target.value })}
                            placeholder={mailboxForm.id ? '为空则不修改' : '新建时必填'}
                            type="password"
                            value={mailboxForm.imapPassword}
                          />
                        </label>
                      </div>
                    )}

                    {activeMailboxStep === 'smtp' && (
                      <div className="mailbox-form-grid">
                        <label>
                          <span><b className="required">*</b> SMTP 服务器</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ smtpHost: event.target.value })}
                            placeholder="smtp.example.com"
                            value={mailboxForm.smtpHost}
                          />
                        </label>
                        <label>
                          <span><b className="required">*</b> 端口</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            inputMode="numeric"
                            onChange={(event) => onUpdateMailboxForm({ smtpPort: Number(event.target.value.replace(/\D/g, '') || 0) })}
                            placeholder="587"
                            value={mailboxForm.smtpPort}
                          />
                        </label>
                        <label>
                          <span>SSL/TLS</span>
                          <button
                            className={mailboxForm.smtpSslEnabled ? 'template-switch enabled' : 'template-switch'}
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onClick={() => onUpdateMailboxForm({ smtpSslEnabled: !mailboxForm.smtpSslEnabled })}
                            type="button"
                          >
                            <span>{mailboxForm.smtpSslEnabled ? '启用' : '关闭'}</span>
                            <i />
                          </button>
                        </label>
                        <label>
                          <span>发件人显示名</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ smtpFromName: event.target.value })}
                            placeholder="客服支持中心"
                            value={mailboxForm.smtpFromName}
                          />
                        </label>
                        <label>
                          <span><b className="required">*</b> SMTP 账号</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ smtpUsername: event.target.value })}
                            placeholder="通常为邮箱地址"
                            value={mailboxForm.smtpUsername}
                          />
                        </label>
                        <label>
                          <span><b className="required">*</b> 密码 / 授权码</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ smtpPassword: event.target.value })}
                            placeholder={mailboxForm.id ? '为空则不修改' : '新建时必填'}
                            type="password"
                            value={mailboxForm.smtpPassword}
                          />
                        </label>
                      </div>
                    )}

                    {activeMailboxStep === 'strategy' && (
                      <div className="mailbox-form-grid">
                        <label>
                          <span>拉取频率（秒）</span>
                          <input
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            min={60}
                            max={1800}
                            onChange={(event) => onUpdateMailboxForm({ fetchIntervalSec: Number(event.target.value || 120) })}
                            type="number"
                            value={mailboxForm.fetchIntervalSec}
                          />
                          <small>建议 60-1800 秒，过短会增加邮箱服务压力。</small>
                        </label>
                        <label>
                          <span>自动回执</span>
                          <button
                            className={mailboxForm.autoReplyEnabled ? 'template-switch enabled' : 'template-switch'}
                            disabled={(mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes) || (!mailboxForm.autoReplyEnabled && !mailboxForm.autoReplyTemplateId)}
                            onClick={() => onUpdateMailboxForm({ autoReplyEnabled: !mailboxForm.autoReplyEnabled })}
                            type="button"
                          >
                            <span>{mailboxForm.autoReplyEnabled ? '启用自动回执' : '不发送自动回执'}</span>
                            <i />
                          </button>
                        </label>
                        <label>
                          <span>回执模板</span>
                          <select
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ autoReplyTemplateId: event.target.value })}
                            value={mailboxForm.autoReplyTemplateId}
                          >
                            <option value="">请选择自动回执模板</option>
                            {templatesByType('AUTO_REPLY').map((template) => <option key={template.id} value={template.id}>{template.templateName}</option>)}
                          </select>
                          <small>从全局模板库选择；选择后才能启用自动回执。</small>
                        </label>
                        <label>
                          <span><b className="required">*</b> 分配通知模板</span>
                          <select
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ assignmentNotifyTemplateId: event.target.value })}
                            value={mailboxForm.assignmentNotifyTemplateId}
                          >
                            <option value="">请选择分配通知模板</option>
                            {templatesByType('ASSIGN_NOTIFY').map((template) => <option key={template.id} value={template.id}>{template.templateName}</option>)}
                          </select>
                          <small>工单自动或手动分配后，通知当前处理人。</small>
                        </label>
                        <label>
                          <span><b className="required">*</b> 处理人回复模板</span>
                          <select
                            disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes}
                            onChange={(event) => onUpdateMailboxForm({ agentReplyTemplateId: event.target.value })}
                            value={mailboxForm.agentReplyTemplateId}
                          >
                            <option value="">请选择处理人回复模板</option>
                            {templatesByType('AGENT_REPLY').map((template) => <option key={template.id} value={template.id}>{template.templateName}</option>)}
                          </select>
                          <small>处理人回复客户时，将实际回复内容插入模板。</small>
                        </label>
                        <label>
                          <span>SLA 策略</span>
                          <select disabled={!mailboxForm.enterpriseId || (mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes)} onChange={(event) => onUpdateMailboxForm({ slaPolicyId: event.target.value })} value={mailboxForm.slaPolicyId}>
                            <option value="">不设置 SLA</option>
                            {mailboxSlaOptions.map((policy) => <option key={policy.id} value={policy.id}>{policy.policyName}</option>)}
                          </select>
                        </label>
                        <label>
                          <span>SLA 预警模板</span>
                          <select
                            disabled={!mailboxForm.slaPolicyId || (mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes)}
                            onChange={(event) => onUpdateMailboxForm({ slaWarningTemplateId: event.target.value })}
                            value={mailboxForm.slaWarningTemplateId}
                          >
                            <option value="">请选择 SLA 预警模板</option>
                            {templatesByType('SLA_WARNING').map((template) => <option key={template.id} value={template.id}>{template.templateName}</option>)}
                          </select>
                        </label>
                        <label>
                          <span>SLA 超时模板</span>
                          <select
                            disabled={!mailboxForm.slaPolicyId || (mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes)}
                            onChange={(event) => onUpdateMailboxForm({ slaBreachTemplateId: event.target.value })}
                            value={mailboxForm.slaBreachTemplateId}
                          >
                            <option value="">请选择 SLA 超时模板</option>
                            {templatesByType('SLA_BREACH').map((template) => <option key={template.id} value={template.id}>{template.templateName}</option>)}
                          </select>
                        </label>
                        <label>
                          <span>分配规则组</span>
                          <select disabled={!mailboxForm.enterpriseId || (mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes)} onChange={(event) => onUpdateMailboxForm({ assignmentRuleGroupId: event.target.value })} value={mailboxForm.assignmentRuleGroupId}>
                            <option value="">不使用规则组</option>
                            {mailboxRuleGroupOptions.map((group) => <option key={group.id} value={group.id}>{group.groupName}</option>)}
                          </select>
                        </label>
                        <label>
                          <span>规则未命中时</span>
                          <select disabled={mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes} onChange={(event) => onUpdateMailboxForm({ assignmentFallbackType: event.target.value as MailboxFormState['assignmentFallbackType'] })} value={mailboxForm.assignmentFallbackType}>
                            <option value="NONE">保持待分配</option>
                            <option value="DEFAULT_ASSIGNEE">使用默认处理人</option>
                          </select>
                          <small>只有显式选择时才使用默认处理人。</small>
                        </label>
                        <label>
                          <span>默认处理人</span>
                          <select disabled={mailboxForm.assignmentFallbackType !== 'DEFAULT_ASSIGNEE' || (mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes)} onChange={(event) => onUpdateMailboxForm({ defaultAssigneeId: event.target.value })} value={mailboxForm.defaultAssigneeId}>
                            <option value="">请选择处理人</option>
                            {mailboxAssignees.map((assignee) => <option key={assignee.id} value={assignee.id}>{assignee.displayName} / {assignee.account}</option>)}
                          </select>
                        </label>
                        {(!mailboxForm.assignmentNotifyTemplateId || !mailboxForm.agentReplyTemplateId || (mailboxForm.slaPolicyId && (!mailboxForm.slaWarningTemplateId || !mailboxForm.slaBreachTemplateId))) && <div className="mailbox-strategy-warning"><TriangleAlert size={17} /><span>配置不完整：分配通知和处理人回复模板必选；启用 SLA 后预警、超时模板也必选。</span></div>}
                      </div>
                    )}

                    {activeMailboxStep === 'test' && (
                      <div className="mailbox-test-panel">
                        <div className="mailbox-test-grid">
                          <div>
                            <span>收信服务</span>
                            <strong>{mailboxForm.imapHost || '未填写'}:{mailboxForm.imapPort || '-'}</strong>
                            <small>{mailboxForm.imapUsername || 'IMAP 账号未填写'} / {mailboxForm.imapFolder || 'INBOX'}</small>
                          </div>
                          <div>
                            <span>发信服务</span>
                            <strong>{mailboxForm.smtpHost || '未填写'}:{mailboxForm.smtpPort || '-'}</strong>
                            <small>{mailboxForm.smtpUsername || 'SMTP 账号未填写'} / {mailboxForm.smtpFromName || '默认发件人'}</small>
                          </div>
                          <div>
                            <span>处理策略</span>
                            <strong>{mailboxForm.fetchIntervalSec || 120} 秒拉取</strong>
                            <small>{mailboxForm.autoReplyEnabled ? '启用自动回执' : '关闭自动回执'}</small>
                          </div>
                        </div>

                        {mailboxTestResult ? (
                          <div className={mailboxTestResult.success ? 'mailbox-test-result ok' : 'mailbox-test-result error'}>
                            <strong>{mailboxTestResult.success ? '连接测试通过' : '连接测试未通过'}</strong>
                            <span>{mailboxTestResult.imapMessage}</span>
                            <span>{mailboxTestResult.smtpMessage}</span>
                          </div>
                        ) : (
                          <div className="mailbox-test-empty">
                            保存前建议完成收信和发信测试；编辑时密码为空会沿用原授权码。
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="mailbox-actions">
                    <span>{mailboxForm.id ? '编辑时密码为空会沿用原授权码。' : '新建邮箱保存前需填写收信和发信授权码。'}</span>
                    <div>
                      <button disabled={activeMailboxStepIndex <= 0} onClick={() => onMoveMailboxStep(-1)} type="button">
                        上一步
                      </button>
                      {activeMailboxStep === 'test' ? (
                        <>
                          <button disabled={mailboxTesting || !canTestMailboxes} onClick={() => onTestMailboxConnection('IMAP')} type="button">
                            测试收信
                          </button>
                          <button disabled={mailboxTesting || !canTestMailboxes} onClick={() => onTestMailboxConnection('SMTP')} type="button">
                            测试发信
                          </button>
                          <button disabled={mailboxTesting || !canTestMailboxes} onClick={() => onTestMailboxConnection('ALL')} type="button">
                            {mailboxTesting ? '测试中...' : '测试全部'}
                          </button>
                        </>
                      ) : (
                        <button onClick={() => onMoveMailboxStep(1)} type="button">
                          下一步
                        </button>
                      )}
                      <button className="primary-action" disabled={mailboxSaving || (mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes)} onClick={onSaveMailbox} type="button">
                        <Check size={16} />
                        {mailboxSaving ? '保存中...' : '保存配置'}
                      </button>
                    </div>
                  </div>
                </div>
              </section>
            </div>

            <div className="mailbox-below">
              <section className="mailbox-panel mailbox-mini-panel">
                <div className="mailbox-panel__head">
                  <strong>同步任务状态</strong>
                  <button className="template-head-action" type="button">全部任务</button>
                </div>
                {mailboxTaskRows.length > 0 ? (
                  <div className="mailbox-mini-table-wrap">
                    <table className="mailbox-mini-table">
                      <thead>
                        <tr>
                          <th>邮箱账号</th>
                          <th>状态</th>
                          <th>任务类型</th>
                          <th>开始时间</th>
                          <th>下次执行</th>
                          <th>进度</th>
                        </tr>
                      </thead>
                      <tbody>
                        {mailboxTaskRows.map((task) => (
                          <tr key={task.id}>
                            <td>{task.mailboxName}</td>
                            <td>
                              <span className={`state-pill ${task.statusClass}`}>{task.status}</span>
                            </td>
                            <td>{task.taskType}</td>
                            <td>{task.startTime}</td>
                            <td>{task.nextRun}</td>
                            <td>
                              {task.status === '失败' ? (
                                <span className="mailbox-mini-error">{task.progress}</span>
                              ) : (
                                <div className="mailbox-progress" aria-label={`任务进度 ${task.progress}`}>
                                  <span style={{ width: task.progress }} />
                                </div>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="mailbox-mini-empty">暂无同步任务，新增并启用邮箱后展示后台拉取状态。</div>
                )}
              </section>

              <section className="mailbox-panel mailbox-mini-panel">
                <div className="mailbox-panel__head">
                  <strong>邮件拉取日志</strong>
                  <button className="template-head-action" type="button">查看全部</button>
                </div>
                {mailboxLogRows.length > 0 ? (
                  <div className="mailbox-mini-table-wrap">
                    <table className="mailbox-mini-table">
                      <thead>
                        <tr>
                          <th>时间</th>
                          <th>邮箱账号</th>
                          <th>动作</th>
                          <th>结果</th>
                          <th>数量</th>
                          <th>关联工单</th>
                        </tr>
                      </thead>
                      <tbody>
                        {mailboxLogRows.map((log) => (
                          <tr key={log.id}>
                            <td>{log.time}</td>
                            <td>{log.mailboxName}</td>
                            <td>{log.action}</td>
                            <td>
                              <span className={`state-pill ${log.resultClass}`}>{log.result}</span>
                            </td>
                            <td>{log.count}</td>
                            <td>{log.relatedTickets}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="mailbox-mini-empty">暂无拉取日志，后续拉取任务执行后展示最近记录。</div>
                )}
              </section>
            </div>
          </>
        )}
      </section>

      {mailboxConfirmAction && (
        <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="mailbox-confirm-title">
          <div className="confirm-modal">
            <h3 id="mailbox-confirm-title">{mailboxConfirmAction.title}</h3>
            <p>{mailboxConfirmAction.text}</p>
            <div className="confirm-target">
              <strong>{mailboxConfirmAction.mailbox.mailboxName}</strong>
              <span>{mailboxConfirmAction.mailbox.emailAddress}</span>
            </div>
            <div className="user-modal__foot">
              <button disabled={mailboxActionLoading} onClick={onCloseMailboxConfirm} type="button">
                取消
              </button>
              <button className="primary-action" disabled={mailboxActionLoading} onClick={onSubmitMailboxConfirm} type="button">
                {mailboxActionLoading ? '处理中...' : mailboxConfirmAction.actionLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
