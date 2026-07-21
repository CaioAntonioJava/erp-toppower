/** Tipos do módulo de boletos. Espelham os DTOs do backend. */
import type { RegistrationStatus } from './registration'

// Re-exporta para permitir que callers importem tudo de um lugar.
export type { RegistrationStatus }

/** Resposta de boleto. Espelha br.com.toppower...boleto.dto.BoletoResponse. */
export interface BoletoResponse {
  id: number
  description: string
  payee: string
  value: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dueDate: string
  status: RegistrationStatus
  createdAt: string
  updatedAt: string
  createdBy: string | null
  updatedBy: string | null
}

/** Corpo de POST /api/v1/boletos. */
export interface BoletoCreateRequest {
  description: string
  payee: string
  value: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dueDate: string
  status?: RegistrationStatus
}

/** Corpo de PATCH /api/v1/boletos/{id}. Todos os campos opcionais. */
export interface BoletoUpdateRequest {
  description?: string
  payee?: string
  value?: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dueDate?: string
  status?: RegistrationStatus
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