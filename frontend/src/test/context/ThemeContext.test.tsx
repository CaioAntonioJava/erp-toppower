import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useTheme, ThemeProvider } from '../../context/ThemeContext'
import type { ReactNode } from 'react'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value }),
    removeItem: vi.fn((key: string) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
  }
})()

Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

function wrapper({ children }: { children: ReactNode }) {
  return <ThemeProvider>{children}</ThemeProvider>
}

describe('ThemeContext', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
    document.documentElement.classList.remove('dark')
  })

  it('inicia com tema light por padrão', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })
    expect(result.current.theme).toBe('light')
  })

  it('toggle alterna entre light e dark', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })

    act(() => {
      result.current.toggle()
    })
    expect(result.current.theme).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    act(() => {
      result.current.toggle()
    })
    expect(result.current.theme).toBe('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('setTheme define tema específico', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })

    act(() => {
      result.current.setTheme('dark')
    })
    expect(result.current.theme).toBe('dark')

    act(() => {
      result.current.setTheme('light')
    })
    expect(result.current.theme).toBe('light')
  })

  it('persiste tema no localStorage', () => {
    const { result } = renderHook(() => useTheme(), { wrapper })

    act(() => {
      result.current.setTheme('dark')
    })
    expect(localStorageMock.setItem).toHaveBeenCalledWith('erp_toppower_theme', 'dark')
  })

  it('lança erro se usado fora do provider', () => {
    expect(() => renderHook(() => useTheme())).toThrow(
      'useTheme deve ser usado dentro de <ThemeProvider>.',
    )
  })
})
