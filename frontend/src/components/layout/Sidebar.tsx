import { Fragment } from 'react'
import { NavLink } from 'react-router-dom'
import {
  Briefcase,
  Building2,
  ClipboardList,
  Factory,
  FileText,
  FileUp,
  LayoutDashboard,
  Package,
  Settings,
  Truck,
  User,
  UserCog,
  Users,
  Wrench,
  Cog,
  FilePenLine,
  Wallet,
  Receipt,
} from 'lucide-react'
import { Button } from '../ui/Button'
import { useAuth } from '../../context/AuthContext'

interface NavItem {
  to: string
  label: string
  icon: typeof LayoutDashboard
}

interface NavSection {
  /** Rótulo curto exibido acima do bloco (uppercase, cinza claro). */
  title: string
  /** Itens do bloco. */
  items: NavItem[]
  /** Quando true, o bloco só é exibido para administradores. */
  adminOnly?: boolean
}

/**
 * Agrupamentos da sidebar. Cada bloco recebe um rótulo de seção
 * (Cadastros, Comercial, Financeiro, Administrativo) separado do
 * anterior por uma linha divisória horizontal.
 */
const navSections: NavSection[] = [
  {
    title: 'Geral',
    items: [
      { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    ],
  },
  {
    title: 'Cadastros',
    items: [
      { to: '/companies', label: 'Empresas (PJ)', icon: Building2 },
      { to: '/customers', label: 'Clientes (PF)', icon: User },
      { to: '/suppliers', label: 'Fornecedores', icon: Factory },
      { to: '/sellers', label: 'Vendedores', icon: Briefcase },
      { to: '/products', label: 'Produtos', icon: Package },
    ],
  },
  {
    title: 'Comercial',
    items: [
      { to: '/quotations', label: 'Propostas Comerciais', icon: FileText },
      { to: '/technical-proposals', label: 'Propostas Técnicas', icon: Wrench },
      { to: '/sales-orders', label: 'Pedidos de Venda', icon: ClipboardList },
      { to: '/contracts', label: 'Contratos', icon: FilePenLine },
    ],
  },
  {
    title: 'Financeiro',
    items: [
      { to: '/receivables', label: 'Contas a Receber', icon: Wallet },
      { to: '/payables', label: 'Contas a Pagar', icon: Receipt },
    ],
  },
  {
    title: 'Administrativo',
    adminOnly: true,
    items: [
      { to: '/carriers', label: 'Transportadoras', icon: Truck },
      { to: '/service-templates', label: 'Serviços', icon: Cog },
      { to: '/organizations', label: 'Empresas (Org.)', icon: Settings },
      { to: '/users', label: 'Usuários', icon: UserCog },
    ],
  },
]

/** Sidebar com os links principais. Mantida simples para um MVP. */
export function Sidebar({ collapsed = false }: { collapsed?: boolean }) {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const sections = isAdmin
    ? navSections
    : navSections.filter((s) => !s.adminOnly)

  return (
    <aside
      className={[
        'hidden border-r border-slate-200 bg-white py-6 transition-[width] duration-300 md:flex md:flex-col dark:border-slate-800 dark:bg-slate-900',
        collapsed ? 'w-16' : 'w-60',
      ].join(' ')}
    >
      {/* Logo / marca */}
      <div className="mb-8 flex items-center justify-center gap-2 px-2">
        <div className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary text-white">
          <Users className="h-5 w-5" />
        </div>
        {!collapsed ? (
          <span className="text-base font-semibold">ERP TOP POWER</span>
        ) : null}
      </div>

      <nav className="flex flex-col gap-1 px-2">
        {sections.map((section, sectionIndex) => (
          <Fragment key={section.title}>
            {/* Separador horizontal acima de cada seção (exceto a primeira). */}
            {sectionIndex > 0 ? (
              <div
                aria-hidden
                className="my-3 h-px bg-slate-300/70 dark:bg-slate-700/70"
              />
            ) : null}

            {/* Rótulo da seção — só aparece quando expandido */}
            {!collapsed ? (
              <p className="px-3 pb-1 pt-1 text-[11px] font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                {section.title}
              </p>
            ) : null}

            {section.items.map((item) => {
              const Icon = item.icon
              return (
                <Fragment key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.to === '/'}
                    className={({ isActive }) =>
                      [
                        'flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                        collapsed ? 'justify-center' : '',
                        isActive
                          ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-200'
                          : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
                      ].join(' ')
                    }
                    title={collapsed ? item.label : undefined}
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    {!collapsed ? item.label : null}
                  </NavLink>

                  {/* Ação rápida logo abaixo do menu Produtos: importação de XML. */}
                  {item.to === '/products' && !collapsed ? (
                    <Button
                      variant="primary"
                      size="sm"
                      fullWidth
                      className="mt-1"
                    >
                      <FileUp className="h-4 w-4" />
                      IMPORTAR XML
                    </Button>
                  ) : null}
                </Fragment>
              )
            })}
          </Fragment>
        ))}
      </nav>
    </aside>
  )
}