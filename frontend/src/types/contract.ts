/**
 * Tipos do módulo de Contratos. Espelham os DTOs do backend
 * (`br.com.toppower.erp_toppower.contract.dto`).
 *
 * <p>Além dos blocos de texto livre ({@code servicesDescription},
 * {@code productsDescription}), o contrato agora suporta itens
 * estruturados de serviço (apenas descrição) e produto (referência +
 * quantidade), seguindo o mesmo padrão de linhas dinâmicas da
 * Proposta Técnica, porém sem preço/margem.</p>
 *
 * <p>O cliente do contrato pode ser pessoa física ({@code Customer})
 * <b>ou</b> pessoa jurídica ({@code Company}). A invariante "exatamente
 * um entre os dois" é garantida pelo backend.</p>
 */
import type { ClientSummaryResponse } from './quotation'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { ClientSummaryResponse }

// =====================================================================
// Enums próprios
// =====================================================================

/** Ciclo de vida do contrato. Espelha {@code ContractStatus}. */
export type ContractStatus = 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA'

export const CONTRACT_STATUS_LABELS: Record<ContractStatus, string> = {
  ABERTA: 'Aberta',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDA: 'Concluída',
}

/** Tipo do cliente referenciado pelo contrato. Espelha {@code ContractResponse.ClientType}. */
export type ContractClientType = 'CUSTOMER' | 'COMPANY'

export const CONTRACT_CLIENT_TYPE_LABELS: Record<ContractClientType, string> = {
  CUSTOMER: 'Cliente (PF)',
  COMPANY: 'Empresa (PJ)',
}

// =====================================================================
// Endereço (opcional)
// =====================================================================

/** Endereço do contrato (opcional). Variante permissiva: todos os campos
 * podem ser nulos. Quando o objeto inteiro for nulo no request, nenhum
 * endereço é persistido. */
export interface ContractAddressRequest {
  street?: string | null
  number?: string | null
  complement?: string | null
  neighborhood?: string | null
  city?: string | null
  state?: string | null
  zipCode?: string | null
}

export interface ContractAddressResponse {
  street: string | null
  number: string | null
  complement: string | null
  neighborhood: string | null
  city: string | null
  state: string | null
  zipCode: string | null
}

// =====================================================================
// Cláusulas
// =====================================================================

/** Linha de cláusula de um contrato (request). */
export interface ContractClauseRequest {
  description: string
}

/** Linha de cláusula de um contrato (response). */
export interface ContractClauseResponse {
  uuid: string
  description: string
}

// =====================================================================
// Itens de serviço
// =====================================================================

/** Item de serviço de um contrato (request) — apenas descrição. */
export interface ContractServiceItemRequest {
  description: string
}

/** Item de serviço de um contrato (response). */
export interface ContractServiceItemResponse {
  uuid: string
  description: string
}

// =====================================================================
// Itens de produto
// =====================================================================

/** Item de produto de um contrato (request) — referência + quantidade. */
export interface ContractProductItemRequest {
  productUuid: string
  quantity: number
}

/** Item de produto de um contrato (response). */
export interface ContractProductItemResponse {
  uuid: string
  productUuid: string
  quantity: number
}

// =====================================================================
// Header
// =====================================================================

