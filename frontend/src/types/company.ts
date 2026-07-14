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
  zipCode?: string
}

/** Resposta de empresa. Espelha br.com.toppower...company.dto.CompanyResponse. */
export interface CompanyResponse {
  id: number
  legalName: string
  tradeName: string | null
  code: string
  cnpj: string
  stateRegistration: string | null
  /**
   * Indica se a empresa é ISENTA de Inscrição Estadual. Quando true,
   * a `stateRegistration` deve ser nula (empresa dispensada de possuir
   * IE — caso comum de MEIs e prestadores de serviço).
   */
  stateRegistrationExempt: boolean
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
  /**
   * Se a empresa é isenta de IE. Quando omitido no create, o backend
   * assume `false` (não isenta). Para marcar como isenta, envie `true`.
   */
  stateRegistrationExempt?: boolean
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
  /**
   * Atualiza a flag de isenção de IE. Envie `true` para marcar como
   * isenta, `false` para desmarcar. Quando omitido no update, o valor
   * atual é preservado.
   */
  stateRegistrationExempt?: boolean
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
