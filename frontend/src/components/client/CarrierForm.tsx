import { useState, type FormEvent } from 'react'
import type {
  CarrierCreateRequest,
  CarrierResponse,
  CarrierUpdateRequest,
  RegistrationStatus,
} from '../../types/carrier'
import { Input } from '../ui/Input'
import { Alert } from '../ui/Alert'
import { toApiError } from '../../lib/errors'
import { useFieldTouched } from '../../hooks/useFieldTouched'

interface CarrierFormProps {
  /** Transportadora existente (modo edição). Quando omitido, é cadastro novo. */
  carrier?: CarrierResponse
  onSaveCreate: (payload: CarrierCreateRequest) => Promise<void>
  onSaveUpdate: (payload: CarrierUpdateRequest) => Promise<void>
}

export function CarrierForm({
  carrier,
  onSaveCreate,
  onSaveUpdate,
}: CarrierFormProps) {
  const isEdit = !!carrier
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  // Erros só são exibidos após o usuário tocar no campo ou tentar submeter.
  const {
    shouldShowError,
    getBlurHandler,
    markAllTouched,
    reset,
  } = useFieldTouched()

  // Campos editáveis.
  const [name, setName] = useState(carrier?.name ?? '')
  const [status, setStatus] = useState<RegistrationStatus>(
    carrier?.status ?? 'ATIVO',
  )

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!name.trim()) errs.name = 'Nome é obrigatório.'

    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
    // Revela os erros de todos os campos no submit, mesmo os ainda não tocados.
    markAllTouched()
    if (!validateAll()) return

    try {
      if (isEdit && carrier) {
        const payload: CarrierUpdateRequest = {
          name: name.trim(),
          status,
        }
        await onSaveUpdate(payload)
        setSuccess('Transportadora atualizada com sucesso!')
        reset()
      } else {
        const payload: CarrierCreateRequest = {
          name: name.trim(),
          status,
        }
        await onSaveCreate(payload)
        setSuccess('Transportadora criada com sucesso!')
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
      id="carrier-form"
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
          Dados básicos da transportadora.
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
            hint="Nome da transportadora (ex: TRANSPORTADORA XPTO LTDA)."
          />
        </div>
      </section>

      {/* Status */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Status</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Define se a transportadora pode ser utilizada em novos pedidos.
          Transportadoras inativas permanecem no cadastro para fins de
          histórico.
        </p>
        <div className="flex gap-2">
          {(['ATIVO', 'INATIVO'] as RegistrationStatus[]).map((s) => (
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
      </section>
    </form>
  )
}