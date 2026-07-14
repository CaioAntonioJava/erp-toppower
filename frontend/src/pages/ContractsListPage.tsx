import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  FileSignature,
  Plus,
  Printer,
  Search,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ContractStatusBadge } from '../components/contract/ContractStatusBadge'
import { listContracts } from '../api/contract.api'
import { toApiError } from '../lib/errors'
import type {
  ContractStatus,
  ContractSummaryResponse,
} from '../types/contract'
import { CONTRACT_STATUS_LABELS } from '../types/contract'
import type { PagedResponse } from '../types/api'

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Todos os status' },
  { value: 'ABERTA', label: CONTRACT_STATUS_LABELS.ABERTA },
  { value: 'EM_ANDAMENTO', label: CONTRACT_STATUS_LABELS.EM_ANDAMENTO },
  { value: 'CONCLUIDA', label: CONTRACT_STATUS_LABELS.CONCLUIDA },
]

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

export function ContractsListPage() {
  const navigate = useNavigate()

  // Filtros
  const [status, setStatus] = useState<ContractStatus | 'ALL'>('ALL')
  const [code, setCode] = useState('')
  const [debouncedCode, setDebouncedCode] = useState('')
  const [page, setPage] = useState(0)
  const size = 10

  const [data, setData] =
    useState<PagedResponse<ContractSummaryResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

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
    const params: Parameters<typeof listContracts>[0] = { page, size }
    if (status !== 'ALL') params.status = status
    if (debouncedCode.length > 0) params.code = debouncedCode
    listContracts(params)
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
          <h1 className="text-2xl font-semibold tracking-tight">Contratos</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Gestão de contratos emitidos pela empresa.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/contracts/new">
            <Button>
              <Plus className="h-4 w-4" />
              Novo contrato
            </Button>
          </Link>
        </div>
      </div>

      {/* Filtros */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid gap-3 sm:grid-cols-[1fr_220px]">
          <Input
            placeholder="Buscar por código (ex.: CT-001-2026)…"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            leftAdornment={<Search className="h-4 w-4" />}
          />
          <Select
            options={STATUS_OPTIONS}
            value={status}
            onChange={(e) =>
              setStatus(e.target.value as ContractStatus | 'ALL')
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
                <th className="px-4 py-3 font-medium">Código</th>
                <th className="px-4 py-3 font-medium">Início</th>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium">Descrição</th>
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
                      <FileSignature className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhum contrato encontrado.</p>
                      <Link to="/contracts/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Criar o primeiro
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                (data?.content ?? []).map((c) => (
                  <tr
                    key={c.id}
                    className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40"
                    onClick={() => navigate(`/contracts/${c.id}/edit`)}
                  >
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-700 dark:text-slate-200">
                      {c.code}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                      {formatDate(c.startDate)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900 dark:text-slate-100">
                        {c.clientName ?? '—'}
                      </div>
                      <div className="text-xs text-slate-500 dark:text-slate-400">
                        <span
                          className={`mr-1 inline-flex items-center rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase ${
                            c.clientType === 'COMPANY'
                              ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-200'
                              : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
                          }`}
                        >
                          {c.clientType === 'COMPANY' ? 'PJ' : 'PF'}
                        </span>
                        {c.clientCode ?? ''}
                      </div>
                    </td>
                    <td className="max-w-[320px] truncate px-4 py-3 text-slate-600 dark:text-slate-300">
                      {c.descriptionPreview ?? '—'}
                    </td>
                    <td className="px-4 py-3">
                      <ContractStatusBadge status={c.status} />
                    </td>
                    <td
                      className="px-4 py-3"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => navigate(`/contracts/${c.id}`)}
                          title="Ver detalhe"
                          aria-label="Ver detalhe"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() =>
                            window.open(`/contracts/${c.id}/pdf`, '_blank')
                          }
                          title="Gerar PDF"
                          aria-label="Gerar PDF"
                        >
                          <Printer className="h-4 w-4" />
                        </Button>
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
              : `${data?.totalElements ?? 0} contrato(s) • Página ${
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
    </div>
  )
}