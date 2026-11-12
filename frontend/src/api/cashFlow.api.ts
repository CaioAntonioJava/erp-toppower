/**
 * API client do fluxo de caixa consolidado.
 *
 * Endpoints backend: /api/v1/cash-flow.
 */
import api from './client'
import type { PagedResponse } from '../types/api'
import type { ClientSummaryResponse } from '../types/contract'
import type {
  CashFlowEntriesPage,
  CashFlowEntryResponse,
  CashFlowFilters,
  CashFlowSettingsRequest,
  CashFlowSettingsResponse,
  CashFlowSummaryResponse,
} from '../types/cashFlow'

const BASE = '/api/v1/cash-flow'

/** GET /cash-flow/settings — saldo inicial do caixa da organização corrente. */
export async function getCashFlowSettings(): Promise<CashFlowSettingsResponse> {
  const { data } = await api.get<CashFlowSettingsResponse>(`${BASE}/settings`)
  return data
}

/** PUT /cash-flow/settings — cria/atualiza o saldo inicial do caixa. */
export async function updateCashFlowSettings(
  payload: CashFlowSettingsRequest,
): Promise<CashFlowSettingsResponse> {
  const { data } = await api.put<CashFlowSettingsResponse>(`${BASE}/settings`, payload)
  return data
}

/** GET /cash-flow/summary — totais e série por período do fluxo de caixa. */
export async function getCashFlowSummary(
  filters: CashFlowFilters = {},
): Promise<CashFlowSummaryResponse> {
  const { data } = await api.get<CashFlowSummaryResponse>(`${BASE}/summary`, {
    params: {
      startDate: filters.startDate,
      endDate: filters.endDate,
      type: filters.type,
      sourceType: filters.sourceType,
      clientType: filters.clientType,
      clientId: filters.clientId,
      query: filters.query,
    },
  })
  return data
}

/** GET /cash-flow/entries — lançamentos consolidados paginados. */
export async function listCashFlowEntries(
  filters: CashFlowFilters = {},
): Promise<CashFlowEntriesPage> {
  const { data } = await api.get<PagedResponse<CashFlowEntryResponse>>(`${BASE}/entries`, {
    params: {
      startDate: filters.startDate,
      endDate: filters.endDate,
      type: filters.type,
      sourceType: filters.sourceType,
      clientType: filters.clientType,
      clientId: filters.clientId,
      query: filters.query,
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    },
  })
  return data
}

/**
 * GET /cash-flow/clients/search — typeahead de clientes (PF e PJ) ativos
 * para o filtro de cliente da tela de fluxo de caixa.
 */
export async function searchCashFlowClients(
  query: string,
  limit = 20,
): Promise<ClientSummaryResponse[]> {
  const { data } = await api.get<ClientSummaryResponse[]>(`${BASE}/clients/search`, {
    params: { query, limit },
  })
  return data
}