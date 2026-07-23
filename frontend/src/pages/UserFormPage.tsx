import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Building2, Info, Mail, Save, ShieldCheck, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Alert } from '../components/ui/Alert'
import { Spinner } from '../components/ui/Spinner'
import { createUser, getUser, updateUser } from '../api/user.api'
import { listAll } from '../api/organization.api'
import { assignUserToOrganization } from '../api/userOrganization.api'
import { MODULES, MODULE_SECTIONS } from '../lib/modules'
import type {
  Module,
  OrganizationSummary,
  RegisterRequest,
  Role,
  UserResponse,
} from '../types/api'
import { toApiError } from '../lib/errors'

interface FormState {
  email: string
  password: string
  passwordConfirmation: string
  role: Role
}

const EMPTY: FormState = {
  email: '',
  password: '',
  passwordConfirmation: '',
  role: 'ROLE_EMPLOYEE',
}

const ROLE_OPTIONS = [
  { value: 'ROLE_ADMIN', label: 'Administrador' },
  { value: 'ROLE_MANAGER', label: 'Gestor' },
  { value: 'ROLE_EMPLOYEE', label: 'Funcionário' },
]

/**
 * Página de cadastro/edição de usuário (acesso restrito a ROLE_ADMIN).
 * Rotas: /users/new (criação) e /users/:id (edição).
 *
 * O admin seleciona o papel (Administrador, Gestor ou Funcionário) e,
 * quando o papel é Funcionário, marca os painéis (módulos) que ele
 * poderá acessar. Administrador e Gestor têm acesso total aos painéis
 * de negócio, então as permissões são ignoradas nesses casos.
 */
