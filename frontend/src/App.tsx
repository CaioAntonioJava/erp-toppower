import { lazy, Suspense } from 'react'
import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { LoginPage } from './pages/LoginPage'
import { SelectOrganizationPage } from './pages/SelectOrganizationPage'
import { AppLayout } from './components/layout/AppLayout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import { ModuleRoute } from './components/ModuleRoute'
import { Spinner } from './components/ui/Spinner'

// Páginas são carregadas sob demanda (React.lazy + dynamic import) para
// dividir o bundle em um chunk por rota. As páginas usam export nomeado,
// então o shim `.then((m) => ({ default: m.X }))` normaliza para `default`.
// Rotas públicas (login/seleção de organização) e guarda de rotas ficam
// estáticas para o primeiro paint não depender de fetch adicional.

const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const ProfilePage = lazy(() => import('./pages/ProfilePage').then((m) => ({ default: m.ProfilePage })))
const CompaniesListPage = lazy(() => import('./pages/CompaniesListPage').then((m) => ({ default: m.CompaniesListPage })))
const CompanyFormPage = lazy(() => import('./pages/CompanyFormPage').then((m) => ({ default: m.CompanyFormPage })))
const CustomersListPage = lazy(() => import('./pages/CustomersListPage').then((m) => ({ default: m.CustomersListPage })))
const CustomerFormPage = lazy(() => import('./pages/CustomerFormPage').then((m) => ({ default: m.CustomerFormPage })))
const SuppliersListPage = lazy(() => import('./pages/SuppliersListPage').then((m) => ({ default: m.SuppliersListPage })))
const SupplierFormPage = lazy(() => import('./pages/SupplierFormPage').then((m) => ({ default: m.SupplierFormPage })))
const SellersListPage = lazy(() => import('./pages/SellersListPage').then((m) => ({ default: m.SellersListPage })))
const SellerFormPage = lazy(() => import('./pages/SellerFormPage').then((m) => ({ default: m.SellerFormPage })))
const ProductsListPage = lazy(() => import('./pages/ProductsListPage').then((m) => ({ default: m.ProductsListPage })))
const ProductFormPage = lazy(() => import('./pages/ProductFormPage').then((m) => ({ default: m.ProductFormPage })))
const QuotationsListPage = lazy(() => import('./pages/QuotationsListPage').then((m) => ({ default: m.QuotationsListPage })))
const QuotationFormPage = lazy(() => import('./pages/QuotationFormPage').then((m) => ({ default: m.QuotationFormPage })))
const QuotationDetailPage = lazy(() => import('./pages/QuotationDetailPage').then((m) => ({ default: m.QuotationDetailPage })))
const QuotationPrintPage = lazy(() => import('./pages/QuotationPrintPage').then((m) => ({ default: m.QuotationPrintPage })))
const SalesOrdersListPage = lazy(() => import('./pages/SalesOrdersListPage').then((m) => ({ default: m.SalesOrdersListPage })))
const SalesOrderFormPage = lazy(() => import('./pages/SalesOrderFormPage').then((m) => ({ default: m.SalesOrderFormPage })))
const SalesOrderDetailPage = lazy(() => import('./pages/SalesOrderDetailPage').then((m) => ({ default: m.SalesOrderDetailPage })))
const SalesOrderPrintPage = lazy(() => import('./pages/SalesOrderPrintPage').then((m) => ({ default: m.SalesOrderPrintPage })))
const TechnicalProposalsListPage = lazy(() => import('./pages/TechnicalProposalsListPage').then((m) => ({ default: m.TechnicalProposalsListPage })))
const TechnicalProposalFormPage = lazy(() => import('./pages/TechnicalProposalFormPage').then((m) => ({ default: m.TechnicalProposalFormPage })))
const TechnicalProposalDetailPage = lazy(() => import('./pages/TechnicalProposalDetailPage').then((m) => ({ default: m.TechnicalProposalDetailPage })))
const TechnicalProposalPrintPage = lazy(() => import('./pages/TechnicalProposalPrintPage').then((m) => ({ default: m.TechnicalProposalPrintPage })))
const ContractsListPage = lazy(() => import('./pages/ContractsListPage').then((m) => ({ default: m.ContractsListPage })))
const ContractFormPage = lazy(() => import('./pages/ContractFormPage').then((m) => ({ default: m.ContractFormPage })))
const ContractPrintPage = lazy(() => import('./pages/ContractPrintPage').then((m) => ({ default: m.ContractPrintPage })))
const ReceivablesListPage = lazy(() => import('./pages/ReceivablesListPage').then((m) => ({ default: m.ReceivablesListPage })))
const ReceivableFormPage = lazy(() => import('./pages/ReceivableFormPage').then((m) => ({ default: m.ReceivableFormPage })))
const PayablesListPage = lazy(() => import('./pages/PayablesListPage').then((m) => ({ default: m.PayablesListPage })))
const PayableFormPage = lazy(() => import('./pages/PayableFormPage').then((m) => ({ default: m.PayableFormPage })))
const BoletosListPage = lazy(() => import('./pages/BoletosListPage').then((m) => ({ default: m.BoletosListPage })))
const PurchaseImportPage = lazy(() => import('./pages/PurchaseImportPage').then((m) => ({ default: m.PurchaseImportPage })))
const CashFlowPage = lazy(() => import('./pages/CashFlowPage').then((m) => ({ default: m.CashFlowPage })))
const ContractFinancesListPage = lazy(() => import('./pages/ContractFinancesListPage').then((m) => ({ default: m.ContractFinancesListPage })))
const ContractFinancesControlDetailPage = lazy(() => import('./pages/ContractFinancesControlDetailPage').then((m) => ({ default: m.ContractFinancesControlDetailPage })))
const ContractFinancesEntryFormPage = lazy(() => import('./pages/ContractFinancesEntryFormPage').then((m) => ({ default: m.ContractFinancesEntryFormPage })))
const UsersListPage = lazy(() => import('./pages/UsersListPage').then((m) => ({ default: m.UsersListPage })))
const UserFormPage = lazy(() => import('./pages/UserFormPage').then((m) => ({ default: m.UserFormPage })))
const CarriersListPage = lazy(() => import('./pages/CarriersListPage').then((m) => ({ default: m.CarriersListPage })))
const CarrierFormPage = lazy(() => import('./pages/CarrierFormPage').then((m) => ({ default: m.CarrierFormPage })))
const ServiceTemplatesListPage = lazy(() => import('./pages/ServiceTemplatesListPage').then((m) => ({ default: m.ServiceTemplatesListPage })))
const ServiceTemplateFormPage = lazy(() => import('./pages/ServiceTemplateFormPage').then((m) => ({ default: m.ServiceTemplateFormPage })))
const OrganizationsListPage = lazy(() => import('./pages/OrganizationsListPage').then((m) => ({ default: m.OrganizationsListPage })))
const OrganizationFormPage = lazy(() => import('./pages/OrganizationFormPage').then((m) => ({ default: m.OrganizationFormPage })))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })))

