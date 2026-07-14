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
  id: number
  description: string
  /** Preço do serviço prestado. */
  price: number | null
}

/** Linha de produto enviada na criação/edição. */
export interface TechnicalProposalProductItemRequest {
  productId: number
  quantity: number
  unitPrice: number
  discountType?: DiscountType | null
  discount?: number | null
}

/** Linha de produto retornada pela API. */
export interface TechnicalProposalProductItemResponse {
  id: number
  productId: number
  quantity: number
  /** Preço unitário do produto (snapshot no momento da emissão). */
  unitPrice: number
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
  id: number
  prefix: string
  sequence: number
  year: number
  /** Código formatado completo (ex.: "PL-001-2026"). */
  code: string
  customerId: number | null
  companyId: number | null
  clientType: TechnicalProposalClientType
  clientName: string | null
  clientCode: string | null
  address: TechnicalProposalAddressResponse | null
  description: string | null
  /** Número da revisão da proposta técnica (opcional). */
  revision: number | null
  /** Nome do responsável técnico (opcional). */
  technicalResponsible: string | null
  /** E-mail de contato do responsável técnico (opcional, campo livre). */
  email: string | null
  /** Telefone de contato do responsável técnico (opcional, campo livre). */
  phone: string | null
  status: TechnicalProposalStatus
  /** Data da proposta (data de emissão, sempre a data atual). */
  startDate: string
  /** Data de entrega, preenchida automaticamente ao concluir. */
  deliveryDate: string | null
  serviceItems: TechnicalProposalServiceItemResponse[]
  productItems: TechnicalProposalProductItemResponse[]
  discountType: DiscountType | null
  discount: number | null
  freightValue: number | null
  deliveryDeadline: string | null
  paymentCondition: PaymentCondition | null
  validity: string | null
  deliveryType: FreightType | null
  notes: string | null
  /** ID da transportadora (Carrier) responsável pelo frete. */
  carrierId: number | null
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
  id: number
  code: string
  clientType: TechnicalProposalClientType
  clientId: number | null
  clientName: string | null
  clientCode: string | null
  status: TechnicalProposalStatus
  /** Data da proposta (data de emissão, sempre a data atual). */
  startDate: string
  /** Data de entrega, preenchida automaticamente ao concluir. */
  deliveryDate: string | null
  total: number
  paymentCondition: PaymentCondition | null
}

/** Corpo de POST /api/v1/technical-proposals. */
export interface TechnicalProposalCreateRequest {
  customerId?: number | null
  companyId?: number | null
  address?: TechnicalProposalAddressRequest | null
  description?: string | null
  /** Número da revisão da proposta técnica (opcional). */
  revision?: number | null
  /** Nome do responsável técnico (opcional). */
  technicalResponsible?: string | null
  /** E-mail de contato do responsável técnico (opcional, sem validação de formato). */
  email?: string | null
  /** Telefone de contato do responsável técnico (opcional, campo livre). */
  phone?: string | null
  serviceItems?: TechnicalProposalServiceItemRequest[] | null
  productItems?: TechnicalProposalProductItemRequest[] | null
  discountType?: DiscountType | null
  discount?: number | null
  freightValue?: number | null
  deliveryDeadline?: string | null
  paymentCondition?: PaymentCondition | null
  validity?: string | null
  deliveryType?: FreightType | null
  notes?: string | null
  /** ID da transportadora (Carrier) responsável pelo frete. Opcional. */
  carrierId?: number | null
}

/** Corpo de PATCH /api/v1/technical-proposals/{id}. */
export interface TechnicalProposalUpdateRequest {
  customerId?: number | null
  companyId?: number | null
  address?: TechnicalProposalAddressRequest | null
  description?: string | null
  /** Número da revisão da proposta técnica (opcional). Envie nulo para remover. */
  revision?: number | null
  /** Nome do responsável técnico. string vazia = limpar. */
  technicalResponsible?: string | null
  /** E-mail do responsável técnico. string vazia = limpar. */
  email?: string | null
  /** Telefone do responsável técnico. string vazia = limpar. */
  phone?: string | null
  serviceItems?: TechnicalProposalServiceItemRequest[] | null
  productItems?: TechnicalProposalProductItemRequest[] | null
  discountType?: DiscountType | null
  discount?: number | null
  freightValue?: number | null
  deliveryDeadline?: string | null
  paymentCondition?: PaymentCondition | null
  validity?: string | null
  deliveryType?: FreightType | null
  notes?: string | null
  /** ID da transportadora (Carrier). null = remover a transportadora vinculada. */
  carrierId?: number | null
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
  clientId?: number
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