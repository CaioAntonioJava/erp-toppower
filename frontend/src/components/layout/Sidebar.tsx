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
  FilePenLine,
  Wallet,
  Receipt,
} from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import type { Module } from '../../types/api'

interface NavItem {
  to: string
  label: string
  icon: typeof LayoutDashboard
  /**
   * Módulo (authority) exigido para visualizar este item. Ausente para
   * itens sempre visíveis (Dashboard) ou restritos por adminOnly.
   * Usado para filtrar a sidebar de ROLE_EMPLOYEE.
   */
  module?: Module
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
      { to: '/companies', label: 'Empresas (PJ)', icon: Building2, module: 'MODULE_COMPANIES' },
      { to: '/customers', label: 'Clientes (PF)', icon: User, module: 'MODULE_CUSTOMERS' },
      { to: '/suppliers', label: 'Fornecedores', icon: Factory, module: 'MODULE_SUPPLIERS' },
      { to: '/sellers', label: 'Vendedores', icon: Briefcase, module: 'MODULE_SELLERS' },
      { to: '/products', label: 'Produtos', icon: Package, module: 'MODULE_PRODUCTS' },
      { to: '/carriers', label: 'Transportadoras', icon: Truck, module: 'MODULE_CARRIERS' },
      { to: '/service-templates', label: 'Serviços', icon: Wrench, module: 'MODULE_SERVICE_TEMPLATES' },
    ],
  },
  {
    title: 'Comercial',
    items: [
      { to: '/quotations', label: 'Propostas Comerciais', icon: FileText, module: 'MODULE_QUOTATIONS' },
      { to: '/technical-proposals', label: 'Propostas Técnicas', icon: Wrench, module: 'MODULE_TECHNICAL_PROPOSALS' },
      { to: '/sales-orders', label: 'Pedidos de Venda', icon: ClipboardList, module: 'MODULE_SALES_ORDERS' },
      { to: '/contracts', label: 'Contratos', icon: FilePenLine, module: 'MODULE_CONTRACTS' },
    ],
  },
  {
    title: 'Financeiro',
    items: [
      { to: '/receivables', label: 'Contas a Receber', icon: Wallet, module: 'MODULE_RECEIVABLES' },
      { to: '/payables', label: 'Contas a Pagar', icon: Receipt, module: 'MODULE_PAYABLES' },
      { to: '/purchases/import', label: 'Importar NF-e', icon: FileUp, module: 'MODULE_PURCHASES_IMPORT' },
    ],
  },
  {
    title: 'Administrativo',
    adminOnly: true,
    items: [
      { to: '/organizations', label: 'Empresas', icon: Settings },
      { to: '/users', label: 'Usuários', icon: UserCog },
    ],
  },
]

/** Sidebar com os links principais. Mantida simples para um MVP. */
export function Sidebar({ collapsed = false }: { collapsed?: boolean }) {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'
  const isEmployee = user?.role === 'ROLE_EMPLOYEE'

  // ADMIN vê apenas a seção Administrativo; demais (MANAGER/EMPLOYEE) veem
  // as seções não-admin. EMPLOYEE tem ainda os itens filtrados por módulo.
  const sections = isAdmin
    ? navSections.filter((s) => s.adminOnly)
    : navSections.filter((s) => !s.adminOnly)

  function itemVisible(item: NavItem): boolean {
    // EMPLOYEE: só vê itens cujo módulo foi concedido (ou itens sem módulo,
    // como Dashboard).
    if (isEmployee && item.module && !user?.modules.includes(item.module)) {
      return false
    }
    return true
  }

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
        {sections
          .map((section) => ({ section, items: section.items.filter(itemVisible) }))
          .filter((s) => s.items.length > 0)
          .map(({ section, items }, sectionIndex) => (
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

            {items.map((item) => {
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

                </Fragment>
              )
            })}
          </Fragment>
        ))}
      </nav>
    </aside>
  )
}