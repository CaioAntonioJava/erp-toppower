import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Info, Mail, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Input } from '../components/ui/Input'
import { Alert } from '../components/ui/Alert'
import { createUser } from '../api/user.api'
import type { RegisterRequest } from '../types/api'
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
 * O usuário é cadastrado com o papel ROLE_MANAGER pelo backend.
 */
export function UserFormPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(EMPTY)
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  function update(field: keyof FormState, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
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
    if (Object.keys(next).length === 0) setSubmitError(null)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitError(null)
    if (!validate()) return

    const payload: RegisterRequest = {
      email: form.email.trim(),
      password: form.password,
      passwordConfirmation: form.passwordConfirmation,
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
            O cadastro é feito como <strong>Gestor</strong> e o e-mail deve ser
            único no sistema.
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
      </form>
    </div>
  )
}