import type { Module } from '../types/api'

/**
 * Registro central dos módulos (paineis) do sistema. Usado tanto pelo
 * formulário de usuário (checkboxes de permissões) quanto pela Sidebar
 * (filtro de itens para ROLE_EMPLOYEE).
 *
 * O campo `to` espelha a rota do frontend, permitindo associar um item da
 * Sidebar ao módulo que ele exige.
 */
export interface ModuleDef {
  /** Valor do enum Module no backend (authority). */
  key: Module
  /** Rótulo exibido ao usuário. */
  label: string
  /** Seção da sidebar à qual o módulo pertence (agrupamento visual). */
  section: 'Cadastros' | 'Comercial' | 'Financeiro'
  /** Rota base do frontend (prefixo das rotas que exigem este módulo). */
  to: string
}

/**
 * Lista de módulos de negócio. A ordem reflete a ordem da Sidebar.
 * Dashboard e Perfil não são módulos (acessíveis a qualquer autenticado).
 */
export const MODULES: ModuleDef[] = [
  // Cadastros
  { key: 'MODULE_COMPANIES', label: 'Empresas (PJ)', section: 'Cadastros', to: '/companies' },
  { key: 'MODULE_CUSTOMERS', label: 'Clientes (PF)', section: 'Cadastros', to: '/customers' },
  { key: 'MODULE_SUPPLIERS', label: 'Fornecedores', section: 'Cadastros', to: '/suppliers' },
  { key: 'MODULE_SELLERS', label: 'Vendedores', section: 'Cadastros', to: '/sellers' },
  { key: 'MODULE_PRODUCTS', label: 'Produtos', section: 'Cadastros', to: '/products' },
  // Comercial
  { key: 'MODULE_QUOTATIONS', label: 'Propostas Comerciais', section: 'Comercial', to: '/quotations' },
  { key: 'MODULE_TECHNICAL_PROPOSALS', label: 'Propostas Técnicas', section: 'Comercial', to: '/technical-proposals' },
  { key: 'MODULE_SALES_ORDERS', label: 'Pedidos de Venda', section: 'Comercial', to: '/sales-orders' },
  { key: 'MODULE_CONTRACTS', label: 'Contratos', section: 'Comercial', to: '/contracts' },
  // Financeiro
  { key: 'MODULE_RECEIVABLES', label: 'Contas a Receber', section: 'Financeiro', to: '/receivables' },
  { key: 'MODULE_PAYABLES', label: 'Contas a Pagar', section: 'Financeiro', to: '/payables' },
  { key: 'MODULE_PURCHASES_IMPORT', label: 'Importar NF-e', section: 'Financeiro', to: '/purchases/import' },
  { key: 'MODULE_BOLETOS', label: 'Boletos', section: 'Financeiro', to: '/boletos' },
]

/** Seções de módulos, na ordem exibida no formulário de permissões. */
export const MODULE_SECTIONS: ModuleDef['section'][] = ['Cadastros', 'Comercial', 'Financeiro']

/**
 * Retorna o módulo (Module) responsável por uma rota, comparando pelo
 * prefixo. Usado por ModuleRoute para autorizar acesso. Retorna undefined
 * para rotas não cobertas por módulos (Dashboard, Perfil, rotas admin).
 */
export function moduleForRoute(pathname: string): Module | undefined {
  // Ordem importa: rotas com prefixos mais longos primeiro.
  const sorted = [...MODULES].sort((a, b) => b.to.length - a.to.length)
  return sorted.find((m) => pathname === m.to || pathname.startsWith(m.to + '/') || pathname.startsWith(m.to))?.key
}