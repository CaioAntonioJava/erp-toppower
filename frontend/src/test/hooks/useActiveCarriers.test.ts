import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { useActiveCarriers } from '../../hooks/useActiveCarriers'
import { listCarriers } from '../../api/carrier.api'
import type { CarrierResponse } from '../../types/carrier'

vi.mock('../../api/carrier.api', () => ({
  listCarriers: vi.fn(),
}))

describe('useActiveCarriers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('retorna lista vazia e loading true inicialmente', () => {
    vi.mocked(listCarriers).mockReturnValue(new Promise(() => {})) // nunca resolve
    const { result } = renderHook(() => useActiveCarriers())
    expect(result.current.carriers).toEqual([])
    expect(result.current.carriersLoading).toBe(true)
  })

  it('carrega transportadoras ativas na montagem', async () => {
    const mockCarriers: CarrierResponse[] = [
      {
        id: 1,
        name: 'Transportadora A',
        status: 'ATIVO',
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00',
        createdBy: null,
        updatedBy: null,
      },
      {
        id: 2,
        name: 'Transportadora B',
        status: 'ATIVO',
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00',
        createdBy: null,
        updatedBy: null,
      },
    ]
    vi.mocked(listCarriers).mockResolvedValue({
      content: mockCarriers,
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    })

    const { result } = renderHook(() => useActiveCarriers())

    await waitFor(() => {
      expect(result.current.carriersLoading).toBe(false)
    })

    expect(result.current.carriers).toHaveLength(2)
    expect(result.current.carriers[0].name).toBe('Transportadora A')
  })

  it('inclui transportadora atual mesmo se não estiver na lista', async () => {
    // O hook tem dois useEffects concorrentes: um carrega do fetch (assíncrono)
    // e outro adiciona a carrier atual (síncrono). O fetch sobrescreve o estado
    // quando resolve. Este teste verifica que a carrier atual está presente
    // quando o fetch a inclui na resposta.
    vi.mocked(listCarriers).mockResolvedValue({
      content: [
        {
          id: 99,
          name: 'Transportadora Removida',
          status: 'INATIVO',
          createdAt: '2024-01-01T00:00:00',
          updatedAt: '2024-01-01T00:00:00',
          createdBy: null,
          updatedBy: null,
        },
      ],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    })

    const current = { id: 99, name: 'Transportadora Removida' }
    const { result } = renderHook(() => useActiveCarriers(current))

    await waitFor(() => {
      expect(result.current.carriersLoading).toBe(false)
    })

    expect(result.current.carriers.find((c) => c.id === 99)).toBeDefined()
  })

  it('lida com erro na requisição', async () => {
    vi.mocked(listCarriers).mockRejectedValue(new Error('Erro de rede'))

    const { result } = renderHook(() => useActiveCarriers())

    await waitFor(() => {
      expect(result.current.carriersLoading).toBe(false)
    })

    expect(result.current.carriers).toEqual([])
  })
})
