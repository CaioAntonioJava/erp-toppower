import { useCallback, useEffect, useState } from 'react'
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  Paperclip,
  Receipt,
  Wallet,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { Badge } from '../components/ui/Badge'
import { BoletoAttachmentsModal } from '../components/dashboard/BoletoAttachmentsModal'
import {
  downloadBoletoPaymentReceipt,
  listBoletosReport,
} from '../api/boleto.api'
import type { BoletoResponse } from '../types/boleto'
import type { RegistrationStatus } from '../types/registration'
import type { PagedResponse } from '../types/api'
import { formatCurrency, formatDate } from '../lib/format'
import { toApiError } from '../lib/errors'

/** Opções do filtro de status de pagamento. */
const PAID_OPTIONS = [
  { value: 'ALL', label: 'Todos' },
  { value: 'OPEN', label: 'Em aberto' },
  { value: 'PAID', label: 'Pagos' },
]

/** Opções do filtro de status de registro. */
const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Todos' },
  { value: 'ATIVO', label: 'Ativo' },
  { value: 'INATIVO', label: 'Inativo' },
]

/** Retorna hoje no formato ISO (yyyy-MM-dd). */
function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

/** Retorna (hoje + dias) no formato ISO. */
function addDaysIso(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

/** Primeiro dia do mês atual. */
function firstOfMonthIso(): string {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)
}

/** Primeiro dia do mês anterior. */
function firstOfPrevMonthIso(): string {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth() - 1, 1).toISOString().slice(0, 10)
}

/** Último dia do mês anterior. */
function lastOfPrevMonthIso(): string {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), 0).toISOString().slice(0, 10)
}

/** Calcula dias até o vencimento a partir de uma data ISO (negativo = vencido). */
function diasAteVencimento(dataVencimento: string): number {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const venc = new Date(`${dataVencimento}T00:00:00`)
  venc.setHours(0, 0, 0, 0)
  const msPorDia = 24 * 60 * 60 * 1000
  return Math.round((venc.getTime() - hoje.getTime()) / msPorDia)
}

/** Monta um rótulo legível para o boleto (nº obra + responsável, se houver). */
function labelBoleto(contractWorkNumber: string | null, responsibleName: string | null): string {
  const obra = contractWorkNumber ?? ''
  const resp = responsibleName ?? ''
  if (obra && resp) return `${obra} · ${resp}`
  return obra || resp || 'Boleto sem identificação'
}

/**
 * Página de relatório de boletos — lista paginada e filtrada por status
 * de pagamento e status de registro. O filtro de período (Hoje, Últimos
 * 7 dias, etc.) aplica-se apenas a boletos <b>pagos</b> e filtra pela
 * <b>data de pagamento</b>. Cada linha permite acessar os anexos do
 * boleto e o comprovante de pagamento (quando liquidado). Espelha a
 * estrutura do `PayablesListPage`.
 */
