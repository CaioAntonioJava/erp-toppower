import { Fragment } from 'react'
import { NavLink } from 'react-router-dom'
import {
  Briefcase,
  Building2,
  FileUp,
  LayoutDashboard,
  Package,
  Truck,
  User,
  Users,
} from 'lucide-react'
import { Button } from '../ui/Button'

interface NavItem {
  to: string
  label: string
  icon: typeof LayoutDashboard
}

const navItems: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/companies', label: 'Empresas (PJ)', icon: Building2 },
  { to: '/customers', label: 'Clientes (PF)', icon: User },
  { to: '/suppliers', label: 'Fornecedores', icon: Truck },
  { to: '/sellers', label: 'Vendedores', icon: Briefcase },
  { to: '/products', label: 'Produtos', icon: Package },
]

/** Sidebar com os links principais. Mantida simples para um MVP. */
export function Sidebar() {
  return (
    <aside className="hidden w-60 shrink-0 border-r border-slate-200 bg-white px-4 py-6 md:flex md:flex-col dark:border-slate-800 dark:bg-slate-900">
      <div className="mb-8 flex items-center gap-2 px-2">
        <div className="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-white">
          <Users className="h-5 w-5" />
        </div>
        <span className="text-base font-semibold">ERP TopPower</span>
      </div>

      <nav className="flex flex-col gap-1">
        {navItems.map((item) => {
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
            </Fragment>
          )
        })}
      </nav>
    </aside>
  )
}
