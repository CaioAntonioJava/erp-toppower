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
} from 'lucide-react'
import { Button } from '../ui/Button'
import { useAuth } from '../../context/AuthContext'

interface NavItem {
  to: string
  label: string
  icon: typeof LayoutDashboard
}

const navItems: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/companies', label: 'Empresas (PJ)', icon: Building2 },
  { to: '/customers', label: 'Clientes (PF)', icon: User },
  { to: '/suppliers', label: 'Fornecedores', icon: Factory },
  { to: '/sellers', label: 'Vendedores', icon: Briefcase },
  { to: '/products', label: 'Produtos', icon: Package },
  { to: '/quotations', label: 'Propostas Comerciais', icon: FileText },
  { to: '/technical-proposals', label: 'Propostas Técnicas', icon: Wrench },
  { to: '/sales-orders', label: 'Pedidos de Venda', icon: ClipboardList },
]

/** Item de menu exclusivo de administradores — gestão de transportadoras. */
const carriersItem: NavItem = { to: '/carriers', label: 'Transportadoras', icon: Truck }

/** Item de menu exclusivo de administradores — catálogo de serviços. */
const serviceTemplatesItem: NavItem = { to: '/service-templates', label: 'Serviços', icon: Cog }

/** Item de menu exclusivo de administradores — gestão de usuários do sistema. */
const usersItem: NavItem = { to: '/users', label: 'Usuários', icon: UserCog }

/**
 * Item de menu exclusivo de administradores — gestão das empresas
 * emissoras (Organizations: Top Power Engenharia, Materiais...).
 * Permite editar dados e fazer upload do logo usado nos PDFs.
 */
const organizationsItem: NavItem = {
  to: '/organizations',
  label: 'Empresas (Org.)',
  icon: Settings,
}

/** Sidebar com os links principais. Mantida simples para um MVP. */
export function Sidebar() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'
  // Admin vê os itens administrativos (Transportadoras, Empresas emissoras,
  // Usuários) ao final. Gestores não os veem.
  const items = isAdmin
    ? [...navItems, carriersItem, serviceTemplatesItem, organizationsItem, usersItem]
    : navItems

  return (
    <aside className="hidden w-60 shrink-0 border-r border-slate-200 bg-white px-4 py-6 md:flex md:flex-col dark:border-slate-800 dark:bg-slate-900">
      <div className="mb-8 flex items-center gap-2 px-2">
        <div className="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-white">
          <Users className="h-5 w-5" />
        </div>
        <span className="text-base font-semibold">ERP TOP POWER</span>
      </div>

      <nav className="flex flex-col gap-1">
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
                    isActive
                      ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-200'
                      : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
                  ].join(' ')
                }
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </NavLink>

              {/* Ação rápida logo abaixo do menu Produtos: importação de XML. */}
              {item.to === '/products' ? (
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

              {/* Separador entre o bloco de cadastros e os próximos módulos,
                  posicionado após Produtos. */}
              {item.to === '/products' ? (
                <div
                  aria-hidden
                  className="mt-[18px] mb-3 h-px bg-slate-300/70 dark:bg-slate-700/70"
                />
              ) : null}

              {/* Separador antes do bloco administrativo
                  (Transportadoras e Usuários). */}
              {item.to === '/sales-orders' && isAdmin ? (
                <div
                  aria-hidden
                  className="mt-[18px] mb-3 h-px bg-slate-300/70 dark:bg-slate-700/70"
                />
              ) : null}
            </Fragment>
          )
        })}
      </nav>
    </aside>
  )
}
