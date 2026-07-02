import { Outlet, useLocation } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { Footer } from './Footer'
import { useAuth } from '../../context/AuthContext'

/**
 * Verifica se a rota atual é uma tela de formulário/detalhe de uma
 * entidade densa (ex.: `/quotations/new`, `/quotations/:id/edit`,
 * `/quotations/:id`). A listagem `/quotations` (acessada pelo menu
 * lateral) NÃO entra aqui — ela mantém o layout padrão com sidebar,
 * porque o usuário ainda está navegando entre módulos.
 *
 * Mantemos checagens explícitas em vez de prefixo-curinga para evitar
 * que sub-rotas futuras (ex.: `/quotations/settings`) herdem o
 * comportamento sem querer.
 */
function isFullWidthRoute(pathname: string): boolean {
  // /quotations/new — cadastro
  if (pathname === '/quotations/new') return true
  // /quotations/:id/edit — edição
  if (/^\/quotations\/[^/]+\/edit$/.test(pathname)) return true
  // /quotations/:id — detalhe
  if (/^\/quotations\/[^/]+$/.test(pathname)) return true
  return false
}

/**
 * Shell da aplicação autenticada.
 *
 * Três modos:
 * 1. `hasProfile === false` → setup inicial (sem sidebar, max-w-2xl).
 * 2. Rota de "tela cheia" (formulário/detalhe de proposta) → sem sidebar,
 *    max-w generoso, para formulários densos aproveitarem o monitor.
 * 3. Padrão → sidebar + topbar + conteúdo (max-w-7xl) + footer.
 */
export function AppLayout() {
  const { hasProfile } = useAuth()
  const { pathname } = useLocation()
  const fullWidth = isFullWidthRoute(pathname)

  // Modo "setup inicial": sem sidebar, mas com Topbar (usuário/logout)
  if (hasProfile === false) {
    return (
      <div className="flex min-h-screen flex-col bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
        <Topbar />
        <main className="flex flex-1 items-center justify-center px-4 py-6">
          <div className="w-full max-w-2xl">
            <Outlet />
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  // Modo "tela cheia": sem sidebar, Topbar + conteúdo largo + Footer.
  if (fullWidth) {
    return (
      <div className="flex min-h-screen flex-col bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
        <Topbar />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          {/* max-w-[1600px] dá bastante espaço para formulários densos
              (ex.: grid de itens com várias colunas) sem esticar até
              monitores ultra-wide. Em mobile o grid interno faz fallback
              para 1 coluna automaticamente. */}
          <div className="mx-auto w-full max-w-[1600px]">
            <Outlet />
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  // Modo normal: sidebar + topbar + conteúdo + rodapé com seletor de tema
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
        <Footer />
      </div>
    </div>
  )
}