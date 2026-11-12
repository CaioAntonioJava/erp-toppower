import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ContractFinancesListPage } from '../../pages/ContractFinancesListPage'
import type {
  ContractFinanceControlsPage,
  ContractFinanceControlResponse,
} from '../../types/contractFinance'

vi.mock('../../api/contractFinance.api', () => ({
  listContractFinancesControls: vi.fn(),
  getContractFinancesControl: vi.fn(),
  listContractFinanceEntries: vi.fn(),
  getContractFinanceEntry: vi.fn(),
  createContractFinanceEntry: vi.fn(),
  updateContractFinanceEntry: vi.fn(),
  inactivateContractFinanceEntry: vi.fn(),
  activateContractFinanceEntry: vi.fn(),
  getContractFinanceSummary: vi.fn(),
}))

import { listContractFinancesControls } from '../../api/contractFinance.api'

function control(overrides: Partial<ContractFinanceControlResponse>): ContractFinanceControlResponse {
  return {
    id: 10,
    contractId: 42,
    contractCode: 'CL-001-2026',
    clientType: 'COMPANY',
    clientCode: 'EMP000001',
    clientName: 'TOP POWER MATÉRIAIS',
    contractDate: '2026-05-01',
    contractValue: 10000,
    totalRecebido: 500,
    totalGasto: 200,
    resultado: 300,
    entriesCount: 2,
    createdAt: '2026-05-01T10:00:00Z',
    updatedAt: '2026-05-01T10:00:00Z',
    createdBy: null,
    updatedBy: null,
    ...overrides,
  }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ContractFinancesListPage', () => {
  it('renderiza a tabela de controles com os totais agregados', async () => {
    const page: ContractFinanceControlsPage = {
      content: [control({})],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    }
    vi.mocked(listContractFinancesControls).mockResolvedValue(page)

    render(
      <MemoryRouter>
        <ContractFinancesListPage />
      </MemoryRouter>,
    )

    expect(
      await screen.findByRole('heading', { name: 'Financeiro de Contratos' }),
    ).toBeInTheDocument()
    expect(screen.getByText('CL-001-2026')).toBeInTheDocument()
    expect(screen.getByText('TOP POWER MATÉRIAIS')).toBeInTheDocument()
    expect(screen.getByText('Contratos (1)')).toBeInTheDocument()
    // Valores formatados em BRL.
    expect(screen.getByText('R$ 10.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 500,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 200,00')).toBeInTheDocument()
  })

  it('exibe mensagem de erro quando a listagem falha', async () => {
    vi.mocked(listContractFinancesControls).mockRejectedValue({
      response: { status: 500, data: { status: 500, message: 'Erro interno no servidor' } },
    })

    render(
      <MemoryRouter>
        <ContractFinancesListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Erro interno no servidor')).toBeInTheDocument()
  })

  it('busca por código do contrato após digitar (debounce)', async () => {
    const page: ContractFinanceControlsPage = {
      content: [control({ contractCode: 'CT-009-2026', clientName: 'OBRA CENTRAL', id: 11, contractId: 43 })],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    }
    vi.mocked(listContractFinancesControls).mockResolvedValue(page)

    render(
      <MemoryRouter>
        <ContractFinancesListPage />
      </MemoryRouter>,
    )
    await screen.findByText('CT-009-2026')
    expect(listContractFinancesControls).toHaveBeenCalledWith(
      expect.objectContaining({ query: '', page: 0 }),
    )

    fireEvent.change(screen.getByLabelText('Buscar contrato ou cliente'), {
      target: { value: 'CT-009' },
    })

    await waitFor(() => {
      expect(listContractFinancesControls).toHaveBeenLastCalledWith(
        expect.objectContaining({ query: 'CT-009', page: 0 }),
      )
    })
  })
})
