import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { LoginPage } from './pages/LoginPage'
import { SelectOrganizationPage } from './pages/SelectOrganizationPage'
import { DashboardPage } from './pages/DashboardPage'
import { ProfilePage } from './pages/ProfilePage'
import { CompaniesListPage } from './pages/CompaniesListPage'
import { CompanyFormPage } from './pages/CompanyFormPage'
import { CustomersListPage } from './pages/CustomersListPage'
import { CustomerFormPage } from './pages/CustomerFormPage'
import { SuppliersListPage } from './pages/SuppliersListPage'
import { SupplierFormPage } from './pages/SupplierFormPage'
import { SellersListPage } from './pages/SellersListPage'
import { SellerFormPage } from './pages/SellerFormPage'
import { ProductsListPage } from './pages/ProductsListPage'
import { ProductFormPage } from './pages/ProductFormPage'
import { QuotationsListPage } from './pages/QuotationsListPage'
import { QuotationFormPage } from './pages/QuotationFormPage'
import { QuotationDetailPage } from './pages/QuotationDetailPage'
import { QuotationPrintPage } from './pages/QuotationPrintPage'
import { SalesOrdersListPage } from './pages/SalesOrdersListPage'
import { SalesOrderFormPage } from './pages/SalesOrderFormPage'
import { SalesOrderDetailPage } from './pages/SalesOrderDetailPage'
import { SalesOrderPrintPage } from './pages/SalesOrderPrintPage'
import { TechnicalProposalsListPage } from './pages/TechnicalProposalsListPage'
import { TechnicalProposalFormPage } from './pages/TechnicalProposalFormPage'
import { TechnicalProposalDetailPage } from './pages/TechnicalProposalDetailPage'
import { TechnicalProposalPrintPage } from './pages/TechnicalProposalPrintPage'
import { ContractsListPage } from './pages/ContractsListPage'
import { ContractFormPage } from './pages/ContractFormPage'
import { ContractPrintPage } from './pages/ContractPrintPage'
import { ReceivablesListPage } from './pages/ReceivablesListPage'
import { ReceivableFormPage } from './pages/ReceivableFormPage'
import { PayablesListPage } from './pages/PayablesListPage'
import { PayableFormPage } from './pages/PayableFormPage'
import { BoletosListPage } from './pages/BoletosListPage'
import { PurchaseImportPage } from './pages/PurchaseImportPage'
import { UsersListPage } from './pages/UsersListPage'
import { UserFormPage } from './pages/UserFormPage'
import { CarriersListPage } from './pages/CarriersListPage'
import { CarrierFormPage } from './pages/CarrierFormPage'
import { ServiceTemplatesListPage } from './pages/ServiceTemplatesListPage'
import { ServiceTemplateFormPage } from './pages/ServiceTemplateFormPage'
import { OrganizationsListPage } from './pages/OrganizationsListPage'
import { OrganizationFormPage } from './pages/OrganizationFormPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { AppLayout } from './components/layout/AppLayout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import { ModuleRoute } from './components/ModuleRoute'

/** Definição central das rotas da aplicação. */
export default function App() {
  return (
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
  )
}
