import { useCallback, useEffect, useState } from 'react'
import { Calendar, Wallet } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { Modal } from '../components/ui/Modal'
import {
  getAgingReport,
  getFlowReport,
  getSupplierPositionReport,
} from '../api/payableReport.api'
import type { PayableSource } from '../types/payable'
import type {
  PayableAgingReportResponse,
  PayableFlowReportResponse,
  PayableReportGranularity,
  PayableSupplierPositionReportResponse,
} from '../types/payableReport'
import { toApiError } from '../lib/errors'

type ReportTab = 'aging' | 'flow' | 'supplier'

const SOURCE_OPTIONS = [
  { value: 'ALL', label: 'Todas as origens' },
  { value: 'MANUAL', label: 'Manual' },
  { value: 'BOLETO', label: 'Boleto' },
  { value: 'PURCHASE_INVOICE', label: 'Nota de compra' },
]

const GRANULARITY_OPTIONS = [
  { value: 'MONTH', label: 'Mês' },
  { value: 'WEEK', label: 'Semana' },
  { value: 'DAY', label: 'Dia' },
]

const TAB_OPTIONS: ReadonlyArray<{ value: ReportTab; label: string }> = [
  { value: 'aging', label: 'A pagar' },
  { value: 'flow', label: 'Pago (fluxo)' },
  { value: 'supplier', label: 'Por fornecedor' },
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

export function PayableReportsModal({
  open,
  onClose,
}: {
  open: boolean
  onClose: () => void
}) {
  const [tab, setTab] = useState<ReportTab>('aging')
  const [sourceFilter, setSourceFilter] = useState<PayableSource | 'ALL'>('ALL')

  // Aging / supplier-position: data de referência (dueTo).
  const [dueTo, setDueTo] = useState<string>(todayIso())

  // Flow: intervalo + granularidade.
  const [from, setFrom] = useState<string>(firstOfMonthIso())
  const [to, setTo] = useState<string>(todayIso())
  const [granularity, setGranularity] = useState<PayableReportGranularity>('MONTH')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [agingData, setAgingData] = useState<PayableAgingReportResponse | null>(null)
  const [flowData, setFlowData] = useState<PayableFlowReportResponse | null>(null)
  const [supplierData, setSupplierData] =
    useState<PayableSupplierPositionReportResponse | null>(null)

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

  const reloadSupplier = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await getSupplierPositionReport({ dueTo, sourceType: sourceParam })
      setSupplierData(result)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dueTo, sourceFilter])

  useEffect(() => {
    if (!open) return
    if (tab === 'aging') reloadAging()
    else if (tab === 'flow') reloadFlow()
    else reloadSupplier()
  }, [open, tab, reloadAging, reloadFlow, reloadSupplier])

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
    <Modal
      open={open}
      onClose={onClose}
      title="Relatórios de Contas a Pagar"
      description="Posição de contas a pagar, fluxo de pagamentos e visão por fornecedor."
      maxWidth="max-w-6xl"
    >
      <div className="space-y-4">
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
                onChange={(e) => setSourceFilter(e.target.value as PayableSource | 'ALL')}
                aria-label="Filtrar por origem"
              />
            </div>

            {tab === 'aging' || tab === 'supplier' ? (
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
                      setGranularity(e.target.value as PayableReportGranularity)
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
            {tab === 'supplier' && supplierData ? (
              <SupplierPositionView data={supplierData} />
            ) : null}
          </>
        )}
      </div>
    </Modal>
  )
}

// =====================================================================
// View: Aging
// =====================================================================

