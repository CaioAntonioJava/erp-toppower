import api from './client'
import type { PagedResponse } from '../types/api'
import type {
  NextSalesOrderNumberResponse,
  SalesOrderCreateRequest,
  SalesOrderFilters,
  SalesOrderFromQuotationRequest,
  SalesOrderResponse,
  SalesOrderSummaryResponse,
  SalesOrderUpdateRequest,
} from '../types/salesOrder'

const BASE = '/api/v1/sales-orders'

/** GET /sales-orders — listagem paginada com filtros opcionais. */
export async function listSalesOrders(
  filters: SalesOrderFilters = {},
): Promise<PagedResponse<SalesOrderSummaryResponse>> {
  const { data } = await api.get<PagedResponse<SalesOrderSummaryResponse>>(
    BASE,
    {
      params: {
        page: filters.page ?? 0,
        size: filters.size ?? 20,
        status: filters.status,
        startDate: filters.startDate,
        endDate: filters.endDate,
        clientUuid: filters.clientUuid,
        sellerUuid: filters.sellerUuid,
        number: filters.number,
        quotationNumber: filters.quotationNumber,
      },
    },
  )
  return data
}

/** GET /sales-orders/next-number — pré-visualiza o próximo número. */
export async function getNextSalesOrderNumber(): Promise<number> {
  const { data } = await api.get<NextSalesOrderNumberResponse>(
    `${BASE}/next-number`,
  )
  return data.number
}

/** GET /sales-orders/{id} — detalhe completo (com itens e totais). */
export async function getSalesOrder(id: string): Promise<SalesOrderResponse> {
  const { data } = await api.get<SalesOrderResponse>(`${BASE}/${id}`)
  return data
}

/** GET /sales-orders/by-number/{number} — busca exata por número. */
export async function getSalesOrderByNumber(
  number: number,
): Promise<SalesOrderResponse> {
  const { data } = await api.get<SalesOrderResponse>(
    `${BASE}/by-number/${encodeURIComponent(String(number))}`,
  )
  return data
}

/** POST /sales-orders — cria um novo pedido (direto, sem proposta de origem). */
export async function createSalesOrder(
  payload: SalesOrderCreateRequest,
): Promise<SalesOrderResponse> {
  const { data } = await api.post<SalesOrderResponse>(BASE, payload)
  return data
}

/**
 * POST /sales-orders/from-quotation/{quotationId} — converte uma proposta
 * ATIVA em pedido de venda. O corpo é opcional (overrides); envia `{}` quando
 * não houver sobrescritas, pois o backend aceita `required = false`.
 */
export async function createSalesOrderFromQuotation(
  quotationId: string,
  override?: SalesOrderFromQuotationRequest,
): Promise<SalesOrderResponse> {
  const { data } = await api.post<SalesOrderResponse>(
    `${BASE}/from-quotation/${encodeURIComponent(quotationId)}`,
    override ?? {},
  )
  return data
}

/** PATCH /sales-orders/{id} — atualização parcial. */
export async function updateSalesOrder(
  id: string,
  payload: SalesOrderUpdateRequest,
): Promise<SalesOrderResponse> {
  const { data } = await api.patch<SalesOrderResponse>(
    `${BASE}/${id}`,
    payload,
  )
  return data
}

/**
 * PATCH /sales-orders/{id}/advance-status — avança o status do pedido para
 * o próximo estado do ciclo (ABERTO → FINALIZADO).
 */
export async function advanceSalesOrderStatus(
  id: string,
): Promise<SalesOrderResponse> {
  const { data } = await api.patch<SalesOrderResponse>(
    `${BASE}/${id}/advance-status`,
  )
  return data
}

/**
 * DELETE /sales-orders/{id} — cancelamento (soft). Retorna o pedido
 * atualizado com status CANCELADO.
 */
export async function cancelSalesOrder(
  id: string,
): Promise<SalesOrderResponse> {
  const { data } = await api.delete<SalesOrderResponse>(`${BASE}/${id}`)
  return data
}