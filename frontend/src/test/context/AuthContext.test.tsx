import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useAuth, AuthProvider } from '../../context/AuthContext'
import type { ReactNode } from 'react'

// Mocks
const mockLogin = vi.fn()
const mockMe = vi.fn()
const mockGetProfileByUserId = vi.fn()

vi.mock('../../api/auth.api', () => ({
  login: (...args: unknown[]) => mockLogin(...args),
  me: (...args: unknown[]) => mockMe(...args),
}))

vi.mock('../../api/profile.api', () => ({
  getProfileByUserId: (...args: unknown[]) => mockGetProfileByUserId(...args),
}))

const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value }),
    removeItem: vi.fn((key: string) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
    get length() { return Object.keys(store).length },
    key: vi.fn((i: number) => Object.keys(store)[i] ?? null),
  }
})()

Object.defineProperty(window, 'localStorage', { value: localStorageMock })

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

const mockUser = { id: 1, email: 'user@test.com', role: 'ROLE_MANAGER', modules: [] }
const mockLoginResponse = {
  accessToken: 'token123',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: mockUser,
  organizations: [],
  defaultOrganizationId: 1,
}

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorageMock.clear()
    mockMe.mockRejectedValue(new Error('No token'))
  })

  it('inicia com isLoading true e user null', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    expect(result.current.isLoading).toBe(true)
    expect(result.current.user).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })

  it('signIn autentica o usuário', async () => {
    mockLogin.mockResolvedValue(mockLoginResponse)
    mockGetProfileByUserId.mockResolvedValue({ id: 1 })

    const { result } = renderHook(() => useAuth(), { wrapper })

    // Aguarda o loading inicial terminar
    await waitFor(() => expect(result.current.isLoading).toBe(false))

    let user
    await act(async () => {
      user = await result.current.signIn({ email: 'user@test.com', password: '123' })
    })

    expect(user).toEqual(mockUser)
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.hasSelectedOrganization).toBe(false)
    expect(localStorageMock.setItem).toHaveBeenCalledWith('erp_toppower_token', 'token123')
  })

  it('signOut limpa sessão', async () => {
    mockLogin.mockResolvedValue(mockLoginResponse)
    mockGetProfileByUserId.mockResolvedValue({ id: 1 })

    const { result } = renderHook(() => useAuth(), { wrapper })

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    await act(async () => {
      await result.current.signIn({ email: 'user@test.com', password: '123' })
    })

    act(() => {
      result.current.signOut()
    })

    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('erp_toppower_token')
  })

  it('admin bypassa verificação de perfil', async () => {
    const adminUser = { id: 2, email: 'admin@test.com', role: 'ROLE_ADMIN', modules: [] }
    mockLogin.mockResolvedValue({
      ...mockLoginResponse,
      user: adminUser,
    })

    const { result } = renderHook(() => useAuth(), { wrapper })

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    await act(async () => {
      await result.current.signIn({ email: 'admin@test.com', password: '123' })
    })

    expect(result.current.hasProfile).toBe(true)
    // Não deve ter chamado getProfileByUserId para admin
    expect(mockGetProfileByUserId).not.toHaveBeenCalled()
  })

  it('markOrganizationSelected libera navegação', async () => {
    mockLogin.mockResolvedValue(mockLoginResponse)
    mockGetProfileByUserId.mockResolvedValue({ id: 1 })

    const { result } = renderHook(() => useAuth(), { wrapper })

    await waitFor(() => expect(result.current.isLoading).toBe(false))

    await act(async () => {
      await result.current.signIn({ email: 'user@test.com', password: '123' })
    })

    expect(result.current.hasSelectedOrganization).toBe(false)

    act(() => {
      result.current.markOrganizationSelected()
    })

    expect(result.current.hasSelectedOrganization).toBe(true)
  })

  it('lança erro se usado fora do provider', () => {
    expect(() => renderHook(() => useAuth())).toThrow(
      'useAuth deve ser usado dentro de <AuthProvider>.',
    )
  })
})
