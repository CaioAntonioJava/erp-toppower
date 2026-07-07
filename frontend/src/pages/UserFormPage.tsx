import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Building2, Info, Mail, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Input } from '../components/ui/Input'
import { Alert } from '../components/ui/Alert'
import { Spinner } from '../components/ui/Spinner'
import { createUser } from '../api/user.api'
import { listAll } from '../api/organization.api'
import { assignUserToOrganization } from '../api/userOrganization.api'
import type {
  OrganizationSummary,
  RegisterRequest,
  UserResponse,
} from '../types/api'
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
 * Fluxo unificado: o admin cria o usuário (que sempre nasce como ROLE_MANAGER)
 * e, opcionalmente, já o vincula a uma ou mais Organizations. Cada vínculo é
 * criado via POST /api/v1/user-organizations logo após o cadastro.
 */
export function UserFormPage() {
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>(EMPTY)
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  // ===== estado do bloco de empresas =====
  const [orgs, setOrgs] = useState<OrganizationSummary[]>([])
  const [loadingOrgs, setLoadingOrgs] = useState(true)
  const [orgsError, setOrgsError] = useState<string | null>(null)
  const [selectedOrgIds, setSelectedOrgIds] = useState<Set<string>>(new Set())

  useEffect(() => {
    let cancelled = false
    setLoadingOrgs(true)
    setOrgsError(null)
    listAll()
      .then((data) => {
        if (cancelled) return
        setOrgs(data)
      })
      .catch((err) => {
        if (cancelled) return
        setOrgsError(toApiError(err).message)
        setOrgs([])
      })
      .finally(() => {
        if (cancelled) return
        setLoadingOrgs(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  function update(field: keyof FormState, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
  }

  function toggleOrg(orgId: string) {
    setSelectedOrgIds((prev) => {
      const next = new Set(prev)
      if (next.has(orgId)) next.delete(orgId)
      else next.add(orgId)
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
    if (Object.keys(next).length === 0) setSubmitError(null)
    return Object.keys(next).length === 0
  }

  /**
   * Cria o usuário e, em seguida, vincula cada Organization selecionada.
   * Se algum vínculo falhar no meio do caminho, o usuário permanece criado
   * e o erro é exibido com a lista do que foi vinculado com sucesso —
   * o admin pode completar manualmente depois.
   */
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
    let createdUser: UserResponse | null = null
    try {
      createdUser = await createUser(payload)
    } catch (err) {
      const apiError = toApiError(err)
      if (apiError.status === 409) {
        setErrors({ email: apiError.message })
      } else if (apiError.fieldErrors?.email) {
        setErrors({ email: apiError.fieldErrors.email })
      } else {
        setSubmitError(apiError.message)
      }
      setSaving(false)
      return
    }

    // ===== vínculos =====
    if (selectedOrgIds.size === 0) {
      setSaving(false)
      navigate('/users', { replace: true })
      return
    }

    const linked: string[] = []
    const failed: { orgName: string; reason: string }[] = []
    const orgsToLink = orgs.filter((o) => selectedOrgIds.has(o.uuid))

    for (const org of orgsToLink) {
      try {
        await assignUserToOrganization({
          userId: createdUser.uuid,
          organizationId: org.uuid,
          role: 'ROLE_MANAGER',
          isDefault: false,
        })
        linked.push(org.corporateName)
      } catch (err) {
        failed.push({
          orgName: org.corporateName,
          reason: toApiError(err).message,
        })
      }
    }

    setSaving(false)

    if (failed.length === 0) {
      navigate('/users', { replace: true })
      return
    }

    // Falha parcial: usuário criado, mas nem todos os vínculos.
    setSubmitError(
      `Usuário "${createdUser.email}" criado. ` +
        `Vínculos realizados: ${linked.length}/${orgsToLink.length}. ` +
        `Falhas: ${failed.map((f) => `${f.orgName} (${f.reason})`).join('; ')}. ` +
        `Tente vincular manualmente pela gestão de usuários.`,
    )
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
            O cadastro é feito como <strong>Gestor</strong>, o e-mail deve ser
            único no sistema e a atribuição de empresas é opcional — você
            pode vincular depois.
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

        {/* ===== Bloco de seleção de empresas ===== */}
        <section className="space-y-3 border-t border-slate-200 pt-5 dark:border-slate-800">
          <div className="flex items-start justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <Building2 className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                <h2 className="text-sm font-medium text-slate-700 dark:text-slate-200">
                  Empresas
                </h2>
                {selectedOrgIds.size > 0 ? (
                  <span className="inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
                    {selectedOrgIds.size} selecionada(s)
                  </span>
                ) : null}
              </div>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Selecione as empresas às quais este usuário terá acesso. Opcional.
              </p>
            </div>
          </div>

          {loadingOrgs ? (
            <div className="inline-flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <Spinner size="sm" /> Carregando empresas…
            </div>
          ) : orgsError ? (
            <Alert variant="error">{orgsError}</Alert>
          ) : orgs.length === 0 ? (
            <p className="rounded-lg border border-dashed border-slate-300 px-3 py-4 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
              Nenhuma empresa ativa cadastrada.
            </p>
          ) : (
            <ul className="max-h-64 space-y-1 overflow-y-auto rounded-lg border border-slate-200 p-2 dark:border-slate-700">
              {orgs.map((org) => {
                const checked = selectedOrgIds.has(org.uuid)
                return (
                  <li key={org.uuid}>
                    <label
                      className={[
                        'flex cursor-pointer items-start gap-3 rounded-md px-2 py-2 text-sm transition-colors',
                        checked
                          ? 'bg-primary-50 dark:bg-primary-900/20'
                          : 'hover:bg-slate-50 dark:hover:bg-slate-800/40',
                      ].join(' ')}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggleOrg(org.uuid)}
                        className="mt-0.5 h-4 w-4 rounded border-slate-300 text-primary focus:ring-focus dark:border-slate-600"
                      />
                      <span className="flex flex-col">
                        <span className="font-medium text-slate-900 dark:text-slate-100">
                          {org.corporateName}
                        </span>
                        <span className="text-xs text-slate-500 dark:text-slate-400">
                          {org.tradeName} · {org.cnpj}
                        </span>
                      </span>
                    </label>
                  </li>
                )
              })}
            </ul>
          )}
        </section>
      </form>
    </div>
  )
}