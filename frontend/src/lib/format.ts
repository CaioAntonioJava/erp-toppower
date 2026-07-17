/**
 * Formatadores compartilhados de data e moeda.
 *
 * Centraliza a formatação usada nos dashboards e listagens (até agora cada
 * página redefinia seu próprio `formatDate`/`Intl.NumberFormat`). Mantém o
 * padrão `pt-BR` e o fuso `America/Sao_Paulo` adotado no backend.
 */

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

/**
 * Formata um valor numérico como moeda brasileira (R$ 1.234,56).
 * Retorna string vazia para valores nulos/não finitos.
 */
export function formatCurrency(value: number | null | undefined): string {
  if (value == null || !Number.isFinite(value)) return ''
  return currencyFormatter.format(value)
}

/**
 * Formata uma data ISO (ou Date) no padrão brasileiro dd/mm/aaaa.
 * Retorna string vazia para valores nulos/inválidos.
 */
export function formatDate(
  value: string | Date | null | undefined,
): string {
  if (value == null || value === '') return ''
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

/**
 * Formata uma data ISO (ou Date) com hora (dd/mm/aaaa hh:mm).
 * Retorna string vazia para valores nulos/inválidos.
 */
export function formatDateTime(
  value: string | Date | null | undefined,
): string {
  if (value == null || value === '') return ''
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}