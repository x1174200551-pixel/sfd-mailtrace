import { useRef } from 'react'
import type { MouseEvent } from 'react'
import { Alert, Button, Empty, Input, Segmented, Space, Tabs, Tag, message } from 'antd'
import {
  ArrowLeftOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EllipsisOutlined,
  FileTextOutlined,
  FlagOutlined,
  PaperClipOutlined,
  SendOutlined,
  StarTwoTone,
  SwapOutlined,
} from '@ant-design/icons'
import { Clock3, Inbox, MessageCircle, ShieldCheck, TriangleAlert, UserPlus, UserRound } from 'lucide-react'
import dayjs from 'dayjs'
import TiptapRichEditor from '../../TiptapRichEditor'
import { EmailHtmlFrame } from '../../components/ticket/EmailHtmlFrame'
import {
  priorityBadgeClass,
  priorityBadgeText,
  priorityLabel,
  statusLabel,
} from '../../constants/status'
import { readStoredToken } from '../../shared/api/request'
import type { TicketAttachment, TicketDetail, TicketEvent, TicketMessage, TicketUploadedFile } from '../../types/ticket'
import { formatFileSize } from '../../utils/format'

type TicketDetailPageProps = {
  canClaimCurrentTicket: boolean
  canOperateCurrentTicket: boolean
  claimSending: boolean
  detail: TicketDetail
  events: TicketEvent[]
  isCurrentTicketTerminal: boolean
  isCurrentTicketUnassigned: boolean
  msgFilter: string
  msgSortAsc: boolean
  onBackToList: () => void
  onClaimTicket: () => void
  onCloseTicket: () => void
  onDeleteAttachment: (attachmentId: number) => void
  onMsgFilterChange: (value: string) => void
  onMsgSortAscChange: (value: boolean) => void
  onOpenAssign: () => void
  onOpenPriority: () => void
  onOpenStatus: () => void
  onRemoveUploadedFile: (objectKey: string) => void
  onReply: () => void
  onReplyUpdate: (html: string, text: string) => void
  onSaveRemark: (remark: string) => void
  onTabChange: (key: string) => void
  onUploadFile: (file: File) => void
  replyContent: string
  replyHtml: string
  replySending: boolean
  remarkDraft: string
  setRemarkDraft: (value: string) => void
  tab: string
  ticketAttachments: TicketAttachment[]
  uploadedFiles: TicketUploadedFile[]
  uploadingFile: boolean
}

function htmlToText(html: string): string {
  return html
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<\/div>/gi, '\n')
    .replace(/<\/li>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function msgBodyText(msg: TicketMessage): string {
  if (msg.contentText) return msg.contentText
  if (msg.contentHtml) return htmlToText(msg.contentHtml)
  if (msg.contentBody) return msg.contentBody
  return '(无内容)'
}

function MessageBody({ msg }: { msg: TicketMessage }) {
  const html = msg.contentHtml?.trim()
  if (html) {
    return <EmailHtmlFrame html={html} />
  }
  return <div className="msg-body msg-body-text">{msgBodyText(msg)}</div>
}

function messageDirection(msg: TicketMessage) {
  return msg.direction || msg.messageDirection
}

function formatDetailDate(value: string | null | undefined) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

async function parseDownloadError(response: Response) {
  const fallback = `附件下载失败：${response.status}`
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('application/json')) {
    return fallback
  }
  try {
    const body = await response.json()
    return typeof body?.message === 'string' && body.message ? body.message : fallback
  } catch {
    return fallback
  }
}

function saveBlob(blob: Blob, fileName: string) {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName || 'attachment'
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
}

