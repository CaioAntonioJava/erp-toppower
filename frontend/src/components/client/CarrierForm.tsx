import { useState, type FormEvent } from 'react'
import type {
  CarrierCreateRequest,
  CarrierName,
  CarrierResponse,
  CarrierStatus,
  CarrierUpdateRequest,
} from '../../types/carrier'
import {
  CARRIER_NAME_LABELS,
} from '../../types/carrier'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Alert } from '../ui/Alert'
import { toApiError } from '../../lib/errors'
import { formatBRLValue, parseNumber } from '../../lib/money'
import { useFieldTouched } from '../../hooks/useFieldTouched'

interface CarrierFormProps {
  /** Transportadora existente (modo edição). Quando omitido, é cadastro novo. */
  carrier?: CarrierResponse
  onSaveCreate: (payload: CarrierCreateRequest) => Promise<void>
  onSaveUpdate: (payload: CarrierUpdateRequest) => Promise<void>
}

// Opções do enum CarrierName para o <Select>.
const CARRIER_NAME_OPTIONS = (
  Object.keys(CARRIER_NAME_LABELS) as CarrierName[]
).map((name) => ({ value: name, label: CARRIER_NAME_LABELS[name] }))

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

  // carrierName é mutável (não é identidade imutável como CNPJ).
  const [carrierName, setCarrierName] = useState<string>(
    carrier?.carrierName ?? '',
  )
  const [freightValue, setFreightValue] = useState<string>(
    carrier?.freightValue != null ? formatBRLValue(carrier.freightValue) : '',
  )
  const [status, setStatus] = useState<CarrierStatus>(
    carrier?.status ?? 'ATIVO',
  )

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!carrierName) {
      errs.carrierName = 'Transportadora é obrigatória.'
    }

    const freightNum = parseNumber(freightValue)
    if (freightValue.trim() !== '' && freightNum === null) {
      errs.freightValue = 'Valor de frete inválido.'
    } else if (freightNum !== null && freightNum < 0) {
      errs.freightValue = 'Valor de frete não pode ser negativo.'
    }

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

    const freightNum = parseNumber(freightValue)

    try {
      const namePayload = carrierName
        ? { carrierName: carrierName as CarrierName }
        : {}

      if (isEdit && carrier) {
        const payload: CarrierUpdateRequest = {
          ...namePayload,
          freightValue: freightNum ?? null,
          status,
        }
        await onSaveUpdate(payload)
        setSuccess('Transportadora atualizada com sucesso!')
        reset()
      } else {
        const payload: CarrierCreateRequest = {
          ...namePayload,
          freightValue: freightNum ?? null,
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
          Nome da transportadora e valor padrão do frete cobrado.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <Select
            label="Transportadora"
            value={carrierName}
            onChange={(e) => setCarrierName(e.target.value)}
            onBlur={getBlurHandler('carrierName')}
            error={shouldShowError('carrierName', fieldErrors.carrierName)}
            required
            placeholder="Selecione…"
            options={CARRIER_NAME_OPTIONS}
            aria-label="Transportadora"
          />
          <Input
            label="Valor do frete (R$)"
            type="text"
            inputMode="decimal"
            placeholder="0,00"
            value={freightValue}
            onChange={(e) => setFreightValue(e.target.value)}
            onBlur={() => {
              // Normaliza para 2 casas decimais no formato brasileiro
              // (vírgula). Se o usuário digitar "45" vira "45,00"; se
              // digitar "45,9" vira "45,90". Campos vazios são mantidos.
              if (freightValue.trim() !== '') {
                const formatted = formatBRLValue(freightValue)
                if (formatted) setFreightValue(formatted)
              }
              getBlurHandler('freightValue')()
            }}
            error={shouldShowError(
              'freightValue',
              fieldErrors.freightValue,
            )}
            hint="Valor padrão do frete (opcional). Use vírgula para os centavos."
          />
        </div>
      </section>

      {/* Status */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Status</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Define se a transportadora pode ser selecionada em cotações e
          pedidos. Transportadoras inativas continuam no cadastro para
          fins de histórico.
        </p>
        <div className="flex gap-2">
          {(['ATIVO', 'INATIVO'] as CarrierStatus[]).map((s) => (
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