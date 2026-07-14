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

/** Parâmetros comuns para listagem e busca. */
interface PageParams {
  page?: number
  size?: number
}

/** GET /companies — listagem paginada (status opcional). */
export async function listCompanies(
  params: PageParams & { status?: RegistrationStatus },
): Promise<PagedResponse<CompanyResponse>> {
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

/** GET /companies/{id} — detalhe. Disponível para ADMIN e MANAGER no backend. */
export async function getCompany(id: number): Promise<CompanyResponse> {
  const { data } = await api.get<CompanyResponse>(`${BASE}/${id}`)
  return data
}

/**
 * GET /companies/next-code — pré-visualiza o próximo código sequencial
 * (ex.: EMP000007) que seria atribuído à próxima empresa cadastrada.
 * Não persiste nada.
 */
export async function getNextCompanyCode(): Promise<string> {
  const { data } = await api.get<CompanyNextCodeResponse>(`${BASE}/next-code`)
  return data.code
}

/** POST /companies — cria uma nova empresa. */
export async function createCompany(
  payload: CompanyCreateRequest,
): Promise<CompanyResponse> {
  const { data } = await api.post<CompanyResponse>(BASE, payload)
  return data
}

/** PATCH /companies/{id} — atualização parcial. */
export async function updateCompany(
  id: number,
  payload: CompanyUpdateRequest,
): Promise<CompanyResponse> {
  const { data } = await api.patch<CompanyResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /companies/{id} — inativação (soft delete). Retorna 204. */
export async function inactivateCompany(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /companies/{id}/activate — reativação. */
export async function activateCompany(id: number): Promise<CompanyResponse> {
  const { data } = await api.patch<CompanyResponse>(`${BASE}/${id}/activate`)
  return data
}