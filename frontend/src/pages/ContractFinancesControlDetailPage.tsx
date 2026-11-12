import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Plus, TrendingUp, Wallet } from 'lucide-react'
import { Alert } from '../components/ui/Alert'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { Spinner } from '../components/ui/Spinner'
import { BackButton } from '../components/ui/BackButton'
import {
  activateContractFinanceEntry,
  getContractFinanceSummary,
  getContractFinancesControl,
  inactivateContractFinanceEntry,
  listContractFinanceEntries,
} from '../api/contractFinance.api'
import { toApiError } from '../lib/errors'
import { formatCurrency, formatDate } from '../lib/format'
import {
  CONTRACT_FINANCE_STATUS_LABELS,
  CONTRACT_FINANCE_TYPE_LABELS,
  contractFinanceTypeBadgeTone,
} from '../types/contractFinance'
import type {
  ContractFinanceControlResponse,
  ContractFinanceEntriesPage,
  ContractFinanceEntryResponse,
  ContractFinancesSummaryResponse,
} from '../types/contractFinance'

const PAGE_SIZE = 20

/** Card de resumo do contrato (retrato + totais). */
function SummaryCard({
  title,
  value,
  hint,
  icon,
  tone,
}: {
  title: string
  value: string
  hint?: string
  icon?: React.ReactNode
  tone?: 'positive' | 'negative'
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
        {icon}
        {title}
      </div>
      <div
        className={[
          'mt-1 text-lg font-semibold',
          tone === 'positive'
            ? 'text-emerald-600 dark:text-emerald-400'
            : tone === 'negative'
              ? 'text-red-600 dark:text-red-400'
              : 'text-slate-900 dark:text-slate-100',
        ].join(' ')}
      >
        {value}
      </div>
      {hint ? <div className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{hint}</div> : null}
    </div>
  )
}

/**
 * Detalhe do controle do financeiro de um contrato: retrato comercial
 * (código, cliente, data, valor), totais agregados, breakdown por
 * natureza e a lista de lançamentos (com editar/ativar/inativar).
 */
export function ContractFinancesControlDetailPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const controlId = Number(id)

  const [control, setControl] = useState<ContractFinanceControlResponse | null>(null)
  const [summary, setSummary] = useState<ContractFinancesSummaryResponse | null>(null)
  const [entries, setEntries] = useState<ContractFinanceEntriesPage | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // Lançamento aguardando confirmação de ativar/inativar.
  const [toggleTarget, setToggleTarget] = useState<{ entry: ContractFinanceEntryResponse; activate: boolean } | null>(null)
  const [toggling, setToggling] = useState(false)

  const reload = useCallback(async () => {
    if (!Number.isFinite(controlId)) return
    setLoading(true)
    setError(null)
    try {
      const [controlData, summaryData, entriesData] = await Promise.all([
        getContractFinancesControl(controlId),
        getContractFinanceSummary({ controlId }),
        listContractFinanceEntries({ controlId, page, size: PAGE_SIZE }),
      ])
      setControl(controlData)
      setSummary(summaryData)
      setEntries(entriesData)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
  }, [controlId, page])

  useEffect(() => {
    void reload()
  }, [reload])

  async function handleToggleConfirm() {
    if (!toggleTarget) return
    setToggling(true)
    try {
      if (toggleTarget.activate) {
        await activateContractFinanceEntry(toggleTarget.entry.id)
      } else {
        await inactivateContractFinanceEntry(toggleTarget.entry.id)
      }
      setToggleTarget(null)
      await reload()
    } catch (err) {
      setError(toApiError(err).message)
      setToggleTarget(null)
    } finally {
      setToggling(false)
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <BackButton label="Voltar" fallback="/contract-finances" />

      {error ? <Alert variant="error">{error}</Alert> : null}

      {loading ? (
        <div className="flex justify-center py-12">
          <Spinner className="h-8 w-8 text-primary" />
        </div>
      ) : control ? (
        <>
          {/* Cabeçalho do contrato */}
          <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-mono text-xl font-bold text-slate-900 dark:text-slate-100">
                  {control.contractCode}
                </span>
              </div>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                {control.clientName ?? '—'}
                {control.clientCode ? ` (${control.clientCode})` : ''}
                {control.contractDate ? ` · Vigência em ${formatDate(control.contractDate)}` : ''}
                {` · Valor: ${formatCurrency(control.contractValue)}`}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Link to={`/contracts/${control.contractId}`}>
                <Button variant="secondary">Abrir contrato</Button>
              </Link>
              <Button
                variant="primary"
                onClick={() => navigate(`/contract-finances/new?contractId=${control.contractId}`)}
              >
                <Plus className="mr-1.5 h-4 w-4" />
                Lançar neste contrato
              </Button>
            </div>
          </div>

          {/* Cards de totais */}
          <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
            <SummaryCard
              title="Valor do contrato"
              value={formatCurrency(control.contractValue)}
              icon={<Wallet className="h-5 w-5" />}
            />
            <SummaryCard
              title="Recebido"
              value={formatCurrency(control.totalRecebido)}
              tone="positive"
              hint={`${control.entriesCount} lançamento(s) ativo(s)`}
            />
            <SummaryCard title="Gastos" value={formatCurrency(control.totalGasto)} tone="negative" />
            <SummaryCard
              title="Resultado"
              value={formatCurrency(control.resultado)}
              icon={<TrendingUp className="h-5 w-5" />}
              tone={control.resultado < 0 ? 'negative' : 'positive'}
              hint="Recebido − gastos"
            />
          </div>

          {/* Breakdown por natureza */}
          {summary && summary.porTipo.length > 0 ? (
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <span className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Por natureza:
              </span>
              {summary.porTipo.map((t) => (
                <Badge key={t.type} tone={contractFinanceTypeBadgeTone(t.type)}>
                  {CONTRACT_FINANCE_TYPE_LABELS[t.type]}: {formatCurrency(t.total)}
                </Badge>
              ))}
            </div>
          ) : null}

          {/* Lançamentos */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Lançamentos ({summary?.entriesCount ?? 0} ativo(s))
              </h2>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead>
                  <tr className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                    <th className="px-4 py-3">Data</th>
                    <th className="px-4 py-3">Tipo</th>
                    <th className="px-4 py-3">Descrição</th>
                    <th className="px-4 py-3 text-right">Valor</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3 text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                  {!entries || entries.content.length === 0 ? (
                    <tr>
                      <td
                        colSpan={6}
                        className="px-4 py-10 text-center text-sm text-slate-500 dark:text-slate-400"
                      >
                        Nenhum lançamento neste contrato ainda.
                      </td>
                    </tr>
                  ) : (
                    entries.content.map((entry) => (
                      <tr
                        key={entry.id}
                        className={[
                          'hover:bg-slate-50 dark:hover:bg-slate-800/50',
                          entry.status === 'INATIVO' ? 'opacity-60' : '',
                        ].join(' ')}
                      >
                        <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                          {formatDate(entry.entryDate)}
                        </td>
                        <td className="px-4 py-3">
                          <Badge tone={contractFinanceTypeBadgeTone(entry.type)}>
                            {CONTRACT_FINANCE_TYPE_LABELS[entry.type]}
                          </Badge>
                        </td>
                        <td
                          className="max-w-[280px] truncate px-4 py-3 text-slate-600 dark:text-slate-300"
                          title={entry.description ?? ''}
                        >
                          {entry.description || '—'}
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                          <span
                            className={
                              entry.type === 'RECEBIDO'
                                ? 'text-emerald-600 dark:text-emerald-400'
                                : 'text-red-600 dark:text-red-400'
                            }
                          >
                            {entry.type === 'RECEBIDO' ? '+' : '−'}
                            {formatCurrency(entry.amount)}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <Badge
                            tone={entry.status === 'ATIVO' ? 'success' : 'neutral'}
                          >
                            {CONTRACT_FINANCE_STATUS_LABELS[entry.status]}
                          </Badge>
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 text-right">
                          <div className="inline-flex gap-1.5">
                            <Button
                              size="sm"
                              variant="secondary"
                              onClick={() => navigate(`/contract-finances/entries/${entry.id}`)}
                            >
                              Editar
                            </Button>
                            <Button
                              size="sm"
                              variant={entry.status === 'ATIVO' ? 'danger' : 'secondary'}
                              onClick={() =>
                                setToggleTarget({ entry, activate: entry.status === 'INATIVO' })
                              }
                            >
                              {entry.status === 'ATIVO' ? 'Inativar' : 'Ativar'}
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            {entries && entries.totalElements > 0 ? (
              <div className="flex items-center justify-between gap-2 border-t border-slate-200 px-4 py-3 text-sm dark:border-slate-800">
                <span className="text-slate-500 dark:text-slate-400">
                  {entries.totalElements} lançamento(s) — Página {entries.page + 1} de{' '}
                  {entries.totalPages || 1}
                </span>
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    variant="secondary"
                    disabled={entries.first}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    Anterior
                  </Button>
                  <Button size="sm" variant="secondary" disabled={entries.last} onClick={() => setPage((p) => p + 1)}>
                    Próxima
                  </Button>
                </div>
              </div>
            ) : null}
          </section>

          <ConfirmDialog
            open={!!toggleTarget}
            title={toggleTarget?.activate ? 'Reativar lançamento' : 'Inativar lançamento'}
            description={
              toggleTarget?.activate
                ? 'O lançamento voltará a somar nos totais do contrato.'
                : `O lançamento de ${formatCurrency(toggleTarget?.entry?.amount)} deixará de somar nos totais. Você pode reativá-lo depois.`
            }
            confirmText={toggleTarget?.activate ? 'Reativar' : 'Inativar'}
            confirmVariant={toggleTarget?.activate ? 'primary' : 'danger'}
            isLoading={toggling}
            onClose={() => setToggleTarget(null)}
            onConfirm={handleToggleConfirm}
          />
        </>
      ) : null}
    </div>
  )
}
