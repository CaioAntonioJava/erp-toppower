import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Save } from 'lucide-react'
import type {
  Address,
  CustomerCreateRequest,
  CustomerResponse,
  CustomerUpdateRequest,
  RegistrationStatus,
} from '../../types/customer'
import { Input } from '../ui/Input'
import { Button } from '../ui/Button'
import { Alert } from '../ui/Alert'
import { AddressFields } from './AddressFields'
import { toApiError } from '../../lib/errors'
import { isValidCpf, maskCpf, maskPhone } from '../../lib/documents'
import { isValidUf } from '../../lib/brazilianStates'
import { useFieldTouched } from '../../hooks/useFieldTouched'
import { getNextCustomerCode } from '../../api/customer.api'

const EMPTY_ADDRESS: Address = {
  street: '',
  number: '',
  complement: '',
  neighborhood: '',
  city: '',
  state: '',
  zipCode: '',
}

interface CustomerFormProps {
  /** Cliente existente (modo edição). Quando omitido, é cadastro novo. */
  customer?: CustomerResponse
  isLoading?: boolean
  onSaveCreate: (payload: CustomerCreateRequest) => Promise<void>
  onSaveUpdate: (payload: CustomerUpdateRequest) => Promise<void>
}

function validateAddress(a: Address): Partial<Record<keyof Address, string>> {
  const errs: Partial<Record<keyof Address, string>> = {}
  if (!a.street.trim()) errs.street = 'Logradouro é obrigatório.'
  if (!a.number.trim()) errs.number = 'Número é obrigatório.'
  if (!a.city.trim()) errs.city = 'Cidade é obrigatória.'
  if (!a.state.trim()) {
    errs.state = 'UF é obrigatória.'
  } else if (!isValidUf(a.state)) {
    errs.state = 'UF inválida.'
  }
  if (!a.zipCode.trim()) {
    errs.zipCode = 'CEP é obrigatório.'
  } else if (a.zipCode.replace(/\D/g, '').length !== 8) {
    errs.zipCode = 'CEP deve conter 8 dígitos.'
  }
  return errs
}

export function CustomerForm({
  customer,
  isLoading = false,
  onSaveCreate,
  onSaveUpdate,
}: CustomerFormProps) {
  const isEdit = !!customer
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  // Erros só são exibidos após o usuário tocar no campo ou tentar submeter.
  const {
    shouldShowError,
    getBlurHandler,
    markAllTouched,
    reset,
    submitAttempted,
  } = useFieldTouched()

  // Campos imutáveis após o cadastro.
  // `code` é gerado pelo servidor; em cadastro novo pré-baixamos o próximo
  // código disponível para exibir no campo desabilitado, e em edição
  // mostramos o código já atribuído.
  const [code, setCode] = useState(customer?.code ?? '')
  const [cpf, setCpf] = useState(customer?.cpf ?? '')

  // Pré-busca o próximo código ao entrar no modo de cadastro. Em modo de
  // edição o código já veio no `customer`; não precisa buscar de novo.
  useEffect(() => {
    if (customer) return
    let cancelled = false
    getNextCustomerCode()
      .then((next) => {
        if (!cancelled) setCode(next)
      })
      .catch(() => {
        // Silencioso: se a chamada falhar, o campo fica vazio até o backend
        // atribuir o código real no momento do POST.
      })
    return () => {
      cancelled = true
    }
  }, [customer])

  // Campos editáveis.
  const [name, setName] = useState(customer?.name ?? '')
  const [email, setEmail] = useState(customer?.email ?? '')
  const [phone, setPhone] = useState(customer?.phone ?? '')
  const [address, setAddress] = useState<Address>(
    customer?.address ?? { ...EMPTY_ADDRESS },
  )
  const [status, setStatus] = useState<RegistrationStatus>(
    customer?.status ?? 'ATIVO',
  )

  const addressErrors = useMemo(() => validateAddress(address), [address])

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

    setFieldErrors(errs)
    return Object.keys(errs).length === 0 && Object.keys(addressErrors).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
    // Revela os erros de todos os campos no submit, mesmo os ainda não tocados.
    markAllTouched()
    if (!validateAll()) return

    const trimmedAddress: Address = {
      street: address.street.trim(),
      number: address.number.trim(),
      city: address.city.trim(),
      state: address.state.trim().toUpperCase(),
      zipCode: address.zipCode.trim(),
      complement: address.complement?.trim() || undefined,
      neighborhood: address.neighborhood?.trim() || undefined,
    }

    try {
      if (isEdit && customer) {
        const payload: CustomerUpdateRequest = {
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          address: trimmedAddress,
          status,
        }
        await onSaveUpdate(payload)
        setSuccess('Cliente atualizado com sucesso!')
        reset()
      } else {
        const payload: CustomerCreateRequest = {
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          cpf: cpf.replace(/\D/g, ''),
          address: trimmedAddress,
          status,
        }
        await onSaveCreate(payload)
        setSuccess('Cliente criado com sucesso!')
        reset()
      }
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) {
        const { address: _address, ...rest } = apiErr.fieldErrors
        setFieldErrors(rest)
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6" noValidate>
      {formError ? <Alert variant="error">{formError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}

      {/* Identificação */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Identificação</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Dados básicos do cliente. O código é gerado automaticamente e o CPF
          não pode ser alterado após o cadastro.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Código"
            value={code}
            disabled
            readOnly
          />

          <Input
            label="CPF"
            value={cpf}
            onChange={(e) => setCpf(maskCpf(e.target.value))}
            onBlur={getBlurHandler('cpf')}
            error={shouldShowError('cpf', fieldErrors.cpf)}
            disabled={isEdit}
            required
            maxLength={14}
          />

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
        </div>
      </section>

      {/* Endereço */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Endereço</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Endereço do cliente. Em alterações, o endereço anterior é substituído.
        </p>
        <AddressFields
          value={address}
          onChange={setAddress}
          errors={addressErrors}
          forceShowErrors={submitAttempted}
        />
      </section>

      {/* Status */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Status</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Define se o cliente pode receber novos pedidos. Clientes inativos
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

      <div className="flex justify-end">
        <Button type="submit" isLoading={isLoading} size="lg">
          <Save className="h-4 w-4" />
          {isEdit ? 'Salvar alterações' : 'Cadastrar cliente'}
        </Button>
      </div>
    </form>
  )
}
