import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Button } from '../ui/Button'
import { Alert } from '../ui/Alert'
import { Spinner } from '../ui/Spinner'
import { RichTextEditor } from '../ui/RichTextEditor'
import { toApiError } from '../../lib/errors'
import { parseNumber, formatBRLValue } from '../../lib/money'
import { useFieldTouched } from '../../hooks/useFieldTouched'
import { useActiveCarriers } from '../../hooks/useActiveCarriers'
import { FreightConditionsFields } from './FreightConditionsFields'
import { getProduct, searchProducts } from '../../api/product.api'
import {
  searchTechnicalProposalClients,
  simulateTechnicalProposal,
} from '../../api/technicalProposal.api'
import { BRAZILIAN_STATES } from '../../lib/brazilianStates'
import { maskZipCode } from '../../lib/documents'
import type { ProductResponse, UnitType } from '../../types/product'
import type {
  ClientSummaryResponse,
  DiscountType,
  FreightType,
  PaymentCondition,
} from '../../types/quotation'
import {
  DISCOUNT_TYPE_OPTIONS,
  PAYMENT_CONDITION_OPTIONS,
} from '../../types/quotation'
import type {
  TechnicalProposalAddressRequest,
  TechnicalProposalClientType,
  TechnicalProposalCreateRequest,
  TechnicalProposalObjectiveRequest,
  TechnicalProposalProductItemRequest,
  TechnicalProposalResponse,
  TechnicalProposalServiceItemRequest,
  TechnicalProposalSimulateResponse,
  TechnicalProposalUpdateRequest,
} from '../../types/technicalProposal'
import { TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS } from '../../types/technicalProposal'

interface TechnicalProposalFormProps {
  /** Proposta existente (modo edição). Quando omitido, é cadastro novo. */
  proposal?: TechnicalProposalResponse
  /** Próximo código previsto pelo backend (modo create). */
  initialCode?: string | null
  onSaveCreate: (payload: TechnicalProposalCreateRequest) => Promise<void>
  onSaveUpdate: (payload: TechnicalProposalUpdateRequest) => Promise<void>
}

/** Linha do editor de objetivos (estado local). */
interface ObjectiveDraft {
  rowKey: string
  description: string
}

/** Linha do editor de serviços (estado local). */
interface ServiceDraft {
  rowKey: string
  description: string
  price: string
}

/** Linha do editor de produtos (estado local). */
interface ProductDraft {
  rowKey: string
  productUuid: string
  productLabel: string
  unitType: UnitType | null
  unitPrice: string
  quantity: string
  discountType: DiscountType | null
  discount: string
}

const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

