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
import { listSellers } from '../../api/seller.api'
import { searchQuotationClients } from '../../api/quotation.api'
import type { ProductResponse, UnitType } from '../../types/product'
import type { SellerResponse } from '../../types/seller'
import type {
  DiscountType,
  FreightType,
  PaymentCondition,
} from '../../types/quotation'
import type {
  ClientSummaryResponse,
  QuotationClientType,
} from '../../types/quotation'
import type {
  SalesOrderClientType,
  SalesOrderCreateRequest,
  SalesOrderItemRequest,
  SalesOrderResponse,
  SalesOrderUpdateRequest,
} from '../../types/salesOrder'
import {
  calculateItemTotalPrice,
  calculateSalesOrderTotals,
} from '../../types/salesOrder'
import {
  DISCOUNT_TYPE_OPTIONS,
  PAYMENT_CONDITION_OPTIONS,
} from '../../types/quotation'
import { QUOTATION_CLIENT_TYPE_LABELS } from '../../types/quotation'

interface SalesOrderFormProps {
  /** Pedido existente (modo edição). Quando omitido, é cadastro novo. */
  salesOrder?: SalesOrderResponse
  /** Próximo número previsto pelo backend (modo create). */
  initialNumber?: number | null
  onSaveCreate: (payload: SalesOrderCreateRequest) => Promise<void>
  onSaveUpdate: (payload: SalesOrderUpdateRequest) => Promise<void>
}

/** Linha do editor de itens (estado local, antes de virar SalesOrderItemRequest). */
interface ItemDraft {
  /** Chave local para controle de lista. Diferente do uuid do backend. */
  rowKey: string
  productUuid: string
  productLabel: string
  /** Unidade de medida do produto (UN/MT/BOB) — definida ao selecionar o produto. */
  unitType: UnitType | null
  unitPrice: number
  quantity: number
  discountType: DiscountType | null
  discount: number | null
}

/**
 * Teto absoluto para valores unitários e totais do pedido (9 bilhões).
 * Evita overflow numérico no cálculo de `qty * unitPrice - discount`.
 */
const MAX_ITEM_VALUE = 9_000_000_000

/** Formatador de moeda BRL compartilhado. */
const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

