import { describe, it, expect } from 'vitest'
import { formatCurrency, formatDate, formatDateTime } from '../../lib/format'

describe('formatCurrency', () => {
  it('formata valor inteiro', () => {
    expect(formatCurrency(1500)).toBe('R$ 1.500,00')
  })

  it('formata valor com centavos', () => {
    expect(formatCurrency(45.9)).toBe('R$ 45,90')
  })

  it('formata zero', () => {
    expect(formatCurrency(0)).toBe('R$ 0,00')
  })

  it('retorna vazio para null', () => {
    expect(formatCurrency(null)).toBe('')
  })

  it('retorna vazio para undefined', () => {
    expect(formatCurrency(undefined)).toBe('')
  })

  it('retorna vazio para Infinity', () => {
    expect(formatCurrency(Infinity)).toBe('')
  })
})

describe('formatDate', () => {
  it('formata data ISO', () => {
    expect(formatDate('2026-07-24T10:00:00Z')).toBe('24/07/2026')
  })

  it('formata objeto Date', () => {
    const d = new Date(2026, 6, 24) // mês 0-indexed
    expect(formatDate(d)).toBe('24/07/2026')
  })

  it('retorna vazio para null', () => {
    expect(formatDate(null)).toBe('')
  })

  it('retorna vazio para undefined', () => {
    expect(formatDate(undefined)).toBe('')
  })

  it('retorna vazio para string vazia', () => {
    expect(formatDate('')).toBe('')
  })

  it('retorna vazio para data inválida', () => {
    expect(formatDate('data-invalida')).toBe('')
  })
})

describe('formatDateTime', () => {
  it('formata data ISO com hora', () => {
    const result = formatDateTime('2026-07-24T10:30:00Z')
    expect(result).toContain('24/07/2026')
    // O jsdom usa UTC, então 10:30 UTC é 07:30 BRT
    expect(result).toContain(':30')
  })

  it('retorna vazio para null', () => {
    expect(formatDateTime(null)).toBe('')
  })

  it('retorna vazio para undefined', () => {
    expect(formatDateTime(undefined)).toBe('')
  })

  it('retorna vazio para string vazia', () => {
    expect(formatDateTime('')).toBe('')
  })

  it('retorna vazio para data inválida', () => {
    expect(formatDateTime('invalida')).toBe('')
  })
})
