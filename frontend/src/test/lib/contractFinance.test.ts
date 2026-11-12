import { describe, it, expect } from 'vitest'
import {
  CONTRACT_FINANCE_TYPES,
  CONTRACT_FINANCE_TYPE_LABELS,
  CONTRACT_FINANCE_STATUS_LABELS,
  contractFinanceTypeBadgeTone,
} from '../../types/contractFinance'

describe('CONTRACT_FINANCE_TYPE_LABELS', () => {
  it('contém rótulo para as 8 naturezas', () => {
    expect(CONTRACT_FINANCE_TYPES).toHaveLength(8)
    expect(CONTRACT_FINANCE_TYPES.every((t) => CONTRACT_FINANCE_TYPE_LABELS[t])).toBe(true)
  })

  it('identifica RECEBIDO como entrada e os demais como gastos', () => {
    expect(CONTRACT_FINANCE_TYPE_LABELS.RECEBIDO).toBe('Recebido')
    expect(CONTRACT_FINANCE_TYPES.filter((t) => t !== 'RECEBIDO')).toEqual([
      'MATERIAL',
      'COMBUSTIVEL',
      'REFEICAO',
      'PEDAGIO',
      'TERCEIROS',
      'OUTROS',
      'SALARIOS',
    ])
  })
})

describe('CONTRACT_FINANCE_STATUS_LABELS', () => {
  it('contém os dois status', () => {
    expect(CONTRACT_FINANCE_STATUS_LABELS.ATIVO).toBe('Ativo')
    expect(CONTRACT_FINANCE_STATUS_LABELS.INATIVO).toBe('Inativo')
    expect(Object.keys(CONTRACT_FINANCE_STATUS_LABELS)).toHaveLength(2)
  })
})

describe('contractFinanceTypeBadgeTone', () => {
  it('usa success para RECEBIDO', () => {
    expect(contractFinanceTypeBadgeTone('RECEBIDO')).toBe('success')
  })

  it('usa danger para mão de obra (Salários/Terceiros)', () => {
    expect(contractFinanceTypeBadgeTone('SALARIOS')).toBe('danger')
    expect(contractFinanceTypeBadgeTone('TERCEIROS')).toBe('danger')
  })

  it('usa warning para os demais gastos', () => {
    expect(contractFinanceTypeBadgeTone('MATERIAL')).toBe('warning')
    expect(contractFinanceTypeBadgeTone('PEDAGIO')).toBe('warning')
    expect(contractFinanceTypeBadgeTone('OUTROS')).toBe('warning')
  })
})
