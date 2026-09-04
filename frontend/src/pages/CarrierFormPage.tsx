import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Power, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { CarrierForm } from '../components/client/CarrierForm'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  activateCarrier,
  createCarrier,
  getCarrier,
  inactivateCarrier,
  updateCarrier,
} from '../api/carrier.api'
import type {
  CarrierCreateRequest,
  CarrierResponse,
  CarrierUpdateRequest,
} from '../types/carrier'
import { toApiError } from '../lib/errors'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar uma transportadora.
 * - /carriers/new        → modo create
 * - /carriers/:id        → modo view (carrega GET /carriers/{id})
 */
export function CarrierFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [mode, setMode] = useState<Mode>('loading')
  const [carrier, setCarrier] = useState<CarrierResponse | null>(null)
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
	    getCarrier(Number(id!))
      .then((data) => {
        if (cancelled) return
        setCarrier(data)
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

  async function handleCreate(payload: CarrierCreateRequest) {
    setSaving(true)
    try {
      await createCarrier(payload)
      // Após salvar, redireciona para a lista (com `replace` para que o
      // botão Voltar do navegador não traga o usuário de volta para o
      // formulário já enviado). O item recém-criado aparecerá na lista
      // após a recarga automática.
      navigate('/carriers', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: CarrierUpdateRequest) {
    if (!carrier) return
    setSaving(true)
    try {
      const updated = await updateCarrier(carrier.id, payload)
      setCarrier(updated)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus() {
    if (!carrier) return
    setToggling(true)
    setToggleError(null)
    try {
      if (carrier.status === 'ATIVO') {
        await inactivateCarrier(carrier.id)
	        try {
	          const fresh = await getCarrier(carrier.id)
          setCarrier(fresh)
        } catch {
          setCarrier({ ...carrier, status: 'INATIVO' })
        }
      } else {
        const updated = await activateCarrier(carrier.id)
        setCarrier(updated)
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
            {mode === 'create' ? 'Nova transportadora' : carrier?.name ?? 'Transportadora'}
          </h1>
          {mode === 'view' && carrier ? (
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <RegistrationStatusBadge status={carrier.status} />
            </div>
          ) : mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para cadastrar uma nova transportadora.
            </p>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {mode === 'view' && carrier ? (
            <Button
              variant={carrier.status === 'ATIVO' ? 'secondary' : 'primary'}
              onClick={() => {
                setToggleError(null)
                setConfirmToggle(true)
              }}
            >
              <Power className="h-4 w-4" />
              {carrier.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
            </Button>
          ) : null}

          {mode !== 'loading' ? (
            <>
              <Button
                type="button"
                variant="secondary"
                onClick={() => navigate('/carriers')}
                size="md"
              >
                <X className="h-4 w-4" />
                Cancelar
              </Button>
              <Button
                type="submit"
                form="carrier-form"
                isLoading={saving}
                size="md"
              >
                <Save className="h-4 w-4" />
                {mode === 'view' ? 'Salvar alterações' : 'Cadastrar transportadora'}
              </Button>
            </>
          ) : null}
        </div>
      </div>

      {loadError ? (
        <Alert variant="error">
          {loadError}.{' '}
          <BackButton />
        </Alert>
      ) : null}

      {mode === 'view' && carrier ? (
        <RegistrationAuditCard
          createdBy={carrier.createdBy}
          createdAt={carrier.createdAt}
          updatedBy={carrier.updatedBy}
          updatedAt={carrier.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <CarrierForm
          carrier={mode === 'view' ? carrier ?? undefined : undefined}
          onSaveCreate={handleCreate}
          onSaveUpdate={handleUpdate}
        />
      )}

      <ConfirmDialog
        open={confirmToggle}
        title={
          carrier?.status === 'ATIVO' ? 'Inativar transportadora?' : 'Reativar transportadora?'
        }
        description={
          carrier?.status === 'ATIVO'
            ? `A transportadora "${carrier?.name}" será marcada como inativa. O registro não é apagado e pode ser reativado depois.`
            : `A transportadora "${carrier?.name}" voltará a ficar ativa.`
        }
        confirmText={carrier?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={carrier?.status === 'ATIVO' ? 'danger' : 'primary'}
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