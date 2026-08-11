import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  Search,
  Trash2,
  Wrench,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { Badge } from '../components/ui/Badge'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import {
  deleteServiceTemplate,
  listServiceTemplates,
  searchServiceTemplates,
} from '../api/servicetemplate.api'
import type { ServiceTemplateResponse } from '../types/servicetemplate'
import type { PagedResponse } from '../types/api'
import { toApiError } from '../lib/errors'

function formatDate(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

/** Extrai o texto puro de um HTML, removendo tags. */
function stripHtml(html: string | null | undefined): string {
  if (!html) return ''
  const doc = new DOMParser().parseFromString(html, 'text/html')
  return doc.body.textContent?.trim() ?? ''
}

export function ServiceTemplatesListPage() {
  const navigate = useNavigate()

  const [data, setData] = useState<PagedResponse<ServiceTemplateResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [toDelete, setToDelete] = useState<ServiceTemplateResponse | null>(null)
  const [deleting, setDeleting] = useState(false)
  const pageSize = 20

  // Debounce da busca
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(query.trim()), 300)
    return () => clearTimeout(t)
  }, [query])

  // Reseta página ao mudar busca
  useEffect(() => {
    setPage(0)
  }, [debouncedQuery])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = debouncedQuery.length >= 2
        ? await searchServiceTemplates({ query: debouncedQuery, page, size: pageSize })
        : await listServiceTemplates({ page, size: pageSize })
      setData(result)
    } catch (err) {
      setError(toApiError(err).message)
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [debouncedQuery, page])

  useEffect(() => {
    load()
  }, [load])

  const items = data?.content ?? []

  async function handleDelete() {
    if (!toDelete) return
    setDeleting(true)
    try {
      await deleteServiceTemplate(toDelete.id)
      setToDelete(null)
      await load()
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Serviços</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Catálogo de serviços prestados, reutilizável em propostas e pedidos.
            <span className="ml-2 inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
              Visão ADMIN
            </span>
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/service-templates/new">
            <Button>
              <Plus className="h-4 w-4" />
              Novo serviço
            </Button>
          </Link>
        </div>
      </div>

      {/* Filtros */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="grid gap-3 sm:grid-cols-[1fr]">
          <Input
            placeholder="Buscar por nome…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            leftAdornment={<Search className="h-4 w-4" />}
            hint={
              query.trim().length > 0 && query.trim().length < 2
                ? 'Digite ao menos 2 caracteres para buscar.'
                : undefined
            }
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
                <th className="px-4 py-3 font-medium">Nome</th>
                <th className="px-4 py-3 font-medium">Categoria</th>
                <th className="px-4 py-3 font-medium">Descrição</th>
                <th className="px-4 py-3 font-medium">Atualizado em</th>
                <th className="px-4 py-3 font-medium text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-4 py-12 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Wrench className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhum serviço encontrado.</p>
                      <Link to="/service-templates/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Cadastrar o primeiro
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                items.map((s) => (
                  <tr
                    key={s.id}
                    className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40"
                    onClick={() => navigate(`/service-templates/${s.id}`)}
                  >
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900 dark:text-slate-100">
                        {s.name}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {s.categoryName ? (
                        <Badge tone="info">{s.categoryName}</Badge>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                    <td className="max-w-xs truncate px-4 py-3 text-slate-600 dark:text-slate-400">
                      {s.description ? stripHtml(s.description) : <span className="text-slate-400">—</span>}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-xs text-slate-500 dark:text-slate-400">
                      {formatDate(s.updatedAt)}
                      {s.updatedBy ? (
                        <div className="text-[10px] text-slate-400 dark:text-slate-500">
                          por {s.updatedBy}
                        </div>
                      ) : null}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          setToDelete(s)
                        }}
                        aria-label="Excluir serviço"
                        title="Excluir serviço"
                        className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-red-50 hover:text-red-600 dark:text-slate-400 dark:hover:bg-red-900/20 dark:hover:text-red-400"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
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
            {!data || data.totalElements === 0
              ? 'Nenhum resultado'
              : `${data.totalElements} serviço(s) • Página ${data.page + 1} de ${Math.max(data.totalPages, 1)}`}
          </span>
          <div className="flex items-center gap-2">
            <Button
              size="sm" variant="secondary"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={loading || data?.first}
            >
              <ChevronLeft className="h-4 w-4" />
              Anterior
            </Button>
            <Button
              size="sm" variant="secondary"
              onClick={() => setPage((p) => p + 1)}
              disabled={loading || data?.last}
            >
              Próxima
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>

      {/* Confirmação de exclusão */}
      <ConfirmDialog
        open={toDelete !== null}
        title="Excluir serviço"
        description={`Tem certeza que deseja excluir o serviço "${toDelete?.name ?? ''}"? Esta ação não pode ser desfeita.`}
        confirmText="Excluir"
        confirmVariant="danger"
        isLoading={deleting}
        onConfirm={handleDelete}
        onClose={() => {
          if (!deleting) setToDelete(null)
        }}
      />
    </div>
  )
}
