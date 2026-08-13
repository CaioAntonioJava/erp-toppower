/**
 * Helpers de data compartilhados.
 *
 * Centraliza funções que eram duplicadas em `BoletoFormModal` e
 * `BoletosListPage`: `todayIso`, `addDaysIso`, `firstOfMonthIso`,
 * `firstOfPrevMonthIso`, `lastOfPrevMonthIso`.
 */

/**
 * Retorna hoje no formato ISO (yyyy-MM-dd).
 */
export function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

/**
 * Retorna (hoje + dias) no formato ISO (yyyy-MM-dd). Dias negativos = passado.
 */
export function addDaysIso(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

/**
 * Primeiro dia do mês atual no formato ISO.
 */
export function firstOfMonthIso(): string {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)
}

/**
 * Primeiro dia do mês anterior no formato ISO.
 */
export function firstOfPrevMonthIso(): string {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth() - 1, 1).toISOString().slice(0, 10)
}

/**
 * Último dia do mês anterior no formato ISO.
 */
export function lastOfPrevMonthIso(): string {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), 0).toISOString().slice(0, 10)
}