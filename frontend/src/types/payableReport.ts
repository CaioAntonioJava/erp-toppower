import type { PayableSource } from './payable'

/** Granularidade do agrupamento por período no relatório de fluxo. */
export type PayableReportGranularity = 'DAY' | 'WEEK' | 'MONTH'

/** Filtros compartilhados pelos relatórios. */
export interface PayableReportFilters {
  sourceType?: PayableSource | null
  supplierId?: number | null
}

// =====================================================================
// Aging (por parcela)
// =====================================================================

export interface AgingBucket {
  count: number
  balance: number
}

export interface AgingBySupplier {
  supplierId: number
  supplierName: string | null
  supplierTaxId: string | null
  totalBalance: number
  count: number
  bucket0_30: AgingBucket
  bucket31_60: AgingBucket
  bucket61_90: AgingBucket
  bucket90Plus: AgingBucket
}

export interface PayableAgingReportResponse {
  referenceDate: string
  totalOpenBalance: number
  totalOpenCount: number
  bucket0_30: AgingBucket
  bucket31_60: AgingBucket
  bucket61_90: AgingBucket
  bucket90Plus: AgingBucket
  bySupplier: AgingBySupplier[]
}

// =====================================================================
// Flow (pagamentos)
// =====================================================================

export interface FlowByPeriod {
  periodStart: string
  label: string
  paid: number
  paymentCount: number
}

export interface FlowBySupplier {
  supplierId: number
  supplierName: string | null
  totalPaid: number
  paymentCount: number
}

export interface PayableFlowReportResponse {
  from: string
  to: string
  granularity: PayableReportGranularity
  totalPaid: number
  paymentCount: number
  byPeriod: FlowByPeriod[]
  bySupplier: FlowBySupplier[]
}

// =====================================================================
// Posição por fornecedor
// =====================================================================

export interface SupplierPosition {
  supplierId: number
  supplierName: string | null
  supplierTaxId: string | null
  totalToPay: number
  totalPaid: number
  openCount: number
  overdueCount: number
  maxOverdueDays: number
}

export interface PayableSupplierPositionReportResponse {
  referenceDate: string
  suppliers: SupplierPosition[]
}