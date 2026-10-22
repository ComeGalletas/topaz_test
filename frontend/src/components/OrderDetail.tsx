import { useEffect, useState } from 'react'
import { errorMessage, supportOrdersApi } from '../api/supportOrders'
import { CATEGORY_LABELS, CHANNEL_LABELS, TRANSITION_ACTION_LABELS } from '../api/types'
import type { OrderStatus, SupportOrderDetail } from '../api/types'
import { formatDate } from '../format'
import { PriorityBadge, StatusBadge } from './Badges'

interface OrderDetailProps {
  orderId: number
  onStatusChanged: (order: SupportOrderDetail) => void
  onClose: () => void
}

export function OrderDetail({ orderId, onStatusChanged, onClose }: OrderDetailProps) {
  const [order, setOrder] = useState<SupportOrderDetail | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [pendingStatus, setPendingStatus] = useState<OrderStatus | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  // App monta un OrderDetail nuevo por cada orden (key={orderId}), así que el estado empieza vacío.
  useEffect(() => {
    let ignore = false
    supportOrdersApi
      .get(orderId)
      .then((loaded) => !ignore && setOrder(loaded))
      .catch((error: unknown) => !ignore && setLoadError(errorMessage(error)))
    return () => {
      ignore = true
    }
  }, [orderId, reloadKey])

  function retryLoad() {
    setLoadError(null)
    setReloadKey((key) => key + 1)
  }

  async function changeStatus(target: OrderStatus) {
    if (target === 'CANCELLED' && !window.confirm('¿Cancelar la orden? Es un estado final.')) {
      return
    }
    setPendingStatus(target)
    setActionError(null)
    try {
      const updated = await supportOrdersApi.changeStatus(orderId, target)
      setOrder(updated)
      onStatusChanged(updated)
    } catch (error) {
      // Si otro operador cambió la orden, la respuesta 409 lo explica; recargamos para mostrar el estado real.
      setActionError(errorMessage(error))
      supportOrdersApi.get(orderId).then(setOrder).catch(() => undefined)
    } finally {
      setPendingStatus(null)
    }
  }

  return (
    <section className="card" aria-labelledby="detail-title" aria-busy={!order && !loadError}>
      <div className="card-header">
        <h2 id="detail-title">Orden #{orderId}</h2>
        <button type="button" className="button-secondary" onClick={onClose}>
          Cerrar
        </button>
      </div>

      {loadError ? (
        <div className="alert alert-error" role="alert">
          <span>{loadError}</span>
          <button type="button" className="button-secondary" onClick={retryLoad}>
            Reintentar
          </button>
        </div>
      ) : !order ? (
        <p className="muted">Cargando detalle…</p>
      ) : (
        <>
          <dl className="detail-grid">
            <dt>Referencia</dt>
            <dd>{order.externalReference}</dd>
            <dt>Cliente</dt>
            <dd>{order.customerId}</dd>
            <dt>Estado</dt>
            <dd>
              <StatusBadge status={order.status} />
            </dd>
            <dt>Prioridad</dt>
            <dd>
              <PriorityBadge priority={order.priority} />
            </dd>
            <dt>Categoría</dt>
            <dd>{CATEGORY_LABELS[order.category]}</dd>
            <dt>Canal</dt>
            <dd>{CHANNEL_LABELS[order.channel]}</dd>
            <dt>Creada</dt>
            <dd>{formatDate(order.createdAt)}</dd>
            <dt>Actualizada</dt>
            <dd>{formatDate(order.updatedAt)}</dd>
          </dl>

          <h3>Descripción</h3>
          <p className="description">{order.description}</p>

          <h3>Acciones</h3>
          {actionError && (
            <div className="alert alert-error" role="alert">
              {actionError}
            </div>
          )}
          {order.allowedTransitions.length === 0 ? (
            <p className="muted">La orden está en un estado final; no admite más cambios.</p>
          ) : (
            <div className="actions">
              {order.allowedTransitions.map((target) => (
                <button
                  key={target}
                  type="button"
                  className={target === 'CANCELLED' ? 'button-danger' : undefined}
                  disabled={pendingStatus !== null}
                  onClick={() => changeStatus(target)}
                >
                  {pendingStatus === target ? 'Actualizando…' : TRANSITION_ACTION_LABELS[target]}
                </button>
              ))}
            </div>
          )}
        </>
      )}
    </section>
  )
}
