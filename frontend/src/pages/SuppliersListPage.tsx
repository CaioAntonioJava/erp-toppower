import { Link, useNavigate } from 'react-router-dom'
import {
  Check,
  ChevronLeft,
  ChevronRight,
  Download,
  Eye,
  Plus,
  Power,
  Search,
  Truck,
  X,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import {
  activateSupplier,
  inactivateSupplier,
  listSuppliers,
  searchSuppliers,
} from '../api/supplier.api'
import type { SupplierResponse } from '../types/supplier'
import type { RegistrationStatus } from '../types/registration'
import { useAuth } from '../context/AuthContext'
import { defaultCsvFilename, downloadCsv, toCsv } from '../lib/csv'
import { useEntityList } from '../hooks/useEntityList'

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Todos' },
  { value: 'ATIVO', label: 'Ativos' },
  { value: 'INATIVO', label: 'Inativos' },
]

const CSV_HEADERS = [
  'Razão Social',
  'Nome Fantasia',
  'CNPJ',
  'IE',
  'IM',
  'E-mail',
  'Telefone',
  'Contato',
  'CEP',
  'Logradouro',
  'Número',
  'Complemento',
  'Bairro',
  'Cidade',
  'UF',
  'Status',
  'Criado em',
  'Criado por',
  'Atualizado em',
  'Atualizado por',
]

