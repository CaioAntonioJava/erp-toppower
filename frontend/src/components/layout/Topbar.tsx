import { LogOut } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { ThemeToggle } from '../ui/ThemeToggle'

/** Topbar com toggle de tema, papel do usuário e botão de logout. */
export function Topbar() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    signOut()
    navigate('/login', { replace: true })
  }

  const initials = (user?.email ?? '?').slice(0, 2).toUpperCase()
  const roleLabel = user?.role === 'ROLE_ADMIN' ? 'Administrador' : 'Gestor'

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 dark:border-slate-800 dark:bg-slate-900">
      <div className="md:hidden">
        <span className="text-base font-semibold">ERP TopPower</span>
      </div>

      <div className="hidden md:block">
        <span className="text-sm text-slate-500 dark:text-slate-400">
          Controle total do seu negócio em um só lugar.
        </span>
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

        <ThemeToggle />

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
