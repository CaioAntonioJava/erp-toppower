/**
 * Helpers monetários compartilhados.
 *
 * Centraliza `parseNumber` (aceita vírgula ou ponto como separador
 * decimal) e `formatBRLValue` (formata para o padrão brasileiro com 2
 * casas decimais), usados em formulários que lidam com valores em reais.
 */

/**
 * Converte a string do input em número, ou `null` se vazia/inválida.
 *
 * Aceita os dois formatos brasileiros comuns:
 *  - com separador de milhar e vírgula decimal: "1.500,00"
 *  - apenas vírgula decimal: "1500,00"
 *
 * Quando há vírgula, os pontos são tratados como separador de milhar
 * (removidos) e a vírgula vira o separador decimal. Sem vírgula, o ponto
 * é interpretado como separador decimal (formato inglês) ou inteiro.
 */
export function parseNumber(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  let normalized: string
  if (trimmed.includes(',')) {
    // Formato brasileiro: "." separa milhares, "," separa decimais.
    normalized = trimmed.replace(/\./g, '').replace(',', '.')
  } else {
    // Sem vírgula: "." como decimal (inglês) ou valor inteiro.
    normalized = trimmed
  }
  const n = Number(normalized)
  return Number.isFinite(n) ? n : null
}

/**
 * Formata um valor (número ou string) para o padrão monetário brasileiro
 * com 2 casas decimais usando vírgula. Ex.: 80 → "80,00"; 45,9 → "45,90".
 * Retorna string vazia se o valor for vazio/inválido.
 */
export function formatBRLValue(
  value: number | string | null | undefined,
): string {
  if (value == null) return ''
  const n = typeof value === 'string' ? parseNumber(value) : value
  if (n == null || !Number.isFinite(n)) return ''
  return n.toFixed(2).replace('.', ',')
}