import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import type { ReactNode } from 'react'
import { login as apiLogin, me as apiMe } from '../api/auth.api'
import { TOKEN_KEY } from '../api/client'
import type {
  AuthenticatedUser,
  LoginRequest,
  RegisterRequest,
} from '../types/api'
import { register as apiRegister } from '../api/user.api'

interface AuthContextValue {
  user: AuthenticatedUser | null
  isAuthenticated: boolean
  isLoading: boolean
  signIn: (payload: LoginRequest) => Promise<AuthenticatedUser>
  signUp: (payload: RegisterRequest) => Promise<void>
  signOut: () => void
  /** Força a releitura do usuário a partir do /me. */
  refresh: () => Promise<AuthenticatedUser | null>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

/** Carrega o usuário a partir do token persistido, validando com /me. */
async function loadUserFromToken(): Promise<AuthenticatedUser | null> {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return null
  try {
    return await apiMe()
  } catch {
    localStorage.removeItem(TOKEN_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [isLoading, setLoading] = useState<boolean>(true)

  // Verifica, na inicialização, se há um token válido em localStorage.
  useEffect(() => {
    let cancelled = false
    loadUserFromToken().then((u) => {
      if (!cancelled) {
        setUser(u)
        setLoading(false)
      }
    })
    return () => {
      cancelled = true
    }
  }, [])

  const signIn = useCallback(
    async (payload: LoginRequest): Promise<AuthenticatedUser> => {
      const response = await apiLogin(payload)
      localStorage.setItem(TOKEN_KEY, response.accessToken)
      setUser(response.user)
      return response.user
    },
    [],
  )

  const signUp = useCallback(async (payload: RegisterRequest) => {
    await apiRegister(payload)
  }, [])

  const signOut = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  const refresh = useCallback(async () => {
    const u = await loadUserFromToken()
    setUser(u)
    return u
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: !!user,
      isLoading,
      signIn,
      signUp,
      signOut,
      refresh,
    }),
    [user, isLoading, signIn, signUp, signOut, refresh],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de <AuthProvider>.')
  }
  return ctx
}
