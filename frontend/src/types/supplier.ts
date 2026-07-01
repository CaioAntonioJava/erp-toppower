/** Tipos do módulo de fornecedores (pessoas jurídicas). Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'
import type { Address } from './company'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus, Address }

/**
 * Resposta de fornecedor. Espelha br.com.toppower...supplier.dto.SupplierResponse.
 * Fornecedores não possuem `code` — o CNPJ (`taxId`) é o identificador fiscal.
 */
export interface SupplierResponse {
  uuid: string
  legalName: string
  tradeName: string | null
  /** CNPJ — imutável após o cadastro. */
  taxId: string
  stateRegistration: string | null
  municipalRegistration: string | null
  email: string
  phone: string | null
  contactName: string | null
  address: Address
  status: RegistrationStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/suppliers. */
export interface SupplierCreateRequest {
  legalName: string
  tradeName?: string
  taxId: string
  stateRegistration?: string
  municipalRegistration?: string
  email: string
  phone?: string
  contactName?: string
  address: Address
  status?: RegistrationStatus
}

/**
 * Corpo de PATCH /api/v1/suppliers/{id}. Campos opcionais.
 * O CNPJ (`taxId`) NÃO pode ser alterado — não enviar.
 */
export interface SupplierUpdateRequest {
  legalName?: string
  tradeName?: string
  stateRegistration?: string
  municipalRegistration?: string
  email?: string
  phone?: string
  contactName?: string
  address?: Address
  status?: RegistrationStatus
}

/** Filtros suportados na listagem/busca de fornecedores. */
export interface SupplierFilters {
  query?: string
  status?: RegistrationStatus | 'ALL'
  page?: number
  size?: number
}