function formatDate(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function supplierToCsvRow(s: SupplierResponse): unknown[] {
  return [
    s.legalName, s.tradeName ?? '',
    s.taxId,
    s.stateRegistration ?? '', s.municipalRegistration ?? '',
    s.email, s.phone ?? '', s.contactName ?? '',
    s.address.zipCode, s.address.street, s.address.number,
    s.address.complement ?? '', s.address.neighborhood ?? '',
    s.address.city, s.address.state,
    s.status,
    formatDate(s.createdAt), s.createdBy ?? '',
    formatDate(s.updatedAt), s.updatedBy ?? '',
  ]
}

export function SuppliersListPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const list = useEntityList<SupplierResponse>({
    api: {
      fetchAll: listSuppliers,
      search: searchSuppliers,
      inactivate: inactivateSupplier,
      activate: activateSupplier,
    },
  })

  function handleExportCsv() {
    if (!list.data) return
    const csv = toCsv(CSV_HEADERS, list.data.content.map(supplierToCsvRow))
    downloadCsv(defaultCsvFilename('erp-fornecedores'), csv)
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Fornecedores</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Cadastro e gestão de fornecedores (pessoas jurídicas).
            {isAdmin ? (
              <span className="ml-2 inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
                Visão ADMIN
              </span>
            ) : null}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {isAdmin ? (
            <Button
              variant="secondary"
              onClick={handleExportCsv}
              disabled={list.loading || list.items.length === 0}
            >
              <Download className="h-4 w-4" />
              Exportar CSV
            </Button>
          ) : null}
          <Link to="/suppliers/new">
            <Button>
              <Plus className="h-4 w-4" />
              Novo fornecedor
            </Button>
          </Link>
        </div>
      </div>

      {/* Filtros */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid gap-3 sm:grid-cols-[1fr_220px]">
          <Input
            placeholder="Buscar por razão social, fantasia, CNPJ ou contato…"
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
              list.setStatusFilter(e.target.value as 'ALL' | RegistrationStatus)
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

      {/* Barra de ação em massa (admin) */}
      {isAdmin && list.hasSelection ? (
        <div className="flex flex-col items-stretch gap-2 rounded-xl border border-primary/30 bg-primary-50 px-4 py-3 text-sm dark:border-primary-900 dark:bg-primary-900/20 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2 text-primary-800 dark:text-primary-200">
            <Check className="h-4 w-4" />
            <span>
              <strong>{list.selectedIds.size}</strong> fornecedor(es) selecionado(s)
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

      {/* Tabela */}
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
                <th className="px-4 py-3 font-medium">Razão social</th>
                <th className="px-4 py-3 font-medium">CNPJ</th>
                <th className="px-4 py-3 font-medium">Contato</th>
                <th className="px-4 py-3 font-medium">Cidade/UF</th>
                <th className="px-4 py-3 font-medium">Status</th>
                {isAdmin ? (
                  <th className="px-4 py-3 font-medium">Atualizado em</th>
                ) : null}
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
                      <Truck className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhum fornecedor encontrado.</p>
                      <Link to="/suppliers/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Cadastrar o primeiro
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                list.items.map((s) => {
                  const isSelected = list.selectedIds.has(s.uuid)
                  return (
                    <tr
                      key={s.uuid}
                      className={[
                        'hover:bg-slate-50 dark:hover:bg-slate-800/40',
                        isSelected ? 'bg-primary-50/50 dark:bg-primary-900/10' : '',
                      ].join(' ')}
                    >
                      {isAdmin ? (
                        <td className="px-4 py-3">
                          <input
                            type="checkbox"
                            aria-label={`Selecionar ${s.legalName}`}
                            className="h-4 w-4 cursor-pointer rounded border-slate-300 text-primary focus:ring-primary dark:border-slate-600 dark:bg-slate-800"
                            checked={isSelected}
                            onChange={() => list.toggleSelect(s.uuid)}
                          />
                        </td>
                      ) : null}
                      <td className="px-4 py-3">
                        <div className="font-medium text-slate-900 dark:text-slate-100">
                          {s.legalName}
                        </div>
                        {s.tradeName ? (
                          <div className="text-xs text-slate-500 dark:text-slate-400">
                            {s.tradeName}
                          </div>
                        ) : null}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-300">
                        {s.taxId}
                      </td>
                      <td className="px-4 py-3">
                        <div className="text-sm text-slate-700 dark:text-slate-200">
                          {s.contactName ?? '—'}
                        </div>
                        {s.email ? (
                          <div className="text-xs text-slate-500 dark:text-slate-400">
                            {s.email}
                          </div>
                        ) : null}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
                        {s.address.city}/{s.address.state}
                      </td>
                      <td className="px-4 py-3">
                        <RegistrationStatusBadge status={s.status} />
                      </td>
                      {isAdmin ? (
                        <td className="whitespace-nowrap px-4 py-3 text-xs text-slate-500 dark:text-slate-400">
                          {formatDate(s.updatedAt)}
                          {s.updatedBy ? (
                            <div className="text-[10px] text-slate-400 dark:text-slate-500">
                              por {s.updatedBy}
                            </div>
                          ) : null}
                        </td>
                      ) : null}
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            size="sm" variant="ghost"
                            onClick={() => navigate(`/suppliers/${s.uuid}`)}
                            title="Ver / editar" aria-label="Ver / editar"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button
                            size="sm"
                            variant={s.status === 'ATIVO' ? 'ghost' : 'secondary'}
                            onClick={() => list.setConfirmSingle(s)}
                            title={s.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
                            aria-label={s.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
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

        {/* Footer com paginação */}
        <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 text-sm sm:flex-row dark:border-slate-800">
          <span className="text-slate-500 dark:text-slate-400">
            {list.totalElements === 0
              ? 'Nenhum resultado'
              : `${list.totalElements} fornecedor(es) • Página ${
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

      {/* Modal: inativar/reativar individual */}
      <ConfirmDialog
        open={!!list.confirmSingle}
        title={
          list.confirmSingle?.status === 'ATIVO'
            ? 'Inativar fornecedor?'
            : 'Reativar fornecedor?'
        }
        description={
          list.confirmSingle?.status === 'ATIVO'
            ? `O fornecedor "${list.confirmSingle?.legalName}" será marcado como inativo. O registro não é apagado e pode ser reativado depois.`
            : `O fornecedor "${list.confirmSingle?.legalName}" voltará a ficar ativo.`
        }
        confirmText={list.confirmSingle?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={list.confirmSingle?.status === 'ATIVO' ? 'danger' : 'primary'}
        isLoading={list.toggling}
        onConfirm={list.handleSingleToggle}
        onClose={() => {
          if (!list.toggling) list.setConfirmSingle(null)
        }}
      />

      {/* Modal: confirmar ação em massa */}
      <ConfirmDialog
        open={!!list.confirmBulk}
        title={
          list.confirmBulk === 'ATIVO'
            ? 'Reativar fornecedores selecionados?'
            : 'Inativar fornecedores selecionados?'
        }
        description={
          list.confirmBulk === 'ATIVO'
            ? `${list.selectedIds.size} fornecedor(es) serão marcados como ativos.`
            : `${list.selectedIds.size} fornecedor(es) serão marcados como inativos. Os registros não são apagados.`
        }
        confirmText={list.confirmBulk === 'ATIVO' ? 'Reativar todos' : 'Inativar todos'}
        confirmVariant={list.confirmBulk === 'INATIVO' ? 'danger' : 'primary'}
        isLoading={list.bulkRunning}
        onConfirm={list.handleBulkConfirm}
        onClose={() => {
          if (!list.bulkRunning) list.setConfirmBulk(null)
        }}
      />
    </div>
  )
}