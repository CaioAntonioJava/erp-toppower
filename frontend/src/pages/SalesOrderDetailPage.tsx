import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ChevronRight,
  FileEdit,
  Printer,
  X,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { BackButton } from '../components/ui/BackButton'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { SalesOrderStatusBadge } from '../components/sales/SalesOrderStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  advanceSalesOrderStatus,
  cancelSalesOrder,
  getSalesOrder,
} from '../api/salesOrder.api'
import { getProduct } from '../api/product.api'
import { toApiError } from '../lib/errors'
import type { SalesOrderResponse, SalesOrderStatus } from '../types/salesOrder'
import { SALES_ORDER_STATUS_LABELS } from '../types/salesOrder'
import {
  DISCOUNT_TYPE_LABELS,
  FREIGHT_TYPE_LABELS,
  PAYMENT_CONDITION_LABELS,
  SALES_ORDER_CLIENT_TYPE_LABELS,
} from '../types/salesOrder'
import { useAuth } from '../context/AuthContext'

/** Formatador de moeda BRL. */
const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDiscount(
  type: string | null | undefined,
  value: number | null | undefined,
): string {
  if (type == null || value == null) return '—'
  if (type === 'PERCENT') return `${value}%`
  return brlFormatter.format(value)
}

/**
 * Próximo status no ciclo do pedido, espelhando `nextStatus` do backend.
 * Retorna null quando não há avanço possível (FINALIZADO ou CANCELADO).
 */
function nextStatus(status: SalesOrderStatus): SalesOrderStatus | null {
  switch (status) {
    case 'ABERTO':
      return 'FINALIZADO'
    default:
      return null
  }
}

