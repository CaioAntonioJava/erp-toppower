/**
 * Tipos do módulo de Propostas Técnicas. Espelham os DTOs do backend
 * (br.com.toppower.erp_toppower.sales.technicalproposal.dto).
 *
 * Reaproveita os enums compartilhados com o módulo de propostas
 * comerciais: `DiscountType`, `FreightType`, `PaymentCondition` e
 * `ClientSummaryResponse` (o endpoint de busca de clientes é delegado
 * ao `ClientSearchService` compartilhado).
 */
import type {
  DiscountType,
  FreightType,
  PaymentCondition,
  ClientSummaryResponse,
} from './quotation'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type {
  DiscountType,
  FreightType,
  PaymentCondition,
  ClientSummaryResponse,
}
export {
  DISCOUNT_TYPE_OPTIONS,
  DISCOUNT_TYPE_LABELS,
  FREIGHT_TYPE_OPTIONS,
  FREIGHT_TYPE_LABELS,
  PAYMENT_CONDITION_OPTIONS,
  PAYMENT_CONDITION_LABELS,
} from './quotation'

// =====================================================================
// Enums próprios
// =====================================================================

/** Ciclo de vida da proposta técnica. Espelha TechnicalProposalStatus. */
export type TechnicalProposalStatus = 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA'

export const TECHNICAL_PROPOSAL_STATUS_LABELS: Record<
  TechnicalProposalStatus,
  string
> = {
  ABERTA: 'Aberta',
  EM_ANDAMENTO: 'Execução',
  CONCLUIDA: 'Concluída',
}

// =====================================================================
// Tipo de cliente (polimorfismo por duas FKs nullable)
// =====================================================================

/** Tipo do cliente referenciado pela proposta (PF ou PJ). */
export type TechnicalProposalClientType = 'CUSTOMER' | 'COMPANY'

export const TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS: Record<
  TechnicalProposalClientType,
  string
> = {
  CUSTOMER: 'Cliente (PF)',
  COMPANY: 'Empresa (PJ)',
}

// =====================================================================
// Objetivos
// =====================================================================

/** Linha de objetivo enviada na criação/edição. */
export interface TechnicalProposalObjectiveRequest {
  description: string
}

/** Linha de objetivo retornada pela API. */
export interface TechnicalProposalObjectiveResponse {
  uuid: string
  description: string
}

// =====================================================================
// Endereço (opcional)
// =====================================================================

/** Endereço de execução da proposta técnica (opcional). */
export interface TechnicalProposalAddressRequest {
  street?: string | null
  number?: string | null
  complement?: string | null
  neighborhood?: string | null
  city?: string | null
  state?: string | null
  zipCode?: string | null
}

export interface TechnicalProposalAddressResponse {
  street: string | null
  number: string | null
  complement: string | null
  neighborhood: string | null
  city: string | null
  state: string | null
  zipCode: string | null
}

// =====================================================================
// Itens
// =====================================================================

/** Linha de serviço enviada na criação/edição. */
export interface TechnicalProposalServiceItemRequest {
  description: string
  price?: number | null
}

/** Linha de serviço retornada pela API. */
export interface TechnicalProposalServiceItemResponse {
  uuid: string
  description: string
  /** Preço final (com margem de lucro embutida). */
  price: number | null
  /**
   * Preço original enviado pelo usuário (sem margem de lucro).
   * É o valor usado como ponto de partida na edição — reaplicar a margem
   * sobre `price` causaria duplicação.
   */
  basePrice: number | null
}

/** Linha de produto enviada na criação/edição. */
export interface TechnicalProposalProductItemRequest {
  productUuid: string
  quantity: number
  unitPrice: number
  discountType?: DiscountType | null
  discount?: number | null
}

/** Linha de produto retornada pela API. */
export interface TechnicalProposalProductItemResponse {
  uuid: string
  productUuid: string
  quantity: number
  /** Preço unitário final (com margem de lucro embutida). */
  unitPrice: number
  /**
   * Preço unitário original enviado pelo usuário (sem margem de lucro).
   * É o valor usado como ponto de partida na edição — reaplicar a margem
   * sobre `unitPrice` causaria duplicação.
   */
  baseUnitPrice: number
  lineSubtotal: number
  discountType: DiscountType | null
  discount: number | null
  totalPrice: number
}

// =====================================================================
// Header
// =====================================================================

