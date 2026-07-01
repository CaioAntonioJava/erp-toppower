import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { useAuth } from '../../context/AuthContext'

/**
 * Shell da aplicação autenticada.
 *
 * Quando o usuário ainda não preencheu o perfil (`hasProfile === false`),
 * exibe apenas o conteúdo da página com o Topbar (mantendo info do
 * usuário, tema e logout) mas sem a Sidebar — para que o usuário foque
 * 100% no preenchimento dos dados pessoais. Após salvar o perfil, o
 * layout completo aparece.
 */
export function AppLayout() {
  const { hasProfile } = useAuth()

  // Modo "setup inicial": sem sidebar, mas com Topbar (usuário/tema/logout)
  if (hasProfile === false) {
    return (
      <div className="flex min-h-screen flex-col bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
        <Topbar />
        <main className="flex flex-1 items-center justify-center px-4 py-6">
          <div className="w-full max-w-2xl">
            <Outlet />
          </div>
        </main>
      </div>
    )
  }

  // Modo normal: sidebar + topbar + conteúdo
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