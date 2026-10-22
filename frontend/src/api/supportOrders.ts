import type {
  CreateSupportOrderRequest,
  OrderStatus,
  SupportOrderDetail,
  SupportOrderSummary,
} from './types'

// En desarrollo Vite redirige /api al backend; VITE_API_URL permite apuntar a otro host.
const BASE_URL = `${import.meta.env.VITE_API_URL ?? ''}/api/support-orders`

/** Error de la API con el cuerpo RFC 7807 ya interpretado. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(message: string, status: number, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

interface ProblemDetail {
  title?: string
  detail?: string
  errors?: Record<string, string>
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (init.body) {
    headers['Content-Type'] = 'application/json'
  }

  let response: Response
  try {
    response = await fetch(BASE_URL + path, { ...init, headers })
  } catch {
    throw new ApiError('No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.', 0)
  }

  if (!response.ok) {
    throw await toApiError(response)
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as ProblemDetail
    const message = problem.detail ?? problem.title ?? `Error ${response.status}`
    return new ApiError(message, response.status, problem.errors ?? {})
  } catch {
    return new ApiError(`Error inesperado del servidor (${response.status}).`, response.status)
  }
}

export const supportOrdersApi = {
  list: (status?: OrderStatus) =>
    request<SupportOrderSummary[]>(status ? `?status=${encodeURIComponent(status)}` : ''),

  get: (id: number) => request<SupportOrderDetail>(`/${id}`),

  create: (order: CreateSupportOrderRequest) =>
    request<SupportOrderDetail>('', { method: 'POST', body: JSON.stringify(order) }),

  changeStatus: (id: number, status: OrderStatus) =>
    request<SupportOrderDetail>(`/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Ocurrió un error inesperado.'
}
