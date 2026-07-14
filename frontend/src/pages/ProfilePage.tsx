import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { KeyRound, Save, UserCircle2 } from 'lucide-react'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Spinner } from '../components/ui/Spinner'
import { useAuth } from '../context/AuthContext'
import { toApiError } from '../lib/errors'
import {
  createProfile,
  getProfileByUserId,
  updateProfile,
} from '../api/profile.api'
import { changePassword } from '../api/user.api'
import type {
  ProfileCreateRequest,
  ProfileResponse,
  ProfileStatus,
  ProfileUpdateRequest,
} from '../types/api'


/* -------------------------- validações client-side ------------------------- */

function maskPhone(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.length <= 2) return digits
  if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`
  if (digits.length <= 10)
    return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`
}

function maskCpf(value: string) {
  const d = value.replace(/\D/g, '').slice(0, 11)
  if (d.length <= 3) return d
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`
}

function validateProfile(values: {
  name: string
  email: string
  phone: string
  cpf: string
}): Record<string, string> {
  const errs: Record<string, string> = {}
  if (!values.name.trim()) errs.name = 'Nome é obrigatório.'
  if (!values.email.trim()) {
    errs.email = 'E-mail é obrigatório.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
    errs.email = 'E-mail inválido.'
  }
  if (!values.phone.trim()) {
    errs.phone = 'Telefone é obrigatório.'
  } else if (values.phone.replace(/\D/g, '').length < 10) {
    errs.phone = 'Telefone incompleto.'
  }
  if (!values.cpf.trim()) {
    errs.cpf = 'CPF é obrigatório.'
  } else if (values.cpf.replace(/\D/g, '').length !== 11) {
    errs.cpf = 'CPF deve conter 11 dígitos.'
  }
  return errs
}

function validatePassword(values: {
  current: string
  next: string
  confirm: string
}): Record<string, string> {
  const errs: Record<string, string> = {}
  if (!values.current) errs.currentPassword = 'Senha atual é obrigatória.'
  if (!values.next) {
    errs.newPassword = 'Nova senha é obrigatória.'
  } else if (values.next.length < 8) {
    errs.newPassword = 'A nova senha deve ter no mínimo 8 caracteres.'
  }
  if (values.next !== values.confirm) {
    errs.newPasswordConfirmation =
      'A confirmação precisa ser igual à nova senha.'
  }
  return errs
}

/* -------------------------------- componente ------------------------------- */

type LoadState = 'loading' | 'not-found' | 'loaded' | 'error'

export function ProfilePage() {
  const { user, refreshProfileStatus } = useAuth()
  const navigate = useNavigate()

  /* ---- perfil ---- */
  const [loadState, setLoadState] = useState<LoadState>('loading')
  const [profile, setProfile] = useState<ProfileResponse | null>(null)

  const [name, setName] = useState('')
  const [pEmail, setPEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [cpf, setCpf] = useState('')
  const [status, setStatus] = useState<ProfileStatus>('ATIVO')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [savingProfile, setSavingProfile] = useState(false)

  /* ---- senha ---- */
  const [currentPwd, setCurrentPwd] = useState('')
  const [newPwd, setNewPwd] = useState('')
  const [confirmPwd, setConfirmPwd] = useState('')
  const [pwdErrors, setPwdErrors] = useState<Record<string, string>>({})
  const [pwdError, setPwdError] = useState<string | null>(null)
  const [pwdSuccess, setPwdSuccess] = useState<string | null>(null)
  const [savingPwd, setSavingPwd] = useState(false)

  /* Carrega o perfil ao montar. */
  useEffect(() => {
    if (!user) return
    let cancelled = false
    setLoadState('loading')
    getProfileByUserId(user.id)
      .then((data) => {
        if (cancelled) return
        setProfile(data)
        setName(data.name)
        setPEmail(data.email)
        setPhone(data.phone)
        setCpf(data.cpf)
        setStatus(data.status)
        setLoadState('loaded')
      })
      .catch((err) => {
        if (cancelled) return
        const apiErr = toApiError(err)
        if (apiErr.status === 404) {
          setProfile(null)
          setLoadState('not-found')
        } else {
          setFormError(apiErr.message)
          setLoadState('error')
        }
      })
    return () => {
      cancelled = true
    }
  }, [user])

  async function handleProfileSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
    const local = validateProfile({ name, email: pEmail, phone, cpf })
    setFieldErrors(local)
    if (Object.keys(local).length > 0) return

    if (!user) return
    setSavingProfile(true)
    try {
      if (loadState === 'not-found' || !profile) {
        const payload: ProfileCreateRequest = {
          name: name.trim(),
          email: pEmail.trim(),
          phone: phone.trim(),
          cpf: cpf.trim(),
          status,
        }
        const created = await createProfile(payload)
        setProfile(created)
        setLoadState('loaded')
        setSuccess('Perfil criado com sucesso!')
        // Atualiza o gate do AuthContext para liberar a navegação
        // para as demais rotas do sistema.
        await refreshProfileStatus()
        // Após criar o perfil pela primeira vez (fluxo de cadastro),
        // redireciona para o dashboard.
        navigate('/', { replace: true })
      } else {
        const payload: ProfileUpdateRequest = {
          name: name.trim(),
          email: pEmail.trim(),
          phone: phone.trim(),
          cpf: cpf.trim(),
          status,
        }
        const updated = await updateProfile(profile.id, payload)
        setProfile(updated)
        setSuccess('Perfil atualizado com sucesso!')
      }
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) setFieldErrors(apiErr.fieldErrors)
    } finally {
      setSavingProfile(false)
    }
  }

  async function handlePasswordSubmit(e: FormEvent) {
    e.preventDefault()
    setPwdError(null)
    setPwdSuccess(null)
    const local = validatePassword({
      current: currentPwd,
      next: newPwd,
      confirm: confirmPwd,
    })
    setPwdErrors(local)
    if (Object.keys(local).length > 0) return

    if (!user) return
    setSavingPwd(true)
    try {
      await changePassword(user.id, {
        currentPassword: currentPwd,
        newPassword: newPwd,
      })
      setPwdSuccess('Senha alterada com sucesso!')
      setCurrentPwd('')
      setNewPwd('')
      setConfirmPwd('')
    } catch (err) {
      setPwdError(toApiError(err).message)
    } finally {
      setSavingPwd(false)
    }
  }

  if (loadState === 'loading') {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  const heading =
    loadState === 'not-found' ? 'Complete seu perfil' : 'Meu perfil'

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{heading}</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          {loadState === 'not-found'
            ? 'Preencha seus dados pessoais para acessar todas as funcionalidades.'
            : 'Atualize seus dados pessoais e sua senha de acesso.'}
        </p>
      </div>

      {/* ---- card: dados pessoais ---- */}
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-5 flex items-center gap-2">
          <div className="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300">
            <UserCircle2 className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-semibold">Dados pessoais</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {loadState === 'not-found'
                ? 'Crie agora o seu perfil.'
                : 'Edite seus dados sempre que precisar.'}
            </p>
          </div>
        </div>

        {formError ? (
          <div className="mb-4">
            <Alert variant="error">{formError}</Alert>
          </div>
        ) : null}
        {success ? (
          <div className="mb-4">
            <Alert variant="success">{success}</Alert>
          </div>
        ) : null}

        <form onSubmit={handleProfileSubmit} className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Nome completo"
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={fieldErrors.name}
            required
            className="sm:col-span-2"
          />
          <Input
            label="E-mail de contato"
            type="email"
            value={pEmail}
            onChange={(e) => setPEmail(e.target.value)}
            error={fieldErrors.email}
            required
          />
          <Input
            label="Telefone"
            value={phone}
            onChange={(e) => setPhone(maskPhone(e.target.value))}
            error={fieldErrors.phone}
            required
          />
          <Input
            label="CPF"
            value={cpf}
            onChange={(e) => setCpf(maskCpf(e.target.value))}
            error={fieldErrors.cpf}
            required
            maxLength={14}
          />

          <div className="sm:col-span-2">
            <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
              Status
            </label>
            <div className="flex gap-2">
              {(['ATIVO', 'INATIVO'] as ProfileStatus[]).map((s) => (
                <button
                  type="button"
                  key={s}
                  onClick={() => setStatus(s)}
                  className={[
                    'inline-flex h-10 items-center rounded-lg border px-3 text-sm font-medium transition-colors',
                    status === s
                      ? 'border-primary bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-200'
                      : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800',
                  ].join(' ')}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          <div className="flex justify-end sm:col-span-2">
            <Button type="submit" isLoading={savingProfile}>
              <Save className="h-4 w-4" />
              {loadState === 'not-found' ? 'Criar perfil' : 'Salvar alterações'}
            </Button>
          </div>
        </form>
      </div>

      {/* ---- card: senha ---- */}
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-5 flex items-center gap-2">
          <div className="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300">
            <KeyRound className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-semibold">Trocar senha</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Use sua senha atual para confirmar a alteração.
            </p>
          </div>
        </div>

        {pwdError ? (
          <div className="mb-4">
            <Alert variant="error">{pwdError}</Alert>
          </div>
        ) : null}
        {pwdSuccess ? (
          <div className="mb-4">
            <Alert variant="success">{pwdSuccess}</Alert>
          </div>
        ) : null}

        <form onSubmit={handlePasswordSubmit} className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Senha atual"
            type="password"
            autoComplete="current-password"
            value={currentPwd}
            onChange={(e) => setCurrentPwd(e.target.value)}
            error={pwdErrors.currentPassword}
            required
            className="sm:col-span-2"
          />
          <Input
            label="Nova senha"
            type="password"
            autoComplete="new-password"
            value={newPwd}
            onChange={(e) => setNewPwd(e.target.value)}
            error={pwdErrors.newPassword}
            required
            hint="Mínimo 8 caracteres."
          />
          <Input
            label="Confirmação da nova senha"
            type="password"
            autoComplete="new-password"
            value={confirmPwd}
            onChange={(e) => setConfirmPwd(e.target.value)}
            error={pwdErrors.newPasswordConfirmation}
            required
          />
          <div className="flex justify-end sm:col-span-2">
            <Button type="submit" isLoading={savingPwd}>
              <Save className="h-4 w-4" />
              Atualizar senha
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
