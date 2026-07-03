import api from './client'
import type {
  CustomerCreateRequest,
  CustomerNextCodeResponse,
  CustomerResponse,
  CustomerUpdateRequest,
} from '../types/customer'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'

const BASE = '/api/v1/customers'

/** Quando `true`, este módulo delega para os mocks em `src/mocks/api/`. */
const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true'

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /customers — listagem paginada (status opcional). */
export async function listCustomers(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<CustomerResponse>> {
  if (USE_MOCKS) {
    const { listCustomers: mock } = await import('../mocks/api/customer.api.mock')
    return mock(params)
  }
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
  if (USE_MOCKS) {
    const { searchCustomers: mock } = await import('../mocks/api/customer.api.mock')
    return mock(params)
  }
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
  if (USE_MOCKS) {
    const { getCustomer: mock } = await import('../mocks/api/customer.api.mock')
    return mock(id)
  }
  const { data } = await api.get<CustomerResponse>(`${BASE}/${id}`)
  return data
}

/**
 * GET /customers/next-code — pré-visualiza o próximo código sequencial
 * (ex.: CLI000007) que seria atribuído ao próximo cliente cadastrado.
 * Não persiste nada.
 */
export async function getNextCustomerCode(): Promise<string> {
  if (USE_MOCKS) {
    const { getNextCustomerCode: mock } = await import('../mocks/api/customer.api.mock')
    return mock()
  }
  const { data } = await api.get<CustomerNextCodeResponse>(`${BASE}/next-code`)
  return data.code
}

/** POST /customers — cria um novo cliente. */
export async function createCustomer(
  payload: CustomerCreateRequest,
): Promise<CustomerResponse> {
  if (USE_MOCKS) {
    const { createCustomer: mock } = await import('../mocks/api/customer.api.mock')
    return mock(payload)
  }
  const { data } = await api.post<CustomerResponse>(BASE, payload)
  return data
}

/** PATCH /customers/{id} — atualização parcial. */
export async function updateCustomer(
  id: string,
  payload: CustomerUpdateRequest,
): Promise<CustomerResponse> {
  if (USE_MOCKS) {
    const { updateCustomer: mock } = await import('../mocks/api/customer.api.mock')
    return mock(id, payload)
  }
  const { data } = await api.patch<CustomerResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /customers/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateCustomer(id: string): Promise<void> {
  if (USE_MOCKS) {
    const { inactivateCustomer: mock } = await import('../mocks/api/customer.api.mock')
    return mock(id)
  }
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /customers/{id}/activate — reativação. */
export async function activateCustomer(id: string): Promise<CustomerResponse> {
  if (USE_MOCKS) {
    const { activateCustomer: mock } = await import('../mocks/api/customer.api.mock')
    return mock(id)
  }
  const { data } = await api.patch<CustomerResponse>(
    `${BASE}/${id}/activate`,
  )
  return data
}