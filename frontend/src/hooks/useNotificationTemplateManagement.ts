import { useCallback, useEffect, useState } from 'react'
import { notificationTemplateApi } from '../api/notification-templates'
import { emptyTemplateForm } from '../constants/notification-templates'
import type {
  NotificationTemplate,
  NotificationTemplateListResponse,
  TemplateFormState,
  TemplatePreviewResponse,
} from '../types/notification-template'

type UseNotificationTemplateManagementParams = {
  activeMenu: string
  canReadTemplates: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

function toTemplateForm(template: NotificationTemplate): TemplateFormState {
  return {
    id: template.id,
    templateCode: template.templateCode,
    templateName: template.templateName,
    subjectTpl: template.subjectTpl,
    contentTpl: template.contentTpl,
    enabled: template.enabled,
  }
}

export function useNotificationTemplateManagement({
  activeMenu,
  canReadTemplates,
  handleAuthExpired,
  token,
}: UseNotificationTemplateManagementParams) {
  const [templateKeyword, setTemplateKeyword] = useState('')
  const [templatesData, setTemplatesData] = useState<NotificationTemplateListResponse | null>(null)
  const [templatesLoading, setTemplatesLoading] = useState(false)
  const [templatesError, setTemplatesError] = useState('')
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null)
  const [templateForm, setTemplateForm] = useState<TemplateFormState>(emptyTemplateForm)
  const [templateDraftMode, setTemplateDraftMode] = useState(false)
  const [templateDirty, setTemplateDirty] = useState(false)
  const [templateSaving, setTemplateSaving] = useState(false)
  const [templateConfirmOpen, setTemplateConfirmOpen] = useState(false)
  const [templatePreview, setTemplatePreview] = useState<TemplatePreviewResponse | null>(null)
  const [templatePreviewLoading, setTemplatePreviewLoading] = useState(false)

  const fetchTemplates = useCallback(async () => {
    if (!token || activeMenu !== '通知模板') return
    if (!canReadTemplates) {
      setTemplatesData(null)
      setTemplatesError('当前账号没有通知模板管理权限')
      return
    }

    setTemplatesLoading(true)
    setTemplatesError('')
    try {
      const data = await notificationTemplateApi.list({ keyword: templateKeyword.trim() || undefined })
      setTemplatesData(data)
      if (templateDraftMode) {
        return
      }
      const selected = data.records.find((template) => template.id === selectedTemplateId) || data.records[0] || null
      if (selected) {
        setSelectedTemplateId(selected.id)
        setTemplateForm(toTemplateForm(selected))
        setTemplateDirty(false)
        setTemplatePreview(null)
      } else {
        setSelectedTemplateId(null)
        setTemplateForm(emptyTemplateForm)
        setTemplateDirty(false)
        setTemplatePreview(null)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTemplatesError(error instanceof Error ? error.message : '模板列表加载失败')
    } finally {
      setTemplatesLoading(false)
    }
  }, [activeMenu, canReadTemplates, handleAuthExpired, selectedTemplateId, templateDraftMode, templateKeyword, token])

  useEffect(() => {
    void fetchTemplates()
  }, [fetchTemplates])

  const selectTemplate = useCallback((template: NotificationTemplate) => {
    setTemplateDraftMode(false)
    setSelectedTemplateId(template.id)
    setTemplateForm(toTemplateForm(template))
    setTemplateDirty(false)
    setTemplatePreview(null)
    setTemplatesError('')
  }, [])

  const openCreateTemplate = useCallback(() => {
    setTemplateDraftMode(true)
    setSelectedTemplateId(null)
    setTemplateKeyword('')
    setTemplateForm({
      id: null,
      templateCode: '',
      templateName: '自定义通知模板',
      subjectTpl: '通知：{ticket_no}',
      contentTpl: '您好，工单 {ticket_no} 有新的通知。\n\n工单主题：{subject}',
      enabled: true,
    })
    setTemplateDirty(true)
    setTemplatePreview(null)
    setTemplatesError('')
  }, [])

  const updateTemplateForm = useCallback((patch: Partial<TemplateFormState>) => {
    setTemplateForm((value) => ({ ...value, ...patch }))
    setTemplateDirty(true)
    setTemplatePreview(null)
  }, [])

  const previewTemplate = useCallback(async () => {
    if (!token) return
    setTemplatePreviewLoading(true)
    setTemplatesError('')
    try {
      const data = await notificationTemplateApi.preview({
        contentTpl: templateForm.contentTpl,
        subjectTpl: templateForm.subjectTpl,
      })
      setTemplatePreview(data)
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTemplatesError(error instanceof Error ? error.message : '预览生成失败')
    } finally {
      setTemplatePreviewLoading(false)
    }
  }, [handleAuthExpired, templateForm.contentTpl, templateForm.subjectTpl, token])

  const saveTemplate = useCallback(async () => {
    if (!token) return
    setTemplateSaving(true)
    setTemplatesError('')
    try {
      const saved = await notificationTemplateApi.save(templateForm.id, {
        contentTpl: templateForm.contentTpl,
        enabled: templateForm.enabled,
        subjectTpl: templateForm.subjectTpl,
        templateCode: templateForm.templateCode,
        templateName: templateForm.templateName,
      })
      setSelectedTemplateId(saved.id)
      setTemplateDraftMode(false)
      setTemplateForm(toTemplateForm(saved))
      setTemplateDirty(false)
      setTemplateConfirmOpen(false)
      await fetchTemplates()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setTemplatesError(error instanceof Error ? error.message : '模板保存失败')
    } finally {
      setTemplateSaving(false)
    }
  }, [fetchTemplates, handleAuthExpired, templateForm, token])

  return {
    fetchTemplates,
    openCreateTemplate,
    previewTemplate,
    saveTemplate,
    selectTemplate,
    selectedTemplateId,
    setTemplateConfirmOpen,
    setTemplateKeyword,
    templateConfirmOpen,
    templateDirty,
    templateForm,
    templateKeyword,
    templatePreview,
    templatePreviewLoading,
    templateSaving,
    templatesData,
    templatesError,
    templatesLoading,
    updateTemplateForm,
  }
}