function nextRowKey(): string {
  return `row_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
}

function todayIso(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const UF_OPTIONS = BRAZILIAN_STATES.map((s) => ({
  value: s.uf,
  label: `${s.uf} — ${s.name}`,
}))

export function TechnicalProposalForm({
  proposal,
  initialCode = null,
  onSaveCreate,
  onSaveUpdate,
}: TechnicalProposalFormProps) {
  const isEdit = !!proposal

  // === cabeçalho ===
  const [clientType, setClientType] = useState<TechnicalProposalClientType>(
    proposal?.clientType ?? 'CUSTOMER',
  )
  const [clientUuid, setClientUuid] = useState<string>(
    proposal?.customerUuid ?? proposal?.companyUuid ?? '',
  )
  const [clientLabel, setClientLabel] = useState<string>('')
  const [objectives, setObjectives] = useState<ObjectiveDraft[]>(() => {
    if (proposal?.objectives && proposal.objectives.length > 0) {
      return proposal.objectives.map((o) => ({
        rowKey: nextRowKey(),
        description: o.description,
      }))
    }
    // Em modo create, iniciamos com uma linha vazia.
    if (!proposal) {
      return [{ rowKey: nextRowKey(), description: '' }]
    }
    return []
  })
  const [description, setDescription] = useState<string>(
    proposal?.description ?? '',
  )
  const [technicalResponsible, setTechnicalResponsible] = useState<string>(
    proposal?.technicalResponsible ?? '',
  )
  const [responsibleEmail, setResponsibleEmail] = useState<string>(
    proposal?.email ?? '',
  )
  const [startDate, setStartDate] = useState<string>(
    proposal?.startDate ?? todayIso(),
  )
  const [endDate, setEndDate] = useState<string>(proposal?.endDate ?? '')
  const [profitMargin, setProfitMargin] = useState<string>(
    proposal?.profitMargin != null ? String(proposal.profitMargin) : '',
  )
  const [discountType, setDiscountType] = useState<DiscountType | ''>(
    proposal?.discountType ?? '',
  )
  const [discount, setDiscount] = useState<string>(
    proposal?.discount != null ? String(proposal.discount) : '',
  )
  const [deliveryDeadline, setDeliveryDeadline] = useState<string>(
    proposal?.deliveryDeadline ?? '',
  )
  const [paymentCondition, setPaymentCondition] = useState<PaymentCondition | ''>(
    proposal?.paymentCondition ?? '',
  )
  const [validity, setValidity] = useState<string>(proposal?.validity ?? '')
  const [deliveryType, setDeliveryType] = useState<FreightType | ''>(
    proposal?.deliveryType ?? '',
  )
  // Valor do frete (manual).
  const [freightValue, setFreightValue] = useState<string>(
    proposal?.freightValue != null ? formatBRLValue(proposal.freightValue) : '',
  )
  // Transportadora (Carrier) responsável pelo frete. Opcional.
  const [carrierUuid, setCarrierUuid] = useState<string>(
    proposal?.carrierUuid ?? '',
  )
  const [notes, setNotes] = useState<string>(proposal?.notes ?? '')

  // === código gerado pelo servidor ===
  const [code, setCode] = useState<string>(
    proposal?.code ?? initialCode ?? '',
  )
  const codeDirtyRef = useRef<boolean>(false)

  // === endereço (opcional) ===
  const [hasAddress, setHasAddress] = useState<boolean>(
    !!proposal?.address &&
      (proposal.address.street != null ||
        proposal.address.city != null ||
        proposal.address.zipCode != null),
  )
  const [address, setAddress] = useState<TechnicalProposalAddressRequest>(() => ({
    street: proposal?.address?.street ?? '',
    number: proposal?.address?.number ?? '',
    complement: proposal?.address?.complement ?? '',
    neighborhood: proposal?.address?.neighborhood ?? '',
    city: proposal?.address?.city ?? '',
    state: proposal?.address?.state ?? '',
    zipCode: proposal?.address?.zipCode ?? '',
  }))

  // === itens de serviço ===
  const [serviceItems, setServiceItems] = useState<ServiceDraft[]>(() => {
    if (proposal?.serviceItems && proposal.serviceItems.length > 0) {
      return proposal.serviceItems.map((s) => ({
        rowKey: nextRowKey(),
        description: s.description,
        price: s.price != null ? formatBRLValue(s.price) : '',
      }))
    }
    // Em modo create, iniciamos com uma linha vazia.
    if (!proposal) {
      return [{ rowKey: nextRowKey(), description: '', price: '' }]
    }
    return []
  })

  // === itens de produto ===
  const [productItems, setProductItems] = useState<ProductDraft[]>(() => {
    if (proposal?.productItems && proposal.productItems.length > 0) {
      return proposal.productItems.map((p) => ({
        rowKey: nextRowKey(),
        productUuid: p.productUuid,
        productLabel: '',
        unitType: null,
        unitPrice: formatBRLValue(p.unitPrice),
        quantity: String(p.quantity),
        discountType: p.discountType ?? null,
        discount: p.discount != null ? formatBRLValue(p.discount) : '',
      }))
    }
    return []
  })

  // === coleções auxiliares ===
  const [clientOptions, setClientOptions] = useState<ClientSummaryResponse[]>(
    [],
  )
  const [clientSearching, setClientSearching] = useState(false)
  const [productOptions, setProductOptions] = useState<ProductResponse[]>([])
  const [productSearching, setProductSearching] = useState(false)
  const [activeProductRow, setActiveProductRow] = useState<string | null>(null)

  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const { shouldShowError, getBlurHandler, markAllTouched, reset } =
    useFieldTouched()

  // Carrega transportadoras ativas (com fallback da selecionada em edição).
  const { carriers, carriersLoading } = useActiveCarriers(
    proposal?.carrierUuid
      ? { uuid: proposal.carrierUuid, name: proposal.carrierName }
      : null,
  )

  // Sincroniza o código com `initialCode` que chega assincronamente.
  useEffect(() => {
    if (proposal) return
    if (initialCode == null) return
    if (codeDirtyRef.current) return
    setCode(initialCode)
  }, [initialCode, proposal])

  // Pré-preenche o rótulo do cliente no modo edição.
  useEffect(() => {
    if (!proposal) return
    if (proposal.clientName) {
      setClientLabel(
        proposal.clientCode
          ? `${proposal.clientCode} — ${proposal.clientName}`
          : proposal.clientName,
      )
    } else if (proposal.customerUuid || proposal.companyUuid) {
      const uuid = proposal.customerUuid ?? proposal.companyUuid ?? ''
      setClientLabel(`${uuid.slice(0, 8)}…`)
    }
    setClientOptions([])
  }, [proposal])

  // Hidrata nomes de produtos no modo edição.
  useEffect(() => {
    if (!proposal) return
    let cancelled = false
    const uniqueUuids = Array.from(
      new Set(proposal.productItems.map((p) => p.productUuid)),
    )
    if (uniqueUuids.length === 0) return
    Promise.all(
      uniqueUuids.map(async (uuid) => {
        try {
          const p = await getProduct(uuid)
          return [uuid, p] as const
        } catch {
          return [uuid, null] as const
        }
      }),
    )
      .then((entries) => {
        if (cancelled) return
        setProductItems((prev) =>
          prev.map((it) => {
            const found = entries.find(([uuid]) => uuid === it.productUuid)
            const product = found?.[1]
            if (product) {
              return {
                ...it,
                productLabel: product.code
                  ? `${product.code} — ${product.name}`
                  : product.name,
                unitType: product.unitType,
              }
            }
            return {
              ...it,
              productLabel: it.productLabel || `${it.productUuid.slice(0, 8)}…`,
            }
          }),
        )
      })
      .catch(() => {
        /* mantém o que já está renderizado */
      })
    return () => {
      cancelled = true
    }
  }, [proposal])

  // Debounce do typeahead de clientes.
  const clientDebounce = useRef<ReturnType<typeof setTimeout> | null>(null)
  const clientTypeRef = useRef<TechnicalProposalClientType>(clientType)
  useEffect(() => {
    clientTypeRef.current = clientType
  }, [clientType])

  const handleClientQuery = useCallback((query: string) => {
    if (clientDebounce.current) clearTimeout(clientDebounce.current)
    const trimmed = query.trim()
    if (trimmed.length < 2) {
      setClientOptions([])
      return
    }
    clientDebounce.current = setTimeout(() => {
      setClientSearching(true)
      searchTechnicalProposalClients(trimmed, 20, clientTypeRef.current)
        .then((r) => setClientOptions(r))
        .catch(() => setClientOptions([]))
        .finally(() => setClientSearching(false))
    }, 300)
  }, [])

  // Typeahead de produtos por linha.
  const productDebounce = useRef<ReturnType<typeof setTimeout> | null>(null)
  const handleProductQuery = useCallback((rowKey: string, query: string) => {
    setActiveProductRow(rowKey)
    if (productDebounce.current) clearTimeout(productDebounce.current)
    const trimmed = query.trim()
    if (trimmed.length < 2) {
      setProductOptions([])
      return
    }
    productDebounce.current = setTimeout(() => {
      setProductSearching(true)
      searchProducts({ query: trimmed, status: 'ATIVO', page: 0, size: 20 })
        .then((p) => setProductOptions(p.content))
        .catch(() => setProductOptions([]))
        .finally(() => setProductSearching(false))
    }, 300)
  }, [])

  // === handlers de objetivos ===
  function addObjective() {
    setObjectives((prev) => [
      ...prev,
      { rowKey: nextRowKey(), description: '' },
    ])
  }
  function removeObjective(rowKey: string) {
    setObjectives((prev) => prev.filter((o) => o.rowKey !== rowKey))
  }
  function updateObjective(rowKey: string, patch: Partial<ObjectiveDraft>) {
    setObjectives((prev) =>
      prev.map((o) => (o.rowKey === rowKey ? { ...o, ...patch } : o)),
    )
  }

  // === handlers de itens de serviço ===
  function addServiceItem() {
    setServiceItems((prev) => [
      ...prev,
      { rowKey: nextRowKey(), description: '', price: '' },
    ])
  }
  function removeServiceItem(rowKey: string) {
    setServiceItems((prev) => prev.filter((s) => s.rowKey !== rowKey))
  }
  function updateServiceItem(rowKey: string, patch: Partial<ServiceDraft>) {
    setServiceItems((prev) =>
      prev.map((s) => (s.rowKey === rowKey ? { ...s, ...patch } : s)),
    )
  }

  // === handlers de itens de produto ===
  function addProductItem() {
    setProductItems((prev) => [
      ...prev,
      {
        rowKey: nextRowKey(),
        productUuid: '',
        productLabel: '',
        unitType: null,
        unitPrice: '',
        quantity: '1',
        discountType: null,
        discount: '',
      },
    ])
  }
  function removeProductItem(rowKey: string) {
    setProductItems((prev) => prev.filter((p) => p.rowKey !== rowKey))
  }
  function updateProductItem(rowKey: string, patch: Partial<ProductDraft>) {
    setProductItems((prev) =>
      prev.map((p) => (p.rowKey === rowKey ? { ...p, ...patch } : p)),
    )
  }
  function selectProduct(rowKey: string, p: ProductResponse) {
    updateProductItem(rowKey, {
      productUuid: p.uuid,
      productLabel: p.name,
      unitType: p.unitType,
      unitPrice: formatBRLValue(p.price),
    })
    setProductOptions([])
    setActiveProductRow(null)
  }

  // === simulação de totais (debounced, sem persistir) ===
  const [simulation, setSimulation] =
    useState<TechnicalProposalSimulateResponse | null>(null)

  useEffect(() => {
    let cancelled = false
    const handle = setTimeout(() => {
      const servicePayload = serviceItems
        .filter((s) => s.description.trim() !== '')
        .map((s) => ({
          description: s.description.trim(),
          price: parseNumber(s.price) ?? null,
        }))
      const productPayload = productItems
        .filter((p) => p.productUuid !== '')
        .map((p) => {
          const base: TechnicalProposalProductItemRequest = {
            productUuid: p.productUuid,
            quantity: parseNumber(p.quantity) ?? 0,
            unitPrice: parseNumber(p.unitPrice) ?? 0,
          }
          if (p.discountType != null) base.discountType = p.discountType
          if (p.discount.trim() !== '') base.discount = parseNumber(p.discount) ?? 0
          return base
        })
      const payload = {
        serviceItems: servicePayload.length > 0 ? servicePayload : null,
        productItems: productPayload.length > 0 ? productPayload : null,
        profitMargin: parseNumber(profitMargin) ?? 0,
        ...(discountType !== ''
          ? { discountType, discount: parseNumber(discount) ?? 0 }
          : {}),
        ...(parseNumber(freightValue) != null
          ? { freightValue: parseNumber(freightValue) }
          : {}),
        ...(deliveryType !== '' ? { deliveryType } : {}),
      }
      simulateTechnicalProposal(payload)
        .then((res) => {
          if (!cancelled) setSimulation(res)
        })
        .catch(() => {
          /* mantém o último preview válido */
        })
    }, 400)
    return () => {
      cancelled = true
      clearTimeout(handle)
    }
  }, [
    serviceItems,
    productItems,
    discountType,
    discount,
    freightValue,
    profitMargin,
    deliveryType,
  ])

  const servicesSubtotal = simulation?.servicesSubtotal ?? 0
  const productsSubtotal = simulation?.productsSubtotal ?? 0
  const subtotal = simulation?.subtotal ?? 0
  const globalDiscountValue = simulation?.globalDiscountValue ?? 0
  const freightValueNumber = parseNumber(freightValue) ?? 0
  const total = simulation?.total ?? 0

  // === validação e submit ===
  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!clientUuid) {
      errs.clientUuid = 'Selecione um cliente.'
    }
    // Validação de objetivos: ao menos um preenchido.
    const validObjectives = objectives.filter(
      (o) => o.description.trim() !== '',
    )
    if (validObjectives.length === 0) {
      errs.objectives = 'A proposta deve ter ao menos um objetivo.'
    } else {
      objectives.forEach((o, idx) => {
        if (o.description.trim() !== '' && o.description.length > 500) {
          errs[`objectives.${idx}`] =
            'Objetivo deve ter no máximo 500 caracteres.'
        }
      })
    }

    if (notes.length > 2000) {
      errs.notes = 'Observações devem ter no máximo 2000 caracteres.'
    }

    if (technicalResponsible.length > 150) {
      errs.technicalResponsible =
        'Responsável técnico deve ter no máximo 150 caracteres.'
    }
    if (responsibleEmail.length > 200) {
      errs.email = 'E-mail deve ter no máximo 200 caracteres.'
    }

    // Ao menos um item (serviço ou produto) preenchido.
    const validServices = serviceItems.filter(
      (s) => s.description.trim() !== '',
    )
    const validProducts = productItems.filter((p) => p.productUuid !== '')
    if (validServices.length === 0 && validProducts.length === 0) {
      errs.items = 'A proposta deve ter ao menos um serviço ou produto.'
    }

    // Validações de itens de produto.
    productItems.forEach((p, idx) => {
      if (!p.productUuid) {
        errs[`products.${idx}.product`] = 'Selecione um produto.'
      }
      const qty = parseNumber(p.quantity)
      if (qty == null || !(qty > 0)) {
        errs[`products.${idx}.quantity`] = 'Quantidade deve ser maior que zero.'
      }
      const price = parseNumber(p.unitPrice)
      if (price == null || price < 0) {
        errs[`products.${idx}.unitPrice`] =
          'Preço unitário não pode ser negativo.'
      }
      if (p.discount.trim() !== '' && p.discountType == null) {
        errs[`products.${idx}.discount`] =
          'Informe o tipo de desconto ou remova o valor.'
      }
    })

    if (discount.trim() !== '') {
      const d = parseNumber(discount)
      if (d == null || d < 0) {
        errs.discount = 'Desconto não pode ser negativo.'
      }
      if (discountType === '') {
        errs.discountType =
          'Informe o tipo de desconto ou deixe o valor em branco.'
      }
    }
    if (discountType !== '' && discount.trim() === '') {
      errs.discount = 'Informe o valor do desconto ou remova o tipo.'
    }

    if (freightValue.trim() !== '') {
      const f = parseNumber(freightValue)
      if (f == null || f < 0) {
        errs.freightValue = 'Valor do frete não pode ser negativo.'
      }
    }

    const margin = parseNumber(profitMargin)
    if (margin == null) {
      errs.profitMargin = 'Margem de lucro é obrigatória.'
    } else if (margin < 0) {
      errs.profitMargin = 'Margem de lucro não pode ser negativa.'
    }

    if (startDate.trim() === '') {
      errs.startDate = 'Data de início é obrigatória.'
    }

    if (deliveryDeadline.length > 50) {
      errs.deliveryDeadline =
        'Prazo de entrega deve ter no máximo 50 caracteres.'
    }
    if (validity.length > 50) {
      errs.validity = 'Validade deve ter no máximo 50 caracteres.'
    }

    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
    markAllTouched()
    if (!validateAll()) return

    const servicePayload: TechnicalProposalServiceItemRequest[] = serviceItems
      .filter((s) => s.description.trim() !== '')
      .map((s) => ({
        description: s.description.trim(),
        price: parseNumber(s.price) ?? null,
      }))

    const productPayload: TechnicalProposalProductItemRequest[] = productItems
      .filter((p) => p.productUuid !== '')
      .map((p) => {
        const base: TechnicalProposalProductItemRequest = {
          productUuid: p.productUuid,
          quantity: parseNumber(p.quantity) ?? 0,
          unitPrice: parseNumber(p.unitPrice) ?? 0,
        }
        if (p.discountType != null) base.discountType = p.discountType
        if (p.discount.trim() !== '') base.discount = parseNumber(p.discount) ?? 0
        return base
      })

    const objectivesPayload: TechnicalProposalObjectiveRequest[] = objectives
      .filter((o) => o.description.trim() !== '')
      .map((o) => ({ description: o.description.trim() }))

    const addressPayload: TechnicalProposalAddressRequest | null = hasAddress
      ? {
          street: address.street?.trim() || null,
          number: address.number?.trim() || null,
          complement: address.complement?.trim() || null,
          neighborhood: address.neighborhood?.trim() || null,
          city: address.city?.trim() || null,
          state: address.state?.trim() || null,
          zipCode: address.zipCode?.trim() || null,
        }
      : null

    try {
      if (isEdit) {
        const payload: TechnicalProposalUpdateRequest = {
          objectives: objectivesPayload,
          description: description.trim() ? description.trim() : null,
          technicalResponsible: technicalResponsible.trim()
            ? technicalResponsible.trim()
            : null,
          email: responsibleEmail.trim() ? responsibleEmail.trim() : null,
          startDate,
          endDate: endDate.trim() ? endDate : null,
          serviceItems: servicePayload.length > 0 ? servicePayload : null,
          productItems: productPayload.length > 0 ? productPayload : null,
          address: addressPayload,
          profitMargin: parseNumber(profitMargin) ?? 0,
        }
        if (clientType === 'CUSTOMER') {
          payload.customerUuid = clientUuid
          payload.companyUuid = null
        } else {
          payload.companyUuid = clientUuid
          payload.customerUuid = null
        }
        if (discountType !== '') {
          payload.discountType = discountType
          payload.discount = parseNumber(discount) ?? 0
        } else {
          payload.discountType = null
          payload.discount = null
        }
        payload.paymentCondition = paymentCondition === '' ? null : paymentCondition
        payload.notes = notes.trim() ? notes.trim() : null
        payload.freightValue = parseNumber(freightValue)
        payload.deliveryType = deliveryType === '' ? null : deliveryType
        payload.deliveryDeadline = deliveryDeadline.trim() || null
        payload.validity = validity.trim() || null
        payload.carrierUuid = carrierUuid || null

        await onSaveUpdate(payload)
        setSuccess('Proposta atualizada com sucesso!')
        reset()
      } else {
        const payload: TechnicalProposalCreateRequest = {
          objectives: objectivesPayload,
          profitMargin: parseNumber(profitMargin) ?? 0,
        }
        if (clientType === 'CUSTOMER') {
          payload.customerUuid = clientUuid
        } else {
          payload.companyUuid = clientUuid
        }
        if (description.trim()) payload.description = description.trim()
        if (technicalResponsible.trim())
          payload.technicalResponsible = technicalResponsible.trim()
        if (responsibleEmail.trim()) payload.email = responsibleEmail.trim()
        if (startDate.trim()) payload.startDate = startDate
        if (endDate.trim()) payload.endDate = endDate
        if (servicePayload.length > 0) payload.serviceItems = servicePayload
        if (productPayload.length > 0) payload.productItems = productPayload
        if (hasAddress && addressPayload) payload.address = addressPayload
        if (discountType !== '') {
          payload.discountType = discountType
          payload.discount = parseNumber(discount) ?? 0
        }
        if (paymentCondition !== '') payload.paymentCondition = paymentCondition
        if (notes.trim()) payload.notes = notes.trim()
        if (freightValue.trim()) payload.freightValue = parseNumber(freightValue)
        if (deliveryType !== '') payload.deliveryType = deliveryType
        if (deliveryDeadline.trim()) payload.deliveryDeadline = deliveryDeadline.trim()
        if (validity.trim()) payload.validity = validity.trim()
        payload.carrierUuid = carrierUuid || null

        await onSaveCreate(payload)
        setSuccess('Proposta criada com sucesso!')
        reset()
      }
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) setFieldErrors(apiErr.fieldErrors)
    }
  }

  const clientTypeOptions = [
    { value: 'CUSTOMER', label: TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS.CUSTOMER },
    { value: 'COMPANY', label: TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS.COMPANY },
  ]

  return (
    <form
      id="technical-proposal-form"
      onSubmit={handleSubmit}
      className="flex flex-col gap-6"
      noValidate
    >
      {formError ? <Alert variant="error">{formError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}

      {/* Cabeçalho — cliente + código + objetivo */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Cliente e objetivo</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Selecione o tipo de cliente (PF ou PJ), busque pelo nome ou código,
          informe o objetivo e a data de início do serviço.
        </p>

        <div className="grid gap-4 sm:grid-cols-[180px_180px_1fr]">
          <Input
            label="Código da proposta"
            value={code}
            onChange={(e) => {
              codeDirtyRef.current = true
              setCode(e.target.value)
            }}
            readOnly
            disabled
            className="max-w-[200px]"
          />
          <Select
            label="Tipo de cliente"
            value={clientType}
            onChange={(e) => {
              setClientType(e.target.value as TechnicalProposalClientType)
              setClientUuid('')
              setClientLabel('')
              setClientOptions([])
            }}
            options={clientTypeOptions}
            required
            aria-label="Tipo de cliente"
          />
          <div className="flex flex-col">
            <label
              htmlFor="tp-client-search"
              className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
            >
              Cliente
              <span className="ml-0.5 text-red-500">*</span>
            </label>
            <div className="relative">
              <Input
                id="tp-client-search"
                placeholder={
                  clientType === 'CUSTOMER'
                    ? 'Buscar cliente (PF) por nome ou código…'
                    : 'Buscar empresa (PJ) por nome ou código…'
                }
                value={clientLabel}
                onChange={(e) => {
                  setClientLabel(e.target.value)
                  setClientUuid('')
                  handleClientQuery(e.target.value)
                }}
                onBlur={getBlurHandler('clientUuid')}
                hint={
                  clientLabel.trim().length > 0 && clientLabel.trim().length < 2
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
                        setClientLabel(
                          `${c.code ? `${c.code} — ` : ''}${c.name}${c.document ? ` (${c.document})` : ''}`,
                        )
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
                          {c.code}
                          {c.document ? ` • ${c.document}` : ''}
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
        </div>

        {/* Responsável técnico + e-mail (opcionais) */}
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <Input
            label="Responsável técnico"
            value={technicalResponsible}
            onChange={(e) => setTechnicalResponsible(e.target.value)}
            onBlur={getBlurHandler('technicalResponsible')}
            error={shouldShowError(
              'technicalResponsible',
              fieldErrors.technicalResponsible,
            )}
            maxLength={150}
            placeholder="Ex.: João da Silva"
          />
          <Input
            label="E-mail do responsável"
            value={responsibleEmail}
            onChange={(e) => setResponsibleEmail(e.target.value)}
            onBlur={getBlurHandler('email')}
            error={shouldShowError('email', fieldErrors.email)}
            maxLength={200}
            placeholder="Ex.: joao.silva@empresa.com"
          />
        </div>

        {/* Objetivos (lista dinâmica) */}
        <div className="mt-4">
          <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              Objetivos
              <span className="ml-0.5 text-red-500">*</span>
            </label>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={addObjective}
            >
              <Plus className="h-4 w-4" />
              Adicionar objetivo
            </Button>
          </div>
          <p className="mb-2 text-xs text-slate-500 dark:text-slate-400">
            Liste os objetivos do serviço prestado. Ao menos um é obrigatório.
          </p>

          {fieldErrors.objectives ? (
            <p className="mb-2 text-sm text-red-600 dark:text-red-400">
              {fieldErrors.objectives}
            </p>
          ) : null}

          <div className="flex flex-col gap-2">
            {objectives.map((o, idx) => (
              <ObjectiveRow
                key={o.rowKey}
                index={idx}
                isFirst={idx === 0}
                draft={o}
                error={fieldErrors[`objectives.${idx}`]}
                onChange={(patch) => updateObjective(o.rowKey, patch)}
                onRemove={() => removeObjective(o.rowKey)}
                canRemove={objectives.length > 1}
              />
            ))}
          </div>
        </div>

        {/* Datas + margem de lucro na mesma linha */}
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <Input
            label="Data de início"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            onBlur={getBlurHandler('startDate')}
            error={shouldShowError('startDate', fieldErrors.startDate)}
            required
          />
          <Input
            label="Data de término"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            onBlur={getBlurHandler('endDate')}
            error={shouldShowError('endDate', fieldErrors.endDate)}
            hint="Opcional — prevista/real do serviço."
          />
          <Input
            label="Margem de lucro (%)"
            type="number"
            inputMode="decimal"
            step="0.01"
            min={0}
            placeholder="0,00"
            aria-label="Margem de lucro (%)"
            value={profitMargin}
            onChange={(e) => setProfitMargin(e.target.value)}
            onBlur={getBlurHandler('profitMargin')}
            error={shouldShowError('profitMargin', fieldErrors.profitMargin)}
            rightAdornment={<span aria-hidden>%</span>}
            required
          />
        </div>

        <div className="mt-4">
          <label
            htmlFor="tp-description"
            className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
          >
            Descrição detalhada
          </label>
          <RichTextEditor
            id="tp-description"
            value={description}
            onChange={setDescription}
            onBlur={getBlurHandler('description')}
            placeholder="Formalização do serviço prestado — escopo, etapas, condições…"
          />
        </div>
      </section>

      {/* Endereço (opcional) */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex items-center justify-between">
          <h3 className="text-base font-semibold">Endereço de execução</h3>
          <label className="inline-flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
            <input
              type="checkbox"
              checked={hasAddress}
              onChange={(e) => setHasAddress(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-focus/30"
            />
            Informar endereço
          </label>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Local onde o serviço será executado. Opcional.
        </p>
        {hasAddress ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Input
              label="Logradouro"
              value={address.street ?? ''}
              onChange={(e) =>
                setAddress((a) => ({ ...a, street: e.target.value }))
              }
              maxLength={200}
            />
            <Input
              label="Número"
              value={address.number ?? ''}
              onChange={(e) =>
                setAddress((a) => ({ ...a, number: e.target.value }))
              }
              maxLength={20}
            />
            <Input
              label="Complemento"
              value={address.complement ?? ''}
              onChange={(e) =>
                setAddress((a) => ({ ...a, complement: e.target.value }))
              }
              maxLength={100}
            />
            <Input
              label="Bairro"
              value={address.neighborhood ?? ''}
              onChange={(e) =>
                setAddress((a) => ({ ...a, neighborhood: e.target.value }))
              }
              maxLength={100}
            />
            <Input
              label="Cidade"
              value={address.city ?? ''}
              onChange={(e) =>
                setAddress((a) => ({ ...a, city: e.target.value }))
              }
              maxLength={100}
            />
            <Select
              label="UF"
              value={address.state ?? ''}
              onChange={(e) =>
                setAddress((a) => ({ ...a, state: e.target.value }))
              }
              options={[{ value: '', label: 'Selecione…' }, ...UF_OPTIONS]}
              aria-label="UF"
            />
            <Input
              label="CEP"
              value={address.zipCode ?? ''}
              onChange={(e) =>
                setAddress((a) => ({
                  ...a,
                  zipCode: maskZipCode(e.target.value),
                }))
              }
              maxLength={9}
              placeholder="00000-000"
            />
          </div>
        ) : null}
      </section>

      {/* Itens de serviço */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Serviços prestados</h3>
          <Button type="button" variant="secondary" onClick={addServiceItem}>
            <Plus className="h-4 w-4" />
            Adicionar serviço
          </Button>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Liste os serviços prestados com descrição e preço (opcional).
        </p>

        {serviceItems.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhum serviço. Clique em <strong>Adicionar serviço</strong> para
            começar.
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {serviceItems.map((s, idx) => (
              <ServiceRow
                key={s.rowKey}
                index={idx}
                isFirst={idx === 0}
                draft={s}
                onChange={(patch) => updateServiceItem(s.rowKey, patch)}
                onRemove={() => removeServiceItem(s.rowKey)}
              />
            ))}
          </div>
        )}
      </section>

      {/* Itens de produto */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Produtos</h3>
          <Button
            type="button"
            variant="primary"
            onClick={addProductItem}
          >
            <Plus className="h-4 w-4" />
            Adicionar produto
          </Button>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Liste os produtos da proposta. Cada linha é um item com quantidade,
          preço unitário e desconto opcional.
        </p>

        {fieldErrors.items ? (
          <Alert variant="error">{fieldErrors.items}</Alert>
        ) : null}

        {productItems.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhum produto. Clique em <strong>Adicionar produto</strong> para
            começar.
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {productItems.map((p, idx) => (
              <ProductRow
                key={p.rowKey}
                index={idx}
                isFirst={idx === 0}
                draft={p}
                lineTotal={
                  simulation?.productItems?.[idx]?.totalPrice ??
                  (parseNumber(p.unitPrice) ?? 0) * (parseNumber(p.quantity) ?? 0)
                }
                // Preço unitário já com margem aplicado — vem do backend
                // via simulate. Quando a simulação ainda não chegou, usa
                // o preço original do draft como fallback visual.
                unitPriceWithMargin={
                  simulation?.productItems?.[idx]?.unitPrice ?? null
                }
                productOptions={productOptions}
                productSearching={
                  productSearching && activeProductRow === p.rowKey
                }
                onQuery={(q) => handleProductQuery(p.rowKey, q)}
                onSelect={(prod) => selectProduct(p.rowKey, prod)}
                onPatch={(patch) => updateProductItem(p.rowKey, patch)}
                onRemove={() => removeProductItem(p.rowKey)}
              />
            ))}
          </div>
        )}
      </section>

      {/* Condições comerciais */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Condições</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Desconto global, frete, prazo de entrega, condição de pagamento,
          validade e observações.
        </p>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Select
            label="Tipo de desconto global"
            value={discountType}
            onChange={(e) =>
              setDiscountType(e.target.value as DiscountType | '')
            }
            options={[{ value: '', label: 'Sem desconto' }, ...DISCOUNT_TYPE_OPTIONS]}
            aria-label="Tipo de desconto"
          />
          <Input
            label={
              discountType === 'PERCENT'
                ? 'Desconto global (%)'
                : discountType === 'AMOUNT'
                  ? 'Desconto global (R$)'
                  : 'Desconto global'
            }
            type="text"
            inputMode="decimal"
            placeholder="0,00"
            value={discount}
            onChange={(e) => setDiscount(e.target.value)}
            onBlur={() => {
              if (discount.trim() !== '') {
                const formatted = formatBRLValue(discount)
                if (formatted) setDiscount(formatted)
              }
              getBlurHandler('discount')()
            }}
            disabled={discountType === ''}
            hint={
              discountType === 'PERCENT'
                ? 'Percentual aplicado sobre o subtotal com margem.'
                : discountType === 'AMOUNT'
                  ? 'Valor fixo em reais.'
                  : 'Defina o tipo de desconto primeiro.'
            }
          />
          <Input
            label="Prazo de entrega"
            value={deliveryDeadline}
            onChange={(e) => setDeliveryDeadline(e.target.value)}
            placeholder="Ex.: 3 dias"
            maxLength={50}
          />
          <Input
            label="Validade da proposta"
            value={validity}
            onChange={(e) => setValidity(e.target.value)}
            placeholder="Ex.: 10 dias"
            maxLength={50}
          />
          <Select
            label="Condição de pagamento"
            value={paymentCondition}
            onChange={(e) =>
              setPaymentCondition(e.target.value as PaymentCondition | '')
            }
            options={[{ value: '', label: 'Selecione…' }, ...PAYMENT_CONDITION_OPTIONS]}
            aria-label="Condição de pagamento"
          />
          <div className="sm:col-span-2 lg:col-span-3">
            <FreightConditionsFields
              freightType={deliveryType}
              onFreightTypeChange={(v) => setDeliveryType(v as FreightType | '')}
              freightValue={freightValue}
              onFreightValueChange={setFreightValue}
              onFreightValueBlur={() => {
                if (freightValue.trim() !== '') {
                  const formatted = formatBRLValue(freightValue)
                  if (formatted) setFreightValue(formatted)
                }
                getBlurHandler('freightValue')()
              }}
              freightValueError={shouldShowError(
                'freightValue',
                fieldErrors.freightValue,
              )}
              carrierUuid={carrierUuid}
              onCarrierUuidChange={setCarrierUuid}
              carriers={carriers}
              carriersLoading={carriersLoading}
              freightTypeLabel="Tipo de entrega"
              freightTypeHint="CIF = remetente; FOB = destinatário."
            />
          </div>
        </div>

        <div className="mt-4">
          <label
            htmlFor="tp-notes"
            className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
          >
            Observações
          </label>
          <RichTextEditor
            id="tp-notes"
            value={notes}
            onChange={setNotes}
            onBlur={getBlurHandler('notes')}
            maxLength={2000}
            placeholder="Instruções de execução, garantias, condições adicionais…"
          />
          {shouldShowError('notes', fieldErrors.notes) ? (
            <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
              {fieldErrors.notes}
            </p>
          ) : (
            <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">
              {notes.length}/2000 caracteres (inclui formatação).
            </p>
          )}
        </div>
      </section>

      {/* Totais */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-4 text-base font-semibold">Totais</h3>
        <dl className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-5">
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Serviços
            </dt>
            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {brlFormatter.format(servicesSubtotal)}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Produtos
            </dt>
            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {brlFormatter.format(productsSubtotal)}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Subtotal
            </dt>
            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {brlFormatter.format(subtotal)}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Frete
            </dt>
            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {brlFormatter.format(freightValueNumber)}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Total
            </dt>
            <dd className="mt-1 text-lg font-semibold text-primary-700 dark:text-primary-200">
              {brlFormatter.format(total)}
            </dd>
          </div>
        </dl>
        {globalDiscountValue > 0 ? (
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            Desconto global aplicado (após margem de lucro):{' '}
            {brlFormatter.format(globalDiscountValue)}
            {freightValueNumber > 0
              ? ' • Frete somado ao total (sem margem nem desconto).'
              : ''}
          </p>
        ) : freightValueNumber > 0 ? (
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            Frete somado ao total (sem margem nem desconto).
          </p>
        ) : null}
      </section>
    </form>
  )
}

// =====================================================================
// Subcomponente: linha de objetivo
// =====================================================================

interface ObjectiveRowProps {
  index: number
  isFirst: boolean
  draft: ObjectiveDraft
  error?: string
  onChange: (patch: Partial<ObjectiveDraft>) => void
  onRemove: () => void
  canRemove: boolean
}

function ObjectiveRow({
  index,
  draft,
  error,
  onChange,
  onRemove,
  canRemove,
}: ObjectiveRowProps) {
  return (
    <div className="flex items-start gap-2 rounded-md border border-slate-200 p-2 dark:border-slate-700">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-slate-100 font-mono text-xs font-semibold text-slate-500 dark:bg-slate-800 dark:text-slate-400">
        {String(index + 1).padStart(2, '0')}
      </div>
      <div className="min-w-0 flex-1">
        <input
          type="text"
          aria-label={`Objetivo ${index + 1}`}
          placeholder="Ex.: Substituição de quadro elétrico."
          value={draft.description}
          onChange={(e) => onChange({ description: e.target.value })}
          maxLength={500}
          className={[
            'h-9 w-full min-w-0 rounded-md border bg-white px-2 text-sm text-slate-900 outline-none',
            'placeholder:text-slate-400 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500',
            'focus:border-focus focus:ring-1 focus:ring-focus/30 transition-colors duration-200',
            error
              ? 'border-red-500 focus:border-red-500 focus:ring-red-500/30'
              : 'border-slate-300 dark:border-slate-700',
          ].join(' ')}
        />
        {error ? (
          <p className="mt-1 text-xs text-red-600 dark:text-red-400">{error}</p>
        ) : null}
      </div>
      <button
        type="button"
        onClick={onRemove}
        disabled={!canRemove}
        aria-label="Remover objetivo"
        title="Remover objetivo"
        className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md text-slate-500 transition-colors hover:bg-slate-100 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-red-400"
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </div>
  )
}

// =====================================================================
// Subcomponente: linha de serviço
// =====================================================================

interface ServiceRowProps {
  index: number
  isFirst: boolean
  draft: ServiceDraft
  onChange: (patch: Partial<ServiceDraft>) => void
  onRemove: () => void
}

function ServiceRow({ index, isFirst, draft, onChange, onRemove }: ServiceRowProps) {
  const labelCls = 'mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-300'
  const [priceDisplay, setPriceDisplay] = useState<string>(draft.price)

  useEffect(() => {
    setPriceDisplay(draft.price)
  }, [draft.price])

  return (
    <div className="grid items-start gap-2 rounded-md border border-slate-200 p-2 sm:grid-cols-[40px_1fr_140px_40px] dark:border-slate-700">
      <div className="flex h-full items-center justify-center font-mono text-xs font-semibold text-slate-500 dark:text-slate-400">
        {String(index + 1).padStart(2, '0')}
      </div>
      <div>
        {isFirst ? <label className={labelCls}>Descrição</label> : null}
        <input
          type="text"
          aria-label="Descrição do serviço"
          placeholder="Descreva o serviço prestado…"
          value={draft.description}
          onChange={(e) => onChange({ description: e.target.value })}
          className="h-9 w-full min-w-0 rounded-md border border-slate-300 bg-white px-2 text-sm text-slate-900 outline-none focus:border-focus focus:ring-1 focus:ring-focus/30 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
        />
      </div>
      <div>
        {isFirst ? <label className={labelCls}>Preço (R$)</label> : null}
        <input
          type="text"
          inputMode="decimal"
          aria-label="Preço do serviço"
          placeholder="0,00"
          value={priceDisplay}
          onChange={(e) => {
            setPriceDisplay(e.target.value)
            onChange({ price: e.target.value })
          }}
          onBlur={() => {
            const formatted = formatBRLValue(priceDisplay)
            setPriceDisplay(formatted)
            onChange({ price: formatted })
          }}
          className="h-9 w-full min-w-0 rounded-md border border-slate-300 bg-white px-2 text-right text-sm text-slate-900 outline-none focus:border-focus focus:ring-1 focus:ring-focus/30 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
        />
      </div>
      <div className="flex h-full items-center justify-center">
        <button
          type="button"
          onClick={onRemove}
          aria-label="Remover serviço"
          title="Remover serviço"
          className="inline-flex h-8 w-8 items-center justify-center rounded-md text-slate-500 transition-colors hover:bg-slate-100 hover:text-red-600 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-red-400"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}

// =====================================================================
// Subcomponente: linha de produto
// =====================================================================

interface ProductRowProps {
  index: number
  isFirst: boolean
  draft: ProductDraft
  lineTotal: number
  /**
   * Preço unitário já com a margem de lucro aplicada, retornado pelo
   * backend em `simulation.productItems[i].unitPrice`. Quando `null`,
   * ainda não há simulação disponível e o componente exibe fallback.
   */
  unitPriceWithMargin: number | null
  productOptions: ProductResponse[]
  productSearching: boolean
  onQuery: (q: string) => void
  onSelect: (p: ProductResponse) => void
  onPatch: (patch: Partial<ProductDraft>) => void
  onRemove: () => void
}

function ProductRow({
  index,
  isFirst,
  draft,
  lineTotal,
  unitPriceWithMargin,
  productOptions,
  productSearching,
  onQuery,
  onSelect,
  onPatch,
  onRemove,
}: ProductRowProps) {
  const labelCls = 'mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-300'
  const inputBase =
    'h-9 w-full min-w-0 rounded-md border bg-white px-2 text-sm text-slate-900 outline-none ' +
    'placeholder:text-slate-400 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500 ' +
    'border-slate-300 focus:border-focus focus:ring-1 focus:ring-focus/30 dark:border-slate-700 ' +
    'transition-colors duration-200'

  const [unitPriceDisplay, setUnitPriceDisplay] = useState<string>(draft.unitPrice)
  const [discountDisplay, setDiscountDisplay] = useState<string>(draft.discount)

  useEffect(() => {
    setUnitPriceDisplay(draft.unitPrice)
  }, [draft.unitPrice])
  useEffect(() => {
    setDiscountDisplay(draft.discount)
  }, [draft.discount])

  return (
    <div className="grid items-start gap-2 px-3 py-2 sm:grid-cols-[40px_1fr_88px_96px_96px_120px_40px]">
      <div className="flex h-full items-center justify-center font-mono text-xs font-semibold text-slate-500 dark:text-slate-400">
        {String(index + 1).padStart(2, '0')}
      </div>
      <div className="relative min-w-0">
        {isFirst ? <label className={labelCls}>Produto</label> : null}
        <input
          type="text"
          aria-label="Produto"
          placeholder="Buscar produto por nome ou código…"
          value={draft.productLabel}
          onChange={(e) => {
            onPatch({ productLabel: e.target.value, productUuid: '' })
            onQuery(e.target.value)
          }}
          className={inputBase}
          required
        />
        {productSearching ? (
          <span className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 text-slate-400">
            <Spinner size="sm" />
          </span>
        ) : null}
        {productOptions.length > 0 && !draft.productUuid ? (
          <ul className="absolute left-0 right-0 top-full z-20 mt-1 max-h-56 overflow-y-auto rounded-lg border border-slate-200 bg-white text-sm shadow-lg dark:border-slate-700 dark:bg-slate-900">
            {productOptions.map((p) => (
              <li key={p.uuid}>
                <button
                  type="button"
                  className="flex w-full items-start gap-2 px-3 py-2 text-left hover:bg-slate-100 dark:hover:bg-slate-800"
                  onClick={() => onSelect(p)}
                >
                  <span className="flex-1">
                    <span className="block font-medium text-slate-900 dark:text-slate-100">
                      {p.name}
                    </span>
                    <span className="block text-xs text-slate-500 dark:text-slate-400">
                      {p.code ? `${p.code} • ` : ''}
                      {brlFormatter.format(p.price)}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        ) : null}
      </div>
      <div className="min-w-0">
        {isFirst ? <label className={labelCls}>Qtde</label> : null}
        <input
          type="number"
          inputMode="decimal"
          step="0.0001"
          min={0.0001}
          aria-label="Quantidade"
          placeholder="Qtde"
          value={draft.quantity}
          onChange={(e) => onPatch({ quantity: e.target.value })}
          className={[inputBase, 'text-right'].join(' ')}
          required
        />
      </div>
      <div className="min-w-0">
        {isFirst ? <label className={labelCls}>Preço</label> : null}
        <input
          type="text"
          inputMode="decimal"
          aria-label="Preço unitário (com margem de lucro aplicada)"
          placeholder="0,00"
          value={
            unitPriceWithMargin != null
              ? formatBRLValue(unitPriceWithMargin)
              : unitPriceDisplay
          }
          // Readonly: o preço unitário exibido reflete a margem de lucro
          // aplicada pelo backend. A margem é controlada pelo campo
          // global "Margem de lucro (%)" e o preço base continua sendo
          // o valor original armazenado em `draft.unitPrice`.
          readOnly
          className={[
            inputBase,
            'text-right',
            'bg-slate-50 dark:bg-slate-800 cursor-not-allowed',
          ].join(' ')}
        />
        {unitPriceWithMargin != null &&
        parseNumber(draft.unitPrice) !== unitPriceWithMargin ? (
          <p className="mt-0.5 text-[10px] text-slate-500 dark:text-slate-400">
            Base {brlFormatter.format(parseNumber(draft.unitPrice) ?? 0)} + margem
          </p>
        ) : null}
      </div>
      <div className="min-w-0">
        {isFirst ? <label className={labelCls}>Desconto</label> : null}
        <input
          type="text"
          inputMode="decimal"
          aria-label="Desconto"
          placeholder="0,00"
          value={discountDisplay}
          onChange={(e) => {
            setDiscountDisplay(e.target.value)
            onPatch({
              discount: e.target.value,
              discountType: e.target.value.trim() !== '' ? 'AMOUNT' : null,
            })
          }}
          onBlur={() => {
            if (discountDisplay.trim() === '') {
              onPatch({ discount: '', discountType: null })
            } else {
              const formatted = formatBRLValue(discountDisplay)
              setDiscountDisplay(formatted)
              onPatch({ discount: formatted, discountType: 'AMOUNT' })
            }
          }}
          className={[inputBase, 'text-right'].join(' ')}
        />
      </div>
      <div className="min-w-0">
        {isFirst ? <label className={labelCls}>Total</label> : null}
        <div
          className="flex h-9 min-w-0 items-center justify-end truncate rounded-md border border-slate-200 bg-slate-50 px-2 text-right text-sm font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
          title={brlFormatter.format(lineTotal)}
        >
          {brlFormatter.format(lineTotal)}
        </div>
      </div>
      <div className="flex h-full items-center justify-center">
        <button
          type="button"
          onClick={onRemove}
          aria-label="Remover item"
          title="Remover item"
          className="inline-flex h-8 w-8 items-center justify-center rounded-md text-slate-500 transition-colors hover:bg-slate-100 hover:text-red-600 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-red-400"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}