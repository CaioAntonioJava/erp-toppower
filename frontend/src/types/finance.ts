/**
 * Tipos do módulo financeiro (dashboard).
 *
 * O tipo {@link BoletoDue} é derivado do boleto cadastrado (módulo
 * `/api/v1/boletos`), reaproveitado pelos widgets `BoletosCadastradosWidget`
 * e `BoletosDueWidget`. Os demais tipos de apresentação (AccountPayable,
 * AccountReceivable, FinanceSummary) foram removidos — os widgets agora
 * consomem diretamente os tipos reais dos módulos (PayableSummaryResponse,
 * ReceivableSummaryResponse) e calculam os totais no frontend.
 */

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
  descricao: string
  /** Beneficiário (pagador), opcional. */
  pagador: string | null
  valor: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dataVencimento: string
  diasAteVencimento: number
  status: 'ABERTO' | 'ATRASADO'
  /** Indica se o boleto foi liquidado (pago). */
  paid: boolean
  /** Data de liquidação, se pago. */
  paymentDate: string | null
  /** Nº de Contrato/Obra vinculado ao boleto (texto livre), se houver. */
  contractWorkNumber: string | null
  /** Data de cadastro do boleto (informável), formato ISO (yyyy-MM-dd). */
  registrationDate: string
}