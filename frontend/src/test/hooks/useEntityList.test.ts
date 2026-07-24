import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor, act } from '@testing-library/react'
import { useEntityList } from '../../hooks/useEntityList'
import type { PagedResponse } from '../../types/api'

const mockFetchAll = vi.fn()
const mockSearch = vi.fn()
const mockInactivate = vi.fn()
const mockActivate = vi.fn()

const api = {
  fetchAll: mockFetchAll,
  search: mockSearch,
  inactivate: mockInactivate,
  activate: mockActivate,
}

const defaultResponse: PagedResponse<{ id: number; status: string }> = {
  content: [
    { id: 1, status: 'ATIVO' },
    { id: 2, status: 'INATIVO' },
  ],
  page: 0,
  size: 10,
  totalElements: 2,
  totalPages: 1,
  first: true,
  last: true,
}

describe('useEntityList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockFetchAll.mockResolvedValue(defaultResponse)
  })

  it('carrega dados na montagem', async () => {
    const { result } = renderHook(() => useEntityList({ api }))

    expect(result.current.loading).toBe(true)

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.items).toHaveLength(2)
    expect(result.current.data?.totalElements).toBe(2)
  })

  it('usa search quando query tem tamanho suficiente', async () => {
    mockSearch.mockResolvedValue({
      content: [{ id: 3, status: 'ATIVO' }],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    })

    const { result } = renderHook(() => useEntityList({ api, minQueryLength: 2 }))

    act(() => {
      result.current.setQuery('teste')
    })

    // Aguarda o debounce de 300ms
    await waitFor(
      () => {
        expect(mockSearch).toHaveBeenCalled()
      },
      { timeout: 500 },
    )

    await waitFor(() => {
      expect(result.current.items).toHaveLength(1)
      expect(result.current.items[0].id).toBe(3)
    })
  })

  it('toggleSelect adiciona e remove ids', () => {
    const { result } = renderHook(() => useEntityList({ api }))

    act(() => {
      result.current.toggleSelect(1)
    })
    expect(result.current.selectedIds.has(1)).toBe(true)

    act(() => {
      result.current.toggleSelect(1)
    })
    expect(result.current.selectedIds.has(1)).toBe(false)
  })

  it('toggleSelectAllVisible seleciona todos visíveis', async () => {
    const { result } = renderHook(() => useEntityList({ api }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    act(() => {
      result.current.toggleSelectAllVisible()
    })

    expect(result.current.selectedIds.has(1)).toBe(true)
    expect(result.current.selectedIds.has(2)).toBe(true)
    expect(result.current.allVisibleSelected).toBe(true)
  })

  it('clearSelection limpa seleção', async () => {
    const { result } = renderHook(() => useEntityList({ api }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    act(() => {
      result.current.toggleSelect(1)
    })
    expect(result.current.hasSelection).toBe(true)

    act(() => {
      result.current.clearSelection()
    })
    expect(result.current.hasSelection).toBe(false)
  })

  it('setStatusFilter reseta página para 0', async () => {
    const { result } = renderHook(() => useEntityList({ api }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    act(() => {
      result.current.setPage(2)
    })

    act(() => {
      result.current.setStatusFilter('ATIVO')
    })

    expect(result.current.page).toBe(0)
  })

  it('lida com erro na requisição', async () => {
    mockFetchAll.mockRejectedValue({
      response: { status: 500, data: { status: 500, message: 'Erro interno', timestamp: '' } },
    })

    const { result } = renderHook(() => useEntityList({ api }))

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.error).toBe('Erro interno')
    expect(result.current.data).toBeNull()
  })
})
