import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/AuthContext'

interface AdminRouteProps {
  children: ReactNode
}

/**
 * Guard de papel: bloqueia rotas restritas a administradores.
 * Redireciona gestores (e qualquer usuário não-admin) para a Dashboard.
 *
 * Deve ser usado DENTRO de <ProtectedRoute>, que já garante que o usuário
 * está autenticado e com perfil preenchido.
 */
export function AdminRoute({ children }: AdminRouteProps) {
  const { user } = useAuth()
  if (user?.role !== 'ROLE_ADMIN') {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}