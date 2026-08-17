import { useCallback, useEffect, useState } from 'react'
import { mailboxApi } from '../api/mailboxes'
import { emptyMailboxForm, mailboxSteps } from '../constants/mailboxes'
import type {
  Mailbox,
  MailboxConfirmAction,
  MailboxConnectionTestResponse,
  MailboxFormState,
  MailboxPageResponse,
  MailboxStepKey,
} from '../types/mailbox'

type UseMailboxManagementParams = {
  activeMenu: string
  canCreateMailboxes: boolean
  canDeleteMailboxes: boolean
  canEnableMailboxes: boolean
  canReadMailboxes: boolean
  canTestMailboxes: boolean
  canUpdateMailboxes: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

function toMailboxForm(mailbox: Mailbox): MailboxFormState {
  return {
    id: mailbox.id,
    mailboxName: mailbox.mailboxName,
    emailAddress: mailbox.emailAddress,
    enabled: mailbox.enabled,
    defaultAssigneeId: mailbox.defaultAssigneeId == null ? '' : String(mailbox.defaultAssigneeId),
    imapHost: mailbox.imapHost,
    imapPort: mailbox.imapPort,
    imapSslEnabled: mailbox.imapSslEnabled,
    imapUsername: mailbox.imapUsername,
    imapPassword: '',
    imapFolder: mailbox.imapFolder,
    fetchIntervalSec: mailbox.fetchIntervalSec,
    smtpHost: mailbox.smtpHost,
    smtpPort: mailbox.smtpPort,
    smtpSslEnabled: mailbox.smtpSslEnabled,
    smtpUsername: mailbox.smtpUsername,
    smtpPassword: '',
    smtpFromName: mailbox.smtpFromName || '',
    autoReplyEnabled: mailbox.autoReplyEnabled,
    autoReplyTemplateId: mailbox.autoReplyTemplateId == null ? '' : String(mailbox.autoReplyTemplateId),
  }
}

export function useMailboxManagement({
  activeMenu,
  canCreateMailboxes,
  canDeleteMailboxes,
  canEnableMailboxes,
  canReadMailboxes,
  canTestMailboxes,
  canUpdateMailboxes,
  handleAuthExpired,
  token,
}: UseMailboxManagementParams) {
  const [mailboxKeyword, setMailboxKeyword] = useState('')
  const [mailboxStatusFilter, setMailboxStatusFilter] = useState('ALL')
  const [mailboxPage, setMailboxPage] = useState(1)
  const [mailboxPageSize, setMailboxPageSize] = useState(10)
  const [mailboxesData, setMailboxesData] = useState<MailboxPageResponse | null>(null)
  const [mailboxesLoading, setMailboxesLoading] = useState(false)
  const [mailboxesError, setMailboxesError] = useState('')
  const [activeMailboxStep, setActiveMailboxStep] = useState<MailboxStepKey>('basic')
  const [mailboxForm, setMailboxForm] = useState<MailboxFormState>(emptyMailboxForm)
  const [mailboxDirty, setMailboxDirty] = useState(false)
  const [mailboxSaving, setMailboxSaving] = useState(false)
  const [mailboxTesting, setMailboxTesting] = useState(false)
  const [mailboxTestResult, setMailboxTestResult] = useState<MailboxConnectionTestResponse | null>(null)
  const [mailboxConfirmAction, setMailboxConfirmAction] = useState<MailboxConfirmAction>(null)
  const [mailboxActionLoading, setMailboxActionLoading] = useState(false)

  const buildMailboxPayload = useCallback(() => ({
    mailboxName: mailboxForm.mailboxName,
    emailAddress: mailboxForm.emailAddress,
    enabled: mailboxForm.enabled,
    defaultAssigneeId: mailboxForm.defaultAssigneeId ? Number(mailboxForm.defaultAssigneeId) : null,
    imapHost: mailboxForm.imapHost,
    imapPort: Number(mailboxForm.imapPort),
    imapSslEnabled: mailboxForm.imapSslEnabled,
    imapUsername: mailboxForm.imapUsername,
    imapPassword: mailboxForm.imapPassword,
    imapFolder: mailboxForm.imapFolder,
    fetchIntervalSec: Number(mailboxForm.fetchIntervalSec),
    smtpHost: mailboxForm.smtpHost,
    smtpPort: Number(mailboxForm.smtpPort),
    smtpSslEnabled: mailboxForm.smtpSslEnabled,
    smtpUsername: mailboxForm.smtpUsername,
    smtpPassword: mailboxForm.smtpPassword,
    smtpFromName: mailboxForm.smtpFromName,
    autoReplyEnabled: mailboxForm.autoReplyEnabled,
    autoReplyTemplateId: mailboxForm.autoReplyTemplateId ? Number(mailboxForm.autoReplyTemplateId) : null,
  }), [mailboxForm])

  const fetchMailboxes = useCallback(async () => {
    if (!token || activeMenu !== '邮箱配置') return
    if (!canReadMailboxes) {
      setMailboxesData(null)
      setMailboxesError('当前账号没有邮箱配置管理权限')
      return
    }

    setMailboxesLoading(true)
    setMailboxesError('')
    try {
      const data = await mailboxApi.list({
        keyword: mailboxKeyword.trim(),
        page: mailboxPage,
        size: mailboxPageSize,
        status: mailboxStatusFilter !== 'ALL' ? mailboxStatusFilter : undefined,
      })
      setMailboxesData(data)
      const selected =
        data.records.find((mailbox) => mailbox.id === mailboxForm.id) ||
        data.records[0] ||
        null
      if (selected && !mailboxDirty) {
        setMailboxForm(toMailboxForm(selected))
        setMailboxTestResult(null)
      }
      if (!selected && !mailboxDirty) {
        setMailboxForm(emptyMailboxForm)
        setMailboxTestResult(null)
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxesError(error instanceof Error ? error.message : '邮箱列表加载失败')
    } finally {
      setMailboxesLoading(false)
    }
  }, [
    activeMenu,
    canReadMailboxes,
    handleAuthExpired,
    mailboxDirty,
    mailboxForm.id,
    mailboxKeyword,
    mailboxPage,
    mailboxPageSize,
    mailboxStatusFilter,
    token,
  ])

  useEffect(() => {
    void fetchMailboxes()
  }, [fetchMailboxes])

  const resetMailboxFilters = useCallback(() => {
    setMailboxKeyword('')
    setMailboxStatusFilter('ALL')
    setMailboxPage(1)
  }, [])

  const updateMailboxForm = useCallback((patch: Partial<MailboxFormState>) => {
    setMailboxForm((value) => ({ ...value, ...patch }))
    setMailboxDirty(true)
    setMailboxTestResult(null)
    setMailboxesError('')
  }, [])

  const selectMailbox = useCallback((mailbox: Mailbox) => {
    setMailboxForm(toMailboxForm(mailbox))
    setMailboxDirty(false)
    setMailboxTestResult(null)
    setMailboxesError('')
    setActiveMailboxStep('basic')
  }, [])

  const openCreateMailbox = useCallback(() => {
    if (!canCreateMailboxes) return
    setMailboxForm(emptyMailboxForm)
    setMailboxDirty(true)
    setMailboxTestResult(null)
    setMailboxesError('')
    setActiveMailboxStep('basic')
  }, [canCreateMailboxes])

  const moveMailboxStep = useCallback((direction: 1 | -1) => {
    const currentIndex = mailboxSteps.findIndex((step) => step.key === activeMailboxStep)
    const nextIndex = Math.min(Math.max(currentIndex + direction, 0), mailboxSteps.length - 1)
    setActiveMailboxStep(mailboxSteps[nextIndex].key)
  }, [activeMailboxStep])

  const saveMailbox = useCallback(async () => {
    if (!token) return
    if (mailboxForm.id ? !canUpdateMailboxes : !canCreateMailboxes) {
      setMailboxesError(mailboxForm.id ? '当前账号没有编辑邮箱配置权限' : '当前账号没有新建邮箱配置权限')
      return
    }
    setMailboxSaving(true)
    setMailboxesError('')
    try {
      const saved = await mailboxApi.save(mailboxForm.id, buildMailboxPayload())
      setMailboxForm(toMailboxForm(saved))
      setMailboxDirty(false)
      await fetchMailboxes()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxesError(error instanceof Error ? error.message : '邮箱配置保存失败')
    } finally {
      setMailboxSaving(false)
    }
  }, [buildMailboxPayload, canCreateMailboxes, canUpdateMailboxes, fetchMailboxes, handleAuthExpired, mailboxForm.id, token])

  const testMailboxConnection = useCallback(async (testType = 'ALL') => {
    if (!token) return
    if (!canTestMailboxes) {
      setMailboxesError('当前账号没有测试邮箱连接权限')
      return
    }
    setMailboxTesting(true)
    setMailboxesError('')
    try {
      const data = mailboxForm.id && !mailboxDirty
        ? await mailboxApi.testExisting(mailboxForm.id, testType)
        : await mailboxApi.testDraft(buildMailboxPayload(), testType)
      setMailboxTestResult(data)
      if (mailboxForm.id && !mailboxDirty) {
        await fetchMailboxes()
      }
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxesError(error instanceof Error ? error.message : '连接测试失败')
    } finally {
      setMailboxTesting(false)
    }
  }, [buildMailboxPayload, canTestMailboxes, fetchMailboxes, handleAuthExpired, mailboxDirty, mailboxForm.id, token])

  const openMailboxConfirm = useCallback((mailbox: Mailbox, type: 'enable' | 'disable' | 'delete') => {
    if (type === 'delete' && !canDeleteMailboxes) return
    if (type !== 'delete' && !canEnableMailboxes) return
    if (type === 'delete') {
      setMailboxConfirmAction({
        mailbox,
        type,
        title: '删除邮箱配置',
        text: '删除后该邮箱不再参与拉取，历史邮件、工单和发送记录保留。',
        actionLabel: '确认删除',
      })
      return
    }
    setMailboxConfirmAction({
      mailbox,
      type,
      title: type === 'enable' ? '启用邮箱配置' : '停用邮箱配置',
      text: type === 'enable' ? '启用后后台任务可继续拉取该邮箱。' : '停用后后台任务将不再拉取该邮箱。',
      actionLabel: type === 'enable' ? '确认启用' : '确认停用',
    })
  }, [canDeleteMailboxes, canEnableMailboxes])

  const submitMailboxConfirm = useCallback(async () => {
    if (!token || !mailboxConfirmAction) return
    if (mailboxConfirmAction.type === 'delete' && !canDeleteMailboxes) return
    if (mailboxConfirmAction.type !== 'delete' && !canEnableMailboxes) return
    setMailboxActionLoading(true)
    setMailboxesError('')
    try {
      if (mailboxConfirmAction.type === 'delete') {
        await mailboxApi.delete(mailboxConfirmAction.mailbox.id)
        if (mailboxForm.id === mailboxConfirmAction.mailbox.id) {
          setMailboxForm(emptyMailboxForm)
          setMailboxDirty(false)
        }
      } else {
        await mailboxApi.setEnabled(mailboxConfirmAction.mailbox.id, mailboxConfirmAction.type === 'enable')
      }
      setMailboxConfirmAction(null)
      await fetchMailboxes()
    } catch (error) {
      if (handleAuthExpired(error)) return
      setMailboxesError(error instanceof Error ? error.message : '邮箱操作失败')
    } finally {
      setMailboxActionLoading(false)
    }
  }, [canDeleteMailboxes, canEnableMailboxes, fetchMailboxes, handleAuthExpired, mailboxConfirmAction, mailboxForm.id, token])

  const changeMailboxKeyword = useCallback((value: string) => {
    setMailboxKeyword(value)
    setMailboxPage(1)
  }, [])

  const changeMailboxPageSize = useCallback((size: number) => {
    setMailboxPageSize(size)
    setMailboxPage(1)
  }, [])

  const changeMailboxStatusFilter = useCallback((value: string) => {
    setMailboxStatusFilter(value)
    setMailboxPage(1)
  }, [])

  return {
    activeMailboxStep,
    changeMailboxKeyword,
    changeMailboxPageSize,
    changeMailboxStatusFilter,
    closeMailboxConfirm: () => setMailboxConfirmAction(null),
    fetchMailboxes,
    mailboxActionLoading,
    mailboxConfirmAction,
    mailboxDirty,
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
    moveMailboxStep,
    openCreateMailbox,
    openMailboxConfirm,
    resetMailboxFilters,
    saveMailbox,
    selectMailbox,
    setActiveMailboxStep,
    setMailboxPage,
    submitMailboxConfirm,
    testMailboxConnection,
    updateMailboxForm,
  }
}
