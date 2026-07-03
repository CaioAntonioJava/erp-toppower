/**
 * Mock das funções de `src/api/quotation.api.ts`.
 *
 * Apenas para desenvolvimento/teste manual. Ativado quando
 * `VITE_USE_MOCKS === 'true'` no .env.
 *
 * Cobre todas as 8 funções expostas pelo módulo real:
 * listQuotations, getNextQuotationNumber, getQuotation,
 * getQuotationByNumber, createQuotation, updateQuotation,
 * cancelQuotation, searchQuotationClients.
 */

import type {
  ClientSummaryResponse,
  QuotationClientType,
  QuotationCreateRequest,
  QuotationFilters,
  QuotationItemRequest,
  QuotationItemResponse,
  QuotationResponse,
  QuotationSummaryResponse,
  QuotationUpdateRequest,
} from '../../types/quotation'
import type { PagedResponse } from '../../types/api'
import { delay, mockError, pagedSlice } from './_helpers'
import { makeUuid, nowIso, store } from './store'

const NUMBER_START = 1500

function toSummary(q: QuotationResponse): QuotationSummaryResponse {
  let clientName = ''
  if (q.clientType === 'CUSTOMER' && q.customerUuid) {
    clientName =
      store.customers.find((c) => c.uuid === q.customerUuid)?.name ?? ''
  } else if (q.clientType === 'COMPANY' && q.companyUuid) {
    const c = store.companies.find((co) => co.uuid === q.companyUuid)
    clientName = c?.tradeName ?? c?.legalName ?? ''
  }
  return {
    uuid: q.uuid,
    number: q.number,
    issueDate: q.issueDate,
    clientType: q.clientType,
    clientUuid: q.clientType === 'CUSTOMER' ? q.customerUuid ?? '' : q.companyUuid ?? '',
    clientName,
    sellerUuid: q.sellerUuid,
    status: q.status,
    totalQuantity: q.totalQuantity,
    total: q.total,
    paymentCondition: q.paymentCondition,
  }
}

function buildItem(req: QuotationItemRequest, itemUuid: string): QuotationItemResponse {
  const lineSubtotal = req.unitPrice * req.quantity
  let totalPrice = lineSubtotal
  if (req.discount != null && req.discountType != null) {
    if (req.discountType === 'PERCENT') {
      totalPrice = Math.max(0, lineSubtotal - (lineSubtotal * req.discount) / 100)
    } else {
      totalPrice = Math.max(0, lineSubtotal - req.discount)
    }
  }
  return {
    uuid: itemUuid,
    productUuid: req.productUuid,
    quantity: req.quantity,
    unitPrice: req.unitPrice,
    lineSubtotal,
    discountType: req.discountType ?? null,
    discount: req.discount ?? null,
    totalPrice,
  }
}

function totalsFor(items: QuotationItemResponse[], discountType: QuotationResponse['discountType'], discount: QuotationResponse['discount']) {
  const subtotal = items.reduce((s, it) => s + it.totalPrice, 0)
  let globalDiscountValue = 0
  if (discount != null && discountType != null) {
    if (discountType === 'PERCENT') {
      globalDiscountValue = Math.min(subtotal, (subtotal * discount) / 100)
    } else {
      globalDiscountValue = Math.min(subtotal, discount)
    }
  }
  const total = Math.max(0, subtotal - globalDiscountValue)
  const totalQuantity = items.reduce((s, it) => s + it.quantity, 0)
  return { subtotal, total, totalQuantity }
}

// =====================================================================
// Endpoints
// =====================================================================

