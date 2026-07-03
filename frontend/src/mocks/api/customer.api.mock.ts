/**
 * Mock das funções de `src/api/customer.api.ts`.
 *
 * Apenas para desenvolvimento/teste manual. Ativado quando
 * `VITE_USE_MOCKS === 'true'` no .env.
 */

import type {
  CustomerCreateRequest,
  CustomerResponse,
  CustomerUpdateRequest,
} from '../../types/customer'
import type { PagedResponse } from '../../types/api'
import type { RegistrationStatus } from '../../types/registration'
import { delay, mockError, nextSequentialCode, pagedSlice } from './_helpers'
import { makeUuid, nowIso, store } from './store'

const CLI_PREFIX = 'CLI'

function matchesQuery(c: CustomerResponse, q: string): boolean {
  return (
    c.name.toLowerCase().includes(q) ||
    c.email.toLowerCase().includes(q) ||
    c.code.toLowerCase().includes(q) ||
    c.cpf.toLowerCase().includes(q) ||
    c.phone.toLowerCase().includes(q)
  )
}

export async function listCustomers(params: {
  page?: number
  size?: number
  status?: RegistrationStatus
}): Promise<PagedResponse<CustomerResponse>> {
  await delay()
  return pagedSlice(store.customers, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'name',
  })
}

export async function searchCustomers(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<CustomerResponse>> {
  await delay()
  return pagedSlice(store.customers, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'name',
    ...(params.query ? { query: params.query, queryFn: matchesQuery } : {}),
  })
}

export async function getCustomer(id: string): Promise<CustomerResponse> {
  await delay()
  const found = store.customers.find((c) => c.uuid === id)
  if (!found) throw mockError(404, `Cliente ${id} não encontrado.`)
  return found
}

export async function getNextCustomerCode(): Promise<string> {
  await delay()
  return nextSequentialCode(
    CLI_PREFIX,
    store.customers.map((c) => c.code),
  )
}

export async function createCustomer(payload: CustomerCreateRequest): Promise<CustomerResponse> {
  await delay()
  const cpfDigits = payload.cpf.replace(/\D/g, '')
  const dup = store.customers.find((c) => c.cpf.replace(/\D/g, '') === cpfDigits)
  if (dup) throw mockError(409, `Já existe cliente cadastrado com o CPF ${payload.cpf}.`)

  const created: CustomerResponse = {
    uuid: makeUuid(),
    name: payload.name,
    email: payload.email,
    phone: payload.phone,
    cpf: payload.cpf,
    code: nextSequentialCode(
      CLI_PREFIX,
      store.customers.map((c) => c.code),
    ),
    address: payload.address,
    status: payload.status ?? 'ATIVO',
    createdAt: nowIso(),
    updatedAt: nowIso(),
    createdBy: 'mock@toppower.local',
    updatedBy: null,
  }
  store.customers.push(created)
  return created
}

export async function updateCustomer(id: string, payload: CustomerUpdateRequest): Promise<CustomerResponse> {
  await delay()
  const idx = store.customers.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Cliente ${id} não encontrado.`)
  const current = store.customers[idx]
  const updated: CustomerResponse = {
    ...current,
    name: payload.name ?? current.name,
    email: payload.email ?? current.email,
    phone: payload.phone ?? current.phone,
    cpf: payload.cpf ?? current.cpf,
    address: payload.address ?? current.address,
    status: payload.status ?? current.status,
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  store.customers[idx] = updated
  return updated
}

export async function inactivateCustomer(id: string): Promise<void> {
  await delay()
  const idx = store.customers.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Cliente ${id} não encontrado.`)
  store.customers[idx] = {
    ...store.customers[idx],
    status: 'INATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
}

export async function activateCustomer(id: string): Promise<CustomerResponse> {
  await delay()
  const idx = store.customers.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Cliente ${id} não encontrado.`)
  store.customers[idx] = {
    ...store.customers[idx],
    status: 'ATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  return store.customers[idx]
}