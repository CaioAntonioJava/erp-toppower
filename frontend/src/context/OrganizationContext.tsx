import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import type { ReactNode } from 'react'
import { listMine as apiListMine } from '../api/organization.api'
import { ORGANIZATION_KEY } from '../api/client'
import { useAuth } from './AuthContext'
import type { OrganizationSummary } from '../types/api'

interface OrganizationContextValue {
  /** Organizations acessíveis ao usuário (para o seletor). */
  organizations: OrganizationSummary[]
  /** Organization atualmente ativa (lida do localStorage), ou null. */
  activeOrganization: OrganizationSummary | null
  /** Altera a Organization ativa. Persiste no localStorage e bumpa revision,
   * forçando o remount do conteúdo da aplicação para recarregar todos os dados. */
  setActive: (org: OrganizationSummary) => void
  /** Recarrega a lista de Organizations a partir do backend. */
  refresh: () => Promise<void>
  /** Incrementado a cada troca de Organization. Usar como `key` no wrapper do
   * conteúdo para forçar remount (recarrega dados, limpa caches, etc.). */
  revision: number
  /** Indica que a lista de Organizations está sendo carregada. */
  isLoading: boolean
}

const OrganizationContext = createContext<OrganizationContextValue | undefined>(undefined)

export function OrganizationProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth()
  const [organizations, setOrganizations] = useState<OrganizationSummary[]>([])
  const [activeOrganization, setActiveOrganization] = useState<OrganizationSummary | null>(null)
  const [revision, setRevision] = useState(0)
  const [isLoading, setLoading] = useState(false)

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      const list = await apiListMine()
      setOrganizations(list)
      const storedId = localStorage.getItem(ORGANIZATION_KEY)
      // Resolve a Organization ativa: a do localStorage, senão a default, senão a primeira.
      const active =
        list.find((o) => o.uuid === storedId) ??
        list.find((o) => o.isDefault) ??
        list[0] ??
        null
      if (active) {
        localStorage.setItem(ORGANIZATION_KEY, active.uuid)
      } else {
        localStorage.removeItem(ORGANIZATION_KEY)
      }
      setActiveOrganization(active)
    } finally {
      setLoading(false)
    }
  }, [])

  // Carrega as Organizations assim que o usuário está autenticado.
  // Roda no boot (token persistido) e após o login.
  useEffect(() => {
    if (isAuthLoading || !isAuthenticated) {
      setOrganizations([])
      setActiveOrganization(null)
      return
    }
    refresh()
  }, [isAuthenticated, isAuthLoading, refresh])

  const setActive = useCallback((org: OrganizationSummary) => {
    localStorage.setItem(ORGANIZATION_KEY, org.uuid)
    setActiveOrganization(org)
    // Bumpa o revision para forçar remount do conteúdo (recarrega dados da tela ativa).
    setRevision((r) => r + 1)
  }, [])

  const value = useMemo<OrganizationContextValue>(
    () => ({
      organizations,
      activeOrganization,
      setActive,
      refresh,
      revision,
      isLoading,
    }),
    [organizations, activeOrganization, setActive, refresh, revision, isLoading],
  )

  return (
    <OrganizationContext.Provider value={value}>
      {children}
    </OrganizationContext.Provider>
  )
}

export function useOrganization(): OrganizationContextValue {
  const ctx = useContext(OrganizationContext)
  if (!ctx) {
    throw new Error('useOrganization deve ser usado dentro de <OrganizationProvider>.')
  }
  return ctx
}