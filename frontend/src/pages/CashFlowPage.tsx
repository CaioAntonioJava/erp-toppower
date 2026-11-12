import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import {
  ArrowDownCircle,
  ArrowUpCircle,
  Calendar,
  Download,
  Pencil,
  Search,
  TrendingUp,
  Wallet,
  X,
} from 'lucide-react'
import { Alert } from '../components/ui/Alert'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import {
  getCashFlowSettings,
  listCashFlowEntries,
  searchCashFlowClients,
  updateCashFlowSettings,
  getCashFlowSummary,
} from '../api/cashFlow.api'
import { toApiError } from '../lib/errors'
import { formatBRLValue, parseNumber } from '../lib/money'
import type { ClientSummaryResponse } from '../types/contract'
import {
  CASH_FLOW_ENTRY_TYPE_LABELS,
  CASH_FLOW_SOURCE_LABELS,
  cashFlowSourceRoute,
} from '../types/cashFlow'
import type {
  CashFlowClientType,
  CashFlowDocumentSource,
  CashFlowEntriesPage,
  CashFlowEntryResponse,
  CashFlowEntryType,
  CashFlowFilters,
  CashFlowGranularity,
  CashFlowSettingsResponse,
  CashFlowSummaryResponse,
} from '../types/cashFlow'

/** Tamanho da página da tabela de lançamentos. */
const PAGE_SIZE = 20

const TYPE_OPTIONS = [
  { value: 'ALL', label: 'Entradas e saídas' },
  { value: 'ENTRADA', label: 'Entradas' },
  { value: 'SAIDA', label: 'Saídas' },
]

const SOURCE_OPTIONS = [
  { value: 'ALL', label: 'Todos os documentos' },
  { value: 'SALES_ORDER', label: 'Pedido de Venda' },
  { value: 'TECHNICAL_PROPOSAL', label: 'Proposta Técnica' },
  { value: 'CONTRACT', label: 'Contrato' },
  { value: 'MANUAL', label: 'Manual' },
  { value: 'BOLETO', label: 'Boleto' },
  { value: 'PURCHASE_INVOICE', label: 'Nota de Compra (NF-e)' },
]

const CLIENT_TYPE_OPTIONS = [
  { value: 'ALL', label: 'PF e PJ' },
  { value: 'CUSTOMER', label: 'Pessoa Física (PF)' },
  { value: 'COMPANY', label: 'Pessoa Jurídica (PJ)' },
]

const GRANULARITY_OPTIONS = [
  { value: 'MONTH', label: 'Mensal' },
  { value: 'YEAR', label: 'Anual' },
]

