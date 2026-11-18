/** Tipos do módulo Financeiro de Contratos. Espelham os DTOs do backend. */
import type { ContractClientType } from './contract'
import type { PagedResponse } from './api'

/** Natureza do lançamento (espelha ContractFinanceEntryType). */
export type ContractFinanceEntryType =
  | 'RECEBIDO'
  | 'MATERIAL'
  | 'COMBUSTIVEL'
  | 'REFEICAO'
  | 'PEDAGIO'
  | 'TERCEIROS'
  | 'OUTROS'
  | 'SALARIOS'

/** Status do lançamento (espelha ContractFinanceEntryStatus). */
export type ContractFinanceEntryStatus = 'ATIVO' | 'INATIVO'

/** Controle do financeiro de um contrato (espelha ContractFinanceControlResponse). */
export interface ContractFinanceControlResponse {
  id: number
  contractId: number
  /** Código comercial formatado do contrato (ex.: 'CL-001-2026'). */
  contractCode: string
  clientType: ContractClientType | null
  clientCode: string | null
  clientName: string | null
  /** Data de vigência do contrato (yyyy-MM-dd). */
  contractDate: string | null
  /** Valor (preço) do contrato. */
  contractValue: number | null
  /** Total recebido nos lançamentos ATIVO. */
  totalRecebido: number
  /** Total de gastos nos lançamentos ATIVO. */
  totalGasto: number
  /** Resultado: recebido − gasto. */
  resultado: number
  /** Quantidade de lançamentos ATIVO. */
  entriesCount: number
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Lançamento do financeiro de contrato (espelha ContractFinanceEntryResponse). */
export interface ContractFinanceEntryResponse {
  id: number
  controlId: number
  contractId: number
  contractCode: string
  clientType: ContractClientType | null
  clientCode: string | null
  clientName: string
  type: ContractFinanceEntryType
  /** Data da movimentação (yyyy-MM-dd). */
  entryDate: string
  /** Valor (sempre positivo; o sentido é dado pelo type). */
  amount: number
  description: string | null
  status: ContractFinanceEntryStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/contract-finances/entries. */
export interface ContractFinanceEntryCreateRequest {
  contractId: number
  type: ContractFinanceEntryType
  /** Data da movimentação (yyyy-MM-dd). */
  entryDate: string
  amount: number
  description?: string
}

/** Um item do lote: natureza preenchida com valor/descrição. */
export interface ContractFinanceBatchItemRequest {
  type: ContractFinanceEntryType
  amount: number
  description?: string
}

/** Corpo de POST /api/v1/contract-finances/entries/batch (contrato e data compartilhados). */
export interface ContractFinanceEntryBatchCreateRequest {
  contractId: number
  entryDate: string
  items: ContractFinanceBatchItemRequest[]
}

/** Corpo de PATCH /api/v1/contract-finances/entries/{id}. Campos opcionais. */
export interface ContractFinanceEntryUpdateRequest {
  type?: ContractFinanceEntryType
  entryDate?: string
  amount?: number
  description?: string
}

/** Resumo consolidado (por filtros) espelhando ContractFinancesSummaryResponse. */
export interface ContractFinancesSummaryResponse {
  totalRecebido: number
  totalGasto: number
  resultado: number
  entriesCount: number
  porTipo: ContractFinancesTypeTotal[]
}

/** Total por natureza (aninhado em ContractFinancesSummaryResponse). */
export interface ContractFinancesTypeTotal {
  type: ContractFinanceEntryType
  total: number
  count: number
}

/** Filtros de listagem de lançamentos. */
export interface ContractFinanceEntryFilters {
  controlId?: number
  type?: ContractFinanceEntryType
  status?: ContractFinanceEntryStatus
  from?: string
  to?: string
  page?: number
  size?: number
}

/** Tipagem de listas paginadas. */
export type ContractFinanceControlsPage = PagedResponse<ContractFinanceControlResponse>
export type ContractFinanceEntriesPage = PagedResponse<ContractFinanceEntryResponse>

/** Rótulos em português das naturezas do lançamento. */
export const CONTRACT_FINANCE_TYPE_LABELS: Record<ContractFinanceEntryType, string> = {
  RECEBIDO: 'Recebido',
  MATERIAL: 'Material',
  COMBUSTIVEL: 'Combustível',
  REFEICAO: 'Refeição',
  PEDAGIO: 'Pedágio',
  TERCEIROS: 'Terceiros',
  OUTROS: 'Outros',
  SALARIOS: 'Salários',
}

/** Rótulos em português do status do lançamento. */
export const CONTRACT_FINANCE_STATUS_LABELS: Record<ContractFinanceEntryStatus, string> = {
  ATIVO: 'Ativo',
  INATIVO: 'Inativo',
}

/** Todas as naturezas na ordem do enum do backend (para selects e breakdown). */
export const CONTRACT_FINANCE_TYPES: ContractFinanceEntryType[] = [
  'RECEBIDO',
  'MATERIAL',
  'COMBUSTIVEL',
  'REFEICAO',
  'PEDAGIO',
  'TERCEIROS',
  'OUTROS',
  'SALARIOS',
]

/**
 * Tone do Badge para a natureza: RECEBIDO (entrada) em verde; gastos em
 * vermelho/vermelho-forte conforme a categoria.
 */
export function contractFinanceTypeBadgeTone(
  type: ContractFinanceEntryType,
): 'success' | 'danger' | 'warning' | 'neutral' {
  if (type === 'RECEBIDO') return 'success'
  if (type === 'SALARIOS' || type === 'TERCEIROS') return 'danger'
  return 'warning'
}
