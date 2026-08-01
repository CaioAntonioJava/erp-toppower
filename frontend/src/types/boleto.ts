/** Tipos do módulo de boletos. Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus }

/** Resposta de boleto. Espelha br.com.toppower...boleto.dto.BoletoResponse. */
export interface BoletoResponse {
  id: number
  description: string
  /** Beneficiário do boleto (opcional). */
  payee: string | null
  value: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dueDate: string
  status: RegistrationStatus
  /** ID do fornecedor vinculado, se houver. */
  supplierId: number | null
  /** Nome de exibição do fornecedor vinculado, se houver. */
  supplierName: string | null
  /** Indica se o boleto foi liquidado (pago). */
  paid: boolean
  /** Data de liquidação do boleto, se pago. */
  paymentDate: string | null
  /** Nº de Contrato/Obra vinculado ao boleto (texto livre), se houver. */
  contractWorkNumber: string | null
  /** Data de cadastro do boleto (informável), formato ISO (yyyy-MM-dd). */
  registrationDate: string
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/boletos. */
export interface BoletoCreateRequest {
  description: string
  /** Beneficiário do boleto (opcional). */
  payee?: string | null
  value: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). Ignorada quando
   * installmentsCount > 1 (os vencimentos derivam de installmentTerms). */
  dueDate: string
  status?: RegistrationStatus
  /** ID do fornecedor vinculado. Quando informado, o cadastro do boleto
   * dispara a geração automática de uma conta a pagar. */
  supplierId?: number | null
  /** Nº de Contrato/Obra vinculado ao boleto (texto livre, opcional). */
  contractWorkNumber?: string | null
  /** Data de cadastro do boleto (informável). Default: data atual. */
  registrationDate?: string
  /** Quantidade de parcelas a gerar. Default 1 (boleto avulso). */
  installmentsCount?: number
  /** Prazos das parcelas em dias, separados por barra (ex: "30/60/90").
   * Usado quando installmentsCount > 1. */
  installmentTerms?: string
}

/** Corpo de PATCH /api/v1/boletos/{id}. Todos os campos opcionais. */
export interface BoletoUpdateRequest {
  description?: string
  /** Beneficiário do boleto (string vazia ou null limpa o campo). */
  payee?: string | null
  value?: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dueDate?: string
  status?: RegistrationStatus
  /** ID do fornecedor vinculado. */
  supplierId?: number | null
  /** Nº de Contrato/Obra vinculado (string vazia limpa o campo). */
  contractWorkNumber?: string | null
  /** Data de cadastro do boleto (ISO yyyy-MM-dd). */
  registrationDate?: string
}

/** Filtros suportados na listagem/busca de boletos. */
export interface BoletoFilters {
  query?: string
  status?: RegistrationStatus | 'ALL'
  page?: number
  size?: number
}

/** Resposta de um anexo de boleto. Espelha BoletoAttachmentResponse do backend. */
export interface BoletoAttachmentResponse {
  id: number
  boletoId: number
  fileName: string
  contentType: string
  sizeBytes: number
  /** URL autenticada para baixar/exibir o anexo. */
  publicUrl: string
  createdAt: string
  createdBy: string | null
}