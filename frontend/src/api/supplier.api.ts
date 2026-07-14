import api from './client'
import type {
  SupplierCreateRequest,
  SupplierResponse,
  SupplierUpdateRequest,
} from '../types/supplier'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'

const BASE = '/api/v1/suppliers'

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /suppliers — listagem paginada (status opcional). */
export async function listSuppliers(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<SupplierResponse>> {
  const { data } = await api.get<PagedResponse<SupplierResponse>>(BASE, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      sort: 'legalName,asc',
      status: params.status,
    },
  })
  return data
}

/**
 * GET /suppliers/search — busca flexível.
 * Aceita query e/ou status. Sem nenhum filtro, retorna todos paginados.
 */
export async function searchSuppliers(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<SupplierResponse>> {
  const { data } = await api.get<PagedResponse<SupplierResponse>>(
    `${BASE}/search`,
    {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: 'legalName,asc',
        query: params.query,
        status: params.status,
      },
    },
  )
  return data
}

/** GET /suppliers/{id} — detalhe. Requer ROLE_ADMIN no backend. */
export async function getSupplier(id: number): Promise<SupplierResponse> {
  const { data } = await api.get<SupplierResponse>(`${BASE}/${id}`)
  return data
}

/** POST /suppliers — cria um novo fornecedor. */
export async function createSupplier(
  payload: SupplierCreateRequest,
): Promise<SupplierResponse> {
  const { data } = await api.post<SupplierResponse>(BASE, payload)
  return data
}

/** PATCH /suppliers/{id} — atualização parcial. CNPJ não pode ser alterado. */
export async function updateSupplier(
  id: number,
  payload: SupplierUpdateRequest,
): Promise<SupplierResponse> {
  const { data } = await api.patch<SupplierResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /suppliers/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateSupplier(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /suppliers/{id}/activate — reativação. */
export async function activateSupplier(id: number): Promise<SupplierResponse> {
  const { data } = await api.patch<SupplierResponse>(
    `${BASE}/${id}/activate`,
  )
  return data
}