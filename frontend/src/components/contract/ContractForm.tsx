import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from 'react'
import { Loader2, MapPin } from 'lucide-react'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Alert } from '../ui/Alert'
import { Spinner } from '../ui/Spinner'
import { RichTextEditor } from '../ui/RichTextEditor'
import { toApiError, errorMessage } from '../../lib/errors'
import { useFieldTouched } from '../../hooks/useFieldTouched'
import { getCep } from '../../api/cep.api'
import { getCustomer } from '../../api/customer.api'
import { getCompany } from '../../api/company.api'
import { searchContractsClients } from '../../api/contract.api'
import { BRAZILIAN_STATES } from '../../lib/brazilianStates'
import { maskZipCode } from '../../lib/documents'
import { useOrganization } from '../../context/OrganizationContext'
import type { ClientSummaryResponse } from '../../types/quotation'
import type {
  ContractAddressRequest,
  ContractClientType,
  ContractCreateRequest,
  ContractResponse,
  ContractUpdateRequest,
} from '../../types/contract'

interface ContractFormProps {
  /** Contrato existente (modo edição). Quando omitido, é cadastro novo. */
  contract?: ContractResponse
  /**
   * Próximo código previsto pelo backend (modo create), no formato
   * `<prefix>-<seq>-<year>` (ex.: `CT-001-2026` ou `CL-001-2026`).
   *
   * <p>O código é gerado integralmente pelo backend a partir da
   * Organization ativa ({@code OrganizationContext.contractPrefix} +
   * {@code MAX(sequence) WHERE year = currentYear AND organization_uuid = :org}).
   * <b>Não pode ser alterado pelo usuário</b> — quando o backend
   * devolver um novo valor (ex.: troca de empresa ativa no Topbar),
   * o input reflete automaticamente.</p>
   */
  initialCode?: string | null
  onSaveCreate: (payload: ContractCreateRequest) => Promise<void>
  onSaveUpdate: (payload: ContractUpdateRequest) => Promise<void>
}

type ClientSearchResult = {
  uuid: string
  name: string
  code: string
  document: string
  type: ContractClientType
}

const UF_OPTIONS = BRAZILIAN_STATES.map((s) => ({
  value: s.uf,
  label: `${s.uf} — ${s.name}`,
}))

const CLIENT_TYPE_OPTIONS = [
  { value: 'CUSTOMER', label: 'Cliente (PF)' },
  { value: 'COMPANY', label: 'Empresa (PJ)' },
]

