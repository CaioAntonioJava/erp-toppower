import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ServiceTemplateForm } from '../components/client/ServiceTemplateForm'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  createServiceTemplate,
  getServiceTemplate,
  updateServiceTemplate,
} from '../api/servicetemplate.api'
import type {
  ServiceTemplateCreateRequest,
  ServiceTemplateResponse,
  ServiceTemplateUpdateRequest,
} from '../types/servicetemplate'
import { toApiError } from '../lib/errors'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar um serviço.
 * - /service-templates/new        → modo create
 * - /service-templates/:id        → modo view (carrega GET /service-templates/{id})
 */
export function ServiceTemplateFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [mode, setMode] = useState<Mode>('loading')
  const [serviceTemplate, setServiceTemplate] = useState<ServiceTemplateResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!id) {
      setMode('create')
      return
    }
    let cancelled = false
    setMode('loading')
    setLoadError(null)
    getServiceTemplate(Number(id!))
      .then((data) => {
        if (cancelled) return
        setServiceTemplate(data)
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

  async function handleCreate(payload: ServiceTemplateCreateRequest) {
    setSaving(true)
    try {
      await createServiceTemplate(payload)
      navigate('/service-templates', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: ServiceTemplateUpdateRequest) {
    if (!serviceTemplate) return
    setSaving(true)
    try {
      const updated = await updateServiceTemplate(serviceTemplate.id, payload)
      setServiceTemplate(updated)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            {mode === 'create' ? 'Novo serviço' : serviceTemplate?.name ?? 'Serviço'}
          </h1>
          {mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para cadastrar um novo serviço no catálogo.
            </p>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {mode !== 'loading' ? (
            <>
              <Button
                type="button"
                variant="secondary"
                onClick={() => navigate('/service-templates')}
                size="md"
              >
                <X className="h-4 w-4" />
                Cancelar
              </Button>
              <Button
                type="submit"
                form="service-template-form"
                isLoading={saving}
                size="md"
              >
                <Save className="h-4 w-4" />
                {mode === 'view' ? 'Salvar alterações' : 'Cadastrar serviço'}
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

      {mode === 'view' && serviceTemplate ? (
        <RegistrationAuditCard
          createdBy={serviceTemplate.createdBy}
          createdAt={serviceTemplate.createdAt}
          updatedBy={serviceTemplate.updatedBy}
          updatedAt={serviceTemplate.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <ServiceTemplateForm
          serviceTemplate={mode === 'view' ? serviceTemplate ?? undefined : undefined}
          onSaveCreate={handleCreate}
          onSaveUpdate={handleUpdate}
        />
      )}
    </div>
  )
}
