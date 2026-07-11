/**
 * Tipos do módulo de Contratos. Espelham os DTOs do backend
 * (`br.com.toppower.erp_toppower.contract.dto`).
 *
 * <p>Ao contrário da Proposta Técnica, este módulo é mais simples: o
 * agregado não possui itens estruturados (serviços/produtos) — os
 * blocos opcionais de descrição de serviços e produtos são persistidos
 * como campos {@code TEXT} diretamente no header.</p>
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
  /** Cláusula contratual (texto livre). */
  clause: string
  /** Bloco de texto descrevendo os serviços (opcional). */
  servicesDescription: string | null
  /** Bloco de texto descrevendo os produtos (opcional). */
  productsDescription: string | null
  status: ContractStatus
  startDate: string
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
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
  clause: string
  servicesDescription?: string | null
  productsDescription?: string | null
  startDate?: string | null
}

/** Corpo de PATCH /api/v1/contracts/{id}. */
export interface ContractUpdateRequest {
  customerUuid?: string | null
  companyUuid?: string | null
  address?: ContractAddressRequest | null
  description?: string | null
  clause?: string | null
  /** String vazia = limpar. */
  servicesDescription?: string | null
  /** String vazia = limpar. */
  productsDescription?: string | null
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