import { useEffect, useMemo, useState, type FormEvent } from 'react'
import type {
  Address,
  CompanyCreateRequest,
  CompanyResponse,
  CompanyUpdateRequest,
  RegistrationStatus,
} from '../../types/company'
import { Input } from '../ui/Input'
import { Alert } from '../ui/Alert'
import { AddressFields } from './AddressFields'
import { toApiError } from '../../lib/errors'
import { isValidCnpj, maskCnpj } from '../../lib/documents'
import { isValidUf } from '../../lib/brazilianStates'
import { useFieldTouched } from '../../hooks/useFieldTouched'
import { getNextCompanyCode } from '../../api/company.api'

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
  onSaveCreate,
  onSaveUpdate,
}: CompanyFormProps) {
  const isEdit = !!company
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
  const [code, setCode] = useState(company?.code ?? '')
  const [cnpj, setCnpj] = useState(company?.cnpj ?? '')

  // Pré-busca o próximo código ao entrar no modo de cadastro. Em modo de
  // edição o código já veio no `company`; não precisa buscar de novo.
  useEffect(() => {
    if (company) return
    let cancelled = false
    getNextCompanyCode()
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
  }, [company])

  // Campos editáveis.
  const [legalName, setLegalName] = useState(company?.legalName ?? '')
  const [tradeName, setTradeName] = useState(company?.tradeName ?? '')
  const [stateRegistration, setStateRegistration] = useState(
    company?.stateRegistration ?? '',
  )
  /**
   * Flag de isenção de Inscrição Estadual. Quando marcada, a empresa
   * é dispensada de possuir IE (MEIs, prestadores de serviço). O campo
   * "Inscrição Estadual" fica desabilitado e é limpo no envio.
   */
  const [stateRegistrationExempt, setStateRegistrationExempt] = useState(
    company?.stateRegistrationExempt ?? false,
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
      if (isEdit && company) {
        const payload: CompanyUpdateRequest = {
          legalName: legalName.trim(),
          tradeName: tradeName.trim() || undefined,
          // Quando isenta, não envia a IE (ou envia como nulo) para
          // evitar inconsistência com a flag.
          stateRegistration: stateRegistrationExempt
            ? undefined
            : stateRegistration.trim() || undefined,
          stateRegistrationExempt,
          municipalRegistration:
            municipalRegistration.trim() || undefined,
          address: trimmedAddress,
          status,
        }
        await onSaveUpdate(payload)
        setSuccess('Empresa atualizada com sucesso!')
        reset()
      } else {
        const payload: CompanyCreateRequest = {
          legalName: legalName.trim(),
          tradeName: tradeName.trim() || undefined,
          cnpj: cnpj.replace(/\D/g, ''),
          stateRegistration: stateRegistrationExempt
            ? undefined
            : stateRegistration.trim() || undefined,
          stateRegistrationExempt,
          municipalRegistration:
            municipalRegistration.trim() || undefined,
          address: trimmedAddress,
          status,
        }
        await onSaveCreate(payload)
        setSuccess('Empresa criada com sucesso!')
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
    <form
      id="company-form"
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
          Dados básicos da empresa. O código é gerado automaticamente e o CNPJ
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
            label="CNPJ"
            value={cnpj}
            onChange={(e) => setCnpj(maskCnpj(e.target.value))}
            onBlur={getBlurHandler('cnpj')}
            error={shouldShowError('cnpj', fieldErrors.cnpj)}
            disabled={isEdit}
            required
            maxLength={18}
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
          <div className="flex flex-col">
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
              disabled={stateRegistrationExempt}
              hint={
                stateRegistrationExempt
                  ? 'IE não se aplica — empresa isenta.'
                  : undefined
              }
            />
            <label className="mt-2 inline-flex cursor-pointer items-center gap-2 text-sm text-slate-700 dark:text-slate-200">
              <input
                type="checkbox"
                checked={stateRegistrationExempt}
                onChange={(e) => {
                  const next = e.target.checked
                  setStateRegistrationExempt(next)
                  // Ao marcar como isenta, limpa o campo IE para evitar
                  // inconsistência. Ao desmarcar, mantém o estado atual
                  // (o usuário pode re-digitar).
                  if (next) setStateRegistration('')
                }}
                className="h-4 w-4 cursor-pointer rounded border-slate-300 text-primary focus:ring-primary dark:border-slate-600 dark:bg-slate-800"
              />
              <span>IE Isento (empresa dispensada de Inscrição Estadual)</span>
            </label>
          </div>
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
          forceShowErrors={submitAttempted}
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
    </form>
  )
}
