/**
 * Mock das funções de `src/api/product.api.ts`.
 *
 * Apenas para desenvolvimento/teste manual. Ativado quando
 * `VITE_USE_MOCKS === 'true'` no .env.
 *
 * Backend de produtos NÃO tem endpoint /activate (apenas inativação),
 * então este mock espelha a mesma superfície: expõe apenas
 * `inactivateProduct`.
 */

import type {
  ProductCreateRequest,
  ProductResponse,
  ProductUpdateRequest,
} from '../../types/product'
import type { PagedResponse } from '../../types/api'
import type { RegistrationStatus } from '../../types/registration'
import { delay, mockError, pagedSlice } from './_helpers'
import { makeUuid, nowIso, store } from './store'

function matchesQuery(p: ProductResponse, q: string): boolean {
  return (
    p.name.toLowerCase().includes(q) ||
    (p.code?.toLowerCase().includes(q) ?? false)
  )
}

export async function listProducts(params: {
  page?: number
  size?: number
  status?: RegistrationStatus
}): Promise<PagedResponse<ProductResponse>> {
  await delay()
  return pagedSlice(store.products, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'name',
  })
}

export async function searchProducts(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<ProductResponse>> {
  await delay()
  // Backend exige no mínimo 2 caracteres quando query é informada — espelhamos aqui.
  if (params.query && params.query.length < 2) {
    return listProducts(params)
  }
  return pagedSlice(store.products, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'name',
    ...(params.query ? { query: params.query, queryFn: matchesQuery } : {}),
  })
}

export async function getProduct(id: string): Promise<ProductResponse> {
  await delay()
  const found = store.products.find((p) => p.uuid === id)
  if (!found) throw mockError(404, `Produto ${id} não encontrado.`)
  return found
}

export async function createProduct(payload: ProductCreateRequest): Promise<ProductResponse> {
  await delay()
  if (payload.code) {
    const dup = store.products.find((p) => p.code === payload.code)
    if (dup) throw mockError(409, `Já existe produto com o SKU ${payload.code}.`)
  }

  const created: ProductResponse = {
    uuid: makeUuid(),
    name: payload.name,
    code: payload.code ?? null,
    unitType: payload.unitType,
    status: payload.status ?? 'ATIVO',
    price: payload.price,
    stockQuantity: payload.stockQuantity,
    createdAt: nowIso(),
    updatedAt: nowIso(),
    createdBy: 'mock@toppower.local',
    updatedBy: null,
  }
  store.products.push(created)
  return created
}

export async function updateProduct(id: string, payload: ProductUpdateRequest): Promise<ProductResponse> {
  await delay()
  const idx = store.products.findIndex((p) => p.uuid === id)
  if (idx === -1) throw mockError(404, `Produto ${id} não encontrado.`)
  const current = store.products[idx]
  if (payload.code && payload.code !== current.code) {
    const dup = store.products.find((p) => p.uuid !== id && p.code === payload.code)
    if (dup) throw mockError(409, `Já existe produto com o SKU ${payload.code}.`)
  }
  const updated: ProductResponse = {
    ...current,
    name: payload.name ?? current.name,
    code: payload.code ?? current.code,
    unitType: payload.unitType ?? current.unitType,
    status: payload.status ?? current.status,
    price: payload.price ?? current.price,
    stockQuantity: payload.stockQuantity ?? current.stockQuantity,
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  store.products[idx] = updated
  return updated
}

export async function inactivateProduct(id: string): Promise<void> {
  await delay()
  const idx = store.products.findIndex((p) => p.uuid === id)
  if (idx === -1) throw mockError(404, `Produto ${id} não encontrado.`)
  store.products[idx] = {
    ...store.products[idx],
    status: 'INATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
}