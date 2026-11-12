import { describe, it, expect } from 'vitest'
import {
  CASH_FLOW_CLIENT_TYPE_LABELS,
  CASH_FLOW_ENTRY_TYPE_LABELS,
  CASH_FLOW_SOURCE_LABELS,
  cashFlowSourceRoute,
} from '../../types/cashFlow'

describe('cashFlowSourceRoute', () => {
  it('retorna rota do documento quando há navegação prevista', () => {
    expect(cashFlowSourceRoute('SALES_ORDER', 11)).toBe('/sales-orders/11')
    expect(cashFlowSourceRoute('TECHNICAL_PROPOSAL', 22)).toBe('/technical-proposals/22')
    expect(cashFlowSourceRoute('CONTRACT', 33)).toBe('/contracts/33')
  })

  it('retorna null para documentos sem navegação (manual, boleto, NF-e)', () => {
    expect(cashFlowSourceRoute('MANUAL', 1)).toBeNull()
    expect(cashFlowSourceRoute('BOLETO', 2)).toBeNull()
    expect(cashFlowSourceRoute('PURCHASE_INVOICE', 3)).toBeNull()
  })

  it('retorna null quando não há documento de origem', () => {
    expect(cashFlowSourceRoute('SALES_ORDER', null)).toBeNull()
  })
})

describe('rótulos do fluxo de caixa', () => {
  it('contém rótulo para cada tipo de movimento', () => {
    expect(Object.keys(CASH_FLOW_ENTRY_TYPE_LABELS)).toHaveLength(2)
    expect(CASH_FLOW_ENTRY_TYPE_LABELS.ENTRADA).toBe('Entrada')
    expect(CASH_FLOW_ENTRY_TYPE_LABELS.SAIDA).toBe('Saída')
  })

  it('contém rótulo para cada documento de origem', () => {
    expect(Object.keys(CASH_FLOW_SOURCE_LABELS)).toHaveLength(6)
    expect(CASH_FLOW_SOURCE_LABELS.SALES_ORDER).toBe('Pedido de Venda')
    expect(CASH_FLOW_SOURCE_LABELS.TECHNICAL_PROPOSAL).toBe('Proposta Técnica')
    expect(CASH_FLOW_SOURCE_LABELS.CONTRACT).toBe('Contrato')
    expect(CASH_FLOW_SOURCE_LABELS.MANUAL).toBe('Manual')
    expect(CASH_FLOW_SOURCE_LABELS.BOLETO).toBe('Boleto')
    expect(CASH_FLOW_SOURCE_LABELS.PURCHASE_INVOICE).toBe('Nota de Compra (NF-e)')
  })

  it('contém rótulo para cada tipo de cliente', () => {
    expect(Object.keys(CASH_FLOW_CLIENT_TYPE_LABELS)).toHaveLength(2)
    expect(CASH_FLOW_CLIENT_TYPE_LABELS.CUSTOMER).toBe('Pessoa Física (PF)')
    expect(CASH_FLOW_CLIENT_TYPE_LABELS.COMPANY).toBe('Pessoa Jurídica (PJ)')
  })
})
