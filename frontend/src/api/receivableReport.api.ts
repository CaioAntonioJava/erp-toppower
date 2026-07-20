/**
 * API client dos relatórios de contas a receber.
 *
 * Endpoints backend: /api/v1/accounts-receivable/reports.
 */
import api from './client'
import type { ReceivableSource } from '../types/receivable'
import type {
  ReceivableAgingReportResponse,
  ReceivableClientPositionReportResponse,
  ReceivableFlowReportResponse,
  ReceivableReportGranularity,
} from '../types/receivableReport'

const BASE = '/api/v1/accounts-receivable/reports'

export interface AgingReportParams {
  dueTo?: string
  sourceType?: ReceivableSource | null
  clientId?: number | null
}

/** GET /accounts-receivable/reports/aging — relatório aging de contas em aberto. */
export async function getAgingReport(
  params: AgingReportParams = {},
): Promise<ReceivableAgingReportResponse> {
  const { data } = await api.get<ReceivableAgingReportResponse>(`${BASE}/aging`, {
    params: {
      dueTo: params.dueTo,
      sourceType: params.sourceType,
      clientId: params.clientId,
    },
  })
  return data
}

export interface FlowReportParams {
  from: string
  to: string
  granularity?: ReceivableReportGranularity
  sourceType?: ReceivableSource | null
  clientId?: number | null
}

/** GET /accounts-receivable/reports/flow — relatório de recebimentos em um período. */
export async function getFlowReport(
  params: FlowReportParams,
): Promise<ReceivableFlowReportResponse> {
  const { data } = await api.get<ReceivableFlowReportResponse>(`${BASE}/flow`, {
    params: {
      from: params.from,
      to: params.to,
      granularity: params.granularity,
      sourceType: params.sourceType,
      clientId: params.clientId,
    },
  })
  return data
}

export interface ClientPositionReportParams {
  dueTo?: string
  sourceType?: ReceivableSource | null
  clientId?: number | null
}

/** GET /accounts-receivable/reports/client-position — posição consolidada por cliente. */
export async function getClientPositionReport(
  params: ClientPositionReportParams = {},
): Promise<ReceivableClientPositionReportResponse> {
  const { data } = await api.get<ReceivableClientPositionReportResponse>(
    `${BASE}/client-position`,
    {
      params: {
        dueTo: params.dueTo,
        sourceType: params.sourceType,
        clientId: params.clientId,
      },
    },
  )
  return data
}