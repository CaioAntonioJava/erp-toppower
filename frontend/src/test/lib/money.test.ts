import { describe, it, expect } from 'vitest'
import { parseNumber, formatBRLValue } from '../../lib/money'

describe('parseNumber', () => {
  it('retorna null para string vazia', () => {
    expect(parseNumber('')).toBeNull()
  })

  it('retorna null para string com apenas espaços', () => {
    expect(parseNumber('   ')).toBeNull()
  })

  it('parseia formato brasileiro com vírgula', () => {
    expect(parseNumber('1.500,00')).toBe(1500)
  })

  it('parseia formato brasileiro sem milhar', () => {
    expect(parseNumber('1500,00')).toBe(1500)
  })

  it('parseia formato inglês com ponto', () => {
    expect(parseNumber('1500.50')).toBe(1500.5)
  })

  it('parseia valor inteiro', () => {
    expect(parseNumber('80')).toBe(80)
  })

  it('retorna null para string inválida', () => {
    expect(parseNumber('abc')).toBeNull()
  })

  it('retorna null para NaN', () => {
    expect(parseNumber('NaN')).toBeNull()
  })

  it('parseia valor com centavos no formato BR', () => {
    expect(parseNumber('45,90')).toBe(45.9)
  })

  it('parseia valor com centavos no formato US', () => {
    expect(parseNumber('45.90')).toBe(45.9)
  })
})

describe('formatBRLValue', () => {
  it('formata número inteiro', () => {
    expect(formatBRLValue(80)).toBe('80,00')
  })

  it('formata número com centavos', () => {
    expect(formatBRLValue(45.9)).toBe('45,90')
  })

  it('formata string no formato BR', () => {
    expect(formatBRLValue('1.500,00')).toBe('1500,00')
  })

  it('formata string no formato US', () => {
    expect(formatBRLValue('1500.50')).toBe('1500,50')
  })

  it('retorna vazio para null', () => {
    expect(formatBRLValue(null)).toBe('')
  })

  it('retorna vazio para undefined', () => {
    expect(formatBRLValue(undefined)).toBe('')
  })

  it('retorna vazio para string inválida', () => {
    expect(formatBRLValue('abc')).toBe('')
  })

  it('retorna vazio para Infinity', () => {
    expect(formatBRLValue(Infinity)).toBe('')
  })

  it('formata zero', () => {
    expect(formatBRLValue(0)).toBe('0,00')
  })
})
