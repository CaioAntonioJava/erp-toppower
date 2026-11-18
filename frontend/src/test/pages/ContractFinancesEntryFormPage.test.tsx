import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ContractFinancesEntryFormPage } from '../../pages/ContractFinancesEntryFormPage'
import type { ContractResponse } from '../../types/contract'

vi.mock('../../api/contractFinance.api', () => ({
  createContractFinanceEntriesBatch: vi.fn(),
  createContractFinanceEntry: vi.fn(),
  getContractFinanceEntry: vi.fn(),
  updateContractFinanceEntry: vi.fn(),
}))

vi.mock('../../api/contract.api', () => ({
  getContract: vi.fn(),
  searchContracts: vi.fn(),
}))

import { createContractFinanceEntriesBatch, getContractFinanceEntry, updateContractFinanceEntry } from '../../api/contractFinance.api'
import { getContract, searchContracts } from '../../api/contract.api'

const contract: ContractResponse = {
  id: 42,
  prefix: 'CL',
  sequence: 1,
  year: 2026,
  code: 'CL-001-2026',
  customerId: 7,
  companyId: null,
  clientType: 'CUSTOMER',
  clientName: 'JOÃO DA SILVA',
  clientCode: 'CLI000001',
  title: 'CONTRATO DE PRESTAÇÃO DE SERVIÇOS: CL-001-2026',
  description: null,
  status: 'ATIVO',
  validityDate: '2026-05-01',
  price: 10000,
  paymentCondition: null,
  deliveryDate: null,
  createdAt: '2026-05-01T10:00:00Z',
  updatedAt: '2026-05-01T10:00:00Z',
  createdBy: null,
  updatedBy: null,
  clauses: [],
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(getContract).mockResolvedValue(contract)
  vi.mocked(searchContracts).mockResolvedValue({
    content: [contract],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
  })
  vi.mocked(createContractFinanceEntriesBatch).mockResolvedValue([
    {
      id: 900,
      controlId: 10,
      contractId: 42,
      contractCode: 'CL-001-2026',
      clientType: 'CUSTOMER',
      clientCode: 'CLI000001',
      clientName: 'JOÃO DA SILVA',
      type: 'COMBUSTIVEL',
      entryDate: '2026-05-10',
      amount: 120,
      description: 'Diesel caminhão 01',
      status: 'ATIVO',
      createdAt: '2026-05-10T10:00:00Z',
      updatedAt: '2026-05-10T10:00:00Z',
      createdBy: null,
      updatedBy: null,
    },
  ])
})

describe('ContractFinancesEntryFormPage (lançamento em lote)', () => {
  it('envia apenas as naturezas preenchidas ao backend', async () => {
    render(
      <MemoryRouter initialEntries={['/contract-finances/new?contractId=42']}>
        <ContractFinancesEntryFormPage />
      </MemoryRouter>,
    )

    // Pré-seleção do contrato via ?contractId carrega a prévia.
    expect(await screen.findByText('CL-001-2026')).toBeInTheDocument()

    // Preenche só combustível; refeição fica em branco de propósito.
    fireEvent.change(screen.getByLabelText('Valor em reais de Combustível'), {
      target: { value: '120,00' },
    })
    fireEvent.change(screen.getByLabelText('Descrição de Combustível'), {
      target: { value: 'Diesel caminhão 01' },
    })

    fireEvent.click(screen.getByRole('button', { name: /Lançar 1 natureza\(s\)/ }))

    await waitFor(() => {
      expect(createContractFinanceEntriesBatch).toHaveBeenCalledTimes(1)
    })
    expect(createContractFinanceEntriesBatch).toHaveBeenCalledWith(
      expect.objectContaining({
        contractId: 42,
        items: [
          expect.objectContaining({
            type: 'COMBUSTIVEL',
            amount: 120,
            description: 'Diesel caminhão 01',
          }),
        ],
      }),
    )
  })

  it('exibe erro quando nenhuma natureza é preenchida e não chama a API', async () => {
    render(
      <MemoryRouter initialEntries={['/contract-finances/new?contractId=42']}>
        <ContractFinancesEntryFormPage />
      </MemoryRouter>,
    )
    await screen.findByText('CL-001-2026')

    fireEvent.click(screen.getByRole('button', { name: 'Lançar' }))

    expect(
      await screen.findByText('Preencha o valor de pelo menos um lançamento para salvar.'),
    ).toBeInTheDocument()
    expect(createContractFinanceEntriesBatch).not.toHaveBeenCalled()
  })

  it('exibe erro na linha com descrição sem valor', async () => {
    render(
      <MemoryRouter initialEntries={['/contract-finances/new?contractId=42']}>
        <ContractFinancesEntryFormPage />
      </MemoryRouter>,
    )
    await screen.findByText('CL-001-2026')

    fireEvent.change(screen.getByLabelText('Descrição de Refeição'), {
      target: { value: 'almoço da equipe' },
    })
    // A descrição preenchida conta como linha a lançar no contador do botão.
    fireEvent.click(screen.getByRole('button', { name: /^Lançar/ }))

    expect(
      await screen.findByText(
        'Preencha o valor para lançar esta natureza (ou apague a descrição).',
      ),
    ).toBeInTheDocument()
    expect(createContractFinanceEntriesBatch).not.toHaveBeenCalled()
  })

  it('carrega o lançamento para edição e envia PATCH individual', async () => {
    const entry = {
      id: 900,
      controlId: 10,
      contractId: 42,
      contractCode: 'CL-001-2026',
      clientType: 'CUSTOMER' as const,
      clientCode: 'CLI000001',
      clientName: 'JOÃO DA SILVA',
      type: 'MATERIAL' as const,
      entryDate: '2026-05-10',
      amount: 300,
      description: 'Cabo 50mm',
      status: 'ATIVO' as const,
      createdAt: '2026-05-10T10:00:00Z',
      updatedAt: '2026-05-10T10:00:00Z',
      createdBy: null,
      updatedBy: null,
    }
    vi.mocked(getContractFinanceEntry).mockResolvedValue(entry)

    render(
      <MemoryRouter initialEntries={['/contract-finances/entries/900']}>
        <Routes>
          <Route path="/contract-finances/entries/:id" element={<ContractFinancesEntryFormPage />} />
        </Routes>
      </MemoryRouter>,
    )

    const naturezaSelect = await screen.findByLabelText('Natureza do lançamento')
    expect((naturezaSelect as HTMLSelectElement).value).toBe('MATERIAL')

    fireEvent.change(naturezaSelect, { target: { value: 'PEDAGIO' } })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    await waitFor(() => {
      expect(updateContractFinanceEntry).toHaveBeenCalledTimes(1)
    })
    expect(updateContractFinanceEntry).toHaveBeenCalledWith(
      900,
      expect.objectContaining({ type: 'PEDAGIO' }),
    )
  })
})
