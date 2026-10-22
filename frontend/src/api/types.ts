// Contrato de la API REST (ver backend/.../api/dto). Los valores deben coincidir con los enums de Java.

export const ORDER_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED'] as const
export type OrderStatus = (typeof ORDER_STATUSES)[number]

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'] as const
export type Priority = (typeof PRIORITIES)[number]

export const CATEGORIES = ['PAYMENT', 'ACCOUNT', 'TECHNICAL', 'SERVICE', 'OTHER'] as const
export type Category = (typeof CATEGORIES)[number]

export const CHANNELS = ['MOBILE_APP', 'WEB', 'CALL_CENTER', 'EMAIL'] as const
export type Channel = (typeof CHANNELS)[number]

export interface SupportOrderSummary {
  id: number
  externalReference: string
  category: Category
  priority: Priority
  status: OrderStatus
  createdAt: string
}

export interface SupportOrderDetail extends SupportOrderSummary {
  customerId: string
  description: string
  channel: Channel
  updatedAt: string
  allowedTransitions: OrderStatus[]
}

export interface CreateSupportOrderRequest {
  externalReference: string
  customerId: string
  category: Category
  description: string
  channel: Channel
  priority?: Priority
}

export const STATUS_LABELS: Record<OrderStatus, string> = {
  OPEN: 'Abierta',
  IN_PROGRESS: 'En progreso',
  RESOLVED: 'Resuelta',
  CANCELLED: 'Cancelada',
}

export const PRIORITY_LABELS: Record<Priority, string> = {
  LOW: 'Baja',
  MEDIUM: 'Media',
  HIGH: 'Alta',
}

export const CATEGORY_LABELS: Record<Category, string> = {
  PAYMENT: 'Pagos',
  ACCOUNT: 'Cuenta',
  TECHNICAL: 'Técnica',
  SERVICE: 'Servicio',
  OTHER: 'Otra',
}

export const CHANNEL_LABELS: Record<Channel, string> = {
  MOBILE_APP: 'App móvil',
  WEB: 'Web',
  CALL_CENTER: 'Call center',
  EMAIL: 'Correo',
}

/** Texto del botón que lleva la orden a cada estado. */
export const TRANSITION_ACTION_LABELS: Record<OrderStatus, string> = {
  OPEN: 'Reabrir',
  IN_PROGRESS: 'Iniciar atención',
  RESOLVED: 'Marcar como resuelta',
  CANCELLED: 'Cancelar orden',
}
