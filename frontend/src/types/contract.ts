/** Tipos do módulo de contratos. Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus }

/** Status do contrato (mesmo domínio de outros cadastros). */
export type ContractStatus = RegistrationStatus

/** Tipo do cliente: CUSTOMER (PF) ou COMPANY (PJ). */
export type ContractClientType = 'CUSTOMER' | 'COMPANY'

/** Resposta de busca de cliente (PF/PJ) para seleção no contrato.
 *  Espelha br.com.toppower...quotation.dto.ClientSummaryResponse. */
export interface ClientSummaryResponse {
  type: ContractClientType
  id: number
  code: string
  name: string
  document: string
}

/** Resposta de contrato. Espelha br.com.toppower...contract.dto.ContractResponse. */
export interface ContractResponse {
  id: number
  prefix: string
  sequence: number
  year: number
  /** Código formatado completo (ex.: "CL-001-2026"). */
  code: string
  customerId: number | null
  companyId: number | null
  clientType: ContractClientType | null
  clientName: string | null
  clientCode: string | null
  title: string
  description: string | null
  status: ContractStatus
  /** Data de vigência (yyyy-MM-dd). */
  validityDate: string
  /** Preço do contrato (em reais). Valor informativo, não exibido no PDF. */
  price: number
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
  /** Cláusulas do contrato, ordenadas por número. */
  clauses: ContractClauseResponse[]
}

/** Cláusula de contrato (resposta). Espelha ContractClauseResponse do backend. */
export interface ContractClauseResponse {
  id: number
  clauseNumber: number
  title: string
  content: string | null
  serviceTemplateId: number | null
  createdAt: string
  updatedAt: string
}

/** Cláusula de contrato (request). Enviada em create/update. */
export interface ContractClauseRequest {
  clauseNumber: number
  title: string
  content?: string | null
  /** ID do ServiceTemplate (apenas cláusula 1). */
  serviceTemplateId?: number | null
}

/** Corpo de POST /api/v1/contracts. Todos os campos são opcionais — o
 * backend preenche title (default) e description (template da Organization)
 * quando omitidos, e gera o código comercial automaticamente.
 * Deve referenciar exatamente um cliente: customerId (PF) ou companyId (PJ). */
export interface ContractCreateRequest {
  customerId?: number | null
  companyId?: number | null
  title?: string
  description?: string
  /** Data de vigência (yyyy-MM-dd). Quando omitida, o backend usa a data atual. */
  validityDate?: string
  /** Preço do contrato (em reais). Obrigatório na criação. Não exibido no PDF. */
  price?: number
  /** Cláusulas do contrato. Omitir para o backend pré-preencher as 11 padrão. */
  clauses?: ContractClauseRequest[]
}

/** Corpo de PATCH /api/v1/contracts/{id}. Campos opcionais. O código
 * comercial não é alterável. O cliente pode ser alterado. */
export interface ContractUpdateRequest {
  customerId?: number | null
  companyId?: number | null
  title?: string
  description?: string
  status?: ContractStatus
  /** Data de vigência (yyyy-MM-dd). */
  validityDate?: string
  /** Preço do contrato (em reais). Não exibido no PDF. */
  price?: number
  /** Cláusulas. null = manter; lista (inclusive vazia) = substituição completa. */
  clauses?: ContractClauseRequest[] | null
}

/** Resposta de GET /api/v1/contracts/next-code. */
export interface ContractNextCodeResponse {
  prefix: string
  sequence: number
  year: number
  /** Código formatado completo (ex.: "CL-001-2026"). */
  code: string
  /** Título padrão que seria atribuído ao contrato. */
  defaultTitle: string
  /** Data de vigência padrão (data atual, yyyy-MM-dd). */
  defaultValidityDate: string
  /** Descrição padrão (template HTML da Organization ativa). Pode ser null. */
  defaultDescription: string | null
}

/** Filtros suportados na listagem/busca de contratos. */
export interface ContractFilters {
  query?: string
  status?: ContractStatus | 'ALL'
  page?: number
  size?: number
}