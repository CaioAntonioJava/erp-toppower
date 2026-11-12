import api from './client'
import type {
  CarrierCreateRequest,
  CarrierResponse,
  CarrierUpdateRequest,
} from '../types/carrier'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'

const BASE = '/api/v1/carriers'

interface PageParams {
  page?: number
  size?: number
}

export async function listCarriers(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<CarrierResponse>> {
  const { data } = await api.get<PagedResponse<CarrierResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'name,asc',
      status: params.status,
    },
  })
  return data
}

export async function searchCarriers(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<CarrierResponse>> {
  const { data } = await api.get<PagedResponse<CarrierResponse>>(
    `${BASE}/search`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: 'name,asc',
        query: params.query,
        status: params.status,
      },
    },
  )
  return data
}

export async function getCarrier(id: number): Promise<CarrierResponse> {
  const { data } = await api.get<CarrierResponse>(`${BASE}/${id}`)
  return data
}

export async function createCarrier(
  payload: CarrierCreateRequest,
): Promise<CarrierResponse> {
  const { data } = await api.post<CarrierResponse>(BASE, payload)
  return data
}

export async function updateCarrier(
  id: number,
  payload: CarrierUpdateRequest,
): Promise<CarrierResponse> {
  const { data } = await api.patch<CarrierResponse>(`${BASE}/${id}`, payload)
  return data
}

export async function inactivateCarrier(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

export async function activateCarrier(id: number): Promise<CarrierResponse> {
  const { data } = await api.patch<CarrierResponse>(
    `${BASE}/${id}/activate`,
  )
  return data
}