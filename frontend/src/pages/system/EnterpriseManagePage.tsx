import { useEffect, useMemo, useState } from 'react'
import {
  Building2,
  CircleAlert,
  CircleCheck,
  CircleOff,
  Contact,
  Edit3,
  FileText,
  Mail,
  MessageCircle,
  Phone,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  TicketCheck,
  Send,
  X,
} from 'lucide-react'
import type {
  Enterprise,
  EnterpriseConfirmAction,
  EnterpriseFormState,
  EnterpriseListResponse,
} from '../../types/enterprise'

type Props = {
  actionLoading: boolean
  canCreate: boolean
  canEnable: boolean
  canRead: boolean
  canUpdate: boolean
  confirmAction: EnterpriseConfirmAction
  data: EnterpriseListResponse | null
  enabledFilter: string
  error: string
  feishuTestMessage: string
  feishuTesting: boolean
  form: EnterpriseFormState
  formOpen: boolean
  keyword: string
  loading: boolean
  page: number
  pageSize: number
  onCloseForm: () => void
  onConfirmActionChange: (action: EnterpriseConfirmAction) => void
  onEnabledFilterChange: (value: string) => void
  onFetch: () => void
  onFormChange: (patch: Partial<EnterpriseFormState>) => void
  onKeywordChange: (value: string) => void
  onOpenCreate: () => void
  onOpenEdit: (enterprise: Enterprise) => void
  onPageChange: (page: number) => void
  onPageSizeChange: (size: number) => void
  onSave: () => void
  onSubmitConfirm: () => void
  onTestFeishuGroup: () => void
  saving: boolean
}

function formatDateTime(value: string | null) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function feishuStatusLabel(status: string | null | undefined) {
  if (status === 'OK') return '测试通过'
  if (status === 'UNTESTED') return '未验证（可选）'
  if (status === 'ERROR') return '测试失败'
  return '未配置'
}

