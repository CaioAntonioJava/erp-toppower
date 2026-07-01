/** Tipos do módulo de vendedores (pessoas físicas). Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus }

/**
 * Resposta de vendedor. Espelha br.com.toppower...seller.dto.SellerResponse.
 * Vendedores não possuem endereço — apenas dados pessoais + percentual de comissão.
 */
export interface SellerResponse {
  uuid: string
  name: string
  email: string
  phone: string
  cpf: string
  /** Percentual de comissão (0,00% a 100,00%). */
  commissionRate: number | null
  status: RegistrationStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/sellers. */
export interface SellerCreateRequest {
  name: string
  email: string
  phone: string
  cpf: string
  commissionRate?: number | null
  status?: RegistrationStatus
}

/** Corpo de PATCH /api/v1/sellers/{id}. Campos opcionais. */
export interface SellerUpdateRequest {
  name?: string
  email?: string
  phone?: string
  cpf?: string
  commissionRate?: number | null
  status?: RegistrationStatus
}

/** Filtros suportados na listagem de vendedores. */
export interface SellerFilters {
  status?: RegistrationStatus | 'ALL'
  page?: number
  size?: number
}