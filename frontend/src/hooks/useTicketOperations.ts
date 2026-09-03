import { useCallback, useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'
import { message } from 'antd'
import { ticketApi } from '../api/tickets'
import { userApi } from '../api/users'
import type { TicketAttachment, TicketDetail, TicketUploadedFile } from '../types/ticket'
import type { ManagedUser } from '../types/user'

type UseTicketOperationsParams = {
  canOperateCurrentTicket: boolean
  fetchTicketStats: () => Promise<void>
  fetchTickets: () => Promise<void>
  handleAuthExpired: (error: unknown) => boolean
  reloadTicketDetail: () => Promise<void>
  setTicketAttachments: Dispatch<SetStateAction<TicketAttachment[]>>
  ticketDetail: TicketDetail | null
  token: string
}

export function useTicketOperations({
  canOperateCurrentTicket,
  fetchTicketStats,
  fetchTickets,
  handleAuthExpired,
  reloadTicketDetail,
  setTicketAttachments,
  ticketDetail,
  token,
}: UseTicketOperationsParams) {
  const [replyContent, setReplyContent] = useState('')
  const [replyHtml, setReplyHtml] = useState('')
  const [replySending, setReplySending] = useState(false)
  const [uploadedFiles, setUploadedFiles] = useState<TicketUploadedFile[]>([])
  const [uploadingFile, setUploadingFile] = useState(false)
  const [assignModalOpen, setAssignModalOpen] = useState(false)
  const [assignUsers, setAssignUsers] = useState<ManagedUser[]>([])
  const [assignUserId, setAssignUserId] = useState<number | null>(null)
  const [assignReason, setAssignReason] = useState('')
  const [assignNotifyAssignee, setAssignNotifyAssignee] = useState(true)
  const [assignSending, setAssignSending] = useState(false)
  const [claimSending, setClaimSending] = useState(false)
  const [closeModalOpen, setCloseModalOpen] = useState(false)
  const [closeReason, setCloseReason] = useState('')
  const [closeConfirmed, setCloseConfirmed] = useState(false)
  const [closeSending, setCloseSending] = useState(false)
  const [priorityModalOpen, setPriorityModalOpen] = useState(false)
  const [priorityValue, setPriorityValue] = useState('NORMAL')
  const [priorityReason, setPriorityReason] = useState('')
  const [prioritySending, setPrioritySending] = useState(false)
  const [statusModalOpen, setStatusModalOpen] = useState(false)
  const [statusValue, setStatusValue] = useState('PROCESSING')
  const [statusReason, setStatusReason] = useState('')
  const [statusSending, setStatusSending] = useState(false)

  const fetchAgentUsers = useCallback(async () => {
    if (!token) return
    try {
      const data = await userApi.list({ page: 1, size: 200, enabled: true })
      setAssignUsers(data.records)
    } catch {
      setAssignUsers([])
    }
  }, [token])

  const handleReply = useCallback(async (replyTemplateId?: number | null) => {
    const content = replyContent.trim()
    const html = replyHtml.trim()
    if (!token || !ticketDetail || !canOperateCurrentTicket || (!content && !html)) return false
    setReplySending(true)
    try {
      const attachments = uploadedFiles.map((file) => ({
        objectKey: file.objectKey,
        fileName: file.fileName,
        fileSize: file.fileSize,
        contentType: file.contentType,
      }))
      const updatedTicket = await ticketApi.reply(ticketDetail.id, {
        attachments,
        content: content || html,
        htmlContent: html || content,
        internal: false,
        replyTemplateId: replyTemplateId ?? null,
      })
      setReplyContent('')
      setReplyHtml('')
      setUploadedFiles([])
      await reloadTicketDetail()
      void fetchTickets()
      const latestOutbound = [...updatedTicket.messages]
        .reverse()
        .find((item) => (item.direction || item.messageDirection) === 'OUTBOUND')
      if (latestOutbound?.sendStatus === 'FAILED') {
        message.error('邮件发送失败，已保留失败记录，可在发件记录中重试')
      } else if (latestOutbound?.sendStatus === 'PENDING') {
        message.success('回复已提交，正在发送')
      } else {
        message.success('回复已发送')
      }
      return true
    } catch (error: any) {
      message.error(error?.message || '回复发送失败')
      if (handleAuthExpired(error)) return false
      return false
    } finally {
      setReplySending(false)
    }
  }, [canOperateCurrentTicket, fetchTickets, handleAuthExpired, reloadTicketDetail, replyContent, replyHtml, ticketDetail, token, uploadedFiles])

  const handleAssign = useCallback(async () => {
    if (!token || !ticketDetail || !canOperateCurrentTicket || !assignUserId) return
    setAssignSending(true)
    try {
      await ticketApi.assign(ticketDetail.id, {
        assigneeId: assignUserId,
        notifyAssignee: assignNotifyAssignee,
        reason: assignReason.trim() || null,
      })
      setAssignModalOpen(false)
      setAssignUserId(null)
      setAssignReason('')
      setAssignNotifyAssignee(true)
      await reloadTicketDetail()
      void fetchTickets()
      message.success('工单已转派')
    } catch (error: any) {
      message.error(error?.message || '转派失败')
      if (handleAuthExpired(error)) return
    } finally {
      setAssignSending(false)
    }
  }, [assignNotifyAssignee, assignReason, assignUserId, canOperateCurrentTicket, fetchTickets, handleAuthExpired, reloadTicketDetail, ticketDetail, token])

  const handleClose = useCallback(async () => {
    if (!token || !ticketDetail || !canOperateCurrentTicket) return
    setCloseSending(true)
    try {
      await ticketApi.close(ticketDetail.id, { reason: closeReason.trim() || null })
      setCloseModalOpen(false)
      setCloseReason('')
      setCloseConfirmed(false)
      await reloadTicketDetail()
      void fetchTickets()
      message.success('工单已关闭')
    } catch (error: any) {
      message.error(error?.message || '关闭失败')
      if (handleAuthExpired(error)) return
    } finally {
      setCloseSending(false)
    }
  }, [canOperateCurrentTicket, closeReason, fetchTickets, handleAuthExpired, reloadTicketDetail, ticketDetail, token])

  const handleUploadFile = useCallback(async (file: File) => {
    if (!token) return
    setUploadingFile(true)
    try {
      const result = await ticketApi.uploadFile(file)
      setUploadedFiles((value) => [...value, result])
      message.success(`"${file.name}" 上传成功`)
    } catch (error: any) {
      message.error(error?.message || '上传失败')
      if (handleAuthExpired(error)) return
    } finally {
      setUploadingFile(false)
    }
  }, [handleAuthExpired, token])

  const handleRemoveFile = useCallback((objectKey: string) => {
    setUploadedFiles((value) => value.filter((file) => file.objectKey !== objectKey))
  }, [])

  const handleDeleteAttachment = useCallback(async (attachmentId: number) => {
    if (!token || !ticketDetail || !canOperateCurrentTicket) return
    try {
      await ticketApi.deleteAttachment(ticketDetail.id, attachmentId)
      setTicketAttachments((value) => value.filter((attachment) => attachment.id !== attachmentId))
      message.success('附件已删除')
    } catch (error: any) {
      message.error(error?.message || '附件删除失败')
      if (handleAuthExpired(error)) return
    }
  }, [canOperateCurrentTicket, handleAuthExpired, setTicketAttachments, ticketDetail, token])

  const handlePriority = useCallback(async () => {
    if (!token || !ticketDetail || !canOperateCurrentTicket) return
    setPrioritySending(true)
    try {
      await ticketApi.updatePriority(ticketDetail.id, {
        priority: priorityValue,
        reason: priorityReason.trim() || null,
      })
      setPriorityModalOpen(false)
      setPriorityReason('')
      await reloadTicketDetail()
      void fetchTickets()
      message.success('优先级已修改')
    } catch (error: any) {
      message.error(error?.message || '修改优先级失败')
      if (handleAuthExpired(error)) return
    } finally {
      setPrioritySending(false)
    }
  }, [canOperateCurrentTicket, fetchTickets, handleAuthExpired, priorityReason, priorityValue, reloadTicketDetail, ticketDetail, token])

  const handleStatusChange = useCallback(async () => {
    if (!token || !ticketDetail || !canOperateCurrentTicket) return
    setStatusSending(true)
    try {
      await ticketApi.updateStatus(ticketDetail.id, {
        reason: statusReason.trim() || null,
        status: statusValue,
      })
      setStatusModalOpen(false)
      setStatusReason('')
      await reloadTicketDetail()
      void fetchTickets()
      message.success('状态已修改')
    } catch (error: any) {
      message.error(error?.message || '修改状态失败')
      if (handleAuthExpired(error)) return
    } finally {
      setStatusSending(false)
    }
  }, [canOperateCurrentTicket, fetchTickets, handleAuthExpired, reloadTicketDetail, statusReason, statusValue, ticketDetail, token])

  const handleClaimTicket = useCallback(async () => {
    if (!token || !ticketDetail) return
    setClaimSending(true)
    try {
      await ticketApi.claim(ticketDetail.id)
      await reloadTicketDetail()
      void fetchTickets()
      void fetchTicketStats()
      message.success('工单已领取')
    } catch (error: any) {
      message.error(error?.message || '领取失败')
      if (handleAuthExpired(error)) return
    } finally {
      setClaimSending(false)
    }
  }, [fetchTicketStats, fetchTickets, handleAuthExpired, reloadTicketDetail, ticketDetail, token])

  return {
    assignModalOpen,
    assignNotifyAssignee,
    assignReason,
    assignSending,
    assignUserId,
    assignUsers,
    claimSending,
    closeConfirmed,
    closeModalOpen,
    closeReason,
    closeSending,
    fetchAgentUsers,
    handleAssign,
    handleClaimTicket,
    handleClose,
    handleDeleteAttachment,
    handlePriority,
    handleRemoveFile,
    handleReply,
    handleStatusChange,
    handleUploadFile,
    priorityModalOpen,
    priorityReason,
    prioritySending,
    priorityValue,
    replyContent,
    replyHtml,
    replySending,
    setAssignModalOpen,
    setAssignNotifyAssignee,
    setAssignReason,
    setAssignUserId,
    setCloseConfirmed,
    setCloseModalOpen,
    setCloseReason,
    setPriorityModalOpen,
    setPriorityReason,
    setPriorityValue,
    setReplyContent,
    setReplyHtml,
    setStatusModalOpen,
    setStatusReason,
    setStatusValue,
    statusModalOpen,
    statusReason,
    statusSending,
    statusValue,
    uploadedFiles,
    uploadingFile,
  }
}
