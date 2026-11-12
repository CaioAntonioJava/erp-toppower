/**
 * API client do módulo Financeiro de Contratos.
 *
 * Endpoints backend: /api/v1/contract-finances.
 */
import api from './client'
import type {
  ContractFinanceControlsPage,
  ContractFinanceControlResponse,
  ContractFinanceEntriesPage,
  ContractFinanceEntryCreateRequest,
  ContractFinanceEntryFilters,
  ContractFinanceEntryResponse,
  ContractFinanceEntryType,
  ContractFinanceEntryUpdateRequest,
  ContractFinancesSummaryResponse,
} from '../types/contractFinance'

const BASE = '/api/v1/contract-finances'

/** GET /contract-finances/controls — controles por contrato com totais. */
export async function listContractFinancesControls(params: {
  query?: string
  page?: number
  size?: number
}): Promise<ContractFinanceControlsPage> {
  const { data } = await api.get<ContractFinanceControlsPage>(`${BASE}/controls`, {
    params: {
      query: params.query,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
  })
  return data
}

/** GET /contract-finances/controls/{id} — detalhe do controle com totais. */
export async function getContractFinancesControl(id: number): Promise<ContractFinanceControlResponse> {
  const { data } = await api.get<ContractFinanceControlResponse>(`${BASE}/controls/${id}`)
  return data
}

/** GET /contract-finances/entries — lançamentos com filtros combináveis. */
export async function listContractFinanceEntries(
  filters: ContractFinanceEntryFilters = {},
): Promise<ContractFinanceEntriesPage> {
  const { data } = await api.get<ContractFinanceEntriesPage>(`${BASE}/entries`, {
    params: {
      controlId: filters.controlId,
      type: filters.type,
      status: filters.status,
      from: filters.from,
      to: filters.to,
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    },
  })
  return data
}

/** GET /contract-finances/entries/summary — resumo consolidado por filtros. */
export async function getContractFinanceSummary(filters: {
  controlId?: number
  type?: ContractFinanceEntryType
  from?: string
  to?: string
}): Promise<ContractFinancesSummaryResponse> {
  const { data } = await api.get<ContractFinancesSummaryResponse>(`${BASE}/entries/summary`, {
    params: {
      controlId: filters.controlId,
      type: filters.type,
      from: filters.from,
      to: filters.to,
    },
  })
  return data
}

/** GET /contract-finances/entries/{id} — detalhe do lançamento. */
export async function getContractFinanceEntry(id: number): Promise<ContractFinanceEntryResponse> {
  const { data } = await api.get<ContractFinanceEntryResponse>(`${BASE}/entries/${id}`)
  return data
}

/** POST /contract-finances/entries — lança um gasto/recebimento. */
export async function createContractFinanceEntry(
  payload: ContractFinanceEntryCreateRequest,
): Promise<ContractFinanceEntryResponse> {
  const { data } = await api.post<ContractFinanceEntryResponse>(`${BASE}/entries`, payload)
  return data
}

/** PATCH /contract-finances/entries/{id} — edita o lançamento (parcial). */
export async function updateContractFinanceEntry(
  id: number,
  payload: ContractFinanceEntryUpdateRequest,
): Promise<ContractFinanceEntryResponse> {
  const { data } = await api.patch<ContractFinanceEntryResponse>(
    `${BASE}/entries/${id}`,
    payload,
  )
  return data
}

/** DELETE /contract-finances/entries/{id} — soft delete (status INATIVO). */
export async function inactivateContractFinanceEntry(id: number): Promise<void> {
  await api.delete(`${BASE}/entries/${id}`)
}

/** PATCH /contract-finances/entries/{id}/activate — reativa o lançamento. */
export async function activateContractFinanceEntry(
  id: number,
): Promise<ContractFinanceEntryResponse> {
  const { data } = await api.patch<ContractFinanceEntryResponse>(`${BASE}/entries/${id}/activate`)
  return data
}
