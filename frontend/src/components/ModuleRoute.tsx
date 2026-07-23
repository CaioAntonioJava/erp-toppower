import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/AuthContext'
import type { Module } from '../types/api'

interface ModuleRouteProps {
  /** Módulo exigido para acessar a rota envolvida. */
  module: Module
  children: ReactNode
}

/**
 * Guard de módulo (painel). Liberado automaticamente para ADMIN e MANAGER
 * (acessam todos os painéis). Para ROLE_EMPLOYEE, só permite acesso se o
 * módulo estiver entre os concedidos ao usuário; caso contrário, redireciona
 * para a Dashboard.
 *
 * Deve ser usado DENTRO de <ProtectedRoute>, que já garante autenticação.
 */
export function ModuleRoute({ module, children }: ModuleRouteProps) {
  const { user } = useAuth()

  // ADMIN e MANAGER têm acesso total aos módulos de negócio.
  if (user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_MANAGER') {
    return <>{children}</>
  }

  // EMPLOYEE: verifica se o módulo foi concedido.
  if (user && user.modules.includes(module)) {
    return <>{children}</>
  }

  return <Navigate to="/" replace />
}