import api from './client'
import type { PagedResponse } from '../types/api'
import type {
  ClientSummaryResponse,
  NextQuotationNumberResponse,
  QuotationClientType,
  QuotationCreateRequest,
  QuotationFilters,
  QuotationResponse,
  QuotationSummaryResponse,
  QuotationUpdateRequest,
} from '../types/quotation'

const BASE = '/api/v1/quotations'

/** Quando `true`, este módulo delega para os mocks em `src/mocks/api/`. */
const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true'

/** GET /quotations — listagem paginada com filtros opcionais. */
export async function listQuotations(
  filters: QuotationFilters = {},
): Promise<PagedResponse<QuotationSummaryResponse>> {
  if (USE_MOCKS) {
    const { listQuotations: mock } = await import('../mocks/api/quotation.api.mock')
    return mock(filters)
  }
  const { data } = await api.get<PagedResponse<QuotationSummaryResponse>>(
    BASE,
    {
      params: {
        page: filters.page ?? 0,
        size: filters.size ?? 20,
        status: filters.status,
        startDate: filters.startDate,
        endDate: filters.endDate,
        clientUuid: filters.clientUuid,
        sellerUuid: filters.sellerUuid,
        number: filters.number,
      },
    },
  )
  return data
}

/** GET /quotations/next-number — pré-visualiza o próximo número. */
export async function getNextQuotationNumber(): Promise<number> {
  if (USE_MOCKS) {
    const { getNextQuotationNumber: mock } = await import('../mocks/api/quotation.api.mock')
    return mock()
  }
  const { data } = await api.get<NextQuotationNumberResponse>(
    `${BASE}/next-number`,
  )
  return data.number
}

/** GET /quotations/{id} — detalhe completo (com itens e totais). */
export async function getQuotation(id: string): Promise<QuotationResponse> {
  if (USE_MOCKS) {
    const { getQuotation: mock } = await import('../mocks/api/quotation.api.mock')
    return mock(id)
  }
  const { data } = await api.get<QuotationResponse>(`${BASE}/${id}`)
  return data
}

/** GET /quotations/by-number/{number} — busca exata por número. */
export async function getQuotationByNumber(
  number: number,
): Promise<QuotationResponse> {
  if (USE_MOCKS) {
    const { getQuotationByNumber: mock } = await import('../mocks/api/quotation.api.mock')
    return mock(number)
  }
  const { data } = await api.get<QuotationResponse>(
    `${BASE}/by-number/${encodeURIComponent(String(number))}`,
  )
  return data
}

/** POST /quotations — cria uma nova proposta. */
export async function createQuotation(
  payload: QuotationCreateRequest,
): Promise<QuotationResponse> {
  if (USE_MOCKS) {
    const { createQuotation: mock } = await import('../mocks/api/quotation.api.mock')
    return mock(payload)
  }
  const { data } = await api.post<QuotationResponse>(BASE, payload)
  return data
}

/** PATCH /quotations/{id} — atualização parcial. */
export async function updateQuotation(
  id: string,
  payload: QuotationUpdateRequest,
): Promise<QuotationResponse> {
  if (USE_MOCKS) {
    const { updateQuotation: mock } = await import('../mocks/api/quotation.api.mock')
    return mock(id, payload)
  }
  const { data } = await api.patch<QuotationResponse>(
    `${BASE}/${id}`,
    payload,
  )
  return data
}

/** DELETE /quotations/{id} — cancelamento (soft). Retorna a proposta atualizada. */
export async function cancelQuotation(id: string): Promise<QuotationResponse> {
  if (USE_MOCKS) {
    const { cancelQuotation: mock } = await import('../mocks/api/quotation.api.mock')
    return mock(id)
  }
  const { data } = await api.delete<QuotationResponse>(`${BASE}/${id}`)
  return data
}

/** GET /quotations/clients/search — typeahead de clientes (PF + PJ) ativos. */
export async function searchQuotationClients(
  query: string,
  limit = 20,
  type?: QuotationClientType,
): Promise<ClientSummaryResponse[]> {
  if (USE_MOCKS) {
    const { searchQuotationClients: mock } = await import('../mocks/api/quotation.api.mock')
    return mock(query, limit, type)
  }
  const { data } = await api.get<ClientSummaryResponse[]>(
    `${BASE}/clients/search`,
    {
      params: { query, limit, type },
    },
  )
  return data
}