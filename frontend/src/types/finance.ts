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
  /** Nº da obra/contrato vinculado ao boleto (texto livre), se houver. */
  contractWorkNumber: string | null
  /** Nome do responsável pelo boleto, se houver. */
  responsibleName: string | null
  /** Valor da parcela do boleto. */
  valor: number
  /** Data de vencimento no formato ISO (yyyy-MM-dd). */
  dataVencimento: string
  diasAteVencimento: number
  status: 'ABERTO' | 'ATRASADO'
  /** Indica se o boleto foi liquidado (pago). */
  paid: boolean
  /** Data de liquidação (pagamento), se pago. */
  paymentDate: string | null
  /** Número da nota fiscal vinculada ao boleto, se houver. */
  invoiceNumber: string | null
  /** Data da nota fiscal vinculada ao boleto, se houver. */
  invoiceDate: string | null
  /** Número da parcela do boleto, se houver. */
  installmentNumber: number | null
  /** ID do plano de parcelamento que agrupa as parcelas, se houver. */
  installmentPlanId: string | null
  /** Nome de exibição da empresa (fornecedor) vinculada, se houver. */
  supplierName: string | null
}