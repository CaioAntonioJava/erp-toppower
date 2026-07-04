/**
 * Mock das funções de `src/api/carrier.api.ts`.
 *
 * Apenas para desenvolvimento/teste manual. Ativado quando
 * `VITE_USE_MOCKS === 'true'` no .env.
 */

import type {
  CarrierCreateRequest,
  CarrierName,
  CarrierResponse,
  CarrierStatus,
  CarrierUpdateRequest,
} from '../../types/carrier'
import type { PagedResponse } from '../../types/api'
import { delay, mockError, pagedSlice } from './_helpers'
import { makeUuid, nowIso, store } from './store'

export async function listCarriers(params: {
  page?: number
  size?: number
  status?: CarrierStatus
}): Promise<PagedResponse<CarrierResponse>> {
  await delay()
  return pagedSlice(store.carriers, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'carrierName',
  })
}

export async function searchCarriers(params: {
  carrierName?: CarrierName
  status?: CarrierStatus
  page?: number
  size?: number
}): Promise<PagedResponse<CarrierResponse>> {
  await delay()
  let filtered = store.carriers.slice()
  if (params.carrierName) {
    filtered = filtered.filter((c) => c.carrierName === params.carrierName)
  }
  return pagedSlice(filtered, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'carrierName',
  })
}

export async function getCarrier(id: string): Promise<CarrierResponse> {
  await delay()
  const found = store.carriers.find((c) => c.uuid === id)
  if (!found) throw mockError(404, `Transportadora ${id} não encontrada.`)
  return found
}

export async function createCarrier(
  payload: CarrierCreateRequest,
): Promise<CarrierResponse> {
  await delay()
  const created: CarrierResponse = {
    uuid: makeUuid(),
    carrierName: payload.carrierName ?? null,
    freightValue: payload.freightValue ?? null,
    status: payload.status ?? 'ATIVO',
    createdAt: nowIso(),
    updatedAt: nowIso(),
    createdBy: 'mock@toppower.local',
    updatedBy: null,
  }
  store.carriers.push(created)
  return created
}

export async function updateCarrier(
  id: string,
  payload: CarrierUpdateRequest,
): Promise<CarrierResponse> {
  await delay()
  const idx = store.carriers.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Transportadora ${id} não encontrada.`)
  const current = store.carriers[idx]
  const updated: CarrierResponse = {
    ...current,
    carrierName:
      payload.carrierName !== undefined ? payload.carrierName : current.carrierName,
    freightValue:
      payload.freightValue !== undefined ? payload.freightValue : current.freightValue,
    status: payload.status ?? current.status,
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  store.carriers[idx] = updated
  return updated
}

export async function inactivateCarrier(id: string): Promise<void> {
  await delay()
  const idx = store.carriers.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Transportadora ${id} não encontrada.`)
  store.carriers[idx] = {
    ...store.carriers[idx],
    status: 'INATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
}

export async function activateCarrier(id: string): Promise<CarrierResponse> {
  await delay()
  const idx = store.carriers.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Transportadora ${id} não encontrada.`)
  store.carriers[idx] = {
    ...store.carriers[idx],
    status: 'ATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  return store.carriers[idx]
}