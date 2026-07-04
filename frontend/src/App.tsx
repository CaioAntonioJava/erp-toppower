import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
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
import { NotFoundPage } from './pages/NotFoundPage'
import { AppLayout } from './components/layout/AppLayout'
import { ProtectedRoute } from './components/ProtectedRoute'

/** Definição central das rotas da aplicação. */
export default function App() {
  return (
    <Routes>
      {/* Rotas públicas */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

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
        <Route path="/companies" element={<CompaniesListPage />} />
        <Route path="/companies/new" element={<CompanyFormPage />} />
        <Route path="/companies/:id" element={<CompanyFormPage />} />
        <Route path="/customers" element={<CustomersListPage />} />
        <Route path="/customers/new" element={<CustomerFormPage />} />
        <Route path="/customers/:id" element={<CustomerFormPage />} />
        <Route path="/suppliers" element={<SuppliersListPage />} />
        <Route path="/suppliers/new" element={<SupplierFormPage />} />
        <Route path="/suppliers/:id" element={<SupplierFormPage />} />
        <Route path="/sellers" element={<SellersListPage />} />
        <Route path="/sellers/new" element={<SellerFormPage />} />
        <Route path="/sellers/:id" element={<SellerFormPage />} />
        <Route path="/products" element={<ProductsListPage />} />
        <Route path="/products/new" element={<ProductFormPage />} />
        <Route path="/products/:id" element={<ProductFormPage />} />
        <Route path="/quotations" element={<QuotationsListPage />} />
        <Route path="/quotations/new" element={<QuotationFormPage />} />
        <Route path="/quotations/:id/edit" element={<QuotationFormPage />} />
        <Route path="/quotations/:id" element={<QuotationDetailPage />} />
        <Route path="/sales-orders" element={<SalesOrdersListPage />} />
        <Route path="/sales-orders/new" element={<SalesOrderFormPage />} />
        <Route path="/sales-orders/:id/edit" element={<SalesOrderFormPage />} />
        <Route path="/sales-orders/:id" element={<SalesOrderDetailPage />} />
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
      </Route>

      {/* Compat: rota raiz redireciona para /login se não houver auth. */}
      <Route path="/index.html" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
