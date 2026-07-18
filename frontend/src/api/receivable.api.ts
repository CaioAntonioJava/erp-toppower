/**
 * API client do módulo de contas a receber.
 *
 * Endpoints backend: /api/v1/accounts-receivable.
 * Mantém a convenção dos demais arquivos `*.api.ts`: funções nomeadas,
 * base path por recurso, tipos em `types/`.
 */
import api from './client'
import type { PagedResponse } from '../types/api'
import type {
  ReceivableCreateRequest,
  ReceivableFilters,
  ReceivablePaymentRequest,
  ReceivablePaymentResponse,
  ReceivableResponse,
  ReceivableSummaryResponse,
  ReceivableUpdateRequest,
} from '../types/receivable'

const BASE = '/api/v1/accounts-receivable'

/** GET /accounts-receivable — listagem paginada com filtros opcionais. */
export async function listReceivables(
  filters: ReceivableFilters = {},
): Promise<PagedResponse<ReceivableSummaryResponse>> {
  const { data } = await api.get<PagedResponse<ReceivableSummaryResponse>>(BASE, {
    params: {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      sort: 'dueDate,asc',
      status: filters.status === 'ALL' ? undefined : filters.status,
      sourceType: filters.sourceType === 'ALL' ? undefined : filters.sourceType,
      clientId: filters.clientId,
      dueFrom: filters.dueFrom,
      dueTo: filters.dueTo,
      query: filters.query,
    },
  })
  return data
}

/** GET /accounts-receivable/{id} — detalhe com histórico de pagamentos. */
export async function getReceivable(id: number): Promise<ReceivableResponse> {
  const { data } = await api.get<ReceivableResponse>(`${BASE}/${id}`)
  return data
}

/** POST /accounts-receivable — cadastro manual. */
export async function createReceivable(
  payload: ReceivableCreateRequest,
): Promise<ReceivableResponse> {
  const { data } = await api.post<ReceivableResponse>(BASE, payload)
  return data
}

/** PATCH /accounts-receivable/{id} — atualização parcial. */
export async function updateReceivable(
  id: number,
  payload: ReceivableUpdateRequest,
): Promise<ReceivableResponse> {
  const { data } = await api.patch<ReceivableResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /accounts-receivable/{id} — cancelamento (soft delete, 204). */
export async function cancelReceivable(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /accounts-receivable/{id}/activate — reativação de conta cancelada. */
export async function activateReceivable(id: number): Promise<ReceivableResponse> {
  const { data } = await api.patch<ReceivableResponse>(`${BASE}/${id}/activate`)
  return data
}

/** POST /accounts-receivable/{id}/payments — registra pagamento avulso. */
export async function registerPayment(
  id: number,
  payload: ReceivablePaymentRequest,
): Promise<ReceivableResponse> {
  const { data } = await api.post<ReceivableResponse>(
    `${BASE}/${id}/payments`,
    payload,
  )
  return data
}

/** DELETE /accounts-receivable/{id}/payments/{paymentId} — remove pagamento. */
export async function removePayment(
  id: number,
  paymentId: number,
): Promise<ReceivableResponse> {
  const { data } = await api.delete<ReceivableResponse>(
    `${BASE}/${id}/payments/${paymentId}`,
  )
  return data
}

/** GET /accounts-receivable/{id}/payments — histórico de pagamentos. */
export async function listPayments(
  id: number,
): Promise<ReceivablePaymentResponse[]> {
  const { data } = await api.get<ReceivablePaymentResponse[]>(
    `${BASE}/${id}/payments`,
  )
  return data
}