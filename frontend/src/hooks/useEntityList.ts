import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'
import { toApiError } from '../lib/errors'

/**
 * Tipo de status da entidade. Por padrão é {@link RegistrationStatus}
 * (ATIVO/INATIVO), mas entidades com ciclo de execução mais rico (ex.:
 * contrato, que adiciona CONCLUIDO) podem passar um supertipo. O hook
 * continua tratando apenas ATIVO/INATIVO no toggle e na seleção em massa;
 * status extras são apenas exibidos/filtrados pela página.
 */
export type EntityStatus = RegistrationStatus

/**
 * Entidade mínima que o hook precisa para seleção, auditoria e toggle.
 * O status pode ser um supertipo de {@link RegistrationStatus} (ex.:
 * {@link ContractStatus}, que adiciona CONCLUIDO) — o hook só interage
 * com ATIVO/INATIVO, mas a entidade pode ter outros valores.
 */
export interface EntityListItem<S extends string = EntityStatus> {
  id: number
  status: S
}

/** Operações da API que diferem entre entidades. */
export interface EntityListApi<T, S extends string = EntityStatus> {
  fetchAll: (params: {
    status?: S
    page: number
    size: number
  }) => Promise<PagedResponse<T>>
  search: (params: {
    query: string
    status?: S
    page: number
    size: number
  }) => Promise<PagedResponse<T>>
  inactivate: (id: number) => Promise<void>
  activate: (id: number) => Promise<T>
}

interface UseEntityListOptions<T, S extends string = EntityStatus> {
  api: EntityListApi<T, S>
  pageSize?: number
  /** Tamanho mínimo do termo de busca. Default: 2. */
  minQueryLength?: number
}

/** Estado e handlers compartilhados entre as listas (Empresas, Clientes PF, ...). */
export function useEntityList<
  T extends EntityListItem<S>,
  S extends string = EntityStatus,
