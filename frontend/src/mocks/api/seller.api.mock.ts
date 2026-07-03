/**
 * Mock das funções de `src/api/seller.api.ts`.
 *
 * Apenas para desenvolvimento/teste manual. Ativado quando
 * `VITE_USE_MOCKS === 'true'` no .env.
 *
 * Observação: o backend de vendedores NÃO expõe /search (vide
 * `seller.api.ts::searchSellers`). O mock aqui também não — `searchSellers`
 * delega para `listSellers` igual à implementação real.
 */

import type {
  SellerCreateRequest,
  SellerResponse,
  SellerUpdateRequest,
} from '../../types/seller'
import type { PagedResponse } from '../../types/api'
import type { RegistrationStatus } from '../../types/registration'
import { delay, mockError, pagedSlice } from './_helpers'
import { makeUuid, nowIso, store } from './store'

export async function listSellers(params: {
  page?: number
  size?: number
  status?: RegistrationStatus
}): Promise<PagedResponse<SellerResponse>> {
  await delay()
  return pagedSlice(store.sellers, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'name',
  })
}

export async function searchSellers(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<SellerResponse>> {
  // Igual à impl real: ignora `query` e delega para listSellers.
  await delay()
  return listSellers(params)
}

export async function getSeller(id: string): Promise<SellerResponse> {
  await delay()
  const found = store.sellers.find((s) => s.uuid === id)
  if (!found) throw mockError(404, `Vendedor ${id} não encontrado.`)
  return found
}

export async function createSeller(payload: SellerCreateRequest): Promise<SellerResponse> {
  await delay()
  const cpfDigits = payload.cpf.replace(/\D/g, '')
  const dup = store.sellers.find((s) => s.cpf.replace(/\D/g, '') === cpfDigits)
  if (dup) throw mockError(409, `Já existe vendedor cadastrado com o CPF ${payload.cpf}.`)

  const created: SellerResponse = {
    uuid: makeUuid(),
    name: payload.name,
    email: payload.email,
    phone: payload.phone,
    cpf: payload.cpf,
    commissionRate: payload.commissionRate ?? null,
    status: payload.status ?? 'ATIVO',
    createdAt: nowIso(),
    updatedAt: nowIso(),
    createdBy: 'mock@toppower.local',
    updatedBy: null,
  }
  store.sellers.push(created)
  return created
}

export async function updateSeller(id: string, payload: SellerUpdateRequest): Promise<SellerResponse> {
  await delay()
  const idx = store.sellers.findIndex((s) => s.uuid === id)
  if (idx === -1) throw mockError(404, `Vendedor ${id} não encontrado.`)
  const current = store.sellers[idx]
  const updated: SellerResponse = {
    ...current,
    name: payload.name ?? current.name,
    email: payload.email ?? current.email,
    phone: payload.phone ?? current.phone,
    cpf: payload.cpf ?? current.cpf,
    commissionRate:
      payload.commissionRate !== undefined ? payload.commissionRate : current.commissionRate,
    status: payload.status ?? current.status,
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  store.sellers[idx] = updated
  return updated
}

export async function inactivateSeller(id: string): Promise<void> {
  await delay()
  const idx = store.sellers.findIndex((s) => s.uuid === id)
  if (idx === -1) throw mockError(404, `Vendedor ${id} não encontrado.`)
  store.sellers[idx] = {
    ...store.sellers[idx],
    status: 'INATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
}

export async function activateSeller(id: string): Promise<SellerResponse> {
  await delay()
  const idx = store.sellers.findIndex((s) => s.uuid === id)
  if (idx === -1) throw mockError(404, `Vendedor ${id} não encontrado.`)
  store.sellers[idx] = {
    ...store.sellers[idx],
    status: 'ATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  return store.sellers[idx]
}