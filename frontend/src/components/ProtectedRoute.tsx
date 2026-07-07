import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/AuthContext'
import { Spinner } from './ui/Spinner'

interface ProtectedRouteProps {
  children: ReactNode
}

/**
 * Bloqueia rotas privadas:
 * 1. Redireciona para /login quando não autenticado
 * 2. Redireciona para /profile quando autenticado mas sem perfil preenchido
 * 3. Redireciona para /select-organization após login novo, antes de escolher a empresa
 *
 * Enquanto o AuthProvider verifica o token e o status do perfil inicial,
 * exibe um spinner para evitar piscar a tela de login indevidamente.
 */
export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, hasProfile, hasSelectedOrganization } = useAuth()
  const location = useLocation()

  // Aguardando verificação inicial do token e do perfil
  if (isLoading || (isAuthenticated && hasProfile === null)) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-50 dark:bg-slate-950">
        <Spinner size="lg" />
      </div>
    )
  }

  // Não autenticado → /login
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }

  // Autenticado mas sem perfil → força preenchimento (exceto se já está em /profile)
  if (hasProfile === false && location.pathname !== '/profile') {
    return <Navigate to="/profile" replace />
  }

  // Login recém-realizado e ainda sem empresa selecionada → tela de seleção
  // (exceto se já está nela). No boot com token persistido a flag é `true`.
  if (!hasSelectedOrganization && location.pathname !== '/select-organization') {
    return <Navigate to="/select-organization" replace />
  }

  return <>{children}</>
}