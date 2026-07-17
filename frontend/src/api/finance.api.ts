/**
 * Stubs da API do módulo financeiro.
 *
 * Os endpoints backend ainda não existem (não há módulo financeiro no
 * backend). Estas funções devolvem listas vazias para que o dashboard
 * renderize estados "vazio" enquanto a integração não é feita. Quando os
 * endpoints `/api/v1/accounts-payable`, `/api/v1/accounts-receivable` e
 * `/api/v1/boletos` forem implementados, basta substituir o corpo das
 * funções por chamadas `api.get(...)` mantendo os tipos de retorno.
 *
 * Mantém a convenção dos demais arquivos `*.api.ts`: funções nomeadas,
 * base path por recurso, tipos em `types/`.
 */
import api from './client'
import type {
  AccountPayable,
  AccountReceivable,
  BoletoDue,
  FinanceSummary,
} from '../types/finance'

const PAYABLE_BASE = '/api/v1/accounts-payable'
const RECEIVABLE_BASE = '/api/v1/accounts-receivable'
const BOLETO_BASE = '/api/v1/boletos'
const SUMMARY_BASE = '/api/v1/finance/summary'

/** Lista contas a pagar em aberto. */
export async function listAccountsPayableOpen(): Promise<AccountPayable[]> {
  // TODO(finance): integrar com backend.
  // return api.get<AccountPayable[]>(`${PAYABLE_BASE}/open`).then(r => r.data)
  void api
  void PAYABLE_BASE
  return []
}

/** Lista contas a receber em aberto. */
export async function listAccountsReceivableOpen(): Promise<AccountReceivable[]> {
  // TODO(finance): integrar com backend.
  // return api.get<AccountReceivable[]>(`${RECEIVABLE_BASE}/open`).then(r => r.data)
  void api
  void RECEIVABLE_BASE
  return []
}

/** Lista boletos próximos do vencimento (próximos 7 dias) e vencidos. */
export async function listBoletosDue(): Promise<BoletoDue[]> {
  // TODO(finance): integrar com backend.
  // return api.get<BoletoDue[]>(`${BOLETO_BASE}/due`).then(r => r.data)
  void api
  void BOLETO_BASE
  return []
}

/** Resumo agregado do módulo financeiro (indicadores do dashboard). */
export async function getFinanceSummary(): Promise<FinanceSummary> {
  // TODO(finance): integrar com backend.
  // return api.get<FinanceSummary>(SUMMARY_BASE).then(r => r.data)
  void api
  void SUMMARY_BASE
  return {
    totalPagarAberto: 0,
    totalPagarVencido: 0,
    totalReceberAberto: 0,
    totalReceberVencido: 0,
    boletosProximosVencimento: 0,
    boletosVencidos: 0,
  }
}