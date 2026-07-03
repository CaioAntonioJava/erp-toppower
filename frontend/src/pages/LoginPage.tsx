import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { LogIn } from 'lucide-react'
import { AuthCard } from '../components/layout/AuthCard'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Alert } from '../components/ui/Alert'
import { useAuth } from '../context/AuthContext'
import { toApiError } from '../lib/errors'

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
  const [isSubmitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await signIn({ email: email.trim(), password })
      navigate(from, { replace: true })
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Acessar sua conta"
      footer={
        <>
          Não tem uma conta?{' '}
          <Link
            to="/register"
            className="font-medium text-primary hover:text-primary-600"
          >
            Cadastre-se
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
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
          isLoading={isSubmitting}
          fullWidth
          size="lg"
        >
          <LogIn className="h-4 w-4" />
          Entrar
        </Button>
      </form>
    </AuthCard>
  )
}
