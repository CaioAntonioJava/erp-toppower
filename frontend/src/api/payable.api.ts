/**
 * API client do módulo de contas a pagar.
 *
 * Endpoints backend: /api/v1/accounts-payable.
 * Mantém a convenção dos demais arquivos `*.api.ts`: funções nomeadas,
 * base path por recurso, tipos em `types/`.
 */
import api from './client'
import type { PagedResponse } from '../types/api'
import type {
  PayableCreateRequest,
  PayableFilters,
  PayableInstallmentResponse,
  PayablePaymentRequest,
  PayablePaymentResponse,
  PayableResponse,
  PayableSummaryResponse,
  PayableUpdateRequest,
} from '../types/payable'

const BASE = '/api/v1/accounts-payable'

/** GET /accounts-payable — listagem paginada com filtros opcionais. */
export async function listPayables(
  filters: PayableFilters = {},
): Promise<PagedResponse<PayableSummaryResponse>> {
  const { data } = await api.get<PagedResponse<PayableSummaryResponse>>(BASE, {
    params: {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      sort: 'dueDate,asc',
      status: filters.status === 'ALL' ? undefined : filters.status,
      sourceType: filters.sourceType === 'ALL' ? undefined : filters.sourceType,
      supplierId: filters.supplierId,
      dueFrom: filters.dueFrom,
      dueTo: filters.dueTo,
      query: filters.query,
    },
  })
  return data
}

/** GET /accounts-payable/{id} — detalhe com parcelas e histórico de pagamentos. */
export async function getPayable(id: number): Promise<PayableResponse> {
  const { data } = await api.get<PayableResponse>(`${BASE}/${id}`)
  return data
}

/** POST /accounts-payable — cadastro manual. */
export async function createPayable(
  payload: PayableCreateRequest,
): Promise<PayableResponse> {
  const { data } = await api.post<PayableResponse>(BASE, payload)
  return data
}

/** PATCH /accounts-payable/{id} — atualização parcial. */
export async function updatePayable(
  id: number,
  payload: PayableUpdateRequest,
): Promise<PayableResponse> {
  const { data } = await api.patch<PayableResponse>(`${BASE}/${id}`, payload)
  return data
}

/** DELETE /accounts-payable/{id} — cancelamento (soft delete, 204). */
export async function cancelPayable(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}

/** PATCH /accounts-payable/{id}/activate — reativação de conta cancelada. */
export async function activatePayable(id: number): Promise<PayableResponse> {
  const { data } = await api.patch<PayableResponse>(`${BASE}/${id}/activate`)
  return data
}

/** GET /accounts-payable/{id}/installments — parcelas programadas. */
export async function listInstallments(
  id: number,
): Promise<PayableInstallmentResponse[]> {
  const { data } = await api.get<PayableInstallmentResponse[]>(
    `${BASE}/${id}/installments`,
  )
  return data
}

/**
 * POST /accounts-payable/{id}/installments/{installmentId}/payments —
 * registra pagamento avulso contra uma parcela.
 */
export async function registerPayment(
  id: number,
  installmentId: number,
  payload: PayablePaymentRequest,
): Promise<PayableResponse> {
  const { data } = await api.post<PayableResponse>(
    `${BASE}/${id}/installments/${installmentId}/payments`,
    payload,
  )
  return data
}

/**
 * POST /accounts-payable/{id}/installments/{installmentId}/settle —
 * liquidar saldo devedor de uma parcela.
 */
export async function settleInstallment(
  id: number,
  installmentId: number,
): Promise<PayableResponse> {
  const { data } = await api.post<PayableResponse>(
    `${BASE}/${id}/installments/${installmentId}/settle`,
  )
  return data
}

/** POST /accounts-payable/{id}/settle — liquidar todas as parcelas abertas. */
export async function settlePayable(id: number): Promise<PayableResponse> {
  const { data } = await api.post<PayableResponse>(`${BASE}/${id}/settle`)
  return data
}

/** DELETE /accounts-payable/{id}/payments/{paymentId} — remove pagamento. */
export async function removePayment(
  id: number,
  paymentId: number,
): Promise<PayableResponse> {
  const { data } = await api.delete<PayableResponse>(
    `${BASE}/${id}/payments/${paymentId}`,
  )
  return data
}

/** GET /accounts-payable/{id}/payments — histórico de pagamentos. */
export async function listPayments(
  id: number,
): Promise<PayablePaymentResponse[]> {
  const { data } = await api.get<PayablePaymentResponse[]>(
    `${BASE}/${id}/payments`,
  )
  return data
}