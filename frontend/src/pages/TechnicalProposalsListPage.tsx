import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Eye,
  FileText,
  Plus,
  Printer,
  Search,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { TechnicalProposalStatusBadge } from '../components/sales/TechnicalProposalStatusBadge'
import {
  completeTechnicalProposal,
  listTechnicalProposals,
} from '../api/technicalProposal.api'
import { toApiError } from '../lib/errors'
import type {
  TechnicalProposalStatus,
  TechnicalProposalSummaryResponse,
} from '../types/technicalProposal'
import {
  TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS,
  TECHNICAL_PROPOSAL_STATUS_LABELS,
} from '../types/technicalProposal'
import type { PagedResponse } from '../types/api'

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Todos os status' },
  { value: 'ABERTA', label: TECHNICAL_PROPOSAL_STATUS_LABELS.ABERTA },
  { value: 'EM_ANDAMENTO', label: TECHNICAL_PROPOSAL_STATUS_LABELS.EM_ANDAMENTO },
  { value: 'CONCLUIDA', label: TECHNICAL_PROPOSAL_STATUS_LABELS.CONCLUIDA },
]

const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

function formatDate(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

export function TechnicalProposalsListPage() {
  const navigate = useNavigate()

  // Filtros
  const [status, setStatus] = useState<TechnicalProposalStatus | 'ALL'>('ALL')
  const [code, setCode] = useState('')
  const [debouncedCode, setDebouncedCode] = useState('')
  const [page, setPage] = useState(0)
  const size = 10

  const [data, setData] =
    useState<PagedResponse<TechnicalProposalSummaryResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Finalização de proposta direto da listagem.
  const [completeTarget, setCompleteTarget] =
    useState<TechnicalProposalSummaryResponse | null>(null)
  const [completing, setCompleting] = useState(false)
  const [completeError, setCompleteError] = useState<string | null>(null)

  async function handleComplete() {
    if (!completeTarget) return
    setCompleting(true)
    setCompleteError(null)
    try {
      await completeTechnicalProposal(completeTarget.id)
      setCompleteTarget(null)
      // Recarrega a página atual para refletir o novo status.
      const params: Parameters<typeof listTechnicalProposals>[0] = { page, size }
      if (status !== 'ALL') params.status = status
      if (debouncedCode.length > 0) params.code = debouncedCode
      const result = await listTechnicalProposals(params)
      setData(result)
    } catch (err) {
      setCompleteError(toApiError(err).message)
    } finally {
      setCompleting(false)
    }
  }

  // Debounce do filtro por código.
  useEffect(() => {
    const t = setTimeout(() => setDebouncedCode(code.trim()), 300)
    return () => clearTimeout(t)
  }, [code])

  useEffect(() => {
    setPage(0)
  }, [status, debouncedCode])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    const params: Parameters<typeof listTechnicalProposals>[0] = { page, size }
    if (status !== 'ALL') params.status = status
    if (debouncedCode.length > 0) params.code = debouncedCode
    listTechnicalProposals(params)
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
  }, [status, debouncedCode, page])

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Propostas Técnicas
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Gestão de propostas técnicas de serviço.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/technical-proposals/new">
            <Button>
              <Plus className="h-4 w-4" />
              Nova proposta
            </Button>
          </Link>
        </div>
      </div>

      {/* Filtros */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid gap-3 sm:grid-cols-[1fr_220px]">
          <Input
            placeholder="Buscar por código (ex.: PL-001-2026)…"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            leftAdornment={<Search className="h-4 w-4" />}
          />
          <Select
            options={STATUS_OPTIONS}
            value={status}
            onChange={(e) =>
              setStatus(e.target.value as TechnicalProposalStatus | 'ALL')
            }
            aria-label="Filtrar por status"
          />
        </div>
      </div>

      {error ? <Alert variant="error">{error}</Alert> : null}
      {completeError ? <Alert variant="error">{completeError}</Alert> : null}

      {/* Tabela */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Código</th>
                <th className="px-4 py-3 font-medium">Início</th>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : (data?.content ?? []).length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <FileText className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhuma proposta técnica encontrada.</p>
                      <Link to="/technical-proposals/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Criar a primeira
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                (data?.content ?? []).map((tp) => (
                  <tr
                    key={tp.id}
                    className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40"
                    onClick={() => navigate(`/technical-proposals/${tp.id}/edit`)}
                  >
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-700 dark:text-slate-200">
                      {tp.code}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                      {formatDate(tp.startDate)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900 dark:text-slate-100">
                        {tp.clientName ?? '—'}
                      </div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">
                        {tp.clientType
                          ? TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS[tp.clientType]
                          : ''}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs font-semibold text-slate-700 dark:text-slate-200">
                      {brlFormatter.format(tp.total + (tp.generalPrice ?? 0))}
                    </td>
                    <td className="px-4 py-3">
                      <TechnicalProposalStatusBadge status={tp.status} />
                    </td>
                    <td
                      className="px-4 py-3"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => navigate(`/technical-proposals/${tp.id}`)}
                          title="Ver detalhe"
                          aria-label="Ver detalhe"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() =>
                            window.open(
                              `/technical-proposals/${tp.id}/pdf`,
                              '_blank',
                            )
                          }
                          title="Gerar PDF"
                          aria-label="Gerar PDF"
                        >
                          <Printer className="h-4 w-4" />
                        </Button>
                        {tp.status === 'EM_ANDAMENTO' ? (
                          <Button
                            size="sm"
                            variant="ghost"
                            className="!text-red-600 hover:!text-red-600 dark:!text-red-500 dark:hover:!text-red-500"
                            onClick={() => setCompleteTarget(tp)}
                            title="Finalizar serviço"
                            aria-label="Finalizar serviço"
                          >
                            <CheckCircle2 className="h-4 w-4" />
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
              : `${data?.totalElements ?? 0} proposta(s) • Página ${
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
        open={completeTarget != null}
        title="Finalizar serviço?"
        description={
          completeTarget
            ? `A proposta ${completeTarget.code} passará para CONCLUIDA e a data de entrega será preenchida com a data de hoje.`
            : ''
        }
        confirmText="Finalizar"
        confirmVariant="primary"
        isLoading={completing}
        onConfirm={handleComplete}
        onClose={() => {
          if (!completing) setCompleteTarget(null)
        }}
      />
    </div>
  )
}