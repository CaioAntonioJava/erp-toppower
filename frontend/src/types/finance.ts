/**
 * Tipos do módulo financeiro (dashboard).
 *
 * Partes ainda sem backend dedicado (contas a pagar/receber, resumo
 * agregado) permanecem como estruturas de apresentação usadas pelos
 * widgets. O tipo {@link BoletoDue} agora é derivado do boleto
 * cadastrado (módulo `/api/v1/boletos`), reaproveitado pelos widgets
 * `BoletosCadastradosWidget` e `BoletosDueWidget`.
 */

/** Status de uma conta a pagar/receber (apresentação no dashboard). */
export type AccountStatus = 'ABERTO' | 'PAGO' | 'ATRASADO' | 'CANCELADO'

/** Conta a pagar (despesa em aberto). Endpoint ainda não existe no backend. */
export interface AccountPayable {
  id: number
  descricao: string
  fornecedor: string
  valor: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dataVencimento: string
  status: AccountStatus
}

/** Conta a receber (recebimento em aberto). Endpoint ainda não existe no backend. */
export interface AccountReceivable {
  id: number
  descricao: string
  cliente: string
  valor: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dataVencimento: string
  status: AccountStatus
}

/** Resumo agregado do módulo financeiro (indicadores do dashboard). */
export interface FinanceSummary {
  totalPagarAberto: number
  totalPagarVencido: number
  totalReceberAberto: number
  totalReceberVencido: number
  boletosProximosVencimento: number
  boletosVencidos: number
}

/**
 * Boleto cadastrado com campos derivados para apresentação no dashboard.
 *
 * Espelha {@link BoletoResponse} (do módulo `/api/v1/boletos`) e adiciona
 * `diasAteVencimento` (negativo = vencido) e `status` de apresentação
 * (`ATRASADO` quando vencido, `ABERTO` caso contrário). Esses campos
 * derivados são calculados no frontend a partir de `dueDate`.
 */
export interface BoletoDue {
  id: number
  numeroDocumento: string
  pagador: string
  valor: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dataVencimento: string
  diasAteVencimento: number
  status: 'ABERTO' | 'ATRASADO'
}