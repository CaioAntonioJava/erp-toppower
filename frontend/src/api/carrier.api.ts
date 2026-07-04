import api from './client'
import type {
  CarrierCreateRequest,
  CarrierResponse,
  CarrierUpdateRequest,
  CarrierName,
  CarrierStatus,
} from '../types/carrier'
import type { PagedResponse } from '../types/api'

const BASE = '/api/v1/carriers'

/** Parâmetros comuns para listagem. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /carriers — listagem paginada (status opcional). */
export async function listCarriers(
  params: PageParams & { status?: CarrierStatus },
): Promise<PagedResponse<CarrierResponse>> {
  const { data } = await api.get<PagedResponse<CarrierResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'carrierName,asc',
      status: params.status,
    },
  })
  return data
}

/**
 * GET /carriers/search — busca paginada por nome (exato) e/ou status.
 * O backend filtra `carrierName` por correspondência exata do enum.
 */
export async function searchCarriers(params: {
  carrierName?: CarrierName
  status?: CarrierStatus
  page?: number
  size?: number
}): Promise<PagedResponse<CarrierResponse>> {
  const { data } = await api.get<PagedResponse<CarrierResponse>>(
    `${BASE}/search`,
    {
      params: {
        carrierName: params.carrierName,
        status: params.status,
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: 'carrierName,asc',
      },
    },
  )
  return data
}

/** GET /carriers/{id} — detalhe. */
export async function getCarrier(id: string): Promise<CarrierResponse> {
  const { data } = await api.get<CarrierResponse>(`${BASE}/${id}`)
  return data
}

/** POST /carriers — cria uma nova transportadora. */
export async function createCarrier(
  payload: CarrierCreateRequest,
): Promise<CarrierResponse> {
  const { data } = await api.post<CarrierResponse>(BASE, payload)
  return data
}

/** PATCH /carriers/{id} — atualização parcial. */
export async function updateCarrier(
  id: string,
  payload: CarrierUpdateRequest,
): Promise<CarrierResponse> {
  const { data } = await api.patch<CarrierResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /carriers/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateCarrier(id: string): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /carriers/{id}/activate — reativação. */
export async function activateCarrier(id: string): Promise<CarrierResponse> {
  const { data } = await api.patch<CarrierResponse>(`${BASE}/${id}/activate`)
  return data
}