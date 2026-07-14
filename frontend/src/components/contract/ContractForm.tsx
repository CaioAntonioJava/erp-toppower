import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from 'react'
import { Loader2, MapPin, Plus, Trash2 } from 'lucide-react'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Button } from '../ui/Button'
import { Alert } from '../ui/Alert'
import { Spinner } from '../ui/Spinner'
import { RichTextEditor } from '../ui/RichTextEditor'
import { toApiError, errorMessage } from '../../lib/errors'
import { useFieldTouched } from '../../hooks/useFieldTouched'
import { getCep } from '../../api/cep.api'
import { getCustomer } from '../../api/customer.api'
import { getCompany } from '../../api/company.api'
import { searchContractsClients } from '../../api/contract.api'
import { searchProducts } from '../../api/product.api'
import { BRAZILIAN_STATES } from '../../lib/brazilianStates'
import { maskZipCode } from '../../lib/documents'
import { useOrganization } from '../../context/OrganizationContext'
import type { ClientSummaryResponse } from '../../types/quotation'
import type {
  ContractAddressRequest,
  ContractClientType,
  ContractClauseRequest,
  ContractCreateRequest,
  ContractProductItemRequest,
  ContractResponse,
  ContractServiceItemRequest,
  ContractUpdateRequest,
} from '../../types/contract'
import type { ProductResponse } from '../../types/product'

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
  id: string
  name: string
  code: string
  document: string
  type: ContractClientType
}

/** Linha do editor de cláusulas (estado local). */
interface ClauseDraft {
  rowKey: string
  description: string
}

/** Linha de item de serviço (estado local). */
interface ServiceItemDraft {
  rowKey: string
  description: string
}

/** Linha de item de produto (estado local). */
interface ProductItemDraft {
  rowKey: string
  productId: string
  productLabel: string
  quantity: string
}

