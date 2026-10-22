import { CATEGORY_LABELS, ORDER_STATUSES, STATUS_LABELS } from '../api/types'
import type { OrderStatus, SupportOrderSummary } from '../api/types'
import { formatDate } from '../format'
import { PriorityBadge, StatusBadge } from './Badges'

interface OrderListProps {
  orders: SupportOrderSummary[]
  loading: boolean
  error: string | null
  statusFilter: OrderStatus | ''
  selectedId: number | null
  onStatusFilterChange: (status: OrderStatus | '') => void
  onSelect: (id: number) => void
  onRetry: () => void
}

export function OrderList({
  orders,
  loading,
  error,
  statusFilter,
  selectedId,
  onStatusFilterChange,
  onSelect,
  onRetry,
}: OrderListProps) {
  return (
    <section className="card" aria-labelledby="orders-title">
      <div className="card-header">
        <h2 id="orders-title">Órdenes</h2>
        <label className="inline-field">
          <span>Estado</span>
          <select
            value={statusFilter}
            onChange={(event) => onStatusFilterChange(event.target.value as OrderStatus | '')}
          >
            <option value="">Todos</option>
            {ORDER_STATUSES.map((status) => (
              <option key={status} value={status}>
                {STATUS_LABELS[status]}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error ? (
        <div className="alert alert-error" role="alert">
          <span>{error}</span>
          <button type="button" className="button-secondary" onClick={onRetry}>
            Reintentar
          </button>
        </div>
      ) : loading && orders.length === 0 ? (
        <p className="muted" aria-live="polite">
          Cargando órdenes…
        </p>
      ) : orders.length === 0 ? (
        <p className="muted">No hay órdenes{statusFilter ? ' con ese estado' : ''}.</p>
      ) : (
        <table className="orders-table" aria-busy={loading}>
          <thead>
            <tr>
              <th>#</th>
              <th>Referencia</th>
              <th>Categoría</th>
              <th>Prioridad</th>
              <th>Estado</th>
              <th>Creada</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr
                key={order.id}
                className={order.id === selectedId ? 'selected' : undefined}
                onClick={() => onSelect(order.id)}
              >
                <td data-label="#">{order.id}</td>
                <td data-label="Referencia">
                  <button
                    type="button"
                    className="link-button"
                    aria-label={`Ver detalle de la orden ${order.externalReference}`}
                    onClick={(event) => {
                      event.stopPropagation()
                      onSelect(order.id)
                    }}
                  >
                    {order.externalReference}
                  </button>
                </td>
                <td data-label="Categoría">{CATEGORY_LABELS[order.category]}</td>
                <td data-label="Prioridad">
                  <PriorityBadge priority={order.priority} />
                </td>
                <td data-label="Estado">
                  <StatusBadge status={order.status} />
                </td>
                <td data-label="Creada">{formatDate(order.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
