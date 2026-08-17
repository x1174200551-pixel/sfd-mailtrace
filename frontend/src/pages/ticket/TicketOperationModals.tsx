import { Alert, Checkbox, Input, Modal, Select, Switch } from 'antd'
import { priorityOptionLabel } from '../../constants/status'
import type { TicketDetail } from '../../types/ticket'
import type { ManagedUser } from '../../types/user'

type TicketOperationModalsProps = {
  assignModalOpen: boolean
  assignNotifyAssignee: boolean
  assignReason: string
  assignSending: boolean
  assignUserId: number | null
  assignUsers: ManagedUser[]
  canOperateCurrentTicket: boolean
  closeConfirmed: boolean
  closeModalOpen: boolean
  closeReason: string
  closeSending: boolean
  onAssignNotifyChange: (value: boolean) => void
  onAssignReasonChange: (value: string) => void
  onAssignUserChange: (value: number | null) => void
  onCancelAssign: () => void
  onCancelClose: () => void
  onCancelPriority: () => void
  onCancelStatus: () => void
  onCloseConfirmedChange: (value: boolean) => void
  onCloseReasonChange: (value: string) => void
  onPriorityReasonChange: (value: string) => void
  onPriorityValueChange: (value: string) => void
  onStatusReasonChange: (value: string) => void
  onStatusValueChange: (value: string) => void
  onSubmitAssign: () => void
  onSubmitClose: () => void
  onSubmitPriority: () => void
  onSubmitStatus: () => void
  priorityModalOpen: boolean
  priorityReason: string
  prioritySending: boolean
  priorityValue: string
  statusModalOpen: boolean
  statusReason: string
  statusSending: boolean
  statusValue: string
  ticketDetail: TicketDetail | null
}

function ticketMessage(ticketDetail: TicketDetail | null) {
  return ticketDetail ? `${ticketDetail.ticketNo} / ${ticketDetail.subject}` : '当前工单'
}

