import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Building2, Check, LogIn, LogOut } from 'lucide-react'
import { AuthCard } from '../components/layout/AuthCard'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { useAuth } from '../context/AuthContext'
import { useOrganization } from '../context/OrganizationContext'
import type { OrganizationSummary } from '../types/api'

/**
 * Tela exibida após um login recém-realizado para o usuário escolher a
 * empresa (Organization) ativa antes de entrar no sistema. Usuários com
 * uma única empresa veem apenas a opção disponível; sem vínculo recebem
 * um aviso e a opção de sair.
 */
export function SelectOrganizationPage() {
  const { isAuthenticated, user, markOrganizationSelected, signOut } = useAuth()
  const { organizations, activeOrganization, isLoading, setActive } =
    useOrganization()
  const navigate = useNavigate()

  // Pré-seleciona a ativa do contexto (default do backend) como escolha inicial.
  const [selected, setSelected] = useState<OrganizationSummary | null>(
    activeOrganization,
  )

  // Sem sessão válida → volta para o login.
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  function handleConfirm() {
    const org = selected ?? activeOrganization ?? organizations[0]
    if (!org) return
    setActive(org)
    markOrganizationSelected()
    navigate('/', { replace: true })
  }

  function handleLogout() {
    signOut()
    navigate('/login', { replace: true })
  }

  /* ---- carregando a lista de empresas ---- */
  if (isLoading) {
    return (
      <AuthCard title="Selecionar empresa">
        <div className="flex h-40 items-center justify-center">
          <Spinner size="lg" />
        </div>
      </AuthCard>
    )
  }

  /* ---- sem vínculo com nenhuma empresa ---- */
  if (organizations.length === 0) {
    return (
      <AuthCard
        title="Selecionar empresa"
        subtitle="Você não possui acesso a nenhuma empresa."
        footer={
          <button
            type="button"
            onClick={handleLogout}
            className="inline-flex items-center gap-1 font-medium text-primary hover:underline"
          >
            <LogOut className="h-4 w-4" /> Sair
          </button>
        }
      >
        <p className="text-sm text-slate-600 dark:text-slate-300">
          Contate um administrador para que seu acesso a uma empresa seja
          liberado e tente novamente.
        </p>
      </AuthCard>
    )
  }

  /* ---- uma única empresa ---- */
  const single = organizations.length === 1

  return (
    <AuthCard
      title="Selecionar empresa"
      subtitle={
        single
          ? 'Confirme a empresa para entrar no sistema.'
          : 'Escolha a empresa que deseja acessar.'
      }
      footer={
        <span>
          Conectado como{' '}
          <strong className="text-slate-800 dark:text-slate-200">
            {user?.email}
          </strong>{' '}
          ·{' '}
          <button
            type="button"
            onClick={handleLogout}
            className="inline-flex items-center gap-1 font-medium text-primary hover:underline"
          >
            <LogOut className="h-4 w-4" /> Trocar conta
          </button>
        </span>
      }
    >
      <ul className="flex flex-col gap-2">
        {organizations.map((org) => {
          const isSelected =
            org.uuid === (selected ?? activeOrganization)?.uuid
          return (
            <li key={org.uuid}>
              <button
                type="button"
                onClick={() => setSelected(org)}
                className={`flex w-full items-start gap-3 rounded-xl border p-3 text-left transition-colors ${
                  isSelected
                    ? 'border-primary bg-primary/5'
                    : 'border-slate-200 bg-white hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:hover:bg-slate-700'
                }`}
              >
                <Building2
                  className={`mt-0.5 h-5 w-5 shrink-0 ${
                    isSelected ? 'text-primary' : 'text-slate-400'
                  }`}
                />
                <span className="flex flex-1 flex-col">
                  <span className="flex items-center gap-2">
                    <span className="font-medium text-slate-900 dark:text-slate-100">
                      {org.tradeName}
                    </span>
                    {org.proposalPrefix ? (
                      <span
                        className="inline-flex h-5 items-center rounded-md border border-primary/30 bg-primary/10 px-1.5 font-mono text-[10px] font-semibold tracking-wide text-primary"
                        title="Prefixo das propostas técnicas desta empresa"
                      >
                        {org.proposalPrefix}
                      </span>
                    ) : null}
                  </span>
                  <span className="text-xs text-slate-500 dark:text-slate-400">
                    {org.corporateName} · {org.cnpj}
                  </span>
                </span>
                {isSelected ? (
                  <Check className="h-5 w-5 shrink-0 text-primary" />
                ) : null}
              </button>
            </li>
          )
        })}
      </ul>

      <Button
        type="button"
        onClick={handleConfirm}
        fullWidth
        size="lg"
        className="mt-5"
      >
        <LogIn className="h-4 w-4" />
        Entrar
      </Button>
    </AuthCard>
  )
}