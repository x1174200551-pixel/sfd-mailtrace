export const TICKET_STATUS_LABELS: Record<string, string> = {
  PENDING_ASSIGN: '待处理',
  PROCESSING: '处理中',
  WAITING_CUSTOMER: '待客户回复',
  CLOSED: '已关闭',
  CANCELLED: '已取消',
}

export function statusLabel(status: string) {
  return TICKET_STATUS_LABELS[status] || status
}

export function priorityLabel(priority: string) {
  return ({ LOW: '低', NORMAL: '普通', HIGH: '高', URGENT: '紧急' } as Record<string, string>)[priority] || priority
}

export function priorityOptionLabel(priority: string) {
  return ({ URGENT: 'P1 - 紧急', HIGH: 'P2 - 高', NORMAL: 'P3 - 普通', LOW: 'P4 - 低' } as Record<string, string>)[priority] || priority
}

export function priorityBadgeText(priority: string) {
  return ({ URGENT: 'P1', HIGH: 'P2', NORMAL: 'P3', LOW: 'P4' } as Record<string, string>)[priority] || 'P3'
}

export function priorityBadgeClass(priority: string) {
  return ({ URGENT: 'p1', HIGH: 'p2', NORMAL: 'p3', LOW: 'p4' } as Record<string, string>)[priority] || 'p3'
}