export function TicketOperationModals({
  assignModalOpen,
  assignNotifyAssignee,
  assignReason,
  assignSending,
  assignUserId,
  assignUsers,
  canOperateCurrentTicket,
  closeConfirmed,
  closeModalOpen,
  closeReason,
  closeSending,
  onAssignNotifyChange,
  onAssignReasonChange,
  onAssignUserChange,
  onCancelAssign,
  onCancelClose,
  onCancelPriority,
  onCancelStatus,
  onCloseConfirmedChange,
  onCloseReasonChange,
  onPriorityReasonChange,
  onPriorityValueChange,
  onStatusReasonChange,
  onStatusValueChange,
  onSubmitAssign,
  onSubmitClose,
  onSubmitPriority,
  onSubmitStatus,
  priorityModalOpen,
  priorityReason,
  prioritySending,
  priorityValue,
  statusModalOpen,
  statusReason,
  statusSending,
  statusValue,
  ticketDetail,
}: TicketOperationModalsProps) {
  return (
    <>
      <Modal
        title="转派工单"
        open={assignModalOpen}
        onCancel={onCancelAssign}
        onOk={onSubmitAssign}
        confirmLoading={assignSending}
        okText="确认转派"
        cancelText="取消"
        okButtonProps={{ disabled: !canOperateCurrentTicket || !assignUserId }}
      >
        <div style={{ padding: '12px 0', display: 'grid', gap: 14 }}>
          <Alert
            type="info"
            showIcon
            title={ticketMessage(ticketDetail)}
            description={`当前处理人：${ticketDetail?.assigneeName || '未分配'}`}
          />
          <div>
            <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>选择处理人</div>
            <Select
              style={{ width: '100%' }}
              placeholder="请选择处理人"
              value={assignUserId}
              onChange={onAssignUserChange}
              disabled={!canOperateCurrentTicket}
              showSearch
              optionFilterProp="label"
              options={assignUsers
                .filter((user) => user.enabled)
                .map((user) => ({
                  label: `${user.displayName} (${user.account}${user.email ? ` / ${user.email}` : ''})`,
                  value: user.id,
                }))}
            />
          </div>
          <div>
            <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>转派原因</div>
            <Input.TextArea
              value={assignReason}
              onChange={(event) => onAssignReasonChange(event.target.value)}
              disabled={!canOperateCurrentTicket}
              maxLength={200}
              rows={3}
              showCount
              placeholder="填写转派原因，保存后会写入工单日志"
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}>
            <div>
              <div style={{ fontSize: 13, fontWeight: 600, color: '#1f2937' }}>通知新处理人</div>
              <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>开启后会发送分配通知邮件；关闭时仅更新工单。</div>
            </div>
            <Switch checked={assignNotifyAssignee} onChange={onAssignNotifyChange} disabled={!canOperateCurrentTicket} />
          </div>
        </div>
      </Modal>

      <Modal
        title="修改优先级"
        open={priorityModalOpen}
        onCancel={onCancelPriority}
        onOk={onSubmitPriority}
        confirmLoading={prioritySending}
        okText="确认修改"
        cancelText="取消"
        okButtonProps={{ disabled: !canOperateCurrentTicket || !ticketDetail || priorityValue === ticketDetail.priority }}
      >
        <div style={{ padding: '12px 0', display: 'grid', gap: 14 }}>
          <Alert
            type="info"
            showIcon
            title={ticketMessage(ticketDetail)}
            description={`当前优先级：${ticketDetail ? priorityOptionLabel(ticketDetail.priority) : '-'}`}
          />
          <div>
            <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>目标优先级</div>
            <Select
              style={{ width: '100%' }}
              value={priorityValue}
              onChange={onPriorityValueChange}
              disabled={!canOperateCurrentTicket}
              options={[
                { label: 'P1 - 紧急', value: 'URGENT' },
                { label: 'P2 - 高', value: 'HIGH' },
                { label: 'P3 - 普通', value: 'NORMAL' },
                { label: 'P4 - 低', value: 'LOW' },
              ]}
            />
          </div>
          <div>
            <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>变更说明</div>
            <Input.TextArea
              value={priorityReason}
              onChange={(event) => onPriorityReasonChange(event.target.value)}
              disabled={!canOperateCurrentTicket}
              maxLength={200}
              rows={3}
              showCount
              placeholder="填写优先级调整原因，保存后会写入工单日志"
            />
          </div>
        </div>
      </Modal>

      <Modal
        title="修改状态"
        open={statusModalOpen}
        onCancel={onCancelStatus}
        onOk={onSubmitStatus}
        confirmLoading={statusSending}
        okText="确认修改"
        cancelText="取消"
        okButtonProps={{ disabled: !canOperateCurrentTicket || !ticketDetail || statusValue === ticketDetail.status }}
      >
        <div style={{ padding: '12px 0', display: 'grid', gap: 14 }}>
          <Alert
            type="warning"
            showIcon
            title={ticketMessage(ticketDetail)}
            description="状态变更会写入生命周期。关闭工单请使用专用关闭确认弹窗；待客户回复由对外回复自动流转。"
          />
          <div>
            <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>目标状态</div>
            <Select
              style={{ width: '100%' }}
              value={statusValue}
              onChange={onStatusValueChange}
              disabled={!canOperateCurrentTicket}
              options={[
                { label: '处理中', value: 'PROCESSING' },
                { label: '已取消', value: 'CANCELLED' },
              ]}
            />
          </div>
          <div>
            <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>变更说明</div>
            <Input.TextArea
              value={statusReason}
              onChange={(event) => onStatusReasonChange(event.target.value)}
              disabled={!canOperateCurrentTicket}
              maxLength={200}
              rows={3}
              showCount
              placeholder="填写状态调整原因，保存后会写入工单日志"
            />
          </div>
        </div>
      </Modal>

      <Modal
        title="关闭工单"
        open={closeModalOpen}
        onCancel={onCancelClose}
        onOk={onSubmitClose}
        confirmLoading={closeSending}
        okText="确认关闭"
        cancelText="取消"
        okButtonProps={{ danger: true, disabled: !canOperateCurrentTicket || !closeConfirmed }}
      >
        <div style={{ padding: '12px 0', display: 'grid', gap: 14 }}>
          <Alert
            type="warning"
            showIcon
            title={ticketMessage(ticketDetail)}
            description="关闭后工单状态会变为已关闭，并写入关闭时间和生命周期事件。客户后续追信将默认关联原单并转回处理中。"
          />
          <div>
            <div style={{ marginBottom: 8, fontSize: 13, color: '#6b7280' }}>关闭说明</div>
            <Input.TextArea
              value={closeReason}
              onChange={(event) => onCloseReasonChange(event.target.value)}
              disabled={!canOperateCurrentTicket}
              maxLength={200}
              rows={3}
              showCount
              placeholder="填写关闭原因或处理结论，保存后会写入工单日志"
            />
          </div>
          <Checkbox checked={closeConfirmed} disabled={!canOperateCurrentTicket} onChange={(event) => onCloseConfirmedChange(event.target.checked)}>
            我确认该工单已处理完成，可以关闭
          </Checkbox>
        </div>
      </Modal>
    </>
  )
}
