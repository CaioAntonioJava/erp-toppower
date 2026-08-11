import api from './client'
import type {
  ServiceCategoryCreateRequest,
  ServiceCategoryResponse,
  ServiceCategoryUpdateRequest,
} from '../types/servicecategory'
import type { PagedResponse } from '../types/api'
import type { ServiceCategoryStatus } from '../types/servicecategory'

const BASE = '/api/v1/service-categories'

interface PageParams {
  page?: number
  size?: number
}

/** Lista todas as categorias ativas (não paginado) para uso em dropdowns/selects. */
export async function listActiveServiceCategories(): Promise<ServiceCategoryResponse[]> {
  const { data } = await api.get<ServiceCategoryResponse[]>(`${BASE}/active`)
  return data
}

export async function listServiceCategories(
  params: PageParams & { status?: ServiceCategoryStatus },
): Promise<PagedResponse<ServiceCategoryResponse>> {
  const { data } = await api.get<PagedResponse<ServiceCategoryResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'name,asc',
      status: params.status,
    },
  })
  return data
}

export async function getServiceCategory(id: number): Promise<ServiceCategoryResponse> {
  const { data } = await api.get<ServiceCategoryResponse>(`${BASE}/${id}`)
  return data
}

export async function createServiceCategory(
  payload: ServiceCategoryCreateRequest,
): Promise<ServiceCategoryResponse> {
  const { data } = await api.post<ServiceCategoryResponse>(BASE, payload)
  return data
}

export async function updateServiceCategory(
  id: number,
  payload: ServiceCategoryUpdateRequest,
): Promise<ServiceCategoryResponse> {
  const { data } = await api.patch<ServiceCategoryResponse>(`${BASE}/${id}`, payload)
  return data
}

export async function inactivateServiceCategory(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

export async function activateServiceCategory(id: number): Promise<ServiceCategoryResponse> {
  const { data } = await api.patch<ServiceCategoryResponse>(`${BASE}/${id}/activate`)
  return data
}