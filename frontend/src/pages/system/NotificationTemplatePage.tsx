import { useRef } from 'react'
import { Bell, Check, Plus, RefreshCw, Search, ShieldCheck } from 'lucide-react'
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
  templatesData,
  templatesError,
  templatesLoading,
}: NotificationTemplatePageProps) {
  const templateContentRef = useRef<HTMLTextAreaElement>(null)

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
        <div className="content-title">
          <div>
            <h1>通知模板</h1>
            <p>维护自动回执、分配通知、处理人回复和 SLA 提醒模板；支持变量插入、预览和保存。</p>
          </div>
          <div className="content-actions">
            <button disabled={templatesLoading} onClick={onFetchTemplates} type="button">
              <RefreshCw size={16} />
              刷新
            </button>
          </div>
        </div>

        {!canReadTemplates ? (
          <div className="permission-state">
            <ShieldCheck size={42} />
            <strong>无通知模板管理权限</strong>
            <p>当前账号没有通知模板查看权限；模板编辑、预览由独立权限控制。</p>
          </div>
        ) : (
          <>
            <div className="user-metrics">
              <div className="user-metric">
                <span>模板总数</span>
                <strong>{templatesData?.summary.totalTemplates ?? '--'}</strong>
                <small>按业务场景唯一编码</small>
              </div>
              <div className="user-metric">
                <span>启用模板</span>
                <strong>{templatesData?.summary.enabledTemplates ?? '--'}</strong>
                <small>可参与自动通知</small>
              </div>
              <div className="user-metric">
                <span>停用模板</span>
                <strong>{templatesData?.summary.disabledTemplates ?? '--'}</strong>
                <small>保留配置但不发送</small>
              </div>
              <div className="user-metric">
                <span>可用变量</span>
                <strong>{templatesData?.summary.availableVariables ?? '--'}</strong>
                <small>工单、客户、处理人信息</small>
              </div>
            </div>

            {templatesError && <div className="user-alert">{templatesError}</div>}

            <div className="template-layout">
              <aside className="template-panel template-list-panel">
                <div className="template-panel__head">
                  <strong>模板列表</strong>
                  <button className="template-head-action primary" onClick={onOpenCreateTemplate} type="button">
                    <Plus size={14} />
                    新建
                  </button>
                </div>
                <label className="template-search">
                  <Search size={15} />
                  <input
                    onChange={(event) => onTemplateKeywordChange(event.target.value)}
                    placeholder="搜索模板名称或编码"
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
                        onClick={() => onSelectTemplate(template)}
                        type="button"
                      >
                        <span className="template-item__top">
                          <strong>{template.templateName}</strong>
                          <i className={template.enabled ? 'state-pill enabled' : 'state-pill disabled'}>
                            {template.enabled ? '启用' : '停用'}
                          </i>
                        </span>
                        <code>{template.templateCode}</code>
                        <small>{templateSceneLabel(template.templateCode)}</small>
                      </button>
                    ))}
                  </div>
                ) : (
                  <div className="empty-state compact">
                    <Bell size={34} />
                    <strong>未找到模板</strong>
                    <p>可清空搜索后重新查询。</p>
                    <button onClick={onClearKeyword} type="button">清空搜索</button>
                  </div>
                )}
              </aside>

              <section className="template-panel template-editor-panel">
                <div className="template-panel__head">
                  <div className="template-editor-title">
                    <strong>{templateForm.id ? '模板编辑' : '新建模板'}</strong>
                    <span className={templateDirty ? 'template-code-pill dirty' : 'template-code-pill'}>
                      {templateDirty ? '未保存' : '已保存'}
                    </span>
                  </div>
                  <button
                    className="template-head-action primary"
                    disabled={!templateDirty || templateSaving}
                    onClick={onRequestSave}
                    type="button"
                  >
                    <Check size={14} />
                    {templateForm.id ? '保存' : '创建'}
                  </button>
                </div>
                <div className="template-editor">
                  <div className="template-form-grid">
                    <label>
                      <span>模板编码</span>
                      <input
                        disabled={Boolean(templateForm.id)}
                        onChange={(event) =>
                          onUpdateTemplateForm({ templateCode: event.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })
                        }
                        placeholder="例如 CUSTOM_NOTICE"
                        value={templateForm.templateCode}
                      />
                      <small>编码唯一；新建时可填写，保存后不可修改。</small>
                    </label>
                    <label>
                      <span>模板名称</span>
                      <input
                        onChange={(event) => onUpdateTemplateForm({ templateName: event.target.value })}
                        value={templateForm.templateName}
                      />
                    </label>
                    <label>
                      <span>发送场景</span>
                      <input disabled value={templateSceneLabel(templateForm.templateCode)} />
                    </label>
                    <label>
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
                    <label className="full">
                      <span>邮件主题</span>
                      <input
                        onChange={(event) => onUpdateTemplateForm({ subjectTpl: event.target.value })}
                        value={templateForm.subjectTpl}
                      />
                      <small>主题可插入变量，保存前需校验变量格式。</small>
                    </label>
                    <label className="full">
                      <span>邮件正文</span>
                      <div className="template-toolbar">
                        <button type="button">B</button>
                        <button type="button">I</button>
                        <button type="button">列表</button>
                        <button type="button">链接</button>
                        <button type="button">撤销</button>
                        <button type="button">重做</button>
                      </div>
                      <textarea
                        onChange={(event) => onUpdateTemplateForm({ contentTpl: event.target.value })}
                        ref={templateContentRef}
                        value={templateForm.contentTpl}
                      />
                      <small>第一版可降级为纯文本编辑；工具栏保留交互位，保存时写入正文模板。</small>
                    </label>
                  </div>
                </div>
              </section>

              <aside className="template-side">
                <section className="template-panel">
                  <div className="template-panel__head">
                    <strong>变量面板</strong>
                    <span className="template-code-pill">点击插入</span>
                  </div>
                  <div className="template-vars">
                    {(templatesData?.variables || []).map((variable) => (
                      <button key={variable.key} onClick={() => insertVariable(variable.key)} type="button">
                        <code>{variable.key}</code>
                        <span>{variable.label}</span>
                        <small>示例：{variable.sampleValue}</small>
                      </button>
                    ))}
                  </div>
                </section>

                <section className="template-panel">
                  <div className="template-panel__head">
                    <strong>预览</strong>
                    <button disabled={templatePreviewLoading} onClick={onPreviewTemplate} type="button">
                      {templatePreviewLoading ? '生成中...' : '生成预览'}
                    </button>
                  </div>
                  <div className="template-preview">
                    <div className="mail-subject">
                      {templatePreview?.subject || '点击生成预览后显示邮件主题'}
                    </div>
                    <div className="mail-body">
                      {templatePreview?.content || '模板正文预览会使用系统默认示例数据进行变量替换。'}
                    </div>
                  </div>
                </section>
              </aside>
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
              <span>{templateForm.templateCode || '未选择模板'}</span>
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