export function BoletosListPage() {
  const [paidFilter, setPaidFilter] = useState<'ALL' | 'OPEN' | 'PAID'>('ALL')
  const [statusFilter, setStatusFilter] = useState<RegistrationStatus | 'ALL'>('ALL')
  const [contractWorkFilter, setContractWorkFilter] = useState('')
  const [dueFrom, setDueFrom] = useState('')
  const [dueTo, setDueTo] = useState('')
  const [page, setPage] = useState(0)
  const size = 20

  const [data, setData] = useState<PagedResponse<BoletoResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [anexosBoleto, setAnexosBoleto] = useState<{ id: number; label: string } | null>(null)
  const [receiptLoading, setReceiptLoading] = useState<number | null>(null)
  const [receiptError, setReceiptError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      // O intervalo de datas (dueFrom/dueTo) só é enviado quando o filtro
      // é "Pagos" — ele filtra pela data de pagamento. Para "Todos" e
      // "Em aberto" o período não se aplica e é ignorado pelo backend.
      const isPaid = paidFilter === 'PAID'
      const result = await listBoletosReport({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        paid: paidFilter === 'ALL' ? undefined : isPaid,
        dueFrom: isPaid ? (dueFrom || undefined) : undefined,
        dueTo: isPaid ? (dueTo || undefined) : undefined,
        contractWorkNumber: contractWorkFilter.trim() || undefined,
        page,
        size,
      })
      setData(result)
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paidFilter, statusFilter, contractWorkFilter, dueFrom, dueTo, page])

  useEffect(() => {
    reload()
  }, [reload])

  // Reseta a páginação sempre que os filtros mudam.
  useEffect(() => {
    setPage(0)
  }, [paidFilter, statusFilter, contractWorkFilter, dueFrom, dueTo])

  /** Abre o comprovante de pagamento em nova aba (inline). */
  async function handleReceipt(boleto: BoletoResponse): Promise<void> {
    setReceiptError(null)
    setReceiptLoading(boleto.id)
    try {
      const { blob, contentType } = await downloadBoletoPaymentReceipt(boleto.id)
      const url = URL.createObjectURL(blob)
      const win = window.open(url, '_blank')
      if (win && contentType === 'application/pdf') {
        win.onload = () => win.print()
      }
      setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch (err) {
      setReceiptError(toApiError(err).message)
    } finally {
      setReceiptLoading(null)
    }
  }

  const items = data?.content ?? []
  const totalElements = data?.totalElements ?? 0
  const totalPages = data?.totalPages ?? 0

  // O intervalo de datas só é enviado quando o filtro é "Pagos" (filtra pela
  // data de pagamento). Para "Todos" e "Em aberto" o período não se aplica.
  const periodoHabilitado = paidFilter === 'PAID'
  const periodoLabel = 'Pagamento'

  // Número de colunas da tabela (para colspan de loading/empty).
  const COL_SPAN = 11

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Relatório de Boletos</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Todos os boletos cadastrados — pagos, em aberto e vencidos. Filtre por
          status, e por período de pagamento quando quiser ver os pagos.
        </p>
      </div>

      {/* Filtros */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid gap-3 sm:grid-cols-[200px_200px_180px_180px]">
          <Select
            options={PAID_OPTIONS}
            value={paidFilter}
            onChange={(e) => {
              // O período só se aplica a "Pagos" (filtra pela data de
              // pagamento). Ao sair de "Pagos", limpa o período para não
              // confundir o usuário com valores que serão ignorados.
              setDueFrom('')
              setDueTo('')
              setPaidFilter(e.target.value as 'ALL' | 'OPEN' | 'PAID')
            }}
            aria-label="Filtrar por status de pagamento"
          />
          <Select
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as RegistrationStatus | 'ALL')
            }
            aria-label="Filtrar por status de registro"
          />
          <Input
            placeholder="Nº Obra"
            value={contractWorkFilter}
            onChange={(e) => setContractWorkFilter(e.target.value)}
            aria-label="Filtrar por Nº Obra"
          />
          <div className="w-full">
            <Input
              type="date"
              value={dueFrom}
              onChange={(e) => setDueFrom(e.target.value)}
              max={dueTo || undefined}
              disabled={!periodoHabilitado}
              aria-label="Filtrar por período a partir de"
            />
            <p className="mt-1.5 text-sm text-slate-700 dark:text-slate-200">
              {periodoLabel} de
            </p>
          </div>
          <div className="w-full">
            <Input
              type="date"
              value={dueTo}
              onChange={(e) => setDueTo(e.target.value)}
              min={dueFrom || undefined}
              disabled={!periodoHabilitado}
              aria-label="Filtrar por período até"
            />
            <p className="mt-1.5 text-sm text-slate-700 dark:text-slate-200">
              {periodoLabel} até
            </p>
          </div>
        </div>

        {/* Atalhos de período — só aplicam a boletos pagos (filtram pela
            data de pagamento). Para "Todos" e "Em aberto" ficam desabilitados. */}
        <div className="mt-3 flex flex-wrap gap-2">
          <Button size="sm" variant="ghost" disabled={!periodoHabilitado} onClick={() => { setDueFrom(todayIso()); setDueTo(todayIso()) }}>
            Hoje
          </Button>
          <Button size="sm" variant="ghost" disabled={!periodoHabilitado} onClick={() => { setDueFrom(addDaysIso(-7)); setDueTo(todayIso()) }}>
            Últimos 7 dias
          </Button>
          <Button size="sm" variant="ghost" disabled={!periodoHabilitado} onClick={() => { setDueFrom(addDaysIso(-30)); setDueTo(todayIso()) }}>
            Últimos 30 dias
          </Button>
          <Button size="sm" variant="ghost" disabled={!periodoHabilitado} onClick={() => { setDueFrom(firstOfMonthIso()); setDueTo(todayIso()) }}>
            Mês atual
          </Button>
          <Button size="sm" variant="ghost" disabled={!periodoHabilitado} onClick={() => { setDueFrom(firstOfPrevMonthIso()); setDueTo(lastOfPrevMonthIso()) }}>
            Mês passado
          </Button>
          <Button size="sm" variant="ghost" disabled={!periodoHabilitado} onClick={() => { setDueFrom(''); setDueTo('') }}>
            Limpar período
          </Button>
        </div>
        {!periodoHabilitado ? (
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            O período de pagamento aplica-se apenas ao filtro “Pagos”.
          </p>
        ) : null}
      </div>

      {error ? <Alert variant="error">{error}</Alert> : null}
      {receiptError ? <Alert variant="error">{receiptError}</Alert> : null}

      {/* Tabela — modo paisagem: largura mínima para acomodar todas as
          colunas sem compressão excessiva em telas largas. */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-[1100px] divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Nº Obra</th>
                <th className="px-4 py-3 font-medium">Responsável</th>
                <th className="px-4 py-3 font-medium">Empresa</th>
                <th className="px-4 py-3 font-medium">NF</th>
                <th className="px-4 py-3 font-medium">Data NF</th>
                <th className="px-4 py-3 text-center font-medium">Parcela</th>
                <th className="px-4 py-3 text-right font-medium">Valor</th>
                <th className="px-4 py-3 font-medium">Vencimento</th>
                <th className="px-4 py-3 font-medium">Pagamento</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={COL_SPAN} className="px-4 py-12 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={COL_SPAN} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Wallet className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhum boleto encontrado.</p>
                    </div>
                  </td>
                </tr>
              ) : (
                items.map((boleto) => {
                  const dias = diasAteVencimento(boleto.dueDate)
                  const vencido = !boleto.paid && dias < 0
                  return (
                    <tr
                      key={boleto.id}
                      className="hover:bg-slate-50 dark:hover:bg-slate-800/40"
                    >
                      {/* Nº Obra */}
                      <td className="px-4 py-3">
                        <span className="line-clamp-1 text-xs font-medium text-slate-900 dark:text-slate-100">
                          {boleto.contractWorkNumber ?? '—'}
                        </span>
                      </td>
                      {/* Responsável */}
                      <td className="px-4 py-3">
                        <span className="text-xs text-slate-600 dark:text-slate-300">
                          {boleto.responsibleName ?? '—'}
                        </span>
                      </td>
                      {/* Empresa */}
                      <td className="px-4 py-3">
                        {boleto.supplierName ? (
                          <span className="text-xs text-slate-900 dark:text-slate-100">
                            {boleto.supplierName}
                          </span>
                        ) : (
                          <span className="text-xs text-slate-400 dark:text-slate-500">—</span>
                        )}
                      </td>
                      {/* NF */}
                      <td className="px-4 py-3">
                        <span className="text-xs text-slate-600 dark:text-slate-300">
                          {boleto.invoiceNumber ?? '—'}
                        </span>
                      </td>
                      {/* Data NF */}
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                        {boleto.invoiceDate ? formatDate(boleto.invoiceDate) : '—'}
                      </td>
                      {/* Parcela */}
                      <td className="px-4 py-3 text-center text-slate-600 dark:text-slate-300">
                        {boleto.installmentNumber ?? '—'}
                      </td>
                      {/* Valor */}
                      <td className="whitespace-nowrap px-4 py-3 text-right text-slate-900 dark:text-slate-100">
                        {formatCurrency(boleto.value)}
                      </td>
                      {/* Vencimento */}
                      <td className="whitespace-nowrap px-4 py-3">
                        <span
                          className={
                            vencido
                              ? 'font-medium text-red-600 dark:text-red-400'
                              : 'text-slate-600 dark:text-slate-300'
                          }
                        >
                          {formatDate(boleto.dueDate)}
                          {vencido ? ' • vencido' : ''}
                        </span>
                      </td>
                      {/* Data de pagamento */}
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                        {boleto.paymentDate ? formatDate(boleto.paymentDate) : '—'}
                      </td>
                      {/* Status */}
                      <td className="px-4 py-3">
                        {boleto.paid ? (
                          <Badge tone="success" className="shrink-0">
                            <CheckCircle2 className="mr-0.5 h-3 w-3" />
                            Pago
                          </Badge>
                        ) : vencido ? (
                          <Badge tone="danger" className="shrink-0">
                            <AlertTriangle className="mr-0.5 h-3 w-3" />
                            Vencido
                          </Badge>
                        ) : (
                          <Badge tone="warning" className="shrink-0">
                            <Clock className="mr-0.5 h-3 w-3" />
                            {dias}d
                          </Badge>
                        )}
                      </td>
                      {/* Ações */}
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          {/* Anexos do boleto */}
                          <span
                            role="button"
                            tabIndex={0}
                            onClick={() => {
                              setReceiptError(null)
                              setAnexosBoleto({
                                id: boleto.id,
                                label: labelBoleto(boleto.contractWorkNumber, boleto.responsibleName),
                              })
                            }}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' || e.key === ' ') {
                                setReceiptError(null)
                                setAnexosBoleto({
                                  id: boleto.id,
                                  label: labelBoleto(boleto.contractWorkNumber, boleto.responsibleName),
                                })
                              }
                            }}
                            className="rounded p-1 text-slate-400 transition-opacity hover:bg-primary-50 hover:text-primary dark:hover:bg-primary-900/30"
                            aria-label="Anexos do boleto"
                            title="Anexos"
                          >
                            <Paperclip className="h-4 w-4" />
                          </span>
                          {/* Comprovante de pagamento (só se pago) */}
                          {boleto.paid ? (
                            <span
                              role="button"
                              tabIndex={0}
                              onClick={() => handleReceipt(boleto)}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                  handleReceipt(boleto)
                                }
                              }}
                              className="rounded p-1 text-slate-400 transition-opacity hover:bg-green-50 hover:text-green-600 dark:hover:bg-green-900/30 dark:hover:text-green-400"
                              aria-label="Ver comprovante de pagamento"
                              title="Comprovante"
                            >
                              {receiptLoading === boleto.id ? (
                                <Spinner size="sm" />
                              ) : (
                                <Receipt className="h-4 w-4" />
                              )}
                            </span>
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

        {/* Paginação */}
        <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 text-sm sm:flex-row dark:border-slate-800">
          <span className="text-slate-500 dark:text-slate-400">
            {totalElements === 0
              ? 'Nenhum resultado'
              : `${totalElements} boleto(s) • Página ${page + 1} de ${Math.max(totalPages, 1)}`}
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

      <BoletoAttachmentsModal
        open={anexosBoleto != null}
        boletoId={anexosBoleto?.id ?? null}
        boletoLabel={anexosBoleto?.label ?? ''}
        onClose={() => setAnexosBoleto(null)}
      />
    </div>
  )
}