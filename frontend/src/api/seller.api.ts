import api from './client'
import type {
  SellerCreateRequest,
  SellerResponse,
  SellerUpdateRequest,
} from '../types/seller'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'

const BASE = '/api/v1/sellers'

/** Parâmetros comuns para listagem. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /sellers — listagem paginada (status opcional). */
export async function listSellers(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<SellerResponse>> {
  const { data } = await api.get<PagedResponse<SellerResponse>>(BASE, {
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
 * O backend de vendedores NÃO expõe endpoint /search. Esta função existe
 * apenas para satisfazer o contrato do hook `useEntityList` e delega para
 * `listSellers`, ignorando o termo de busca. A página de lista não
 * renderiza o input de busca para refletir a ausência do recurso no backend.
 */
export async function searchSellers(_params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<SellerResponse>> {
  return listSellers({
    status: _params.status,
    page: _params.page,
    size: _params.size,
  })
}

/** GET /sellers/{id} — detalhe. Requer ROLE_ADMIN no backend. */
export async function getSeller(id: number): Promise<SellerResponse> {
  const { data } = await api.get<SellerResponse>(`${BASE}/${id}`)
  return data
}

/** POST /sellers — cria um novo vendedor. */
export async function createSeller(
  payload: SellerCreateRequest,
): Promise<SellerResponse> {
  const { data } = await api.post<SellerResponse>(BASE, payload)
  return data
}

/** PATCH /sellers/{id} — atualização parcial. */
export async function updateSeller(
  id: number,
  payload: SellerUpdateRequest,
): Promise<SellerResponse> {
  const { data } = await api.patch<SellerResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /sellers/{id} — inativação (soft delete). Retorna 204. Requer ROLE_ADMIN. */
export async function inactivateSeller(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /sellers/{id}/activate — reativação. Requer ROLE_ADMIN. */
export async function activateSeller(id: number): Promise<SellerResponse> {
  const { data } = await api.patch<SellerResponse>(
    `${BASE}/${id}/activate`,
  )
  return data
}