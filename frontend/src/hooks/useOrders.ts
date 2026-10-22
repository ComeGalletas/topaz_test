import { useEffect, useState } from 'react'
import { errorMessage, supportOrdersApi } from '../api/supportOrders'
import type { OrderStatus, SupportOrderSummary } from '../api/types'

/** Resultado de la última carga; `requestKey` indica a qué filtro/recarga corresponde. */
interface ListResult {
  requestKey: string
  orders: SupportOrderSummary[]
  error: string | null
}

/**
 * Carga el listado de órdenes para un filtro de estado. Descarta respuestas de filtros anteriores y,
 * mientras llega la nueva, mantiene visibles las órdenes previas.
 */
export function useOrders(statusFilter: OrderStatus | '') {
  const [reloadKey, setReloadKey] = useState(0)
  const [result, setResult] = useState<ListResult | null>(null)

  const requestKey = `${statusFilter}#${reloadKey}`
  useEffect(() => {
    let ignore = false
    supportOrdersApi
      .list(statusFilter || undefined)
      .then((orders) => !ignore && setResult({ requestKey, orders, error: null }))
      .catch(
        (error: unknown) =>
          !ignore && setResult((previous) => ({ requestKey, orders: previous?.orders ?? [], error: errorMessage(error) })),
      )
    return () => {
      ignore = true
    }
  }, [statusFilter, requestKey])

  const loading = result?.requestKey !== requestKey
  return {
    orders: result?.orders ?? [],
    loading,
    error: loading ? null : (result?.error ?? null),
    reload: () => setReloadKey((key) => key + 1),
  }
}
