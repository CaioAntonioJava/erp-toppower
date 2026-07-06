import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Building2, LogIn, Search } from 'lucide-react'
import { AuthCard } from '../components/layout/AuthCard'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Alert } from '../components/ui/Alert'
import { useAuth } from '../context/AuthContext'
import { toApiError } from '../lib/errors'
import { listTenantsByEmail } from '../api/auth.api'
import type { TenantSummary } from '../types/api'

interface LocationState {
  from?: string
}

export function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as LocationState | null)?.from ?? '/'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [tenants, setTenants] = useState<TenantSummary[]>([])
  const [selectedTenant, setSelectedTenant] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const [isSearchingTenants, setSearchingTenants] = useState(false)
  const [error, setError] = useState<string | null>(null)

  /**
   * Passo 1: valida email + senha, busca as empresas (tenants) às quais
   * o email tem acesso. Se houver apenas uma empresa, avança direto para
   * o login; se houver várias, exibe o dropdown de seleção.
   */
  async function handleSearchTenants(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSearchingTenants(true)
    try {
      const found = await listTenantsByEmail(email.trim())
      if (found.length === 0) {
        setError('Nenhuma empresa encontrada para este e-mail. Verifique o e-mail ou cadastre-se.')
        setTenants([])
        return
      }
      setTenants(found)
      // Se há apenas uma empresa, seleciona automaticamente e faz login direto.
      if (found.length === 1) {
        setSelectedTenant(found[0].uuid)
        await doLogin(found[0].uuid)
      }
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setSearchingTenants(false)
    }
  }

  /**
   * Passo 2: login efetivo com email + senha + tenant selecionado.
   * Recebe o tenantUuid como parâmetro para suportar o caso de auto-login
   * (uma única empresa) e o caso normal (seleção manual no dropdown).
   */
  async function doLogin(tenantUuid: string) {
    setSubmitting(true)
    try {
      await signIn({ email: email.trim(), password, tenantUuid })
      navigate(from, { replace: true })
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!selectedTenant) {
      setError('Selecione uma empresa para continuar.')
      return
    }
    await doLogin(selectedTenant)
  }

  const tenantOptions = tenants.map((t) => ({
    value: t.uuid,
    label: t.displayName,
  }))

  const showTenantSelect = tenants.length > 1

  return (
    <AuthCard
      title="Acessar sua conta"
    >
      {!showTenantSelect ? (
        // Etapa 1: email + senha (buscar empresas)
        <form onSubmit={handleSearchTenants} className="flex flex-col gap-4" noValidate>
          {error ? <Alert variant="error">{error}</Alert> : null}

          <Input
            label="E-mail"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <Input
            label="Senha"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <Button
            type="submit"
            isLoading={isSearchingTenants}
            fullWidth
            size="lg"
          >
            <Search className="h-4 w-4" />
            Continuar
          </Button>
        </form>
      ) : (
        // Etapa 2: seleção de empresa + login
        <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
          {error ? <Alert variant="error">{error}</Alert> : null}

          <div className="flex items-center gap-2 rounded-lg bg-slate-50 px-3 py-2.5 text-sm text-slate-600 dark:bg-slate-800 dark:text-slate-300">
            <span className="font-medium">{email}</span>
            <button
              type="button"
              onClick={() => {
                setTenants([])
                setSelectedTenant('')
                setError(null)
              }}
              className="ml-auto text-xs font-medium text-primary hover:text-primary-600"
            >
              Trocar e-mail
            </button>
          </div>

          <Select
            label="Empresa"
            placeholder="Selecione a empresa"
            options={tenantOptions}
            required
            value={selectedTenant}
            onChange={(e) => setSelectedTenant(e.target.value)}
            leftAdornment={<Building2 className="h-4 w-4" />}
          />

          <Button
            type="submit"
            isLoading={isSubmitting}
            fullWidth
            size="lg"
          >
            <LogIn className="h-4 w-4" />
            Entrar
          </Button>
        </form>
      )}
    </AuthCard>
  )
}