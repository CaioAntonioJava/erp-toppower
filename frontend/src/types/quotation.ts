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
  id: number
  code: string
  name: string
  document: string
}

/** Linha de produto retornada pela API. Espelha QuotationItemResponse. */
export interface QuotationItemResponse {
  id: number
  productId: number
  quantity: number
  /** Preço unitário final (com margem de lucro embutida). */
  unitPrice: number
  /**
   * Preço unitário original enviado pelo usuário (sem margem de lucro).
   * É o valor usado como ponto de partida na edição — reaplicar a margem
   * sobre `unitPrice` causaria duplicação.
   */
  baseUnitPrice: number
  /** Subtotal bruto da linha (unitPrice * quantity). */
  lineSubtotal: number
  /**
   * Margem de lucro (%) aplicada a esta linha (null = usou a margem do
   * cabeçalho da proposta). Quando informada, sobrescreve a margem do
   * cabeçalho para este item.
   */
  profitMargin: number | null
  /** Total da linha (lineSubtotal, já com margem embutida). */
  totalPrice: number
}

/** Linha de produto enviada na criação/edição. Espelha QuotationItemRequest. */
export interface QuotationItemRequest {
  productId: number
  quantity: number
  unitPrice: number
  /**
   * Margem de lucro (%) aplicada a esta linha. Omitir/usar null faz a
   * linha usar a margem do cabeçalho.
   */
  profitMargin?: number | null
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
  id: number
  number: number
  issueDate: string
  customerId: number | null
  companyId: number | null
  clientType: QuotationClientType
  /** Nome de exibição do cliente (PF: nome; PJ: nome fantasia/razão social). Resolvido no backend. */
  clientName: string | null
  /** Código interno do cliente (ex.: "CLI000001", "EMP000001"). Resolvido no backend. */
  clientCode: string | null
  attention: string | null
  sellerId: number
  /** Nome do vendedor (resolvido no backend). */
  sellerName: string | null
  items: QuotationItemResponse[]
  discountType: DiscountType | null
  discount: number | null
  validityDays: number | null
  paymentCondition: PaymentCondition | null
  notes: string | null
  status: QuotationStatus
  /** Tipo de frete (CIF/FOB). */
  freightType: FreightType | null
  /** Valor do frete (manual). */
  freightValue: number | null
  /** ID da transportadora (Carrier) responsável pelo frete. */
  carrierId: number | null
  /** Nome da transportadora (resolvido no backend). */
  carrierName: string | null
  /**
   * Margem de lucro aplicada sobre o total da proposta (em %). Ex.: 10 = 10%.
   * Aplicada como multiplicação (fator `1 + profitMargin/100`) sobre o subtotal
   * dos itens, antes do desconto global e do frete.
   */
  profitMargin: number
  /** Soma dos totais líquidos dos itens (antes do desconto global). */
  subtotal: number
  /**
   * Total final da proposta. Composição:
   * `(subtotal × (1 + profitMargin/100)) − desconto global + frete`.
   * A margem incide apenas sobre o subtotal dos itens; o desconto é retirado
   * depois da margem; o frete é somado por último e não participa da margem
   * nem do desconto.
   */
  total: number
  /**
   * Valor em R$ do desconto global efetivamente aplicado (já considerando a
   * margem de lucro sobre o subtotal). Calculado pelo backend.
   */
  globalDiscountValue: number
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
  id: number
  number: number
  issueDate: string
  clientType: QuotationClientType
  clientId: number
  clientName: string
  /** Código interno do cliente (ex.: "CLI000001", "EMP000001"). Resolvido no backend. */
  clientCode: string | null
  sellerId: number
  /** Nome do vendedor (resolvido no backend). */
  sellerName: string | null
  status: QuotationStatus
  totalQuantity: number
  total: number
  paymentCondition: PaymentCondition | null
}

/** Corpo de POST /api/v1/quotations. Espelha QuotationCreateRequest. */
export interface QuotationCreateRequest {
  customerId?: number | null
  companyId?: number | null
  attention?: string | null
  sellerId: number
  items: QuotationItemRequest[]
  discountType?: DiscountType | null
  discount?: number | null
  validityDays?: number | null
  paymentCondition?: PaymentCondition | null
  notes?: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType?: FreightType | null
  /** Valor do frete (manual). */
  freightValue?: number | null
  /**
   * Margem de lucro (%) aplicada a todos os itens sem margem própria.
   * Opcional na criação quando todos os itens informam margem própria.
   * Ex.: 10 = 10%.
   */
  profitMargin: number | null
  /** ID da transportadora (Carrier) responsável pelo frete. Opcional. */
  carrierId?: number | null
}

/** Corpo de PATCH /api/v1/quotations/{id}. Espelha QuotationUpdateRequest. */
export interface QuotationUpdateRequest {
  customerId?: number | null
  companyId?: number | null
  attention?: string | null
  sellerId?: number | null
  items?: QuotationItemRequest[]
  discountType?: DiscountType | null
  discount?: number | null
  validityDays?: number | null
  paymentCondition?: PaymentCondition | null
  notes?: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType?: FreightType | null
  /** Valor do frete (manual). */
  freightValue?: number | null
  /**
   * Margem de lucro (%) aplicada a todos os itens sem margem própria.
   * Opcional no PATCH (quando omitida, mantém o valor atual). Ex.: 10
   * = 10%.
   */
  profitMargin?: number | null
  /** ID da transportadora (Carrier). null = remover a transportadora vinculada. */
  carrierId?: number | null
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
  clientId?: number
  sellerId?: number
  number?: string
  page?: number
  size?: number
}

// =====================================================================
// Simulação de totais (preview sem persistência)
// =====================================================================

/**
 * Linha de produto enviada à simulação de totais. Variação permissiva de
 * {@link QuotationItemRequest}: campos numéricos são opcionais para tolerar
 * preview com o formulário em estado intermediário. Espelha
 * QuotationSimulateItemRequest no backend.
 */
export interface QuotationSimulateItemRequest {
  productId?: number | null
  quantity?: number | null
  unitPrice?: number | null
  /** Margem de lucro (%) aplicada a esta linha (sobrescreve a do cabeçalho). */
  profitMargin?: number | null
}

/**
 * Corpo de POST /api/v1/quotations/simulate. Variação permissiva de
 * {@link QuotationCreateRequest}: nenhum campo é obrigatório, pois o
 * preview pode ser disparado com o formulário incompleto. Espelha
 * QuotationSimulateRequest no backend.
 */
export interface QuotationSimulateRequest {
  customerId?: number | null
  companyId?: number | null
  attention?: string | null
  sellerId?: number | null
  items?: QuotationSimulateItemRequest[] | null
  discountType?: DiscountType | null
  discount?: number | null
  validityDays?: number | null
  paymentCondition?: PaymentCondition | null
  notes?: string | null
  freightType?: FreightType | null
  freightValue?: number | null
  profitMargin?: number | null
}

/**
 * Resultado da simulação de totais (preview). Espelha
 * QuotationSimulateResponse no backend. Todos os valores são calculados
 * pelo backend.
 */
export interface QuotationSimulateResponse {
  items: QuotationItemResponse[]
  subtotal: number
  /** Valor em R$ do desconto global efetivamente aplicado. */
  globalDiscountValue: number
  total: number
  totalQuantity: number
}