import { useCallback, useEffect, useState } from 'react'
import { enterpriseApi } from '../api/enterprises'
import type {
  Enterprise,
  EnterpriseConfirmAction,
  EnterpriseFormState,
  EnterpriseListResponse,
} from '../types/enterprise'

const emptyForm: EnterpriseFormState = {
  id: null,
  enterpriseName: '',
  contactName: '',
  contactEmail: '',
  contactPhone: '',
  enabled: true,
  feishuNotifyEnabled: false,
  feishuGroupName: '',
  feishuWebhookUrl: '',
  feishuSigningSecret: '',
  clearFeishuConfig: false,
  remark: '',
}

type Params = {
  activeMenu: string
  canRead: boolean
  handleAuthExpired: (error: unknown) => boolean
  token: string
}

function toForm(enterprise: Enterprise): EnterpriseFormState {
  return {
    id: enterprise.id,
    enterpriseName: enterprise.enterpriseName,
    contactName: enterprise.contactName || '',
    contactEmail: enterprise.contactEmail || '',
    contactPhone: enterprise.contactPhone || '',
    enabled: enterprise.enabled,
    feishuNotifyEnabled: enterprise.feishuNotifyEnabled,
    feishuGroupName: enterprise.feishuGroupName || '',
    feishuWebhookUrl: '',
    feishuSigningSecret: '',
    clearFeishuConfig: false,
    remark: enterprise.remark || '',
  }
}

export function useEnterpriseManagement({ activeMenu, canRead, handleAuthExpired, token }: Params) {
  const [data, setData] = useState<EnterpriseListResponse | null>(null)
  const [keyword, setKeyword] = useState('')
  const [enabledFilter, setEnabledFilter] = useState('ALL')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<EnterpriseFormState>(emptyForm)
  const [confirmAction, setConfirmAction] = useState<EnterpriseConfirmAction>(null)
  const [feishuTesting, setFeishuTesting] = useState(false)
  const [feishuTestMessage, setFeishuTestMessage] = useState('')

  const fetchEnterprises = useCallback(async () => {
    if (!token || activeMenu !== '企业管理') return
    if (!canRead) {
      setData(null)
      setError('当前账号没有企业管理权限')
      return
    }
    setLoading(true)
    setError('')
    try {
      const nextData = await enterpriseApi.list({
        keyword: keyword.trim() || undefined,
        enabled: enabledFilter === 'ALL' ? undefined : enabledFilter === 'true',
        page,
        size: pageSize,
      })
      if (nextData.pages > 0 && page > nextData.pages) {
        setPage(nextData.pages)
        return
      }
      setData(nextData)
    } catch (nextError) {
      if (handleAuthExpired(nextError)) return
      setError(nextError instanceof Error ? nextError.message : '企业列表加载失败')
    } finally {
      setLoading(false)
    }
  }, [activeMenu, canRead, enabledFilter, handleAuthExpired, keyword, page, pageSize, token])

  useEffect(() => {
    void fetchEnterprises()
  }, [fetchEnterprises])

  const openCreate = useCallback(() => {
    setForm(emptyForm)
    setError('')
    setFormOpen(true)
    setFeishuTestMessage('')
  }, [])

  const changeKeyword = useCallback((value: string) => {
    setKeyword(value)
    setPage(1)
  }, [])

  const changeEnabledFilter = useCallback((value: string) => {
    setEnabledFilter(value)
    setPage(1)
  }, [])

  const changePageSize = useCallback((size: number) => {
    setPageSize(size)
    setPage(1)
  }, [])

  const openEdit = useCallback((enterprise: Enterprise) => {
    setForm(toForm(enterprise))
    setError('')
    setFormOpen(true)
    setFeishuTestMessage('')
  }, [])

  const save = useCallback(async () => {
    if (!form.enterpriseName.trim()) {
      setError('请输入企业名称')
      return
    }
    if (form.clearFeishuConfig && !window.confirm('确认清除该企业的飞书群机器人配置吗？清除后企业飞书通知会立即关闭。')) {
      return
    }
    setSaving(true)
    setError('')
    try {
      await enterpriseApi.save(form.id, {
        enterpriseName: form.enterpriseName.trim(),
        contactName: form.contactName.trim(),
        contactEmail: form.contactEmail.trim(),
        contactPhone: form.contactPhone.trim(),
        enabled: form.enabled,
        feishuNotifyEnabled: form.feishuNotifyEnabled,
        feishuGroupName: form.feishuGroupName.trim(),
        feishuWebhookUrl: form.feishuWebhookUrl.trim(),
        feishuSigningSecret: form.feishuSigningSecret.trim(),
        clearFeishuConfig: form.clearFeishuConfig,
        remark: form.remark.trim(),
      })
      setFormOpen(false)
      await fetchEnterprises()
    } catch (nextError) {
      if (handleAuthExpired(nextError)) return
      setError(nextError instanceof Error ? nextError.message : '企业保存失败')
    } finally {
      setSaving(false)
    }
  }, [fetchEnterprises, form, handleAuthExpired])

  const testFeishuGroup = useCallback(async () => {
    if (!form.id) {
      setFeishuTestMessage('请先保存企业和群机器人配置，再发送测试消息')
      return
    }
    setFeishuTesting(true)
    setFeishuTestMessage('')
    try {
      const result = await enterpriseApi.testFeishuGroup(form.id)
      setFeishuTestMessage(result.message)
      await fetchEnterprises()
    } catch (nextError) {
      if (handleAuthExpired(nextError)) return
      setFeishuTestMessage(nextError instanceof Error ? nextError.message : '飞书通知群测试失败')
    } finally {
      setFeishuTesting(false)
    }
  }, [fetchEnterprises, form.id, handleAuthExpired])

  const submitConfirm = useCallback(async () => {
    if (!confirmAction) return
    setActionLoading(true)
    setError('')
    try {
      await enterpriseApi.setEnabled(confirmAction.enterprise.id, confirmAction.nextEnabled)
      setConfirmAction(null)
      await fetchEnterprises()
    } catch (nextError) {
      if (handleAuthExpired(nextError)) return
      setError(nextError instanceof Error ? nextError.message : '企业状态更新失败')
    } finally {
      setActionLoading(false)
    }
  }, [confirmAction, fetchEnterprises, handleAuthExpired])

  return {
    actionLoading,
    confirmAction,
    data,
    enabledFilter,
    error,
    feishuTestMessage,
    feishuTesting,
    fetchEnterprises,
    form,
    formOpen,
    keyword,
    loading,
    openCreate,
    openEdit,
    save,
    saving,
    page,
    pageSize,
    setConfirmAction,
    setEnabledFilter: changeEnabledFilter,
    setForm: (patch: Partial<EnterpriseFormState>) => setForm((value) => ({ ...value, ...patch })),
    setFormOpen,
    setKeyword: changeKeyword,
    setPage,
    setPageSize: changePageSize,
    submitConfirm,
    testFeishuGroup,
  }
}
