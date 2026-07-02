/** Tipos do módulo de empresas (pessoas jurídicas). Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus }

/** Endereço embutido em Company, Customer, Supplier, etc. */
export interface Address {
  street: string
  number: string
  complement?: string
  neighborhood?: string
  city: string
  state: string
  zipCode: string
}

/** Resposta de empresa. Espelha br.com.toppower...company.dto.CompanyResponse. */
export interface CompanyResponse {
  uuid: string
  legalName: string
  tradeName: string | null
  code: string
  cnpj: string
  stateRegistration: string | null
  municipalRegistration: string | null
  address: Address
  status: RegistrationStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/companies. */
export interface CompanyCreateRequest {
  legalName: string
  tradeName?: string
  cnpj: string
  stateRegistration?: string
  municipalRegistration?: string
  address: Address
  status?: RegistrationStatus
}

/** Resposta de GET /api/v1/companies/next-code. */
export interface CompanyNextCodeResponse {
  code: string
}

/** Corpo de PATCH /api/v1/companies/{id}. Campos opcionais. */
export interface CompanyUpdateRequest {
  legalName?: string
  tradeName?: string
  stateRegistration?: string
  municipalRegistration?: string
  address?: Address
  status?: RegistrationStatus
}

/** Filtros suportados na listagem/busca de empresas. */
export interface CompanyFilters {
  query?: string
  status?: RegistrationStatus | 'ALL'
  page?: number
  size?: number
}
