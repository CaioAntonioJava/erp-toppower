/** Tipos do módulo de produtos. Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus }

/**
 * Status do produto no cadastro. Os valores são idênticos ao
 * `RegistrationStatus` compartilhado, mas mantemos o nome para refletir a
 * semântica do módulo de produtos.
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
 * Origem da mercadoria (campo `orig` da NF-e). Espelha
 * br.com.toppower...product.enums.OrigemProduto.
 */
export type OrigemProduto =
  | 'NACIONAL'
  | 'ESTRANGEIRA_IMPORTACAO_DIRETA'
  | 'ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO'
  | 'NACIONAL_IMPORTACAO_SUPERIOR_40'
  | 'NACIONAL_PROCESSOS_PRODUTIVOS_BASICOS'
  | 'NACIONAL_IMPORTACAO_SUPERIOR_70'
  | 'ESTRANGEIRA_IMPORTACAO_DIRETA_SEM_SIMILAR'
  | 'ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO_SEM_SIMILAR'
  | 'NACIONAL_IMPORTACAO_ACIMA_70'

/** Rótulos em português para cada `OrigemProduto`. */
export const ORIGEM_LABELS: Record<OrigemProduto, string> = {
  NACIONAL: 'Nacional',
  ESTRANGEIRA_IMPORTACAO_DIRETA: 'Estrangeira — Importação direta',
  ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO: 'Estrangeira — Adquirida no mercado interno',
  NACIONAL_IMPORTACAO_SUPERIOR_40: 'Nacional — Conteúdo de importação > 40% e <= 70%',
  NACIONAL_PROCESSOS_PRODUTIVOS_BASICOS: 'Nacional — Processos Produtivos Básicos (PPB)',
  NACIONAL_IMPORTACAO_SUPERIOR_70: 'Nacional — Conteúdo de importação <= 40%',
  ESTRANGEIRA_IMPORTACAO_DIRETA_SEM_SIMILAR: 'Estrangeira — Importação direta sem similar nacional',
  ESTRANGEIRA_ADQUIRIDA_MERCADO_INTERNO_SEM_SIMILAR:
    'Estrangeira — Adquirida no mercado interno sem similar nacional',
  NACIONAL_IMPORTACAO_ACIMA_70: 'Nacional — Conteúdo de importação > 70%',
}

/** Opções prontas para uso com o componente `Select`. */
export const ORIGEM_OPTIONS: ReadonlyArray<{ value: OrigemProduto; label: string }> =
  (Object.keys(ORIGEM_LABELS) as OrigemProduto[]).map((value) => ({
    value,
    label: ORIGEM_LABELS[value],
  }))

/**
 * CSOSN — Código de Situação da Operação no Simples Nacional.
 * Lista os códigos mais comuns; o backend valida o formato (3 dígitos).
 */
export const CSOSN_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
  { value: '101', label: '101 — Tributada com permissão de crédito' },
  { value: '102', label: '102 — Tributada sem permissão de crédito' },
  { value: '103', label: '103 — Isenção por faixa de receita' },
  { value: '300', label: '300 — Imune' },
  { value: '400', label: '400 — Não tributada' },
  { value: '500', label: '500 — Substituição tributária' },
  { value: '900', label: '900 — Outros' },
]

/** CST do IPI — valores típicos do Simples Nacional. */
export const CST_IPI_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
  { value: '00', label: '00 — Entrada com recuperação de crédito' },
  { value: '01', label: '01 — Entrada tributada com alíquota zero' },
  { value: '04', label: '04 — Entrada não tributada' },
  { value: '05', label: '05 — Entrada com suspensão' },
  { value: '49', label: '49 — Outras entradas' },
  { value: '50', label: '50 — Saída tributada' },
  { value: '51', label: '51 — Saída tributada com alíquota zero' },
  { value: '54', label: '54 — Saída não tributada' },
  { value: '55', label: '55 — Saída com suspensão' },
  { value: '99', label: '99 — Outras saídas (Simples Nacional)' },
]

