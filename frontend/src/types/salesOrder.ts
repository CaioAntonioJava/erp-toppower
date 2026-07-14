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
 * Transições: ABERTO → FINALIZADO.
 * CANCELADO é terminal, alcançável apenas antes da finalização.
 */
export type SalesOrderStatus =
  | 'ABERTO'
  | 'FINALIZADO'
  | 'CANCELADO'

/** Rótulos em português para cada `SalesOrderStatus`. */
export const SALES_ORDER_STATUS_LABELS: Record<SalesOrderStatus, string> = {
  ABERTO: 'Aberto',
  FINALIZADO: 'Finalizado',
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
  id: number
  productId: number
  quantity: number
  /** Preço unitário (com margem aplicada quando houver). */
  unitPrice: number
  /** Preço unitário original (sem margem). Igual ao unitPrice quando não há margem. */
  baseUnitPrice: number
  /** Subtotal bruto da linha (unitPrice * quantity). */
  lineSubtotal: number
  discountType: import('./quotation').DiscountType | null
  discount: number | null
  /** Total líquido da linha (lineSubtotal - desconto da linha). */
  totalPrice: number
}

/** Linha de produto enviada na criação/edição. Espelha SalesOrderItemRequest. */
export interface SalesOrderItemRequest {
  productId: number
  quantity: number
  unitPrice: number
  discountType?: import('./quotation').DiscountType
  discount?: number | null
}

/**
 * Representação completa do pedido de venda.
 * Espelha SalesOrderResponse no backend.
 *
 * <p>A margem de lucro ({@code profitMargin}) é opcional, aplicada
 * apenas na criação/edição direta. Nula em pedidos convertidos de
 * proposta. É informação interna e não aparece no PDF do pedido.</p>
 */
export interface SalesOrderResponse {
  id: number
  number: number
  /** Data de emissão (yyyy-MM-dd). */
  orderDate: string
  customerId: number | null
  companyId: number | null
  clientType: SalesOrderClientType
  /** Nome de exibição do cliente (PF: nome; PJ: nome fantasia/razão social). Resolvido no backend. */
  clientName: string | null
  /** Código interno do cliente (ex.: "CLI000001", "EMP000001"). Resolvido no backend. */
  clientCode: string | null
  attention: string | null
  sellerId: number
  /** Nome do vendedor (resolvido no backend). */
  sellerName: string | null
  items: SalesOrderItemResponse[]
  discountType: import('./quotation').DiscountType | null
  discount: number | null
  paymentCondition: import('./quotation').PaymentCondition | null
  notes: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType: import('./quotation').FreightType | null
  /** Valor do frete (manual). */
  freightValue: number | null
  /** ID da transportadora (Carrier) responsável pelo frete. */
  carrierId: number | null
  /** Nome da transportadora (resolvido no backend). */
  carrierName: string | null
  status: SalesOrderStatus
  /** ID da proposta que deu origem ao pedido (nulo em criação direta). */
  quotationId: number | null
  /** Número da proposta de origem (nulo em criação direta). */
  quotationNumber: number | null
  /** Margem de lucro (%) aplicada na criação/edição direta. Nula em pedidos convertidos. */
  profitMargin: number | null
  /** Soma dos totais líquidos dos itens (antes do desconto global). */
  subtotal: number
  /**
   * Total final do pedido. Composição:
   * `(subtotal − desconto global) + frete`. A margem, quando presente,
   * já está embutida nos preços dos itens.
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
  id: number
  number: number
  orderDate: string
  clientType: SalesOrderClientType
  clientId: number
  clientName: string
  /** Código interno do cliente (ex.: "CLI000001", "EMP000001"). Resolvido no backend. */
  clientCode: string | null
  sellerId: number
  /** Nome do vendedor (resolvido no backend). */
  sellerName: string | null
  status: SalesOrderStatus
  totalQuantity: number
  total: number
  paymentCondition: import('./quotation').PaymentCondition | null
  /** Número da proposta de origem (nulo em criação direta). */
  quotationNumber: number | null
}

