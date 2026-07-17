import { useState, type FormEvent } from 'react'
import type {
  ServiceCategory,
  ServiceTemplateCreateRequest,
  ServiceTemplateResponse,
  ServiceTemplateUpdateRequest,
} from '../../types/servicetemplate'
import { SERVICE_CATEGORIES } from '../../types/servicetemplate'
import { Select } from '../ui/Select'
import { RichTextEditor } from '../ui/RichTextEditor'
import { Alert } from '../ui/Alert'
import { toApiError } from '../../lib/errors'
import { useFieldTouched } from '../../hooks/useFieldTouched'

interface ServiceTemplateFormProps {
  /** Serviço existente (modo edição). Quando omitido, é cadastro novo. */
  serviceTemplate?: ServiceTemplateResponse
  onSaveCreate: (payload: ServiceTemplateCreateRequest) => Promise<void>
  onSaveUpdate: (payload: ServiceTemplateUpdateRequest) => Promise<void>
}

export function ServiceTemplateForm({
  serviceTemplate,
  onSaveCreate,
  onSaveUpdate,
}: ServiceTemplateFormProps) {
  const isEdit = !!serviceTemplate
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const {
    shouldShowError,
    getBlurHandler,
    markAllTouched,
    reset,
  } = useFieldTouched()

  const [description, setDescription] = useState(serviceTemplate?.description ?? '')
  const [category, setCategory] = useState<ServiceCategory>(serviceTemplate?.category ?? 'EXECUÇÃO_SPDA')

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!category) errs.category = 'Categoria é obrigatória.'

    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
    markAllTouched()
    if (!validateAll()) return

    try {
      if (isEdit && serviceTemplate) {
        const payload: ServiceTemplateUpdateRequest = {
          description: description || null,
          category,
        }
        await onSaveUpdate(payload)
        setSuccess('Serviço atualizado com sucesso!')
        reset()
      } else {
        const payload: ServiceTemplateCreateRequest = {
          description: description || null,
          category,
        }
        await onSaveCreate(payload)
        setSuccess('Serviço criado com sucesso!')
        reset()
      }
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) {
        setFieldErrors(apiErr.fieldErrors)
      }
    }
  }

  return (
    <form
      id="service-template-form"
      onSubmit={handleSubmit}
      className="flex flex-col gap-6"
      noValidate
    >
      {formError ? <Alert variant="error">{formError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}

      {/* Categoria */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Categoria</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Classificação do serviço no catálogo.
        </p>

        <Select
          label="Categoria"
          value={category}
          onChange={(e) => setCategory(e.target.value as ServiceCategory)}
          onBlur={getBlurHandler('category')}
          error={shouldShowError('category', fieldErrors.category)}
          required
          options={SERVICE_CATEGORIES}
          placeholder="Selecione uma categoria"
        />
      </section>

      {/* Descrição */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Descrição dos Serviços</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Apresentação do serviço com formatação rica. Este texto pode ser
          reutilizado em propostas e contratos.
        </p>

        <RichTextEditor
          value={description}
          onChange={setDescription}
          onBlur={getBlurHandler('description')}
          placeholder="Descreva o serviço detalhadamente…"
          maxLength={10000}
        />
        {shouldShowError('description', fieldErrors.description) ? (
          <p className="mt-1 text-xs text-red-600 dark:text-red-400">
            {fieldErrors.description}
          </p>
        ) : null}
      </section>
    </form>
  )
}