function nextRowKey(): string {
  return `row_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
}

const UF_OPTIONS = BRAZILIAN_STATES.map((s) => ({
  value: s.uf,
  label: s.uf,
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
    id: String(c.id),
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
  const [clientId, setClientId] = useState<string>(
    contract?.clientId != null ? String(contract.clientId) : '',
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
  const [clauses, setClauses] = useState<ClauseDraft[]>(() => {
    if (contract?.clauses && contract.clauses.length > 0) {
      return contract.clauses.map((c) => ({
        rowKey: nextRowKey(),
        description: c.description,
      }))
    }
    return [{ rowKey: nextRowKey(), description: '' }]
  })
  const [servicesDescription, setServicesDescription] = useState<string>(
    contract?.servicesDescription ?? '',
  )
  const [productsDescription, setProductsDescription] = useState<string>(
    contract?.productsDescription ?? '',
  )
  const [additionalDescription, setAdditionalDescription] = useState<string>(
    contract?.additionalDescription ?? '',
  )

  // === Itens de serviço ===
  const [serviceItems, setServiceItems] = useState<ServiceItemDraft[]>(() => {
    if (contract?.serviceItems && contract.serviceItems.length > 0) {
      return contract.serviceItems.map((s) => ({
        rowKey: nextRowKey(),
        description: s.description,
      }))
    }
    return []
  })

  // === Itens de produto ===
  const [productItems, setProductItems] = useState<ProductItemDraft[]>(() => {
    if (contract?.productItems && contract.productItems.length > 0) {
      return contract.productItems.map((p) => ({
        rowKey: nextRowKey(),
        productId: String(p.productId),
        productLabel: '',
        quantity: String(p.quantity),
      }))
    }
    return []
  })
  const [productOptions, setProductOptions] = useState<ProductResponse[]>([])
  const [productSearching, setProductSearching] = useState(false)
  const productTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // === Valor total ===
  const [totalValue, setTotalValue] = useState<string>(
    contract?.totalValue != null ? String(contract.totalValue) : '',
  )

  // === Prazo de entrega ===
  const [deliveryDeadline, setDeliveryDeadline] = useState<string>(
    contract?.deliveryDeadline ?? '',
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
    setClientId('')
    setClientLabel('')
    setClientOptions([])
  }

  // Auto-preencher endereço a partir do cliente selecionado
  // (somente quando o form está vazio — não sobrescreve edição manual).
  const lastAutoFilledIdRef = useRef<string | null>(null)
  useEffect(() => {
    if (!clientId) {
      lastAutoFilledIdRef.current = null
      return
    }
    // Já auto-preenchemos para este cliente nesta sessão? Sai.
    if (lastAutoFilledIdRef.current === clientId) return
    // Só auto-preenche quando o form está vazio e não é modo edit
    // (em edit o usuário já viu/tem o endereço que está persistido).
    if (!isEdit && hasAddress) {
      const isEmpty = !address.street && !address.city && !address.zipCode
      if (isEmpty) {
        lastAutoFilledIdRef.current = clientId
        const fetcher =
          clientType === 'CUSTOMER' ? getCustomer : getCompany
        fetcher(Number(clientId))
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
  }, [clientId, clientType, hasAddress])

  // === handlers de cláusulas ===
  function addClause() {
    setClauses((prev) => [
      ...prev,
      { rowKey: nextRowKey(), description: '' },
    ])
  }
  function removeClause(rowKey: string) {
    setClauses((prev) => prev.filter((c) => c.rowKey !== rowKey))
  }
  function updateClause(rowKey: string, description: string) {
    setClauses((prev) =>
      prev.map((c) => (c.rowKey === rowKey ? { ...c, description } : c)),
    )
  }

  /**
   * Divisão puramente visual da lista linear {@link clauses} em três
   * caixas no formulário ("Cláusula 1", "Cláusula 2" e "Cláusula 3").
   * A divisão é por terço: primeiro terço em Cláusula 1, segundo terço
   * em Cláusula 2, restante em Cláusula 3.
   *
   * <p>A divisão é recomputada a cada render: novas cláusulas adicionadas
   * por qualquer uma das caixas entram no array linear e podem migrar de
   * caixa ao atingir/ultrapassar os pontos de corte. Isso é intencional —
   * o usuário está editando uma lista única, apenas com layout dividido.</p>
   */
  const thirdSize = Math.ceil(clauses.length / 3)
  const firstThirdEnd = thirdSize
  const secondThirdEnd = thirdSize * 2
  const clausesFirstThird = clauses.slice(0, firstThirdEnd)
  const clausesSecondThird = clauses.slice(firstThirdEnd, secondThirdEnd)
  const clausesThirdThird = clauses.slice(secondThirdEnd)

  // === handlers de itens de serviço ===
  function addServiceItem() {
    setServiceItems((prev) => [
      ...prev,
      { rowKey: nextRowKey(), description: '' },
    ])
  }
  function removeServiceItem(rowKey: string) {
    setServiceItems((prev) => prev.filter((s) => s.rowKey !== rowKey))
  }
  function updateServiceItem(rowKey: string, description: string) {
    setServiceItems((prev) =>
      prev.map((s) => (s.rowKey === rowKey ? { ...s, description } : s)),
    )
  }

  // === handlers de itens de produto ===
  function addProductItem() {
    setProductItems((prev) => [
      ...prev,
      {
        rowKey: nextRowKey(),
        productId: '',
        productLabel: '',
        quantity: '',
      },
    ])
  }
  function removeProductItem(rowKey: string) {
    setProductItems((prev) => prev.filter((p) => p.rowKey !== rowKey))
  }
  function patchProductItem(
    rowKey: string,
    patch: Partial<ProductItemDraft>,
  ) {
    setProductItems((prev) =>
      prev.map((p) => (p.rowKey === rowKey ? { ...p, ...patch } : p)),
    )
  }

  const handleProductQuery = useCallback((query: string) => {
    if (productTimerRef.current) {
      clearTimeout(productTimerRef.current)
      productTimerRef.current = null
    }
    if (query.trim().length < 2) {
      setProductOptions([])
      setProductSearching(false)
      return
    }
    setProductSearching(true)
    productTimerRef.current = setTimeout(async () => {
      try {
        const results = await searchProducts({ query: query.trim(), size: 10 })
        setProductOptions(results.content)
      } catch {
        setProductOptions([])
      } finally {
        setProductSearching(false)
      }
    }, 300)
  }, [])

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
    if (!clientId) errs.clientId = 'Selecione um cliente ou empresa.'
    if (!description.trim()) errs.description = 'Descrição do contrato é obrigatória.'
    if (description.length > 4000) errs.description = 'Descrição deve ter no máximo 4000 caracteres.'
    const validClauses = clauses.filter((c) => c.description.trim() !== '')
    if (validClauses.length === 0) {
      errs.clauses = 'O contrato deve ter ao menos uma cláusula.'
    } else {
      clauses.forEach((c, idx) => {
        if (c.description.trim() !== '' && c.description.length > 4000) {
          errs[`clauses.${idx}`] = 'Cláusula deve ter no máximo 4000 caracteres.'
        }
      })
    }
    if (hasAddress && address.zipCode) {
      const digits = address.zipCode.replace(/\D/g, '')
      if (digits.length !== 8) {
        errs['address.zipCode'] = 'CEP deve conter 8 dígitos.'
      }
    }
    // Valida itens de serviço
    serviceItems.forEach((s, idx) => {
      if (s.description.trim() !== '' && s.description.length > 2000) {
        errs[`serviceItems.${idx}`] = 'Descrição do serviço deve ter no máximo 2000 caracteres.'
      }
    })
    // Valida itens de produto
    productItems.forEach((p, idx) => {
      if (p.productId && (!p.quantity || Number(p.quantity) <= 0)) {
        errs[`productItems.${idx}.quantity`] = 'Quantidade deve ser maior que zero.'
      }
    })
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
    const customerId = clientType === 'CUSTOMER' ? (clientId ? Number(clientId) : null) : null
    const companyId = clientType === 'COMPANY' ? (clientId ? Number(clientId) : null) : null

    const clausesPayload: ContractClauseRequest[] = clauses
      .filter((c) => c.description.trim() !== '')
      .map((c) => ({ description: c.description.trim() }))

    const serviceItemsPayload: ContractServiceItemRequest[] = serviceItems
      .filter((s) => s.description.trim() !== '')
      .map((s) => ({ description: s.description.trim() }))

    const productItemsPayload: ContractProductItemRequest[] = productItems
      .filter((p) => p.productId !== '' && p.quantity !== '')
      .map((p) => ({
        productId: Number(p.productId),
        quantity: Number(p.quantity),
      }))

    const totalValueNum = totalValue.trim()
      ? Number(totalValue.trim().replace(',', '.'))
      : null

    setSubmitting(true)
    try {
      if (isEdit) {
        const payload: ContractUpdateRequest = {
          customerId,
          companyId,
          address: addressPayload,
          description: description.trim(),
          clauses: clausesPayload.length > 0 ? clausesPayload : undefined,
          servicesDescription: servicesDescription.trim() || '',
          productsDescription: productsDescription.trim() || '',
          serviceItems: serviceItemsPayload.length > 0 ? serviceItemsPayload : undefined,
          productItems: productItemsPayload.length > 0 ? productItemsPayload : undefined,
          totalValue: totalValueNum,
          deliveryDeadline: deliveryDeadline.trim() || null,
          additionalDescription: additionalDescription.trim() || '',
          startDate,
        }
        await onSaveUpdate(payload)
      } else {
        const payload: ContractCreateRequest = {
          customerId,
          companyId,
          address: addressPayload,
          description: description.trim(),
          clauses: clausesPayload,
          servicesDescription: servicesDescription.trim() || null,
          productsDescription: productsDescription.trim() || null,
          serviceItems: serviceItemsPayload.length > 0 ? serviceItemsPayload : null,
          productItems: productItemsPayload.length > 0 ? productItemsPayload : null,
          totalValue: totalValueNum,
          deliveryDeadline: deliveryDeadline.trim() || null,
          additionalDescription: additionalDescription.trim() || null,
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
                  setClientId('')
                  handleClientQuery(e.target.value)
                }}
                onBlur={getBlurHandler('clientId')}
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
            {clientOptions.length > 0 && !clientId ? (
              <ul className="mt-1 max-h-56 overflow-y-auto rounded-lg border border-slate-200 bg-white text-sm shadow-sm dark:border-slate-700 dark:bg-slate-900">
                {clientOptions.map((c) => (
                  <li key={c.id}>
                    <button
                      type="button"
                      className="flex w-full items-start gap-2 px-3 py-2 text-left hover:bg-slate-100 dark:hover:bg-slate-800"
                      onClick={() => {
                        setClientId(String(c.id))
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
            {shouldShowError('clientId', fieldErrors.clientId) ? (
              <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
                {fieldErrors.clientId}
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

      {/*
        Cláusula 1
        ---------------------------------------------------------------------
        O array `clauses` é uma lista linear única, mas é renderizado em três
        caixas no formulário (Cláusula 1, Cláusula 2 e Cláusula 3) — a
        Cláusula 2 fica abaixo da seção "Produtos" e a Cláusula 3 fica
        abaixo do "Prazo de entrega", antes de "Valor total". A divisão é
        por terço, recomputada a cada render a partir de `firstThirdEnd`.
      */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Cláusula 1</h3>
          <Button type="button" variant="secondary" onClick={addClause}>
            <Plus className="h-4 w-4" />
            Adicionar cláusula
          </Button>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Cláusulas contratuais — cada cláusula é um texto livre. Adicione
          quantas forem necessárias.
        </p>

        {shouldShowError('clauses', fieldErrors.clauses) ? (
          <p className="mb-3 text-sm text-red-600 dark:text-red-400">
            {fieldErrors.clauses}
          </p>
        ) : null}

        {clausesFirstThird.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhuma cláusula. Clique em <strong>Adicionar cláusula</strong> para
            começar.
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {clausesFirstThird.map((c, idx) => (
              <div
                key={c.rowKey}
                className="flex items-start gap-2 rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-900/50"
              >
                <div className="flex-1">
                  <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">
                    Cláusula {idx + 1}
                  </label>
                  <input
                    type="text"
                    value={c.description}
                    onChange={(e) => updateClause(c.rowKey, e.target.value)}
                    onBlur={() => markAllTouched()}
                    maxLength={4000}
                    placeholder="Digite a cláusula..."
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/40 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                  />
                  {shouldShowError(`clauses.${idx}`, fieldErrors[`clauses.${idx}`]) ? (
                    <p className="mt-1 text-xs text-red-600 dark:text-red-400">
                      {fieldErrors[`clauses.${idx}`]}
                    </p>
                  ) : null}
                </div>
                <button
                  type="button"
                  onClick={() => removeClause(c.rowKey)}
                  disabled={clauses.length <= 1}
                  aria-label="Remover cláusula"
                  title="Remover cláusula"
                  className="mt-5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-slate-500 hover:bg-slate-200 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-slate-700 dark:hover:text-red-400"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}
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
            <div className="grid gap-3 sm:grid-cols-[1fr_1fr_120px]">
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
            <div className="grid gap-3 sm:grid-cols-[1fr_1fr_1fr]">
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
              <Input
                label="Cidade"
                value={address.city ?? ''}
                onChange={(e) =>
                  setAddress((prev) => ({ ...prev, city: e.target.value }))
                }
              />
            </div>
            <div className="grid gap-3 sm:grid-cols-[120px]">
              <Select
                label="UF"
                options={[{ value: '', label: 'UF' }, ...UF_OPTIONS]}
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

      {/* Serviços — Itens + Descrição */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Serviços</h3>
          <Button type="button" variant="secondary" onClick={addServiceItem}>
            <Plus className="h-4 w-4" />
            Adicionar serviço
          </Button>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Itens de serviço do contrato. Cada item é uma descrição livre —
          sem preço. Opcional.
        </p>

        {serviceItems.length === 0 ? (
          <div className="mb-4 rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhum item de serviço. Clique em <strong>Adicionar serviço</strong> para
            começar.
          </div>
        ) : (
          <div className="mb-4 flex flex-col gap-3">
            {serviceItems.map((s, idx) => (
              <div
                key={s.rowKey}
                className="flex items-start gap-2 rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-900/50"
              >
                <div className="flex-1">
                  <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">
                    Serviço {idx + 1}
                  </label>
                  <input
                    type="text"
                    value={s.description}
                    onChange={(e) => updateServiceItem(s.rowKey, e.target.value)}
                    onBlur={() => markAllTouched()}
                    maxLength={2000}
                    placeholder="Digite a descrição do serviço..."
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/40 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                  />
                  {shouldShowError(`serviceItems.${idx}`, fieldErrors[`serviceItems.${idx}`]) ? (
                    <p className="mt-1 text-xs text-red-600 dark:text-red-400">
                      {fieldErrors[`serviceItems.${idx}`]}
                    </p>
                  ) : null}
                </div>
                <button
                  type="button"
                  onClick={() => removeServiceItem(s.rowKey)}
                  disabled={serviceItems.length <= 1}
                  aria-label="Remover serviço"
                  title="Remover serviço"
                  className="mt-5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-slate-500 hover:bg-slate-200 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-slate-700 dark:hover:text-red-400"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}

        <p className="mb-2 text-sm font-medium text-slate-600 dark:text-slate-300">
          Descrição dos serviços
        </p>
        <RichTextEditor
          value={servicesDescription}
          onChange={setServicesDescription}
          onBlur={() => markAllTouched()}
          maxLength={4000}
          aria-label="Descrição dos serviços"
        />
      </section>

      {/* Produtos — Itens + Descrição */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Produtos</h3>
          <Button type="button" variant="secondary" onClick={addProductItem}>
            <Plus className="h-4 w-4" />
            Adicionar produto
          </Button>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Itens de produto do contrato. Selecione o produto e informe a
          quantidade. Opcional.
        </p>

        {productItems.length === 0 ? (
          <div className="mb-4 rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhum item de produto. Clique em <strong>Adicionar produto</strong> para
            começar.
          </div>
        ) : (
          <div className="mb-4 flex flex-col gap-3">
            {productItems.map((p, idx) => (
              <div
                key={p.rowKey}
                className="flex items-start gap-2 rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-900/50"
              >
                <div className="flex-1 space-y-2">
                  <label className="block text-xs font-medium text-slate-500 dark:text-slate-400">
                    Produto {idx + 1}
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      value={p.productLabel}
                      onChange={(e) => {
                        patchProductItem(p.rowKey, {
                          productLabel: e.target.value,
                          productId: '',
                        })
                        handleProductQuery(e.target.value)
                      }}
                      placeholder="Buscar produto por nome ou código…"
                      className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/40 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    />
                    {productSearching ? (
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400">
                        <Spinner size="sm" />
                      </span>
                    ) : null}
                    {productOptions.length > 0 && !p.productId ? (
                      <ul className="absolute z-10 mt-1 max-h-40 w-full overflow-y-auto rounded-lg border border-slate-200 bg-white text-sm shadow-sm dark:border-slate-700 dark:bg-slate-900">
                        {productOptions.map((prod) => (
                          <li key={prod.id}>
                            <button
                              type="button"
                              className="flex w-full items-start gap-2 px-3 py-2 text-left hover:bg-slate-100 dark:hover:bg-slate-800"
                              onClick={() => {
                                patchProductItem(p.rowKey, {
                                  productId: String(prod.id),
                                  productLabel: prod.code
                                    ? `${prod.code} — ${prod.name}`
                                    : prod.name,
                                })
                                setProductOptions([])
                              }}
                            >
                              <span className="flex-1">
                                <span className="block font-medium text-slate-900 dark:text-slate-100">
                                  {prod.name}
                                </span>
                                {prod.code ? (
                                  <span className="block text-xs text-slate-500 dark:text-slate-400">
                                    {prod.code}
                                  </span>
                                ) : null}
                              </span>
                            </button>
                          </li>
                        ))}
                      </ul>
                    ) : null}
                  </div>
                  <div className="grid grid-cols-[1fr_120px] gap-2">
                    <div />
                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">
                        Quantidade
                      </label>
                      <input
                        type="number"
                        step="0.0001"
                        min="0.0001"
                        value={p.quantity}
                        onChange={(e) =>
                          patchProductItem(p.rowKey, { quantity: e.target.value })
                        }
                        onBlur={() => markAllTouched()}
                        placeholder="0,0000"
                        className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/40 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                      />
                      {shouldShowError(`productItems.${idx}.quantity`, fieldErrors[`productItems.${idx}.quantity`]) ? (
                        <p className="mt-1 text-xs text-red-600 dark:text-red-400">
                          {fieldErrors[`productItems.${idx}.quantity`]}
                        </p>
                      ) : null}
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => removeProductItem(p.rowKey)}
                  disabled={productItems.length <= 1}
                  aria-label="Remover produto"
                  title="Remover produto"
                  className="mt-5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-slate-500 hover:bg-slate-200 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-slate-700 dark:hover:text-red-400"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}

        <p className="mb-2 text-sm font-medium text-slate-600 dark:text-slate-300">
          Descrição dos produtos
        </p>
        <RichTextEditor
          value={productsDescription}
          onChange={setProductsDescription}
          onBlur={() => markAllTouched()}
          maxLength={4000}
          aria-label="Descrição dos produtos"
        />
      </section>

      {/*
        Cláusula 2 — duplicação visual da seção "Cláusula 1"
        ---------------------------------------------------------------------
        Esta seção é uma duplicação visual (mesmo modelo, mesmo array
        `clauses`) posicionada abaixo da "Descrição dos produtos" e antes
        de "Prazo de entrega". O usuário continua editando uma única
        lista linear: novas cláusulas adicionadas aqui entram no mesmo
        array que alimenta a "Cláusula 1" e a "Cláusula 3". A numeração
        segue a posição real na lista (Cláusula {firstThirdEnd + idx + 1}).
      */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Cláusula 2</h3>
          <Button type="button" variant="secondary" onClick={addClause}>
            <Plus className="h-4 w-4" />
            Adicionar cláusula
          </Button>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Continuação das cláusulas contratuais — mesmo modelo da seção
          anterior.
        </p>

        {clausesSecondThird.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhuma cláusula nesta seção. Clique em <strong>Adicionar
            cláusula</strong> para começar.
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {clausesSecondThird.map((c, idx) => (
              <div
                key={c.rowKey}
                className="flex items-start gap-2 rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-900/50"
              >
                <div className="flex-1">
                  <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">
                    Cláusula {firstThirdEnd + idx + 1}
                  </label>
                  <input
                    type="text"
                    value={c.description}
                    onChange={(e) => updateClause(c.rowKey, e.target.value)}
                    onBlur={() => markAllTouched()}
                    maxLength={4000}
                    placeholder="Digite a cláusula..."
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/40 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                  />
                  {shouldShowError(
                    `clauses.${firstThirdEnd + idx}`,
                    fieldErrors[`clauses.${firstThirdEnd + idx}`],
                  ) ? (
                    <p className="mt-1 text-xs text-red-600 dark:text-red-400">
                      {fieldErrors[`clauses.${firstThirdEnd + idx}`]}
                    </p>
                  ) : null}
                </div>
                <button
                  type="button"
                  onClick={() => removeClause(c.rowKey)}
                  disabled={clauses.length <= 1}
                  aria-label="Remover cláusula"
                  title="Remover cláusula"
                  className="mt-5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-slate-500 hover:bg-slate-200 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-slate-700 dark:hover:text-red-400"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Prazo de entrega */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Prazo de entrega</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Texto livre. Opcional — não tem semântica de data, é apenas uma
          descrição (ex.: "30 dias úteis", "15 dias após a assinatura",
          "entrega imediata").
        </p>
        <div className="max-w-2xl">
          <Input
            label="Prazo de entrega"
            value={deliveryDeadline}
            onChange={(e) => setDeliveryDeadline(e.target.value)}
            placeholder="Ex.: 30 dias úteis"
            maxLength={500}
          />
        </div>
      </section>

      {/*
        Cláusula 3 — duplicação visual das seções "Cláusula 1" e "Cláusula 2"
        ---------------------------------------------------------------------
        Esta seção é uma duplicação visual (mesmo modelo, mesmo array
        `clauses`) posicionada abaixo do "Prazo de entrega" e antes de
        "Valor total". O usuário continua editando uma única lista
        linear: novas cláusulas adicionadas aqui entram no mesmo array
        que alimenta as outras duas caixas. A numeração segue a posição
        real na lista (Cláusula {secondThirdEnd + idx + 1}).
      */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Cláusula 3</h3>
          <Button type="button" variant="secondary" onClick={addClause}>
            <Plus className="h-4 w-4" />
            Adicionar cláusula
          </Button>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Continuação das cláusulas contratuais — mesmo modelo das seções
          anteriores.
        </p>

        {clausesThirdThird.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhuma cláusula nesta seção. Clique em <strong>Adicionar
            cláusula</strong> para começar.
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {clausesThirdThird.map((c, idx) => (
              <div
                key={c.rowKey}
                className="flex items-start gap-2 rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-900/50"
              >
                <div className="flex-1">
                  <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">
                    Cláusula {secondThirdEnd + idx + 1}
                  </label>
                  <input
                    type="text"
                    value={c.description}
                    onChange={(e) => updateClause(c.rowKey, e.target.value)}
                    onBlur={() => markAllTouched()}
                    maxLength={4000}
                    placeholder="Digite a cláusula..."
                    className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/40 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                  />
                  {shouldShowError(
                    `clauses.${secondThirdEnd + idx}`,
                    fieldErrors[`clauses.${secondThirdEnd + idx}`],
                  ) ? (
                    <p className="mt-1 text-xs text-red-600 dark:text-red-400">
                      {fieldErrors[`clauses.${secondThirdEnd + idx}`]}
                    </p>
                  ) : null}
                </div>
                <button
                  type="button"
                  onClick={() => removeClause(c.rowKey)}
                  disabled={clauses.length <= 1}
                  aria-label="Remover cláusula"
                  title="Remover cláusula"
                  className="mt-5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-slate-500 hover:bg-slate-200 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-slate-700 dark:hover:text-red-400"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Descrição adicional — bloco rich text opcional exibido após
          as cláusulas e antes do valor total, no formulário, na página
          de detalhe e no PDF. */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Descrição adicional</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Bloco de texto opcional exibido após as cláusulas contratuais
          e antes do valor total. Use o editor para formatar.
        </p>
        <RichTextEditor
          value={additionalDescription}
          onChange={setAdditionalDescription}
          onBlur={() => markAllTouched()}
          maxLength={4000}
          aria-label="Descrição adicional"
        />
      </section>

      {/* Valor total */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Valor total</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Valor total do contrato (preenchimento manual). Opcional.
        </p>
        <div className="max-w-xs">
          <Input
            label="Valor total (R$)"
            type="text"
            inputMode="decimal"
            value={totalValue}
            onChange={(e) => setTotalValue(e.target.value)}
            placeholder="0,00"
          />
        </div>
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