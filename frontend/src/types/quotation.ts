/** Tipos do módulo de vendas (Propostas Comerciais). Espelham os DTOs do backend. */

// =====================================================================
// Enums
// =====================================================================

/** Ciclo de vida da proposta. Espelha QuotationStatus no backend. */
export type QuotationStatus = 'ATIVA' | 'CONVERTIDA' | 'CANCELADA' | 'EXPIRADA'

/** Rótulos em português para cada `QuotationStatus`. */
export const QUOTATION_STATUS_LABELS: Record<QuotationStatus, string> = {
  ATIVA: 'Ativa',
  CONVERTIDA: 'Convertida',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada',
}

/** Tipo de aplicação do desconto (valor fixo ou percentual). */
export type DiscountType = 'AMOUNT' | 'PERCENT'

/** Tipo de frete. CIF = por conta do remetente; FOB = por conta do destinatário. */
export type FreightType = 'CIF' | 'FOB'

export const FREIGHT_TYPE_LABELS: Record<FreightType, string> = {
  CIF: 'CIF — Por conta do remetente',
  FOB: 'FOB — Por conta do destinatário',
}

export const FREIGHT_TYPE_OPTIONS: ReadonlyArray<{
  value: FreightType
  label: string
}> = (Object.keys(FREIGHT_TYPE_LABELS) as FreightType[]).map((value) => ({
  value,
  label: FREIGHT_TYPE_LABELS[value],
}))

export const DISCOUNT_TYPE_LABELS: Record<DiscountType, string> = {
  AMOUNT: 'R$ (valor fixo)',
  PERCENT: '% (percentual)',
}

export const DISCOUNT_TYPE_OPTIONS: ReadonlyArray<{
  value: DiscountType
  label: string
}> = (Object.keys(DISCOUNT_TYPE_LABELS) as DiscountType[]).map((value) => ({
  value,
  label: DISCOUNT_TYPE_LABELS[value],
}))

/** Condição de pagamento (35 valores no backend). */
export type PaymentCondition =
  // À vista
  | 'A_VISTA_DINHEIRO' | 'PIX' | 'BOLETO_A_VISTA'
  // Boleto — uma parcela
  | 'BOLETO_15_DIAS' | 'BOLETO_28_DIAS' | 'BOLETO_30_DIAS'
  | 'BOLETO_45_DIAS' | 'BOLETO_60_DIAS' | 'BOLETO_90_DIAS'
  // Prazo único
  | 'PRAZO_7_DIAS' | 'PRAZO_14_DIAS' | 'PRAZO_15_DIAS' | 'PRAZO_21_DIAS'
  | 'PRAZO_28_DIAS' | 'PRAZO_30_DIAS' | 'PRAZO_45_DIAS'
  | 'PRAZO_60_DIAS' | 'PRAZO_90_DIAS'
  // Entrada + parcelas
  | 'ENTRADA_MAIS_30_DIAS' | 'ENTRADA_MAIS_30_60_DIAS'
  | 'ENTRADA_MAIS_30_60_90_DIAS'
  // Parcelamento múltiplo
  | 'PARCELAS_30_60' | 'PARCELAS_30_60_90' | 'PARCELAS_30_60_90_120'
  | 'PARCELAS_15_30_45' | 'PARCELAS_28_56_84' | 'PARCELAS_30_45_60'
  | 'PARCELAS_30_60_90_120_150'
  // Faturado
  | 'FATURADO_30_DIAS' | 'FATURADO_45_DIAS'
  | 'FATURADO_60_DIAS' | 'FATURADO_90_DIAS'

export const PAYMENT_CONDITION_LABELS: Record<PaymentCondition, string> = {
  A_VISTA_DINHEIRO: 'À Vista (Dinheiro)',
  PIX: 'PIX',
  BOLETO_A_VISTA: 'Boleto à Vista',
  BOLETO_15_DIAS: 'Boleto 15 Dias',
  BOLETO_28_DIAS: 'Boleto 28 Dias',
  BOLETO_30_DIAS: 'Boleto 30 Dias',
  BOLETO_45_DIAS: 'Boleto 45 Dias',
  BOLETO_60_DIAS: 'Boleto 60 Dias',
  BOLETO_90_DIAS: 'Boleto 90 Dias',
  PRAZO_7_DIAS: '7 Dias',
  PRAZO_14_DIAS: '14 Dias',
  PRAZO_15_DIAS: '15 Dias',
  PRAZO_21_DIAS: '21 Dias',
  PRAZO_28_DIAS: '28 Dias',
  PRAZO_30_DIAS: '30 Dias',
  PRAZO_45_DIAS: '45 Dias',
  PRAZO_60_DIAS: '60 Dias',
  PRAZO_90_DIAS: '90 Dias',
  ENTRADA_MAIS_30_DIAS: 'Entrada + 30 Dias',
  ENTRADA_MAIS_30_60_DIAS: 'Entrada + 30 + 60 Dias',
  ENTRADA_MAIS_30_60_90_DIAS: 'Entrada + 30 + 60 + 90 Dias',
  PARCELAS_30_60: '30/60 Dias',
  PARCELAS_30_60_90: '30/60/90 Dias',
  PARCELAS_30_60_90_120: '30/60/90/120 Dias',
  PARCELAS_15_30_45: '15/30/45 Dias',
  PARCELAS_28_56_84: '28/56/84 Dias',
  PARCELAS_30_45_60: '30/45/60 Dias',
  PARCELAS_30_60_90_120_150: '30/60/90/120/150 Dias',
  FATURADO_30_DIAS: 'Faturado para 30 Dias',
  FATURADO_45_DIAS: 'Faturado para 45 Dias',
  FATURADO_60_DIAS: 'Faturado para 60 Dias',
  FATURADO_90_DIAS: 'Faturado para 90 Dias',
}

