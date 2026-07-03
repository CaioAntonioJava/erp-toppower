/**
 * Helpers compartilhados pelos mocks de API.
 *
 * Apenas para desenvolvimento/teste manual. NÃO importar fora de `src/mocks/api/`.
 *
 * - `delay` simula latência de rede para que os estados de loading
 *   apareçam no UI (igual ao backend real).
 * - `mockError` produz um erro no mesmo formato que o axios entrega,
 *   para que `toApiError(err)` em `lib/errors.ts` extraia `message`
 *   corretamente sem precisar de tratamento especial no hook/página.
 * - `pagedSlice` aplica paginação client-side sobre arrays em memória.
 */

import type { PagedResponse } from '../../types/api'
import type { RegistrationStatus } from '../../types/registration'

/** Latência simulada em ms. Curta o suficiente para não irritar no dev. */
const MOCK_DELAY_MS = 180

/** Aguarda `MOCK_DELAY_MS` antes de resolver a promise. */
export function delay(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, MOCK_DELAY_MS))
}

/**
 * Produz um erro no formato do axios (com `.response.data`),
 * para que `toApiError(err)` funcione sem mudanças nas páginas.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function mockError(status: number, message: string, fieldErrors?: Record<string, string>): any {
  const err = new Error(message)
  // Anexa a estrutura que `lib/errors.ts::toApiError` espera.
  ;(err as any).response = {
    status,
    data: {
      status,
      message,
      timestamp: new Date().toISOString(),
      fieldErrors,
    },
  }
  return err
}

/**
 * Aplica filtro, paginação e ordenação a um array em memória.
 *
 * `sortBy` é o nome do campo do item usado para ordenação ascendente.
 * `queryFn` decide se um item casa com o termo de busca (pode ser `null`
 * para desabilitar busca).
 * `statusFn` extrai o status para comparar com o filtro opcional.
 */
export function pagedSlice<T extends { uuid: string; status: RegistrationStatus }>(
  items: ReadonlyArray<T>,
  params: {
    query?: string
    status?: RegistrationStatus
    page?: number
    size?: number
    sortBy?: keyof T
    queryFn?: (item: T, query: string) => boolean
  },
): PagedResponse<T> {
  const page = params.page ?? 0
  const size = params.size ?? 20
  const sortBy = params.sortBy

  let filtered: T[] = [...items]

  if (params.status) {
    filtered = filtered.filter((it) => it.status === params.status)
  }

  if (params.query && params.queryFn) {
    const q = params.query.toLowerCase()
    filtered = filtered.filter((it) => params.queryFn!(it, q))
  }

  if (sortBy) {
    filtered.sort((a, b) => {
      const av = String(a[sortBy] ?? '')
      const bv = String(b[sortBy] ?? '')
      return av.localeCompare(bv, 'pt-BR')
    })
  }

  const totalElements = filtered.length
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  const start = page * size
  const content = filtered.slice(start, start + size)

  return {
    content,
    page,
    size,
    totalElements,
    totalPages,
    first: page === 0,
    last: page >= totalPages - 1,
  }
}

/**
 * Calcula o próximo código sequencial no padrão `PREFIX000001`.
 * Usado por `getNextCompanyCode`, `getNextCustomerCode` etc.
 */
export function nextSequentialCode(prefix: string, codes: ReadonlyArray<string>): string {
  const re = new RegExp(`^${prefix}(\\d+)$`)
  let max = 0
  for (const c of codes) {
    const m = re.exec(c)
    if (m) {
      const n = parseInt(m[1], 10)
      if (n > max) max = n
    }
  }
  return `${prefix}${String(max + 1).padStart(6, '0')}`
}