/** Tipos do módulo de clientes (pessoas físicas). Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'
import type { Address } from './company'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus, Address }

/** Resposta de cliente PF. Espelha br.com.toppower...customer.dto.CustomerResponse. */
export interface CustomerResponse {
  uuid: string
  name: string
  email: string
  phone: string
  cpf: string
  code: string
  address: Address
  status: RegistrationStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/customers. */
export interface CustomerCreateRequest {
  name: string
  email: string
  phone: string
  cpf: string
  address: Address
  status?: RegistrationStatus
}

/** Resposta de GET /api/v1/customers/next-code. */
export interface CustomerNextCodeResponse {
  code: string
}

/** Corpo de PATCH /api/v1/customers/{id}. Campos opcionais. */
export interface CustomerUpdateRequest {
  name?: string
  email?: string
  phone?: string
  cpf?: string
  address?: Address
  status?: RegistrationStatus
}

/** Filtros suportados na listagem/busca de clientes. */
export interface CustomerFilters {
  query?: string
  status?: RegistrationStatus | 'ALL'
  page?: number
  size?: number
}
