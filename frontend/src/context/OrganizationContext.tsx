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
  getOrganization,
  listMine as apiListMine,
} from '../api/organization.api'
import { ORGANIZATION_KEY } from '../api/client'
import { useAuth } from './AuthContext'
import type { OrganizationResponse, OrganizationSummary } from '../types/api'

interface OrganizationContextValue {
  /** Organizations acessíveis ao usuário (para o seletor). */
  organizations: OrganizationSummary[]
  /** Organization atualmente ativa (resumo do seletor/login). */
  activeOrganization: OrganizationSummary | null
  /**
   * Organization ativa em sua forma rica (com endereço, telefone,
   * e-mail, logoUrl). Hidratada sob demanda a partir de
   * {@code GET /organizations/{id}}. Pode ser {@code null} durante a
   * hidratação inicial ou se o fetch falhar.
   */
  activeOrganizationRich: OrganizationResponse | null
  /** Recarrega tanto a lista quanto a Organization ativa rica. */
  refreshRich: () => Promise<void>
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
  const [activeOrganizationRich, setActiveOrganizationRich] = useState<OrganizationResponse | null>(null)
  const [revision, setRevision] = useState(0)
  const [isLoading, setLoading] = useState(false)

  /**
   * Hidrata a Organization ativa em sua forma rica. Chamado sempre que
   * {@code activeOrganization} muda (boot, troca de org) e exposto como
   * {@code refreshRich} para que páginas que alteram o logo possam
   * forçar refetch sem precisar trocar a org.
   */
  const refreshRich = useCallback(async () => {
    if (!activeOrganization) {
      setActiveOrganizationRich(null)
      return
    }
    try {
      const rich = await getOrganization(activeOrganization.id)
      setActiveOrganizationRich(rich)
    } catch {
      // Falha silenciosa: o consumidor usa `activeOrganizationRich ?? null`
      // como fallback. Em geral só ocorre se a org foi removida
      // enquanto o usuário estava logado.
      setActiveOrganizationRich(null)
    }
  }, [activeOrganization])

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      const list = await apiListMine()
      setOrganizations(list)
      const storedId = localStorage.getItem(ORGANIZATION_KEY)
      const storedIdNum = storedId ? Number(storedId) : null
      // Resolve a Organization ativa: a do localStorage, senão a default, senão a primeira.
      const active =
        list.find((o) => o.id === storedIdNum) ??
        list.find((o) => o.isDefault) ??
        list[0] ??
        null
      if (active) {
        localStorage.setItem(ORGANIZATION_KEY, String(active.id))
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
      setActiveOrganizationRich(null)
      return
    }
    refresh()
  }, [isAuthenticated, isAuthLoading, refresh])

  // Sempre que a Organization ativa muda (boot ou troca manual),
  // busca os dados ricos (incluindo logo e endereço).
  useEffect(() => {
    if (!activeOrganization) {
      setActiveOrganizationRich(null)
      return
    }
    let cancelled = false
    getOrganization(activeOrganization.id)
      .then((rich) => {
        if (!cancelled) setActiveOrganizationRich(rich)
      })
      .catch(() => {
        if (!cancelled) setActiveOrganizationRich(null)
      })
    return () => { cancelled = true }
  }, [activeOrganization])

  const setActive = useCallback((org: OrganizationSummary) => {
    localStorage.setItem(ORGANIZATION_KEY, String(org.id))
    setActiveOrganization(org)
    // Bumpa o revision para forçar remount do conteúdo (recarrega dados da tela ativa).
    setRevision((r) => r + 1)
  }, [])

  const value = useMemo<OrganizationContextValue>(
    () => ({
      organizations,
      activeOrganization,
      activeOrganizationRich,
      setActive,
      refresh,
      refreshRich,
      revision,
      isLoading,
    }),
    [organizations, activeOrganization, activeOrganizationRich, setActive, refresh, refreshRich, revision, isLoading],
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