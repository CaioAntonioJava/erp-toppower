import api from './client'
import type {
  CompanyCreateRequest,
  CompanyNextCodeResponse,
  CompanyResponse,
  CompanyUpdateRequest,
} from '../types/company'
import type { PagedResponse } from '../types/api'
import type { RegistrationStatus } from '../types/registration'

const BASE = '/api/v1/companies'

/** Quando `true`, este módulo delega para os mocks em `src/mocks/api/`. */
const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true'

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /companies — listagem paginada (status opcional). */
export async function listCompanies(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<CompanyResponse>> {
  if (USE_MOCKS) {
    const { listCompanies: mock } = await import('../mocks/api/company.api.mock')
    return mock(params)
  }
  const { data } = await api.get<PagedResponse<CompanyResponse>>(BASE, {
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
 * GET /companies/search — busca flexível.
 * Aceita query e/ou status. Sem nenhum filtro, retorna todos paginados.
 */
export async function searchCompanies(params: {
  query?: string
  status?: RegistrationStatus
  page?: number
  size?: number
}): Promise<PagedResponse<CompanyResponse>> {
  if (USE_MOCKS) {
    const { searchCompanies: mock } = await import('../mocks/api/company.api.mock')
    return mock(params)
  }
  const { data } = await api.get<PagedResponse<CompanyResponse>>(
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

/** GET /companies/{id} — detalhe. Requer ROLE_ADMIN no backend. */
export async function getCompany(id: string): Promise<CompanyResponse> {
  if (USE_MOCKS) {
    const { getCompany: mock } = await import('../mocks/api/company.api.mock')
    return mock(id)
  }
  const { data } = await api.get<CompanyResponse>(`${BASE}/${id}`)
  return data
}

/**
 * GET /companies/next-code — pré-visualiza o próximo código sequencial
 * (ex.: EMP000007) que seria atribuído à próxima empresa cadastrada.
 * Não persiste nada.
 */
export async function getNextCompanyCode(): Promise<string> {
  if (USE_MOCKS) {
    const { getNextCompanyCode: mock } = await import('../mocks/api/company.api.mock')
    return mock()
  }
  const { data } = await api.get<CompanyNextCodeResponse>(`${BASE}/next-code`)
  return data.code
}

/** POST /companies — cria uma nova empresa. */
export async function createCompany(
  payload: CompanyCreateRequest,
): Promise<CompanyResponse> {
  if (USE_MOCKS) {
    const { createCompany: mock } = await import('../mocks/api/company.api.mock')
    return mock(payload)
  }
  const { data } = await api.post<CompanyResponse>(BASE, payload)
  return data
}

/** PATCH /companies/{id} — atualização parcial. */
export async function updateCompany(
  id: string,
  payload: CompanyUpdateRequest,
): Promise<CompanyResponse> {
  if (USE_MOCKS) {
    const { updateCompany: mock } = await import('../mocks/api/company.api.mock')
    return mock(id, payload)
  }
  const { data } = await api.patch<CompanyResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /companies/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateCompany(id: string): Promise<void> {
  if (USE_MOCKS) {
    const { inactivateCompany: mock } = await import('../mocks/api/company.api.mock')
    return mock(id)
  }
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /companies/{id}/activate — reativação. */
export async function activateCompany(id: string): Promise<CompanyResponse> {
  if (USE_MOCKS) {
    const { activateCompany: mock } = await import('../mocks/api/company.api.mock')
    return mock(id)
  }
  const { data } = await api.patch<CompanyResponse>(`${BASE}/${id}/activate`)
  return data
}