export const PAYMENT_CONDITION_OPTIONS: ReadonlyArray<{
  value: PaymentCondition
  label: string
}> = (Object.keys(PAYMENT_CONDITION_LABELS) as PaymentCondition[]).map(
  (value) => ({ value, label: PAYMENT_CONDITION_LABELS[value] }),
)

// =====================================================================
// Tipo de cliente (polimorfismo por duas FKs nullable)
// =====================================================================

/** Tipo do cliente referenciado pela proposta (PF ou PJ). */
export type QuotationClientType = 'CUSTOMER' | 'COMPANY'

export const QUOTATION_CLIENT_TYPE_LABELS: Record<
  QuotationClientType,
  string
> = {
  CUSTOMER: 'Cliente (PF)',
  COMPANY: 'Empresa (PJ)',
}

// =====================================================================
// DTOs
// =====================================================================

/**
 * Resumo de cliente (PF ou PJ) retornado pelo typeahead de seleção.
 * Espelha ClientSummaryResponse no backend.
 */
export interface ClientSummaryResponse {
  type: QuotationClientType
  uuid: string
  code: string
  name: string
  document: string
}

/** Linha de produto retornada pela API. Espelha QuotationItemResponse. */
export interface QuotationItemResponse {
  uuid: string
  productUuid: string
  quantity: number
  unitPrice: number
  /** Subtotal bruto da linha (unitPrice * quantity). */
  lineSubtotal: number
  discountType: DiscountType | null
  discount: number | null
  /** Total líquido da linha (lineSubtotal - desconto da linha). */
  totalPrice: number
}

/** Linha de produto enviada na criação/edição. Espelha QuotationItemRequest. */
export interface QuotationItemRequest {
  productUuid: string
  quantity: number
  unitPrice: number
  discountType?: DiscountType
  discount?: number | null
}

/**
 * Representação completa da proposta comercial.
 * Espelha QuotationResponse no backend.
 *
 * <p>O campo {@link number} agora é numérico (Long no backend) — sem
 * prefixo textual. A primeira proposta do sistema é {@code 1500} e
 * incrementa em +1 a cada nova emissão.</p>
 */
export interface QuotationResponse {
  uuid: string
  number: number
  issueDate: string
  customerUuid: string | null
  companyUuid: string | null
  clientType: QuotationClientType
  attention: string | null
  sellerUuid: string
  items: QuotationItemResponse[]
  discountType: DiscountType | null
  discount: number | null
  validityDays: number | null
  paymentCondition: PaymentCondition | null
  notes: string | null
  status: QuotationStatus
  /** Transportadora selecionada (opcional). FK para Carrier. */
  carrierUuid: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType: FreightType | null
  /** Valor do frete (manual, independente do Carrier selecionado). */
  freightValue: number | null
  /** Soma dos totais líquidos dos itens (antes do desconto global). */
  subtotal: number
  /** Total final (subtotal - desconto global). */
  total: number
  /** Soma das quantidades de todos os itens. */
  totalQuantity: number
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/**
 * Resumo da proposta para listagens paginadas.
 * Espelha QuotationSummaryResponse no backend.
 */
export interface QuotationSummaryResponse {
  uuid: string
  number: number
  issueDate: string
  clientType: QuotationClientType
  clientUuid: string
  clientName: string
  sellerUuid: string
  status: QuotationStatus
  totalQuantity: number
  total: number
  paymentCondition: PaymentCondition | null
}

/** Corpo de POST /api/v1/quotations. Espelha QuotationCreateRequest. */
export interface QuotationCreateRequest {
  customerUuid?: string | null
  companyUuid?: string | null
  attention?: string | null
  sellerUuid: string
  items: QuotationItemRequest[]
  discountType?: DiscountType | null
  discount?: number | null
  validityDays?: number | null
  paymentCondition?: PaymentCondition | null
  notes?: string | null
  /** Transportadora selecionada (opcional). FK para Carrier. */
  carrierUuid?: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType?: FreightType | null
  /** Valor do frete (manual, independente do Carrier selecionado). */
  freightValue?: number | null
}

/** Corpo de PATCH /api/v1/quotations/{id}. Espelha QuotationUpdateRequest. */
export interface QuotationUpdateRequest {
  customerUuid?: string | null
  companyUuid?: string | null
  attention?: string | null
  sellerUuid?: string | null
  items?: QuotationItemRequest[]
  discountType?: DiscountType | null
  discount?: number | null
  validityDays?: number | null
  paymentCondition?: PaymentCondition | null
  notes?: string | null
  /** Transportadora selecionada (opcional). FK para Carrier. */
  carrierUuid?: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType?: FreightType | null
  /** Valor do frete (manual, independente do Carrier selecionado). */
  freightValue?: number | null
}

/** Resposta do endpoint /quotations/next-number. */
export interface NextQuotationNumberResponse {
  number: number
}

/** Filtros suportados no endpoint de listagem/busca de propostas. */
export interface QuotationFilters {
  status?: QuotationStatus
  startDate?: string
  endDate?: string
  clientUuid?: string
  sellerUuid?: string
  number?: string
  page?: number
  size?: number
}