export function UserFormPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id?: string }>()
  const isEdit = !!id

  const [form, setForm] = useState<FormState>(EMPTY)
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [loadingUser, setLoadingUser] = useState(isEdit)

  // ===== estado do bloco de empresas =====
  const [orgs, setOrgs] = useState<OrganizationSummary[]>([])
  const [loadingOrgs, setLoadingOrgs] = useState(true)
  const [orgsError, setOrgsError] = useState<string | null>(null)
  const [selectedOrgIds, setSelectedOrgIds] = useState<Set<string>>(new Set())

  // ===== estado do bloco de permissões (módulos) =====
  const [selectedModules, setSelectedModules] = useState<Set<Module>>(new Set())

  // Carrega dados do usuário em edição.
  useEffect(() => {
    if (!isEdit || id === undefined) return
    let cancelled = false
    setLoadingUser(true)
    getUser(Number(id))
      .then((u: UserResponse) => {
        if (cancelled) return
        setForm((prev) => ({
          ...prev,
          email: u.email,
          // Em edição não mexemos na senha; mantemos vazios.
          password: '',
          passwordConfirmation: '',
          role: u.role,
        }))
        // Apenas EMPLOYEE guarda módulos concedidos; ADMIN/MANAGER recebem
        // todos do backend, mas no formulário mostramos vazio (irrelevante).
        setSelectedModules(new Set(u.role === 'ROLE_EMPLOYEE' ? u.modules : []))
      })
      .catch((err) => {
        if (!cancelled) setSubmitError(toApiError(err).message)
      })
      .finally(() => {
        if (!cancelled) setLoadingUser(false)
      })
    return () => {
      cancelled = true
    }
  }, [id, isEdit])

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
        if (!cancelled) return
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

  function toggleModule(m: Module) {
    setSelectedModules((prev) => {
      const next = new Set(prev)
      if (next.has(m)) next.delete(m)
      else next.add(m)
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
    // Senha obrigatória apenas na criação.
    if (!isEdit && form.password.length < 8) {
      next.password = 'A senha deve ter ao menos 8 caracteres.'
    }
    if (!isEdit && form.passwordConfirmation !== form.password) {
      next.passwordConfirmation = 'As senhas não coincidem.'
    }
    setErrors(next)
    if (Object.keys(next).length === 0) setSubmitError(null)
    return Object.keys(next).length === 0
  }

  /**
   * Cria o usuário e, em seguida, vincula cada Organization selecionada.
   * Se algum vínculo falhar no meio do caminho, o usuário permanece criado
   * e o erro é exibido com a lista do que foi vinculado com sucesso.
   */
  async function handleCreate() {
    const modulesForRole =
      form.role === 'ROLE_EMPLOYEE' ? Array.from(selectedModules) : []

    const payload: RegisterRequest = {
      email: form.email.trim(),
      password: form.password,
      passwordConfirmation: form.passwordConfirmation,
      role: form.role,
      modules: modulesForRole,
    }

    const createdUser = await createUser(payload)

    // ===== vínculos =====
    if (selectedOrgIds.size === 0) {
      navigate('/users', { replace: true })
      return
    }

    const linked: string[] = []
    const failed: { orgName: string; reason: string }[] = []
    const orgsToLink = orgs.filter((o) => selectedOrgIds.has(String(o.id)))

    for (const org of orgsToLink) {
      try {
        await assignUserToOrganization({
          userId: createdUser.id,
          organizationId: org.id,
          role: form.role,
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

    if (failed.length === 0) {
      navigate('/users', { replace: true })
      return
    }

    setSubmitError(
      `Usuário "${createdUser.email}" criado. ` +
        `Vínculos realizados: ${linked.length}/${orgsToLink.length}. ` +
        `Falhas: ${failed.map((f) => `${f.orgName} (${f.reason})`).join('; ')}. ` +
        `Tente vincular manualmente pela gestão de usuários.`,
    )
  }

  /**
   * Atualiza apenas role e módulos (a senha é tratada à parte, via
   * "Redefinir senha" na listagem). O e-mail não é editável aqui.
   */
  async function handleUpdate() {
    if (id === undefined) return
    const modulesForRole =
      form.role === 'ROLE_EMPLOYEE' ? Array.from(selectedModules) : []
    await updateUser(Number(id), {
      role: form.role,
      modules: modulesForRole,
    })
    navigate('/users', { replace: true })
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitError(null)
    if (!validate()) return

    setSaving(true)
    try {
      if (isEdit) {
        await handleUpdate()
      } else {
        await handleCreate()
      }
    } catch (err) {
      const apiError = toApiError(err)
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

  const showModules = form.role === 'ROLE_EMPLOYEE'

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton fallback="/users" />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            {isEdit ? 'Editar usuário' : 'Novo usuário'}
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {isEdit
              ? 'Ajuste o papel e as permissões do usuário.'
              : 'Preencha os dados para conceder acesso ao sistema.'}
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
            isLoading={saving || loadingUser}
            size="md"
          >
            <Save className="h-4 w-4" />
            {isEdit ? 'Salvar alterações' : 'Cadastrar usuário'}
          </Button>
        </div>
      </div>

      <Alert variant="info">
        <div className="inline-flex items-start gap-2">
          <Info className="mt-0.5 h-4 w-4 shrink-0" />
          <span>
            <strong>Administrador</strong> e <strong>Gestor</strong> têm acesso
            total aos painéis de negócio. <strong>Funcionário</strong> acessa
            apenas os painéis marcados abaixo.
          </span>
        </div>
      </Alert>

      {submitError ? <Alert variant="error">{submitError}</Alert> : null}

      {loadingUser ? (
        <div className="inline-flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
          <Spinner size="sm" /> Carregando usuário…
        </div>
      ) : (
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
            disabled={isEdit}
            hint={isEdit ? 'O e-mail não pode ser alterado.' : undefined}
          />

          <Select
            label="Papel"
            required
            value={form.role}
            onChange={(e) => {
              update('role', e.target.value)
              // Ao sair de EMPLOYEE, limpa seleção de módulos (irrelevante).
              if (e.target.value !== 'ROLE_EMPLOYEE') {
                setSelectedModules(new Set())
              }
            }}
            options={ROLE_OPTIONS}
            leftAdornment={<ShieldCheck className="h-4 w-4" />}
          />

          {!isEdit ? (
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
          ) : null}

          {/* ===== Bloco de permissões (painéis) — visível apenas para EMPLOYEE ===== */}
          {showModules ? (
            <section className="space-y-3 border-t border-slate-200 pt-5 dark:border-slate-800">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <ShieldCheck className="h-4 w-4 text-slate-500 dark:text-slate-400" />
                    <h2 className="text-sm font-medium text-slate-700 dark:text-slate-200">
                      Permissões (Painéis)
                    </h2>
                    {selectedModules.size > 0 ? (
                      <span className="inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
                        {selectedModules.size} selecionado(s)
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                    Selecione os painéis que este funcionário poderá acessar.
                  </p>
                </div>
              </div>

              <div className="space-y-4">
                {MODULE_SECTIONS.map((sectionTitle) => {
                  const sectionModules = MODULES.filter(
                    (m) => m.section === sectionTitle,
                  )
                  return (
                    <div key={sectionTitle}>
                      <p className="mb-1 px-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                        {sectionTitle}
                      </p>
                      <ul className="space-y-1 rounded-lg border border-slate-200 p-2 dark:border-slate-700">
                        {sectionModules.map((m) => {
                          const checked = selectedModules.has(m.key)
                          return (
                            <li key={m.key}>
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
                                  onChange={() => toggleModule(m.key)}
                                  className="mt-0.5 h-4 w-4 rounded border-slate-300 text-primary focus:ring-focus dark:border-slate-600"
                                />
                                <span className="font-medium text-slate-900 dark:text-slate-100">
                                  {m.label}
                                </span>
                              </label>
                            </li>
                          )
                        })}
                      </ul>
                    </div>
                  )
                })}
              </div>
            </section>
          ) : null}

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
                  Selecione as empresas às quais este usuário terá acesso.{' '}
                  {isEdit
                    ? 'Os vínculos são gerenciados pela listagem.'
                    : 'Opcional — você pode vincular depois.'}
                </p>
              </div>
            </div>

            {isEdit ? (
              <p className="rounded-lg border border-dashed border-slate-300 px-3 py-4 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
                Use a ação “Empresas” na listagem para gerenciar vínculos.
              </p>
            ) : loadingOrgs ? (
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
                  const checked = selectedOrgIds.has(String(org.id))
                  return (
                    <li key={org.id}>
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
                          onChange={() => toggleOrg(String(org.id))}
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
      )}
    </div>
  )
}