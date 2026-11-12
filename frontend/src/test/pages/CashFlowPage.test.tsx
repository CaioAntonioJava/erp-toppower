import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CashFlowPage } from '../../pages/CashFlowPage'
import type {
  CashFlowEntriesPage,
  CashFlowEntryResponse,
  CashFlowSettingsResponse,
  CashFlowSummaryResponse,
} from '../../types/cashFlow'
import {
  getCashFlowSettings,
  getCashFlowSummary,
  listCashFlowEntries,
  updateCashFlowSettings,
} from '../../api/cashFlow.api'

vi.mock('../../api/cashFlow.api', () => ({
  getCashFlowSettings: vi.fn(),
  updateCashFlowSettings: vi.fn(),
  getCashFlowSummary: vi.fn(),
  listCashFlowEntries: vi.fn(),
  searchCashFlowClients: vi.fn(),
}))

const settings: CashFlowSettingsResponse = {
  openingBalance: 1000,
  updatedAt: '2026-05-01T10:00:00Z',
  updatedBy: 'admin@toppower.com.br',
}

const summary: CashFlowSummaryResponse = {
  startDate: '2026-05-01',
  endDate: '2026-05-31',
  openingBalance: 1000,
  saldoAnterior: 1000,
  totalEntradas: 500,
  totalSaidas: 300,
  resultado: 200,
  entradasCount: 1,
  saidasCount: 1,
  periods: [
    {
      periodStart: '2026-05-01',
      label: '05/2026',
      entradas: 500,
      saidas: 300,
      resultado: 200,
      count: 2,
      saldoAcumulado: 1200,
    },
  ],
}

const entrada: CashFlowEntryResponse = {
  id: 950,
  entryType: 'ENTRADA',
  date: '2026-05-10',
  amount: 500,
  sourceType: 'SALES_ORDER',
  sourceCode: 'PV-0001-2026',
  documentLabel: 'Pedido de Venda PV-0001-2026',
  sourceDocId: 11,
  clientType: 'CUSTOMER',
  clientId: 1,
  clientName: 'JOÃO DA SILVA',
  clientCode: 'CLI000001',
  counterpartyName: null,
  description: 'Pedido de Venda',
  notes: 'PIX recebido',
  balanceAfter: 1300,
}

const entriesPage: CashFlowEntriesPage = {
  content: [entrada],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(getCashFlowSettings).mockResolvedValue(settings)
  vi.mocked(getCashFlowSummary).mockResolvedValue(summary)
  vi.mocked(listCashFlowEntries).mockResolvedValue(entriesPage)
  vi.mocked(updateCashFlowSettings).mockResolvedValue(settings)
})

describe('CashFlowPage', () => {
  it('renderiza os cartões do resumo, a série por período e os lançamentos', async () => {
    render(<CashFlowPage />)

    expect(
      await screen.findByRole('heading', { name: 'Fluxo de Caixa' }),
    ).toBeInTheDocument()

    // Cartões do resumo (valores formatados em BRL).
    expect(screen.getAllByText('R$ 1.000,00').length).toBeGreaterThanOrEqual(1)
    // "Saldo inicial" aparece no botão do cabeçalho e no cartão.
    expect(screen.getAllByText('Saldo inicial').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText('Saldo anterior')).toBeInTheDocument()
    expect(screen.getAllByText('R$ 500,00').length).toBeGreaterThanOrEqual(1)
    expect(screen.getAllByText('R$ 300,00').length).toBeGreaterThanOrEqual(1)
    expect(screen.getAllByText('R$ 200,00').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('Saldo final')).toBeInTheDocument()

    // Série por período (mensal).
    expect(screen.getByText('05/2026')).toBeInTheDocument()
    expect(screen.getByText('Saldo do caixa')).toBeInTheDocument()

    // Lançamentos: link para o documento de origem e cliente resolvido.
    expect(screen.getByText('Lançamentos (1)')).toBeInTheDocument()
    const link = screen.getByRole('link', { name: 'Pedido de Venda PV-0001-2026' })
    expect(link).toHaveAttribute('href', '/sales-orders/11')
    expect(screen.getByText('JOÃO DA SILVA')).toBeInTheDocument()
  })

  it('exibe mensagem de erro quando o resumo falha', async () => {
    vi.mocked(getCashFlowSummary).mockRejectedValue({
      response: { status: 500, data: { status: 500, message: 'Erro interno no servidor' } },
    })

    render(<CashFlowPage />)

    expect(await screen.findByText('Erro interno no servidor')).toBeInTheDocument()
  })

  it('altera o saldo inicial pelo modal', async () => {
    render(<CashFlowPage />)

    await screen.findByText('Lançamentos (1)')

    fireEvent.click(screen.getByRole('button', { name: 'Saldo inicial' }))

    const input = await screen.findByLabelText('Saldo inicial do caixa em reais')
    expect(input).toHaveValue('1000,00')

    fireEvent.change(input, { target: { value: '1500,00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => {
      expect(vi.mocked(updateCashFlowSettings)).toHaveBeenCalledWith({
        openingBalance: 1500,
      })
    })
  })
})
