import type { PaymentCondition } from './quotation'

/** Tipos do módulo de contas a receber. Espelham os DTOs do backend. */

/** Status do ciclo de vida de uma conta a receber. */
export type ReceivableStatus = 'ABERTO' | 'PAGO' | 'CANCELADO'

/** Origem da conta (manual ou gerada por documento de vendas/contrato). */
export type ReceivableSource = 'MANUAL' | 'SALES_ORDER' | 'TECHNICAL_PROPOSAL' | 'CONTRACT'

/** Tipo do cliente devedor: CUSTOMER (PF) ou COMPANY (PJ). */
export type ReceivableClientType = 'CUSTOMER' | 'COMPANY'

/** Parcela programada de uma conta a receber. */
export interface ReceivableInstallmentResponse {
  id: number
  installmentNumber: number
  amount: number
  paidAmount: number
  /** Saldo devedor da parcela (amount - paidAmount). */
  balance: number
  /** Vencimento programado (yyyy-MM-dd). */
  dueDate: string
  status: ReceivableStatus
  /** Data do último pagamento da parcela (yyyy-MM-dd). Nula se não houver. */
  paymentDate: string | null
}

/** Preview de parcela gerada a partir da condição de pagamento. */
export interface ReceivableInstallmentPreviewResponse {
  installmentNumber: number
  amount: number
  dueDate: string
}

/** Pagamento avulso de uma conta a receber. */
export interface ReceivablePaymentResponse {
  id: number
  /** ID da parcela vinculada, se aplicável. */
  installmentId: number | null
  /** Número da parcela vinculada (0 para pagamentos antigos sem parcela). */
  installmentNumber: number
  amount: number
  /** Data do pagamento (yyyy-MM-dd). */
  paymentDate: string
  notes: string | null
  createdAt: string
}

/** Resposta completa de uma conta a receber (detalhe). */
export interface ReceivableResponse {
  id: number
  description: string
  /** Valor total da conta. */
  value: number
  /** Valor já recebido (soma dos pagamentos). */
  paidAmount: number
  /** Saldo devedor (value - paidAmount). */
  balance: number
  /** Vencimento-base (1ª parcela, yyyy-MM-dd). */
  dueDate: string
  status: ReceivableStatus
  sourceType: ReceivableSource
  customerId: number | null
  companyId: number | null
  clientName: string | null
  clientCode: string | null
  paymentCondition: PaymentCondition | null
  /** Quantidade de parcelas programadas. */
  installmentsCount: number
  salesOrderId: number | null
  salesOrderNumber: number | null
  technicalProposalId: number | null
  technicalProposalCode: string | null
  contractId: number | null
  contractCode: string | null
  /** Data do último pagamento (yyyy-MM-dd). Nula se não houver pagamentos. */
  paymentDate: string | null
  /** Parcelas programadas ordenadas por número. */
  installments: ReceivableInstallmentResponse[]
  payments: ReceivablePaymentResponse[]
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Resumo de uma conta a receber para listas paginadas. */
export interface ReceivableSummaryResponse {
  id: number
  description: string
  value: number
  paidAmount: number
  balance: number
  dueDate: string
  status: ReceivableStatus
  sourceType: ReceivableSource
  /** Código do documento de origem (ex.: "CL-001-2026"). Nulo para contas manuais. */
  sourceCode: string | null
  clientName: string | null
  clientCode: string | null
  /** Quantidade de parcelas programadas. */
  installmentsCount: number
  paymentDate: string | null
}

/** Parcela programada informada no cadastro/geração. */
export interface ReceivableInstallmentRequest {
  amount: number
  /** Vencimento programado (yyyy-MM-dd). */
  dueDate: string
}

/** Corpo de POST /api/v1/accounts-receivable (cadastro manual). */
export interface ReceivableCreateRequest {
  description: string
  value: number
  /** Data de emissão (yyyy-MM-dd). Base para cálculo dos vencimentos das parcelas automáticas. */
  issueDate?: string
  /** Vencimento-base / 1ª parcela (yyyy-MM-dd). */
  dueDate: string
  customerId?: number | null
  companyId?: number | null
  paymentCondition?: PaymentCondition | null
  /** Parcelas explícitas. Quando omitido, o backend gera a partir da condição ou 1 parcela à vista. */
  installments?: ReceivableInstallmentRequest[] | null
}

/** Corpo de PATCH /api/v1/accounts-receivable/{id}. Campos opcionais. */
export interface ReceivableUpdateRequest {
  description?: string
  dueDate?: string
  paymentCondition?: PaymentCondition | null
}

/** Corpo de POST /api/v1/accounts-receivable/{id}/installments/{installmentId}/payments. */
export interface ReceivablePaymentRequest {
  amount: number
  /** Data do pagamento (yyyy-MM-dd). */
  paymentDate: string
  notes?: string | null
}

/** Corpo de POST /api/v1/accounts-receivable/{id}/installments/generate (botão Gerar parcelas). */
export interface GenerateInstallmentsRequest {
  paymentCondition?: PaymentCondition | null
  /** Data-base para o cálculo dos vencimentos. Default: vencimento-base da conta. */
  baseDate?: string
  /** Parcelas explícitas (alternativa à paymentCondition). */
  installments?: ReceivableInstallmentRequest[] | null
}

/** Corpo de POST /api/v1/accounts-receivable/installments/preview. */
export interface PreviewInstallmentsRequest {
  paymentCondition: PaymentCondition
  value: number
  /** Data-base. Default: hoje. */
  baseDate?: string
}

/** Filtros suportados na listagem de contas a receber. */
export interface ReceivableFilters {
  status?: ReceivableStatus | 'ALL'
  sourceType?: ReceivableSource | 'ALL'
  clientId?: number
  /** Vencimento a partir de (yyyy-MM-dd). */
  dueFrom?: string
  /** Vencimento até (yyyy-MM-dd). */
  dueTo?: string
  query?: string
  page?: number
  size?: number
}