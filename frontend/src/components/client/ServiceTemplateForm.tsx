import { useEffect, useState, type FormEvent } from 'react'
import { Plus } from 'lucide-react'
import type {
  ServiceTemplateCreateRequest,
  ServiceTemplateResponse,
  ServiceTemplateUpdateRequest,
} from '../../types/servicetemplate'
import type { ServiceCategoryResponse } from '../../types/servicecategory'
import { listActiveServiceCategories } from '../../api/servicecategory.api'
import { ServiceCategoryFormModal } from './ServiceCategoryFormModal'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Button } from '../ui/Button'
import { Spinner } from '../ui/Spinner'
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

  const [name, setName] = useState(serviceTemplate?.name ?? '')
  const [description, setDescription] = useState(serviceTemplate?.description ?? '')
  const [categoryId, setCategoryId] = useState<number | ''>(serviceTemplate?.categoryId ?? '')
  const [categories, setCategories] = useState<ServiceCategoryResponse[]>([])
  const [categoriesLoading, setCategoriesLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)

  // Carrega as categorias ativas da API ao montar.
  useEffect(() => {
    setCategoriesLoading(true)
    listActiveServiceCategories()
      .then((cats) => setCategories(cats))
      .catch(() => setCategories([]))
      .finally(() => setCategoriesLoading(false))
  }, [])

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!name.trim()) errs.name = 'Nome é obrigatório.'
    if (!categoryId) errs.categoryId = 'Categoria é obrigatória.'

    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  // Callback do modal: adiciona a nova categoria na lista e auto-seleciona.
  function handleCategoryCreated(cat: ServiceCategoryResponse) {
    setCategories((prev) => [...prev, cat])
    setCategoryId(cat.id)
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
          name: name.trim(),
          description: description || null,
          categoryId: categoryId || undefined,
        }
        await onSaveUpdate(payload)
        setSuccess('Serviço atualizado com sucesso!')
        reset()
      } else {
        const payload: ServiceTemplateCreateRequest = {
          name: name.trim(),
          description: description || null,
          categoryId: Number(categoryId),
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

  const categoryOptions = categories.map((c) => ({ value: String(c.id), label: c.name }))

  return (
    <>
    <form
      id="service-template-form"
      onSubmit={handleSubmit}
      className="flex flex-col gap-6"
      noValidate
    >
      {formError ? <Alert variant="error">{formError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}

      {/* Identificação */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Identificação</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Dados básicos do serviço.
        </p>

        <div className="grid gap-4 sm:grid-cols-1">
          <Input
            label="Nome"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onBlur={getBlurHandler('name')}
            error={shouldShowError('name', fieldErrors.name)}
            required
            maxLength={200}
            hint="Nome do serviço (ex: INSTALAÇÃO DE QUADRO ELÉTRICO)."
          />
        </div>
      </section>

      {/* Categoria */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Categoria</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Classificação do serviço no catálogo. Selecione uma categoria
          existente ou cadastre uma nova.
        </p>

        {categoriesLoading ? (
          <div className="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
            <Spinner size="sm" /> Carregando categorias…
          </div>
        ) : (
          <div className="flex flex-col gap-3 sm:flex-row sm:items-start">
            <div className="flex-1">
              <Select
                label="Categoria"
                value={categoryId ? String(categoryId) : ''}
                onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
                onBlur={getBlurHandler('categoryId')}
                error={shouldShowError('categoryId', fieldErrors.categoryId)}
                required
                options={categoryOptions}
                placeholder="Selecione uma categoria"
              />
            </div>
            <div className="sm:pt-6">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setModalOpen(true)}
              >
                <Plus className="h-4 w-4" />
                Nova categoria
              </Button>
            </div>
          </div>
        )}
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

    {/* Modal fora do <form> — HTML não permite forms aninhados. */}
    <ServiceCategoryFormModal
      open={modalOpen}
      onClose={() => setModalOpen(false)}
      onSuccess={handleCategoryCreated}
    />
    </>
  )
}