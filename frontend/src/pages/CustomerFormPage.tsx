import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Power } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { CustomerForm } from '../components/client/CustomerForm'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  activateCustomer,
  createCustomer,
  getCustomer,
  inactivateCustomer,
  updateCustomer,
} from '../api/customer.api'
import type {
  CustomerCreateRequest,
  CustomerResponse,
  CustomerUpdateRequest,
} from '../types/customer'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar um cliente (pessoa física).
 * - /customers/new        → modo create
 * - /customers/:id        → modo view (carrega GET /customers/{id})
 */
export function CustomerFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [customer, setCustomer] = useState<CustomerResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [confirmToggle, setConfirmToggle] = useState(false)
  const [toggling, setToggling] = useState(false)
  const [toggleError, setToggleError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      setMode('create')
      return
    }
    let cancelled = false
    setMode('loading')
    setLoadError(null)
    getCustomer(id)
      .then((data) => {
        if (cancelled) return
        setCustomer(data)
        setMode('view')
      })
      .catch((err) => {
        if (cancelled) return
        setLoadError(toApiError(err).message)
        setMode('create')
      })
    return () => {
      cancelled = true
    }
  }, [id])

  async function handleCreate(payload: CustomerCreateRequest) {
    setSaving(true)
    try {
      await createCustomer(payload)
      // Após salvar, redireciona para a lista (com `replace` para que o
      // botão Voltar do navegador não traga o usuário de volta para o
      // formulário já enviado). O item recém-criado aparecerá na lista
      // após a recarga automática.
      navigate('/customers', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: CustomerUpdateRequest) {
    if (!customer) return
    setSaving(true)
    try {
      const updated = await updateCustomer(customer.uuid, payload)
      setCustomer(updated)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus() {
    if (!customer) return
    setToggling(true)
    setToggleError(null)
    try {
      if (customer.status === 'ATIVO') {
        await inactivateCustomer(customer.uuid)
        try {
          const fresh = await getCustomer(customer.uuid)
          setCustomer(fresh)
        } catch {
          setCustomer({ ...customer, status: 'INATIVO' })
        }
      } else {
        const updated = await activateCustomer(customer.uuid)
        setCustomer(updated)
      }
    } catch (err) {
      setToggleError(toApiError(err).message)
    } finally {
      setToggling(false)
      setConfirmToggle(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            {mode === 'create' ? 'Novo cliente' : customer?.name ?? 'Cliente'}
          </h1>
          {mode === 'view' && customer ? (
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono text-xs">{customer.code}</span>
              <span aria-hidden>•</span>
              <RegistrationStatusBadge status={customer.status} />
            </div>
          ) : mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para cadastrar um novo cliente (pessoa física).
            </p>
          ) : null}
        </div>

        {mode === 'view' && customer ? (
          <Button
            variant={customer.status === 'ATIVO' ? 'secondary' : 'primary'}
            onClick={() => {
              setToggleError(null)
              setConfirmToggle(true)
            }}
          >
            <Power className="h-4 w-4" />
            {customer.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
          </Button>
        ) : null}
      </div>

      {loadError ? (
        <Alert variant="error">
          {loadError}.{' '}
          <BackButton />
        </Alert>
      ) : null}

      {isAdmin && mode === 'view' && customer ? (
        <RegistrationAuditCard
          createdBy={customer.createdBy}
          createdAt={customer.createdAt}
          updatedBy={customer.updatedBy}
          updatedAt={customer.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <CustomerForm
          customer={mode === 'view' ? customer ?? undefined : undefined}
          isLoading={saving}
          onSaveCreate={handleCreate}
          onSaveUpdate={handleUpdate}
        />
      )}

      <ConfirmDialog
        open={confirmToggle}
        title={
          customer?.status === 'ATIVO' ? 'Inativar cliente?' : 'Reativar cliente?'
        }
        description={
          customer?.status === 'ATIVO'
            ? `O cliente "${customer?.name}" será marcado como inativo. O registro não é apagado e pode ser reativado depois.`
            : `O cliente "${customer?.name}" voltará a ficar ativo.`
        }
        confirmText={customer?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={customer?.status === 'ATIVO' ? 'danger' : 'primary'}
        isLoading={toggling}
        onConfirm={handleToggleStatus}
        onClose={() => {
          if (!toggling) setConfirmToggle(false)
        }}
      />

      {toggleError ? <Alert variant="error">{toggleError}</Alert> : null}
    </div>
  )
}
