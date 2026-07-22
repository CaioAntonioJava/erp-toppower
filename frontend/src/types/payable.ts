import type { PaymentCondition } from './quotation'

/** Tipos do módulo de contas a pagar. Espelham os DTOs do backend. */

/** Status do ciclo de vida de uma conta a pagar (ou parcela). */
export type PayableStatus = 'ABERTO' | 'PAGO' | 'CANCELADO'

/** Origem da conta (manual, boleto ou nota de compra — futuro). */
export type PayableSource = 'MANUAL' | 'BOLETO' | 'PURCHASE_INVOICE'

/** Parcela programada de uma conta a pagar. */
export interface PayableInstallmentResponse {
  id: number
  installmentNumber: number
  amount: number
  paidAmount: number
  /** Saldo devedor da parcela (amount - paidAmount). */
  balance: number
  /** Vencimento programado (yyyy-MM-dd). */
  dueDate: string
  status: PayableStatus
  /** Data do último pagamento da parcela (yyyy-MM-dd). */
  paymentDate: string | null
}

/** Pagamento avulso de uma conta a pagar (contra uma parcela). */
export interface PayablePaymentResponse {
  id: number
  /** ID da parcela baixada por este pagamento. */
  installmentId: number
  /** Número da parcela baixada (para exibição). */
  installmentNumber: number
  amount: number
  /** Data do pagamento (yyyy-MM-dd). */
  paymentDate: string
  notes: string | null
  /** URL autenticada do comprovante de pagamento. */
  receiptUrl: string | null
  createdAt: string
}

/** Resposta completa de uma conta a pagar (detalhe). */
export interface PayableResponse {
  id: number
  description: string
  /** Valor total da conta. */
  value: number
  /** Valor já pago (soma dos pagamentos). */
  paidAmount: number
  /** Saldo devedor (value - paidAmount). */
  balance: number
  /** Data de emissão (yyyy-MM-dd). */
  issueDate: string
  /** Vencimento-base = 1ª parcela (yyyy-MM-dd). */
  dueDate: string
  status: PayableStatus
  sourceType: PayableSource
  supplierId: number
  supplierName: string | null
  supplierTaxId: string | null
  paymentCondition: PaymentCondition | null
  /** Quantidade de parcelas programadas. */
  installmentsCount: number
  boletoId: number | null
  /** ID da nota de compra de origem (futuro, NF-e XML). */
  purchaseInvoiceId: number | null
  /** Número da nota de compra de origem (futuro). */
  purchaseInvoiceNumber: string | null
  /** Data do último pagamento (yyyy-MM-dd). Nula se não houver pagamentos. */
  paymentDate: string | null
  installments: PayableInstallmentResponse[]
  payments: PayablePaymentResponse[]
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Resumo de uma conta a pagar para listas paginadas. */
export interface PayableSummaryResponse {
  id: number
  description: string
  value: number
  paidAmount: number
  balance: number
  issueDate: string
  dueDate: string
  status: PayableStatus
  sourceType: PayableSource
  supplierId: number
  supplierName: string | null
  supplierTaxId: string | null
  installmentsCount: number
  paymentDate: string | null
}

/** Dados de uma parcela informada no cadastro de uma conta a pagar. */
export interface PayableInstallmentRequest {
  amount: number
  /** Vencimento programado (yyyy-MM-dd). */
  dueDate: string
}

/** Corpo de POST /api/v1/accounts-payable (cadastro manual). */
export interface PayableCreateRequest {
  description: string
  value: number
  /** Data de emissão (yyyy-MM-dd). */
  issueDate: string
  /** Vencimento-base = 1ª parcela (yyyy-MM-dd). Usado quando a lista de
   * parcelas não é informada. */
  dueDate: string
  supplierId: number
  paymentCondition?: PaymentCondition | null
  /** Parcelas explícitas. Quando omitido, é criada uma única parcela à
   * vista com o valor total e o vencimento `dueDate`. */
  installments?: PayableInstallmentRequest[] | null
}

/** Corpo de PATCH /api/v1/accounts-payable/{id}. Campos opcionais. */
export interface PayableUpdateRequest {
  description?: string
  issueDate?: string
  dueDate?: string
  paymentCondition?: PaymentCondition | null
}

/** Corpo de POST /api/v1/accounts-payable/{id}/installments/{installmentId}/payments. */
export interface PayablePaymentRequest {
  amount: number
  /** Data do pagamento (yyyy-MM-dd). */
  paymentDate: string
  notes?: string | null
}

/** Filtros suportados na listagem de contas a pagar. */
export interface PayableFilters {
  status?: PayableStatus | 'ALL'
  sourceType?: PayableSource | 'ALL'
  supplierId?: number
  /** Vencimento a partir de (yyyy-MM-dd). */
  dueFrom?: string
  /** Vencimento até (yyyy-MM-dd). */
  dueTo?: string
  query?: string
  page?: number
  size?: number
}