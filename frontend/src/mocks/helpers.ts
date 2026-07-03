/**
 * Helpers internos dos mocks. **NÃO** exportar para fora de `src/mocks/`.
 *
 * Os mocks são exclusivamente para desenvolvimento/teste manual — nada
 * aqui deve ir para produção. Os CPFs/CNPJs gerados passam no algoritmo
 * mod-11 espelhado em `lib/documents.ts`, então podem ser submetidos
 * direto pelo frontend caso o backend esteja rodando.
 */

/** Pesos do primeiro e segundo dígitos verificadores do CPF. */
const CPF_FIRST_WEIGHTS = [10, 9, 8, 7, 6, 5, 4, 3, 2] as const
const CPF_SECOND_WEIGHTS = [11, 10, 9, 8, 7, 6, 5, 4, 3, 2] as const

/** Pesos do CNPJ (13 e 14 dígitos). */
const CNPJ_FIRST_WEIGHTS = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] as const
const CNPJ_SECOND_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] as const

function mod11(digits: readonly number[], weights: readonly number[]): number {
  let sum = 0
  for (let i = 0; i < weights.length; i++) sum += digits[i] * weights[i]
  const mod = sum % 11
  return mod < 2 ? 0 : 11 - mod
}

/**
 * Recebe 9 dígitos (CPF base) e devolve os 11 dígitos com verificadores.
 * Garante que o resultado NÃO é uma sequência repetida (ex.: 111.111.111-11).
 */
function buildCpf(base: readonly number[]): string {
  if (base.length !== 9) throw new Error('CPF base deve ter 9 dígitos')
  const allSame = base.every((d) => d === base[0])
  if (allSame) throw new Error('CPF base não pode ter todos dígitos iguais')
  const dv1 = mod11(base, CPF_FIRST_WEIGHTS)
  const dv2 = mod11([...base, dv1], CPF_SECOND_WEIGHTS)
  return [...base, dv1, dv2].join('')
}

/** Recebe 12 dígitos (CNPJ base) e devolve os 14 dígitos com verificadores. */
function buildCnpj(base: readonly number[]): string {
  if (base.length !== 12) throw new Error('CNPJ base deve ter 12 dígitos')
  const allSame = base.every((d) => d === base[0])
  if (allSame) throw new Error('CNPJ base não pode ter todos dígitos iguais')
  const dv1 = mod11(base, CNPJ_FIRST_WEIGHTS)
  const dv2 = mod11([...base, dv1], CNPJ_SECOND_WEIGHTS)
  return [...base, dv1, dv2].join('')
}

/** Formata CPF: 000.000.000-00 */
export function formatCpf(digits: string): string {
  const d = digits.padStart(11, '0')
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9, 11)}`
}

/** Formata CNPJ: 00.000.000/0000-00 */
export function formatCnpj(digits: string): string {
  const d = digits.padStart(14, '0')
  return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12, 14)}`
}

/** Formata telefone: (00) 00000-0000 ou (00) 0000-0000 */
export function formatPhone(digits: string): string {
  const d = digits.padStart(11, '0')
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7, 11)}`
}

/** Formata CEP: 00000-000 */
export function formatZip(digits: string): string {
  const d = digits.padStart(8, '0')
  return `${d.slice(0, 5)}-${d.slice(5, 8)}`
}

/** Gera um CPF válido (11 dígitos sem pontuação) a partir de 9 dígitos-base. */
export function digitsCpf(seed: readonly number[]): string {
  return buildCpf(seed)
}

/** Gera um CNPJ válido (14 dígitos sem pontuação) a partir de 12 dígitos-base. */
export function digitsCnpj(seed: readonly number[]): string {
  return buildCnpj(seed)
}

/** Gera um CPF já formatado (000.000.000-00) a partir de 9 dígitos-base. */
export function makeCpf(seed: readonly number[]): string {
  return formatCpf(buildCpf(seed))
}

/** Gera um CNPJ já formatado (00.000.000/0000-00) a partir de 12 dígitos-base. */
export function makeCnpj(seed: readonly number[]): string {
  return formatCnpj(buildCnpj(seed))
}

/**
 * Timestamp ISO fixado em uma data estável para que os mocks
 * sejam determinísticos entre reloads (útil para snapshots de teste).
 */
export const SEED_TIMESTAMP = '2025-06-01T12:00:00Z'

/** Data/Hora local formatada no padrão brasileiro — usada em `createdBy` etc. */
export const SEED_AUTHOR = 'seed@toppower.local'