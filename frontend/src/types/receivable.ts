/** Tipos do módulo de contas a receber. Espelham os DTOs do backend. */

/** Status do ciclo de vida de uma conta a receber. */
export type ReceivableStatus = 'ABERTO' | 'PAGO' | 'CANCELADO'

/** Origem da conta (manual ou gerada por documento de vendas/contrato). */
export type ReceivableSource = 'MANUAL' | 'SALES_ORDER' | 'TECHNICAL_PROPOSAL' | 'CONTRACT'

/** Tipo do cliente devedor: CUSTOMER (PF) ou COMPANY (PJ). */
export type ReceivableClientType = 'CUSTOMER' | 'COMPANY'

/** Pagamento avulso de uma conta a receber. */
export interface ReceivablePaymentResponse {
  id: number
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
  /** Data de vencimento (yyyy-MM-dd). */
  dueDate: string
  status: ReceivableStatus
  sourceType: ReceivableSource
  customerId: number | null
  companyId: number | null
  clientName: string | null
  clientCode: string | null
  paymentCondition: string | null
  salesOrderId: number | null
  salesOrderNumber: number | null
  technicalProposalId: number | null
  technicalProposalCode: string | null
  contractId: number | null
  contractCode: string | null
  /** Data do último pagamento (yyyy-MM-dd). Nula se não houver pagamentos. */
  paymentDate: string | null
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
  clientName: string | null
  clientCode: string | null
  paymentDate: string | null
}

/** Corpo de POST /api/v1/accounts-receivable (cadastro manual). */
export interface ReceivableCreateRequest {
  description: string
  value: number
  /** Data de vencimento (yyyy-MM-dd). */
  dueDate: string
  customerId?: number | null
  companyId?: number | null
  paymentCondition?: string | null
}

/** Corpo de PATCH /api/v1/accounts-receivable/{id}. Campos opcionais. */
export interface ReceivableUpdateRequest {
  description?: string
  dueDate?: string
  paymentCondition?: string | null
}

/** Corpo de POST /api/v1/accounts-receivable/{id}/payments. */
export interface ReceivablePaymentRequest {
  amount: number
  /** Data do pagamento (yyyy-MM-dd). */
  paymentDate: string
  notes?: string | null
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