import { useMemo, useState, type FormEvent } from 'react'
import { Save } from 'lucide-react'
import type {
  Address,
  RegistrationStatus,
  SupplierCreateRequest,
  SupplierResponse,
  SupplierUpdateRequest,
} from '../../types/supplier'
import { Input } from '../ui/Input'
import { Button } from '../ui/Button'
import { Alert } from '../ui/Alert'
import { AddressFields } from './AddressFields'
import { toApiError } from '../../lib/errors'
import { isValidCnpj, maskCnpj, maskPhone } from '../../lib/documents'
import { isValidUf } from '../../lib/brazilianStates'
import { useFieldTouched } from '../../hooks/useFieldTouched'

const EMPTY_ADDRESS: Address = {
  street: '',
  number: '',
  complement: '',
  neighborhood: '',
  city: '',
  state: '',
  zipCode: '',
}

interface SupplierFormProps {
  /** Fornecedor existente (modo edição). Quando omitido, é cadastro novo. */
  supplier?: SupplierResponse
  isLoading?: boolean
  onSaveCreate: (payload: SupplierCreateRequest) => Promise<void>
  onSaveUpdate: (payload: SupplierUpdateRequest) => Promise<void>
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

export function SupplierForm({
  supplier,
  isLoading = false,
  onSaveCreate,
  onSaveUpdate,
}: SupplierFormProps) {
  const isEdit = !!supplier
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

  // CNPJ imutável após o cadastro (identidade fiscal).
  const [taxId, setTaxId] = useState(supplier?.taxId ?? '')

  // Campos editáveis.
  const [legalName, setLegalName] = useState(supplier?.legalName ?? '')
  const [tradeName, setTradeName] = useState(supplier?.tradeName ?? '')
  const [stateRegistration, setStateRegistration] = useState(
    supplier?.stateRegistration ?? '',
  )
  const [municipalRegistration, setMunicipalRegistration] = useState(
    supplier?.municipalRegistration ?? '',
  )
  const [email, setEmail] = useState(supplier?.email ?? '')
  const [phone, setPhone] = useState(supplier?.phone ?? '')
  const [contactName, setContactName] = useState(supplier?.contactName ?? '')
  const [address, setAddress] = useState<Address>(
    supplier?.address ?? { ...EMPTY_ADDRESS },
  )
  const [status, setStatus] = useState<RegistrationStatus>(
    supplier?.status ?? 'ATIVO',
  )

  const addressErrors = useMemo(() => validateAddress(address), [address])

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!legalName.trim()) errs.legalName = 'Razão social é obrigatória.'

    if (!email.trim()) {
      errs.email = 'E-mail é obrigatório.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errs.email = 'E-mail inválido.'
    }

    if (phone.trim()) {
      const digits = phone.replace(/\D/g, '')
      if (digits.length < 10) errs.phone = 'Telefone incompleto.'
    }

    const taxDigits = taxId.replace(/\D/g, '')
    if (!taxId.trim()) {
      errs.taxId = 'CNPJ é obrigatório.'
    } else if (taxDigits.length !== 14) {
      errs.taxId = 'CNPJ deve conter 14 dígitos.'
    } else if (!isValidCnpj(taxId)) {
      errs.taxId = 'CNPJ inválido (dígitos verificadores).'
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
      if (isEdit && supplier) {
        const payload: SupplierUpdateRequest = {
          legalName: legalName.trim(),
          tradeName: tradeName.trim() || undefined,
          stateRegistration: stateRegistration.trim() || undefined,
          municipalRegistration:
            municipalRegistration.trim() || undefined,
          email: email.trim(),
          phone: phone.trim() || undefined,
          contactName: contactName.trim() || undefined,
          address: trimmedAddress,
          status,
        }
        await onSaveUpdate(payload)
        setSuccess('Fornecedor atualizado com sucesso!')
        reset()
      } else {
        const payload: SupplierCreateRequest = {
          legalName: legalName.trim(),
          tradeName: tradeName.trim() || undefined,
          taxId: taxId.replace(/\D/g, ''),
          stateRegistration: stateRegistration.trim() || undefined,
          municipalRegistration:
            municipalRegistration.trim() || undefined,
          email: email.trim(),
          phone: phone.trim() || undefined,
          contactName: contactName.trim() || undefined,
          address: trimmedAddress,
          status,
        }
        await onSaveCreate(payload)
        setSuccess('Fornecedor criado com sucesso!')
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
          Dados básicos do fornecedor. O CNPJ não pode ser alterado após o
          cadastro.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="CNPJ"
            value={taxId}
            onChange={(e) => setTaxId(maskCnpj(e.target.value))}
            onBlur={getBlurHandler('taxId')}
            error={shouldShowError('taxId', fieldErrors.taxId)}
            disabled={isEdit}
            required
            maxLength={18}
            hint={
              isEdit
                ? 'O CNPJ não pode ser alterado.'
                : 'Identificador fiscal do fornecedor.'
            }
          />

          <Input
            label="Razão social"
            value={legalName}
            onChange={(e) => setLegalName(e.target.value)}
            onBlur={getBlurHandler('legalName')}
            error={shouldShowError('legalName', fieldErrors.legalName)}
            required
            maxLength={200}
          />
          <Input
            label="Nome fantasia"
            value={tradeName}
            onChange={(e) => setTradeName(e.target.value)}
            onBlur={getBlurHandler('tradeName')}
            error={shouldShowError('tradeName', fieldErrors.tradeName)}
            maxLength={200}
          />
          <Input
            label="Inscrição Estadual"
            value={stateRegistration}
            onChange={(e) => setStateRegistration(e.target.value)}
            onBlur={getBlurHandler('stateRegistration')}
            error={shouldShowError(
              'stateRegistration',
              fieldErrors.stateRegistration,
            )}
            maxLength={30}
          />
          <Input
            label="Inscrição Municipal"
            value={municipalRegistration}
            onChange={(e) => setMunicipalRegistration(e.target.value)}
            onBlur={getBlurHandler('municipalRegistration')}
            error={shouldShowError(
              'municipalRegistration',
              fieldErrors.municipalRegistration,
            )}
            maxLength={30}
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
            maxLength={15}
          />
          <Input
            label="Pessoa de contato"
            value={contactName}
            onChange={(e) => setContactName(e.target.value)}
            onBlur={getBlurHandler('contactName')}
            error={shouldShowError('contactName', fieldErrors.contactName)}
            hint="Nome do responsável/vendedor no fornecedor."
            maxLength={150}
          />
        </div>
      </section>

      {/* Endereço */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Endereço</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Endereço fiscal/comercial do fornecedor. Em alterações, o endereço
          anterior é substituído.
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
          Define se o fornecedor pode receber novos pedidos. Fornecedores
          inativos continuam no cadastro para fins de histórico.
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
          {isEdit ? 'Salvar alterações' : 'Cadastrar fornecedor'}
        </Button>
      </div>
    </form>
  )
}