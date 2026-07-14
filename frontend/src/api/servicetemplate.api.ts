import api from './client'
import type {
  ServiceTemplateCreateRequest,
  ServiceTemplateResponse,
  ServiceTemplateUpdateRequest,
} from '../types/servicetemplate'
import type { PagedResponse } from '../types/api'

const BASE = '/api/v1/service-templates'

interface PageParams {
  page?: number
  size?: number
}

export async function listServiceTemplates(
  params: PageParams,
): Promise<PagedResponse<ServiceTemplateResponse>> {
  const { data } = await api.get<PagedResponse<ServiceTemplateResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'name,asc',
    },
  })
  return data
}

export async function searchServiceTemplates(params: {
  query?: string
  page?: number
  size?: number
}): Promise<PagedResponse<ServiceTemplateResponse>> {
  const { data } = await api.get<PagedResponse<ServiceTemplateResponse>>(
    `${BASE}/search`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: 'name,asc',
        query: params.query,
      },
    },
  )
  return data
}

export async function getServiceTemplate(id: number): Promise<ServiceTemplateResponse> {
  const { data } = await api.get<ServiceTemplateResponse>(`${BASE}/${id}`)
  return data
}

export async function createServiceTemplate(
  payload: ServiceTemplateCreateRequest,
): Promise<ServiceTemplateResponse> {
  const { data } = await api.post<ServiceTemplateResponse>(BASE, payload)
  return data
}

export async function updateServiceTemplate(
  id: number,
  payload: ServiceTemplateUpdateRequest,
): Promise<ServiceTemplateResponse> {
  const { data } = await api.patch<ServiceTemplateResponse>(`${BASE}/${id}`, payload)
  return data
}

export async function deleteServiceTemplate(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}