/** CST do PIS — valores típicos do Simples Nacional. */
export const CST_PIS_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
  { value: '01', label: '01 — Operação tributável com alíquota básica' },
  { value: '02', label: '02 — Operação tributável com alíquota diferenciada' },
  { value: '04', label: '04 — Operação tributável monofásica' },
  { value: '05', label: '05 — Operação tributável por substituição tributária' },
  { value: '06', label: '06 — Operação tributável a alíquota zero' },
  { value: '07', label: '07 — Operação isenta da contribuição' },
  { value: '08', label: '08 — Operação sem incidência da contribuição' },
  { value: '09', label: '09 — Operação com suspensão da contribuição' },
  { value: '49', label: '49 — Outras operações de saída (Simples Nacional)' },
]

/** CST do COFINS — valores típicos do Simples Nacional. */
export const CST_COFINS_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
  { value: '01', label: '01 — Operação tributável com alíquota básica' },
  { value: '02', label: '02 — Operação tributável com alíquota diferenciada' },
  { value: '04', label: '04 — Operação tributável monofásica' },
  { value: '05', label: '05 — Operação tributável por substituição tributária' },
  { value: '06', label: '06 — Operação tributável a alíquota zero' },
  { value: '07', label: '07 — Operação isenta da contribuição' },
  { value: '08', label: '08 — Operação sem incidência da contribuição' },
  { value: '09', label: '09 — Operação com suspensão da contribuição' },
  { value: '49', label: '49 — Outras operações de saída (Simples Nacional)' },
]

/**
 * Resposta de produto. Espelha br.com.toppower...product.dto.ProductResponse.
 */
export interface ProductResponse {
  id: number
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
  // ----- Campos fiscais (Simples Nacional) -----
  /** NCM — Nomenclatura Comum do Mercosul (8 dígitos). Pode ser nulo em produtos legados. */
  ncm: string | null
  /** Origem da mercadoria (campo orig da NF-e). */
  origem: OrigemProduto | null
  /** Código de barras / GTIN (EAN-13/14). */
  codigoBarras: string | null
  /** CEST — Código Especificador da Substituição Tributária (7 dígitos). */
  cest: string | null
  /** EX TIPI — Exceção da TIPI (2 dígitos). */
  exTipi: string | null
  /** Peso líquido em kg. */
  pesoLiquido: number | null
  /** Peso bruto em kg. */
  pesoBruto: number | null
  /** CSOSN — Código de Situação da Operação no Simples Nacional (3 dígitos). */
  csosn: string | null
  /** Alíquota do ICMS-ST (%). */
  aliquotaIcmsSt: number | null
  /** MVA (Margem de Valor Adicionado) para ST (%). */
  mvaSt: number | null
  /** CST do IPI (2 dígitos). */
  cstIpi: string | null
  /** Classe de enquadramento do IPI (5 dígitos). */
  classeEnqIpi: string | null
  /** CST do PIS (2 dígitos). */
  cstPis: string | null
  /** CST do COFINS (2 dígitos). */
  cstCofins: string | null
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/**
 * Corpo de POST /api/v1/products.
 * `code` é opcional: omitir (ou enviar `undefined`) cadastra o produto
 * sem SKU. Não envie string vazia — o `@Pattern` do backend rejeita.
 * Campos fiscais opcionais (`codigoBarras`, `cest`, etc.) devem ser
 * omitidos quando vazios; `ncm` é obrigatório (8 dígitos numéricos).
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
  /** NCM (8 dígitos numéricos). Obrigatório. */
  ncm: string
  origem?: OrigemProduto
  codigoBarras?: string
  cest?: string
  exTipi?: string
  pesoLiquido?: number
  pesoBruto?: number
  csosn?: string
  aliquotaIcmsSt?: number
  mvaSt?: number
  cstIpi?: string
  classeEnqIpi?: string
  cstPis?: string
  cstCofins?: string
}

/** Corpo de PATCH /api/v1/products/{id}. Campos opcionais. */
export interface ProductUpdateRequest {
  name?: string
  code?: string
  unitType?: UnitType
  status?: ProductStatus
  price?: number
  stockQuantity?: number
  ncm?: string
  origem?: OrigemProduto
  codigoBarras?: string
  cest?: string
  exTipi?: string
  pesoLiquido?: number
  pesoBruto?: number
  csosn?: string
  aliquotaIcmsSt?: number
  mvaSt?: number
  cstIpi?: string
  classeEnqIpi?: string
  cstPis?: string
  cstCofins?: string
}

/** Filtros suportados na listagem/busca de produtos. */
export interface ProductFilters {
  query?: string
  status?: ProductStatus | 'ALL'
  page?: number
  size?: number
}