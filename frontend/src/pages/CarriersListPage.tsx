import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  Plus,
  Power,
  Truck,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import {
  activateCarrier,
  inactivateCarrier,
  listCarriers,
} from '../api/carrier.api'
import type { CarrierResponse, CarrierStatus } from '../types/carrier'
import { CARRIER_NAME_LABELS } from '../types/carrier'
import type { PagedResponse } from '../types/api'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Todos' },
  { value: 'ATIVO', label: 'Ativos' },
  { value: 'INATIVO', label: 'Inativos' },
]

const PAGE_SIZE = 10

function formatDate(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatFreight(value: number | null | undefined): string {
  if (value == null) return '—'
  return value.toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

export function CarriersListPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [statusFilter, setStatusFilter] = useState<'ALL' | CarrierStatus>('ALL')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PagedResponse<CarrierResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // --- toggle individual ---
  const [confirmSingle, setConfirmSingle] = useState<CarrierResponse | null>(
    null,
  )
  const [toggling, setToggling] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const status =
        statusFilter === 'ALL' ? undefined : (statusFilter as CarrierStatus)
      const result = await listCarriers({
        status,
        page,
        size: PAGE_SIZE,
      })
      setData(result)
    } catch (err) {
      setError(toApiError(err).message)
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [statusFilter, page])

  useEffect(() => {
    load()
  }, [load])

  // Reseta a página ao mudar o filtro de status.
  useEffect(() => {
    setPage(0)
  }, [statusFilter])

  async function handleToggleStatus() {
    if (!confirmSingle) return
    setToggling(true)
    setError(null)
    try {
      if (confirmSingle.status === 'ATIVO') {
        await inactivateCarrier(confirmSingle.uuid)
      } else {
        await activateCarrier(confirmSingle.uuid)
      }
      setConfirmSingle(null)
      await load()
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setToggling(false)
    }
  }

  const items = data?.content ?? []

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Transportadoras
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Cadastro e gestão de transportadoras disponíveis para cotações e
            pedidos.
            {isAdmin ? (
              <span className="ml-2 inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
                Visão ADMIN
              </span>
            ) : null}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/carriers/new">
            <Button>
              <Plus className="h-4 w-4" />
              Nova transportadora
            </Button>
          </Link>
        </div>
      </div>

      {/* Filtros */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="sm:max-w-[240px]">
          <Select
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as 'ALL' | CarrierStatus)
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
                <th className="px-4 py-3 font-medium">Transportadora</th>
                <th className="px-4 py-3 font-medium">Valor do frete</th>
                <th className="px-4 py-3 font-medium">Status</th>
                {isAdmin ? (
                  <th className="px-4 py-3 font-medium">Atualizado em</th>
                ) : null}
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td
                    colSpan={isAdmin ? 5 : 4}
                    className="px-4 py-12 text-center"
                  >
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td
                    colSpan={isAdmin ? 5 : 4}
                    className="px-4 py-12 text-center"
                  >
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Truck className="h-8 w-8 opacity-60" />
                      <p className="text-sm">
                        Nenhuma transportadora encontrada.
                      </p>
                      <Link to="/carriers/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Cadastrar a primeira
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                items.map((c) => (
                  <tr
                    key={c.uuid}
                    className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40"
                    onClick={() => navigate(`/carriers/${c.uuid}`)}
                  >
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900 dark:text-slate-100">
                        {c.carrierName ? CARRIER_NAME_LABELS[c.carrierName] : '—'}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-300">
                      {formatFreight(c.freightValue)}
                    </td>
                    <td className="px-4 py-3">
                      <RegistrationStatusBadge status={c.status} />
                    </td>
                    {isAdmin ? (
                      <td className="whitespace-nowrap px-4 py-3 text-xs text-slate-500 dark:text-slate-400">
                        {formatDate(c.updatedAt)}
                        {c.updatedBy ? (
                          <div className="text-[10px] text-slate-400 dark:text-slate-500">
                            por {c.updatedBy}
                          </div>
                        ) : null}
                      </td>
                    ) : null}
                    <td
                      className="px-4 py-3"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => navigate(`/carriers/${c.uuid}`)}
                          title="Ver / editar"
                          aria-label="Ver / editar"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant={c.status === 'ATIVO' ? 'ghost' : 'secondary'}
                          onClick={() => setConfirmSingle(c)}
                          title={c.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
                          aria-label={
                            c.status === 'ATIVO' ? 'Inativar' : 'Reativar'
                          }
                        >
                          <Power className="h-4 w-4" />
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
            {data?.totalElements == null || data.totalElements === 0
              ? 'Nenhum resultado'
              : `${data.totalElements} transportadora(s) • Página ${
                  data.page + 1
                } de ${Math.max(data.totalPages, 1)}`}
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

      {/* Modal: inativar/reativar individual */}
      <ConfirmDialog
        open={!!confirmSingle}
        title={
          confirmSingle?.status === 'ATIVO'
            ? 'Inativar transportadora?'
            : 'Reativar transportadora?'
        }
        description={
          confirmSingle?.status === 'ATIVO'
            ? `A transportadora "${confirmSingle?.carrierName ? CARRIER_NAME_LABELS[confirmSingle.carrierName] : ''}" será marcada como inativa. O registro não é apagado e pode ser reativado depois.`
            : `A transportadora "${confirmSingle?.carrierName ? CARRIER_NAME_LABELS[confirmSingle.carrierName] : ''}" voltará a ficar ativa.`
        }
        confirmText={
          confirmSingle?.status === 'ATIVO' ? 'Inativar' : 'Reativar'
        }
        confirmVariant={
          confirmSingle?.status === 'ATIVO' ? 'danger' : 'primary'
        }
        isLoading={toggling}
        onConfirm={handleToggleStatus}
        onClose={() => {
          if (!toggling) setConfirmSingle(null)
        }}
      />
    </div>
  )
}