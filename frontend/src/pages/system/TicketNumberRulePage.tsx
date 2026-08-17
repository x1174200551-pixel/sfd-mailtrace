import { Check, RefreshCw, ShieldCheck, TriangleAlert } from 'lucide-react'
import type { SystemGroup, SystemGroupKey, TicketNumberRule, TicketRuleFormState } from '../../types/system-config'

type TicketNumberRulePageProps = {
  activeSystemGroup: SystemGroupKey
  activeSystemGroupConfig: SystemGroup
  canReadTicketNumberRule: boolean
  canUpdateTicketNumberRule: boolean
  onActiveSystemGroupChange: (key: SystemGroupKey) => void
  onFetchTicketRule: () => void
  onPreviewTicketRule: () => void
  onRequestSave: () => void
  onResetTicketRule: () => void
  onUpdateTicketRuleForm: (patch: Partial<TicketRuleFormState>) => void
  systemGroups: SystemGroup[]
  ticketRule: TicketNumberRule | null
  ticketRuleDirty: boolean
  ticketRuleError: string
  ticketRuleForm: TicketRuleFormState
  ticketRuleLoading: boolean
  ticketRuleMessage: string
  ticketRulePreviewLoading: boolean
  ticketRuleSaving: boolean
}

export function TicketNumberRulePage({
  activeSystemGroup,
  activeSystemGroupConfig,
  canReadTicketNumberRule,
  canUpdateTicketNumberRule,
  onActiveSystemGroupChange,
  onFetchTicketRule,
  onPreviewTicketRule,
  onRequestSave,
  onResetTicketRule,
  onUpdateTicketRuleForm,
  systemGroups,
  ticketRule,
  ticketRuleDirty,
  ticketRuleError,
  ticketRuleForm,
  ticketRuleLoading,
  ticketRuleMessage,
  ticketRulePreviewLoading,
  ticketRuleSaving,
}: TicketNumberRulePageProps) {
  return (
    <section className="app-content system-page" aria-label="编号规则配置">
      <div className="content-title">
        <div>
          <h1>编号规则配置</h1>
          <p>维护业务可理解的工单号生成规则；系统技术参数由管理员或运维在后台维护。</p>
        </div>
        <div className="content-actions">
          <button disabled={ticketRuleLoading} onClick={onFetchTicketRule} type="button">
            <RefreshCw size={16} />
            刷新
          </button>
          <button
            className="primary-action"
            disabled={activeSystemGroup !== 'ticket' || !canUpdateTicketNumberRule || !ticketRuleDirty || ticketRuleSaving}
            onClick={onRequestSave}
            type="button"
          >
            <Check size={16} />
            保存修改
          </button>
        </div>
      </div>

      {!canReadTicketNumberRule ? (
        <div className="permission-state">
          <ShieldCheck size={42} />
          <strong>无编号规则管理权限</strong>
          <p>当前账号没有编号规则查看权限；保存修改由独立权限控制。</p>
        </div>
      ) : (
        <>
          <div className="user-metrics">
            <div className="user-metric">
              <span>规则数量</span>
              <strong>{ticketRule ? 1 : '--'}</strong>
              <small>当前启用工单编号规则</small>
            </div>
            <div className="user-metric">
              <span>编号规则</span>
              <strong>{ticketRuleForm.enabled ? 1 : 0}</strong>
              <small>当前前缀 {ticketRuleForm.prefix || '--'}</small>
            </div>
            <div className="user-metric">
              <span>未保存变更</span>
              <strong>{ticketRuleDirty ? 1 : 0}</strong>
              <small>{ticketRuleDirty ? '规则待确认' : '暂无待保存内容'}</small>
            </div>
            <div className="user-metric">
              <span>当前流水</span>
              <strong>{ticketRule?.usedSeq ?? '--'}</strong>
              <small>下一号 {ticketRule?.nextSeq ?? '--'}</small>
            </div>
          </div>

          <div className={ticketRuleError ? 'system-alert danger' : 'system-alert'}>
            <span>
              {ticketRuleError ||
                ticketRuleMessage ||
                (activeSystemGroup === 'ticket'
                  ? '编号规则影响后续新建工单，历史工单号不会回写变更。'
                  : `${activeSystemGroupConfig.title}当前不开放业务编辑，仅展示维护边界。`)}
            </span>
            <button
              onClick={onPreviewTicketRule}
              disabled={activeSystemGroup !== 'ticket' || ticketRulePreviewLoading}
              type="button"
            >
              {ticketRulePreviewLoading ? '生成中...' : '生成预览'}
            </button>
          </div>

          <div className="system-layout">
            <aside className="system-panel system-groups">
              <div className="system-panel__head">
                <strong>配置分组</strong>
                <span className="template-code-pill">业务可配</span>
              </div>
              {systemGroups.map((group) => (
                <button
                  aria-pressed={activeSystemGroup === group.key}
                  className={activeSystemGroup === group.key ? 'system-group active' : 'system-group'}
                  key={group.key}
                  onClick={() => onActiveSystemGroupChange(group.key)}
                  type="button"
                >
                  <strong>{group.title}</strong>
                  <small>{group.summary}</small>
                </button>
              ))}
            </aside>

            <section className="system-panel system-editor">
              <div className="system-panel__head">
                <strong>{activeSystemGroupConfig.title}</strong>
                <span className={ticketRuleDirty && activeSystemGroup === 'ticket' ? 'template-code-pill dirty' : 'template-code-pill'}>
                  {activeSystemGroup === 'ticket' ? (ticketRuleDirty ? '未保存' : '已保存') : activeSystemGroupConfig.owner}
                </span>
              </div>
              {activeSystemGroup !== 'ticket' ? (
                <div className="system-readonly">
                  <ShieldCheck size={38} />
                  <strong>{activeSystemGroupConfig.title}不在当前页编辑</strong>
                  <p>{activeSystemGroupConfig.detail}</p>
                  <div className="readonly-facts">
                    <span>当前状态</span>
                    <strong>{activeSystemGroupConfig.owner}</strong>
                  </div>
                </div>
              ) : ticketRuleLoading ? (
                <div className="user-loading">
                  {[0, 1, 2, 3].map((item) => (
                    <span key={item} />
                  ))}
                </div>
              ) : (
                <div className="system-form">
                  <label>
                    <span>启用状态</span>
                    <select
                      onChange={(event) => onUpdateTicketRuleForm({ enabled: event.target.value === 'true' })}
                      value={String(ticketRuleForm.enabled)}
                    >
                      <option value="true">启用</option>
                      <option value="false">停用</option>
                    </select>
                    <small>停用后使用默认规则 TCK-yyyyMMdd-0001。</small>
                  </label>
                  <label>
                    <span>工单前缀</span>
                    <input
                      maxLength={8}
                      onChange={(event) =>
                        onUpdateTicketRuleForm({
                          prefix: event.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''),
                        })
                      }
                      value={ticketRuleForm.prefix}
                    />
                    <small>建议使用 2-8 位大写英文或数字。</small>
                  </label>
                  <label>
                    <span>日期格式</span>
                    <select
                      onChange={(event) => onUpdateTicketRuleForm({ dateFormat: event.target.value })}
                      value={ticketRuleForm.dateFormat}
                    >
                      <option value="yyyyMMdd">yyyyMMdd</option>
                      <option value="yyyyMM">yyyyMM</option>
                      <option value="yyyy">yyyy</option>
                    </select>
                    <small>变更后新工单按新日期维度取流水。</small>
                  </label>
                  <label>
                    <span>流水位数</span>
                    <input
                      max={8}
                      min={3}
                      onChange={(event) =>
                        onUpdateTicketRuleForm({ seqLength: Number(event.target.value || 4) })
                      }
                      type="number"
                      value={ticketRuleForm.seqLength}
                    />
                    <small>示例：4 位生成 0001，6 位生成 000001。</small>
                  </label>
                  <label>
                    <span>分隔符</span>
                    <select
                      onChange={(event) => onUpdateTicketRuleForm({ separator: event.target.value })}
                      value={ticketRuleForm.separator}
                    >
                      <option value="-">短横线 -</option>
                      <option value="">无分隔符</option>
                      <option value="_">下划线 _</option>
                    </select>
                    <small>建议保留短横线，便于邮件主题识别。</small>
                  </label>
                  <label className="full">
                    <span>参数说明</span>
                    <textarea
                      onChange={(event) => onUpdateTicketRuleForm({ description: event.target.value })}
                      value={ticketRuleForm.description}
                    />
                  </label>
                  <div className="system-token-row">
                    <span>{'{prefix}'}</span>
                    <span>{`{${ticketRuleForm.dateFormat}}`}</span>
                    <span>{'{seq}'}</span>
                    <span>{ticketRuleForm.separator || '无分隔符'}</span>
                  </div>
                  <div className="system-actions">
                    <span>保存前会校验格式合法性和下一号预览。</span>
                    <div>
                      <button onClick={onResetTicketRule} type="button">恢复默认</button>
                      <button
                        className="primary-action"
                        disabled={activeSystemGroup !== 'ticket'}
                        onClick={onRequestSave}
                        type="button"
                      >
                        保存规则
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </section>

            <aside className="system-side">
              {activeSystemGroup === 'ticket' ? (
                <>
                  <section className="system-panel">
                    <div className="system-panel__head">
                      <strong>规则预览</strong>
                      <span className="state-pill enabled">可用</span>
                    </div>
                    <div className="rule-preview-card">
                      <span>下一工单号</span>
                      <strong>{ticketRule?.nextTicketNo || '点击生成预览'}</strong>
                    </div>
                    <div className="rule-preview-list">
                      <div><span>今日日期</span><strong>{ticketRule?.todayDate || '--'}</strong></div>
                      <div><span>当前日期维度</span><strong>{ticketRule?.dateKey || '--'}</strong></div>
                      <div><span>当前已用流水</span><strong>{ticketRule?.usedSeq ?? '--'}</strong></div>
                      <div><span>主题匹配样例</span><strong>{ticketRule?.subjectPreview || '--'}</strong></div>
                    </div>
                  </section>

                  <section className="system-panel">
                    <div className="system-panel__head">
                      <strong>发布检查</strong>
                      <span className="template-code-pill">自动校验</span>
                    </div>
                    <div className="system-check-list">
                      <div><Check size={16} /><span><strong>规则格式合法</strong><small>前缀、日期和流水片段均可解析。</small></span></div>
                      <div><Check size={16} /><span><strong>下一号可预览</strong><small>预览编号按当前日期维度生成。</small></span></div>
                      <div><TriangleAlert size={16} /><span><strong>影响新工单</strong><small>保存后仅影响后续自动建单。</small></span></div>
                    </div>
                  </section>
                </>
              ) : (
                <section className="system-panel">
                  <div className="system-panel__head">
                    <strong>分组说明</strong>
                    <span className="template-code-pill">{activeSystemGroupConfig.owner}</span>
                  </div>
                  <div className="system-readonly side">
                    <ShieldCheck size={34} />
                    <strong>{activeSystemGroupConfig.title}</strong>
                    <p>{activeSystemGroupConfig.detail}</p>
                  </div>
                </section>
              )}
            </aside>
          </div>
        </>
      )}
    </section>
  )
}
