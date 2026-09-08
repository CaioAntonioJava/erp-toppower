/** Tipos do módulo de Fluxo de Caixa. Espelham os DTOs do backend. */
import type { PagedResponse } from './api'

/** Tipo de movimento do fluxo de caixa (espelha CashFlowEntryType). */
export type CashFlowEntryType = 'ENTRADA' | 'SAIDA'

/** Documento de origem do movimento (espelha CashFlowDocumentSource). */
export type CashFlowDocumentSource =
  | 'MANUAL'
  | 'SALES_ORDER'
  | 'TECHNICAL_PROPOSAL'
  | 'CONTRACT'
  | 'BOLETO'
  | 'PURCHASE_INVOICE'

/** Tipo de cliente (PF/PJ) — aplica-se apenas a entradas. */
export type CashFlowClientType = 'CUSTOMER' | 'COMPANY'

/** Granularidade da série por período. */
export type CashFlowGranularity = 'MONTH' | 'YEAR'

/** Configuração do fluxo de caixa (saldo inicial). Espelha CashFlowSettingsResponse. */
export interface CashFlowSettingsResponse {
  openingBalance: number
  updatedAt: string | null
  updatedBy: string | null
}

/** Payload de atualização do saldo inicial. Espelha CashFlowSettingsRequest. */
export interface CashFlowSettingsRequest {
  openingBalance: number
}

/** Lançamento consolidado do fluxo de caixa. Espelha CashFlowEntryResponse. */
export interface CashFlowEntryResponse {
  id: number
  entryType: CashFlowEntryType
  /** Data do pagamento (yyyy-MM-dd). */
  date: string
  /** Valor (sempre positivo; o sentido é dado por entryType). */
  amount: number
  sourceType: CashFlowDocumentSource
  /** Código formatado do documento de origem (ex.: 'PV-2800-2026'). */
  sourceCode: string | null
  /** Rótulo completo em pt-BR (ex.: 'Pedido de Venda PV-2800-2026'). */
  documentLabel: string
  /** ID do documento de origem, para navegação. */
  sourceDocId: number | null
  clientType: CashFlowClientType | null
  clientId: number | null
  clientName: string | null
  clientCode: string | null
  /** Nome do fornecedor (apenas saídas). */
  counterpartyName: string | null
  description: string | null
  notes: string | null
  /** Saldo real do caixa após o movimento (independe dos filtros). */
  balanceAfter: number
}

/** Linha da série por período (mês/ano). Espelha CashFlowPeriodRow. */
export interface CashFlowPeriodRow {
  /** Início do período (yyyy-MM-dd). */
  periodStart: string
  /** Rótulo de exibição (ex.: '01/2026' ou '2026'). */
  label: string
  entradas: number
  saidas: number
  resultado: number
  count: number
  saldoAcumulado: number
}

/** Resumo consolidado do fluxo de caixa. Espelha CashFlowSummaryResponse. */
export interface CashFlowSummaryResponse {
  startDate: string | null
  endDate: string | null
  openingBalance: number
  saldoAnterior: number
  totalEntradas: number
  totalSaidas: number
  resultado: number
  entradasCount: number
  saidasCount: number
  periods: CashFlowPeriodRow[]
}

/** Filtros dos endpoints /cash-flow/summary e /cash-flow/entries. */
export interface CashFlowFilters {
  startDate?: string
  endDate?: string
  type?: CashFlowEntryType
  sourceType?: CashFlowDocumentSource
  clientType?: CashFlowClientType
  clientId?: number
  query?: string
  page?: number
  size?: number
}

/** Tipagem de lista de lançamentos. */
export type CashFlowEntriesPage = PagedResponse<CashFlowEntryResponse>

/** Rótulos em português dos tipos de movimento. */
export const CASH_FLOW_ENTRY_TYPE_LABELS: Record<CashFlowEntryType, string> = {
  ENTRADA: 'Entrada',
  SAIDA: 'Saída',
}

/** Rótulos em português dos documentos de origem. */
export const CASH_FLOW_SOURCE_LABELS: Record<CashFlowDocumentSource, string> = {
  MANUAL: 'Manual',
  SALES_ORDER: 'Pedido de Venda',
  TECHNICAL_PROPOSAL: 'Proposta Técnica',
  CONTRACT: 'Contrato',
  BOLETO: 'Boleto',
  PURCHASE_INVOICE: 'Nota de Compra (NF-e)',
}

/** Rótulos em português dos tipos de cliente. */
export const CASH_FLOW_CLIENT_TYPE_LABELS: Record<CashFlowClientType, string> = {
  CUSTOMER: 'Pessoa Física (PF)',
  COMPANY: 'Pessoa Jurídica (PJ)',
}

/**
 * Rota do frontend para abrir o documento de origem do lançamento.
 * Retorna null quando não há navegação prevista (manual, boleto, NF-e).
 */
export function cashFlowSourceRoute(
  sourceType: CashFlowDocumentSource,
  sourceDocId: number | null,
): string | null {
  if (sourceDocId == null) return null
  switch (sourceType) {
    case 'SALES_ORDER':
      return `/sales-orders/${sourceDocId}`
    case 'TECHNICAL_PROPOSAL':
      return `/technical-proposals/${sourceDocId}`
    case 'CONTRACT':
      return `/contracts/${sourceDocId}`
    default:
      return null
  }
}