import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Eye,
  FileSignature,
  Plus,
  Power,
  Printer,
  RotateCcw,
  Search,
  X,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { ContractStatusBadge } from '../components/contract/ContractStatusBadge'
import {
  activateContract,
  completeContract,
  inactivateContract,
  listContracts,
  reopenContract,
  searchContracts,
} from '../api/contract.api'
import type { ContractResponse, ContractStatus } from '../types/contract'
import { useAuth } from '../context/AuthContext'
import { useEntityList } from '../hooks/useEntityList'
import { toApiError } from '../lib/errors'

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Todos' },
  { value: 'ATIVO', label: 'Ativos' },
  { value: 'CONCLUIDO', label: 'Concluídos' },
  { value: 'INATIVO', label: 'Inativos' },
]

function formatValidityDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  })
}

export function ContractsListPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const list = useEntityList<ContractResponse, ContractStatus>({
    api: {
      fetchAll: listContracts,
      search: searchContracts,
      inactivate: inactivateContract,
      activate: activateContract,
    },
  })

  // Finalização / reabertura de contrato direto da listagem.
  const [completeTarget, setCompleteTarget] =
    useState<ContractResponse | null>(null)
  const [completing, setCompleting] = useState(false)
  const [completeError, setCompleteError] = useState<string | null>(null)
  const [reopenTarget, setReopenTarget] =
    useState<ContractResponse | null>(null)
  const [reopening, setReopening] = useState(false)
  const [reopenError, setReopenError] = useState<string | null>(null)

  async function handleComplete() {
    if (!completeTarget) return
    setCompleting(true)
    setCompleteError(null)
    try {
      await completeContract(completeTarget.id)
      setCompleteTarget(null)
      await list.reload()
    } catch (err) {
      setCompleteError(toApiError(err).message)
    } finally {
      setCompleting(false)
    }
  }

  async function handleReopen() {
    if (!reopenTarget) return
    setReopening(true)
    setReopenError(null)
    try {
      await reopenContract(reopenTarget.id)
      setReopenTarget(null)
      await list.reload()
    } catch (err) {
      setReopenError(toApiError(err).message)
    } finally {
      setReopening(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Contratos
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Cadastro e gestão de contratos de prestação de serviços.
            {isAdmin ? (
              <span className="ml-2 inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
                Visão ADMIN
              </span>
            ) : null}
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

      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid gap-3 sm:grid-cols-[1fr_220px]">
          <Input
            placeholder="Buscar por código, título ou descrição…"
            value={list.query}
            onChange={(e) => list.setQuery(e.target.value)}
            leftAdornment={<Search className="h-4 w-4" />}
            hint={
              list.query.trim().length > 0 &&
              list.query.trim().length < list.minQueryLength
                ? `Digite ao menos ${list.minQueryLength} caracteres para buscar.`
                : undefined
            }
          />
          <Select
            options={STATUS_OPTIONS}
            value={list.statusFilter}
            onChange={(e) =>
              list.setStatusFilter(e.target.value as 'ALL' | ContractStatus)
            }
            aria-label="Filtrar por status"
          />
        </div>
      </div>

      {list.bulkFeedback ? (
        <Alert variant={list.bulkFeedback.fail > 0 ? 'error' : 'success'}>
          {list.bulkFeedback.message}
        </Alert>
      ) : null}
      {list.error ? <Alert variant="error">{list.error}</Alert> : null}
      {completeError ? (
        <Alert variant="error">{completeError}</Alert>
      ) : null}
      {reopenError ? <Alert variant="error">{reopenError}</Alert> : null}

      {isAdmin && list.hasSelection ? (
        <div className="flex flex-col items-stretch gap-2 rounded-xl border border-primary/30 bg-primary-50 px-4 py-3 text-sm dark:border-primary-900 dark:bg-primary-900/20 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2 text-primary-800 dark:text-primary-200">
            <Check className="h-4 w-4" />
            <span>
              <strong>{list.selectedIds.size}</strong> contrato(s) selecionado(s)
              {list.hasOffpageSelection ? ' (em outras páginas)' : ''}
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button
              size="sm" variant="ghost"
              onClick={list.clearSelection}
              disabled={list.bulkRunning}
            >
              <X className="h-4 w-4" />
              Limpar
            </Button>
            <Button
              size="sm" variant="secondary"
              onClick={() => list.setConfirmBulk('ATIVO')}
              disabled={list.bulkRunning || (list.selectedActiveCount === 0 && !list.hasOffpageSelection)}
            >
              <Power className="h-4 w-4" />
              Reativar selecionados
            </Button>
            <Button
              size="sm" variant="danger"
              onClick={() => list.setConfirmBulk('INATIVO')}
              disabled={list.bulkRunning || (list.selectedInactiveCount === 0 && !list.hasOffpageSelection)}
            >
              <Power className="h-4 w-4" />
              Inativar selecionados
            </Button>
          </div>
        </div>
      ) : null}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                {isAdmin ? (
                  <th scope="col" className="w-10 px-4 py-3">
                    <input
                      type="checkbox"
                      aria-label="Selecionar todos da página"
                      className="h-4 w-4 cursor-pointer rounded border-slate-300 text-primary focus:ring-primary dark:border-slate-600 dark:bg-slate-800"
                      checked={list.allVisibleSelected}
                      onChange={list.toggleSelectAllVisible}
                      disabled={list.loading || list.items.length === 0}
                    />
                  </th>
                ) : null}
                <th className="px-4 py-3 font-medium">Código</th>
                <th className="px-4 py-3 font-medium">Cliente</th>
                <th className="px-4 py-3 font-medium">Título</th>
                <th className="px-4 py-3 font-medium">Vigência</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {list.loading ? (
                <tr>
                  <td colSpan={isAdmin ? 7 : 6} className="px-4 py-12 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : list.items.length === 0 ? (
                <tr>
                  <td colSpan={isAdmin ? 7 : 6} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <FileSignature className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhum contrato encontrado.</p>
                      <Link to="/contracts/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Cadastrar o primeiro
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                list.items.map((c) => {
                  const isSelected = list.selectedIds.has(c.id)
                  return (
                    <tr
                      key={c.id}
                      className={[
                        'cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40',
                        isSelected ? 'bg-primary-50/50 dark:bg-primary-900/10' : '',
                      ].join(' ')}
                      onClick={() => navigate(`/contracts/${c.id}`)}
                    >
                      {isAdmin ? (
                        <td
                          className="px-4 py-3"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <input
                            type="checkbox"
                            aria-label={`Selecionar ${c.code}`}
                            className="h-4 w-4 cursor-pointer rounded border-slate-300 text-primary focus:ring-primary dark:border-slate-600 dark:bg-slate-800"
                            checked={isSelected}
                            onChange={() => list.toggleSelect(c.id)}
                          />
                        </td>
                      ) : null}
                      <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-700 dark:text-slate-200">
                        {c.code}
                      </td>
                      <td className="px-4 py-3">
                        {c.clientName ? (
                          <div>
                            <div className="font-medium text-slate-900 dark:text-slate-100">
                              {c.clientName}
                            </div>
                            {c.clientCode ? (
                              <div className="text-xs text-slate-500 dark:text-slate-400">
                                {c.clientCode}
                                {c.clientType ? (
                                  <span
                                    className={[
                                      'ml-1.5 inline-flex items-center rounded px-1 py-0.5 text-[10px] font-bold uppercase leading-none text-white',
                                      c.clientType === 'CUSTOMER'
                                        ? 'bg-blue-500'
                                        : 'bg-emerald-600',
                                    ].join(' ')}
                                  >
                                    {c.clientType === 'CUSTOMER' ? 'PF' : 'PJ'}
                                  </span>
                                ) : null}
                              </div>
                            ) : null}
                          </div>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <div className="line-clamp-1 text-slate-900 dark:text-slate-100">
                          {c.title}
                        </div>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                        {formatValidityDate(c.validityDate)}
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
                            size="sm" variant="ghost"
                            onClick={() => navigate(`/contracts/${c.id}`)}
                            title="Ver / editar" aria-label="Ver / editar"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button
                            size="sm" variant="ghost"
                            onClick={() => window.open(`/contracts/${c.id}/pdf`, '_blank', 'noopener,noreferrer')}
                            title="Imprimir / PDF" aria-label="Imprimir / PDF"
                          >
                            <Printer className="h-4 w-4" />
                          </Button>
                          {c.status === 'ATIVO' ? (
                            <Button
                              size="sm"
                              variant="ghost"
                              className="!text-emerald-600 hover:!text-emerald-600 dark:!text-emerald-500 dark:hover:!text-emerald-500"
                              onClick={() => setCompleteTarget(c)}
                              title="Finalizar contrato"
                              aria-label="Finalizar contrato"
                            >
                              <CheckCircle2 className="h-4 w-4" />
                            </Button>
                          ) : null}
                          {c.status === 'CONCLUIDO' ? (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => setReopenTarget(c)}
                              title="Reabrir contrato"
                              aria-label="Reabrir contrato"
                            >
                              <RotateCcw className="h-4 w-4" />
                            </Button>
                          ) : null}
                          <Button
                            size="sm"
                            variant={c.status === 'ATIVO' ? 'ghost' : 'secondary'}
                            onClick={() => list.setConfirmSingle(c)}
                            title={c.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
                            aria-label={c.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
                            disabled={c.status === 'CONCLUIDO'}
                          >
                            <Power className="h-4 w-4" />
                          </Button>
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
            {list.totalElements === 0
              ? 'Nenhum resultado'
              : `${list.totalElements} contrato(s) • Página ${
                  list.data ? list.data.page + 1 : 0
                } de ${Math.max(list.totalPages, 1)}`}
            {isAdmin && list.selectedIds.size > 0 ? (
              <span className="ml-2 text-primary-700 dark:text-primary-300">
                • {list.selectedIds.size} selecionado(s)
              </span>
            ) : null}
          </span>
          <div className="flex items-center gap-2">
            <Button
              size="sm" variant="secondary"
              onClick={() => list.setPage((p) => Math.max(0, p - 1))}
              disabled={list.loading || list.data?.first}
            >
              <ChevronLeft className="h-4 w-4" />
              Anterior
            </Button>
            <Button
              size="sm" variant="secondary"
              onClick={() => list.setPage((p) => p + 1)}
              disabled={list.loading || list.data?.last}
            >
              Próxima
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={!!list.confirmSingle}
        title={
          list.confirmSingle?.status === 'ATIVO'
            ? 'Inativar contrato?'
            : 'Reativar contrato?'
        }
        description={
          list.confirmSingle?.status === 'ATIVO'
            ? `O contrato "${list.confirmSingle?.code}" será marcado como inativo. O registro não é apagado e pode ser reativado depois.`
            : `O contrato "${list.confirmSingle?.code}" voltará a ficar ativo.`
        }
        confirmText={list.confirmSingle?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={list.confirmSingle?.status === 'ATIVO' ? 'danger' : 'primary'}
        isLoading={list.toggling}
        onConfirm={list.handleSingleToggle}
        onClose={() => {
          if (!list.toggling) list.setConfirmSingle(null)
        }}
      />

      <ConfirmDialog
        open={!!list.confirmBulk}
        title={
          list.confirmBulk === 'ATIVO'
            ? 'Reativar contratos selecionados?'
            : 'Inativar contratos selecionados?'
        }
        description={
          list.confirmBulk === 'ATIVO'
            ? `${list.selectedIds.size} contrato(s) serão marcados como ativos.`
            : `${list.selectedIds.size} contrato(s) serão marcados como inativos. Os registros não são apagados.`
        }
        confirmText={list.confirmBulk === 'ATIVO' ? 'Reativar todos' : 'Inativar todos'}
        confirmVariant={list.confirmBulk === 'INATIVO' ? 'danger' : 'primary'}
        isLoading={list.bulkRunning}
        onConfirm={list.handleBulkConfirm}
        onClose={() => {
          if (!list.bulkRunning) list.setConfirmBulk(null)
        }}
      />

      <ConfirmDialog
        open={completeTarget != null}
        title="Finalizar contrato?"
        description={
          completeTarget
            ? `O contrato "${completeTarget.code}" será marcado como CONCLUIDO e a data de entrega será preenchida com a data de hoje.`
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

      <ConfirmDialog
        open={reopenTarget != null}
        title="Reabrir contrato?"
        description={
          reopenTarget
            ? `O contrato "${reopenTarget.code}" voltará a ficar ATIVO e a data de entrega será limpa.`
            : ''
        }
        confirmText="Reabrir"
        confirmVariant="primary"
        isLoading={reopening}
        onConfirm={handleReopen}
        onClose={() => {
          if (!reopening) setReopenTarget(null)
        }}
      />
    </div>
  )
}