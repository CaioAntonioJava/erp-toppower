import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Building2, Info, Mail, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Input } from '../components/ui/Input'
import { Alert } from '../components/ui/Alert'
import { Spinner } from '../components/ui/Spinner'
import { createUser } from '../api/user.api'
import { listTenants } from '../api/tenant.api'
import type { RegisterRequest, TenantSummary } from '../types/api'
import { toApiError } from '../lib/errors'

interface FormState {
  email: string
  password: string
  passwordConfirmation: string
}

const EMPTY: FormState = { email: '', password: '', passwordConfirmation: '' }

/**
 * Página de cadastro de usuário (acesso restrito a ROLE_ADMIN).
 * Rota: /users/new.
 *
 * O admin seleciona a quais empresas (tenants) o novo usuário terá acesso
 * — pode ser uma ou mais. O usuário é cadastrado com o papel ROLE_MANAGER
 * pelo backend.
 */
export function UserFormPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(EMPTY)
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [tenants, setTenants] = useState<TenantSummary[]>([])
  const [tenantsLoading, setTenantsLoading] = useState(true)
  const [tenantsError, setTenantsError] = useState<string | null>(null)
  const [selectedTenants, setSelectedTenants] = useState<Set<string>>(new Set())

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setTenantsLoading(true)
      try {
        const data = await listTenants()
        if (cancelled) return
        setTenants(data)
      } catch (err) {
        if (cancelled) return
        setTenantsError(toApiError(err).message)
      } finally {
        if (!cancelled) setTenantsLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  function update(field: keyof FormState, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
  }

  function toggleTenant(uuid: string) {
    setSelectedTenants((prev) => {
      const next = new Set(prev)
      if (next.has(uuid)) next.delete(uuid)
      else next.add(uuid)
      return next
    })
  }

  function validate(): boolean {
    const next: Partial<Record<keyof FormState, string>> = {}
    if (!form.email.trim()) {
      next.email = 'E-mail é obrigatório.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      next.email = 'Informe um e-mail válido.'
    }
    if (form.password.length < 8) {
      next.password = 'A senha deve ter ao menos 8 caracteres.'
    }
    if (form.passwordConfirmation !== form.password) {
      next.passwordConfirmation = 'As senhas não coincidem.'
    }
    setErrors(next)
    const tenantsValid = selectedTenants.size > 0
    if (!tenantsValid) setSubmitError('Selecione ao menos uma empresa.')
    else if (Object.keys(next).length === 0) setSubmitError(null)
    return Object.keys(next).length === 0 && tenantsValid
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitError(null)
    if (!validate()) return

    const payload: RegisterRequest = {
      email: form.email.trim(),
      password: form.password,
      passwordConfirmation: form.passwordConfirmation,
      tenantUuids: Array.from(selectedTenants),
    }

    setSaving(true)
    try {
      await createUser(payload)
      navigate('/users', { replace: true })
    } catch (err) {
      const apiError = toApiError(err)
      // Erro de e-mail duplicado (409) cai no campo; demais no Alert.
      if (apiError.status === 409) {
        setErrors({ email: apiError.message })
      } else if (apiError.fieldErrors?.email) {
        setErrors({ email: apiError.fieldErrors.email })
      } else if (apiError.fieldErrors?.tenantUuids) {
        setSubmitError(apiError.fieldErrors.tenantUuids)
      } else {
        setSubmitError(apiError.message)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton fallback="/users" />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">Novo usuário</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Preencha os dados para conceder acesso ao sistema.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate('/users')}
            size="md"
          >
            <X className="h-4 w-4" />
            Cancelar
          </Button>
          <Button
            type="submit"
            form="user-form"
            isLoading={saving}
            size="md"
          >
            <Save className="h-4 w-4" />
            Cadastrar usuário
          </Button>
        </div>
      </div>

      <Alert variant="info">
        <div className="inline-flex items-start gap-2">
          <Info className="mt-0.5 h-4 w-4 shrink-0" />
          <span>
            Selecione a quais empresas o usuário terá acesso. Ele poderá
            alternar entre elas no login. O cadastro é feito como{' '}
            <strong>Gestor</strong> e o e-mail deve ser único no sistema.
          </span>
        </div>
      </Alert>

      {submitError ? <Alert variant="error">{submitError}</Alert> : null}

      <form
        id="user-form"
        onSubmit={handleSubmit}
        className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        <Input
          label="E-mail"
          type="email"
          required
          autoComplete="email"
          placeholder="nome@empresa.com.br"
          value={form.email}
          onChange={(e) => update('email', e.target.value)}
          error={errors.email ?? null}
          leftAdornment={<Mail className="h-4 w-4" />}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Senha"
            type="password"
            required
            autoComplete="new-password"
            placeholder="Mínimo de 8 caracteres"
            value={form.password}
            onChange={(e) => update('password', e.target.value)}
            error={errors.password ?? null}
          />
          <Input
            label="Confirmar senha"
            type="password"
            required
            autoComplete="new-password"
            value={form.passwordConfirmation}
            onChange={(e) => update('passwordConfirmation', e.target.value)}
            error={errors.passwordConfirmation ?? null}
          />
        </div>

        <div>
          <div className="mb-2 flex items-center gap-2">
            <Building2 className="h-4 w-4 text-slate-400" />
            <span className="text-sm font-medium text-slate-700 dark:text-slate-200">
              Empresas com acesso
            </span>
            <span className="text-xs text-slate-500 dark:text-slate-400">
              (selecione uma ou mais)
            </span>
          </div>

          {tenantsLoading ? (
            <div className="inline-flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <Spinner size="sm" /> Carregando empresas…
            </div>
          ) : tenantsError ? (
            <Alert variant="error">{tenantsError}</Alert>
          ) : tenants.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Nenhuma empresa cadastrada.
            </p>
          ) : (
            <div className="grid gap-2 sm:grid-cols-2">
              {tenants.map((t) => {
                const checked = selectedTenants.has(t.uuid)
                return (
                  <label
                    key={t.uuid}
                    className={`flex cursor-pointer items-center gap-3 rounded-lg border px-3 py-2.5 text-sm transition ${
                      checked
                        ? 'border-primary bg-primary-50 text-primary-700 dark:border-primary-900 dark:bg-primary-900/20 dark:text-primary-200'
                        : 'border-slate-200 text-slate-700 hover:bg-slate-50 dark:border-slate-800 dark:text-slate-200 dark:hover:bg-slate-800/40'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggleTenant(t.uuid)}
                      className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary"
                    />
                    <span className="font-medium">{t.displayName}</span>
                    <span className="ml-auto text-xs text-slate-500 dark:text-slate-400">
                      {t.code}
                    </span>
                  </label>
                )
              })}
            </div>
          )}
        </div>
      </form>
    </div>
  )
}