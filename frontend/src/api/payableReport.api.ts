/**
 * API client dos relatórios de contas a pagar.
 *
 * Endpoints backend: /api/v1/accounts-payable/reports.
 */
import api from './client'
import type { PayableSource } from '../types/payable'
import type {
  PayableAgingReportResponse,
  PayableFlowReportResponse,
  PayableReportGranularity,
  PayableSupplierPositionReportResponse,
} from '../types/payableReport'

const BASE = '/api/v1/accounts-payable/reports'

export interface AgingReportParams {
  dueTo?: string
  sourceType?: PayableSource | null
  supplierId?: number | null
}

/** GET /accounts-payable/reports/aging — aging de parcelas em aberto por fornecedor. */
export async function getAgingReport(
  params: AgingReportParams = {},
): Promise<PayableAgingReportResponse> {
  const { data } = await api.get<PayableAgingReportResponse>(`${BASE}/aging`, {
    params: {
      dueTo: params.dueTo,
      sourceType: params.sourceType,
      supplierId: params.supplierId,
    },
  })
  return data
}

export interface FlowReportParams {
  from: string
  to: string
  granularity?: PayableReportGranularity
  sourceType?: PayableSource | null
  supplierId?: number | null
}

/** GET /accounts-payable/reports/flow — relatório de pagamentos em um período. */
export async function getFlowReport(
  params: FlowReportParams,
): Promise<PayableFlowReportResponse> {
  const { data } = await api.get<PayableFlowReportResponse>(`${BASE}/flow`, {
    params: {
      from: params.from,
      to: params.to,
      granularity: params.granularity,
      sourceType: params.sourceType,
      supplierId: params.supplierId,
    },
  })
  return data
}

export interface SupplierPositionReportParams {
  dueTo?: string
  sourceType?: PayableSource | null
  supplierId?: number | null
}

/** GET /accounts-payable/reports/supplier-position — posição consolidada por fornecedor. */
export async function getSupplierPositionReport(
  params: SupplierPositionReportParams = {},
): Promise<PayableSupplierPositionReportResponse> {
  const { data } = await api.get<PayableSupplierPositionReportResponse>(
    `${BASE}/supplier-position`,
    {
      params: {
        dueTo: params.dueTo,
        sourceType: params.sourceType,
        supplierId: params.supplierId,
      },
    },
  )
  return data
}