/** Fallback global enquanto o chunk de uma página é baixado (React.lazy). */
function PageLoadingFallback() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <Spinner size="lg" />
    </div>
  )
}

/** Definição central das rotas da aplicação. */
export default function App() {
  return (
    <Suspense fallback={<PageLoadingFallback />}>
      <Routes>
        {/* Rotas públicas */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/select-organization" element={<SelectOrganizationPage />} />

        {/* Rotas protegidas — compartilham o AppLayout (sidebar + topbar) */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<DashboardPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/companies" element={<ModuleRoute module="MODULE_COMPANIES"><CompaniesListPage /></ModuleRoute>} />
          <Route path="/companies/new" element={<ModuleRoute module="MODULE_COMPANIES"><CompanyFormPage /></ModuleRoute>} />
          <Route path="/companies/:id" element={<ModuleRoute module="MODULE_COMPANIES"><CompanyFormPage /></ModuleRoute>} />
          <Route path="/customers" element={<ModuleRoute module="MODULE_CUSTOMERS"><CustomersListPage /></ModuleRoute>} />
          <Route path="/customers/new" element={<ModuleRoute module="MODULE_CUSTOMERS"><CustomerFormPage /></ModuleRoute>} />
          <Route path="/customers/:id" element={<ModuleRoute module="MODULE_CUSTOMERS"><CustomerFormPage /></ModuleRoute>} />
          <Route path="/suppliers" element={<ModuleRoute module="MODULE_SUPPLIERS"><SuppliersListPage /></ModuleRoute>} />
          <Route path="/suppliers/new" element={<ModuleRoute module="MODULE_SUPPLIERS"><SupplierFormPage /></ModuleRoute>} />
          <Route path="/suppliers/:id" element={<ModuleRoute module="MODULE_SUPPLIERS"><SupplierFormPage /></ModuleRoute>} />
          <Route path="/sellers" element={<ModuleRoute module="MODULE_SELLERS"><SellersListPage /></ModuleRoute>} />
          <Route path="/sellers/new" element={<ModuleRoute module="MODULE_SELLERS"><SellerFormPage /></ModuleRoute>} />
          <Route path="/sellers/:id" element={<ModuleRoute module="MODULE_SELLERS"><SellerFormPage /></ModuleRoute>} />
          <Route path="/products" element={<ModuleRoute module="MODULE_PRODUCTS"><ProductsListPage /></ModuleRoute>} />
          <Route path="/products/new" element={<ModuleRoute module="MODULE_PRODUCTS"><ProductFormPage /></ModuleRoute>} />
          <Route path="/products/:id" element={<ModuleRoute module="MODULE_PRODUCTS"><ProductFormPage /></ModuleRoute>} />
          <Route path="/quotations" element={<ModuleRoute module="MODULE_QUOTATIONS"><QuotationsListPage /></ModuleRoute>} />
          <Route path="/quotations/new" element={<ModuleRoute module="MODULE_QUOTATIONS"><QuotationFormPage /></ModuleRoute>} />
          <Route path="/quotations/:id/edit" element={<ModuleRoute module="MODULE_QUOTATIONS"><QuotationFormPage /></ModuleRoute>} />
          <Route path="/quotations/:id" element={<ModuleRoute module="MODULE_QUOTATIONS"><QuotationDetailPage /></ModuleRoute>} />
          <Route path="/sales-orders" element={<ModuleRoute module="MODULE_SALES_ORDERS"><SalesOrdersListPage /></ModuleRoute>} />
          <Route path="/sales-orders/new" element={<ModuleRoute module="MODULE_SALES_ORDERS"><SalesOrderFormPage /></ModuleRoute>} />
          <Route path="/sales-orders/:id/edit" element={<ModuleRoute module="MODULE_SALES_ORDERS"><SalesOrderFormPage /></ModuleRoute>} />
          <Route path="/sales-orders/:id" element={<ModuleRoute module="MODULE_SALES_ORDERS"><SalesOrderDetailPage /></ModuleRoute>} />
          <Route path="/technical-proposals" element={<ModuleRoute module="MODULE_TECHNICAL_PROPOSALS"><TechnicalProposalsListPage /></ModuleRoute>} />
          <Route path="/technical-proposals/new" element={<ModuleRoute module="MODULE_TECHNICAL_PROPOSALS"><TechnicalProposalFormPage /></ModuleRoute>} />
          <Route path="/technical-proposals/:id/edit" element={<ModuleRoute module="MODULE_TECHNICAL_PROPOSALS"><TechnicalProposalFormPage /></ModuleRoute>} />
          <Route path="/technical-proposals/:id" element={<ModuleRoute module="MODULE_TECHNICAL_PROPOSALS"><TechnicalProposalDetailPage /></ModuleRoute>} />
          <Route path="/contracts" element={<ModuleRoute module="MODULE_CONTRACTS"><ContractsListPage /></ModuleRoute>} />
          <Route path="/contracts/new" element={<ModuleRoute module="MODULE_CONTRACTS"><ContractFormPage /></ModuleRoute>} />
          <Route path="/contracts/:id" element={<ModuleRoute module="MODULE_CONTRACTS"><ContractFormPage /></ModuleRoute>} />
          <Route path="/receivables" element={<ModuleRoute module="MODULE_RECEIVABLES"><ReceivablesListPage /></ModuleRoute>} />
          <Route path="/receivables/new" element={<ModuleRoute module="MODULE_RECEIVABLES"><ReceivableFormPage /></ModuleRoute>} />
          <Route path="/receivables/:id" element={<ModuleRoute module="MODULE_RECEIVABLES"><ReceivableFormPage /></ModuleRoute>} />
          <Route path="/payables" element={<ModuleRoute module="MODULE_PAYABLES"><PayablesListPage /></ModuleRoute>} />
          <Route path="/payables/new" element={<ModuleRoute module="MODULE_PAYABLES"><PayableFormPage /></ModuleRoute>} />
          <Route path="/payables/:id" element={<ModuleRoute module="MODULE_PAYABLES"><PayableFormPage /></ModuleRoute>} />
          <Route path="/boletos" element={<ModuleRoute module="MODULE_BOLETOS"><BoletosListPage /></ModuleRoute>} />
          <Route path="/purchases/import" element={<ModuleRoute module="MODULE_PURCHASES_IMPORT"><PurchaseImportPage /></ModuleRoute>} />
          <Route path="/cash-flow" element={<ModuleRoute module="MODULE_CASH_FLOW"><CashFlowPage /></ModuleRoute>} />

          {/* Financeiro — controle de gastos/recebimentos por contrato (MODULE_CONTRACT_FINANCES). */}
          <Route path="/contract-finances" element={<ModuleRoute module="MODULE_CONTRACT_FINANCES"><ContractFinancesListPage /></ModuleRoute>} />
          <Route path="/contract-finances/controls/:id" element={<ModuleRoute module="MODULE_CONTRACT_FINANCES"><ContractFinancesControlDetailPage /></ModuleRoute>} />
          <Route path="/contract-finances/new" element={<ModuleRoute module="MODULE_CONTRACT_FINANCES"><ContractFinancesEntryFormPage /></ModuleRoute>} />
          <Route path="/contract-finances/entries/:id" element={<ModuleRoute module="MODULE_CONTRACT_FINANCES"><ContractFinancesEntryFormPage /></ModuleRoute>} />

          {/* Rotas administrativas — gestão de usuários (ROLE_ADMIN). */}
          <Route path="/users" element={<AdminRoute><UsersListPage /></AdminRoute>} />
          <Route path="/users/new" element={<AdminRoute><UserFormPage /></AdminRoute>} />
          <Route path="/users/:id" element={<AdminRoute><UserFormPage /></AdminRoute>} />

          {/* Cadastros — Transportadoras. Acesso controlado pelo módulo MODULE_CARRIERS. */}
          <Route path="/carriers" element={<ModuleRoute module="MODULE_CARRIERS"><CarriersListPage /></ModuleRoute>} />
          <Route path="/carriers/new" element={<ModuleRoute module="MODULE_CARRIERS"><CarrierFormPage /></ModuleRoute>} />
          <Route path="/carriers/:id" element={<ModuleRoute module="MODULE_CARRIERS"><CarrierFormPage /></ModuleRoute>} />

          {/* Cadastros — Catálogo de serviços. Acesso controlado pelo módulo MODULE_SERVICE_TEMPLATES. */}
          <Route path="/service-templates" element={<ModuleRoute module="MODULE_SERVICE_TEMPLATES"><ServiceTemplatesListPage /></ModuleRoute>} />
          <Route path="/service-templates/new" element={<ModuleRoute module="MODULE_SERVICE_TEMPLATES"><ServiceTemplateFormPage /></ModuleRoute>} />
          <Route path="/service-templates/:id" element={<ModuleRoute module="MODULE_SERVICE_TEMPLATES"><ServiceTemplateFormPage /></ModuleRoute>} />

          {/* Rotas administrativas — gestão de empresas emissoras (Organizations).
              Permite editar dados da empresa e fazer upload/remoção do logo
              usado no cabeçalho dos PDFs (cotação/proposta/pedido). */}
          <Route path="/organizations" element={<AdminRoute><OrganizationsListPage /></AdminRoute>} />
          <Route path="/organizations/:id" element={<AdminRoute><OrganizationFormPage /></AdminRoute>} />
        </Route>

        {/* Rota de impressão/PDF da proposta — sem AppLayout (sidebar/topbar),
            para uma saída limpa em window.print(). */}
        <Route
          element={
            <ProtectedRoute>
              <div className="min-h-screen bg-white text-slate-900">
                <Outlet />
              </div>
            </ProtectedRoute>
          }
        >
          <Route path="/quotations/:id/pdf" element={<QuotationPrintPage />} />
          <Route path="/sales-orders/:id/pdf" element={<SalesOrderPrintPage />} />
          <Route path="/technical-proposals/:id/pdf" element={<TechnicalProposalPrintPage />} />
          <Route path="/contracts/:id/pdf" element={<ContractPrintPage />} />
        </Route>

        {/* Compat: rota raiz redireciona para /login se não houver auth. */}
        <Route path="/index.html" element={<Navigate to="/" replace />} />
        {/* Compat: /register não existe mais — cadastro de usuários agora é via admin. */}
        <Route path="/register" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}