/** Representação completa de um contrato. Espelha {@code ContractResponse}. */
export interface ContractResponse {
  uuid: string
  /** Prefixo do código (ex.: 'CT' ou 'CL'). */
  prefix: string
  /** Numeral sequencial do código (reseta por ano). */
  sequence: number
  /** Ano do código. */
  year: number
  /** Código formatado completo (ex.: 'CT-001-2026'). */
  code: string
  /** UUID do cliente pessoa física contratante. Presente quando {@link clientType} === 'CUSTOMER'. */
  customerUuid: string | null
  /** UUID da empresa (pessoa jurídica) contratante. Presente quando {@link clientType} === 'COMPANY'. */
  companyUuid: string | null
  /** Tipo do cliente referenciado pelo contrato. */
  clientType: ContractClientType
  /** UUID efetivo do cliente (igual a {@link customerUuid} ou {@link companyUuid} conforme o tipo). */
  clientUuid: string | null
  /** Nome de exibição do cliente (resolvido no backend). */
  clientName: string | null
  /** Código interno do cliente (resolvido no backend). */
  clientCode: string | null
  address: ContractAddressResponse | null
  /** Descrição detalhada do contrato (~1000 chars). */
  description: string
  /** Cláusulas contratuais (lista de textos livres). */
  clauses: ContractClauseResponse[]
  /** Bloco de texto descrevendo os serviços (opcional). */
  servicesDescription: string | null
  /** Bloco de texto descrevendo os produtos (opcional). */
  productsDescription: string | null
  /** Itens de serviço estruturados (opcional). */
  serviceItems: ContractServiceItemResponse[]
  /** Itens de produto estruturados (opcional). */
  productItems: ContractProductItemResponse[]
  status: ContractStatus
  startDate: string
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
  /** Valor total do contrato (preenchimento manual). */
  totalValue: number | null
  /** Prazo de entrega do contrato (texto livre, opcional). */
  deliveryDeadline: string | null
  /** Bloco de texto adicional ao final do contrato (opcional, rich text). */
  additionalDescription: string | null
}

/** Resumo do contrato para listagens paginadas. */
export interface ContractSummaryResponse {
  uuid: string
  code: string
  clientType: ContractClientType
  /** UUID efetivo do cliente (PF ou PJ). */
  clientUuid: string | null
  clientName: string | null
  clientCode: string | null
  status: ContractStatus
  startDate: string
  /** Primeiros ~120 caracteres da descrição (preview na listagem). */
  descriptionPreview: string | null
}

/** Corpo de POST /api/v1/contracts. */
export interface ContractCreateRequest {
  /** Obrigatório se {@link companyUuid} não for informado. */
  customerUuid?: string | null
  /** Obrigatório se {@link customerUuid} não for informado. */
  companyUuid?: string | null
  address?: ContractAddressRequest | null
  description: string
  /** Lista de cláusulas contratuais. O contrato deve ter ao menos uma. */
  clauses: ContractClauseRequest[]
  servicesDescription?: string | null
  productsDescription?: string | null
  /** Itens de serviço (opcional). */
  serviceItems?: ContractServiceItemRequest[] | null
  /** Itens de produto (opcional). */
  productItems?: ContractProductItemRequest[] | null
  /** Valor total do contrato (preenchimento manual, opcional). */
  totalValue?: number | null
  /** Prazo de entrega (texto livre, opcional). */
  deliveryDeadline?: string | null
  /** Descrição adicional ao final do contrato (rich text, opcional). */
  additionalDescription?: string | null
  startDate?: string | null
}

/** Corpo de PATCH /api/v1/contracts/{id}. */
export interface ContractUpdateRequest {
  customerUuid?: string | null
  companyUuid?: string | null
  address?: ContractAddressRequest | null
  description?: string | null
  /** Nova lista de cláusulas (substitui a anterior por completo). */
  clauses?: ContractClauseRequest[]
  /** String vazia = limpar. */
  servicesDescription?: string | null
  /** String vazia = limpar. */
  productsDescription?: string | null
  /** Nova lista de itens de serviço (substitui a anterior). */
  serviceItems?: ContractServiceItemRequest[] | null
  /** Nova lista de itens de produto (substitui a anterior). */
  productItems?: ContractProductItemRequest[] | null
  /** Novo valor total. */
  totalValue?: number | null
  /** Novo prazo de entrega (texto livre). null = não alterar, "" = limpar. */
  deliveryDeadline?: string | null
  /** Nova descrição adicional (rich text). null = não alterar, "" = limpar. */
  additionalDescription?: string | null
  startDate?: string | null
}

/** Resposta do endpoint /contracts/next-code. */
export interface NextContractCodeResponse {
  prefix: string
  sequence: number
  year: number
  code: string
}

/** Filtros suportados no endpoint de listagem/busca. */
export interface ContractFilters {
  status?: ContractStatus
  startDate?: string
  endDate?: string
  customerUuid?: string
  code?: string
  page?: number
  size?: number
}