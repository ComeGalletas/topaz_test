import { useEffect, useRef, useState } from 'react'
import { STATUS_LABELS } from './api/types'
import type { OrderStatus, SupportOrderDetail } from './api/types'
import { CreateOrderForm } from './components/CreateOrderForm'
import { OrderDetail } from './components/OrderDetail'
import { OrderList } from './components/OrderList'
import { useOrders } from './hooks/useOrders'

type Panel = { kind: 'none' } | { kind: 'create' } | { kind: 'detail'; orderId: number }

interface Notice {
  id: number
  text: string
}

function App() {
  const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>('')
  const { orders, loading, error: listError, reload: reloadList } = useOrders(statusFilter)
  const [panel, setPanel] = useState<Panel>({ kind: 'none' })
  const [notice, setNotice] = useState<Notice | null>(null)

  // En pantallas estrechas el panel queda debajo del listado: lo llevamos a la vista al abrirlo.
  const panelRef = useRef<HTMLElement>(null)
  const panelKey = panel.kind === 'detail' ? `detail-${panel.orderId}` : panel.kind
  useEffect(() => {
    if (panelKey !== 'none' && window.matchMedia('(max-width: 959px)').matches) {
      panelRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [panelKey])

  useEffect(() => {
    if (!notice) return
    const timer = window.setTimeout(() => setNotice(null), 4000)
    return () => window.clearTimeout(timer)
  }, [notice])

  const showNotice = (text: string) => setNotice({ id: Date.now(), text })

  function handleCreated(order: SupportOrderDetail) {
    showNotice(`Orden ${order.externalReference} creada.`)
    setPanel({ kind: 'detail', orderId: order.id })
    reloadList()
  }

  function handleStatusChanged(order: SupportOrderDetail) {
    showNotice(`Orden ${order.externalReference} → ${STATUS_LABELS[order.status]}.`)
    reloadList()
  }

  const selectedId = panel.kind === 'detail' ? panel.orderId : null

  return (
    <>
      <header className="app-header">
        <div className="container header-content">
          <h1>Órdenes de soporte</h1>
          <button type="button" onClick={() => setPanel({ kind: 'create' })}>
            + Nueva orden
          </button>
        </div>
      </header>

      <main className="container">
        <div className="notice-area" aria-live="polite">
          {notice && (
            <div key={notice.id} className="alert alert-success">
              {notice.text}
            </div>
          )}
        </div>

        <div className={`layout${panel.kind === 'none' ? '' : ' with-panel'}`}>
          <OrderList
            orders={orders}
            loading={loading}
            error={listError}
            statusFilter={statusFilter}
            selectedId={selectedId}
            onStatusFilterChange={setStatusFilter}
            onSelect={(orderId) => setPanel({ kind: 'detail', orderId })}
            onRetry={reloadList}
          />

          {panel.kind !== 'none' && (
            <aside className="panel" ref={panelRef}>
              {panel.kind === 'create' ? (
                <CreateOrderForm onCreated={handleCreated} onCancel={() => setPanel({ kind: 'none' })} />
              ) : (
                <OrderDetail
                  key={panel.orderId}
                  orderId={panel.orderId}
                  onStatusChanged={handleStatusChanged}
                  onClose={() => setPanel({ kind: 'none' })}
                />
              )}
            </aside>
          )}
        </div>
      </main>
    </>
  )
}

export default App
