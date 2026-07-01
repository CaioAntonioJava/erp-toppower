/**
 * Helpers para CPF e CNPJ.
 * Implementa o mesmo algoritmo de validação do backend
 * (DocumentValidator.java) para feedback imediato no formulário.
 *
 * Como o backend agora modela PF e PJ em entidades separadas
 * (Customer e Company, respectivamente), cada uma usa diretamente
 * o helper de CPF ou de CNPJ — não há mais `maskTaxId` polimórfico.
 */

const CPF_FIRST_WEIGHTS = [10, 9, 8, 7, 6, 5, 4, 3, 2] as const
const CPF_SECOND_WEIGHTS = [11, 10, 9, 8, 7, 6, 5, 4, 3, 2] as const
const CNPJ_FIRST_WEIGHTS = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] as const
const CNPJ_SECOND_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] as const

function onlyDigits(value: string): string {
  return value.replace(/\D/g, '')
}

function isAllSame(digits: string): boolean {
  return /^(\d)\1+$/.test(digits)
}

function mod11(digits: string, length: number, weights: readonly number[]): number {
  let sum = 0
  for (let i = 0; i < length; i++) {
    sum += Number(digits.charAt(i)) * weights[i]
  }
  const mod = sum % 11
  return mod < 2 ? 0 : 11 - mod
}

/** Valida um CPF (com ou sem formatação). */
export function isValidCpf(value: string): boolean {
  if (!value) return false
  const digits = onlyDigits(value)
  if (digits.length !== 11) return false
  if (isAllSame(digits)) return false

  const first = mod11(digits, 9, CPF_FIRST_WEIGHTS)
  if (first !== Number(digits.charAt(9))) return false

  const second = mod11(digits, 10, CPF_SECOND_WEIGHTS)
  return second === Number(digits.charAt(10))
}

/** Valida um CNPJ (com ou sem formatação). */
export function isValidCnpj(value: string): boolean {
  if (!value) return false
  const digits = onlyDigits(value)
  if (digits.length !== 14) return false
  if (isAllSame(digits)) return false

  const first = mod11(digits, 12, CNPJ_FIRST_WEIGHTS)
  if (first !== Number(digits.charAt(12))) return false

  const second = mod11(digits, 13, CNPJ_SECOND_WEIGHTS)
  return second === Number(digits.charAt(13))
}

/** Aplica a máscara de CPF (000.000.000-00). */
export function maskCpf(value: string): string {
  const d = onlyDigits(value).slice(0, 11)
  if (d.length <= 3) return d
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`
}

/** Aplica a máscara de CNPJ (00.000.000/0000-00). */
export function maskCnpj(value: string): string {
  const d = onlyDigits(value).slice(0, 14)
  if (d.length <= 2) return d
  if (d.length <= 5) return `${d.slice(0, 2)}.${d.slice(2)}`
  if (d.length <= 8) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5)}`
  if (d.length <= 12)
    return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8)}`
  return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12)}`
}

/** Aplica a máscara de CEP (00000-000). */
export function maskZipCode(value: string): string {
  const d = onlyDigits(value).slice(0, 8)
  if (d.length <= 5) return d
  return `${d.slice(0, 5)}-${d.slice(5)}`
}

/** Aplica máscara de telefone celular/fixo. */
export function maskPhone(value: string): string {
  const d = onlyDigits(value).slice(0, 11)
  if (d.length <= 2) return d
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`
  if (d.length <= 10)
    return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
}
