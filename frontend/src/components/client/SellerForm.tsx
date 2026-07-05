import { useState, type FormEvent } from 'react'
import type {
  RegistrationStatus,
  SellerCreateRequest,
  SellerResponse,
  SellerUpdateRequest,
} from '../../types/seller'
import { Input } from '../ui/Input'
import { Alert } from '../ui/Alert'
import { toApiError } from '../../lib/errors'
import { isValidCpf, maskCpf, maskPhone } from '../../lib/documents'
import { useFieldTouched } from '../../hooks/useFieldTouched'

interface SellerFormProps {
  /** Vendedor existente (modo edição). Quando omitido, é cadastro novo. */
  seller?: SellerResponse
  onSaveCreate: (payload: SellerCreateRequest) => Promise<void>
  onSaveUpdate: (payload: SellerUpdateRequest) => Promise<void>
}

/** Converte string da UI (ex: "5,5" ou "10") em número ou null. */
function parseCommission(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  // Aceita vírgula ou ponto como separador decimal.
  const normalized = trimmed.replace(',', '.')
  const n = Number(normalized)
  return Number.isFinite(n) ? n : null
}

export function SellerForm({
  seller,
  onSaveCreate,
  onSaveUpdate,
}: SellerFormProps) {
  const isEdit = !!seller
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

  // Campos.
  const [name, setName] = useState(seller?.name ?? '')
  const [email, setEmail] = useState(seller?.email ?? '')
  const [phone, setPhone] = useState(seller?.phone ?? '')
  const [cpf, setCpf] = useState(seller?.cpf ?? '')
  const [commissionRate, setCommissionRate] = useState<string>(
    seller?.commissionRate != null ? String(seller.commissionRate) : '',
  )
  const [status, setStatus] = useState<RegistrationStatus>(
    seller?.status ?? 'ATIVO',
  )

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!name.trim()) errs.name = 'Nome é obrigatório.'

    if (!email.trim()) {
      errs.email = 'E-mail é obrigatório.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errs.email = 'E-mail inválido.'
    }

    if (!phone.trim()) {
      errs.phone = 'Telefone é obrigatório.'
    } else if (phone.replace(/\D/g, '').length < 10) {
      errs.phone = 'Telefone incompleto.'
    }

    const cpfDigits = cpf.replace(/\D/g, '')
    if (!cpf.trim()) {
      errs.cpf = 'CPF é obrigatório.'
    } else if (cpfDigits.length !== 11) {
      errs.cpf = 'CPF deve conter 11 dígitos.'
    } else if (!isValidCpf(cpf)) {
      errs.cpf = 'CPF inválido (dígitos verificadores).'
    }

    if (commissionRate.trim()) {
      const n = parseCommission(commissionRate)
      if (n === null) {
        errs.commissionRate = 'Comissão inválida.'
      } else if (n < 0 || n > 100) {
        errs.commissionRate = 'Comissão deve estar entre 0% e 100%.'
      }
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

    const commission = parseCommission(commissionRate)

    try {
      if (isEdit && seller) {
        const payload: SellerUpdateRequest = {
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          cpf: cpf.replace(/\D/g, ''),
          commissionRate: commission,
          status,
        }
        await onSaveUpdate(payload)
        setSuccess('Vendedor atualizado com sucesso!')
        reset()
      } else {
        const payload: SellerCreateRequest = {
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          cpf: cpf.replace(/\D/g, ''),
          commissionRate: commission,
          status,
        }
        await onSaveCreate(payload)
        setSuccess('Vendedor criado com sucesso!')
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
      id="seller-form"
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
          Dados pessoais e percentual de comissão do vendedor.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Nome completo"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onBlur={getBlurHandler('name')}
            error={shouldShowError('name', fieldErrors.name)}
            required
            maxLength={150}
          />
          <Input
            label="CPF"
            value={cpf}
            onChange={(e) => setCpf(maskCpf(e.target.value))}
            onBlur={getBlurHandler('cpf')}
            error={shouldShowError('cpf', fieldErrors.cpf)}
            required
            maxLength={14}
          />
          <Input
            label="E-mail"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onBlur={getBlurHandler('email')}
            error={shouldShowError('email', fieldErrors.email)}
            required
          />
          <Input
            label="Telefone"
            value={phone}
            onChange={(e) => setPhone(maskPhone(e.target.value))}
            onBlur={getBlurHandler('phone')}
            error={shouldShowError('phone', fieldErrors.phone)}
            required
          />
          <Input
            label="Comissão (%)"
            type="number"
            inputMode="decimal"
            step="0.01"
            min={0}
            max={100}
            value={commissionRate}
            onChange={(e) => setCommissionRate(e.target.value)}
            onBlur={getBlurHandler('commissionRate')}
            error={shouldShowError(
              'commissionRate',
              fieldErrors.commissionRate,
            )}
            hint="Percentual de 0% a 100%. Deixe em branco para 0%."
            className="sm:col-span-2"
          />
        </div>
      </section>

      {/* Status */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Status</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Define se o vendedor pode receber novos pedidos. Vendedores inativos
          continuam no cadastro para fins de histórico.
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