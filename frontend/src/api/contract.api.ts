import api from './client'
import type { PagedResponse } from '../types/api'
import type { ClientSummaryResponse } from '../types/quotation'
import type {
  ContractClientType,
  ContractCreateRequest,
  ContractFilters,
  ContractResponse,
  ContractSummaryResponse,
  ContractUpdateRequest,
  NextContractCodeResponse,
} from '../types/contract'

const BASE = '/api/v1/contracts'

/** GET /contracts — listagem paginada com filtros opcionais. */
export async function listContracts(
  filters: ContractFilters = {},
): Promise<PagedResponse<ContractSummaryResponse>> {
  const { data } = await api.get<PagedResponse<ContractSummaryResponse>>(BASE, {
    params: {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      status: filters.status,
      startDate: filters.startDate,
      endDate: filters.endDate,
      customerUuid: filters.customerUuid,
      code: filters.code,
    },
  })
  return data
}

/** GET /contracts/next-code — pré-visualiza o próximo código. */
export async function getNextContractCode(): Promise<NextContractCodeResponse> {
  const { data } = await api.get<NextContractCodeResponse>(`${BASE}/next-code`)
  return data
}

/** GET /contracts/{id} — detalhe completo. */
export async function getContract(id: string): Promise<ContractResponse> {
  const { data } = await api.get<ContractResponse>(`${BASE}/${id}`)
  return data
}

/** GET /contracts/by-code/{code} — busca exata por código formatado. */
export async function getContractByCode(code: string): Promise<ContractResponse> {
  const { data } = await api.get<ContractResponse>(
    `${BASE}/by-code/${encodeURIComponent(code)}`,
  )
  return data
}

/** POST /contracts — cria um novo contrato. */
export async function createContract(
  payload: ContractCreateRequest,
): Promise<ContractResponse> {
  const { data } = await api.post<ContractResponse>(BASE, payload)
  return data
}

/** PATCH /contracts/{id} — atualização parcial. */
export async function updateContract(
  id: string,
  payload: ContractUpdateRequest,
): Promise<ContractResponse> {
  const { data } = await api.patch<ContractResponse>(`${BASE}/${id}`, payload)
  return data
}

/** POST /contracts/{id}/start — ABERTA → EM_ANDAMENTO. */
export async function startContract(id: string): Promise<ContractResponse> {
  const { data } = await api.post<ContractResponse>(`${BASE}/${id}/start`)
  return data
}

/** POST /contracts/{id}/complete — EM_ANDAMENTO → CONCLUIDA. */
export async function completeContract(id: string): Promise<ContractResponse> {
  const { data } = await api.post<ContractResponse>(`${BASE}/${id}/complete`)
  return data
}

/** POST /contracts/{id}/reopen — CONCLUIDA → EM_ANDAMENTO. */
export async function reopenContract(id: string): Promise<ContractResponse> {
  const { data } = await api.post<ContractResponse>(`${BASE}/${id}/reopen`)
  return data
}

/** GET /contracts/clients/search — typeahead de clientes (PF + PJ). */
export async function searchContractsClients(
  query: string,
  limit = 20,
  type?: ContractClientType,
): Promise<ClientSummaryResponse[]> {
  const { data } = await api.get<ClientSummaryResponse[]>(
    `${BASE}/clients/search`,
    { params: { query, limit, type } },
  )
  return data
}

/**
 * GET /contracts/{id}/pdf — baixa o PDF do contrato como Blob,
 * autenticado pelo axios. Use com `URL.createObjectURL` para preview ou
 * `<a download>` para download direto.
 */
export async function getContractPdf(
  id: string,
  disposition: 'inline' | 'attachment' = 'inline',
): Promise<{ blob: Blob; filename: string }> {
  const response = await api.get<Blob>(`${BASE}/${id}/pdf`, {
    params: { disposition },
    responseType: 'blob',
  })
  const filename = parseFilename(response.headers['content-disposition'])
    ?? `contrato-${id}.pdf`
  return { blob: response.data, filename }
}

/** Helper interno: extrai filename do header Content-Disposition. */
function parseFilename(contentDisposition: string | undefined): string | null {
  if (!contentDisposition) return null
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(contentDisposition)
  return match ? match[1] : null
}