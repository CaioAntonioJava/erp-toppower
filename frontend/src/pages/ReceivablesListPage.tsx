import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  DollarSign,
  Eye,
  Plus,
  RotateCcw,
  Search,
  Wallet,
  X,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { ReceivableStatusBadge } from '../components/receivable/ReceivableStatusBadge'
import { ReceivablePaymentModal } from '../components/receivable/ReceivablePaymentModal'
import {
  activateReceivable,
  cancelReceivable,
  listReceivables,
} from '../api/receivable.api'
import type {
  ReceivableFilters,
  ReceivableSource,
  ReceivableStatus,
  ReceivableSummaryResponse,
} from '../types/receivable'
import type { PagedResponse } from '../types/api'
import { formatBRLValue } from '../lib/money'
import { toApiError } from '../lib/errors'

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Todos' },
  { value: 'ABERTO', label: 'Aberto' },
  { value: 'PAGO', label: 'Pagos' },
  { value: 'CANCELADO', label: 'Cancelados' },
]

const SOURCE_OPTIONS = [
  { value: 'ALL', label: 'Todas as origens' },
  { value: 'MANUAL', label: 'Manual' },
  { value: 'SALES_ORDER', label: 'Pedido de venda' },
  { value: 'TECHNICAL_PROPOSAL', label: 'Proposta técnica' },
  { value: 'CONTRACT', label: 'Contrato' },
]

const SOURCE_LABEL: Record<ReceivableSource, string> = {
  MANUAL: 'Manual',
  SALES_ORDER: 'Pedido de venda',
  TECHNICAL_PROPOSAL: 'Proposta técnica',
  CONTRACT: 'Contrato',
}

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  })
}

/** Retorna true se o vencimento está vencido (antes de hoje). */
function isOverdue(iso: string | null | undefined): boolean {
  if (!iso) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const due = new Date(`${iso}T00:00:00`)
  return due < today
}

