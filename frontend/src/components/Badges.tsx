import { PRIORITY_LABELS, STATUS_LABELS } from '../api/types'
import type { OrderStatus, Priority } from '../api/types'

export function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return <span className={`badge priority-${priority.toLowerCase()}`}>{PRIORITY_LABELS[priority]}</span>
}