export async function listQuotations(
  filters: QuotationFilters = {},
): Promise<PagedResponse<QuotationSummaryResponse>> {
  await delay()
  let items = store.quotations.slice()

  // Filtro por status.
  if (filters.status) {
    items = items.filter((q) => q.status === filters.status)
  }
  // Filtro por número (substring do número).
  if (filters.number && filters.number.trim().length > 0) {
    const n = filters.number.trim()
    items = items.filter((q) => String(q.number).includes(n))
  }
  // Filtros por data (issueDate é YYYY-MM-DD string).
  if (filters.startDate) {
    items = items.filter((q) => q.issueDate >= filters.startDate!)
  }
  if (filters.endDate) {
    items = items.filter((q) => q.issueDate <= filters.endDate!)
  }
  if (filters.clientUuid) {
    const u = filters.clientUuid
    items = items.filter(
      (q) => q.customerUuid === u || q.companyUuid === u,
    )
  }
  if (filters.sellerUuid) {
    const u = filters.sellerUuid
    items = items.filter((q) => q.sellerUuid === u)
  }

  // Ordena por número decrescente (proposta mais recente primeiro).
  items.sort((a, b) => b.number - a.number)

  // Paginação.
  const page = filters.page ?? 0
  const size = filters.size ?? 20
  const totalElements = items.length
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  const start = page * size
  const content = items.slice(start, start + size).map(toSummary)

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

export async function getNextQuotationNumber(): Promise<number> {
  await delay()
  const max = store.quotations.reduce(
    (m, q) => (q.number > m ? q.number : m),
    NUMBER_START - 1,
  )
  return max + 1
}

export async function getQuotation(id: string): Promise<QuotationResponse> {
  await delay()
  const found = store.quotations.find((q) => q.uuid === id)
  if (!found) throw mockError(404, `Proposta ${id} não encontrada.`)
  return found
}

export async function getQuotationByNumber(number: number): Promise<QuotationResponse> {
  await delay()
  const found = store.quotations.find((q) => q.number === number)
  if (!found) throw mockError(404, `Proposta ${number} não encontrada.`)
  return found
}

export async function createQuotation(
  payload: QuotationCreateRequest,
): Promise<QuotationResponse> {
  await delay()
  if (payload.items.length === 0) {
    throw mockError(400, 'A proposta deve ter ao menos um item.', { items: 'obrigatório' })
  }
  // Valida cliente.
  const clientType: QuotationClientType = payload.customerUuid ? 'CUSTOMER' : 'COMPANY'
  const clientUuid = payload.customerUuid ?? payload.companyUuid ?? ''
  if (!clientUuid) {
    throw mockError(400, 'Cliente obrigatório.', { clientUuid: 'obrigatório' })
  }
  // Valida vendedor.
  const seller = store.sellers.find((s) => s.uuid === payload.sellerUuid)
  if (!seller) {
    throw mockError(400, 'Vendedor inválido.', { sellerUuid: 'inválido' })
  }

  const items: QuotationItemResponse[] = payload.items.map((it) =>
    buildItem(it, makeUuid()),
  )
  const t = totalsFor(items, payload.discountType ?? null, payload.discount ?? null)

  const created: QuotationResponse = {
    uuid: makeUuid(),
    number: await getNextQuotationNumber(),
    issueDate: new Date().toISOString().slice(0, 10),
    customerUuid: payload.customerUuid ?? null,
    companyUuid: payload.companyUuid ?? null,
    clientType,
    attention: payload.attention ?? null,
    sellerUuid: payload.sellerUuid,
    items,
    discountType: payload.discountType ?? null,
    discount: payload.discount ?? null,
    validityDays: payload.validityDays ?? null,
    paymentCondition: payload.paymentCondition ?? null,
    notes: payload.notes ?? null,
    status: 'ATIVA',
    subtotal: t.subtotal,
    total: t.total,
    totalQuantity: t.totalQuantity,
    createdAt: nowIso(),
    updatedAt: nowIso(),
    createdBy: 'mock@toppower.local',
    updatedBy: null,
  }
  store.quotations.push(created)
  return created
}

export async function updateQuotation(
  id: string,
  payload: QuotationUpdateRequest,
): Promise<QuotationResponse> {
  await delay()
  const idx = store.quotations.findIndex((q) => q.uuid === id)
  if (idx === -1) throw mockError(404, `Proposta ${id} não encontrada.`)
  const current = store.quotations[idx]
  if (current.status === 'CANCELADA' || current.status === 'CONVERTIDA') {
    throw mockError(
      409,
      `Propostas ${current.status === 'CANCELADA' ? 'canceladas' : 'convertidas'} não podem ser editadas.`,
    )
  }

  const items = payload.items
    ? payload.items.map((it) => buildItem(it, makeUuid()))
    : current.items
  const t = totalsFor(
    items,
    payload.discountType !== undefined ? payload.discountType : current.discountType,
    payload.discount !== undefined ? payload.discount : current.discount,
  )

  const updated: QuotationResponse = {
    ...current,
    customerUuid:
      payload.customerUuid !== undefined ? payload.customerUuid : current.customerUuid,
    companyUuid:
      payload.companyUuid !== undefined ? payload.companyUuid : current.companyUuid,
    clientType: payload.customerUuid
      ? 'CUSTOMER'
      : payload.companyUuid
        ? 'COMPANY'
        : current.clientType,
    attention: payload.attention !== undefined ? payload.attention : current.attention,
    sellerUuid: payload.sellerUuid ?? current.sellerUuid,
    items,
    discountType:
      payload.discountType !== undefined ? payload.discountType : current.discountType,
    discount: payload.discount !== undefined ? payload.discount : current.discount,
    validityDays:
      payload.validityDays !== undefined ? payload.validityDays : current.validityDays,
    paymentCondition:
      payload.paymentCondition !== undefined
        ? payload.paymentCondition
        : current.paymentCondition,
    notes: payload.notes !== undefined ? payload.notes : current.notes,
    subtotal: t.subtotal,
    total: t.total,
    totalQuantity: t.totalQuantity,
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  store.quotations[idx] = updated
  return updated
}

export async function cancelQuotation(id: string): Promise<QuotationResponse> {
  await delay()
  const idx = store.quotations.findIndex((q) => q.uuid === id)
  if (idx === -1) throw mockError(404, `Proposta ${id} não encontrada.`)
  if (store.quotations[idx].status === 'CANCELADA') {
    return store.quotations[idx]
  }
  store.quotations[idx] = {
    ...store.quotations[idx],
    status: 'CANCELADA',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  return store.quotations[idx]
}

export async function searchQuotationClients(
  query: string,
  limit = 20,
  type?: QuotationClientType,
): Promise<ClientSummaryResponse[]> {
  await delay()
  const q = query.trim().toLowerCase()
  // eslint-disable-next-line no-console
  console.log('[MOCK searchQuotationClients]', { query, limit, type, q })

  // Filtro de tipo: 'CUSTOMER' (apenas PF), 'COMPANY' (apenas PJ) ou
  // undefined/vazio (ambos). Espelha o contrato do endpoint real.
  const includeCustomers = !type || type === 'CUSTOMER'
  const includeCompanies = !type || type === 'COMPANY'

  const customerMatches: ClientSummaryResponse[] = includeCustomers
    ? store.customers
        .filter((c) => c.status === 'ATIVO')
        .filter(
          (c) =>
            !q ||
            c.name.toLowerCase().includes(q) ||
            c.code.toLowerCase().includes(q) ||
            c.cpf.replace(/\D/g, '').includes(q.replace(/\D/g, '')),
        )
        .map((c) => ({
          type: 'CUSTOMER',
          uuid: c.uuid,
          code: c.code,
          name: c.name,
          document: c.cpf,
        }))
    : []

  const companyMatches: ClientSummaryResponse[] = includeCompanies
    ? store.companies
        .filter((c) => c.status === 'ATIVO')
        .filter(
          (c) =>
            !q ||
            c.legalName.toLowerCase().includes(q) ||
            (c.tradeName?.toLowerCase().includes(q) ?? false) ||
            c.code.toLowerCase().includes(q) ||
            c.cnpj.replace(/\D/g, '').includes(q.replace(/\D/g, '')),
        )
        .map((c) => ({
          type: 'COMPANY',
          uuid: c.uuid,
          code: c.code,
          name: c.tradeName ?? c.legalName,
          document: c.cnpj,
        }))
    : []

  return [...customerMatches, ...companyMatches].slice(0, limit)
}

// Re-exporta pagedSlice caso algum import queira usar helpers compartilhados.
void pagedSlice