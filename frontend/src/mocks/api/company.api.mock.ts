/**
 * Mock das funções de `src/api/company.api.ts`.
 *
 * Apenas para desenvolvimento/teste manual. Ativado quando
 * `VITE_USE_MOCKS === 'true'` no .env. Mantém a mesma assinatura
 * dos exports reais para que o dispatch no `company.api.ts` seja transparente.
 */

import type {
  CompanyCreateRequest,
  CompanyResponse,
  CompanyUpdateRequest,
} from '../../types/company'
import type { PagedResponse } from '../../types/api'
import type { RegistrationStatus } from '../../types/registration'
import { delay, mockError, nextSequentialCode, pagedSlice } from './_helpers'
import { makeUuid, nowIso, store } from './store'

const EMP_PREFIX = 'EMP'

function matchesQuery(c: CompanyResponse, q: string): boolean {
  return (
    c.legalName.toLowerCase().includes(q) ||
    (c.tradeName?.toLowerCase().includes(q) ?? false) ||
    c.code.toLowerCase().includes(q) ||
    c.cnpj.toLowerCase().includes(q) ||
    (c.stateRegistration?.toLowerCase().includes(q) ?? false)
  )
}

export async function listCompanies(params: {
  page?: number
  size?: number
  status?: RegistrationStatus
}): Promise<PagedResponse<CompanyResponse>> {
  await delay()
  return pagedSlice(store.companies, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'legalName',
  })
}

export async function searchCompanies(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<CompanyResponse>> {
  await delay()
  return pagedSlice(store.companies, {
    page: params.page,
    size: params.size,
    status: params.status,
    sortBy: 'legalName',
    ...(params.query ? { query: params.query, queryFn: matchesQuery } : {}),
  })
}

export async function getCompany(id: string): Promise<CompanyResponse> {
  await delay()
  const found = store.companies.find((c) => c.uuid === id)
  if (!found) throw mockError(404, `Empresa ${id} não encontrada.`)
  return found
}

export async function getNextCompanyCode(): Promise<string> {
  await delay()
  return nextSequentialCode(
    EMP_PREFIX,
    store.companies.map((c) => c.code),
  )
}

export async function createCompany(payload: CompanyCreateRequest): Promise<CompanyResponse> {
  await delay()

  // Duplicidade de CNPJ — emula o que o backend faria.
  const cnpjDigits = payload.cnpj.replace(/\D/g, '')
  const dup = store.companies.find((c) => c.cnpj.replace(/\D/g, '') === cnpjDigits)
  if (dup) throw mockError(409, `Já existe empresa cadastrada com o CNPJ ${payload.cnpj}.`)

  const created: CompanyResponse = {
    uuid: makeUuid(),
    legalName: payload.legalName,
    tradeName: payload.tradeName ?? null,
    code: nextSequentialCode(
      EMP_PREFIX,
      store.companies.map((c) => c.code),
    ),
    cnpj: payload.cnpj,
    stateRegistration: payload.stateRegistrationExempt ? null : payload.stateRegistration ?? null,
    stateRegistrationExempt: payload.stateRegistrationExempt ?? false,
    municipalRegistration: payload.municipalRegistration ?? null,
    address: payload.address,
    status: payload.status ?? 'ATIVO',
    createdAt: nowIso(),
    updatedAt: nowIso(),
    createdBy: 'mock@toppower.local',
    updatedBy: null,
  }
  store.companies.push(created)
  return created
}

export async function updateCompany(id: string, payload: CompanyUpdateRequest): Promise<CompanyResponse> {
  await delay()
  const idx = store.companies.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Empresa ${id} não encontrada.`)
  const current = store.companies[idx]
  const updated: CompanyResponse = {
    ...current,
    legalName: payload.legalName ?? current.legalName,
    tradeName: payload.tradeName ?? current.tradeName,
    stateRegistration:
      payload.stateRegistrationExempt === true
        ? null
        : (payload.stateRegistration ?? current.stateRegistration),
    stateRegistrationExempt:
      payload.stateRegistrationExempt ?? current.stateRegistrationExempt,
    municipalRegistration: payload.municipalRegistration ?? current.municipalRegistration,
    address: payload.address ?? current.address,
    status: payload.status ?? current.status,
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  store.companies[idx] = updated
  return updated
}

export async function inactivateCompany(id: string): Promise<void> {
  await delay()
  const idx = store.companies.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Empresa ${id} não encontrada.`)
  store.companies[idx] = {
    ...store.companies[idx],
    status: 'INATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
}

export async function activateCompany(id: string): Promise<CompanyResponse> {
  await delay()
  const idx = store.companies.findIndex((c) => c.uuid === id)
  if (idx === -1) throw mockError(404, `Empresa ${id} não encontrada.`)
  store.companies[idx] = {
    ...store.companies[idx],
    status: 'ATIVO',
    updatedAt: nowIso(),
    updatedBy: 'mock@toppower.local',
  }
  return store.companies[idx]
}