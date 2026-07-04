/** Tipos do módulo de Pedidos de Venda. Espelham os DTOs do backend. */

// Reaproveita os enums compartilhados com o módulo de propostas — no
// backend, DiscountType/FreightType/PaymentCondition vivem no package
// sales.quotation.enums e são importados pelo salesorder.
export type {
  DiscountType,
  FreightType,
  PaymentCondition,
} from './quotation'
export {
  DISCOUNT_TYPE_LABELS,
  DISCOUNT_TYPE_OPTIONS,
  FREIGHT_TYPE_LABELS,
  FREIGHT_TYPE_OPTIONS,
  PAYMENT_CONDITION_LABELS,
  PAYMENT_CONDITION_OPTIONS,
} from './quotation'

// =====================================================================
// Status do pedido
// =====================================================================

/**
 * Ciclo de vida do pedido de venda. Espelha SalesOrderStatus no backend.
 *
 * Transições: ABERTO → EM_SEPARACAO → FATURADO → ENTREGUE.
 * CANCELADO é terminal, alcançável apenas antes do faturamento.
 */
export type SalesOrderStatus =
  | 'ABERTO'
  | 'EM_SEPARACAO'
  | 'FATURADO'
  | 'ENTREGUE'
  | 'CANCELADO'

/** Rótulos em português para cada `SalesOrderStatus`. */
export const SALES_ORDER_STATUS_LABELS: Record<SalesOrderStatus, string> = {
  ABERTO: 'Aberto',
  EM_SEPARACAO: 'Em Separação',
  FATURADO: 'Faturado',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
}

export const SALES_ORDER_STATUS_OPTIONS: ReadonlyArray<{
  value: SalesOrderStatus
  label: string
}> = (Object.keys(SALES_ORDER_STATUS_LABELS) as SalesOrderStatus[]).map(
  (value) => ({ value, label: SALES_ORDER_STATUS_LABELS[value] }),
)

// =====================================================================
// Tipo de cliente (polimorfismo por duas FKs nullable)
// =====================================================================

/**
 * Tipo do cliente referenciado pelo pedido (PF ou PJ). Espelha
 * SalesOrderResponse.ClientType no backend.
 */
export type SalesOrderClientType = 'CUSTOMER' | 'COMPANY'

export const SALES_ORDER_CLIENT_TYPE_LABELS: Record<
  SalesOrderClientType,
  string
> = {
  CUSTOMER: 'Cliente (PF)',
  COMPANY: 'Empresa (PJ)',
}

// =====================================================================
// DTOs
// =====================================================================

/** Linha de produto retornada pela API. Espelha SalesOrderItemResponse. */
export interface SalesOrderItemResponse {
  uuid: string
  productUuid: string
  quantity: number
  unitPrice: number
  /** Subtotal bruto da linha (unitPrice * quantity). */
  lineSubtotal: number
  discountType: import('./quotation').DiscountType | null
  discount: number | null
  /** Total líquido da linha (lineSubtotal - desconto da linha). */
  totalPrice: number
}

/** Linha de produto enviada na criação/edição. Espelha SalesOrderItemRequest. */
export interface SalesOrderItemRequest {
  productUuid: string
  quantity: number
  unitPrice: number
  discountType?: import('./quotation').DiscountType
  discount?: number | null
}

/**
 * Representação completa do pedido de venda.
 * Espelha SalesOrderResponse no backend.
 *
 * <p><b>Não expõe margem de lucro</b> — o pedido é o documento externo
 * enviado ao cliente, e a margem é informação interna da proposta.</p>
 */