>({
  api,
  pageSize = 10,
  minQueryLength = 2,
}: UseEntityListOptions<T, S>) {
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | S>('ALL')
  const [data, setData] = useState<PagedResponse<T> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // --- seleção em massa (ADMIN) ---
  const [selectedIds, setSelectedIds] = useState<Set<number>>(() => new Set())
  const [bulkRunning, setBulkRunning] = useState(false)
  const [bulkFeedback, setBulkFeedback] = useState<{
    ok: number
    fail: number
    message?: string
  } | null>(null)
  const [confirmBulk, setConfirmBulk] = useState<'ATIVO' | 'INATIVO' | null>(
    null,
  )

  // --- toggle individual ---
  const [confirmSingle, setConfirmSingle] = useState<T | null>(null)
  const [toggling, setToggling] = useState(false)

  /**
   * Referência estável para o objeto `api` recebido via prop.
   *
   * O caller geralmente passa um objeto inline (ex: `{ fetchAll: listCompanies, ... }`),
   * o que cria uma NOVA referência a cada render da página. Se `load` (useCallback)
   * dependesse de `api`, ele seria recriado a cada render, disparando o
   * useEffect que chama `load()`, que atualiza `data`, que re-renderiza a página,
   * que cria um novo `api` — **loop infinito de GETs**.
   *
   * Solução: ler a api atual via ref dentro de `load`, sem colocá-la nas deps.
   * O ref sempre aponta para a versão mais recente do `api`.
   */
  const apiRef = useRef(api)
  useEffect(() => {
    apiRef.current = api
  }, [api])

  // Debounce da busca.
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(query.trim()), 300)
    return () => clearTimeout(t)
  }, [query])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const currentApi = apiRef.current
      const status =
        statusFilter === 'ALL' ? undefined : (statusFilter as S)
      const useSearch = debouncedQuery.length >= minQueryLength
      const result = useSearch
        ? await currentApi.search({
            query: debouncedQuery,
            status,
            page,
            size: pageSize,
          })
        : await currentApi.fetchAll({ status, page, size: pageSize })
      setData(result)
    } catch (err) {
      setError(toApiError(err).message)
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [debouncedQuery, statusFilter, page, pageSize, minQueryLength])

  // Limpa seleção ao mudar filtro/busca.
  useEffect(() => {
    setSelectedIds(new Set())
    setBulkFeedback(null)
  }, [debouncedQuery, statusFilter])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery, statusFilter])

  useEffect(() => {
    load()
  }, [load])

  // Mantém a seleção consistente: ids que saíram dos resultados (ex: página
  // mudou) são removidos em silêncio.
  useEffect(() => {
    if (selectedIds.size === 0 || !data) return
    const visible = new Set(data.content.map((c) => c.id))
    setSelectedIds((prev) => {
      const next = new Set<number>()
      prev.forEach((id) => {
        if (visible.has(id)) next.add(id)
      })
      return next.size === prev.size ? prev : next
    })
  }, [data, selectedIds])

  /* ----------------------------- handlers ----------------------------- */

  async function handleSingleToggle() {
    if (!confirmSingle) return
    setToggling(true)
    setError(null)
    try {
      if (confirmSingle.status === 'ATIVO') {
        await api.inactivate(confirmSingle.id)
      } else {
        await api.activate(confirmSingle.id)
      }
      setConfirmSingle(null)
      await load()
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setToggling(false)
    }
  }

  async function handleBulkConfirm() {
    if (!confirmBulk) return
    const ids = Array.from(selectedIds)
    setBulkRunning(true)
    setBulkFeedback(null)
    setError(null)

    const results = await Promise.allSettled(
      ids.map((id) =>
        confirmBulk === 'ATIVO'
          ? api.activate(id)
          : api.inactivate(id),
      ),
    )
    const ok = results.filter((r) => r.status === 'fulfilled').length
    const fail = ids.length - ok
    setBulkFeedback({
      ok,
      fail,
      message:
        fail > 0
          ? `${ok} processado(s) com sucesso, ${fail} falha(s).`
          : `${ok} atualizado(s) com sucesso.`,
    })
    setSelectedIds(new Set())
    setBulkRunning(false)
    setConfirmBulk(null)
    await load()
  }

  function toggleSelect(id: number) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function toggleSelectAllVisible() {
    if (!data) return
    const visibleIds = data.content.map((c) => c.id)
    const allSelected = visibleIds.every((id) => selectedIds.has(id))
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (allSelected) visibleIds.forEach((id) => next.delete(id))
      else visibleIds.forEach((id) => next.add(id))
      return next
    })
  }

  function clearSelection() {
    setSelectedIds(new Set())
    setBulkFeedback(null)
  }

  const items = data?.content ?? []
  const visibleIds = useMemo(
    () => new Set(items.map((c) => c.id)),
    [items],
  )
  const allVisibleSelected =
    items.length > 0 && items.every((c) => selectedIds.has(c.id))
  const hasSelection = selectedIds.size > 0
  const selectedActiveCount = items
    .filter((c) => selectedIds.has(c.id) && c.status === 'ATIVO')
    .length
  const selectedInactiveCount = items
    .filter((c) => selectedIds.has(c.id) && c.status === 'INATIVO')
    .length
  const hasOffpageSelection =
    selectedIds.size > visibleIds.size ||
    Array.from(selectedIds).some((id) => !visibleIds.has(id))

  return {
    // data
    items,
    data,
    loading,
    error,
    // pagination
    page,
    setPage,
    totalPages: data?.totalPages ?? 0,
    totalElements: data?.totalElements ?? 0,
    // filters
    query,
    setQuery,
    statusFilter,
    setStatusFilter,
    minQueryLength,
    // selection
    selectedIds,
    setSelectedIds,
    toggleSelect,
    toggleSelectAllVisible,
    clearSelection,
    allVisibleSelected,
    hasSelection,
    hasOffpageSelection,
    selectedActiveCount,
    selectedInactiveCount,
    // bulk
    bulkRunning,
    bulkFeedback,
    setBulkFeedback,
    confirmBulk,
    setConfirmBulk,
    handleBulkConfirm,
    // single
    confirmSingle,
    setConfirmSingle,
    toggling,
    handleSingleToggle,
    // reload
    reload: load,
  }
}
