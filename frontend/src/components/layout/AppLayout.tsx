import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

/** Shell da aplicação autenticada: sidebar + topbar + área de conteúdo. */
export function AppLayout() {
  return (
    <div className="flex min-h-screen bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="flex-1 overflow-y-auto p-6">
          {/* max-w-7xl (1280px) dá mais espaço para desktop, especialmente
              para a seção de endereço (Logradouro, Cidade). Em mobile o
              grid interno faz fallback para 1 coluna automaticamente. */}
          <div className="mx-auto w-full max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
