import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import type { ReactNode } from 'react'
import {
  login as apiLogin,
  me as apiMe,
} from '../api/auth.api'
import { getProfileByUserId } from '../api/profile.api'
import { TOKEN_KEY, ORGANIZATION_KEY } from '../api/client'
import { toApiError } from '../lib/errors'
import type {
  AuthenticatedUser,
  LoginRequest,
} from '../types/api'

interface AuthContextValue {
  user: AuthenticatedUser | null
  isAuthenticated: boolean
  isLoading: boolean
  /**
   * Estado do perfil do usuário:
   * - `null`  → ainda não verificado (loading inicial)
   * - `true`  → perfil existe, usuário pode navegar livremente
   * - `false` → perfil NÃO existe, usuário deve completar o cadastro
   */
  hasProfile: boolean | null
  signIn: (payload: LoginRequest) => Promise<AuthenticatedUser>
  signOut: () => void
  /** Força a releitura do usuário a partir do /me. */
  refresh: () => Promise<AuthenticatedUser | null>
  /** Re-checa se o usuário autenticado possui perfil cadastrado. */
  refreshProfileStatus: () => Promise<boolean>
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

/**
 * Verifica se o usuário possui perfil cadastrado.
 * Retorna `true` se existir, `false` se 404.
 * Outros erros propagam para serem tratados pelo caller.
 */
async function checkHasProfile(userId: string): Promise<boolean> {
  try {
    await getProfileByUserId(userId)
    return true
  } catch (err) {
    const apiErr = toApiError(err)
    if (apiErr.status === 404) return false
    throw err
  }
}

/**
 * Usuários com `ROLE_ADMIN` não precisam completar o cadastro de perfil
 * — o papel administrativo já lhes dá acesso completo ao sistema. Essa
 * função centraliza a regra para evitar chamadas ao backend
 * `GET /profiles/user/{id}` e para que o `ProtectedRoute` não redirecione
 * admins para `/profile` indevidamente.
 */
function adminBypassesProfile(role: AuthenticatedUser['role']): boolean {
  return role === 'ROLE_ADMIN'
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [isLoading, setLoading] = useState<boolean>(true)
  const [hasProfile, setHasProfile] = useState<boolean | null>(null)

  // Verifica, na inicialização, se há um token válido em localStorage
  // e, em caso positivo, também checa se o perfil está preenchido.
  // Admins (`ROLE_ADMIN`) pulam a checagem — não precisam de perfil.
  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const u = await loadUserFromToken()
      if (cancelled) return
      setUser(u)
      if (u) {
        if (adminBypassesProfile(u.role)) {
          if (!cancelled) setHasProfile(true)
        } else {
          try {
            const ok = await checkHasProfile(u.uuid)
            if (!cancelled) setHasProfile(ok)
          } catch {
            // Em caso de erro de rede, marca como null para permitir retry
            if (!cancelled) setHasProfile(null)
          }
        }
      } else {
        setHasProfile(null)
      }
      if (!cancelled) setLoading(false)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  /**
   * Re-checa o status do perfil do usuário atual e atualiza o estado.
   * Usado após criar/editar o perfil para liberar a navegação.
   * Admins sempre retornam `true` sem chamar o backend.
   */
  const refreshProfileStatus = useCallback(async (): Promise<boolean> => {
    if (!user) {
      setHasProfile(null)
      return false
    }
    if (adminBypassesProfile(user.role)) {
      setHasProfile(true)
      return true
    }
    try {
      const ok = await checkHasProfile(user.uuid)
      setHasProfile(ok)
      return ok
    } catch {
      // Mantém o estado anterior em caso de falha de rede
      return hasProfile ?? false
    }
  }, [user, hasProfile])

  const signIn = useCallback(
    async (payload: LoginRequest): Promise<AuthenticatedUser> => {
      const response = await apiLogin(payload)
      localStorage.setItem(TOKEN_KEY, response.accessToken)
      setUser(response.user)
      // Pré-seleciona a Organization default do usuário (ou limpa se nenhuma).
      // O OrganizationContext lê essa chave no refresh para definir a ativa.
      if (response.defaultOrganizationId) {
        localStorage.setItem(ORGANIZATION_KEY, response.defaultOrganizationId)
      } else {
        localStorage.removeItem(ORGANIZATION_KEY)
      }
      // Admins não precisam completar perfil — pula a checagem e a
      // chamada ao backend, liberando navegação imediatamente.
      if (adminBypassesProfile(response.user.role)) {
        setHasProfile(true)
        return response.user
      }
      // Verifica o perfil em paralelo ao prosseguir com o login
      setHasProfile(null)
      try {
        const ok = await checkHasProfile(response.user.uuid)
        setHasProfile(ok)
      } catch {
        setHasProfile(null)
      }
      return response.user
    },
    [],
  )

  const signOut = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(ORGANIZATION_KEY)
    setUser(null)
    setHasProfile(null)
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
      hasProfile,
      signIn,
      signOut,
      refresh,
      refreshProfileStatus,
    }),
    [user, isLoading, hasProfile, signIn, signOut, refresh, refreshProfileStatus],
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