import { useRef, useState } from 'react'
import {
  Bell,
  Braces,
  Check,
  CircleOff,
  FileText,
  MailCheck,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
} from 'lucide-react'
import { templateSceneLabel } from '../../constants/notification-templates'
import type {
  NotificationTemplate,
  NotificationTemplateListResponse,
  TemplateFormState,
  TemplatePreviewResponse,
} from '../../types/notification-template'

type NotificationTemplatePageProps = {
  canReadTemplates: boolean
  confirmOpen: boolean
  onCancelConfirm: () => void
  onClearKeyword: () => void
  onFetchTemplates: () => void
  onOpenCreateTemplate: () => void
  onPreviewTemplate: () => void
  onRequestSave: () => void
  onSaveTemplate: () => void
  onSelectTemplate: (template: NotificationTemplate) => void
  onTemplateKeywordChange: (value: string) => void
  onUpdateTemplateForm: (patch: Partial<TemplateFormState>) => void
  selectedTemplateId: number | null
  templateDirty: boolean
  templateForm: TemplateFormState
  templateKeyword: string
  templatePreview: TemplatePreviewResponse | null
  templatePreviewLoading: boolean
  templateSaving: boolean
  templateTypeFilter: string
  onTemplateTypeFilterChange: (value: string) => void
  templatesData: NotificationTemplateListResponse | null
  templatesError: string
  templatesLoading: boolean
}

