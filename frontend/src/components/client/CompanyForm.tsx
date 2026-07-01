import { useMemo, useState, type FormEvent } from 'react'
import { Save } from 'lucide-react'
import type {
  Address,
  CompanyCreateRequest,
  CompanyResponse,
  CompanyUpdateRequest,
  RegistrationStatus,
} from '../../types/company'
import { Input } from '../ui/Input'
import { Button } from '../ui/Button'
import { Alert } from '../ui/Alert'
import { AddressFields } from './AddressFields'
import { toApiError } from '../../lib/errors'
import { isValidCnpj, maskCnpj } from '../../lib/documents'
import { isValidUf } from '../../lib/brazilianStates'

const EMPTY_ADDRESS: Address = {
  street: '',
  number: '',
  complement: '',
  neighborhood: '',
  city: '',
  state: '',
  zipCode: '',
}

interface CompanyFormProps {
  /** Empresa existente (modo edição). Quando omitido, é cadastro novo. */
  company?: CompanyResponse
  isLoading?: boolean
  onSaveCreate: (payload: CompanyCreateRequest) => Promise<void>
  onSaveUpdate: (payload: CompanyUpdateRequest) => Promise<void>
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

export function CompanyForm({
  company,
  isLoading = false,
  onSaveCreate,
  onSaveUpdate,
}: CompanyFormProps) {
  const isEdit = !!company
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  // Campos imutáveis após o cadastro.
  const [code, setCode] = useState(company?.code ?? '')
  const [cnpj, setCnpj] = useState(company?.cnpj ?? '')

  // Campos editáveis.
  const [legalName, setLegalName] = useState(company?.legalName ?? '')
  const [tradeName, setTradeName] = useState(company?.tradeName ?? '')
  const [stateRegistration, setStateRegistration] = useState(
    company?.stateRegistration ?? '',
  )
  const [municipalRegistration, setMunicipalRegistration] = useState(
    company?.municipalRegistration ?? '',
  )
  const [address, setAddress] = useState<Address>(
    company?.address ?? { ...EMPTY_ADDRESS },
  )
  const [status, setStatus] = useState<RegistrationStatus>(
    company?.status ?? 'ATIVO',
  )

  const addressErrors = useMemo(() => validateAddress(address), [address])

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!isEdit) {
      if (!code.trim()) errs.code = 'Código é obrigatório.'
    }
    if (!legalName.trim()) errs.legalName = 'Razão social é obrigatória.'

    const digits = cnpj.replace(/\D/g, '')
    if (!cnpj.trim()) {
      errs.cnpj = 'CNPJ é obrigatório.'
    } else if (digits.length !== 14) {
      errs.cnpj = 'CNPJ deve conter 14 dígitos.'
    } else if (!isValidCnpj(cnpj)) {
      errs.cnpj = 'CNPJ inválido (dígitos verificadores).'
    }

    setFieldErrors(errs)
    return Object.keys(errs).length === 0 && Object.keys(addressErrors).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
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
      if (isEdit && company) {
        const payload: CompanyUpdateRequest = {
          legalName: legalName.trim(),
          tradeName: tradeName.trim() || undefined,
          stateRegistration: stateRegistration.trim() || undefined,
          municipalRegistration:
            municipalRegistration.trim() || undefined,
          address: trimmedAddress,
          status,
        }
        await onSaveUpdate(payload)
        setSuccess('Empresa atualizada com sucesso!')
      } else {
        const payload: CompanyCreateRequest = {
          code: code.trim(),
          legalName: legalName.trim(),
          tradeName: tradeName.trim() || undefined,
          cnpj: cnpj.replace(/\D/g, ''),
          stateRegistration: stateRegistration.trim() || undefined,
          municipalRegistration:
            municipalRegistration.trim() || undefined,
          address: trimmedAddress,
          status,
        }
        await onSaveCreate(payload)
        setSuccess('Empresa criada com sucesso!')
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
          Dados básicos da empresa. Código e CNPJ não podem ser alterados após o
          cadastro.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Código"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            error={fieldErrors.code}
            disabled={isEdit}
            required={!isEdit}
            hint={
              isEdit
                ? 'O código não pode ser alterado.'
                : 'Identificador único (ex: EMP-001).'
            }
          />

          <Input
            label="CNPJ"
            value={cnpj}
            onChange={(e) => setCnpj(maskCnpj(e.target.value))}
            error={fieldErrors.cnpj}
            disabled={isEdit}
            required
            maxLength={18}
          />

          <Input
            label="Razão social"
            value={legalName}
            onChange={(e) => setLegalName(e.target.value)}
            error={fieldErrors.legalName}
            required
            maxLength={200}
          />
          <Input
            label="Nome fantasia"
            value={tradeName}
            onChange={(e) => setTradeName(e.target.value)}
            error={fieldErrors.tradeName}
            maxLength={200}
          />
          <Input
            label="Inscrição Estadual"
            value={stateRegistration}
            onChange={(e) => setStateRegistration(e.target.value)}
            error={fieldErrors.stateRegistration}
            maxLength={30}
          />
          <Input
            label="Inscrição Municipal"
            value={municipalRegistration}
            onChange={(e) => setMunicipalRegistration(e.target.value)}
            error={fieldErrors.municipalRegistration}
            maxLength={30}
          />
        </div>
      </section>

      {/* Endereço */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Endereço</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Endereço fiscal da empresa. Em alterações, o endereço anterior é
          substituído.
        </p>
        <AddressFields
          value={address}
          onChange={setAddress}
          errors={addressErrors}
        />
      </section>

      {/* Status */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Status</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Define se a empresa pode receber novos pedidos. Empresas inativas
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
          {isEdit ? 'Salvar alterações' : 'Cadastrar empresa'}
        </Button>
      </div>
    </form>
  )
}
