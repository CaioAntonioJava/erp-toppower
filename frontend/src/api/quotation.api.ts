import api from './client'
import type { PagedResponse } from '../types/api'
import type {
  ClientSummaryResponse,
  NextQuotationNumberResponse,
  QuotationCreateRequest,
  QuotationFilters,
  QuotationResponse,
  QuotationSummaryResponse,
  QuotationUpdateRequest,
} from '../types/quotation'

const BASE = '/api/v1/quotations'

/** GET /quotations — listagem paginada com filtros opcionais. */
export async function listQuotations(
  filters: QuotationFilters = {},
): Promise<PagedResponse<QuotationSummaryResponse>> {
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
  const { data } = await api.get<NextQuotationNumberResponse>(
    `${BASE}/next-number`,
  )
  return data.number
}

/** GET /quotations/{id} — detalhe completo (com itens e totais). */
export async function getQuotation(id: string): Promise<QuotationResponse> {
  const { data } = await api.get<QuotationResponse>(`${BASE}/${id}`)
  return data
}

/** GET /quotations/by-number/{number} — busca exata por número. */
export async function getQuotationByNumber(
  number: number,
): Promise<QuotationResponse> {
  const { data } = await api.get<QuotationResponse>(
    `${BASE}/by-number/${encodeURIComponent(String(number))}`,
  )
  return data
}

/** POST /quotations — cria uma nova proposta. */
export async function createQuotation(
  payload: QuotationCreateRequest,
): Promise<QuotationResponse> {
  const { data } = await api.post<QuotationResponse>(BASE, payload)
  return data
}

/** PATCH /quotations/{id} — atualização parcial. */
export async function updateQuotation(
  id: string,
  payload: QuotationUpdateRequest,
): Promise<QuotationResponse> {
  const { data } = await api.patch<QuotationResponse>(
    `${BASE}/${id}`,
    payload,
  )
  return data
}

/** DELETE /quotations/{id} — cancelamento (soft). Retorna a proposta atualizada. */
export async function cancelQuotation(id: string): Promise<QuotationResponse> {
  const { data } = await api.delete<QuotationResponse>(`${BASE}/${id}`)
  return data
}

/** GET /quotations/clients/search — typeahead de clientes (PF + PJ) ativos. */
export async function searchQuotationClients(
  query: string,
  limit = 20,
): Promise<ClientSummaryResponse[]> {
  const { data } = await api.get<ClientSummaryResponse[]>(
    `${BASE}/clients/search`,
    {
      params: { query, limit },
    },
  )
  return data
}