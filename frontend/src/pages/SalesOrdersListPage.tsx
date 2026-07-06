import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Eye,
  Plus,
  Printer,
  Search,
  X,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { SalesOrderStatusBadge } from '../components/sales/SalesOrderStatusBadge'
import { cancelSalesOrder, listSalesOrders } from '../api/salesOrder.api'
import { toApiError } from '../lib/errors'
import type {
  SalesOrderStatus,
  SalesOrderSummaryResponse,
} from '../types/salesOrder'
import {
  SALES_ORDER_CLIENT_TYPE_LABELS,
  SALES_ORDER_STATUS_LABELS,
} from '../types/salesOrder'
import type { PagedResponse } from '../types/api'
import { useAuth } from '../context/AuthContext'

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Todos os status' },
  { value: 'ABERTO', label: SALES_ORDER_STATUS_LABELS.ABERTO },
  { value: 'FINALIZADO', label: SALES_ORDER_STATUS_LABELS.FINALIZADO },
  { value: 'CANCELADO', label: SALES_ORDER_STATUS_LABELS.CANCELADO },
]

/** Formatador de moeda BRL. */
const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

/** Formata data ISO (yyyy-MM-dd) em pt-BR. */
function formatDate(iso: string | null | undefined): string {
  if (!iso) return ''
  // Datas do backend vêm como "2026-07-02" (sem hora) ou ISO completo.
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

export function SalesOrdersListPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  // Filtros
  const [status, setStatus] = useState<SalesOrderStatus | 'ALL'>('ALL')
  const [number, setNumber] = useState('')
  const [debouncedNumber, setDebouncedNumber] = useState('')
  const [page, setPage] = useState(0)
  const size = 10

  const [data, setData] = useState<PagedResponse<SalesOrderSummaryResponse> | null>(
    null,
  )
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Cancelamento
  const [confirmCancel, setConfirmCancel] =
    useState<SalesOrderSummaryResponse | null>(null)
  const [canceling, setCanceling] = useState(false)

  // Debounce do filtro por número.
  useEffect(() => {
    const t = setTimeout(() => setDebouncedNumber(number.trim()), 300)
    return () => clearTimeout(t)
  }, [number])

  // Reset de página ao mudar filtros.
  useEffect(() => {
    setPage(0)
  }, [status, debouncedNumber])

  // Carregamento.
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    const params: Parameters<typeof listSalesOrders>[0] = { page, size }
    if (status !== 'ALL') params.status = status
    if (debouncedNumber.length > 0) params.number = debouncedNumber
    listSalesOrders(params)
      .then((result) => {
        if (cancelled) return
        setData(result)
      })
      .catch((err) => {
        if (cancelled) return
        setError(toApiError(err).message)
        setData(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [status, debouncedNumber, page])

  async function handleCancel() {
    if (!confirmCancel) return
    setCanceling(true)
    setError(null)
    try {
      await cancelSalesOrder(confirmCancel.uuid)
      setConfirmCancel(null)
      // Recarrega a página atual.
      const params: Parameters<typeof listSalesOrders>[0] = { page, size }
      if (status !== 'ALL') params.status = status
      if (debouncedNumber.length > 0) params.number = debouncedNumber
      const result = await listSalesOrders(params)
      setData(result)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setCanceling(false)
    }
  }

  // Pedidos ABERTO e FINALIZADO podem ser cancelados. Cancelar um
  // FINALIZADO estorna automaticamente o estoque (backend). CANCELADO é
  // terminal.
  const canCancel = (o: SalesOrderSummaryResponse) =>
    o.status === 'ABERTO' || o.status === 'FINALIZADO'

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Pedidos de Venda
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Gestão de pedidos (conversão de propostas ou criação direta).
            {isAdmin ? (
              <span className="ml-2 inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
                Visão ADMIN
              </span>
            ) : null}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/sales-orders/new">
            <Button>
              <Plus className="h-4 w-4" />
              Novo pedido
            </Button>
          </Link>
        </div>
      </div>

      {/* Filtros */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid gap-3 sm:grid-cols-[1fr_220px]">
          <Input
            placeholder="Buscar por número (ex.: 1000)…"
            value={number}
            onChange={(e) => setNumber(e.target.value)}
            leftAdornment={<Search className="h-4 w-4" />}
            hint={
              number.trim().length > 0 && number.trim().length < 2
                ? 'Digite ao menos 2 caracteres para buscar.'
                : undefined
            }
          />
          <Select
            options={STATUS_OPTIONS}
            value={status}
            onChange={(e) =>
              setStatus(e.target.value as SalesOrderStatus | 'ALL')
            }
            aria-label="Filtrar por status"
          />
        </div>
      </div>

      {error ? <Alert variant="error">{error}</Alert> : null}

      {/* Tabela */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Número</th>
                <th className="px-4 py-3 font-medium">Emissão</th>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium">Itens</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : (data?.content ?? []).length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <ClipboardList className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhum pedido encontrado.</p>
                      <Link to="/sales-orders/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Criar o primeiro
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                (data?.content ?? []).map((o) => (
                  <tr
                    key={o.uuid}
                    className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40"
                    onClick={() => navigate(`/sales-orders/${o.uuid}/edit`)}
                  >
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-700 dark:text-slate-200">
                      {o.number}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                      {formatDate(o.orderDate)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900 dark:text-slate-100">
                        {o.clientName}
                      </div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">
                        {SALES_ORDER_CLIENT_TYPE_LABELS[o.clientType]}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                      {o.totalQuantity}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs font-semibold text-slate-700 dark:text-slate-200">
                      {brlFormatter.format(o.total)}
                    </td>
                    <td className="px-4 py-3">
                      <SalesOrderStatusBadge status={o.status} />
                    </td>
                    <td
                      className="px-4 py-3"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => navigate(`/sales-orders/${o.uuid}`)}
                          title="Ver resumo"
                          aria-label="Ver resumo"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() =>
                            window.open(`/sales-orders/${o.uuid}/pdf`, '_blank')
                          }
                          title="Gerar PDF"
                          aria-label="Gerar PDF"
                        >
                          <Printer className="h-4 w-4" />
                        </Button>
                        {canCancel(o) ? (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => setConfirmCancel(o)}
                            title="Cancelar pedido"
                            aria-label="Cancelar pedido"
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Footer com paginação */}
        <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 text-sm sm:flex-row dark:border-slate-800">
          <span className="text-slate-500 dark:text-slate-400">
            {data?.totalElements === 0
              ? 'Nenhum resultado'
              : `${data?.totalElements ?? 0} pedido(s) • Página ${
                  (data?.page ?? 0) + 1
                } de ${Math.max(data?.totalPages ?? 1, 1)}`}
          </span>
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="secondary"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={loading || data?.first}
            >
              <ChevronLeft className="h-4 w-4" />
              Anterior
            </Button>
            <Button
              size="sm"
              variant="secondary"
              onClick={() => setPage((p) => p + 1)}
              disabled={loading || data?.last}
            >
              Próxima
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={!!confirmCancel}
        title="Cancelar pedido?"
        description={
          confirmCancel?.status === 'FINALIZADO'
            ? `O pedido ${confirmCancel?.number} será marcado como CANCELADO. Como já estava finalizado, as saídas de estoque serão estornadas automaticamente, devolvendo o saldo dos itens aos produtos. O registro não é apagado.`
            : `O pedido ${confirmCancel?.number} será marcado como CANCELADO. O registro não é apagado.`
        }
        confirmText="Cancelar pedido"
        confirmVariant="danger"
        isLoading={canceling}
        onConfirm={handleCancel}
        onClose={() => {
          if (!canceling) setConfirmCancel(null)
        }}
      />
    </div>
  )
}