/** Corpo de POST /api/v1/sales-orders. Espelha SalesOrderCreateRequest. */
export interface SalesOrderCreateRequest {
  customerId?: number | null
  companyId?: number | null
  attention?: string | null
  sellerId: number
  items: SalesOrderItemRequest[]
  discountType?: import('./quotation').DiscountType | null
  discount?: number | null
  paymentCondition?: import('./quotation').PaymentCondition | null
  notes?: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType?: import('./quotation').FreightType | null
  /** Valor do frete (manual). */
  freightValue?: number | null
  /** ID da transportadora (Carrier) responsável pelo frete. Opcional. */
  carrierId?: number | null
  /** Margem de lucro (%) aplicada sobre o preço unitário dos itens. Opcional. */
  profitMargin?: number | null
}

/** Corpo de PATCH /api/v1/sales-orders/{id}. Espelha SalesOrderUpdateRequest. */
export interface SalesOrderUpdateRequest {
  customerId?: number | null
  companyId?: number | null
  attention?: string | null
  sellerId?: number | null
  items?: SalesOrderItemRequest[]
  discountType?: import('./quotation').DiscountType | null
  discount?: number | null
  paymentCondition?: import('./quotation').PaymentCondition | null
  notes?: string | null
  /** Tipo de frete (CIF/FOB). */
  freightType?: import('./quotation').FreightType | null
  /** Valor do frete (manual). */
  freightValue?: number | null
  /** ID da transportadora (Carrier). null = remover a transportadora vinculada. */
  carrierId?: number | null
  /** Margem de lucro (%) aplicada sobre o preço unitário dos itens. Omitir mantém a atual. */
  profitMargin?: number | null
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
  clientId?: number
  sellerId?: number
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
//   unitPriceComMargem = unitPrice × (1 + profitMargin/100)   (se houver margem)
//   item.totalPrice = unitPriceComMargem * quantity - discountAmount
//     discountAmount = AMOUNT ? discount : gross * discount / 100
//   subtotal = Σ item.totalPrice
//   globalDiscountValue = AMOUNT ? discount : subtotal * discount / 100
//   total = (subtotal - globalDiscountValue) + freight
//   totalQuantity = Σ item.quantity
//
// A margem, quando informada, é aplicada sobre o preço unitário antes
// do desconto da linha — espelhando SalesOrderMapper.calculateItemTotalPrice.

/** Arredonda para 2 casas (HALF_UP), como o backend (BigDecimal). */
export function round2(n: number): number {
  return Math.round((n + Number.EPSILON) * 100) / 100
}

/**
 * Aplica a margem de lucro como multiplicação percentual sobre o preço
 * unitário: `unitPrice × (1 + profitMargin/100)`. Sem margem (nula ou
 * zero) retorna o próprio preço arredondado. Espelha
 * `SalesOrderMapper.applyProfitMargin`.
 */
export function applyProfitMargin(
  unitPrice: number,
  profitMargin?: number | null,
): number {
  if (unitPrice == null) return 0
  if (profitMargin == null || profitMargin === 0) return round2(unitPrice)
  return round2(unitPrice * (1 + profitMargin / 100))
}

/**
 * Calcula o total líquido de uma linha de item, espelhando
 * `SalesOrderMapper.calculateItemTotalPrice`. A margem, quando
 * informada, é aplicada sobre o preço unitário antes do desconto da
 * linha.
 */
export function calculateItemTotalPrice(
  unitPrice: number,
  quantity: number,
  discount: number | null,
  discountType: import('./quotation').DiscountType | null,
  profitMargin?: number | null,
): number {
  if (unitPrice == null || quantity == null) return 0
  const unitPriceWithMargin = applyProfitMargin(unitPrice, profitMargin)
  const gross = unitPriceWithMargin * quantity
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
 * espelhando `SalesOrder.recalculateTotals`. A margem, quando
 * informada, é aplicada sobre o preço unitário de cada item antes do
 * desconto da linha.
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
  profitMargin?: number | null,
): SalesOrderTotals {
  const subtotal = items.reduce(
    (acc, it) => acc + calculateItemTotalPrice(it.unitPrice, it.quantity, it.discount, it.discountType, profitMargin),
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