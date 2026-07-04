/**
 * Barrel de mocks do frontend.
 *
 * Apenas para desenvolvimento e teste manual. NÃO importar deste módulo
 * em código de produção (páginas, hooks, api).
 *
 * Uso típico em testes:
 *
 *   import { mockCompanies, mockCustomers } from '@/mocks'
 *
 * Cada lista é `ReadonlyArray<*Response>` — tipos idênticos aos
 * retornados pelo backend, então pode ser injetada em qualquer ponto
 * que aceite o resultado das APIs.
 */

export { mockCompanies } from './companies.mock'
export { mockCustomers } from './customers.mock'
export { mockSellers } from './sellers.mock'
export { mockProducts } from './products.mock'
export { mockQuotations } from './quotations.mock'
export { mockCarriers } from './carriers.mock'

/** Helper para embrulhar um array como `PagedResponse` (igual ao backend). */
export function asPaged<T>(
  content: ReadonlyArray<T>,
  page = 0,
  size = 20,
): {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
} {
  const totalElements = content.length
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  return {
    content: [...content],
    page,
    size,
    totalElements,
    totalPages,
    first: page === 0,
    last: page >= totalPages - 1,
  }
}