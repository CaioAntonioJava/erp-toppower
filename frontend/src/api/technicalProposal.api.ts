import api from './client'
import type { PagedResponse } from '../types/api'
import type { ClientSummaryResponse } from '../types/quotation'
import type {
  NextTechnicalProposalCodeResponse,
  TechnicalProposalClientType,
  TechnicalProposalCreateRequest,
  TechnicalProposalFilters,
  TechnicalProposalResponse,
  TechnicalProposalSimulateRequest,
  TechnicalProposalSimulateResponse,
  TechnicalProposalSummaryResponse,
  TechnicalProposalUpdateRequest,
} from '../types/technicalProposal'

const BASE = '/api/v1/technical-proposals'

/** GET /technical-proposals — listagem paginada com filtros opcionais. */
export async function listTechnicalProposals(
  filters: TechnicalProposalFilters = {},
): Promise<PagedResponse<TechnicalProposalSummaryResponse>> {
  const { data } = await api.get<PagedResponse<TechnicalProposalSummaryResponse>>(
    BASE,
    {
      params: {
        page: filters.page ?? 0,
        size: filters.size ?? 20,
        status: filters.status,
        startDate: filters.startDate,
        endDate: filters.endDate,
        clientUuid: filters.clientUuid,
        code: filters.code,
      },
    },
  )
  return data
}

/** GET /technical-proposals/next-code — pré-visualiza o próximo código. */
export async function getNextTechnicalProposalCode(): Promise<NextTechnicalProposalCodeResponse> {
  const { data } = await api.get<NextTechnicalProposalCodeResponse>(
    `${BASE}/next-code`,
  )
  return data
}

/** GET /technical-proposals/{id} — detalhe completo (com itens e totais). */
export async function getTechnicalProposal(
  id: string,
): Promise<TechnicalProposalResponse> {
  const { data } = await api.get<TechnicalProposalResponse>(`${BASE}/${id}`)
  return data
}

/** GET /technical-proposals/by-code/{code} — busca exata por código formatado. */
export async function getTechnicalProposalByCode(
  code: string,
): Promise<TechnicalProposalResponse> {
  const { data } = await api.get<TechnicalProposalResponse>(
    `${BASE}/by-code/${encodeURIComponent(code)}`,
  )
  return data
}

/** POST /technical-proposals — cria uma nova proposta técnica. */
export async function createTechnicalProposal(
  payload: TechnicalProposalCreateRequest,
): Promise<TechnicalProposalResponse> {
  const { data } = await api.post<TechnicalProposalResponse>(BASE, payload)
  return data
}

/** POST /technical-proposals/simulate — calcula os totais sem persistir. */
export async function simulateTechnicalProposal(
  payload: TechnicalProposalSimulateRequest,
): Promise<TechnicalProposalSimulateResponse> {
  const { data } = await api.post<TechnicalProposalSimulateResponse>(
    `${BASE}/simulate`,
    payload,
  )
  return data
}

/** PATCH /technical-proposals/{id} — atualização parcial. */
export async function updateTechnicalProposal(
  id: string,
  payload: TechnicalProposalUpdateRequest,
): Promise<TechnicalProposalResponse> {
  const { data } = await api.patch<TechnicalProposalResponse>(
    `${BASE}/${id}`,
    payload,
  )
  return data
}

/** POST /technical-proposals/{id}/start — ABERTA → EM_ANDAMENTO. */
export async function startTechnicalProposal(
  id: string,
): Promise<TechnicalProposalResponse> {
  const { data } = await api.post<TechnicalProposalResponse>(
    `${BASE}/${id}/start`,
  )
  return data
}

/** POST /technical-proposals/{id}/complete — EM_ANDAMENTO → CONCLUIDA. */
export async function completeTechnicalProposal(
  id: string,
): Promise<TechnicalProposalResponse> {
  const { data } = await api.post<TechnicalProposalResponse>(
    `${BASE}/${id}/complete`,
  )
  return data
}

/** POST /technical-proposals/{id}/reopen — CONCLUIDA → EM_ANDAMENTO. */
export async function reopenTechnicalProposal(
  id: string,
): Promise<TechnicalProposalResponse> {
  const { data } = await api.post<TechnicalProposalResponse>(
    `${BASE}/${id}/reopen`,
  )
  return data
}

/** GET /technical-proposals/clients/search — typeahead de clientes (delegado ao serviço compartilhado). */
export async function searchTechnicalProposalClients(
  query: string,
  limit = 20,
  type?: TechnicalProposalClientType,
): Promise<ClientSummaryResponse[]> {
  const { data } = await api.get<ClientSummaryResponse[]>(
    `${BASE}/clients/search`,
    { params: { query, limit, type } },
  )
  return data
}

/**
 * GET /technical-proposals/{id}/pdf — baixa o PDF da proposta técnica
 * como Blob, autenticado pelo axios. Use com `URL.createObjectURL` para
 * preview ou `<a download>` para download direto.
 */
export async function getTechnicalProposalPdf(
  id: string,
  disposition: 'inline' | 'attachment' = 'inline',
): Promise<{ blob: Blob; filename: string }> {
  const response = await api.get<Blob>(`${BASE}/${id}/pdf`, {
    params: { disposition },
    responseType: 'blob',
  })
  const filename = parseFilename(response.headers['content-disposition'])
    ?? `proposta-tecnica-${id}.pdf`
  return { blob: response.data, filename }
}

/** Helper interno: extrai filename do header Content-Disposition. */
function parseFilename(contentDisposition: string | undefined): string | null {
  if (!contentDisposition) return null
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(contentDisposition)
  return match ? match[1] : null
}