export function EnterpriseManagePage({
  actionLoading,
  canCreate,
  canEnable,
  canRead,
  canUpdate,
  confirmAction,
  data,
  enabledFilter,
  error,
  feishuTestMessage,
  feishuTesting,
  form,
  formOpen,
  keyword,
  loading,
  page,
  pageSize,
  onCloseForm,
  onConfirmActionChange,
  onEnabledFilterChange,
  onFetch,
  onFormChange,
  onKeywordChange,
  onOpenCreate,
  onOpenEdit,
  onPageChange,
  onPageSizeChange,
  onSave,
  onSubmitConfirm,
  onTestFeishuGroup,
  saving,
}: Props) {
  const [selectedEnterpriseId, setSelectedEnterpriseId] = useState<number | null>(null)
  const enterprises = useMemo(() => data?.records ?? [], [data?.records])
  const selectedEnterprise = enterprises.find((enterprise) => enterprise.id === selectedEnterpriseId) ?? null
  const mailboxTotal = enterprises.reduce((total, enterprise) => total + enterprise.mailboxCount, 0)
  const ticketTotal = enterprises.reduce((total, enterprise) => total + enterprise.ticketCount, 0)

  useEffect(() => {
    if (enterprises.length === 0) {
      if (selectedEnterpriseId !== null) setSelectedEnterpriseId(null)
      return
    }
    if (!enterprises.some((enterprise) => enterprise.id === selectedEnterpriseId)) {
      setSelectedEnterpriseId(enterprises[0].id)
    }
  }, [enterprises, selectedEnterpriseId])

  return (
    <section className="app-content enterprise-page" aria-label="企业管理">
      <header className="enterprise-topbar">
        <div className="enterprise-title-block">
          <h2>企业管理</h2>
          <span>维护企业档案，统一承载邮箱、工单和数据权限归属</span>
        </div>
        <div className="enterprise-top-actions">
          <button disabled={loading} onClick={onFetch} type="button">
            <RefreshCw className={loading ? 'is-spinning' : ''} size={15} />
            刷新
          </button>
          <button className="primary-action" disabled={!canCreate} onClick={onOpenCreate} type="button">
            <Plus size={15} />
            新建企业
          </button>
        </div>
      </header>

      {!canRead ? (
        <div className="permission-state">
          <ShieldCheck size={42} />
          <strong>无企业管理权限</strong>
          <p>请联系管理员开通企业配置查看权限。</p>
        </div>
      ) : (
        <>
          <section className="enterprise-summary-strip" aria-label="企业统计">
            <div className="enterprise-summary-item active">
              <span className="enterprise-summary-icon"><Building2 size={17} /></span>
              <span className="enterprise-summary-copy"><strong>企业总数</strong><small>当前已纳入管理</small></span>
              <b>{data?.totalCount ?? '--'}</b>
            </div>
            <div className="enterprise-summary-item">
              <span className="enterprise-summary-icon success"><CircleCheck size={17} /></span>
              <span className="enterprise-summary-copy"><strong>启用企业</strong><small>可正常开展业务</small></span>
              <b>{data?.enabledCount ?? '--'}</b>
            </div>
            <div className="enterprise-summary-item">
              <span className="enterprise-summary-icon info"><Mail size={17} /></span>
              <span className="enterprise-summary-copy"><strong>关联邮箱</strong><small>当前结果内邮箱总数</small></span>
              <b>{loading ? '--' : mailboxTotal}</b>
            </div>
            <div className="enterprise-summary-item">
              <span className="enterprise-summary-icon warning"><TicketCheck size={17} /></span>
              <span className="enterprise-summary-copy"><strong>关联工单</strong><small>当前结果内工单总数</small></span>
              <b>{loading ? '--' : ticketTotal}</b>
            </div>
          </section>

          {error && <div className="enterprise-alert">{error}</div>}

          <div className="enterprise-workspace">
            <section className="enterprise-ledger-panel">
              <header className="enterprise-panel-head">
                <div>
                  <strong>企业列表</strong>
                  <span>选择企业后在右侧查看完整档案</span>
                </div>
                <em>{data?.totalCount ?? 0} 家</em>
              </header>

              <div className="enterprise-toolbar">
                <label className="enterprise-search">
                  <Search size={15} />
                  <input
                    onChange={(event) => onKeywordChange(event.target.value)}
                    placeholder="搜索企业、联系人或联系方式"
                    type="search"
                    value={keyword}
                  />
                </label>
                <label className="enterprise-status-filter">
                  <span>状态</span>
                  <select onChange={(event) => onEnabledFilterChange(event.target.value)} value={enabledFilter}>
                    <option value="ALL">全部状态</option>
                    <option value="true">启用</option>
                    <option value="false">停用</option>
                  </select>
                </label>
              </div>

              <div className="enterprise-table-wrap">
                {loading ? (
                  <div className="enterprise-loading" aria-label="企业列表加载中">
                    <span /><span /><span />
                  </div>
                ) : enterprises.length ? (
                  <table className="enterprise-table">
                    <thead>
                      <tr><th>企业</th><th>联系人</th><th>业务规模</th><th>状态</th><th>最近更新</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                      {enterprises.map((enterprise) => (
                        <tr className={selectedEnterpriseId === enterprise.id ? 'selected' : ''} key={enterprise.id}>
                          <td>
                            <button className="enterprise-name-cell" onClick={() => setSelectedEnterpriseId(enterprise.id)} type="button">
                              <span className="enterprise-logo"><Building2 size={16} /></span>
                              <span><strong>{enterprise.enterpriseName}</strong><small>{enterprise.remark || '暂无备注'}</small></span>
                            </button>
                          </td>
                          <td><strong>{enterprise.contactName || '未填写'}</strong><small>{enterprise.contactEmail || enterprise.contactPhone || '暂无联系方式'}</small></td>
                          <td><strong>{enterprise.mailboxCount} 个邮箱</strong><small>{enterprise.ticketCount} 个工单</small></td>
                          <td><span className={`enterprise-state ${enterprise.enabled ? 'enabled' : 'disabled'}`}><i />{enterprise.enabled ? '启用' : '停用'}</span></td>
                          <td>{formatDateTime(enterprise.updatedAt)}</td>
                          <td className="enterprise-row-actions-cell">
                            <div className="enterprise-row-actions">
                              <button
                                disabled={!canUpdate}
                                onClick={() => {
                                  setSelectedEnterpriseId(enterprise.id)
                                  onOpenEdit(enterprise)
                                }}
                                type="button"
                              >
                                <Edit3 size={13} />
                                编辑
                              </button>
                              <button
                                className={enterprise.enabled ? 'danger' : 'success'}
                                disabled={!canEnable}
                                onClick={() => {
                                  setSelectedEnterpriseId(enterprise.id)
                                  onConfirmActionChange({ enterprise, nextEnabled: !enterprise.enabled })
                                }}
                                type="button"
                              >
                                {enterprise.enabled ? <CircleOff size={13} /> : <CircleCheck size={13} />}
                                {enterprise.enabled ? '停用' : '启用'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <div className="enterprise-empty">
                    <Building2 size={34} />
                    <strong>暂无企业</strong>
                    <p>可调整筛选条件，或创建第一个企业。</p>
                  </div>
                )}
              </div>
              <footer className="enterprise-list-foot enterprise-pagination">
                <span>
                  共 {data?.total ?? 0} 条，每页
                  <select onChange={(event) => onPageSizeChange(Number(event.target.value))} value={pageSize}>
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                    <option value={50}>50</option>
                  </select>
                  条
                </span>
                <div>
                  <button disabled={page <= 1} onClick={() => onPageChange(page - 1)} type="button">上一页</button>
                  <strong>{data?.page ?? page} / {Math.max(data?.pages ?? 1, 1)}</strong>
                  <button
                    disabled={!data || page >= Math.max(data.pages, 1)}
                    onClick={() => onPageChange(page + 1)}
                    type="button"
                  >下一页</button>
                </div>
              </footer>
            </section>

            <aside className="enterprise-detail-panel" aria-label="企业详情">
              {selectedEnterprise ? (
                <>
                  <header className="enterprise-detail-head">
                    <span className="enterprise-detail-logo"><Building2 size={20} /></span>
                    <div><strong>{selectedEnterprise.enterpriseName}</strong><small>企业业务档案</small></div>
                    <span className={`enterprise-state ${selectedEnterprise.enabled ? 'enabled' : 'disabled'}`}><i />{selectedEnterprise.enabled ? '启用' : '停用'}</span>
                  </header>

                  <div className="enterprise-detail-scroll">
                    <section className="enterprise-facts" aria-label="企业业务概览">
                      <div><span>邮箱数量</span><strong>{selectedEnterprise.mailboxCount}</strong><small>已归属企业</small></div>
                      <div><span>工单数量</span><strong>{selectedEnterprise.ticketCount}</strong><small>历史业务数据</small></div>
                      <div><span>创建时间</span><strong>{formatDateTime(selectedEnterprise.createdAt)}</strong></div>
                      <div><span>最近更新</span><strong>{formatDateTime(selectedEnterprise.updatedAt)}</strong></div>
                    </section>

                    <section className="enterprise-detail-section">
                      <h3><Contact size={15} />联系信息</h3>
                      <dl>
                        <div><dt>联系人</dt><dd>{selectedEnterprise.contactName || '未填写'}</dd></div>
                        <div><dt><Mail size={13} />联系邮箱</dt><dd>{selectedEnterprise.contactEmail || '未填写'}</dd></div>
                        <div><dt><Phone size={13} />联系电话</dt><dd>{selectedEnterprise.contactPhone || '未填写'}</dd></div>
                      </dl>
                    </section>

                    <section className="enterprise-detail-section">
                      <h3><FileText size={15} />企业备注</h3>
                      <p>{selectedEnterprise.remark || '暂未填写企业备注。'}</p>
                    </section>

                    <section className="enterprise-detail-section enterprise-feishu-summary">
                      <h3><MessageCircle size={15} />飞书通知群</h3>
                      <dl>
                        <div><dt>通知群</dt><dd>{selectedEnterprise.feishuGroupName || '未配置'}</dd></div>
                        <div><dt>机器人配置</dt><dd>{selectedEnterprise.feishuConfigured ? '已配置' : '未配置'}</dd></div>
                        <div><dt>最近测试</dt><dd>{feishuStatusLabel(selectedEnterprise.feishuConnectionStatus)}</dd></div>
                        <div><dt>通知开关</dt><dd>{selectedEnterprise.feishuNotifyEnabled ? '已启用' : '未启用'}</dd></div>
                      </dl>
                      {selectedEnterprise.feishuLastError && <p className="enterprise-feishu-error">{selectedEnterprise.feishuLastError}</p>}
                    </section>

                    <div className="enterprise-scope-note">
                      <ShieldCheck size={15} />
                      <span>邮箱、工单、策略与用户授权均以该企业作为业务边界。</span>
                    </div>
                  </div>
                </>
              ) : (
                <div className="enterprise-detail-empty">
                  <Building2 size={36} />
                  <strong>选择一个企业</strong>
                  <p>企业的联系信息、业务规模和状态会显示在这里。</p>
                </div>
              )}
            </aside>
          </div>
        </>
      )}

      {formOpen && (
        <div className="p5-modal-backdrop" role="presentation">
          <section className="p5-modal enterprise-modal" role="dialog" aria-modal="true" aria-label={form.id ? '编辑企业' : '新建企业'}>
            <header className="enterprise-modal__head">
              <span className="enterprise-modal__mark"><Building2 size={19} /></span>
              <div><h3>{form.id ? '编辑企业' : '新建企业'}</h3><p>统一维护企业档案、业务状态与通知渠道</p></div>
              <button aria-label="关闭" onClick={onCloseForm} type="button"><X size={18} /></button>
            </header>
            <div className="enterprise-modal__body">
              <section className="enterprise-form-card">
                <div className="enterprise-form-card__head">
                  <span><Contact size={16} /></span>
                  <div><strong>基础资料</strong><small>用于企业识别、业务联系和后台检索</small></div>
                </div>
                <div className="enterprise-profile-grid">
                  <label className="enterprise-field enterprise-field--name"><span>企业名称 <em>*</em></span><input autoFocus maxLength={128} onChange={(event) => onFormChange({ enterpriseName: event.target.value })} placeholder="请输入企业名称" value={form.enterpriseName} /></label>
                  <label className={`enterprise-status-card ${form.enabled ? 'is-enabled' : 'is-disabled'}`}>
                    <input checked={form.enabled} onChange={(event) => onFormChange({ enabled: event.target.checked })} type="checkbox" />
                    <span><strong>{form.enabled ? '企业启用' : '企业停用'}</strong><small>{form.enabled ? '允许开展新业务' : '仅保留历史数据'}</small></span>
                  </label>
                  <label className="enterprise-field"><span>联系人</span><input maxLength={64} onChange={(event) => onFormChange({ contactName: event.target.value })} placeholder="请输入联系人姓名" value={form.contactName} /></label>
                  <label className="enterprise-field"><span>联系电话</span><input maxLength={32} onChange={(event) => onFormChange({ contactPhone: event.target.value })} placeholder="请输入联系电话" value={form.contactPhone} /></label>
                  <label className="enterprise-field"><span>联系邮箱</span><input maxLength={128} onChange={(event) => onFormChange({ contactEmail: event.target.value })} placeholder="name@example.com" type="email" value={form.contactEmail} /></label>
                  <label className="enterprise-field enterprise-field--remark"><span>企业备注</span><textarea maxLength={512} onChange={(event) => onFormChange({ remark: event.target.value })} placeholder="补充业务范围、服务约定等说明" rows={3} value={form.remark} /></label>
                </div>
              </section>

              <section className="enterprise-form-card enterprise-feishu-editor">
                <div className="enterprise-feishu-editor__head">
                  <div><span className="enterprise-form-card__icon"><MessageCircle size={16} /></span><span><strong>飞书通知群</strong><small>分配通知与 SLA 提醒统一发送到该企业群</small></span></div>
                  <em>{form.id && selectedEnterprise ? feishuStatusLabel(selectedEnterprise.feishuConnectionStatus) : '未保存'}</em>
                </div>
                <div className="enterprise-feishu-grid">
                  <label className="enterprise-field"><span>通知群名称</span><input disabled={form.clearFeishuConfig} maxLength={128} onChange={(event) => onFormChange({ feishuGroupName: event.target.value })} placeholder="例如：A 企业工单通知群" value={form.feishuGroupName} /></label>
                  <label className="enterprise-field enterprise-field--webhook"><span>机器人 Webhook</span><input disabled={form.clearFeishuConfig} maxLength={1024} onChange={(event) => onFormChange({ feishuWebhookUrl: event.target.value })} placeholder={selectedEnterprise?.feishuConfigured ? '已配置，留空保持原值' : 'https://open.feishu.cn/open-apis/bot/v2/hook/...'} value={form.feishuWebhookUrl} /></label>
                  <label className="enterprise-field"><span>签名密钥</span><input autoComplete="new-password" disabled={form.clearFeishuConfig} maxLength={512} onChange={(event) => onFormChange({ feishuSigningSecret: event.target.value })} placeholder={selectedEnterprise?.feishuConfigured ? '已配置，留空保持原值' : '请输入群机器人签名密钥'} type="password" value={form.feishuSigningSecret} /></label>
                  <label className={`enterprise-status-card enterprise-feishu-switch ${form.feishuNotifyEnabled ? 'is-enabled' : 'is-disabled'}`}>
                    <input checked={form.feishuNotifyEnabled} disabled={form.clearFeishuConfig} onChange={(event) => onFormChange({ feishuNotifyEnabled: event.target.checked })} type="checkbox" />
                    <span><strong>{form.feishuNotifyEnabled ? '群通知已启用' : '群通知未启用'}</strong><small>{form.feishuNotifyEnabled ? '分配与 SLA 消息将进入群聊' : '保存配置但暂不发送消息'}</small></span>
                  </label>
                </div>
                {form.id && selectedEnterprise?.feishuConfigured && (
                  <div className="enterprise-feishu-test">
                    <span><strong>连接测试</strong><small>使用已保存配置向当前通知群发送一条普通消息，不会 @ 群成员。</small></span>
                    <button disabled={feishuTesting} onClick={onTestFeishuGroup} type="button"><Send size={14} />{feishuTesting ? '发送中...' : '发送测试消息'}</button>
                  </div>
                )}
                {form.id && selectedEnterprise?.feishuConfigured && <small className="enterprise-feishu-help">可选：测试使用数据库中已保存的机器人配置；修改 Webhook 或密钥后请先保存再测试。</small>}
                {feishuTestMessage && <p className="enterprise-feishu-message">{feishuTestMessage}</p>}
                {form.id && selectedEnterprise?.feishuConfigured && <label className="enterprise-feishu-clear"><input checked={form.clearFeishuConfig} onChange={(event) => onFormChange({ clearFeishuConfig: event.target.checked, feishuNotifyEnabled: event.target.checked ? false : form.feishuNotifyEnabled })} type="checkbox" /><span>清除该企业的飞书群机器人配置</span></label>}
              </section>
            </div>
            <footer className="enterprise-modal__footer">
              <span><ShieldCheck size={14} />保存仅影响后续业务，历史工单数据保持不变</span>
              <div><button onClick={onCloseForm} type="button">取消</button><button className="primary-action" disabled={saving} onClick={onSave} type="button">{saving ? '保存中...' : '保存企业'}</button></div>
            </footer>
          </section>
        </div>
      )}

      {confirmAction && (
        <div className="p5-modal-backdrop" role="presentation">
          <section className="p5-confirm enterprise-confirm" role="dialog" aria-modal="true">
            <CircleAlert size={34} />
            <h3>{confirmAction.nextEnabled ? '启用企业' : '停用企业'}</h3>
            <p>{confirmAction.nextEnabled ? '启用后，企业下已启用邮箱可继续执行新业务。' : '停用后不强制停用邮箱，但将禁止拉信、建单、发信等新业务；历史数据仍可查看。'}</p>
            <footer><button onClick={() => onConfirmActionChange(null)} type="button">取消</button><button className={confirmAction.nextEnabled ? 'primary-action' : 'danger-action'} disabled={actionLoading} onClick={onSubmitConfirm} type="button">{actionLoading ? '处理中...' : '确认'}</button></footer>
          </section>
        </div>
      )}
    </section>
  )
}