/** Gera uma chave local única para cada linha do editor. */
function nextRowKey(): string {
  return `row_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
}

/** Retorna a data atual no formato ISO `YYYY-MM-DD` aceito por `<input type="date">`. */
function todayIso(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

export function SalesOrderForm({
  salesOrder,
  initialNumber = null,
  onSaveCreate,
  onSaveUpdate,
}: SalesOrderFormProps) {
  const isEdit = !!salesOrder

  // === cabeçalho ===
  const [clientType, setClientType] = useState<SalesOrderClientType>(
    salesOrder?.clientType ?? 'CUSTOMER',
  )
  const [clientUuid, setClientUuid] = useState<string>(
    salesOrder?.customerUuid ?? salesOrder?.companyUuid ?? '',
  )
  const [clientLabel, setClientLabel] = useState<string>('')
  const [attention, setAttention] = useState<string>(salesOrder?.attention ?? '')
  const [sellerUuid, setSellerUuid] = useState<string>(
    salesOrder?.sellerUuid ?? '',
  )
  const [paymentCondition, setPaymentCondition] = useState<PaymentCondition | ''>(
    salesOrder?.paymentCondition ?? '',
  )
  // Tipo de frete (CIF/FOB).
  const [freightType, setFreightType] = useState<FreightType | ''>(
    salesOrder?.freightType ?? '',
  )
  // Valor do frete (manual).
  const [freightValue, setFreightValue] = useState<string>(
    salesOrder?.freightValue != null ? formatBRLValue(salesOrder.freightValue) : '',
  )
  // Transportadora (Carrier) responsável pelo frete. Opcional.
  const [carrierUuid, setCarrierUuid] = useState<string>(
    salesOrder?.carrierUuid ?? '',
  )
  const [notes, setNotes] = useState<string>(salesOrder?.notes ?? '')
  const [discountType, setDiscountType] = useState<DiscountType | ''>(
    salesOrder?.discountType ?? '',
  )
  const [discount, setDiscount] = useState<string>(
    salesOrder?.discount != null ? String(salesOrder.discount) : '',
  )

  // === campos gerados pelo servidor ===
  // O número vem do backend (a partir de 1000). Pré-preenchido em create
  // com `initialNumber` (next-number) e em edit com `salesOrder.number`.
  // Data de emissão: preenchida automaticamente pelo servidor; exibida em
  // create como hoje e em edit como o valor persistido. Ambos bloqueados
  // (o backend não aceita esses campos no payload de pedido).
  const [number, setNumber] = useState<string>(() => {
    if (salesOrder?.number != null) return String(salesOrder.number)
    if (initialNumber != null) return String(initialNumber)
    return ''
  })
  // Marca se o usuário editou manualmente o número. Usado para evitar
  // sobrescrever a digitação quando o `initialNumber` chega
  // assincronamente após a primeira renderização do formulário.
  const numberDirtyRef = useRef<boolean>(false)
  const [orderDate, setOrderDate] = useState<string>(
    salesOrder?.orderDate ?? todayIso(),
  )

  // === itens ===
  // Em modo create já iniciamos com uma linha vazia para o primeiro item.
  // Em modo edit, carregamos os itens persistidos.
  const [items, setItems] = useState<ItemDraft[]>(() => {
    if (salesOrder?.items && salesOrder.items.length > 0) {
      return salesOrder.items.map((it) => ({
        rowKey: nextRowKey(),
        productUuid: it.productUuid,
        // Rótulo inicial vazio — hidratado com o nome real via `getProduct`
        // no efeito abaixo. Exibir o UUID aqui faria o campo mostrá-lo até
        // a resolução (ou para sempre, se a busca falhar).
        productLabel: '',
        // Unidade de medida (UN/MT/BOB) — também hidratada via `getProduct`.
        unitType: null,
        unitPrice: it.unitPrice,
        quantity: it.quantity,
        discountType: it.discountType ?? null,
        discount: it.discount ?? null,
      }))
    }
    if (!salesOrder) {
      return [
        {
          rowKey: nextRowKey(),
          productUuid: '',
          productLabel: '',
          unitType: null,
          unitPrice: 0,
          quantity: 1,
          discountType: null,
          discount: null,
        },
      ]
    }
    return []
  })

  // === coleções auxiliares ===
  const [sellers, setSellers] = useState<SellerResponse[]>([])
  const [sellersLoading, setSellersLoading] = useState(false)
  const [clientOptions, setClientOptions] = useState<ClientSummaryResponse[]>([])
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
    salesOrder?.carrierUuid
      ? { uuid: salesOrder.carrierUuid, name: salesOrder.carrierName }
      : null,
  )

  // Carrega lista de vendedores ativos uma vez.
  useEffect(() => {
    let cancelled = false
    setSellersLoading(true)
    listSellers({ status: 'ATIVO', size: 100, page: 0 })
      .then((p) => {
        if (cancelled) return
        setSellers(p.content)
      })
      .catch(() => {
        if (cancelled) return
        setSellers([])
      })
      .finally(() => {
        if (!cancelled) setSellersLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])


  // Sincroniza o número do pedido com o `initialNumber` que chega
  // assincronamente do endpoint `/sales-orders/next-number`. Em modo
  // create, o componente monta antes do fetch resolver, então precisamos
  // refletir o valor assim que ele chega — sem sobrescrever a digitação.
  useEffect(() => {
    if (salesOrder) return
    if (initialNumber == null) return
    if (numberDirtyRef.current) return
    setNumber(String(initialNumber))
  }, [initialNumber, salesOrder])

  // Pré-preenche o rótulo do cliente no modo edição.
  //
  // O `SalesOrderResponse` já traz o nome e o código do cliente resolvidos
  // no backend (`clientName`/`clientCode`), como acontece com `sellerName`.
  // Basta montar o rótulo `${code} — ${name}` espelhando o formato do
  // typeahead. Se o cliente foi inativado/removido, ambos chegam nulos e
  // mantemos o UUID curto como fallback visual (mesmo critério do
  // `SalesOrderDetailPage`).
  useEffect(() => {
    if (!salesOrder) return
    const targetUuid =
      salesOrder.clientType === 'CUSTOMER'
        ? salesOrder.customerUuid
        : salesOrder.companyUuid
    if (!targetUuid) return

    if (salesOrder.clientName) {
      setClientLabel(
        salesOrder.clientCode
          ? `${salesOrder.clientCode} — ${salesOrder.clientName}`
          : salesOrder.clientName,
      )
    } else {
      setClientLabel(`${targetUuid.slice(0, 8)}…`)
    }
    setClientOptions([])
  }, [salesOrder])

  // Hidrata o nome e a unidade de medida dos itens no modo edição.
  //
  // O `SalesOrderResponse` carrega apenas o `productUuid` em cada item —
  // sem nome e sem unidade. Sem este efeito, o campo "Produto" de cada
  // linha ficava vazio. Aqui dedupamos os UUIDs e buscamos cada produto
  // uma vez, atualizando todas as linhas que o referenciam. Mantemos o
  // UUID curto como fallback caso o produto tenha sido inativado/removido
  // ou a busca falhe.
  useEffect(() => {
    if (!salesOrder) return
    let cancelled = false

    const uniqueProductUuids = Array.from(
      new Set(salesOrder.items.map((it) => it.productUuid)),
    )
    if (uniqueProductUuids.length === 0) return

    Promise.all(
      uniqueProductUuids.map(async (uuid) => {
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
        setItems((prev) =>
          prev.map((it) => {
            const found = entries.find(([uuid]) => uuid === it.productUuid)
            const product = found?.[1]
            if (product) {
              const label = product.code
                ? `${product.code} — ${product.name}`
                : product.name
              return {
                ...it,
                productLabel: label,
                unitType: product.unitType,
              }
            }
            // Fallback: UUID curto mantém o campo visível.
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
  }, [salesOrder])

  // Debounce do typeahead de clientes.
  const clientDebounce = useRef<ReturnType<typeof setTimeout> | null>(null)
  // Ref que espelha o `clientType` mais recente, permitindo que o
  // callback do debounce (criado uma única vez no mount) sempre leia o
  // tipo atual sem precisar dele nas deps do useCallback. Isso evita
  // reinstanciar o callback quando o usuário troca PF/PJ no select.
  const clientTypeRef = useRef<SalesOrderClientType>(clientType)
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
      // Lê o tipo atual via ref para que mudanças no select (PF/PJ)
      // reflitam na próxima busca agendada. O endpoint de busca de
      // clientes é compartilhado com o módulo de propostas.
      searchQuotationClients(trimmed, 20, clientTypeRef.current as QuotationClientType)
        .then((r) => {
          setClientOptions(r)
        })
        .catch(() => {
          setClientOptions([])
        })
        .finally(() => {
          setClientSearching(false)
        })
    }, 300)
  }, [])

  // Typeahead de produtos por linha.
  const productDebounce = useRef<ReturnType<typeof setTimeout> | null>(null)
  const handleProductQuery = useCallback(
    (rowKey: string, query: string) => {
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
          .then((p) => {
            // Só atualiza se o usuário ainda está na mesma linha.
            setProductOptions(p.content)
          })
          .catch(() => {
            setProductOptions([])
          })
          .finally(() => {
            setProductSearching(false)
          })
      }, 300)
    },
    [],
  )

  // === handlers de itens ===
  function addItem() {
    setItems((prev) => [
      ...prev,
      {
        rowKey: nextRowKey(),
        productUuid: '',
        productLabel: '',
        unitType: null,
        unitPrice: 0,
        // Pré-preenche com 1 unidade para evitar que o item novo entre
        // com quantidade zero (o que dispararia erro de validação no submit).
        quantity: 1,
        discountType: null,
        discount: null,
      },
    ])
  }

  function removeItem(rowKey: string) {
    setItems((prev) => prev.filter((it) => it.rowKey !== rowKey))
  }

  function updateItem(rowKey: string, patch: Partial<ItemDraft>) {
    setItems((prev) =>
      prev.map((it) => (it.rowKey === rowKey ? { ...it, ...patch } : it)),
    )
  }

  function selectProduct(rowKey: string, p: ProductResponse) {
    updateItem(rowKey, {
      productUuid: p.uuid,
      productLabel: p.name,
      unitType: p.unitType,
      // snapshot do preço atual do produto
      unitPrice: p.price,
    })
    setProductOptions([])
    setActiveProductRow(null)
  }

  // === totais calculados localmente ===
  // Diferente das propostas, o backend de pedidos NÃO expõe um endpoint
  // /simulate. Replicamos a fórmula do backend (SalesOrderMapper /
  // SalesOrder.recalculateTotals) para oferecer o mesmo preview em tempo
  // real. Sem margem de lucro — o total reflete o valor cobrado do cliente.
  const discountNumber = discountType !== '' ? parseNumber(discount) : null
  const freightValueNumber = parseNumber(freightValue)
  const totals = calculateSalesOrderTotals(
    items.map((it) => ({
      quantity: it.quantity,
      unitPrice: it.unitPrice,
      discount: it.discount,
      discountType: it.discountType,
    })),
    discountNumber,
    discountType !== '' ? discountType : null,
    freightValueNumber,
  )

  // === validação e submit ===
  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!clientUuid) {
      errs.clientUuid = 'Selecione um cliente.'
    }
    if (!sellerUuid) {
      errs.sellerUuid = 'Selecione o vendedor responsável.'
    }

    if (items.length === 0) {
      errs.items = 'O pedido deve ter ao menos um item.'
    } else {
      items.forEach((it, idx) => {
        if (!it.productUuid) {
          errs[`items.${idx}.product`] = 'Selecione um produto.'
        }
        if (!(it.quantity > 0)) {
          errs[`items.${idx}.quantity`] = 'Quantidade deve ser maior que zero.'
        }
        if (it.quantity > MAX_ITEM_VALUE) {
          errs[`items.${idx}.quantity`] =
            `Quantidade não pode exceder ${MAX_ITEM_VALUE.toLocaleString('pt-BR')}.`
        }
        if (!(it.unitPrice >= 0)) {
          errs[`items.${idx}.unitPrice`] =
            'Preço unitário não pode ser negativo.'
        }
        if (it.unitPrice > MAX_ITEM_VALUE) {
          errs[`items.${idx}.unitPrice`] =
            `Preço unitário não pode exceder ${MAX_ITEM_VALUE.toLocaleString('pt-BR')}.`
        }
        const grossLine = it.unitPrice * it.quantity
        if (grossLine > MAX_ITEM_VALUE) {
          errs[`items.${idx}.unitPrice`] =
            `Total da linha não pode exceder ${MAX_ITEM_VALUE.toLocaleString('pt-BR')}.`
        }
      })
    }

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

    if (attention.length > 150) {
      errs.attention = 'Aos cuidados de deve ter no máximo 150 caracteres.'
    }
    if (notes.length > 2000) {
      errs.notes = 'Observações devem ter no máximo 2000 caracteres.'
    }

    if (freightValue.trim() !== '') {
      const f = parseNumber(freightValue)
      if (f == null || f < 0) {
        errs.freightValue = 'Valor do frete não pode ser negativo.'
      }
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

    const itemsPayload: SalesOrderItemRequest[] = items.map((it) => {
      const base: SalesOrderItemRequest = {
        productUuid: it.productUuid,
        quantity: it.quantity,
        unitPrice: it.unitPrice,
      }
      if (it.discountType != null) base.discountType = it.discountType
      if (it.discount != null) base.discount = it.discount
      return base
    })

    try {
      if (isEdit) {
        const payload: SalesOrderUpdateRequest = {
          attention: attention.trim() ? attention.trim() : null,
          sellerUuid,
          items: itemsPayload,
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
        payload.freightType = freightType === '' ? null : freightType
        payload.freightValue = parseNumber(freightValue)
        payload.carrierUuid = carrierUuid || null

        await onSaveUpdate(payload)
        setSuccess('Pedido atualizado com sucesso!')
        reset()
      } else {
        const payload: SalesOrderCreateRequest = {
          sellerUuid,
          items: itemsPayload,
        }
        if (clientType === 'CUSTOMER') {
          payload.customerUuid = clientUuid
        } else {
          payload.companyUuid = clientUuid
        }
        if (attention.trim()) payload.attention = attention.trim()
        if (discountType !== '') {
          payload.discountType = discountType
          payload.discount = parseNumber(discount) ?? 0
        }
        if (paymentCondition !== '') {
          payload.paymentCondition = paymentCondition
        }
        if (notes.trim()) payload.notes = notes.trim()
        if (freightType !== '') payload.freightType = freightType
        if (freightValue.trim()) {
          payload.freightValue = parseNumber(freightValue)
        }
        payload.carrierUuid = carrierUuid || null

        await onSaveCreate(payload)
        setSuccess('Pedido criado com sucesso!')
        reset()
      }
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) setFieldErrors(apiErr.fieldErrors)
    }
  }

  const clientTypeOptions = [
    { value: 'CUSTOMER', label: QUOTATION_CLIENT_TYPE_LABELS.CUSTOMER },
    { value: 'COMPANY', label: QUOTATION_CLIENT_TYPE_LABELS.COMPANY },
  ]

  return (
    <form
      id="sales-order-form"
      onSubmit={handleSubmit}
      className="flex flex-col gap-6"
      noValidate
    >
      {formError ? <Alert variant="error">{formError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}

      {/* Cabeçalho — cliente + vendedor */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Cliente e vendedor</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Selecione o tipo de cliente (PF ou PJ), busque pelo nome ou código,
          e informe o vendedor responsável.
        </p>

        <div className="grid gap-4 sm:grid-cols-[160px_180px_1fr]">
          <Input
            label="Número do pedido"
            type="number"
            inputMode="numeric"
            min={1}
            value={number}
            onChange={(e) => {
              numberDirtyRef.current = true
              setNumber(e.target.value)
            }}
            onBlur={getBlurHandler('number')}
            error={shouldShowError('number', fieldErrors.number)}
            disabled
            readOnly
            hint="Gerado automaticamente (a partir de 1000)."
            className="max-w-[160px]"
          />
          <Select
            label="Tipo de cliente"
            value={clientType}
            onChange={(e) => {
              setClientType(e.target.value as SalesOrderClientType)
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
              htmlFor="client-search"
              className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
            >
              Cliente
              <span className="ml-0.5 text-red-500">*</span>
            </label>
            <div className="relative">
              <Input
                id="client-search"
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

        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <Select
            label="Vendedor"
            value={sellerUuid}
            onChange={(e) => setSellerUuid(e.target.value)}
            onBlur={getBlurHandler('sellerUuid')}
            error={shouldShowError('sellerUuid', fieldErrors.sellerUuid)}
            required
            options={[
              { value: '', label: sellersLoading ? 'Carregando…' : 'Selecione…' },
              ...sellers.map((s) => ({ value: s.uuid, label: s.name })),
            ]}
            aria-label="Vendedor responsável"
          />
          <Input
            label="Aos cuidados de"
            value={attention}
            onChange={(e) => setAttention(e.target.value)}
            onBlur={getBlurHandler('attention')}
            error={shouldShowError('attention', fieldErrors.attention)}
            maxLength={150}
          />
        </div>
      </section>

      {/* Itens */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Itens</h3>
          <Button type="button" variant="primary" onClick={addItem} className="mb-0.5 mr-6">
            <Plus className="h-4 w-4" />
            Adicionar item
          </Button>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Liste os produtos do pedido. Cada linha é um item com quantidade,
          preço unitário e desconto opcional.
        </p>

        {fieldErrors.items && !fieldErrors.items.startsWith('items.') ? (
          <Alert variant="error">{fieldErrors.items}</Alert>
        ) : null}

        {items.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhum item. Clique em <strong>Adicionar item</strong> para começar.
          </div>
        ) : (
          <div className="rounded-lg border border-slate-200 dark:border-slate-700">
            {items.map((it, idx) => (
              <ItemRow
                key={it.rowKey}
                index={idx}
                isFirst={idx === 0}
                item={it}
                lineTotal={calculateItemTotalPrice(
                  it.unitPrice,
                  it.quantity,
                  it.discount,
                  it.discountType,
                )}
                fieldErrors={fieldErrors}
                productOptions={productOptions}
                productSearching={
                  productSearching && activeProductRow === it.rowKey
                }
                showError={shouldShowError}
                getBlurHandler={getBlurHandler}
                onQuery={(q) => handleProductQuery(it.rowKey, q)}
                onSelect={(p) => selectProduct(it.rowKey, p)}
                onPatch={(patch) => updateItem(it.rowKey, patch)}
                onRemove={() => removeItem(it.rowKey)}
              />
            ))}
          </div>
        )}
      </section>

      {/* Desconto, pagamento, frete, observações */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Condições</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Desconto global, condição de pagamento, frete (tipo,
          transportadora e valor) e observações do pedido.
        </p>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Select
            label="Tipo de desconto global"
            value={discountType}
            onChange={(e) =>
              setDiscountType(e.target.value as DiscountType | '')
            }
            onBlur={getBlurHandler('discountType')}
            error={shouldShowError('discountType', fieldErrors.discountType)}
            options={[
              { value: '', label: 'Sem desconto' },
              ...DISCOUNT_TYPE_OPTIONS,
            ]}
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
              // Normaliza para 2 casas decimais no formato brasileiro
              // (vírgula). Aplica tanto para valor em R$ quanto para
              // percentual, mantendo consistência com os demais campos.
              if (discount.trim() !== '') {
                const formatted = formatBRLValue(discount)
                if (formatted) setDiscount(formatted)
              }
              getBlurHandler('discount')()
            }}
            error={shouldShowError('discount', fieldErrors.discount)}
            disabled={discountType === ''}
            hint={
              discountType === 'PERCENT'
                ? 'Percentual aplicado sobre o subtotal.'
                : discountType === 'AMOUNT'
                  ? 'Valor fixo em reais.'
                  : 'Defina o tipo de desconto primeiro.'
            }
          />
          <Input
            label="Data de emissão"
            type="date"
            value={orderDate}
            onChange={(e) => setOrderDate(e.target.value)}
            onBlur={getBlurHandler('orderDate')}
            error={shouldShowError('orderDate', fieldErrors.orderDate)}
            disabled
            readOnly
            hint="Preenchida automaticamente com a data atual (somente leitura)."
          />
          <div className="sm:col-span-2 lg:col-span-2">
            <Select
              label="Condição de pagamento"
              value={paymentCondition}
              onChange={(e) =>
                setPaymentCondition(e.target.value as PaymentCondition | '')
              }
              options={[
                { value: '', label: 'Selecione…' },
                ...PAYMENT_CONDITION_OPTIONS,
              ]}
              aria-label="Condição de pagamento"
            />
          </div>
          <div className="sm:col-span-2 lg:col-span-3">
            <FreightConditionsFields
              freightType={freightType}
              onFreightTypeChange={(v) => setFreightType(v as FreightType | '')}
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
            />
          </div>
        </div>

        <div className="mt-4">
          <label
            htmlFor="sales-order-notes"
            className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
          >
            Observações
          </label>
          <RichTextEditor
            id="sales-order-notes"
            value={notes}
            onChange={setNotes}
            onBlur={getBlurHandler('notes')}
            maxLength={2000}
            placeholder="Instruções de entrega, garantias, condições adicionais…"
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
              Itens
            </dt>
            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {items.length}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Quantidade
            </dt>
            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {totals.totalQuantity}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Subtotal
            </dt>
            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {brlFormatter.format(totals.subtotal)}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Frete
            </dt>
            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {brlFormatter.format(freightValueNumber ?? 0)}
            </dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Total
            </dt>
            <dd className="mt-1 text-lg font-semibold text-primary-700 dark:text-primary-200">
              {brlFormatter.format(totals.total)}
            </dd>
          </div>
        </dl>
        {totals.globalDiscountValue > 0 ? (
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            Desconto global aplicado: {brlFormatter.format(totals.globalDiscountValue)}
            {freightValueNumber != null && freightValueNumber > 0
              ? ' • Frete somado ao total (sem desconto).'
              : ''}
          </p>
        ) : freightValueNumber != null && freightValueNumber > 0 ? (
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            Frete somado ao total (sem desconto).
          </p>
        ) : null}
      </section>
    </form>
  )
}

// =====================================================================
// Subcomponente de linha de item
// =====================================================================

interface ItemRowProps {
  index: number
  /** Indica se esta é a primeira linha — usada para mostrar labels acima dos campos. */
  isFirst: boolean
  item: ItemDraft
  /** Total líquido da linha (calculado localmente). */
  lineTotal: number
  fieldErrors: Record<string, string>
  productOptions: ProductResponse[]
  productSearching: boolean
  /**
   * Mesmo formato de `shouldShowError` do hook `useFieldTouched`:
   * recebe (field, errorMessage) e devolve a mensagem se o campo já
   * foi tocado/houve submit, ou `undefined` caso contrário.
   */
  showError: <E extends string | null | undefined>(
    field: string,
    error: E,
  ) => E | undefined
  getBlurHandler: (field: string) => () => void
  onQuery: (query: string) => void
  onSelect: (p: ProductResponse) => void
  onPatch: (patch: Partial<ItemDraft>) => void
  onRemove: () => void
}

function ItemRow({
  index,
  isFirst,
  item,
  lineTotal,
  fieldErrors,
  productOptions,
  productSearching,
  showError,
  getBlurHandler,
  onQuery,
  onSelect,
  onPatch,
  onRemove,
}: ItemRowProps) {
  const total = lineTotal
  const quantityField = `items.${index}.quantity`
  const unitPriceField = `items.${index}.unitPrice`
  const discountField = `items.${index}.discount`
  const productField = `items.${index}.product`

  const productError = showError(productField, fieldErrors[productField])
  const quantityError = showError(quantityField, fieldErrors[quantityField])
  const unitPriceError = showError(unitPriceField, fieldErrors[unitPriceField])

  // Estado local de exibição para os campos monetários (Preço e Desconto).
  // Permite mostrar o valor formatado com 2 casas decimais (ex.: "80,00")
  // mesmo quando o número armazenado for 80. O estado é sincronizado com
  // o número externo sempre que ele muda (ex.: ao selecionar um produto,
  // que sobrescreve o preço) e formatado no blur.
  const [unitPriceDisplay, setUnitPriceDisplay] = useState<string>(
    item.unitPrice != null ? formatBRLValue(item.unitPrice) : '',
  )
  const [discountDisplay, setDiscountDisplay] = useState<string>(
    item.discount != null ? formatBRLValue(item.discount) : '',
  )

  useEffect(() => {
    setUnitPriceDisplay(item.unitPrice != null ? formatBRLValue(item.unitPrice) : '')
  }, [item.unitPrice])

  useEffect(() => {
    setDiscountDisplay(item.discount != null ? formatBRLValue(item.discount) : '')
  }, [item.discount])

  // Estilo base dos inputs compactos da linha. Mesmo padrão visual dos
  // campos da tabela — borda + focus ring com a cor `--color-focus`.
  const inputBase =
    'h-9 w-full min-w-0 rounded-md border bg-white px-2 text-sm text-slate-900 outline-none ' +
    'placeholder:text-slate-400 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500 ' +
    'border-slate-300 focus:border-focus focus:ring-1 focus:ring-focus/30 dark:border-slate-700 ' +
    'transition-colors duration-200'
  const inputError =
    'border-red-500 focus:border-red-500 focus:ring-red-500/30'

  // Estilo do label — exibido apenas na primeira linha, alinhado ao input.
  const labelCls =
    'mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-300'

  return (
    <div
      className={[
        // `items-start` (em vez de `items-center`) é necessário porque a
        // primeira linha tem label acima do input, ficando mais alta que
        // as demais. O restante do grid continua alinhado pelo topo.
        'grid items-start gap-2 px-3 py-2 sm:grid-cols-[40px_1fr_64px_64px_80px_80px_120px_32px] lg:grid-cols-[40px_minmax(0,2fr)_88px_80px_96px_96px_160px_40px]',
      ].join(' ')}
      role="row"
    >
      {/* # — sempre centralizado verticalmente, sem label */}
      <div
        role="cell"
        className="flex h-full flex-col items-center justify-center text-center font-mono text-xs font-semibold text-slate-500 dark:text-slate-400"
      >
        {String(index + 1).padStart(2, '0')}
      </div>

      {/* Produto (busca com typeahead) */}
      <div role="cell" className="relative min-w-0">
        {isFirst ? <label className={labelCls}>Produto</label> : null}
        <input
          type="text"
          aria-label="Produto"
          aria-invalid={!!productError}
          placeholder="Buscar produto por nome ou código…"
          value={item.productLabel}
          onChange={(e) => {
            onPatch({ productLabel: e.target.value, productUuid: '' })
            onQuery(e.target.value)
          }}
          onBlur={getBlurHandler(productField)}
          className={[inputBase, productError ? inputError : ''].join(' ')}
          required
        />
        {productSearching ? (
          <span className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 text-slate-400">
            <Spinner size="sm" />
          </span>
        ) : null}
        {productOptions.length > 0 && !item.productUuid ? (
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
        {productError ? (
          <p className="mt-0.5 text-xs text-red-600 dark:text-red-400">
            {productError}
          </p>
        ) : null}
      </div>

      {/* Unidade (Select compacto) */}
      <div role="cell" className="min-w-0">
        {isFirst ? <label className={labelCls}>Unidade</label> : null}
        <select
          aria-label="Unidade de medida"
          value={item.unitType ?? ''}
          onChange={(e) => {
            const v = e.target.value
            onPatch({ unitType: v === '' ? null : (v as UnitType) })
          }}
          className={[
            'h-9 w-full min-w-0 rounded-md border border-slate-300 bg-white px-1 text-center text-sm text-slate-900 outline-none',
            'focus:border-focus focus:ring-1 focus:ring-focus/30 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100',
            'transition-colors duration-200',
          ].join(' ')}
        >
          <option value="">—</option>
          <option value="UNIDADE">UN</option>
          <option value="METROS">MT</option>
          <option value="BOBINA">BOB</option>
        </select>
      </div>

      {/* Quantidade */}
      <div role="cell" className="min-w-0">
        {isFirst ? <label className={labelCls}>Qtde</label> : null}
        <input
          type="number"
          inputMode="decimal"
          step="0.0001"
          min={0.0001}
          max={MAX_ITEM_VALUE}
          aria-label="Quantidade"
          placeholder="Qtde"
          value={String(item.quantity ?? '')}
          onChange={(e) => {
            const v = parseNumber(e.target.value)
            onPatch({ quantity: v ?? 0 })
          }}
          onBlur={getBlurHandler(quantityField)}
          className={[inputBase, 'text-right', quantityError ? inputError : ''].join(' ')}
          required
        />
      </div>

      {/* Preço Unit */}
      <div role="cell" className="min-w-0">
        {isFirst ? <label className={labelCls}>Preço</label> : null}
        <input
          type="text"
          inputMode="decimal"
          aria-label="Preço unitário"
          placeholder="0,00"
          value={unitPriceDisplay}
          onChange={(e) => {
            setUnitPriceDisplay(e.target.value)
            const v = parseNumber(e.target.value)
            onPatch({ unitPrice: v ?? 0 })
          }}
          onBlur={() => {
            // Normaliza para 2 casas decimais no formato brasileiro
            // (vírgula): "80" → "80,00"; "45,9" → "45,90".
            const formatted = formatBRLValue(unitPriceDisplay)
            setUnitPriceDisplay(formatted)
            const n = parseNumber(formatted)
            onPatch({ unitPrice: n ?? 0 })
            getBlurHandler(unitPriceField)()
          }}
          className={[inputBase, 'text-right', unitPriceError ? inputError : ''].join(' ')}
          required
        />
      </div>

      {/* Desconto */}
      <div role="cell" className="min-w-0">
        {isFirst ? <label className={labelCls}>Desconto</label> : null}
        <input
          type="text"
          inputMode="decimal"
          aria-label="Desconto"
          placeholder="0,00"
          value={discountDisplay}
          onChange={(e) => {
            setDiscountDisplay(e.target.value)
            const v = parseNumber(e.target.value)
            onPatch({
              discount: v,
              discountType: v != null ? 'AMOUNT' : null,
            })
          }}
          onBlur={() => {
            // Normaliza para 2 casas decimais no formato brasileiro
            // (vírgula): "80" → "80,00"; "45,9" → "45,90". Se o campo
            // estiver vazio, limpa o desconto e o tipo.
            if (discountDisplay.trim() === '') {
              onPatch({ discount: null, discountType: null })
            } else {
              const formatted = formatBRLValue(discountDisplay)
              setDiscountDisplay(formatted)
              const n = parseNumber(formatted)
              onPatch({ discount: n, discountType: 'AMOUNT' })
            }
            getBlurHandler(discountField)()
          }}
          className={[inputBase, 'text-right'].join(' ')}
        />
      </div>

      {/* Total (read-only) */}
      <div role="cell" className="min-w-0">
        {isFirst ? <label className={labelCls}>Total</label> : null}
        <div
          className="flex h-9 min-w-0 items-center justify-end truncate rounded-md border border-slate-200 bg-slate-50 px-2 text-right text-sm font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
          title={brlFormatter.format(total)}
        >
          {brlFormatter.format(total)}
        </div>
      </div>

      {/* Ações */}
      <div role="cell" className="flex h-full flex-col items-center justify-center">
        {isFirst ? (
          // Label invisível só para ocupar o espaço do label das outras
          // células e alinhar verticalmente o ícone.
          <span className={`${labelCls} invisible`} aria-hidden="true">
            ·
          </span>
        ) : null}
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