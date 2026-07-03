/**
 * Store em memória para os mocks de API.
 *
 * Apenas para desenvolvimento/teste manual. NÃO importar fora de `src/mocks/`.
 *
 * As listas começam com uma cópia profunda dos seeds (deep clone) para
 * que mutações (create/update/inactivate/activate) não afetem o array
 * original importado por testes que não querem comportamento dinâmico.
 *
 * Mutações são síncronas e vivem até o reload da página. Não há
 * persistência em localStorage — isso é proposital para manter os
 * mocks determinísticos entre sessões (sempre partem dos seeds).
 */

import {
  mockCompanies as seedCompanies,
  mockCustomers as seedCustomers,
  mockSellers as seedSellers,
  mockProducts as seedProducts,
  mockQuotations as seedQuotations,
} from '..'
import type { CompanyResponse } from '../../types/company'
import type { CustomerResponse } from '../../types/customer'
import type { SellerResponse } from '../../types/seller'
import type { ProductResponse } from '../../types/product'
import type { QuotationResponse } from '../../types/quotation'

/** Clone profundo via JSON — seguro porque os seeds só têm dados primitivos. */
function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

interface Store {
  companies: CompanyResponse[]
  customers: CustomerResponse[]
  sellers: SellerResponse[]
  products: ProductResponse[]
  quotations: QuotationResponse[]
}

/**
 * Singleton criado na primeira importação do módulo. Cada módulo de mock
 * (companies, customers, sellers, products, quotations) importa o mesmo
 * objeto, garantindo que mutações feitas por um módulo sejam visíveis
 * pelos outros.
 */
export const store: Store = {
  companies: clone(seedCompanies) as CompanyResponse[],
  customers: clone(seedCustomers) as CustomerResponse[],
  sellers: clone(seedSellers) as SellerResponse[],
  products: clone(seedProducts) as ProductResponse[],
  quotations: clone(seedQuotations) as QuotationResponse[],
}

/**
 * Gera um UUID v4 simples. Não usa `crypto.randomUUID()` para evitar
 * problemas em navegadores antigos durante dev — e porque os seeds
 * já têm UUIDs determinísticos.
 */
export function makeUuid(): string {
  const hex = '0123456789abcdef'
  let out = ''
  for (let i = 0; i < 36; i++) {
    if (i === 8 || i === 13 || i === 18 || i === 23) {
      out += '-'
    } else if (i === 14) {
      out += '4'
    } else if (i === 19) {
      out += hex.charAt(8 + Math.floor(Math.random() * 4))
    } else {
      out += hex.charAt(Math.floor(Math.random() * 16))
    }
  }
  return out
}

/** Data/hora ISO atual — usada em `createdAt`/`updatedAt` ao criar/editar. */
export function nowIso(): string {
  return new Date().toISOString()
}