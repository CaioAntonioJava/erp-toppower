/** Tipos do módulo de transportadoras. Espelham os DTOs do backend. */

// =====================================================================
// Enums
// =====================================================================

/** Nome padronizado da transportadora. Espelha CarrierName no backend. */
export type CarrierName =
  | 'CORREIOS_SEDEX'
  | 'CORREIOS_PAC'
  | 'JADLOG'
  | 'OUTRAS_TRANSPORTADORAS'

/** Rótulos em português para cada `CarrierName`. */
export const CARRIER_NAME_LABELS: Record<CarrierName, string> = {
  CORREIOS_SEDEX: 'Correios SEDEX',
  CORREIOS_PAC: 'Correios PAC',
  JADLOG: 'JadLog',
  OUTRAS_TRANSPORTADORAS: 'Outras Transportadoras',
}

/** Status da transportadora no cadastro. Espelha CarrierStatus no backend. */
export type CarrierStatus = 'ATIVO' | 'INATIVO'

// =====================================================================
// DTOs
// =====================================================================

/**
 * Resposta de transportadora. Espelha
 * br.com.toppower...carrier.dto.CarrierResponse.
 */
export interface CarrierResponse {
  uuid: string
  carrierName: CarrierName | null
  freightValue: number | null
  status: CarrierStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/carriers. Espelha CarrierCreateRequest. */
export interface CarrierCreateRequest {
  carrierName?: CarrierName | null
  freightValue?: number | null
  status?: CarrierStatus
}

/** Corpo de PATCH /api/v1/carriers/{id}. Campos opcionais. */
export interface CarrierUpdateRequest {
  carrierName?: CarrierName | null
  freightValue?: number | null
  status?: CarrierStatus
}

/** Filtros suportados na listagem/busca de transportadoras. */
export interface CarrierFilters {
  carrierName?: CarrierName
  status?: CarrierStatus
  page?: number
  size?: number
}