export function SalesOrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [salesOrder, setSalesOrder] = useState<SalesOrderResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [canceling, setCanceling] = useState(false)
  const [cancelError, setCancelError] = useState<string | null>(null)
  const [advancing, setAdvancing] = useState(false)
  const [advanceError, setAdvanceError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getSalesOrder(id)
      .then((data) => {
        if (cancelled) return
        setSalesOrder(data)
      })
      .catch((err) => {
        if (cancelled) return
        setError(toApiError(err).message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id])

  // === resolução de nomes (produtos) ===
  // O `SalesOrderResponse` já traz o nome do cliente (`clientName`/`clientCode`)
  // e do vendedor (`sellerName`) resolvidos no backend. Apenas os nomes de
  // produtos ainda precisam ser hidratados no frontend.
  // Mantemos o UUID curto como fallback caso a resolução falhe.
  const [productNames, setProductNames] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!salesOrder) return
    let cancelled = false

    // Produtos — dedupe por UUID para não buscar o mesmo item duas vezes.
    const uniqueProductUuids = Array.from(
      new Set(salesOrder.items.map((it) => it.productUuid)),
    )
    const productEntriesPromise = Promise.all(
      uniqueProductUuids.map(async (uuid) => {
        try {
          const p = await getProduct(uuid)
          const label = p.code ? `${p.code} — ${p.name}` : p.name
          return [uuid, label] as const
        } catch {
          return [uuid, null] as const
        }
      }),
    ).then((entries) => {
      const map: Record<string, string> = {}
      for (const [uuid, label] of entries) {
        if (label != null) map[uuid] = label
      }
      return map
    })

    productEntriesPromise
      .then((products) => {
        if (cancelled) return
        setProductNames(products)
      })
      .catch(() => {
        /* mantém fallbacks (UUID curto) */
      })

    return () => {
      cancelled = true
    }
  }, [salesOrder])

  async function handleCancel() {
    if (!salesOrder) return
    setCanceling(true)
    setCancelError(null)
    try {
      const updated = await cancelSalesOrder(salesOrder.uuid)
      setSalesOrder(updated)
      setConfirmCancel(false)
    } catch (err) {
      setCancelError(toApiError(err).message)
    } finally {
      setCanceling(false)
    }
  }

  async function handleAdvance() {
    if (!salesOrder) return
    setAdvancing(true)
    setAdvanceError(null)
    try {
      const updated = await advanceSalesOrderStatus(salesOrder.uuid)
      setSalesOrder(updated)
    } catch (err) {
      setAdvanceError(toApiError(err).message)
    } finally {
      setAdvancing(false)
    }
  }

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  if (error || !salesOrder) {
    return (
      <div className="space-y-4">
        <BackButton
          variant="ghost"
          label="Voltar para a lista"
          fallback="/sales-orders"
        />
        <Alert variant="error">
          {error ?? 'Pedido não encontrado.'}
        </Alert>
      </div>
    )
  }

  // Edição permitida apenas enquanto o pedido está ABERTO.
  const canEdit = salesOrder.status === 'ABERTO'
  // Cancelamento permitido para ABERTO e FINALIZADO. Cancelar um
  // FINALIZADO estorna automaticamente o estoque (backend). CANCELADO é
  // terminal.
  const canCancel =
    salesOrder.status === 'ABERTO' || salesOrder.status === 'FINALIZADO'
  // Avanço de status permitido enquanto houver próximo estado.
  const next = nextStatus(salesOrder.status)
  const canAdvance = next != null

  // UUID "curto" usado como fallback visual quando o nome real não está
  // disponível no payload (cliente inativado/removido após a criação).
  const clientUuid =
    salesOrder.clientType === 'CUSTOMER'
      ? salesOrder.customerUuid
      : salesOrder.companyUuid
  const clientDisplay = salesOrder.clientName
    ? (salesOrder.clientCode
        ? `${salesOrder.clientCode} — ${salesOrder.clientName}`
        : salesOrder.clientName)
    : (clientUuid ? `${clientUuid.slice(0, 8)}…` : '—')

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton fallback="/sales-orders" />
          <h1 className="mt-4 flex items-center gap-3 text-2xl font-semibold tracking-tight">
            <span>Pedido {salesOrder.number}</span>
            <SalesOrderStatusBadge status={salesOrder.status} />
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Emitido em {formatDate(salesOrder.orderDate)}
            {salesOrder.quotationNumber != null
              ? ` • Originado da proposta ${salesOrder.quotationNumber}`
              : ''}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="secondary"
            onClick={() =>
              window.open(`/sales-orders/${salesOrder.uuid}/pdf`, '_blank')
            }
          >
            <Printer className="h-4 w-4" />
            Gerar PDF
          </Button>
          {canAdvance ? (
            <Button
              onClick={handleAdvance}
              isLoading={advancing}
            >
              <ChevronRight className="h-4 w-4" />
              Avançar para {SALES_ORDER_STATUS_LABELS[next!]}
            </Button>
          ) : null}
          {canEdit ? (
            <Button
              variant="secondary"
              onClick={() => navigate(`/sales-orders/${salesOrder.uuid}/edit`)}
            >
              <FileEdit className="h-4 w-4" />
              Editar
            </Button>
          ) : null}
          {canCancel ? (
            <Button
              variant="danger"
              onClick={() => {
                setCancelError(null)
                setConfirmCancel(true)
              }}
            >
              <X className="h-4 w-4" />
              Cancelar pedido
            </Button>
          ) : null}
        </div>
      </div>

      {advanceError ? <Alert variant="error">{advanceError}</Alert> : null}
      {cancelError ? <Alert variant="error">{cancelError}</Alert> : null}

      {/* Resumo */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-4 text-base font-semibold">Resumo</h3>
        <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Tipo de cliente
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {SALES_ORDER_CLIENT_TYPE_LABELS[salesOrder.clientType]}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Cliente
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {clientDisplay}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Vendedor
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {salesOrder.sellerName ?? 'Vendedor não encontrado'}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Condição de pagamento
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {salesOrder.paymentCondition
                ? PAYMENT_CONDITION_LABELS[salesOrder.paymentCondition]
                : '—'}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Desconto global
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {salesOrder.discountType
                ? `${DISCOUNT_TYPE_LABELS[salesOrder.discountType]} — ${formatDiscount(
                    salesOrder.discountType,
                    salesOrder.discount,
                  )}`
                : '—'}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Tipo de frete
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {salesOrder.freightType
                ? FREIGHT_TYPE_LABELS[salesOrder.freightType]
                : '—'}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Transportadora
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {salesOrder.carrierName
                ? `${salesOrder.carrierName}${salesOrder.carrierServiceName ? ` (${salesOrder.carrierServiceName})` : ''}`
                : '—'}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Valor do frete
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {salesOrder.freightValue != null
                ? brlFormatter.format(salesOrder.freightValue)
                : '—'}
            </dd>
          </div>
          {salesOrder.attention ? (
            <div className="sm:col-span-2 lg:col-span-3">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Aos cuidados de
              </dt>
              <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
                {salesOrder.attention}
              </dd>
            </div>
          ) : null}
        </dl>
      </section>

      {/* Itens */}
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="border-b border-slate-200 p-6 dark:border-slate-800">
          <h3 className="text-base font-semibold">Itens</h3>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {salesOrder.items.length} item(ns) • {salesOrder.totalQuantity}{' '}
            unidade(s)
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Produto</th>
                <th className="px-4 py-3 font-medium text-right">Qtd.</th>
                <th className="px-4 py-3 font-medium text-right">Preço un.</th>
                <th className="px-4 py-3 font-medium text-right">Subtotal</th>
                <th className="px-4 py-3 font-medium text-right">Desc.</th>
                <th className="px-4 py-3 font-medium text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {salesOrder.items.map((it) => {
                const productLabel = productNames[it.productUuid]
                return (
                <tr key={it.uuid}>
                  <td className="px-4 py-3">
                    {productLabel != null ? (
                      <span className="text-sm text-slate-800 dark:text-slate-200">
                        {productLabel}
                      </span>
                    ) : (
                      <span
                        className="break-all font-mono text-xs text-slate-500 dark:text-slate-400"
                        title={it.productUuid}
                      >
                        {`${it.productUuid.slice(0, 8)}…`}
                      </span>
                    )}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {it.quantity}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {brlFormatter.format(it.unitPrice)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {brlFormatter.format(it.lineSubtotal)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {formatDiscount(it.discountType, it.discount)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                    {brlFormatter.format(it.totalPrice)}
                  </td>
                </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        <div className="flex flex-col items-stretch gap-2 border-t border-slate-200 p-4 text-sm sm:flex-row sm:items-center sm:justify-end dark:border-slate-800">
          <div className="flex justify-between gap-8 sm:justify-end">
            <span className="text-slate-500 dark:text-slate-400">Subtotal:</span>
            <span className="font-mono font-semibold">
              {brlFormatter.format(salesOrder.subtotal)}
            </span>
          </div>
          <div className="flex justify-between gap-8 sm:justify-end">
            <span className="text-slate-500 dark:text-slate-400">Desconto global:</span>
            <span className="font-mono font-semibold">
              - {brlFormatter.format(salesOrder.globalDiscountValue)}
            </span>
          </div>
          <div className="flex justify-between gap-8 sm:justify-end">
            <span className="text-slate-500 dark:text-slate-400">Frete:</span>
            <span className="font-mono font-semibold">
              {salesOrder.freightValue != null
                ? brlFormatter.format(salesOrder.freightValue)
                : '—'}
            </span>
          </div>
          <div className="flex justify-between gap-8 sm:justify-end">
            <span className="text-slate-500 dark:text-slate-400">Total:</span>
            <span className="font-mono text-base font-semibold text-primary-700 dark:text-primary-200">
              {brlFormatter.format(salesOrder.total)}
            </span>
          </div>
        </div>
      </section>

      {/* Observações */}
      {salesOrder.notes ? (
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <h3 className="mb-2 text-base font-semibold">Observações</h3>
          <p className="whitespace-pre-wrap text-sm text-slate-700 dark:text-slate-200">
            {salesOrder.notes}
          </p>
        </section>
      ) : null}

      {/* Auditoria (admin) */}
      {isAdmin ? (
        <RegistrationAuditCard
          createdBy={salesOrder.createdBy}
          createdAt={salesOrder.createdAt}
          updatedBy={salesOrder.updatedBy}
          updatedAt={formatDateTime(salesOrder.updatedAt)}
        />
      ) : null}

      <ConfirmDialog
        open={confirmCancel}
        title="Cancelar pedido?"
        description={
          salesOrder.status === 'FINALIZADO'
            ? `O pedido ${salesOrder.number} será marcado como CANCELADO. Como já estava finalizado, as saídas de estoque serão estornadas automaticamente, devolvendo o saldo dos itens aos produtos. O registro não é apagado.`
            : `O pedido ${salesOrder.number} será marcado como CANCELADO. O registro não é apagado. Pedidos CANCELADOS não podem ser revertidos.`
        }
        confirmText="Cancelar pedido"
        confirmVariant="danger"
        isLoading={canceling}
        onConfirm={handleCancel}
        onClose={() => {
          if (!canceling) setConfirmCancel(false)
        }}
      />
    </div>
  )
}