/** Tipos do módulo de produtos. Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus }

/**
 * Status do produto no cadastro. Os valores são idênticos ao
 * `RegistrationStatus` compartilhado, mas mantemos o nome para refletir
 * a semântica do módulo de produtos.
 */
export type ProductStatus = RegistrationStatus

/**
 * Unidade de medida em que o produto é comercializado/controlado.
 * Espelha br.com.toppower...product.enums.UnitType.
 */
export type UnitType = 'UNIDADE' | 'METROS' | 'BOBINA' | 'PECAS' | 'QUILOS' | 'ROLO'

/** Rótulos em português para cada `UnitType`. */
export const UNIT_TYPE_LABELS: Record<UnitType, string> = {
  UNIDADE: 'Unidade',
  METROS: 'Metros',
  BOBINA: 'Bobina',
  PECAS: 'Peças',
  QUILOS: 'Quilos',
  ROLO: 'Rolo',
}

/** Opções prontas para uso com o componente `Select`. */
export const UNIT_TYPE_OPTIONS: ReadonlyArray<{ value: UnitType; label: string }> =
  (Object.keys(UNIT_TYPE_LABELS) as UnitType[]).map((value) => ({
    value,
    label: UNIT_TYPE_LABELS[value],
  }))

/**
 * Resposta de produto. Espelha br.com.toppower...product.dto.ProductResponse.
 */
export interface ProductResponse {
  uuid: string
  name: string
  /**
   * SKU — único quando informado. Opcional no backend: produtos sem SKU
   * vêm com `code = null` (a coluna aceita NULL e a constraint única
   * do banco ignora nulos).
   */
  code: string | null
  unitType: UnitType
  status: ProductStatus
  /** Preço unitário de venda. */
  price: number
  /** Quantidade em estoque (permite fracionamento para METROS/BOBINA). */
  stockQuantity: number
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/**
 * Corpo de POST /api/v1/products.
 * `code` é opcional: omitir (ou enviar `undefined`) cadastra o produto
 * sem SKU. Não envie string vazia — o `@Pattern` do backend rejeita.
 */
export interface ProductCreateRequest {
  name: string
  code?: string
  unitType: UnitType
  status?: ProductStatus
  /** Preço unitário (> 0). */
  price: number
  /** Quantidade em estoque (>= 0). */
  stockQuantity: number
}

/** Corpo de PATCH /api/v1/products/{id}. Campos opcionais. */
export interface ProductUpdateRequest {
  name?: string
  code?: string
  unitType?: UnitType
  status?: ProductStatus
  price?: number
  stockQuantity?: number
}

/** Filtros suportados na listagem/busca de produtos. */
export interface ProductFilters {
  query?: string
  status?: ProductStatus | 'ALL'
  page?: number
  size?: number
}