import type { ReceivableSource } from './receivable'

/** Granularidade do agrupamento por período no relatório de fluxo. */
export type ReceivableReportGranularity = 'DAY' | 'WEEK' | 'MONTH'

/** Filtros compartilhados pelos relatórios. */
export interface ReceivableReportFilters {
  sourceType?: ReceivableSource | null
  clientId?: number | null
}

// =====================================================================
// Aging
// =====================================================================

export interface AgingBucket {
  count: number
  balance: number
}

export interface AgingByClient {
  clientId: number
  clientName: string | null
  clientCode: string | null
  totalBalance: number
  count: number
  bucket0_30: AgingBucket
  bucket31_60: AgingBucket
  bucket61_90: AgingBucket
  bucket90Plus: AgingBucket
}

export interface ReceivableAgingReportResponse {
  referenceDate: string
  totalOpenBalance: number
  totalOpenCount: number
  bucket0_30: AgingBucket
  bucket31_60: AgingBucket
  bucket61_90: AgingBucket
  bucket90Plus: AgingBucket
  byClient: AgingByClient[]
}

// =====================================================================
// Flow (recebimentos)
// =====================================================================

export interface FlowByPeriod {
  periodStart: string
  label: string
  received: number
  paymentCount: number
}

export interface FlowByClient {
  clientId: number
  clientName: string | null
  totalReceived: number
  paymentCount: number
}

export interface ReceivableFlowReportResponse {
  from: string
  to: string
  granularity: ReceivableReportGranularity
  totalReceived: number
  paymentCount: number
  byPeriod: FlowByPeriod[]
  byClient: FlowByClient[]
}

// =====================================================================
// Posição por cliente
// =====================================================================

export interface ClientPosition {
  clientId: number
  clientName: string | null
  clientCode: string | null
  totalToReceive: number
  totalReceived: number
  openCount: number
  overdueCount: number
  maxOverdueDays: number
}

export interface ReceivableClientPositionReportResponse {
  referenceDate: string
  clients: ClientPosition[]
}