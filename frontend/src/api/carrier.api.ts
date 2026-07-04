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

/** Quando `true`, este módulo delega para os mocks em `src/mocks/api/`. */
const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true'

/** Parâmetros comuns para listagem. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /carriers — listagem paginada (status opcional). */
export async function listCarriers(
  params: PageParams & { status?: CarrierStatus },
): Promise<PagedResponse<CarrierResponse>> {
  if (USE_MOCKS) {
    const { listCarriers: mock } = await import('../mocks/api/carrier.api.mock')
    return mock(params)
  }
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
  if (USE_MOCKS) {
    const { searchCarriers: mock } = await import('../mocks/api/carrier.api.mock')
    return mock(params)
  }
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
  if (USE_MOCKS) {
    const { getCarrier: mock } = await import('../mocks/api/carrier.api.mock')
    return mock(id)
  }
  const { data } = await api.get<CarrierResponse>(`${BASE}/${id}`)
  return data
}

/** POST /carriers — cria uma nova transportadora. */
export async function createCarrier(
  payload: CarrierCreateRequest,
): Promise<CarrierResponse> {
  if (USE_MOCKS) {
    const { createCarrier: mock } = await import('../mocks/api/carrier.api.mock')
    return mock(payload)
  }
  const { data } = await api.post<CarrierResponse>(BASE, payload)
  return data
}

/** PATCH /carriers/{id} — atualização parcial. */
export async function updateCarrier(
  id: string,
  payload: CarrierUpdateRequest,
): Promise<CarrierResponse> {
  if (USE_MOCKS) {
    const { updateCarrier: mock } = await import('../mocks/api/carrier.api.mock')
    return mock(id, payload)
  }
  const { data } = await api.patch<CarrierResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /carriers/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateCarrier(id: string): Promise<void> {
  if (USE_MOCKS) {
    const { inactivateCarrier: mock } = await import(
      '../mocks/api/carrier.api.mock'
    )
    return mock(id)
  }
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /carriers/{id}/activate — reativação. */
export async function activateCarrier(id: string): Promise<CarrierResponse> {
  if (USE_MOCKS) {
    const { activateCarrier: mock } = await import(
      '../mocks/api/carrier.api.mock'
    )
    return mock(id)
  }
  const { data } = await api.patch<CarrierResponse>(`${BASE}/${id}/activate`)
  return data
}