import api from './client'
import type {
  CustomerCreateRequest,
  CustomerResponse,
  CustomerUpdateRequest,
} from '../types/customer'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'

const BASE = '/api/v1/customers'

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /customers — listagem paginada (status opcional). */
export async function listCustomers(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<CustomerResponse>> {
  const { data } = await api.get<PagedResponse<CustomerResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'name,asc',
      status: params.status,
    },
  })
  return data
}

/**
 * GET /customers/search — busca flexível.
 * Aceita query e/ou status. Sem nenhum filtro, retorna todos paginados.
 */
export async function searchCustomers(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<CustomerResponse>> {
  const { data } = await api.get<PagedResponse<CustomerResponse>>(
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

/** GET /customers/{id} — detalhe. Requer ROLE_ADMIN no backend. */
export async function getCustomer(id: string): Promise<CustomerResponse> {
  const { data } = await api.get<CustomerResponse>(`${BASE}/${id}`)
  return data
}

/** POST /customers — cria um novo cliente. */
export async function createCustomer(
  payload: CustomerCreateRequest,
): Promise<CustomerResponse> {
  const { data } = await api.post<CustomerResponse>(BASE, payload)
  return data
}

/** PATCH /customers/{id} — atualização parcial. */
export async function updateCustomer(
  id: string,
  payload: CustomerUpdateRequest,
): Promise<CustomerResponse> {
  const { data } = await api.patch<CustomerResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /customers/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateCustomer(id: string): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /customers/{id}/activate — reativação. */
export async function activateCustomer(id: string): Promise<CustomerResponse> {
  const { data } = await api.patch<CustomerResponse>(
    `${BASE}/${id}/activate`,
  )
  return data
}
