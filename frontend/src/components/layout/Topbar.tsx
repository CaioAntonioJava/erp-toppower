import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Building2, ChevronDown, LogOut, UserCircle } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { useOrganization } from '../../context/OrganizationContext'

/** Topbar com seletor de Organization, papel do usuário e botão de logout. */
export function Topbar() {
  const { user, signOut } = useAuth()
  const { organizations, activeOrganization, setActive } = useOrganization()
  const navigate = useNavigate()
  const [orgMenuOpen, setOrgMenuOpen] = useState(false)
  const orgMenuRef = useRef<HTMLDivElement>(null)

  function handleLogout() {
    signOut()
    navigate('/login', { replace: true })
  }

  function handleProfile() {
    navigate('/profile')
  }

  function handleSelectOrg(uuid: string) {
    const org = organizations.find((o) => o.uuid === uuid)
    if (org) {
      setActive(org)
    }
    setOrgMenuOpen(false)
  }

  // Fecha o dropdown ao clicar fora.
  useEffect(() => {
    if (!orgMenuOpen) return
    function handleClick(e: MouseEvent) {
      if (orgMenuRef.current && !orgMenuRef.current.contains(e.target as Node)) {
        setOrgMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [orgMenuOpen])

  const initials = (user?.email ?? '?').slice(0, 2).toUpperCase()
  const roleLabel = user?.role === 'ROLE_ADMIN' ? 'Administrador' : 'Gestor'

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 dark:border-slate-800 dark:bg-slate-900">
      {/* Seletor de Organization */}
      <div className="relative" ref={orgMenuRef}>
        {organizations.length > 0 ? (
          <button
            type="button"
            onClick={() => setOrgMenuOpen((v) => !v)}
            className="inline-flex h-10 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
            aria-label="Selecionar Organization"
            title="Selecionar Organization"
          >
            <Building2 className="h-4 w-4 text-primary" />
            <span className="hidden max-w-[14rem] truncate sm:inline">
              {activeOrganization?.tradeName ?? 'Selecionar Organization'}
            </span>
            <ChevronDown className="h-4 w-4 text-slate-400" />
          </button>
        ) : (
          <div className="inline-flex h-10 items-center gap-2 px-3 text-sm text-slate-400">
            <Building2 className="h-4 w-4" />
            <span className="hidden sm:inline">Sem Organization</span>
          </div>
        )}

        {orgMenuOpen && organizations.length > 0 && (
          <div className="absolute left-0 top-12 z-50 w-72 overflow-hidden rounded-lg border border-slate-200 bg-white shadow-lg dark:border-slate-700 dark:bg-slate-800">
            <div className="border-b border-slate-100 px-3 py-2 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-700 dark:text-slate-400">
              Organizations
            </div>
            <ul className="max-h-72 overflow-y-auto py-1">
              {organizations.map((org) => {
                const isActive = org.uuid === activeOrganization?.uuid
                return (
                  <li key={org.uuid}>
                    <button
                      type="button"
                      onClick={() => handleSelectOrg(org.uuid)}
                      className={`flex w-full items-start gap-2 px-3 py-2 text-left text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-700 ${
                        isActive ? 'bg-primary/5 text-primary' : 'text-slate-700 dark:text-slate-200'
                      }`}
                    >
                      <Building2 className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" />
                      <span className="flex flex-col">
                        <span className="font-medium">{org.tradeName}</span>
                        <span className="text-xs text-slate-500 dark:text-slate-400">
                          {org.corporateName}
                        </span>
                      </span>
                    </button>
                  </li>
                )
              })}
            </ul>
          </div>
        )}
      </div>

      <div className="flex items-center gap-3">
        <div className="hidden items-center gap-2 sm:flex">
          <div className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-semibold text-white">
            {initials}
          </div>
          <div className="flex flex-col leading-tight">
            <span className="text-sm font-medium">{user?.email}</span>
            <span className="text-xs text-slate-500 dark:text-slate-400">
              {roleLabel}
            </span>
          </div>
        </div>

        <button
          type="button"
          onClick={handleProfile}
          className="inline-flex h-10 items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
          aria-label="Meu perfil"
          title="Meu perfil"
        >
          <UserCircle className="h-4 w-4" />
          <span className="hidden sm:inline">Meu perfil</span>
        </button>

        <button
          type="button"
          onClick={handleLogout}
          className="inline-flex h-10 items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
          aria-label="Sair"
          title="Sair"
        >
          <LogOut className="h-4 w-4" />
          <span className="hidden sm:inline">Sair</span>
        </button>
      </div>
    </header>
  )
}