/** Representação completa da proposta técnica. Espelha TechnicalProposalResponse. */
export interface TechnicalProposalResponse {
  uuid: string
  prefix: string
  sequence: number
  year: number
  /** Código formatado completo (ex.: "PL-001-2026"). */
  code: string
  customerUuid: string | null
  companyUuid: string | null
  clientType: TechnicalProposalClientType
  clientName: string | null
  clientCode: string | null
  address: TechnicalProposalAddressResponse | null
  objectives: TechnicalProposalObjectiveResponse[]
  description: string | null
  /** Nome do responsável técnico (opcional). */
  technicalResponsible: string | null
  /** E-mail de contato do responsável técnico (opcional, campo livre). */
  email: string | null
  status: TechnicalProposalStatus
  startDate: string
  /** Data de término prevista/real, informada manualmente. */
  endDate: string | null
  /** Data de entrega, preenchida automaticamente ao concluir. */
  deliveryDate: string | null
  serviceItems: TechnicalProposalServiceItemResponse[]
  productItems: TechnicalProposalProductItemResponse[]
  profitMargin: number
  discountType: DiscountType | null
  discount: number | null
  freightValue: number | null
  deliveryDeadline: string | null
  paymentCondition: PaymentCondition | null
  validity: string | null
  deliveryType: FreightType | null
  notes: string | null
  /** UUID da transportadora (Carrier) responsável pelo frete. */
  carrierUuid: string | null
  /** Nome da transportadora (resolvido no backend). */
  carrierName: string | null
  servicesSubtotal: number
  productsSubtotal: number
  subtotal: number
  globalDiscountValue: number
  total: number
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Resumo da proposta para listagens paginadas. */
export interface TechnicalProposalSummaryResponse {
  uuid: string
  code: string
  clientType: TechnicalProposalClientType
  clientUuid: string | null
  clientName: string | null
  clientCode: string | null
  objectives: TechnicalProposalObjectiveResponse[]
  status: TechnicalProposalStatus
  startDate: string
  /** Data de término prevista/real, informada manualmente. */
  endDate: string | null
  /** Data de entrega, preenchida automaticamente ao concluir. */
  deliveryDate: string | null
  total: number
  paymentCondition: PaymentCondition | null
}

/** Corpo de POST /api/v1/technical-proposals. */
export interface TechnicalProposalCreateRequest {
  customerUuid?: string | null
  companyUuid?: string | null
  address?: TechnicalProposalAddressRequest | null
  objectives: TechnicalProposalObjectiveRequest[]
  description?: string | null
  /** Nome do responsável técnico (opcional). */
  technicalResponsible?: string | null
  /** E-mail de contato do responsável técnico (opcional, sem validação de formato). */
  email?: string | null
  startDate?: string | null
  endDate?: string | null
  serviceItems?: TechnicalProposalServiceItemRequest[] | null
  productItems?: TechnicalProposalProductItemRequest[] | null
  profitMargin: number
  discountType?: DiscountType | null
  discount?: number | null
  freightValue?: number | null
  deliveryDeadline?: string | null
  paymentCondition?: PaymentCondition | null
  validity?: string | null
  deliveryType?: FreightType | null
  notes?: string | null
  /** UUID da transportadora (Carrier) responsável pelo frete. Opcional. */
  carrierUuid?: string | null
}

/** Corpo de PATCH /api/v1/technical-proposals/{id}. */
export interface TechnicalProposalUpdateRequest {
  customerUuid?: string | null
  companyUuid?: string | null
  address?: TechnicalProposalAddressRequest | null
  objectives?: TechnicalProposalObjectiveRequest[] | null
  description?: string | null
  /** Nome do responsável técnico. string vazia = limpar. */
  technicalResponsible?: string | null
  /** E-mail do responsável técnico. string vazia = limpar. */
  email?: string | null
  startDate?: string | null
  endDate?: string | null
  serviceItems?: TechnicalProposalServiceItemRequest[] | null
  productItems?: TechnicalProposalProductItemRequest[] | null
  profitMargin?: number | null
  discountType?: DiscountType | null
  discount?: number | null
  freightValue?: number | null
  deliveryDeadline?: string | null
  paymentCondition?: PaymentCondition | null
  validity?: string | null
  deliveryType?: FreightType | null
  notes?: string | null
  /** UUID da transportadora (Carrier). null = remover a transportadora vinculada. */
  carrierUuid?: string | null
}

/** Resposta do endpoint /technical-proposals/next-code. */
export interface NextTechnicalProposalCodeResponse {
  prefix: string
  sequence: number
  year: number
  code: string
}

/** Filtros suportados no endpoint de listagem/busca. */
export interface TechnicalProposalFilters {
  status?: TechnicalProposalStatus
  startDate?: string
  endDate?: string
  clientUuid?: string
  code?: string
  page?: number
  size?: number
}

// =====================================================================
// Simulação de totais (preview sem persistência)
// =====================================================================

/** Corpo de POST /api/v1/technical-proposals/simulate. */
export interface TechnicalProposalSimulateRequest {
  serviceItems?: TechnicalProposalServiceItemRequest[] | null
  productItems?: TechnicalProposalProductItemRequest[] | null
  profitMargin?: number | null
  discountType?: DiscountType | null
  discount?: number | null
  freightValue?: number | null
  deliveryType?: FreightType | null
}

/** Resultado da simulação de totais (preview). */
export interface TechnicalProposalSimulateResponse {
  serviceItems: TechnicalProposalServiceItemResponse[]
  productItems: TechnicalProposalProductItemResponse[]
  servicesSubtotal: number
  productsSubtotal: number
  subtotal: number
  globalDiscountValue: number
  total: number
}