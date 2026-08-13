/**
 * Helpers compartilhados do módulo de boletos.
 *
 * Centraliza funções que eram duplicadas em `useBoletosStorage`,
 * `BoletosDueWidget`, `BoletosCadastradosWidget` e `BoletosListPage`:
 * `diasAteVencimento`, `derivarStatus`, `toDue` e `labelBoleto`.
 */
import type { BoletoResponse } from '../types/boleto'
import type { BoletoDue } from '../types/finance'

/**
 * Calcula dias até o vencimento a partir de uma data ISO (negativo = vencido).
 *
 * Usa `T00:00:00` para forçar interpretação como hora local, evitando
 * off-by-one em fusos diferentes de UTC (o bare `new Date('yyyy-MM-dd')`
 * é interpretado como UTC meia-noite, que em fusos negativos vira o dia
 * anterior).
 */
export function diasAteVencimento(dataVencimento: string): number {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const venc = new Date(`${dataVencimento}T00:00:00`)
  venc.setHours(0, 0, 0, 0)
  const msPorDia = 24 * 60 * 60 * 1000
  return Math.round((venc.getTime() - hoje.getTime()) / msPorDia)
}

/**
 * Deriva o status de apresentação a partir dos dias até o vencimento.
 */
export function derivarStatus(dias: number): BoletoDue['status'] {
  return dias < 0 ? 'ATRASADO' : 'ABERTO'
}

/**
 * Converte um {@link BoletoResponse} do backend em {@link BoletoDue}
 * (com campos derivados para apresentação no dashboard).
 */
export function toDue(boleto: BoletoResponse): BoletoDue {
  const dias = diasAteVencimento(boleto.dueDate)
  return {
    id: boleto.id,
    contractWorkNumber: boleto.contractWorkNumber,
    responsibleName: boleto.responsibleName,
    valor: boleto.value,
    dataVencimento: boleto.dueDate,
    diasAteVencimento: dias,
    status: derivarStatus(dias),
    paid: boleto.paid,
    paymentDate: boleto.paymentDate,
    invoiceNumber: boleto.invoiceNumber,
    invoiceDate: boleto.invoiceDate,
    installmentNumber: boleto.installmentNumber,
    installmentPlanId: boleto.installmentPlanId,
    supplierName: boleto.supplierName,
  }
}

/**
 * Monta um rótulo legível para o boleto (nº obra + responsável, se houver).
 */
export function labelBoleto(
  contractWorkNumber: string | null,
  responsibleName: string | null,
): string {
  const obra = contractWorkNumber ?? ''
  const resp = responsibleName ?? ''
  if (obra && resp) return `${obra} · ${resp}`
  return obra || resp || 'Boleto sem identificação'
}