/**
 * Cliente HTTP do módulo de boletos.
 *
 * Endpoints sob `/api/v1/boletos` (backend: BoletoController). Segue a
 * convenção dos demais `*.api.ts`: funções nomeadas, base path por
 * recurso, tipos em `types/boleto.ts`.
 */
import api from './client'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'
import type {
  BoletoCreateRequest,
  BoletoResponse,
  BoletoUpdateRequest,
} from '../types/boleto'

const BASE = '/api/v1/boletos'

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /boletos — listagem paginada (status opcional). */
export async function listBoletos(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<BoletoResponse>> {
  const { data } = await api.get<PagedResponse<BoletoResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'dueDate,asc',
      status: params.status,
    },
  })
  return data
}

/**
 * GET /boletos/search — busca flexível.
 * Aceita query e/ou status. Sem nenhum filtro, retorna todos paginados.
 */
export async function searchBoletos(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<BoletoResponse>> {
  const { data } = await api.get<PagedResponse<BoletoResponse>>(
    `${BASE}/search`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: 'dueDate,asc',
        query: params.query,
        status: params.status,
      },
    },
  )
  return data
}

/** GET /boletos/{id} — detalhe. */
export async function getBoleto(id: number): Promise<BoletoResponse> {
  const { data } = await api.get<BoletoResponse>(`${BASE}/${id}`)
  return data
}

/** POST /boletos — cria um novo boleto. */
export async function createBoleto(
  payload: BoletoCreateRequest,
): Promise<BoletoResponse> {
  const { data } = await api.post<BoletoResponse>(BASE, payload)
  return data
}

/** PATCH /boletos/{id} — atualização parcial. */
export async function updateBoleto(
  id: number,
  payload: BoletoUpdateRequest,
): Promise<BoletoResponse> {
  const { data } = await api.patch<BoletoResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /boletos/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateBoleto(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /boletos/{id}/activate — reativação. */
export async function activateBoleto(id: number): Promise<BoletoResponse> {
  const { data } = await api.patch<BoletoResponse>(`${BASE}/${id}/activate`)
  return data
}