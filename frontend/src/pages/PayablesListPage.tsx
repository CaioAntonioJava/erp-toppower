import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  DollarSign,
  BarChart3,
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
import { PayableStatusBadge } from '../components/payable/PayableStatusBadge'
import { PayableReportsModal } from './PayablesReportsPage'
import {
  activatePayable,
  cancelPayable,
  listPayables,
  settlePayable,
} from '../api/payable.api'
import type {
  PayableFilters,
  PayableSource,
  PayableStatus,
  PayableSummaryResponse,
} from '../types/payable'
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
  { value: 'BOLETO', label: 'Boleto' },
  { value: 'PURCHASE_INVOICE', label: 'Nota de compra' },
]

const SOURCE_LABEL: Record<PayableSource, string> = {
  MANUAL: 'Manual',
  BOLETO: 'Boleto',
  PURCHASE_INVOICE: 'Nota de compra',
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

export function PayablesListPage() {
  const navigate = useNavigate()

  const [statusFilter, setStatusFilter] =
    useState<PayableStatus | 'ALL'>('ALL')
  const [sourceFilter, setSourceFilter] =
    useState<PayableSource | 'ALL'>('ALL')
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  // Filtro por intervalo de vencimento (yyyy-MM-dd). String vazia = sem
  // filtro; o backend recebe dueFrom/dueTo quando definidos.
  const [dueFrom, setDueFrom] = useState('')
  const [dueTo, setDueTo] = useState('')
  const [page, setPage] = useState(0)
  const size = 20

  const [data, setData] = useState<PagedResponse<PayableSummaryResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [cancelTarget, setCancelTarget] = useState<PayableSummaryResponse | null>(null)
  const [cancelling, setCancelling] = useState(false)
  const [cancelError, setCancelError] = useState<string | null>(null)
  const [activateTarget, setActivateTarget] = useState<PayableSummaryResponse | null>(null)
  const [activating, setActivating] = useState(false)
  const [activateError, setActivateError] = useState<string | null>(null)
  const [settleTarget, setSettleTarget] = useState<PayableSummaryResponse | null>(null)
  const [settling, setSettling] = useState(false)
  const [settleError, setSettleError] = useState<string | null>(null)
  const [reportsOpen, setReportsOpen] = useState(false)

  // Debounce simples da busca textual.
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(query.trim()), 350)
    return () => clearTimeout(t)
  }, [query])

  const filters: PayableFilters = {
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
      const result = await listPayables(filters)
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
      await cancelPayable(cancelTarget.id)
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
      await activatePayable(activateTarget.id)
      setActivateTarget(null)
      await reload()
    } catch (err) {
      setActivateError(toApiError(err).message)
    } finally {
      setActivating(false)
    }
  }

  async function handleSettle() {
    if (!settleTarget) return
    setSettling(true)
    setSettleError(null)
    try {
      await settlePayable(settleTarget.id)
      setSettleTarget(null)
      await reload()
    } catch (err) {
      setSettleError(toApiError(err).message)
    } finally {
      setSettling(false)
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
            Contas a Pagar
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Pagamentos em aberto, parcelas programadas e contas geradas
            automaticamente a partir de boletos vinculados a fornecedores.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="secondary"
            onClick={() => setReportsOpen(true)}
          >
            <BarChart3 className="h-4 w-4" />
            Relatórios
          </Button>
          <Link to="/payables/new">
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
            placeholder="Buscar por descrição ou número de nota…"
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
              setStatusFilter(e.target.value as PayableStatus | 'ALL')
            }
            aria-label="Filtrar por status"
          />
          <Select
            options={SOURCE_OPTIONS}
            value={sourceFilter}
            onChange={(e) =>
              setSourceFilter(e.target.value as PayableSource | 'ALL')
            }
            aria-label="Filtrar por origem"
          />
          <div className="w-full">
            <Input
              type="date"
              value={dueFrom}
              onChange={(e) => setDueFrom(e.target.value)}
              max={dueTo || undefined}
              aria-label="Filtrar por vencimento a partir de"
            />
            <p className="mt-1.5 text-sm text-slate-700 dark:text-slate-200">
              Vencimento de
            </p>
          </div>
          <div className="w-full">
            <Input
              type="date"
              value={dueTo}
              onChange={(e) => setDueTo(e.target.value)}
              min={dueFrom || undefined}
              aria-label="Filtrar por vencimento até"
            />
            <p className="mt-1.5 text-sm text-slate-700 dark:text-slate-200">
              Vencimento até
            </p>
          </div>
        </div>
      </div>

      {error ? <Alert variant="error">{error}</Alert> : null}
      {cancelError ? <Alert variant="error">{cancelError}</Alert> : null}
      {activateError ? <Alert variant="error">{activateError}</Alert> : null}
      {settleError ? <Alert variant="error">{settleError}</Alert> : null}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Descrição</th>
                <th className="px-4 py-3 font-medium">Fornecedor</th>
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
                      <p className="text-sm">Nenhuma conta a pagar encontrada.</p>
                      <Link to="/payables/new">
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
                      onClick={() => navigate(`/payables/${r.id}`)}
                    >
                      <td className="px-4 py-3">
                        <div className="line-clamp-1 text-xs font-medium text-slate-900 dark:text-slate-100">
                          {r.description}
                        </div>
                        {r.installmentsCount > 1 ? (
                          <div className="text-xs text-slate-500 dark:text-slate-400">
                            {r.installmentsCount} parcela(s)
                          </div>
                        ) : null}
                      </td>
                      <td className="px-4 py-3">
                        {r.supplierName ? (
                          <div>
                            <div className="text-xs text-slate-900 dark:text-slate-100">
                              {r.supplierName}
                            </div>
                            {r.supplierTaxId ? (
                              <div className="text-xs text-slate-500 dark:text-slate-400">
                                {r.supplierTaxId}
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
                        <PayableStatusBadge status={r.status} />
                      </td>
                      <td
                        className="px-4 py-3"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <div className="flex items-center justify-end gap-1">
                          {r.status === 'ABERTO' ? (
                            <Link to={`/payables/${r.id}`}>
                              <Button
                                size="sm"
                                variant="ghost"
                                title="Registrar pagamento"
                                aria-label="Registrar pagamento"
                                className="!text-emerald-600 hover:!text-emerald-600 dark:!text-emerald-500 dark:hover:!text-emerald-500"
                              >
                                <DollarSign className="h-4 w-4" />
                              </Button>
                            </Link>
                          ) : null}
                          {r.status === 'ABERTO' ? (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => {
                                setSettleError(null)
                                setSettleTarget(r)
                              }}
                              title="Liquidar todas as parcelas abertas"
                              aria-label="Liquidar todas as parcelas abertas"
                              className="!text-emerald-600 hover:!text-emerald-600 dark:!text-emerald-500 dark:hover:!text-emerald-500"
                            >
                              <CheckCircle2 className="h-4 w-4" />
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

      <ConfirmDialog
        open={cancelTarget != null}
        title="Cancelar conta a pagar?"
        description={
          cancelTarget
            ? `A conta "${cancelTarget.description}" será marcada como CANCELADA. As parcelas em aberto sem pagamentos também serão canceladas. O registro não é apagado e pode ser reativado depois.`
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
        title="Reativar conta a pagar?"
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

      <ConfirmDialog
        open={settleTarget != null}
        title="Liquidar todas as parcelas abertas?"
        description={
          settleTarget
            ? `Serão registrados pagamentos cobrindo o saldo devedor de todas as parcelas ABERTO da conta "${settleTarget.description}" (saldo total R$ ${settleTarget.balance.toFixed(2).replace('.', ',')}), transitando-a para PAGO.`
            : ''
        }
        confirmText="Liquidar"
        confirmVariant="primary"
        isLoading={settling}
        onConfirm={handleSettle}
        onClose={() => {
          if (!settling) setSettleTarget(null)
        }}
      />

      <PayableReportsModal open={reportsOpen} onClose={() => setReportsOpen(false)} />
    </div>
  )
}