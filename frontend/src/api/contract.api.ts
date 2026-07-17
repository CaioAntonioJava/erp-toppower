import api from './client'
import type {
  ClientSummaryResponse,
  ContractClientType,
  ContractCreateRequest,
  ContractNextCodeResponse,
  ContractResponse,
  ContractUpdateRequest,
} from '../types/contract'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'

const BASE = '/api/v1/contracts'

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /contracts — listagem paginada (status opcional). */
export async function listContracts(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<ContractResponse>> {
  const { data } = await api.get<PagedResponse<ContractResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'validityDate,desc',
      status: params.status,
    },
  })
  return data
}

/**
 * GET /contracts/search — busca flexível.
 * Aceita query e/ou status. Sem nenhum filtro, retorna todos paginados.
 */
export async function searchContracts(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<ContractResponse>> {
  const { data } = await api.get<PagedResponse<ContractResponse>>(
    `${BASE}/search`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: 'validityDate,desc',
        query: params.query,
        status: params.status,
      },
    },
  )
  return data
}

/** GET /contracts/{id} — detalhe. */
export async function getContract(id: number): Promise<ContractResponse> {
  const { data } = await api.get<ContractResponse>(`${BASE}/${id}`)
  return data
}

/**
 * GET /contracts/next-code — pré-visualiza o próximo código comercial
 * (ex.: CL-001-2026) e o título padrão que seria atribuído ao contrato.
 * Não persiste nada.
 */
export async function getNextContractCode(): Promise<ContractNextCodeResponse> {
  const { data } = await api.get<ContractNextCodeResponse>(`${BASE}/next-code`)
  return data
}

/** POST /contracts — cria um novo contrato. O código é gerado pelo servidor. */
export async function createContract(
  payload: ContractCreateRequest,
): Promise<ContractResponse> {
  const { data } = await api.post<ContractResponse>(BASE, payload)
  return data
}

/** PATCH /contracts/{id} — atualização parcial. */
export async function updateContract(
  id: number,
  payload: ContractUpdateRequest,
): Promise<ContractResponse> {
  const { data } = await api.patch<ContractResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /contracts/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateContract(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /contracts/{id}/activate — reativação. */
export async function activateContract(id: number): Promise<ContractResponse> {
  const { data } = await api.patch<ContractResponse>(
    `${BASE}/${id}/activate`,
  )
  return data
}

/**
 * GET /contracts/{id}/pdf — baixa o PDF do contrato como Blob,
 * autenticado pelo axios e respeitando a Organization ativa.
 * Retorna o Blob + o nome de arquivo sugerido pelo servidor.
 */
export async function getContractPdf(
  id: number,
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

/**
 * Extrai o filename do cabeçalho Content-Disposition.
 */
function parseFilename(contentDisposition: string | undefined): string | null {
  if (!contentDisposition) return null
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(contentDisposition)
  return match ? match[1] : null
}

/**
 * GET /contracts/clients/search — busca clientes (PF e PJ) para
 * seleção no formulário de contrato.
 */
export async function searchContractClients(
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