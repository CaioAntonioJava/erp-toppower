import { useCallback, useEffect, useState } from 'react'
import { BarChart3, Calendar, Wallet } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import {
  getAgingReport,
  getClientPositionReport,
  getFlowReport,
} from '../api/receivableReport.api'
import type { ReceivableSource } from '../types/receivable'
import type {
  ReceivableAgingReportResponse,
  ReceivableClientPositionReportResponse,
  ReceivableFlowReportResponse,
  ReceivableReportGranularity,
} from '../types/receivableReport'
import { toApiError } from '../lib/errors'

type ReportTab = 'aging' | 'flow' | 'client'

const SOURCE_OPTIONS = [
  { value: 'ALL', label: 'Todas as origens' },
  { value: 'MANUAL', label: 'Manual' },
  { value: 'SALES_ORDER', label: 'Pedido de venda' },
  { value: 'TECHNICAL_PROPOSAL', label: 'Proposta técnica' },
  { value: 'CONTRACT', label: 'Contrato' },
]

const GRANULARITY_OPTIONS = [
  { value: 'MONTH', label: 'Mês' },
  { value: 'WEEK', label: 'Semana' },
  { value: 'DAY', label: 'Dia' },
]

const TAB_OPTIONS: ReadonlyArray<{ value: ReportTab; label: string }> = [
  { value: 'aging', label: 'A receber (aging)' },
  { value: 'flow', label: 'Recebido (fluxo)' },
  { value: 'client', label: 'Por cliente' },
]