function todayIso(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function toSearchResult(c: ClientSummaryResponse): ClientSearchResult {
  return {
    uuid: c.uuid,
    name: c.name,
    code: c.code,
    document: c.document,
    type: c.type,
  }
}

export function ContractForm({
  contract,
  initialCode = null,
  onSaveCreate,
  onSaveUpdate,
}: ContractFormProps) {
  const isEdit = !!contract
  const { activeOrganizationRich } = useOrganization()

  // === cabeçalho ===
  /**
   * Código do contrato. Em modo create vem integralmente do backend
   * via {@link ContractFormProps.initialCode} (endpoint
   * {@code GET /api/v1/contracts/next-code}). Em modo edit, vem do
   * próprio contrato carregado. <b>Não pode ser editado pelo usuário.</b>
   *
   * <p>Sincronizado por {@code useEffect}: quando o backend devolve um
   * novo {@code initialCode} (ex.: troca de empresa ativa no Topbar),
   * o input reflete o novo valor sem precisar de remount. Como o
   * {@code ContractFormPage} já força remount via {@code key} baseado
   * em {@code activeOrganizationUuid}, isto é uma garantia adicional
   * (defense in depth) caso a page seja refatorada no futuro.</p>
   */
  const [code, setCode] = useState<string>(
    initialCode ?? contract?.code ?? '',
  )
  useEffect(() => {
    // Em modo edit, o código nunca muda (imutável por contrato de negócio).
    if (isEdit) return
    if (initialCode) {
      setCode(initialCode)
    }
  }, [initialCode, isEdit])

  // === Cliente (PF ou PJ) ===
  const initialClientType: ContractClientType = contract?.clientType ?? 'CUSTOMER'
  const [clientType, setClientType] = useState<ContractClientType>(initialClientType)
  const [clientUuid, setClientUuid] = useState<string>(
    contract?.clientUuid ?? '',
  )
  const [clientLabel, setClientLabel] = useState<string>(
    contract?.clientCode && contract.clientName
      ? `${contract.clientCode} — ${contract.clientName}`
      : contract?.clientName ?? '',
  )
  const [clientOptions, setClientOptions] = useState<ClientSearchResult[]>([])
  const [clientSearching, setClientSearching] = useState(false)
  const clientTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // === Endereço ===
  const [hasAddress, setHasAddress] = useState<boolean>(
    !!contract?.address,
  )
  const [address, setAddress] = useState<ContractAddressRequest>({
    street: contract?.address?.street ?? '',
    number: contract?.address?.number ?? '',
    complement: contract?.address?.complement ?? '',
    neighborhood: contract?.address?.neighborhood ?? '',
    city: contract?.address?.city ?? '',
    state: contract?.address?.state ?? '',
    zipCode: contract?.address?.zipCode ?? '',
  })
  const [cepLoading, setCepLoading] = useState(false)
  const [cepError, setCepError] = useState<string | null>(null)

  // === Blocos de texto ===
  const [description, setDescription] = useState<string>(
    contract?.description ?? '',
  )

  // Pré-preenche a descrição com o texto padrão da Organization ativa
  // quando é um cadastro novo e a descrição ainda está vazia.
  const hasPrefilledRef = useRef(false)
  useEffect(() => {
    if (isEdit) return
    if (hasPrefilledRef.current) return
    const defaultDesc = activeOrganizationRich?.contractDefaultDescription
    if (defaultDesc && !description) {
      hasPrefilledRef.current = true
      setDescription(defaultDesc)
    }
  }, [isEdit, activeOrganizationRich, description])
  const [clause, setClause] = useState<string>(contract?.clause ?? '')
  const [servicesDescription, setServicesDescription] = useState<string>(
    contract?.servicesDescription ?? '',
  )
  const [productsDescription, setProductsDescription] = useState<string>(
    contract?.productsDescription ?? '',
  )

  // === Título do contrato ===
  const [contractTitle, setContractTitle] = useState<string>(
    `Contrato de Prestação de Serviço ${initialCode ?? contract?.code ?? ''}`,
  )
  useEffect(() => {
    if (isEdit) return
    if (initialCode) {
      setContractTitle(`Contrato de Prestação de Serviço ${initialCode}`)
    }
  }, [initialCode, isEdit])

  // === Data de início ===
  const [startDate, setStartDate] = useState<string>(
    contract?.startDate ?? todayIso(),
  )

  // === Submit ===
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const { shouldShowError, getBlurHandler, markAllTouched } = useFieldTouched()

  // === handlers ===

  const handleClientQuery = useCallback((query: string) => {
    if (clientTimerRef.current) {
      clearTimeout(clientTimerRef.current)
      clientTimerRef.current = null
    }
    if (query.trim().length < 2) {
      setClientOptions([])
      setClientSearching(false)
      return
    }
    setClientSearching(true)
    clientTimerRef.current = setTimeout(async () => {
      try {
        const results = await searchContractsClients(query.trim(), 10)
        setClientOptions(results.map(toSearchResult))
      } catch {
        setClientOptions([])
      } finally {
        setClientSearching(false)
      }
    }, 300)
  }, [])

  // Quando o tipo de cliente muda, limpa a seleção atual.
  function handleClientTypeChange(newType: ContractClientType) {
    setClientType(newType)
    setClientUuid('')
    setClientLabel('')
    setClientOptions([])
  }

  // Auto-preencher endereço a partir do cliente selecionado
  // (somente quando o form está vazio — não sobrescreve edição manual).
  const lastAutoFilledUuidRef = useRef<string | null>(null)
  useEffect(() => {
    if (!clientUuid) {
      lastAutoFilledUuidRef.current = null
      return
    }
    // Já auto-preenchemos para este cliente nesta sessão? Sai.
    if (lastAutoFilledUuidRef.current === clientUuid) return
    // Só auto-preenche quando o form está vazio e não é modo edit
    // (em edit o usuário já viu/tem o endereço que está persistido).
    if (!isEdit && hasAddress) {
      const isEmpty = !address.street && !address.city && !address.zipCode
      if (isEmpty) {
        lastAutoFilledUuidRef.current = clientUuid
        const fetcher =
          clientType === 'CUSTOMER' ? getCustomer : getCompany
        fetcher(clientUuid)
          .then((c) => {
            setAddress({
              street: c.address.street ?? '',
              number: c.address.number ?? '',
              complement: c.address.complement ?? '',
              neighborhood: c.address.neighborhood ?? '',
              city: c.address.city ?? '',
              state: c.address.state ?? '',
              zipCode: c.address.zipCode ?? '',
            })
          })
          .catch(() => {/* silencioso */})
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientUuid, clientType])

  // === Preenchimento automático da cláusula com dados do cliente ===
  // Removido: a feature de placeholders/substituição automática foi
  // descontinuada. O campo de cláusula agora é apenas um campo de texto
  // livre que o usuário preenche manualmente (ou cola de um template
  // externo). O template da Organization também foi removido.

  const handleCepBlur = useCallback(async () => {
    setCepError(null)
    const digits = (address.zipCode ?? '').replace(/\D/g, '')
    if (digits.length !== 8) return
    setCepLoading(true)
    try {
      const result = await getCep(digits)
      setAddress((prev) => ({
        ...prev,
        street: result.street ?? prev.street,
        neighborhood: result.neighborhood ?? prev.neighborhood,
        city: result.city ?? prev.city,
        state: result.state ?? prev.state,
        // number, complement e zipCode: nunca sobrescrever.
      }))
    } catch (err) {
      setCepError(errorMessage(err))
    } finally {
      setCepLoading(false)
    }
  }, [address.zipCode])

  function validate(): Record<string, string> {
    const errs: Record<string, string> = {}
    if (!clientUuid) errs.clientUuid = 'Selecione um cliente ou empresa.'
    if (!description.trim()) errs.description = 'Descrição do contrato é obrigatória.'
    if (description.length > 4000) errs.description = 'Descrição deve ter no máximo 4000 caracteres.'
    if (!clause.trim()) errs.clause = 'Cláusula é obrigatória.'
    if (clause.length > 4000) errs.clause = 'Cláusula deve ter no máximo 4000 caracteres.'
    if (hasAddress && address.zipCode) {
      const digits = address.zipCode.replace(/\D/g, '')
      if (digits.length !== 8) {
        errs['address.zipCode'] = 'CEP deve conter 8 dígitos.'
      }
    }
    return errs
  }

  function buildAddressPayload(): ContractAddressRequest | null {
    if (!hasAddress) return null
    return {
      street: address.street?.trim() || null,
      number: address.number?.trim() || null,
      complement: address.complement?.trim() || null,
      neighborhood: address.neighborhood?.trim() || null,
      city: address.city?.trim() || null,
      state: address.state?.trim() || null,
      zipCode: address.zipCode?.trim() || null,
    }
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    markAllTouched()
    setSubmitError(null)
    const errs = validate()
    setFieldErrors(errs)
    if (Object.keys(errs).length > 0) return

    const addressPayload = buildAddressPayload()
    const customerUuid = clientType === 'CUSTOMER' ? clientUuid : null
    const companyUuid = clientType === 'COMPANY' ? clientUuid : null

    setSubmitting(true)
    try {
      if (isEdit) {
        const payload: ContractUpdateRequest = {
          customerUuid,
          companyUuid,
          address: addressPayload,
          description: description.trim(),
          clause: clause.trim(),
          servicesDescription: servicesDescription.trim() || '',
          productsDescription: productsDescription.trim() || '',
          startDate,
        }
        await onSaveUpdate(payload)
      } else {
        const payload: ContractCreateRequest = {
          customerUuid,
          companyUuid,
          address: addressPayload,
          description: description.trim(),
          clause: clause.trim(),
          servicesDescription: servicesDescription.trim() || null,
          productsDescription: productsDescription.trim() || null,
          startDate,
        }
        await onSaveCreate(payload)
      }
    } catch (err) {
      const apiErr = toApiError(err)
      setSubmitError(apiErr.message)
      if (apiErr.fieldErrors) setFieldErrors(apiErr.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form
      id="contract-form"
      onSubmit={handleSubmit}
      noValidate
      className="space-y-6"
    >
      {submitError ? <Alert variant="error">{submitError}</Alert> : null}

      {/* Cabeçalho: código + cliente + data de início */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Dados do contrato</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Selecione o tipo de cliente (PF ou PJ), busque pelo nome ou código
          e informe a data de início da vigência.
        </p>

        <div className="grid gap-4 sm:grid-cols-[200px_180px_1fr_180px]">
          <Input
            label="Código"
            value={code}
            readOnly
            disabled
            aria-readonly="true"
            className="font-mono"
          />

          <Select
            label="Tipo de cliente"
            value={clientType}
            onChange={(e) => handleClientTypeChange(e.target.value as ContractClientType)}
            options={CLIENT_TYPE_OPTIONS}
            aria-label="Tipo de cliente"
          />

          <div className="flex flex-col">
            <label
              htmlFor="contract-client"
              className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
            >
              {clientType === 'CUSTOMER' ? 'Cliente (PF)' : 'Empresa (PJ)'}
              <span className="ml-0.5 text-red-500">*</span>
            </label>
            <div className="relative">
              <Input
                id="contract-client"
                placeholder={
                  clientType === 'CUSTOMER'
                    ? 'Buscar cliente por nome, CPF ou código…'
                    : 'Buscar empresa por nome fantasia, CNPJ ou código…'
                }
                value={clientLabel}
                onChange={(e) => {
                  setClientLabel(e.target.value)
                  setClientUuid('')
                  handleClientQuery(e.target.value)
                }}
                onBlur={getBlurHandler('clientUuid')}
                hint={
                  clientLabel.trim().length > 0 &&
                  clientLabel.trim().length < 2
                    ? 'Digite ao menos 2 caracteres para buscar.'
                    : undefined
                }
                required
              />
              {clientSearching ? (
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
                  <Spinner size="sm" />
                </span>
              ) : null}
            </div>
            {clientOptions.length > 0 && !clientUuid ? (
              <ul className="mt-1 max-h-56 overflow-y-auto rounded-lg border border-slate-200 bg-white text-sm shadow-sm dark:border-slate-700 dark:bg-slate-900">
                {clientOptions.map((c) => (
                  <li key={c.uuid}>
                    <button
                      type="button"
                      className="flex w-full items-start gap-2 px-3 py-2 text-left hover:bg-slate-100 dark:hover:bg-slate-800"
                      onClick={() => {
                        setClientUuid(c.uuid)
                        setClientLabel(`${c.code} — ${c.name}`)
                        setClientOptions([])
                      }}
                    >
                      <span className="inline-flex shrink-0 rounded border border-slate-300 px-1.5 py-0.5 text-[10px] font-medium uppercase text-slate-600 dark:border-slate-600 dark:text-slate-300">
                        {c.type === 'CUSTOMER' ? 'PF' : 'PJ'}
                      </span>
                      <span className="flex-1">
                        <span className="block font-medium text-slate-900 dark:text-slate-100">
                          {c.name}
                        </span>
                        <span className="block text-xs text-slate-500 dark:text-slate-400">
                          {c.code} • {c.document}
                        </span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
            {shouldShowError('clientUuid', fieldErrors.clientUuid) ? (
              <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
                {fieldErrors.clientUuid}
              </p>
            ) : null}
          </div>

          <Input
            label="Início da vigência"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            onBlur={getBlurHandler('startDate')}
            required
          />
        </div>

        <div className="mt-4">
          <Input
            label="Título do contrato"
            value={contractTitle}
            onChange={(e) => setContractTitle(e.target.value)}
            placeholder="Contrato de Prestação de Serviço …"
            className="text-center"
          />
        </div>
      </section>

      {/* Descrição do contrato */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Descrição do contrato</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Bloco de texto principal (~1000 caracteres). Use o editor para
          formatar.
        </p>
        <RichTextEditor
          value={description}
          onChange={setDescription}
          onBlur={() => markAllTouched()}
          maxLength={4000}
          aria-label="Descrição do contrato"
        />
        {shouldShowError('description', fieldErrors.description) ? (
          <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
            {fieldErrors.description}
          </p>
        ) : null}
      </section>

      {/* Cláusula */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Cláusula</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Cláusula contratual — input largo de texto livre.
        </p>
        <textarea
          value={clause}
          onChange={(e) => setClause(e.target.value)}
          onBlur={() => markAllTouched()}
          rows={6}
          maxLength={4000}
          placeholder="As partes acordam que..."
          className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/40 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
        />
        {shouldShowError('clause', fieldErrors.clause) ? (
          <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
            {fieldErrors.clause}
          </p>
        ) : null}
      </section>

      {/* Endereço (opcional) */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex items-center justify-between">
          <h3 className="text-base font-semibold">Endereço</h3>
          <label className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
            <input
              type="checkbox"
              checked={hasAddress}
              onChange={(e) => setHasAddress(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary"
            />
            Informar endereço
          </label>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Quando preenchido, é sugerido a partir do cadastro do cliente
          selecionado (no cadastro novo, só se todos os campos estiverem vazios).
        </p>

        {hasAddress ? (
          <div className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-[1fr_120px]">
              <Input
                label="CEP"
                value={address.zipCode ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({
                    ...prev,
                    zipCode: maskZipCode(e.target.value),
                  }))
                }
                onBlur={() => {
                  getBlurHandler('address.zipCode')()
                  void handleCepBlur()
                }}
                error={shouldShowError(
                  'address.zipCode',
                  fieldErrors['address.zipCode'],
                )}
                hint={cepError ?? undefined}
                rightAdornment={
                  cepLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : undefined
                }
                placeholder="00000-000"
              />
              <Input
                label="UF"
                value={address.state ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({
                    ...prev,
                    state: e.target.value.toUpperCase().slice(0, 2),
                  }))
                }
                maxLength={2}
              />
            </div>
            <div className="grid gap-3 sm:grid-cols-[1fr_120px]">
              <Input
                label="Logradouro"
                value={address.street ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({ ...prev, street: e.target.value }))
                }
              />
              <Input
                label="Número"
                value={address.number ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({ ...prev, number: e.target.value }))
                }
                placeholder="S/N"
              />
            </div>
            <div className="grid gap-3 sm:grid-cols-[1fr_1fr]">
              <Input
                label="Complemento"
                value={address.complement ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({
                    ...prev,
                    complement: e.target.value,
                  }))
                }
              />
              <Input
                label="Bairro"
                value={address.neighborhood ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({
                    ...prev,
                    neighborhood: e.target.value,
                  }))
                }
              />
            </div>
            <div className="grid gap-3 sm:grid-cols-[1fr_120px]">
              <Input
                label="Cidade"
                value={address.city ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({ ...prev, city: e.target.value }))
                }
              />
              <Select
                label="UF"
                options={[{ value: '', label: 'Selecione…' }, ...UF_OPTIONS]}
                value={address.state ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({ ...prev, state: e.target.value }))
                }
              />
            </div>
          </div>
        ) : (
          <p className="text-sm text-slate-500 dark:text-slate-400">
            <MapPin className="mr-1 inline h-4 w-4" /> Sem endereço informado.
          </p>
        )}
      </section>

      {/* Serviços */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Serviços</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Bloco de texto descrevendo os serviços do contrato. Opcional —
          sem preço, sem linhas estruturadas.
        </p>
        <RichTextEditor
          value={servicesDescription}
          onChange={setServicesDescription}
          onBlur={() => markAllTouched()}
          maxLength={4000}
          aria-label="Descrição dos serviços"
        />
      </section>

      {/* Produtos */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Produtos</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Bloco de texto descrevendo os produtos do contrato. Opcional.
        </p>
        <RichTextEditor
          value={productsDescription}
          onChange={setProductsDescription}
          onBlur={() => markAllTouched()}
          maxLength={4000}
          aria-label="Descrição dos produtos"
        />
      </section>

      {/* Botão oculto — submit feito pelo botão na página externa (form="contract-form"). */}
      <button
        type="submit"
        className="hidden"
        disabled={submitting}
        aria-hidden
      />
    </form>
  )
}