/** Data atual no formato ISO yyyy-MM-dd (fuso local, como no backend). */
function todayIso(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

/** Soma dias a uma data ISO yyyy-MM-dd (negativo = passado). */
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
  d.setDate(0)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

/** Formata data ISO curta (yyyy-MM-dd) em dd/mm/aaaa com fuso local. */
function formatDateIso(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

/** Escapa uma célula para CSV (delimitador ";", aspas duplicadas). */
function csvCell(value: string | number | null | undefined): string {
  if (value == null) return ''
  const s = String(value)
  return /[";\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

/**
 * Página do Fluxo de Caixa: saldo inicial configurável, lançamentos
 * consolidados (entradas de contas a receber + saídas de contas a
 * pagar) com filtros e totais por mês/ano/período.
 */
export function CashFlowPage() {
  // -------------------------------------------------------------------
  // Estado: configuração (saldo inicial)
  // -------------------------------------------------------------------
  const [settings, setSettings] = useState<CashFlowSettingsResponse | null>(null)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [openingStr, setOpeningStr] = useState('')
  const [savingSettings, setSavingSettings] = useState(false)
  const [settingsError, setSettingsError] = useState<string | null>(null)

  // -------------------------------------------------------------------
  // Estado: filtros
  // -------------------------------------------------------------------
  const [from, setFrom] = useState<string>(firstOfMonthIso())
  const [to, setTo] = useState<string>(todayIso())
  const [typeFilter, setTypeFilter] = useState<CashFlowEntryType | 'ALL'>('ALL')
  const [sourceFilter, setSourceFilter] = useState<CashFlowDocumentSource | 'ALL'>('ALL')
  const [clientTypeFilter, setClientTypeFilter] = useState<CashFlowClientType | 'ALL'>('ALL')
  const [selectedClient, setSelectedClient] = useState<ClientSummaryResponse | null>(null)
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)

  // -------------------------------------------------------------------
  // Estado: dados
  // -------------------------------------------------------------------
  const [summary, setSummary] = useState<CashFlowSummaryResponse | null>(null)
  const [entries, setEntries] = useState<CashFlowEntriesPage | null>(null)
  const [granularity, setGranularity] = useState<CashFlowGranularity>('MONTH')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)

  /** Parâmetros compartilhados por resumo e lançamentos ('ALL' → undefined). */
  const filterParams: CashFlowFilters = useMemo(
    () => ({
      startDate: from || undefined,
      endDate: to || undefined,
      type: typeFilter === 'ALL' ? undefined : typeFilter,
      sourceType: sourceFilter === 'ALL' ? undefined : sourceFilter,
      clientType: clientTypeFilter === 'ALL' ? undefined : clientTypeFilter,
      clientId: selectedClient?.id,
      query: query.trim() ? query.trim() : undefined,
    }),
    [from, to, typeFilter, sourceFilter, clientTypeFilter, selectedClient, query],
  )

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [summaryData, entriesData] = await Promise.all([
        getCashFlowSummary(filterParams),
        listCashFlowEntries({ ...filterParams, page, size: PAGE_SIZE }),
      ])
      setSummary(summaryData)
      setEntries(entriesData)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
  }, [filterParams, page])

  useEffect(() => {
    void reload()
  }, [reload])

  // Carrega o saldo inicial uma vez (e após cada atualização).
  const reloadSettings = useCallback(async () => {
    try {
      setSettings(await getCashFlowSettings())
    } catch (err) {
      setError(toApiError(err).message)
    }
  }, [])

  useEffect(() => {
    void reloadSettings()
  }, [reloadSettings])

  // Debounce da busca livre (300ms, padrão do projeto).
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(queryInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [queryInput])

  // Atalhos rápidos de período.
  function applyShortcut(kind: 'today' | '7d' | '30d' | 'thisMonth' | 'prevMonth' | 'thisYear') {
    const today = todayIso()
    if (kind === 'today') {
      setFrom(today)
      setTo(today)
    } else if (kind === '7d') {
      setFrom(addDaysIso(today, -6))
      setTo(today)
    } else if (kind === '30d') {
      setFrom(addDaysIso(today, -29))
      setTo(today)
    } else if (kind === 'thisMonth') {
      setFrom(firstOfMonthIso())
      setTo(today)
    } else if (kind === 'prevMonth') {
      setFrom(firstOfPrevMonthIso())
      setTo(lastOfPrevMonthIso())
    } else {
      setFrom(`${today.slice(0, 4)}-01-01`)
      setTo(today)
    }
    setPage(0)
  }

  // -------------------------------------------------------------------
  // Saldo inicial (modal)
  // -------------------------------------------------------------------
  function openSettingsModal() {
    setOpeningStr(formatBRLValue(settings?.openingBalance ?? 0))
    setSettingsError(null)
    setSettingsOpen(true)
  }

  async function handleSaveSettings() {
    const value = parseNumber(openingStr)
    if (value == null) {
      setSettingsError('Informe um valor válido para o saldo inicial.')
      return
    }
    setSavingSettings(true)
    setSettingsError(null)
    try {
      setSettings(await updateCashFlowSettings({ openingBalance: value }))
      setSettingsOpen(false)
      await reload()
    } catch (err) {
      setSettingsError(toApiError(err).message)
    } finally {
      setSavingSettings(false)
    }
  }

  // -------------------------------------------------------------------
  // Exportação CSV (todos os lançamentos filtrados)
  // -------------------------------------------------------------------
  async function handleExportCsv() {
    setExporting(true)
    setError(null)
    try {
      const rows: Array<Array<string | number | null>> = [
        ['Data', 'Tipo', 'Documento', 'Cliente/Fornecedor', 'Descrição', 'Observação', 'Valor (R$)', 'Saldo após (R$)'],
      ]
      for (let csvPage = 0; ; csvPage++) {
        const data = await listCashFlowEntries({ ...filterParams, page: csvPage, size: 200 })
        for (const entry of data.content) {
          rows.push([
            formatDateIso(entry.date),
            CASH_FLOW_ENTRY_TYPE_LABELS[entry.entryType],
            entry.documentLabel,
            counterpartyOf(entry),
            entry.description,
            entry.notes,
            signedAmount(entry),
            entry.balanceAfter,
          ])
        }
        if (data.last || data.content.length === 0) break
      }
      const csv =
        '\uFEFF' +
        rows
          .map((row) =>
            row
              .map((cell, index) =>
                index >= 6 && typeof cell === 'number'
                  ? cell.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
                  : csvCell(cell),
              )
              .join(';'),
          )
          .join('\r\n')
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `fluxo-de-caixa-${from || 'inicio'}-${to || todayIso()}.csv`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setExporting(false)
    }
  }

  function handleSelectClient(client: ClientSummaryResponse | null) {
    setSelectedClient(client)
    setPage(0)
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Fluxo de Caixa</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Controle consolidado de entradas (contas a receber) e saídas (contas a pagar),
            ligadas a pedidos de venda, propostas técnicas, contratos, boletos e notas de compra.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" onClick={openSettingsModal}>
            <Pencil className="mr-1.5 h-4 w-4" />
            Saldo inicial
          </Button>
          <Button variant="primary" onClick={handleExportCsv} disabled={exporting || loading}>
            <Download className="mr-1.5 h-4 w-4" />
            {exporting ? 'Exportando…' : 'Exportar CSV'}
          </Button>
        </div>
      </div>

      {/* Filtros */}
      <section className="mb-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <Input
              type="date"
              label="De"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value)
                setPage(0)
              }}
              max={to || undefined}
              aria-label="Data inicial do período"
            />
          </div>
          <div>
            <Input
              type="date"
              label="Até"
              value={to}
              onChange={(e) => {
                setTo(e.target.value)
                setPage(0)
              }}
              min={from || undefined}
              aria-label="Data final do período"
            />
          </div>
          <div className="min-w-[170px]">
            <Select
              label="Tipo"
              options={TYPE_OPTIONS}
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value as CashFlowEntryType | 'ALL')
                setPage(0)
              }}
              aria-label="Filtrar por tipo de movimento"
            />
          </div>
          <div className="min-w-[190px]">
            <Select
              label="Documento de origem"
              options={SOURCE_OPTIONS}
              value={sourceFilter}
              onChange={(e) => {
                setSourceFilter(e.target.value as CashFlowDocumentSource | 'ALL')
                setPage(0)
              }}
              aria-label="Filtrar por documento de origem"
            />
          </div>
          <div className="min-w-[170px]">
            <Select
              label="Tipo de cliente"
              options={CLIENT_TYPE_OPTIONS}
              value={clientTypeFilter}
              onChange={(e) => {
                setClientTypeFilter(e.target.value as CashFlowClientType | 'ALL')
                setPage(0)
              }}
              aria-label="Filtrar por tipo de cliente"
            />
          </div>
          <ClientTypeahead selected={selectedClient} onSelect={handleSelectClient} />
          <div className="min-w-[200px]">
            <Input
              label="Buscar"
              value={queryInput}
              onChange={(e) => setQueryInput(e.target.value)}
              placeholder="Cliente, documento, descrição…"
              leftAdornment={<Search className="h-4 w-4" />}
              aria-label="Busca livre nos lançamentos"
            />
          </div>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
            <Calendar className="mr-1 inline h-3.5 w-3.5" />
            Atalhos:
          </span>
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
          <Button size="sm" variant="ghost" onClick={() => applyShortcut('thisYear')}>
            Ano atual
          </Button>
        </div>
      </section>

      {error ? <Alert variant="error">{error}</Alert> : null}

      {loading ? (
        <div className="flex justify-center py-12">
          <Spinner className="h-8 w-8 text-primary" />
        </div>
      ) : (
        <>
          {/* Resumo do período */}
          {summary ? (
            <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
              <Card
                title="Saldo inicial"
                value={brl.format(summary.openingBalance)}
                hint={
                  settings?.updatedAt
                    ? `Atualizado em ${formatDateIso(settings.updatedAt.slice(0, 10))}`
                    : 'Não configurado'
                }
                icon={<Wallet className="h-5 w-5" />}
              />
              <Card
                title="Saldo anterior"
                value={brl.format(summary.saldoAnterior)}
                hint="Caixa no início do período"
                valueClass={resultadoClass(summary.saldoAnterior)}
              />
              <Card
                title="Entradas"
                value={brl.format(summary.totalEntradas)}
                hint={`${summary.entradasCount} lançamento(s)`}
                icon={<ArrowDownCircle className="h-5 w-5" />}
                valueClass="text-emerald-600 dark:text-emerald-400"
              />
              <Card
                title="Saídas"
                value={brl.format(summary.totalSaidas)}
                hint={`${summary.saidasCount} lançamento(s)`}
                icon={<ArrowUpCircle className="h-5 w-5" />}
                valueClass="text-red-600 dark:text-red-400"
              />
              <Card
                title="Resultado"
                value={brl.format(summary.resultado)}
                hint="Entradas − saídas no período"
                icon={<TrendingUp className="h-5 w-5" />}
                valueClass={resultadoClass(summary.resultado)}
              />
              <Card
                title="Saldo final"
                value={brl.format(summary.saldoAnterior + summary.resultado)}
                hint="Caixa ao fim do período"
                valueClass={resultadoClass(summary.saldoAnterior + summary.resultado)}
              />
            </div>
          ) : null}

          {/* Totais por período */}
          {summary ? (
            <section className="mb-4 rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 px-4 py-3 dark:border-slate-800">
                <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Totais por período
                </h2>
                <div className="min-w-[130px]">
                  <Select
                    options={GRANULARITY_OPTIONS}
                    value={granularity}
                    onChange={(e) => setGranularity(e.target.value as CashFlowGranularity)}
                    aria-label="Granularidade da série por período"
                  />
                </div>
              </div>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                  <thead>
                    <tr className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      <th className="px-4 py-3">Período</th>
                      <th className="px-4 py-3 text-right">Entradas</th>
                      <th className="px-4 py-3 text-right">Saídas</th>
                      <th className="px-4 py-3 text-right">Resultado</th>
                      <th className="px-4 py-3 text-right">Saldo do caixa</th>
                      <th className="px-4 py-3 text-right">Lançamentos</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                    {summary.periods.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-4 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
                          Nenhum lançamento no período com os filtros aplicados.
                        </td>
                      </tr>
                    ) : (
                      summary.periods.map((row) => (
                        <tr key={row.periodStart} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                          <td className="px-4 py-3 font-medium text-slate-800 dark:text-slate-200">{row.label}</td>
                          <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs text-emerald-600 dark:text-emerald-400">
                            {brl.format(row.entradas)}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs text-red-600 dark:text-red-400">
                            {brl.format(row.saidas)}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                            <span className={resultadoClass(row.resultado)}>{brl.format(row.resultado)}</span>
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                            <span className={resultadoClass(row.saldoAcumulado)}>{brl.format(row.saldoAcumulado)}</span>
                          </td>
                          <td className="px-4 py-3 text-right font-mono text-xs text-slate-500 dark:text-slate-400">
                            {row.count}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          {/* Lançamentos */}
          {entries ? (
            <section className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
                <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Lançamentos ({entries.totalElements})
                </h2>
              </div>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                  <thead>
                    <tr className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                      <th className="px-4 py-3">Data</th>
                      <th className="px-4 py-3">Tipo</th>
                      <th className="px-4 py-3">Documento</th>
                      <th className="px-4 py-3">Cliente / Fornecedor</th>
                      <th className="px-4 py-3">Descrição</th>
                      <th className="px-4 py-3 text-right">Valor</th>
                      <th className="px-4 py-3 text-right">Saldo após</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                    {entries.content.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="px-4 py-10 text-center text-sm text-slate-500 dark:text-slate-400">
                          Nenhum lançamento encontrado com os filtros aplicados.
                        </td>
                      </tr>
                    ) : (
                      entries.content.map((entry) => {
                        const route = cashFlowSourceRoute(entry.sourceType, entry.sourceDocId)
                        return (
                          <tr key={`${entry.entryType}-${entry.id}`} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                            <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                              {formatDateIso(entry.date)}
                            </td>
                            <td className="px-4 py-3">
                              <Badge tone={entry.entryType === 'ENTRADA' ? 'success' : 'danger'}>
                                {CASH_FLOW_ENTRY_TYPE_LABELS[entry.entryType]}
                              </Badge>
                            </td>
                            <td className="px-4 py-3">
                              {route ? (
                                <a
                                  href={route}
                                  className="font-medium text-primary hover:underline"
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  {entry.documentLabel}
                                </a>
                              ) : (
                                <span className="text-slate-700 dark:text-slate-300">{entry.documentLabel}</span>
                              )}
                            </td>
                            <td className="px-4 py-3">
                              <div className="font-medium text-slate-800 dark:text-slate-200">
                                {counterpartyOf(entry)}
                              </div>
                              <div className="text-xs text-slate-500 dark:text-slate-400">
                                {entry.entryType === 'ENTRADA'
                                  ? (entry.clientCode ?? '')
                                  : entry.sourceType === 'BOLETO' || entry.sourceType === 'PURCHASE_INVOICE'
                                    ? CASH_FLOW_SOURCE_LABELS[entry.sourceType]
                                    : ''}
                              </div>
                            </td>
                            <td className="max-w-[280px] truncate px-4 py-3 text-slate-600 dark:text-slate-300" title={entry.description ?? ''}>
                              {entry.description ?? '—'}
                              {entry.notes ? (
                                <div className="truncate text-xs text-slate-400 dark:text-slate-500" title={entry.notes}>
                                  {entry.notes}
                                </div>
                              ) : null}
                            </td>
                            <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                              <span className={entry.entryType === 'ENTRADA' ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'}>
                                {entry.entryType === 'ENTRADA' ? '+' : '−'}
                                {brl.format(entry.amount)}
                              </span>
                            </td>
                            <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                              <span className={resultadoClass(entry.balanceAfter)}>{brl.format(entry.balanceAfter)}</span>
                            </td>
                          </tr>
                        )
                      })
                    )}
                  </tbody>
                </table>
              </div>

              {/* Paginação */}
              <div className="flex items-center justify-between gap-2 border-t border-slate-200 px-4 py-3 text-sm dark:border-slate-800">
                <span className="text-slate-500 dark:text-slate-400">
                  {entries.totalElements} lançamento(s) — Página {entries.page + 1} de {entries.totalPages || 1}
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
                  <Button
                    size="sm"
                    variant="secondary"
                    disabled={entries.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Próxima
                  </Button>
                </div>
              </div>
            </section>
          ) : null}
        </>
      )}

      {/* Modal: saldo inicial */}
      <Modal
        open={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        title="Saldo inicial do caixa"
        description="Valor existente no caixa antes do primeiro movimento registrado no sistema. Todas as entradas e saídas são somadas a este saldo."
        maxWidth="max-w-md"
        footer={
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setSettingsOpen(false)}>
              Cancelar
            </Button>
            <Button variant="primary" onClick={handleSaveSettings} disabled={savingSettings}>
              {savingSettings ? 'Salvando…' : 'Salvar'}
            </Button>
          </div>
        }
      >
        <div className="space-y-3">
          {settingsError ? <Alert variant="error">{settingsError}</Alert> : null}
          <Input
            label="Saldo inicial (R$)"
            value={openingStr}
            onChange={(e) => setOpeningStr(e.target.value)}
            hint="Aceita valor negativo. Ex.: 10.000,00"
            aria-label="Saldo inicial do caixa em reais"
          />
          {settings?.updatedBy ? (
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Última atualização por {settings.updatedBy}
              {settings.updatedAt ? ` em ${formatDateIso(settings.updatedAt.slice(0, 10))}` : ''}.
            </p>
          ) : null}
        </div>
      </Modal>
    </div>
  )
}

/** Nome da contraparte do lançamento (cliente na entrada, fornecedor na saída). */
function counterpartyOf(entry: CashFlowEntryResponse): string {
  if (entry.entryType === 'ENTRADA') {
    return entry.clientName ?? '—'
  }
  return entry.counterpartyName ?? '—'
}

/** Valor com sinal para o CSV: entrada positivo, saída negativo. */
function signedAmount(entry: CashFlowEntryResponse): number {
  return entry.entryType === 'ENTRADA' ? entry.amount : -entry.amount
}

/** Classe de cor para valores que podem ser negativos. */
function resultadoClass(value: number): string {
  return value < 0 ? 'text-red-600 dark:text-red-400' : 'text-slate-900 dark:text-slate-100'
}

// =====================================================================
// Card de resumo
// =====================================================================

function Card({
  title,
  value,
  hint,
  icon,
  valueClass,
}: {
  title: string
  value: string
  hint?: string
  icon?: ReactNode
  /** Classes extras de cor para o valor (ex.: positivo/negativo). */
  valueClass?: string
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
        {icon}
        {title}
      </div>
      <div className={`mt-1 text-lg font-semibold ${valueClass ?? 'text-slate-900 dark:text-slate-100'}`}>
        {value}
      </div>
      {hint ? (
        <div className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{hint}</div>
      ) : null}
    </div>
  )
}

// =====================================================================
// Typeahead de cliente (PF + PJ) para o filtro
// =====================================================================

function ClientTypeahead({
  selected,
  onSelect,
}: {
  selected: ClientSummaryResponse | null
  onSelect: (client: ClientSummaryResponse | null) => void
}) {
  const [text, setText] = useState('')
  const [options, setOptions] = useState<ClientSummaryResponse[]>([])
  const [open, setOpen] = useState(false)
  const [searching, setSearching] = useState(false)
  const wrapperRef = useRef<HTMLDivElement | null>(null)

  // Sincroniza o campo quando a seleção muda externamente (ex.: limpar filtro).
  useEffect(() => {
    setText(selected ? selected.name : '')
  }, [selected])

  // Busca com debounce — apenas quando não há cliente selecionado.
  useEffect(() => {
    if (selected) return
    const trimmed = text.trim()
    if (trimmed.length < 2) {
      setOptions([])
      setOpen(false)
      return
    }
    let cancelled = false
    const timer = setTimeout(async () => {
      setSearching(true)
      try {
        const results = await searchCashFlowClients(trimmed, 8)
        if (cancelled) return
        setOptions(results)
        setOpen(results.length > 0)
      } catch {
        if (!cancelled) {
          setOptions([])
          setOpen(false)
        }
      } finally {
        if (!cancelled) setSearching(false)
      }
    }, 300)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [text, selected])

  // Fecha o dropdown ao clicar fora.
  useEffect(() => {
    if (!open) return
    function onPointerDown(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [open])

  function pick(client: ClientSummaryResponse) {
    onSelect(client)
    setOpen(false)
    setOptions([])
  }

  return (
    <div className="relative min-w-[230px]" ref={wrapperRef}>
      <Input
        label="Cliente"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Nome, código ou CPF/CNPJ"
        rightAdornment={
          selected ? (
            <button
              type="button"
              onClick={() => onSelect(null)}
              className="rounded p-0.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800"
              aria-label="Limpar filtro de cliente"
            >
              <X className="h-4 w-4" />
            </button>
          ) : (
            <Search className="h-4 w-4" />
          )
        }
        aria-label="Filtrar por cliente"
      />
      {open ? (
        <div className="absolute z-20 mt-1 max-h-60 w-full overflow-y-auto rounded-lg border border-slate-200 bg-white shadow-lg dark:border-slate-700 dark:bg-slate-900">
          {searching ? (
            <div className="px-3 py-2 text-xs text-slate-500 dark:text-slate-400">Buscando…</div>
          ) : (
            options.map((client) => (
              <button
                key={`${client.type}-${client.id}`}
                type="button"
                className="flex w-full flex-col items-start px-3 py-2 text-left text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
                onClick={() => pick(client)}
              >
                <span className="font-medium text-slate-800 dark:text-slate-200">{client.name}</span>
                <span className="text-xs text-slate-500 dark:text-slate-400">
                  {client.code}
                  {client.document ? ` · ${client.document}` : ''}
                </span>
              </button>
            ))
          )}
        </div>
      ) : null}
    </div>
  )
}