/** Data atual no formato ISO `YYYY-MM-DD`. */
function todayIso(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

/** Soma dias a uma data ISO (yyyy-MM-dd). diasNegativos = subtrai. */
function addDaysIso(iso: string, days: number): string {
  const d = new Date(`${iso}T00:00:00`)
  d.setDate(d.getDate() + days)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

/** Primeiro dia do mês atual (yyyy-MM-dd). */
function firstOfMonthIso(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  return `${yyyy}-${mm}-01`
}

/** Primeiro dia do mês anterior (yyyy-MM-dd). */
function firstOfPrevMonthIso(): string {
  const d = new Date()
  d.setDate(1)
  d.setMonth(d.getMonth() - 1)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  return `${yyyy}-${mm}-01`
}

/** Último dia do mês anterior (yyyy-MM-dd). */
function lastOfPrevMonthIso(): string {
  const d = new Date()
  d.setDate(0) // último dia do mês anterior
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const brl = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export function ReceivableReportsPage() {
  const [tab, setTab] = useState<ReportTab>('aging')
  const [sourceFilter, setSourceFilter] = useState<ReceivableSource | 'ALL'>('ALL')

  // Aging / client-position: data de referência (dueTo).
  const [dueTo, setDueTo] = useState<string>(todayIso())

  // Flow: intervalo + granularidade.
  const [from, setFrom] = useState<string>(firstOfMonthIso())
  const [to, setTo] = useState<string>(todayIso())
  const [granularity, setGranularity] = useState<ReceivableReportGranularity>('MONTH')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [agingData, setAgingData] = useState<ReceivableAgingReportResponse | null>(null)
  const [flowData, setFlowData] = useState<ReceivableFlowReportResponse | null>(null)
  const [clientData, setClientData] =
    useState<ReceivableClientPositionReportResponse | null>(null)

  const sourceParam = sourceFilter === 'ALL' ? null : sourceFilter

  const reloadAging = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await getAgingReport({ dueTo, sourceType: sourceParam })
      setAgingData(result)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dueTo, sourceFilter])

  const reloadFlow = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await getFlowReport({
        from,
        to,
        granularity,
        sourceType: sourceParam,
      })
      setFlowData(result)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [from, to, granularity, sourceFilter])

  const reloadClient = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await getClientPositionReport({ dueTo, sourceType: sourceParam })
      setClientData(result)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dueTo, sourceFilter])

  useEffect(() => {
    if (tab === 'aging') reloadAging()
    else if (tab === 'flow') reloadFlow()
    else reloadClient()
  }, [tab, reloadAging, reloadFlow, reloadClient])

  // Atalhos rápidos de período — preenchem os campos de data conforme o relatório.
  function applyShortcut(kind: 'today' | '7d' | '30d' | 'thisMonth' | 'prevMonth' | '60d' | '90d') {
    const today = todayIso()
    if (kind === 'today') {
      setDueTo(today)
      setFrom(today)
      setTo(today)
    } else if (kind === '7d') {
      setFrom(addDaysIso(today, -6))
      setTo(today)
    } else if (kind === '30d') {
      setFrom(addDaysIso(today, -29))
      setTo(today)
    } else if (kind === '60d') {
      setFrom(addDaysIso(today, -59))
      setTo(today)
    } else if (kind === '90d') {
      setFrom(addDaysIso(today, -89))
      setTo(today)
    } else if (kind === 'thisMonth') {
      setFrom(firstOfMonthIso())
      setTo(today)
    } else if (kind === 'prevMonth') {
      setFrom(firstOfPrevMonthIso())
      setTo(lastOfPrevMonthIso())
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <header className="mb-6 flex flex-col gap-2">
        <div className="flex items-center gap-2 text-slate-700 dark:text-slate-200">
          <BarChart3 className="h-6 w-6" />
          <h1 className="text-2xl font-bold">Relatórios de Contas a Receber</h1>
        </div>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Posição de contas a receber, fluxo de recebimentos e visão por cliente.
        </p>
      </header>

      {/* Abas de relatório */}
      <div className="mb-4 flex flex-wrap gap-1 rounded-lg border border-slate-200 bg-white p-1 dark:border-slate-800 dark:bg-slate-900">
        {TAB_OPTIONS.map((t) => (
          <button
            key={t.value}
            type="button"
            onClick={() => setTab(t.value)}
            className={
              'rounded-md px-3 py-1.5 text-sm font-medium transition-colors ' +
              (tab === t.value
                ? 'bg-primary text-white'
                : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800')
            }
            aria-pressed={tab === t.value}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Filtros comuns + específicos por relatório */}
      <section className="mb-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-[200px]">
            <Select
              label="Origem"
              options={SOURCE_OPTIONS}
              value={sourceFilter}
              onChange={(e) => setSourceFilter(e.target.value as ReceivableSource | 'ALL')}
              aria-label="Filtrar por origem"
            />
          </div>

          {tab === 'aging' || tab === 'client' ? (
            <div>
              <Input
                type="date"
                label="Data de referência"
                value={dueTo}
                onChange={(e) => setDueTo(e.target.value)}
                aria-label="Data de referência"
              />
            </div>
          ) : null}

          {tab === 'flow' ? (
            <>
              <div>
                <Input
                  type="date"
                  label="De"
                  value={from}
                  onChange={(e) => setFrom(e.target.value)}
                  max={to || undefined}
                  aria-label="Data inicial"
                />
              </div>
              <div>
                <Input
                  type="date"
                  label="Até"
                  value={to}
                  onChange={(e) => setTo(e.target.value)}
                  min={from || undefined}
                  aria-label="Data final"
                />
              </div>
              <div className="min-w-[140px]">
                <Select
                  label="Granularidade"
                  options={GRANULARITY_OPTIONS}
                  value={granularity}
                  onChange={(e) =>
                    setGranularity(e.target.value as ReceivableReportGranularity)
                  }
                  aria-label="Granularidade do agrupamento"
                />
              </div>
            </>
          ) : null}
        </div>

        {/* Atalhos rápidos de período */}
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
            <Calendar className="mr-1 inline h-3.5 w-3.5" />
            Atalhos:
          </span>
          {tab === 'flow' ? (
            <>
              <Button size="sm" variant="ghost" onClick={() => applyShortcut('today')}>
                Hoje
              </Button>
              <Button size="sm" variant="ghost" onClick={() => applyShortcut('7d')}>
                7 dias
              </Button>
              <Button size="sm" variant="ghost" onClick={() => applyShortcut('30d')}>
                30 dias
              </Button>
              <Button size="sm" variant="ghost" onClick={() => applyShortcut('thisMonth')}>
                Mês atual
              </Button>
              <Button size="sm" variant="ghost" onClick={() => applyShortcut('prevMonth')}>
                Mês passado
              </Button>
            </>
          ) : (
            <>
              <Button size="sm" variant="ghost" onClick={() => setDueTo(todayIso())}>
                Hoje
              </Button>
              <Button size="sm" variant="ghost" onClick={() => applyShortcut('30d')}>
                Últimos 30 dias
              </Button>
              <Button size="sm" variant="ghost" onClick={() => applyShortcut('60d')}>
                Últimos 60 dias
              </Button>
              <Button size="sm" variant="ghost" onClick={() => applyShortcut('90d')}>
                Últimos 90 dias
              </Button>
            </>
          )}
        </div>
      </section>

      {error ? <Alert variant="error">{error}</Alert> : null}

      {loading ? (
        <div className="flex justify-center py-12">
          <Spinner className="h-8 w-8 text-primary" />
        </div>
      ) : (
        <>
          {tab === 'aging' && agingData ? (
            <AgingView data={agingData} />
          ) : null}
          {tab === 'flow' && flowData ? (
            <FlowView data={flowData} />
          ) : null}
          {tab === 'client' && clientData ? (
            <ClientPositionView data={clientData} />
          ) : null}
        </>
      )}
    </div>
  )
}

// =====================================================================
// View: Aging
// =====================================================================

function AgingView({ data }: { data: ReceivableAgingReportResponse }) {
  const buckets = [
    { label: '0–30 dias', bucket: data.bucket0_30 },
    { label: '31–60 dias', bucket: data.bucket31_60 },
    { label: '61–90 dias', bucket: data.bucket61_90 },
    { label: '90+ dias', bucket: data.bucket90Plus },
  ]
  return (
    <div className="space-y-4">
      {/* Resumo */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <Card title="Saldo devedor total" value={brl.format(data.totalOpenBalance)} icon={<Wallet className="h-5 w-5" />} />
        <Card title="Contas em aberto" value={String(data.totalOpenCount)} />
        {buckets.map((b) => (
          <Card
            key={b.label}
            title={b.label}
            value={brl.format(b.bucket.balance)}
            hint={`${b.bucket.count} conta(s)`}
          />
        ))}
      </div>

      {/* Tabela por cliente */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
          <h2 className="text-base font-semibold">Aging por cliente</h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Referência: {formatDate(data.referenceDate)}
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium text-right">Saldo devedor</th>
                <th className="px-4 py-3 font-medium text-right">Contas</th>
                <th className="px-4 py-3 font-medium text-right">0–30</th>
                <th className="px-4 py-3 font-medium text-right">31–60</th>
                <th className="px-4 py-3 font-medium text-right">61–90</th>
                <th className="px-4 py-3 font-medium text-right">90+</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {data.byClient.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                    Nenhuma conta em aberto para os filtros informados.
                  </td>
                </tr>
              ) : (
                data.byClient.map((c) => (
                  <tr key={c.clientId}>
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-800 dark:text-slate-200">
                        {c.clientName ?? '—'}
                      </div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">
                        {c.clientCode ?? '—'}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                      {brl.format(c.totalBalance)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {c.count}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {brl.format(c.bucket0_30.balance)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {brl.format(c.bucket31_60.balance)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {brl.format(c.bucket61_90.balance)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {brl.format(c.bucket90Plus.balance)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// =====================================================================
// View: Flow
// =====================================================================

function FlowView({ data }: { data: ReceivableFlowReportResponse }) {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Card
          title="Total recebido no período"
          value={brl.format(data.totalReceived)}
          icon={<Wallet className="h-5 w-5" />}
        />
        <Card title="Nº de pagamentos" value={String(data.paymentCount)} />
        <Card
          title="Período"
          value={`${formatDate(data.from)} — ${formatDate(data.to)}`}
          hint={`Granularidade: ${data.granularity}`}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* Por período */}
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
            <h2 className="text-base font-semibold">Recebido por período</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Período</th>
                  <th className="px-4 py-3 font-medium text-right">Recebido</th>
                  <th className="px-4 py-3 font-medium text-right">Nº</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {data.byPeriod.length === 0 ? (
                  <tr>
                    <td colSpan={3} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                      Sem recebimentos no período.
                    </td>
                  </tr>
                ) : (
                  data.byPeriod.map((p) => (
                    <tr key={p.periodStart}>
                      <td className="px-4 py-3 font-medium">{p.label}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                        {brl.format(p.received)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                        {p.paymentCount}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Por cliente */}
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
            <h2 className="text-base font-semibold">Recebido por cliente</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Cliente</th>
                  <th className="px-4 py-3 font-medium text-right">Recebido</th>
                  <th className="px-4 py-3 font-medium text-right">Nº</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {data.byClient.length === 0 ? (
                  <tr>
                    <td colSpan={3} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                      Sem recebimentos no período.
                    </td>
                  </tr>
                ) : (
                  data.byClient.map((c) => (
                    <tr key={c.clientId}>
                      <td className="px-4 py-3 font-medium">
                        {c.clientName ?? '—'}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                        {brl.format(c.totalReceived)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                        {c.paymentCount}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}

// =====================================================================
// View: Posição por cliente
// =====================================================================

function ClientPositionView({ data }: { data: ReceivableClientPositionReportResponse }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
        <h2 className="text-base font-semibold">Posição por cliente</h2>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Referência: {formatDate(data.referenceDate)} • ordenado por total a receber (desc)
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">Cliente</th>
              <th className="px-4 py-3 font-medium text-right">A receber</th>
              <th className="px-4 py-3 font-medium text-right">Recebido</th>
              <th className="px-4 py-3 font-medium text-right">Contas em aberto</th>
              <th className="px-4 py-3 font-medium text-right">Em atraso</th>
              <th className="px-4 py-3 font-medium text-right">Maior atraso (dias)</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
            {data.clients.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                  Sem clientes para os filtros informados.
                </td>
              </tr>
            ) : (
              data.clients.map((c) => (
                <tr key={c.clientId}>
                  <td className="px-4 py-3">
                    <div className="font-medium text-slate-800 dark:text-slate-200">
                      {c.clientName ?? '—'}
                    </div>
                    <div className="text-xs text-slate-500 dark:text-slate-400">
                      {c.clientCode ?? '—'}
                    </div>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                    {brl.format(c.totalToReceive)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {brl.format(c.totalReceived)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {c.openCount}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {c.overdueCount > 0 ? (
                      <span className="text-red-600 dark:text-red-400">{c.overdueCount}</span>
                    ) : (
                      c.overdueCount
                    )}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {c.maxOverdueDays > 0 ? (
                      <span className="text-red-600 dark:text-red-400">{c.maxOverdueDays}</span>
                    ) : (
                      '—'
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// =====================================================================
// Card de resumo
// =====================================================================

function Card({
  title,
  value,
  hint,
  icon,
}: {
  title: string
  value: string
  hint?: string
  icon?: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
        {icon}
        {title}
      </div>
      <div className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
        {value}
      </div>
      {hint ? (
        <div className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{hint}</div>
      ) : null}
    </div>
  )
}