export function ReceivablesListPage() {
  const navigate = useNavigate()

  const [statusFilter, setStatusFilter] =
    useState<ReceivableStatus | 'ALL'>('ALL')
  const [sourceFilter, setSourceFilter] =
    useState<ReceivableSource | 'ALL'>('ALL')
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  // Filtro por intervalo de vencimento (yyyy-MM-dd). String vazia = sem
  // filtro; o backend recebe dueFrom/dueTo quando definidos.
  const [dueFrom, setDueFrom] = useState('')
  const [dueTo, setDueTo] = useState('')
  const [page, setPage] = useState(0)
  const size = 20

  const [data, setData] = useState<PagedResponse<ReceivableSummaryResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [cancelTarget, setCancelTarget] = useState<ReceivableSummaryResponse | null>(null)
  const [cancelling, setCancelling] = useState(false)
  const [cancelError, setCancelError] = useState<string | null>(null)
  const [activateTarget, setActivateTarget] = useState<ReceivableSummaryResponse | null>(null)
  const [activating, setActivating] = useState(false)
  const [activateError, setActivateError] = useState<string | null>(null)
  const [paymentTarget, setPaymentTarget] = useState<ReceivableSummaryResponse | null>(null)

  // Debounce simples da busca textual.
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(query.trim()), 350)
    return () => clearTimeout(t)
  }, [query])

  const filters: ReceivableFilters = {
    status: statusFilter,
    sourceType: sourceFilter,
    query: debouncedQuery.length >= 2 ? debouncedQuery : undefined,
    dueFrom: dueFrom || undefined,
    dueTo: dueTo || undefined,
    page,
    size,
  }

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await listReceivables(filters)
      setData(result)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, sourceFilter, debouncedQuery, dueFrom, dueTo, page])

  useEffect(() => {
    reload()
  }, [reload])

  // Reseta a páginação sempre que os filtros mudam.
  useEffect(() => {
    setPage(0)
  }, [statusFilter, sourceFilter, debouncedQuery, dueFrom, dueTo])

  async function handleCancel() {
    if (!cancelTarget) return
    setCancelling(true)
    setCancelError(null)
    try {
      await cancelReceivable(cancelTarget.id)
      setCancelTarget(null)
      await reload()
    } catch (err) {
      setCancelError(toApiError(err).message)
    } finally {
      setCancelling(false)
    }
  }

  async function handleActivate() {
    if (!activateTarget) return
    setActivating(true)
    setActivateError(null)
    try {
      await activateReceivable(activateTarget.id)
      setActivateTarget(null)
      await reload()
    } catch (err) {
      setActivateError(toApiError(err).message)
    } finally {
      setActivating(false)
    }
  }

  const items = data?.content ?? []
  const totalElements = data?.totalElements ?? 0
  const totalPages = data?.totalPages ?? 0

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Contas a Receber
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Recebimentos em aberto, pagamentos parciais e contas geradas
            automaticamente por pedidos, propostas técnicas e contratos.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/receivables/new">
            <Button>
              <Plus className="h-4 w-4" />
              Nova conta
            </Button>
          </Link>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid gap-3 sm:grid-cols-[1fr_200px_200px] lg:grid-cols-[1fr_200px_200px_180px_180px]">
          <Input
            placeholder="Buscar por descrição, código do contrato/proposta…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            leftAdornment={<Search className="h-4 w-4" />}
            hint={
              query.trim().length > 0 && query.trim().length < 2
                ? 'Digite ao menos 2 caracteres para buscar.'
                : undefined
            }
          />
          <Select
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as ReceivableStatus | 'ALL')
            }
            aria-label="Filtrar por status"
          />
          <Select
            options={SOURCE_OPTIONS}
            value={sourceFilter}
            onChange={(e) =>
              setSourceFilter(e.target.value as ReceivableSource | 'ALL')
            }
            aria-label="Filtrar por origem"
          />
          <Input
            label="Vencimento de"
            type="date"
            value={dueFrom}
            onChange={(e) => setDueFrom(e.target.value)}
            max={dueTo || undefined}
            aria-label="Filtrar por vencimento a partir de"
          />
          <Input
            label="Vencimento até"
            type="date"
            value={dueTo}
            onChange={(e) => setDueTo(e.target.value)}
            min={dueFrom || undefined}
            aria-label="Filtrar por vencimento até"
          />
        </div>
      </div>

      {error ? <Alert variant="error">{error}</Alert> : null}
      {cancelError ? <Alert variant="error">{cancelError}</Alert> : null}
      {activateError ? <Alert variant="error">{activateError}</Alert> : null}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Descrição</th>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium">Origem</th>
                <th className="px-4 py-3 text-right font-medium">Valor</th>
                <th className="px-4 py-3 text-right font-medium">Pago</th>
                <th className="px-4 py-3 text-right font-medium">Saldo</th>
                <th className="px-4 py-3 font-medium">Vencimento</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={9} className="px-4 py-12 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Wallet className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhuma conta a receber encontrada.</p>
                      <Link to="/receivables/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Cadastrar a primeira
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                items.map((r) => {
                  const overdue = r.status === 'ABERTO' && isOverdue(r.dueDate)
                  return (
                    <tr
                      key={r.id}
                      className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40"
                      onClick={() => navigate(`/receivables/${r.id}`)}
                    >
                      <td className="px-4 py-3">
                        <div className="line-clamp-1 text-xs font-medium text-slate-900 dark:text-slate-100">
                          {r.sourceCode ?? r.description}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        {r.clientName ? (
                          <div>
                            <div className="text-xs text-slate-900 dark:text-slate-100">
                              {r.clientName}
                            </div>
                            {r.clientCode ? (
                              <div className="text-xs text-slate-500 dark:text-slate-400">
                                {r.clientCode}
                              </div>
                            ) : null}
                          </div>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                        {SOURCE_LABEL[r.sourceType]}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right text-slate-900 dark:text-slate-100">
                        {formatBRLValue(r.value)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right text-emerald-700 dark:text-emerald-400">
                        {formatBRLValue(r.paidAmount)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-medium text-slate-900 dark:text-slate-100">
                        {formatBRLValue(r.balance)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3">
                        <span
                          className={
                            overdue
                              ? 'font-medium text-red-600 dark:text-red-400'
                              : 'text-slate-600 dark:text-slate-300'
                          }
                        >
                          {formatDate(r.dueDate)}
                          {overdue ? ' • vencido' : ''}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <ReceivableStatusBadge status={r.status} />
                      </td>
                      <td
                        className="px-4 py-3"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            size="sm" variant="ghost"
                            onClick={() => navigate(`/receivables/${r.id}`)}
                            title="Ver / detalhe" aria-label="Ver / detalhe"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          {r.status === 'ABERTO' ? (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => setPaymentTarget(r)}
                              title="Registrar pagamento"
                              aria-label="Registrar pagamento"
                              className="!text-emerald-600 hover:!text-emerald-600 dark:!text-emerald-500 dark:hover:!text-emerald-500"
                            >
                              <DollarSign className="h-4 w-4" />
                            </Button>
                          ) : null}
                          {r.status === 'ABERTO' ? (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => setCancelTarget(r)}
                              title="Cancelar conta"
                              aria-label="Cancelar conta"
                            >
                              <X className="h-4 w-4" />
                            </Button>
                          ) : null}
                          {r.status === 'CANCELADO' ? (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => setActivateTarget(r)}
                              title="Reativar conta"
                              aria-label="Reativar conta"
                            >
                              <RotateCcw className="h-4 w-4" />
                            </Button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 text-sm sm:flex-row dark:border-slate-800">
          <span className="text-slate-500 dark:text-slate-400">
            {totalElements === 0
              ? 'Nenhum resultado'
              : `${totalElements} conta(s) • Página ${page + 1} de ${Math.max(totalPages, 1)}`}
          </span>
          <div className="flex items-center gap-2">
            <Button
              size="sm" variant="secondary"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={loading || data?.first}
            >
              <ChevronLeft className="h-4 w-4" />
              Anterior
            </Button>
            <Button
              size="sm" variant="secondary"
              onClick={() => setPage((p) => p + 1)}
              disabled={loading || data?.last}
            >
              Próxima
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>

      <ReceivablePaymentModal
        receivable={paymentTarget}
        open={paymentTarget != null}
        onClose={() => setPaymentTarget(null)}
        onSuccess={() => reload()}
      />

      <ConfirmDialog
        open={cancelTarget != null}
        title="Cancelar conta a receber?"
        description={
          cancelTarget
            ? `A conta "${cancelTarget.description}" será marcada como CANCELADA. O registro não é apagado e pode ser reativado depois.`
            : ''
        }
        confirmText="Cancelar conta"
        confirmVariant="danger"
        isLoading={cancelling}
        onConfirm={handleCancel}
        onClose={() => {
          if (!cancelling) setCancelTarget(null)
        }}
      />

      <ConfirmDialog
        open={activateTarget != null}
        title="Reativar conta a receber?"
        description={
          activateTarget
            ? `A conta "${activateTarget.description}" voltará a ficar ativa (ou paga, se já quitada).`
            : ''
        }
        confirmText="Reativar"
        confirmVariant="primary"
        isLoading={activating}
        onConfirm={handleActivate}
        onClose={() => {
          if (!activating) setActivateTarget(null)
        }}
      />
    </div>
  )
}