export function TicketDetailPage({
  canClaimCurrentTicket,
  canOperateCurrentTicket,
  claimSending,
  detail,
  events,
  isCurrentTicketTerminal,
  isCurrentTicketUnassigned,
  msgFilter,
  msgSortAsc,
  onBackToList,
  onClaimTicket,
  onCloseTicket,
  onDeleteAttachment,
  onMsgFilterChange,
  onMsgSortAscChange,
  onOpenAssign,
  onOpenPriority,
  onOpenStatus,
  onRemoveUploadedFile,
  onReply,
  onReplyUpdate,
  onSaveRemark,
  onTabChange,
  onUploadFile,
  replyContent,
  replyHtml,
  replySending,
  remarkDraft,
  setRemarkDraft,
  tab,
  ticketAttachments,
  uploadedFiles,
  uploadingFile,
}: TicketDetailPageProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const inboundCount = detail.messages.filter((msg) => messageDirection(msg) === 'INBOUND').length
  const outboundCount = detail.messages.filter((msg) => messageDirection(msg) === 'OUTBOUND').length
  const visibleMessageCount = detail.messages.filter((msg) => messageDirection(msg) !== 'INTERNAL').length
  const filteredMessages = [...detail.messages]
    .filter((msg) => {
      const dir = messageDirection(msg)
      return msgFilter === 'ALL' ? dir !== 'INTERNAL' : dir === msgFilter
    })
    .sort((left, right) => {
      const leftTime = left.sentAt || left.createdAt
      const rightTime = right.sentAt || right.createdAt
      if (!leftTime || !rightTime) return 0
      return msgSortAsc
        ? new Date(leftTime).getTime() - new Date(rightTime).getTime()
        : new Date(rightTime).getTime() - new Date(leftTime).getTime()
    })
  const lifecycleEvents = [...events].reverse()
  const responseDeadline = formatDetailDate(detail.slaResponseDeadline)
  const resolveDeadline = formatDetailDate(detail.slaResolveDeadline)
  const handleAttachmentDownload = async (event: MouseEvent<HTMLAnchorElement>, attachment: TicketAttachment) => {
    event.preventDefault()
    if (!attachment.downloadUrl) {
      message.error('附件下载地址不存在')
      return
    }
    const token = readStoredToken()
    if (!token) {
      message.error('登录状态已失效，请重新登录')
      return
    }

    try {
      const response = await fetch(attachment.downloadUrl, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) {
        message.error(await parseDownloadError(response))
        return
      }
      saveBlob(await response.blob(), attachment.fileName)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '附件下载失败')
    }
  }

  return (
    <section className="app-content ticket-detail-page" aria-label="工单详情">
      <header className="detail-topbar">
        <div className="detail-title-block">
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={onBackToList} className="detail-back-btn">
            返回列表
          </Button>
          <div>
            <h2>工单详情</h2>
            <span>{detail.ticketNo}</span>
          </div>
        </div>
        <div className="detail-top-actions">
          {canClaimCurrentTicket && (
            <Button type="primary" size="small" icon={<UserPlus size={14} />} loading={claimSending} onClick={onClaimTicket}>
              领取工单
            </Button>
          )}
          <Button size="small" icon={<SwapOutlined />} disabled={!canOperateCurrentTicket} onClick={onOpenAssign}>转派</Button>
          <Button size="small" icon={<FlagOutlined />} disabled={!canOperateCurrentTicket} onClick={onOpenPriority}>优先级</Button>
          <Button size="small" disabled={!canOperateCurrentTicket} onClick={onOpenStatus}>状态</Button>
          <Button size="small" icon={<CloseCircleOutlined />} disabled={!canOperateCurrentTicket} onClick={onCloseTicket}>关闭</Button>
          <Button size="small" icon={<EllipsisOutlined />} disabled={!canOperateCurrentTicket}>更多</Button>
        </div>
      </header>

      <section className="detail-summary-strip" aria-label="工单概览">
        <div className="detail-summary-item active">
          <span className="detail-summary-icon"><Inbox size={17} /></span>
          <span className="detail-summary-copy">
            <span>当前状态</span>
            <small>{statusLabel(detail.status)}</small>
          </span>
          <strong>{detail.slaBreached ? '超时' : '正常'}</strong>
        </div>
        <div className="detail-summary-item detail-summary-item--info">
          <span className="detail-summary-icon"><UserRound size={17} /></span>
          <span className="detail-summary-copy">
            <span>处理人</span>
            <small>{detail.assigneeName || '暂未分配'}</small>
          </span>
          <strong>{detail.assigneeName ? '已分配' : '待分配'}</strong>
        </div>
        <div className="detail-summary-item detail-summary-item--success">
          <span className="detail-summary-icon"><MessageCircle size={17} /></span>
          <span className="detail-summary-copy">
            <span>邮件会话</span>
            <small>客户 {inboundCount} / 客服 {outboundCount}</small>
          </span>
          <strong>{visibleMessageCount}</strong>
        </div>
        <div className={`detail-summary-item ${detail.slaBreached ? 'detail-summary-item--danger' : 'detail-summary-item--warning'}`}>
          <span className="detail-summary-icon">{detail.slaBreached ? <TriangleAlert size={17} /> : <Clock3 size={17} />}</span>
          <span className="detail-summary-copy">
            <span>SLA</span>
            <small>首次响应 {responseDeadline}</small>
          </span>
          <strong>{detail.slaBreached ? '已超时' : '监控中'}</strong>
        </div>
      </section>

      <div className="detail-body">
        <main className="detail-main">
          <section className="detail-header-card">
            <div className="detail-header-top">
              <div className="detail-header-left">
                <span className={`priority-pill ${priorityBadgeClass(detail.priority)}`}>{priorityBadgeText(detail.priority)}</span>
                <span className={`detail-priority-text priority-${detail.priority.toLowerCase()}`}>{priorityLabel(detail.priority)}</span>
                <StarTwoTone twoToneColor="#f59e0b" />
                <span className="detail-ticket-no">{detail.ticketNo}</span>
                <span className={`ticket-status-tag ${detail.slaBreached ? 'overdue' : detail.status === 'WAITING_CUSTOMER' ? 'waiting' : detail.status === 'CLOSED' ? 'closed' : 'processing'}`}>
                  {statusLabel(detail.status)}
                </span>
                {detail.linkSuspect && <Tag color="warning">疑似断链</Tag>}
              </div>
            </div>
            <h1 className="detail-subject">{detail.subject}</h1>
            <div className="detail-meta-grid">
              <div>
                <span>来源邮箱</span>
                <strong>{detail.mailboxName || '客户邮件'}</strong>
              </div>
              <div>
                <span>客户</span>
                <strong>{detail.customerEmail}</strong>
              </div>
              <div>
                <span>创建时间</span>
                <strong>{formatDetailDate(detail.createdAt)}</strong>
              </div>
              <div>
                <span>更新时间</span>
                <strong>{formatDetailDate(detail.updatedAt)}</strong>
              </div>
            </div>
          </section>

          <section className="detail-content-card">
            <Tabs
              activeKey={tab}
              onChange={onTabChange}
              items={[
                {
                  key: 'mail',
                  label: '邮件会话',
                  children: (
                    <div className="detail-tab-scroll detail-mail-conversation">
                      {detail.messages.length > 0 && (
                        <div className="msg-filter-bar">
                          <Segmented
                            size="small"
                            value={msgFilter}
                            onChange={(value) => onMsgFilterChange(String(value))}
                            options={[
                              { label: `全部邮件 (${visibleMessageCount})`, value: 'ALL' },
                              { label: `客户 (${inboundCount})`, value: 'INBOUND' },
                              { label: `客服 (${outboundCount})`, value: 'OUTBOUND' },
                            ]}
                          />
                          <button className="msg-sort-button" onClick={() => onMsgSortAscChange(!msgSortAsc)} type="button">
                            <span>排序</span>
                            <strong>{msgSortAsc ? '时间升序' : '时间降序'}</strong>
                          </button>
                        </div>
                      )}

                      {detail.messages.length === 0 ? (
                        <div className="detail-empty-block"><Empty description="暂无邮件消息" /></div>
                      ) : (
                        filteredMessages.map((msg) => {
                          const dir = messageDirection(msg)
                          const isAgent = dir === 'OUTBOUND'
                          const isAuto = dir === 'INTERNAL'
                          const displayName = msg.displayName || (msg.fromAddress ? msg.fromAddress.split('@')[0] : '')
                          const firstChar = displayName ? displayName[0].toUpperCase() : isAuto ? 'S' : 'C'
                          const msgAttachments = ticketAttachments.filter((attachment) => attachment.messageId === msg.id)

                          return (
                            <article key={msg.id} className={`msg-card ${isAuto ? 'msg-system' : isAgent ? 'msg-agent' : 'msg-customer'}`}>
                              <div className={`msg-avatar-circle ${isAuto ? 'avatar-system' : isAgent ? 'avatar-agent' : 'avatar-customer'}`}>
                                {firstChar}
                              </div>
                              <div className="msg-content">
                                <header className="msg-header">
                                  <div className="msg-header-left">
                                    <span className="msg-from">{msg.fromAddress || '系统'}</span>
                                    <span className={`msg-badge ${isAuto ? 'badge-system' : isAgent ? 'badge-agent' : 'badge-customer'}`}>
                                      {isAuto ? '系统' : isAgent ? '客服' : '客户'}
                                    </span>
                                  </div>
                                  <span className="msg-time">{formatDetailDate(msg.sentAt || msg.createdAt)}</span>
                                </header>
                                {msg.toAddress && <div className="msg-to">收件人：{msg.toAddress}</div>}
                                <MessageBody msg={msg} />
                                {msgAttachments.length > 0 && (
                                  <div className="msg-attachments">
                                    {msgAttachments.map((attachment) => (
                                      <div key={attachment.id} className="msg-attachment-item">
                                        <PaperClipOutlined />
                                        <a
                                          href={attachment.downloadUrl || undefined}
                                          className="msg-attachment-link"
                                          onClick={(event) => handleAttachmentDownload(event, attachment)}
                                        >
                                          {attachment.fileName}
                                        </a>
                                        <span className="msg-attachment-size">({formatFileSize(attachment.fileSize)})</span>
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            </article>
                          )
                        })
                      )}

                      <section className="detail-editor">
                        <header className="detail-editor-head">
                          <strong>回复客户</strong>
                          <span>{canOperateCurrentTicket ? '将通过邮件发送给客户' : '当前不可回复'}</span>
                        </header>
                        {!canOperateCurrentTicket && !isCurrentTicketTerminal && (
                          <Alert
                            type="info"
                            showIcon
                            title={isCurrentTicketUnassigned ? '领取后即可处理该工单' : '当前账号不可操作该工单'}
                            className="detail-editor-alert"
                          />
                        )}
                        <TiptapRichEditor
                          placeholder="请输入回复内容（将发送邮件给客户）..."
                          disabled={!canOperateCurrentTicket}
                          onUpdate={onReplyUpdate}
                        />
                        {uploadedFiles.length > 0 && (
                          <div className="uploaded-file-list">
                            {uploadedFiles.map((file) => (
                              <Tag key={file.objectKey} closable onClose={() => onRemoveUploadedFile(file.objectKey)}>
                                <PaperClipOutlined /> {file.fileName} ({formatFileSize(file.fileSize)})
                              </Tag>
                            ))}
                          </div>
                        )}
                        <div className="detail-editor-actions">
                          <div>
                            <input
                              type="file"
                              ref={fileInputRef}
                              className="detail-file-input"
                              onChange={(event) => {
                                const file = event.target.files?.[0]
                                if (file) {
                                  onUploadFile(file)
                                  event.target.value = ''
                                }
                              }}
                            />
                            <Button type="text" icon={<PaperClipOutlined />} loading={uploadingFile} disabled={!canOperateCurrentTicket} onClick={() => fileInputRef.current?.click()}>
                              添加附件
                            </Button>
                            <Button type="text" icon={<FileTextOutlined />} disabled>插入模板</Button>
                          </div>
                          <Space>
                            <Button disabled>保存草稿</Button>
                            <Button
                              type="primary"
                              icon={<SendOutlined />}
                              onClick={onReply}
                              loading={replySending}
                              disabled={!canOperateCurrentTicket || (!replyContent.trim() && !replyHtml.trim())}
                            >
                              发送邮件
                            </Button>
                          </Space>
                        </div>
                      </section>
                    </div>
                  ),
                },
                {
                  key: 'log',
                  label: '工单日志',
                  children: events.length > 0 ? (
                    <div className="detail-tab-scroll detail-event-list">
                      {events.map((event) => (
                        <div className="detail-event-item" key={event.id}>
                          <i />
                          <div>
                            <strong>{event.eventContent}</strong>
                            <span>{event.operator} · {formatDetailDate(event.eventAt)}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="detail-empty-block"><Empty description="暂无工单日志" /></div>
                  ),
                },
                {
                  key: 'customer',
                  label: '客户信息',
                  children: (
                    <div className="detail-tab-scroll detail-info-list">
                      <div><span>客户邮箱</span><strong>{detail.customerEmail}</strong></div>
                      <div><span>来源邮箱</span><strong>{detail.mailboxName || `#${detail.mailboxId}`}</strong></div>
                    </div>
                  ),
                },
                {
                  key: 'sla',
                  label: 'SLA',
                  children: (
                    <div className="detail-tab-scroll detail-info-list">
                      <div><span>SLA 状态</span><strong className={detail.slaBreached ? 'danger' : 'success'}>{detail.slaBreached ? '已超时' : '正常'}</strong></div>
                      <div><span>首次响应截止</span><strong>{responseDeadline}</strong></div>
                      <div><span>解决截止</span><strong>{resolveDeadline}</strong></div>
                    </div>
                  ),
                },
                {
                  key: 'attachment',
                  label: `附件 (${ticketAttachments.length})`,
                  children: (
                    <div className="detail-tab-scroll detail-attachments">
                      {ticketAttachments.length === 0 ? (
                        <div className="detail-empty-block"><Empty description="暂无附件" /></div>
                      ) : (
                        <div className="attachment-grid">
                          {ticketAttachments.map((attachment) => (
                            <div key={attachment.id} className="attachment-card">
                              <div className="attachment-icon"><FileTextOutlined /></div>
                              <div className="attachment-info">
                                <div className="attachment-name" title={attachment.fileName}>{attachment.fileName}</div>
                                <div className="attachment-meta">
                                  {formatFileSize(attachment.fileSize)}
                                  {attachment.contentType && <span> · {attachment.contentType}</span>}
                                </div>
                              </div>
                              <div className="attachment-actions">
                                <Button type="link" size="small" icon={<DownloadOutlined />} href={attachment.downloadUrl || undefined} target="_blank" />
                                <Button
                                  type="link"
                                  size="small"
                                  danger
                                  icon={<DeleteOutlined />}
                                  disabled={!canOperateCurrentTicket}
                                  onClick={() => {
                                    if (window.confirm('确认删除此附件？')) onDeleteAttachment(attachment.id)
                                  }}
                                />
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ),
                },
              ]}
            />
          </section>
        </main>

        <aside className="detail-sidebar">
          <section className="detail-side-card">
            <header>
              <div>
                <strong>工单信息</strong>
                <span>处理上下文</span>
              </div>
            </header>
            <div className="detail-side-list">
              <div>
                <span>负责人</span>
                <strong>
                  {detail.assigneeName ? <i className="detail-assignee-avatar">{detail.assigneeName[0]}</i> : null}
                  {detail.assigneeName || '未分配'}
                </strong>
              </div>
              <div><span>客户</span><strong>{detail.customerEmail}</strong></div>
              <div><span>优先级</span><strong>{priorityLabel(detail.priority)}</strong></div>
              <div><span>状态</span><strong>{statusLabel(detail.status)}</strong></div>
              <div><span>来源</span><strong>{detail.mailboxName || '客户邮件'}</strong></div>
            </div>
          </section>

          <section className="detail-side-card">
            <header>
              <div>
                <strong>备注</strong>
                <span>{canOperateCurrentTicket ? '失焦后自动保存' : '只读'}</span>
              </div>
            </header>
            <Input.TextArea
              rows={3}
              size="small"
              value={remarkDraft}
              onChange={(event) => setRemarkDraft(event.target.value)}
              disabled={!canOperateCurrentTicket}
              placeholder="点击添加备注..."
              className="detail-remark-input"
              onBlur={(event) => {
                if (!canOperateCurrentTicket) return
                const value = event.target.value.trim()
                if (value !== (detail.remark || '')) onSaveRemark(value)
              }}
            />
          </section>

          <section className="detail-side-card">
            <header>
              <div>
                <strong>SLA 信息</strong>
                <span>{detail.slaBreached ? '已超时' : '监控中'}</span>
              </div>
              <Tag color={detail.slaBreached ? 'red' : 'green'}>{detail.slaBreached ? '已超时' : '正常'}</Tag>
            </header>
            <div className="detail-side-list">
              <div><span>首次响应截止</span><strong>{responseDeadline}</strong></div>
              <div><span>解决截止</span><strong>{resolveDeadline}</strong></div>
              <div><span>首次回复</span><strong>{formatDetailDate(detail.firstReplyAt)}</strong></div>
              <div><span>关闭时间</span><strong>{formatDetailDate(detail.closedAt)}</strong></div>
            </div>
          </section>

          <section className="detail-side-card">
            <header>
              <div>
                <strong>生命周期</strong>
                <span>共 {lifecycleEvents.length} 条</span>
              </div>
            </header>
            {lifecycleEvents.length > 0 ? (
              <div className="detail-mini-events">
                {lifecycleEvents.map((event) => (
                  <div key={event.id}>
                    <i />
                    <span>
                      <strong>{event.eventContent}</strong>
                      <small>{formatDetailDate(event.eventAt)}</small>
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="detail-empty-note"><ShieldCheck size={18} />暂无记录</div>
            )}
          </section>
        </aside>
      </div>
    </section>
  )
}
