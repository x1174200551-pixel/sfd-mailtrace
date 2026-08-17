import { TICKET_STATUS_LABELS } from '../constants/status'
import type { TicketEvent } from '../types/ticket'

function formatTicketEventContent(content: string) {
  return Object.entries(TICKET_STATUS_LABELS).reduce(
    (text, [status, label]) => text.replaceAll(status, label),
    content,
  )
}

export function getVisibleTicketEvents(events: TicketEvent[]) {
  const hasClosedEvent = events.some(ev => ev.eventType === 'CLOSED')
  return events
    .filter(ev => !(hasClosedEvent && ev.eventType === 'STATUS_CHANGED' && formatTicketEventContent(ev.eventContent).includes('→ 已关闭')))
    .map(ev => ({ ...ev, eventContent: formatTicketEventContent(ev.eventContent) }))
}

export function isTerminalTicket(status: string) {
  return status === 'CLOSED' || status === 'CANCELLED'
}