export function NotificationTemplatePage({
  canReadTemplates,
  confirmOpen,
  onCancelConfirm,
  onClearKeyword,
  onFetchTemplates,
  onOpenCreateTemplate,
  onPreviewTemplate,
  onRequestSave,
  onSaveTemplate,
  onSelectTemplate,
  onTemplateKeywordChange,
  onUpdateTemplateForm,
  selectedTemplateId,
  templateDirty,
  templateForm,
  templateKeyword,
  templatePreview,
  templatePreviewLoading,
  templateSaving,
  templateTypeFilter,
  onTemplateTypeFilterChange,
  templatesData,
  templatesError,
  templatesLoading,
}: NotificationTemplatePageProps) {
  const templateContentRef = useRef<HTMLTextAreaElement>(null)
  const [activeWorkspace, setActiveWorkspace] = useState<'editor' | 'preview'>('editor')
  const templateFormReady = Boolean(
    templateForm.templateCode.trim() &&
    templateForm.templateName.trim() &&
    templateForm.subjectTpl.trim() &&
    templateForm.contentTpl.trim(),
  )

  function openTemplateDraft() {
    setActiveWorkspace('editor')
    onOpenCreateTemplate()
  }

  function selectTemplate(template: NotificationTemplate) {
    setActiveWorkspace('editor')
    onSelectTemplate(template)
  }

  function previewTemplate() {
    setActiveWorkspace('preview')
    onPreviewTemplate()
  }

  function changeScene(scene: string) {
    if (scene === templateTypeFilter) return
    if (templateDirty && !window.confirm('切换场景会放弃当前未保存的模板修改，是否继续？')) return
    setActiveWorkspace('editor')
    onTemplateTypeFilterChange(scene)
  }

  function associationLabel(templateType: string) {
    if (templateType === 'AUTO_REPLY') return '邮箱自动回执场景选择'
    if (templateType === 'ASSIGN_NOTIFY') return '邮箱分配通知场景选择'
    if (templateType === 'AGENT_REPLY') return '邮箱处理人回复场景选择'
    if (templateType === 'SLA_WARNING') return '邮箱 SLA 预警场景选择'
    if (templateType === 'SLA_BREACH') return '邮箱 SLA 超时场景选择'
    return '全局系统场景'
  }

  const scenes = [
    ['ALL', '全部模板', '查看全局模板库'],
    ['AUTO_REPLY', '自动回执', '客户来信自动建单'],
    ['ASSIGN_NOTIFY', '分配通知', '工单分配给处理人'],
    ['AGENT_REPLY', '处理人回复', '处理人对外回复客户'],
    ['SLA_WARNING', 'SLA 预警', '工单即将超时'],
    ['SLA_BREACH', 'SLA 超时', '工单已经超时'],
    ['SYSTEM', '系统通知', '其他平台通知场景'],
  ] as const

  function insertVariable(variableKey: string) {
    const textarea = templateContentRef.current
    if (!textarea) {
      onUpdateTemplateForm({ contentTpl: `${templateForm.contentTpl}${variableKey}` })
      return
    }
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const contentTpl = `${templateForm.contentTpl.slice(0, start)}${variableKey}${templateForm.contentTpl.slice(end)}`
    onUpdateTemplateForm({ contentTpl })
    window.requestAnimationFrame(() => {
      textarea.focus()
      textarea.selectionStart = start + variableKey.length
      textarea.selectionEnd = start + variableKey.length
    })
  }

  return (
    <>
      <section className="app-content template-page" aria-label="通知模板">
        <header className="template-topbar">
          <div className="template-title-block">
            <h2>通知模板</h2>
            <span>全局共享模板库，由各邮箱按发送场景选择使用</span>
          </div>
          <div className="template-top-actions">
            <button disabled={templatesLoading} onClick={onFetchTemplates} type="button">
              <RefreshCw size={16} />
              刷新
            </button>
            <button className="primary-action" onClick={openTemplateDraft} type="button">
              <Plus size={16} />
              新建模板
            </button>
          </div>
        </header>

        {!canReadTemplates ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无通知模板管理权限</strong>
            <p>当前账号没有通知模板查看权限；模板编辑、预览由独立权限控制。</p>
          </div>
        ) : (
          <>
            <div className="user-summary-strip template-summary-strip">
              <div className="user-summary-item active">
                <span className="user-summary-icon">
                  <FileText size={17} />
                </span>
                <span className="user-summary-copy">
                  <span>模板总数</span>
                  <small>全局共享模板库</small>
                </span>
                <strong>{templatesData?.summary.totalTemplates ?? '--'}</strong>
              </div>
              <div className="user-summary-item">
                <span className="user-summary-icon success">
                  <Check size={17} />
                </span>
                <span className="user-summary-copy">
                  <span>启用模板</span>
                  <small>可参与自动通知</small>
                </span>
                <strong>{templatesData?.summary.enabledTemplates ?? '--'}</strong>
              </div>
              <div className="user-summary-item">
                <span className="user-summary-icon warning">
                  <CircleOff size={17} />
                </span>
                <span className="user-summary-copy">
                  <span>停用模板</span>
                  <small>保留配置但不发送</small>
                </span>
                <strong>{templatesData?.summary.disabledTemplates ?? '--'}</strong>
              </div>
              <div className="user-summary-item">
                <span className="user-summary-icon info">
                  <Braces size={17} />
                </span>
                <span className="user-summary-copy">
                  <span>可用变量</span>
                  <small>工单、客户、处理人信息</small>
                </span>
                <strong>{templatesData?.summary.availableVariables ?? '--'}</strong>
              </div>
            </div>

            {templatesError && <div className="user-alert">{templatesError}</div>}

            <div className="template-layout">
              <aside className="template-panel template-enterprise-panel">
                <div className="template-panel__head">
                  <div>
                    <strong>通知场景</strong>
                    <small>按实际用途快速定位</small>
                  </div>
                  <span>6 类</span>
                </div>
                <div className="template-enterprise-list">
                  {scenes.map(([value, label, description]) => (
                    <button
                      className={value === templateTypeFilter ? 'template-enterprise-item active' : 'template-enterprise-item'}
                      key={value}
                      onClick={() => changeScene(value)}
                      type="button"
                    >
                      <span className="template-enterprise-icon"><MailCheck size={16} /></span>
                      <span>
                        <strong>{label}</strong>
                        <small>{description}</small>
                      </span>
                    </button>
                  ))}
                </div>
                <div className="template-enterprise-note">
                  <MailCheck size={17} />
                  <div>
                    <strong>全局共享，邮箱绑定</strong>
                    <span>模板不属于企业；每个邮箱为五个邮件场景分别选择模板。</span>
                  </div>
                </div>
              </aside>

              <aside className="template-panel template-list-panel">
                <div className="template-panel__head">
                  <div>
                    <strong>模板场景</strong>
                    <small>全局模板库</small>
                  </div>
                  <span>{templatesData?.records.length ?? 0} 条</span>
                </div>
                <label className="template-search">
                  <Search size={15} />
                  <input
                    onChange={(event) => onTemplateKeywordChange(event.target.value)}
                    placeholder="搜索模板名称"
                    type="search"
                    value={templateKeyword}
                  />
                </label>
                {templatesLoading ? (
                  <div className="user-loading">
                    {[0, 1, 2, 3, 4].map((item) => (
                      <span key={item} />
                    ))}
                  </div>
                ) : templatesData && templatesData.records.length > 0 ? (
                  <div className="template-list">
                    {templatesData.records.map((template) => (
                      <button
                        className={selectedTemplateId === template.id ? 'template-item active' : 'template-item'}
                        key={template.id}
                        onClick={() => selectTemplate(template)}
                        type="button"
                      >
                        <span className="template-item__top">
                          <strong>{template.templateName}</strong>
                          <i className={template.enabled ? 'state-pill enabled' : 'state-pill disabled'}>
                            {template.enabled ? '启用' : '停用'}
                          </i>
                        </span>
                        <small>{templateSceneLabel(template.templateType)} · {associationLabel(template.templateType)}</small>
                      </button>
                    ))}
                  </div>
                ) : (
                  <div className="empty-state compact">
                    <Bell size={34} />
                    <strong>未找到模板</strong>
                    <p>当前场景下没有符合条件的模板。</p>
                    {templateKeyword && <button onClick={onClearKeyword} type="button">清空搜索</button>}
                  </div>
                )}
              </aside>

              <section className="template-panel template-editor-panel">
                <div className="template-panel__head">
                  <div className="template-editor-title">
                    <div>
                      <strong>{templateForm.id ? '编辑模板' : '新建模板'}</strong>
                      <small>全局共享 · {associationLabel(templateForm.templateType)}</small>
                    </div>
                    <span className={templateDirty ? 'template-code-pill dirty' : 'template-code-pill'}>{templateDirty ? '未保存' : '已保存'}</span>
                  </div>
                  <button
                    className="template-head-action primary"
                    disabled={!templateDirty || !templateFormReady || templateSaving}
                    onClick={onRequestSave}
                    type="button"
                  >
                    <Check size={14} />
                    {templateForm.id ? '保存' : '创建'}
                  </button>
                </div>
                <nav className="template-workspace-tabs" aria-label="模板工作区" role="tablist">
                  <button
                    aria-selected={activeWorkspace === 'editor'}
                    className={activeWorkspace === 'editor' ? 'active' : ''}
                    onClick={() => setActiveWorkspace('editor')}
                    role="tab"
                    type="button"
                  >
                    模板内容
                  </button>
                  <button
                    aria-selected={activeWorkspace === 'preview'}
                    className={activeWorkspace === 'preview' ? 'active' : ''}
                    onClick={() => setActiveWorkspace('preview')}
                    role="tab"
                    type="button"
                  >
                    发送预览
                  </button>
                </nav>

                {activeWorkspace === 'editor' ? (
                  <div className="template-editor">
                    <section className="template-form-section">
                      <div className="template-section-title">
                        <strong>基本信息</strong>
                        <span>只需设置模板名称和使用场景，具体邮箱在邮箱配置页选择</span>
                      </div>
                      <div className="template-form-grid template-basic-grid">
                        <label>
                          <span>模板名称</span>
                          <input
                            onChange={(event) => onUpdateTemplateForm({ templateName: event.target.value })}
                            value={templateForm.templateName}
                          />
                        </label>
                        <label>
                          <span>模板类型</span>
                          <select onChange={(event) => onUpdateTemplateForm({ templateType: event.target.value })} value={templateForm.templateType}>
                            <option value="AUTO_REPLY">自动回执</option><option value="ASSIGN_NOTIFY">分配通知</option><option value="AGENT_REPLY">处理人回复</option><option value="SLA_WARNING">SLA 预警</option><option value="SLA_BREACH">SLA 超时</option><option value="SYSTEM">系统通知</option>
                          </select>
                        </label>
                        <label className="full">
                          <span>启用状态</span>
                          <button
                            className={templateForm.enabled ? 'template-switch enabled' : 'template-switch'}
                            onClick={() => onUpdateTemplateForm({ enabled: !templateForm.enabled })}
                            type="button"
                          >
                            <span>{templateForm.enabled ? '启用后参与自动通知' : '停用后不参与发送'}</span>
                            <i />
                          </button>
                        </label>
                      </div>
                    </section>

                    <section className="template-form-section template-compose-section">
                      <div className="template-section-title">
                        <strong>邮件内容</strong>
                        <span>先写主题，再通过变量快速补充工单上下文</span>
                      </div>
                      <div className="template-form-grid">
                        <label className="full">
                          <span>邮件主题</span>
                          <input
                            onChange={(event) => onUpdateTemplateForm({ subjectTpl: event.target.value })}
                            value={templateForm.subjectTpl}
                          />
                          <small>主题可插入变量，保存前会校验变量格式。</small>
                        </label>
                        <div className="template-variable-dock full">
                          <div>
                            <strong>插入变量</strong>
                            <span>点击后插入到正文光标位置</span>
                          </div>
                          <div className="template-variable-chips">
                            {(templatesData?.variables || []).map((variable) => (
                              <button
                                aria-label={`插入变量：${variable.label}`}
                                key={variable.key}
                                onClick={() => insertVariable(variable.key)}
                                title={`示例：${variable.sampleValue}`}
                                type="button"
                              >
                                <span>{variable.label}</span>
                                <code>{variable.key}</code>
                              </button>
                            ))}
                          </div>
                        </div>
                        <label className="full">
                          <span>邮件正文</span>
                          <textarea
                            onChange={(event) => onUpdateTemplateForm({ contentTpl: event.target.value })}
                            ref={templateContentRef}
                            value={templateForm.contentTpl}
                          />
                          <small>正文支持变量占位符；可随时切换到“发送预览”检查最终效果。</small>
                        </label>
                      </div>
                    </section>
                  </div>
                ) : (
                  <div className="template-preview-workspace">
                    <header>
                      <div>
                        <strong>发送效果预览</strong>
                        <span>使用系统示例数据替换变量，不会实际发送邮件。</span>
                      </div>
                      <button disabled={templatePreviewLoading} onClick={previewTemplate} type="button">
                        {templatePreviewLoading ? '生成中...' : templatePreview ? '重新生成预览' : '生成预览'}
                      </button>
                    </header>
                    <div className="template-preview template-preview-large">
                      <div className="mail-subject">
                        {templatePreview?.subject || '点击“重新生成预览”后显示邮件主题'}
                      </div>
                      <div className="mail-body">
                        {templatePreview?.content || '模板正文预览会使用系统默认示例数据进行变量替换。'}
                      </div>
                    </div>
                  </div>
                )}
              </section>
            </div>
          </>
        )}
      </section>

      {confirmOpen && (
        <div className="modal-mask user-modal-mask" role="dialog" aria-modal="true" aria-labelledby="template-confirm-title">
          <div className="confirm-modal">
            <h3 id="template-confirm-title">{templateForm.id ? '保存模板确认' : '新建模板确认'}</h3>
            <p>
              {templateForm.id
                ? '保存后新产生的自动回执和通知将使用当前模板内容，历史已发送邮件不受影响。'
                : '创建后模板会进入列表，启用状态由当前开关决定。'}
            </p>
            <div className="confirm-target">
              <strong>{templateForm.templateName || '未命名模板'}</strong>
              <span>{templateSceneLabel(templateForm.templateType)} · 全局共享</span>
            </div>
            <div className="user-modal__foot">
              <button disabled={templateSaving} onClick={onCancelConfirm} type="button">取消</button>
              <button className="primary-action" disabled={templateSaving} onClick={onSaveTemplate} type="button">
                {templateSaving ? '保存中...' : templateForm.id ? '确认保存' : '确认创建'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
