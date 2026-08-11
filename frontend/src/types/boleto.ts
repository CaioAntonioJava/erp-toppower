/** Tipos do módulo de boletos. Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus }

/** Resposta de boleto. Espelha br.com.toppower...boleto.dto.BoletoResponse. */
export interface BoletoResponse {
  id: number
  /** Nº da obra/contrato vinculado ao boleto (texto livre), se houver. */
  contractWorkNumber: string | null
  /** Nome do responsável pelo boleto, se houver. */
  responsibleName: string | null
  /** Valor da parcela do boleto. */
  value: number
  /** Data de vencimento da parcela no formato ISO (yyyy-MM-dd). */
  dueDate: string
  status: RegistrationStatus
  /** ID da empresa (fornecedor) vinculada, se houver. */
  supplierId: number | null
  /** Nome de exibição da empresa (fornecedor) vinculada, se houver. */
  supplierName: string | null
  /** Indica se o boleto foi liquidado (pago). */
  paid: boolean
  /** Data de liquidação (pagamento) do boleto, se pago. */
  paymentDate: string | null
  /** Número da nota fiscal vinculada ao boleto, se houver. */
  invoiceNumber: string | null
  /** Data da nota fiscal vinculada ao boleto, se houver. */
  invoiceDate: string | null
  /** Número da parcela do boleto, se houver. */
  installmentNumber: number | null
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/boletos. */
export interface BoletoCreateRequest {
  /** Nº da obra/contrato vinculado ao boleto (texto livre, opcional). */
  contractWorkNumber?: string | null
  /** Nome do responsável pelo boleto (opcional). */
  responsibleName?: string | null
  /** Valor da parcela (ou valor total parcelado). */
  value: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). Ignorada quando
   * installmentsCount > 1 (os vencimentos derivam de installmentTerms). */
  dueDate: string
  status?: RegistrationStatus
  /** ID da empresa (fornecedor) vinculada. Quando informado, o cadastro
   * do boleto dispara a geração automática de uma conta a pagar. */
  supplierId?: number | null
  /** Número da nota fiscal vinculada ao boleto (opcional). */
  invoiceNumber?: string | null
  /** Data da nota fiscal vinculada ao boleto (ISO yyyy-MM-dd, opcional). */
  invoiceDate?: string | null
  /** Número da parcela (manual). Ignorado quando installmentsCount > 1
   * (o número da parcela é gerado automaticamente). */
  installmentNumber?: number | null
  /** Quantidade de parcelas a gerar. Default 1 (boleto avulso). */
  installmentsCount?: number
  /** Prazos das parcelas em dias, separados por barra (ex: "30/60/90").
   * Usado quando installmentsCount > 1. */
  installmentTerms?: string
}

/** Corpo de PATCH /api/v1/boletos/{id}. Todos os campos opcionais. */
export interface BoletoUpdateRequest {
  /** Nº da obra/contrato (string vazia ou null limpa o campo). */
  contractWorkNumber?: string | null
  /** Nome do responsável (string vazia limpa o campo). */
  responsibleName?: string | null
  value?: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dueDate?: string
  status?: RegistrationStatus
  /** ID da empresa (fornecedor) vinculada. */
  supplierId?: number | null
  /** Número da nota fiscal (string vazia limpa o campo). */
  invoiceNumber?: string | null
  /** Data da nota fiscal (ISO yyyy-MM-dd). */
  invoiceDate?: string | null
  /** Número da parcela (manual). */
  installmentNumber?: number | null
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