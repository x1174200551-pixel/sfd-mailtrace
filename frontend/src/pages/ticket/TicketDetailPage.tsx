import { useRef } from 'react'
import { Button, Alert, Avatar, Card, Descriptions, Empty, Input, Segmented, Space, Tabs, Tag, Timeline } from 'antd'
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
import { UserPlus } from 'lucide-react'
import dayjs from 'dayjs'
import TiptapRichEditor from '../../TiptapRichEditor'
import {
  priorityBadgeClass,
  priorityBadgeText,
  priorityLabel,
  statusLabel,
} from '../../constants/status'
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

function messageDirection(msg: TicketMessage) {
  return msg.direction || msg.messageDirection
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

  return (
    <div className="ticket-detail-page">
      <div className="detail-topbar">
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={onBackToList} className="detail-back-btn">
          返回列表
        </Button>
        <h2>工单详情</h2>
      </div>

      <div className="detail-body">
        <div className="detail-main">
          <div className="detail-header-card">
            <div className="detail-header-top">
              <div className="detail-header-left">
                <span className={`priority-pill ${priorityBadgeClass(detail.priority)}`}>
                  {priorityBadgeText(detail.priority)}
                </span>
                <span style={{ fontSize: 14, fontWeight: 500, color: detail.priority === 'URGENT' ? '#dc2626' : detail.priority === 'HIGH' ? '#d97706' : '#6b7280', marginRight: 8 }}>
                  {priorityLabel(detail.priority)}
                </span>
                <StarTwoTone twoToneColor="#f59e0b" style={{ fontSize: 16 }} />
                <span className="detail-ticket-no">{detail.ticketNo}</span>
              </div>
              <div className="detail-header-actions">
                {canClaimCurrentTicket && (
                  <Button type="primary" size="small" icon={<UserPlus size={14} />} loading={claimSending} onClick={onClaimTicket}>
                    领取工单
                  </Button>
                )}
                <Button size="small" icon={<SwapOutlined />} disabled={!canOperateCurrentTicket} onClick={onOpenAssign}>
                  转派
                </Button>
                <Button size="small" icon={<FlagOutlined />} disabled={!canOperateCurrentTicket} onClick={onOpenPriority}>
                  修改优先级
                </Button>
                <Button size="small" disabled={!canOperateCurrentTicket} onClick={onOpenStatus}>
                  修改状态
                </Button>
                <Button size="small" icon={<CloseCircleOutlined />} disabled={!canOperateCurrentTicket} onClick={onCloseTicket}>
                  关闭工单
                </Button>
                <Button size="small" icon={<EllipsisOutlined />} disabled={!canOperateCurrentTicket}>更多</Button>
              </div>
            </div>
            <h1 className="detail-subject">{detail.subject}</h1>
            <div className="detail-meta">
              <span>来源：<b>{detail.mailboxName || '客户邮件'}</b></span>
              <span>创建时间：<b>{dayjs(detail.createdAt).format('YYYY-MM-DD HH:mm')}</b></span>
              <span>更新时间：<b>{dayjs(detail.updatedAt).format('YYYY-MM-DD HH:mm')}</b></span>
              <Tag color={detail.slaBreached ? 'red' : detail.status === 'CLOSED' ? 'default' : 'blue'}>
                {statusLabel(detail.status)}
              </Tag>
              {detail.linkSuspect && <Tag color="warning" style={{ marginLeft: 4 }}>疑似断链</Tag>}
            </div>
          </div>

          <div className="detail-content-card">
            <Tabs
              activeKey={tab}
              onChange={onTabChange}
              items={[
                {
                  key: 'mail',
                  label: '邮件会话',
                  children: (
                    <div className="detail-mail-conversation">
                      {detail.messages.length > 0 && (
                        <div className="msg-filter-bar">
                          <div className="msg-filter-left">
                            <Segmented
                              size="small"
                              value={msgFilter}
                              onChange={(value) => onMsgFilterChange(String(value))}
                              options={[
                                { label: `全部邮件 (${detail.messages.filter((msg) => messageDirection(msg) !== 'INTERNAL').length})`, value: 'ALL' },
                                { label: `客户 (${detail.messages.filter((msg) => messageDirection(msg) === 'INBOUND').length})`, value: 'INBOUND' },
                                { label: `客服 (${detail.messages.filter((msg) => messageDirection(msg) === 'OUTBOUND').length})`, value: 'OUTBOUND' },
                              ]}
                            />
                          </div>
                          <div className="msg-filter-right" onClick={() => onMsgSortAscChange(!msgSortAsc)}>
                            <span>排序：</span>
                            <span style={{ fontWeight: 500, color: '#1f2937', cursor: 'pointer' }}>
                              {msgSortAsc ? '时间升序' : '时间降序'}
                              <span style={{ marginLeft: 4, fontSize: 11, color: '#9ca3af' }}>
                                {msgSortAsc ? '↑' : '↓'}
                              </span>
                            </span>
                          </div>
                        </div>
                      )}

                      {detail.messages.length === 0 ? (
                        <Empty description="暂无邮件消息" style={{ padding: '40px 0' }} />
                      ) : (
                        [...detail.messages]
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
                          .map((msg) => {
                            const dir = messageDirection(msg)
                            const isAgent = dir === 'OUTBOUND'
                            const isAuto = false
                            const displayName = msg.displayName || (msg.fromAddress ? msg.fromAddress.split('@')[0] : '')
                            const firstChar = displayName ? displayName[0].toUpperCase() : isAuto ? 'S' : 'C'
                            const msgAttachments = ticketAttachments.filter((attachment) => attachment.messageId === msg.id)

                            return (
                              <div key={msg.id} className={`msg-card ${isAuto ? 'msg-system' : isAgent ? 'msg-agent' : 'msg-customer'}`}>
                                <div className="msg-avatar">
                                  <div className={`msg-avatar-circle ${isAuto ? 'avatar-system' : isAgent ? 'avatar-agent' : 'avatar-customer'}`}>
                                    {firstChar}
                                  </div>
                                </div>
                                <div className="msg-content">
                                  <div className="msg-header">
                                    <div className="msg-header-left">
                                      <span className="msg-from">{msg.fromAddress || '系统'}</span>
                                      <span className={`msg-badge ${isAuto ? 'badge-system' : isAgent ? 'badge-agent' : 'badge-customer'}`}>
                                        {isAuto ? '系统' : isAgent ? '客服' : '客户'}
                                      </span>
                                    </div>
                                    <span className="msg-time">{msg.sentAt ? dayjs(msg.sentAt).format('YYYY-MM-DD HH:mm') : ''}</span>
                                  </div>
                                  {msg.toAddress && <div className="msg-to">收件人：{msg.toAddress}</div>}
                                  <div className="msg-body">{msgBodyText(msg)}</div>
                                  {msgAttachments.length > 0 && (
                                    <div className="msg-attachments">
                                      {msgAttachments.map((attachment) => (
                                        <div key={attachment.id} className="msg-attachment-item">
                                          <span className="msg-attachment-icon">📎</span>
                                          <a href={attachment.downloadUrl || undefined} target="_blank" rel="noopener noreferrer" className="msg-attachment-link">
                                            {attachment.fileName}
                                          </a>
                                          <span className="msg-attachment-size">({formatFileSize(attachment.fileSize)})</span>
                                        </div>
                                      ))}
                                    </div>
                                  )}
                                </div>
                              </div>
                            )
                          })
                      )}

                      <div className="detail-editor">
                        <div style={{ fontSize: 13, fontWeight: 600, color: '#1f2937', marginBottom: 8 }}>回复客户</div>
                        {!canOperateCurrentTicket && !isCurrentTicketTerminal && (
                          <Alert
                            type="info"
                            showIcon
                            title={isCurrentTicketUnassigned ? '领取后即可处理该工单' : '当前账号不可操作该工单'}
                            style={{ marginBottom: 10 }}
                          />
                        )}
                        <TiptapRichEditor
                          placeholder="请输入回复内容（将发送邮件给客户）..."
                          disabled={!canOperateCurrentTicket}
                          onUpdate={onReplyUpdate}
                        />
                        {uploadedFiles.length > 0 && (
                          <div style={{ margin: '6px 0', display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                            {uploadedFiles.map((file) => (
                              <Tag key={file.objectKey} closable onClose={() => onRemoveUploadedFile(file.objectKey)} style={{ fontSize: 12, margin: 0 }}>
                                📎 {file.fileName} ({formatFileSize(file.fileSize)})
                              </Tag>
                            ))}
                          </div>
                        )}
                        <div className="detail-editor-actions">
                          <div>
                            <input
                              type="file"
                              ref={fileInputRef}
                              style={{ display: 'none' }}
                              onChange={(event) => {
                                const file = event.target.files?.[0]
                                if (file) {
                                  onUploadFile(file)
                                  event.target.value = ''
                                }
                              }}
                            />
                            <Button
                              type="text"
                              icon={<PaperClipOutlined />}
                              loading={uploadingFile}
                              disabled={!canOperateCurrentTicket}
                              onClick={() => fileInputRef.current?.click()}
                            >
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
                      </div>
                    </div>
                  ),
                },
                {
                  key: 'log',
                  label: '工单日志',
                  children: events.length > 0 ? (
                    <Timeline
                      items={events.map((event) => ({
                        color: 'blue',
                        content: (
                          <div>
                            <div style={{ fontWeight: 500, color: '#1f2937' }}>{event.eventContent}</div>
                            <div style={{ fontSize: 12, color: '#9ca3af' }}>{event.operator} · {dayjs(event.eventAt).format('YYYY-MM-DD HH:mm')}</div>
                          </div>
                        ),
                      }))}
                    />
                  ) : (
                    <Empty description="暂无工单日志" style={{ padding: '40px 0' }} />
                  ),
                },
                {
                  key: 'customer',
                  label: '客户信息',
                  children: (
                    <Descriptions column={1} size="small" style={{ padding: '16px 0' }}>
                      <Descriptions.Item label="客户邮箱">{detail.customerEmail}</Descriptions.Item>
                      <Descriptions.Item label="来源邮箱">{detail.mailboxName || `#${detail.mailboxId}`}</Descriptions.Item>
                    </Descriptions>
                  ),
                },
                {
                  key: 'sla',
                  label: 'SLA',
                  children: (
                    <Descriptions column={1} size="small" style={{ padding: '16px 0' }}>
                      <Descriptions.Item label="SLA状态">
                        <Tag color={detail.slaBreached ? 'red' : 'green'}>
                          {detail.slaBreached ? '已超时' : '正常'}
                        </Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="首次响应截止">
                        {detail.slaResponseDeadline ? dayjs(detail.slaResponseDeadline).format('YYYY-MM-DD HH:mm') : '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="解决截止">
                        {detail.slaResolveDeadline ? dayjs(detail.slaResolveDeadline).format('YYYY-MM-DD HH:mm') : '-'}
                      </Descriptions.Item>
                    </Descriptions>
                  ),
                },
                {
                  key: 'attachment',
                  label: `附件 (${ticketAttachments.length})`,
                  children: (
                    <div className="detail-attachments">
                      {ticketAttachments.length === 0 ? (
                        <Empty description="暂无附件" style={{ padding: '40px 0' }} />
                      ) : (
                        <div className="attachment-grid">
                          {ticketAttachments.map((attachment) => (
                            <div key={attachment.id} className="attachment-card">
                              <div className="attachment-icon">
                                {attachment.contentType?.startsWith('image/') ? '🖼' : '📄'}
                              </div>
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
          </div>
        </div>

        <aside className="detail-sidebar">
          <Card size="small" title="工单信息">
            <Descriptions column={1} size="small">
              <Descriptions.Item label="负责人">
                <Space>
                  {detail.assigneeName ? <Avatar size="small" style={{ backgroundColor: '#10b981' }}>{detail.assigneeName[0]}</Avatar> : null}
                  {detail.assigneeName || '未分配'}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="客户">{detail.customerEmail}</Descriptions.Item>
              <Descriptions.Item label="优先级">{priorityLabel(detail.priority)}</Descriptions.Item>
              <Descriptions.Item label="状态">{statusLabel(detail.status)}</Descriptions.Item>
              <Descriptions.Item label="来源">{detail.mailboxName || '客户邮件'}</Descriptions.Item>
              <Descriptions.Item label="备注">
                <Input.TextArea
                  rows={2}
                  size="small"
                  value={remarkDraft}
                  onChange={(event) => setRemarkDraft(event.target.value)}
                  disabled={!canOperateCurrentTicket}
                  placeholder="点击添加备注..."
                  style={{ fontSize: 12 }}
                  onBlur={(event) => {
                    if (!canOperateCurrentTicket) return
                    const value = event.target.value.trim()
                    if (value !== (detail.remark || '')) onSaveRemark(value)
                  }}
                />
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card
            size="small"
            title="SLA 信息"
            extra={(
              <Tag color={detail.slaBreached ? 'red' : 'green'}>
                {detail.slaBreached ? '已超时' : '正常'}
              </Tag>
            )}
          >
            <Descriptions column={1} size="small">
              <Descriptions.Item label="首次响应截止">
                {detail.slaResponseDeadline ? dayjs(detail.slaResponseDeadline).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="解决截止">
                {detail.slaResolveDeadline ? dayjs(detail.slaResolveDeadline).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card size="small" title="工单生命周期">
            {events.length > 0 ? (
              <Timeline
                items={events.map((event) => ({
                  color: 'blue',
                  content: (
                    <div>
                      <div style={{ fontWeight: 500, color: '#1f2937', fontSize: 13 }}>{event.eventContent}</div>
                      <div style={{ fontSize: 11, color: '#9ca3af' }}>{dayjs(event.eventAt).format('YYYY-MM-DD HH:mm')}</div>
                    </div>
                  ),
                }))}
              />
            ) : (
              <Empty description="暂无记录" />
            )}
          </Card>
        </aside>
      </div>
    </div>
  )
}