export interface SalesOrderResponse {
  uuid: string
  number: number
  /** Data de emissão (yyyy-MM-dd). */
  orderDate: string
  customerUuid: string | null
  companyUuid: string | null
  clientType: SalesOrderClientType
  attention: string | null
  sellerUuid: string
  items: SalesOrderItemResponse[]
  discountType: import('./quotation').DiscountType | null
  discount: number | null
  paymentCondition: import('./quotation').PaymentCondition | null
  notes: string | null
  /** Transportadora selecionada (opcional). FK para Carrier. */
  carrierUuid: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType: import('./quotation').FreightType | null
  /** Valor do frete (manual, independente do Carrier selecionado). */
  freightValue: number | null
  status: SalesOrderStatus
  /** UUID da proposta que deu origem ao pedido (nulo em criação direta). */
  quotationUuid: string | null
  /** Número da proposta de origem (nulo em criação direta). */
  quotationNumber: number | null
  /** Soma dos totais líquidos dos itens (antes do desconto global). */
  subtotal: number
  /**
   * Total final do pedido. Composição:
   * `(subtotal − desconto global) + frete`. Sem margem de lucro.
   */
  total: number
  /** Valor em R$ do desconto global efetivamente aplicado. */
  globalDiscountValue: number
  /** Soma das quantidades de todos os itens. */
  totalQuantity: number
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/**
 * Resumo do pedido para listagens paginadas.
 * Espelha SalesOrderSummaryResponse no backend.
 */
export interface SalesOrderSummaryResponse {
  uuid: string
  number: number
  orderDate: string
  clientType: SalesOrderClientType
  clientUuid: string
  clientName: string
  sellerUuid: string
  status: SalesOrderStatus
  totalQuantity: number
  total: number
  paymentCondition: import('./quotation').PaymentCondition | null
  /** Número da proposta de origem (nulo em criação direta). */
  quotationNumber: number | null
}

/** Corpo de POST /api/v1/sales-orders. Espelha SalesOrderCreateRequest. */
export interface SalesOrderCreateRequest {
  customerUuid?: string | null
  companyUuid?: string | null
  attention?: string | null
  sellerUuid: string
  items: SalesOrderItemRequest[]
  discountType?: import('./quotation').DiscountType | null
  discount?: number | null
  paymentCondition?: import('./quotation').PaymentCondition | null
  notes?: string | null
  /** Transportadora selecionada (opcional). FK para Carrier. */
  carrierUuid?: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType?: import('./quotation').FreightType | null
  /** Valor do frete (manual, independente do Carrier selecionado). */
  freightValue?: number | null
}

/** Corpo de PATCH /api/v1/sales-orders/{id}. Espelha SalesOrderUpdateRequest. */
export interface SalesOrderUpdateRequest {
  customerUuid?: string | null
  companyUuid?: string | null
  attention?: string | null
  sellerUuid?: string | null
  items?: SalesOrderItemRequest[]
  discountType?: import('./quotation').DiscountType | null
  discount?: number | null
  paymentCondition?: import('./quotation').PaymentCondition | null
  notes?: string | null
  /** Transportadora selecionada (opcional). FK para Carrier. */
  carrierUuid?: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType?: import('./quotation').FreightType | null
  /** Valor do frete (manual, independente do Carrier selecionado). */
  freightValue?: number | null
}

/**
 * Corpo de POST /api/v1/sales-orders/from-quotation/{quotationId}.
 * Todos os campos são opcionais — quando omitidos, o pedido herda os
 * valores da proposta de origem. Espelha SalesOrderFromQuotationRequest.
 */
export interface SalesOrderFromQuotationRequest {
  attention?: string | null
  paymentCondition?: import('./quotation').PaymentCondition | null
  notes?: string | null
}

/** Resposta do endpoint /sales-orders/next-number. */
export interface NextSalesOrderNumberResponse {
  number: number
}

/** Filtros suportados no endpoint de listagem/busca de pedidos. */
export interface SalesOrderFilters {
  status?: SalesOrderStatus
  startDate?: string
  endDate?: string
  clientUuid?: string
  sellerUuid?: string
  number?: string
  /** Filtro exato pelo número da proposta de origem. */
  quotationNumber?: number
  page?: number
  size?: number
}

// =====================================================================
// Cálculo de totais (cliente) — espelha SalesOrderMapper/SalesOrder
// =====================================================================
//
// Diferente das propostas, o backend de pedidos NÃO expõe um endpoint
// /simulate. Para oferecer o mesmo preview em tempo real no formulário,
// replicamos aqui a fórmula do backend:
//
//   item.totalPrice = unitPrice * quantity - discountAmount
//     discountAmount = AMOUNT ? discount : gross * discount / 100
//   subtotal = Σ item.totalPrice
//   globalDiscountValue = AMOUNT ? discount : subtotal * discount / 100
//   total = (subtotal - globalDiscountValue) + freight
//   totalQuantity = Σ item.quantity

/** Arredonda para 2 casas (HALF_UP), como o backend (BigDecimal). */
export function round2(n: number): number {
  return Math.round((n + Number.EPSILON) * 100) / 100
}

/**
 * Calcula o total líquido de uma linha de item, espelhando
 * `SalesOrderMapper.calculateItemTotalPrice`.
 */
export function calculateItemTotalPrice(
  unitPrice: number,
  quantity: number,
  discount: number | null,
  discountType: import('./quotation').DiscountType | null,
): number {
  if (unitPrice == null || quantity == null) return 0
  const gross = unitPrice * quantity
  if (discount == null || discountType == null || discount === 0) {
    return round2(gross)
  }
  const discountAmount =
    discountType === 'AMOUNT'
      ? discount
      : round2((gross * discount) / 100)
  return round2(gross - discountAmount)
}

/**
 * Calcula o valor em R$ do desconto global efetivamente aplicado,
 * espelhando `SalesOrder.calculateGlobalDiscountValue`.
 */
export function calculateGlobalDiscountValue(
  subtotal: number,
  discount: number | null,
  discountType: import('./quotation').DiscountType | null,
): number {
  if (discount == null || discountType == null) return 0
  const value =
    discountType === 'AMOUNT'
      ? discount
      : round2((subtotal * discount) / 100)
  return round2(Math.min(value, subtotal))
}

export interface SalesOrderTotals {
  subtotal: number
  total: number
  globalDiscountValue: number
  totalQuantity: number
}

/**
 * Calcula todos os totais do pedido a partir dos itens e condições,
 * espelhando `SalesOrder.recalculateTotals`.
 */
export function calculateSalesOrderTotals(
  items: ReadonlyArray<{
    quantity: number
    unitPrice: number
    discount: number | null
    discountType: import('./quotation').DiscountType | null
  }>,
  discount: number | null,
  discountType: import('./quotation').DiscountType | null,
  freightValue: number | null,
): SalesOrderTotals {
  const subtotal = items.reduce(
    (acc, it) => acc + calculateItemTotalPrice(it.unitPrice, it.quantity, it.discount, it.discountType),
    0,
  )
  const totalQuantity = items.reduce((acc, it) => acc + (it.quantity ?? 0), 0)
  const globalDiscountValue = calculateGlobalDiscountValue(subtotal, discount, discountType)
  const freight = freightValue ?? 0
  const total = round2(subtotal - globalDiscountValue + freight)
  return {
    subtotal: round2(subtotal),
    total,
    globalDiscountValue,
    totalQuantity,
  }
}