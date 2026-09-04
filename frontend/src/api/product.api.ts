import api from './client'
import type {
  ProductCreateRequest,
  ProductResponse,
  ProductUpdateRequest,
} from '../types/product'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'

const BASE = '/api/v1/products'

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /products — listagem paginada (status opcional). */
export async function listProducts(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<ProductResponse>> {
  const { data } = await api.get<PagedResponse<ProductResponse>>(BASE, {
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
 * GET /products/search — busca flexível.
 * Aceita query e/ou status. Sem nenhum filtro, retorna todos paginados.
 * Backend exige no mínimo 2 caracteres quando `query` é informada.
 */
export async function searchProducts(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<ProductResponse>> {
  const { data } = await api.get<PagedResponse<ProductResponse>>(
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

/** GET /products/{id} — detalhe. */
export async function getProduct(id: number): Promise<ProductResponse> {
  const { data } = await api.get<ProductResponse>(`${BASE}/${id}`)
  return data
}

/** POST /products — cria um novo produto. */
export async function createProduct(
  payload: ProductCreateRequest,
): Promise<ProductResponse> {
  const { data } = await api.post<ProductResponse>(BASE, payload)
  return data
}

/** PATCH /products/{id} — atualização parcial. */
export async function updateProduct(
  id: number,
  payload: ProductUpdateRequest,
): Promise<ProductResponse> {
  const { data } = await api.patch<ProductResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /products/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateProduct(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}