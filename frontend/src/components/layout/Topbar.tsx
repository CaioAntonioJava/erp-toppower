import { useEffect, useRef, useState } from 'react'
import { Building2, Check, ChevronDown, LogOut, UserCircle } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { toApiError } from '../../lib/errors'

/** Topbar com empresa ativa, papel do usuário e botão de logout. */
export function Topbar() {
  const { user, signOut, switchTenant } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const [switching, setSwitching] = useState(false)
  const [switchError, setSwitchError] = useState<string | null>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  function handleLogout() {
    signOut()
    navigate('/login', { replace: true })
  }

  function handleProfile() {
    navigate('/profile')
  }

  const initials = (user?.email ?? '?').slice(0, 2).toUpperCase()
  const roleLabel = user?.role === 'ROLE_ADMIN' ? 'Administrador' : 'Gestor'

  // A empresa (tenant) ativa é a que corresponde ao tenantUuid do JWT.
  // user.tenants traz todas as empresas às quais o usuário tem acesso.
  const currentTenant = user?.tenants.find((t) => t.uuid === user.tenantUuid)
  const tenantLabel = currentTenant?.displayName ?? 'Empresa não definida'
  const hasMultipleTenants = (user?.tenants.length ?? 0) > 1

  // Fecha o dropdown ao clicar fora ou pressionar Escape.
  useEffect(() => {
    if (!menuOpen) return
    function onClick(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setMenuOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [menuOpen])

  async function handleSwitchTenant(tenantUuid: string) {
    if (switching || tenantUuid === user?.tenantUuid) {
      setMenuOpen(false)
      return
    }
    setSwitching(true)
    setSwitchError(null)
    try {
      await switchTenant(tenantUuid)
      setMenuOpen(false)
      // Volta para o dashboard para evitar exibir dados do tenant anterior.
      navigate('/', { replace: true })
    } catch (err) {
      const apiErr = toApiError(err)
      setSwitchError(apiErr.message ?? 'Não foi possível trocar de empresa.')
    } finally {
      setSwitching(false)
    }
  }

  function renderTenantBlock() {
    if (!hasMultipleTenants) {
      // Empresa única — apenas exibe, sem interação.
      return (
        <div className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 dark:border-slate-700 dark:bg-slate-800">
          <Building2 className="h-4 w-4 text-primary" />
          <div className="flex flex-col leading-tight">
            <span className="text-[10px] uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Empresa
            </span>
            <span className="max-w-[160px] truncate text-sm font-medium text-slate-800 dark:text-slate-100 sm:max-w-none">
              {tenantLabel}
            </span>
          </div>
        </div>
      )
    }

    // Multi-tenant — bloco vira botão que abre o dropdown de troca.
    return (
      <div className="relative" ref={menuRef}>
        <button
          type="button"
          onClick={() => setMenuOpen((v) => !v)}
          disabled={switching}
          className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-left transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-800 dark:hover:bg-slate-700"
          aria-haspopup="listbox"
          aria-expanded={menuOpen}
          aria-label="Trocar empresa"
        >
          <Building2 className="h-4 w-4 text-primary" />
          <div className="flex flex-col leading-tight">
            <span className="text-[10px] uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Empresa
            </span>
            <span className="max-w-[160px] truncate text-sm font-medium text-slate-800 dark:text-slate-100 sm:max-w-none">
              {tenantLabel}
            </span>
          </div>
          <ChevronDown
            className={`h-4 w-4 text-slate-500 transition-transform ${menuOpen ? 'rotate-180' : ''}`}
          />
        </button>

        {menuOpen ? (
          <div
            role="listbox"
            className="absolute left-0 z-50 mt-1 min-w-[240px] rounded-lg border border-slate-200 bg-white py-1 shadow-lg dark:border-slate-700 dark:bg-slate-800"
          >
            <div className="px-3 py-1 text-[10px] uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Trocar de empresa
            </div>
            {switchError ? (
              <p className="mx-3 my-1 text-xs text-red-600 dark:text-red-400">
                {switchError}
              </p>
            ) : null}
            <ul className="max-h-64 overflow-auto">
              {user?.tenants.map((t) => {
                const active = t.uuid === user.tenantUuid
                return (
                  <li key={t.uuid}>
                    <button
                      type="button"
                      role="option"
                      aria-selected={active}
                      disabled={switching}
                      onClick={() => handleSwitchTenant(t.uuid)}
                      className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left text-sm text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60 dark:text-slate-200 dark:hover:bg-slate-700"
                    >
                      <span className="truncate">{t.displayName}</span>
                      {active ? (
                        <Check className="h-4 w-4 shrink-0 text-primary" />
                      ) : null}
                    </button>
                  </li>
                )
              })}
            </ul>
          </div>
        ) : null}
      </div>
    )
  }

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center gap-2">{renderTenantBlock()}</div>

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