import { useAuth } from '../context/AuthContext'

/** Placeholder simples do dashboard — futuro lar dos módulos do ERP. */
export function DashboardPage() {
  const { user } = useAuth()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Bem-vindo, <span className="font-medium text-slate-700 dark:text-slate-200">{user?.email}</span>.
          Utilize o menu lateral para navegar pelos módulos e acessar os recursos disponíveis.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">
            Papel
          </p>
          <p className="mt-1 text-lg font-semibold">
            {user?.role === 'ROLE_ADMIN' ? 'Administrador' : 'Gestor'}
          </p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">
            Identificador
          </p>
          <p className="mt-1 break-all text-sm font-mono">{user?.uuid}</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">
            Sessão
          </p>
          <p className="mt-1 text-lg font-semibold text-emerald-600 dark:text-emerald-400">
            Ativa
          </p>
        </div>
      </div>
    </div>
  )
}
