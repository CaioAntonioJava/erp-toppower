import { Building2, LogOut, UserCircle } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

/** Topbar com empresa ativa, papel do usuário e botão de logout. */
export function Topbar() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()

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

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center gap-2">
        {/* Empresa ativa — no mobile mostra só o ícone + nome curto. */}
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
          {hasMultipleTenants ? (
            <span
              title="Você tem acesso a mais de uma empresa — troque na tela de login."
              className="ml-1 rounded-full bg-primary-50 px-1.5 py-0.5 text-[10px] font-medium text-primary-700 dark:bg-primary-900/30 dark:text-primary-200"
            >
              +{user!.tenants.length - 1}
            </span>
          ) : null}
        </div>
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