function AgingView({ data }: { data: PayableAgingReportResponse }) {
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
        <Card title="Parcelas em aberto" value={String(data.totalOpenCount)} />
        {buckets.map((b) => (
          <Card
            key={b.label}
            title={b.label}
            value={brl.format(b.bucket.balance)}
            hint={`${b.bucket.count} parcela(s)`}
          />
        ))}
      </div>

      {/* Tabela por fornecedor */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
          <h2 className="text-base font-semibold">Parcelas em aberto por fornecedor</h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Referência: {formatDate(data.referenceDate)}
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Fornecedor</th>
                <th className="px-4 py-3 font-medium text-right">Saldo devedor</th>
                <th className="px-4 py-3 font-medium text-right">Parcelas</th>
                <th className="px-4 py-3 font-medium text-right">0–30</th>
                <th className="px-4 py-3 font-medium text-right">31–60</th>
                <th className="px-4 py-3 font-medium text-right">61–90</th>
                <th className="px-4 py-3 font-medium text-right">90+</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {data.bySupplier.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                    Nenhuma parcela em aberto para os filtros informados.
                  </td>
                </tr>
              ) : (
                data.bySupplier.map((s) => (
                  <tr key={s.supplierId}>
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-800 dark:text-slate-200">
                        {s.supplierName ?? '—'}
                      </div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">
                        {s.supplierTaxId ?? '—'}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                      {brl.format(s.totalBalance)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {s.count}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {brl.format(s.bucket0_30.balance)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {brl.format(s.bucket31_60.balance)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {brl.format(s.bucket61_90.balance)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                      {brl.format(s.bucket90Plus.balance)}
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

function FlowView({ data }: { data: PayableFlowReportResponse }) {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Card
          title="Total pago no período"
          value={brl.format(data.totalPaid)}
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
            <h2 className="text-base font-semibold">Pago por período</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Período</th>
                  <th className="px-4 py-3 font-medium text-right">Pago</th>
                  <th className="px-4 py-3 font-medium text-right">Nº</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {data.byPeriod.length === 0 ? (
                  <tr>
                    <td colSpan={3} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                      Sem pagamentos no período.
                    </td>
                  </tr>
                ) : (
                  data.byPeriod.map((p) => (
                    <tr key={p.periodStart}>
                      <td className="px-4 py-3 font-medium">{p.label}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                        {brl.format(p.paid)}
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

        {/* Por fornecedor */}
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
            <h2 className="text-base font-semibold">Pago por fornecedor</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Fornecedor</th>
                  <th className="px-4 py-3 font-medium text-right">Pago</th>
                  <th className="px-4 py-3 font-medium text-right">Nº</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {data.bySupplier.length === 0 ? (
                  <tr>
                    <td colSpan={3} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                      Sem pagamentos no período.
                    </td>
                  </tr>
                ) : (
                  data.bySupplier.map((s) => (
                    <tr key={s.supplierId}>
                      <td className="px-4 py-3 font-medium">
                        {s.supplierName ?? '—'}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                        {brl.format(s.totalPaid)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                        {s.paymentCount}
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
// View: Posição por fornecedor
// =====================================================================

function SupplierPositionView({ data }: { data: PayableSupplierPositionReportResponse }) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
        <h2 className="text-base font-semibold">Posição por fornecedor</h2>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Referência: {formatDate(data.referenceDate)} • ordenado por total a pagar (desc)
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
            <tr>
              <th className="px-4 py-3 font-medium">Fornecedor</th>
              <th className="px-4 py-3 font-medium text-right">A pagar</th>
              <th className="px-4 py-3 font-medium text-right">Pago</th>
              <th className="px-4 py-3 font-medium text-right">Parcelas em aberto</th>
              <th className="px-4 py-3 font-medium text-right">Em atraso</th>
              <th className="px-4 py-3 font-medium text-right">Maior atraso (dias)</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
            {data.suppliers.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                  Sem fornecedores para os filtros informados.
                </td>
              </tr>
            ) : (
              data.suppliers.map((s) => (
                <tr key={s.supplierId}>
                  <td className="px-4 py-3">
                    <div className="font-medium text-slate-800 dark:text-slate-200">
                      {s.supplierName ?? '—'}
                    </div>
                    <div className="text-xs text-slate-500 dark:text-slate-400">
                      {s.supplierTaxId ?? '—'}
                    </div>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                    {brl.format(s.totalToPay)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {brl.format(s.totalPaid)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {s.openCount}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {s.overdueCount > 0 ? (
                      <span className="text-red-600 dark:text-red-400">{s.overdueCount}</span>
                    ) : (
                      s.overdueCount
                    )}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {s.maxOverdueDays > 0 ? (
                      <span className="text-red-600 dark:text-red-400">{s.maxOverdueDays}</span>
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