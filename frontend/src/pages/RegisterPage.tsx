import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { UserPlus } from 'lucide-react'
import { AuthCard } from '../components/layout/AuthCard'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Alert } from '../components/ui/Alert'
import { useAuth } from '../context/AuthContext'
import { toApiError } from '../lib/errors'

const PASSWORD_MIN = 8

/** Validação client-side espelhando o backend (LoginRequest/UserCreateRequest). */
function validate(values: { email: string; password: string; confirm: string }) {
  const errors: Record<string, string> = {}
  if (!values.email.trim()) {
    errors.email = 'E-mail é obrigatório.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
    errors.email = 'E-mail inválido.'
  }
  if (!values.password) {
    errors.password = 'Senha é obrigatória.'
  } else if (values.password.length < PASSWORD_MIN) {
    errors.password = `A senha deve ter no mínimo ${PASSWORD_MIN} caracteres.`
  }
  if (values.password !== values.confirm) {
    errors.passwordConfirmation = 'A confirmação precisa ser igual à senha.'
  }
  return errors
}

export function RegisterPage() {
  const { signUp } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [isSubmitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    const local = validate({ email, password, confirm })
    setFieldErrors(local)
    if (Object.keys(local).length > 0) return

    setSubmitting(true)
    try {
      await signUp({
        email: email.trim(),
        password,
        passwordConfirmation: confirm,
      })
      setSuccess(true)
      // signUp já autentica o usuário recém-criado — segue direto para o
      // dashboard, sem precisar passar pela tela de login.
      navigate('/', { replace: true })
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) setFieldErrors(apiErr.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Criar sua conta"
      subtitle="Cadastre-se para acessar o ERP TopPower"
      footer={
        <>
          Já tem uma conta?{' '}
          <Link
            to="/login"
            className="font-medium text-primary hover:text-primary-600"
          >
            Entrar
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        {formError ? <Alert variant="error">{formError}</Alert> : null}
        {success ? (
          <Alert variant="success">
            Conta criada com sucesso! Entrando no sistema…
          </Alert>
        ) : null}

        <Input
          label="E-mail"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={fieldErrors.email}
        />

        <Input
          label="Senha"
          type="password"
          autoComplete="new-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          hint={`Mínimo de ${PASSWORD_MIN} caracteres.`}
        />

        <Input
          label="Confirmação de senha"
          type="password"
          autoComplete="new-password"
          required
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          error={fieldErrors.passwordConfirmation}
        />

        <Button
          type="submit"
          isLoading={isSubmitting}
          fullWidth
          size="lg"
        >
          <UserPlus className="h-4 w-4" />
          Cadastrar
        </Button>
      </form>
    </AuthCard>
  )
}
