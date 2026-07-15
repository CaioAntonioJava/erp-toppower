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
import {
  searchTechnicalProposalClients,
  simulateTechnicalProposal,
} from '../../api/technicalProposal.api'
import { listServiceTemplatesByCategory } from '../../api/servicetemplate.api'
import type { ClientSummaryResponse } from '../../types/quotation'
import type {
  TechnicalProposalClientType,
  TechnicalProposalConditionRequest,
  TechnicalProposalCreateRequest,
  TechnicalProposalResponse,
  TechnicalProposalServiceItemRequest,
  TechnicalProposalSimulateResponse,
  TechnicalProposalUpdateRequest,
} from '../../types/technicalProposal'
import { TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS } from '../../types/technicalProposal'
import type { ServiceCategory, ServiceTemplateResponse } from '../../types/servicetemplate'
import { SERVICE_CATEGORIES } from '../../types/servicetemplate'

interface TechnicalProposalFormProps {
  /** Proposta existente (modo edição). Quando omitido, é cadastro novo. */
  proposal?: TechnicalProposalResponse
  /** Próximo código previsto pelo backend (modo create). */
  initialCode?: string | null
  onSaveCreate: (payload: TechnicalProposalCreateRequest) => Promise<void>
  onSaveUpdate: (payload: TechnicalProposalUpdateRequest) => Promise<void>
}

/** Modo de uma linha de serviço: catálogo (com categoria + seleção) ou simples. */
type ServiceItemMode = 'CATALOG' | 'SIMPLE'

/** Linha do editor de serviços (estado local). */
interface ServiceDraft {
  rowKey: string
  /** Modo da linha: catálogo (com categoria + seleção) ou simples. */
  mode: ServiceItemMode
  /** Categoria do catálogo de serviços (opcional, só em modo CATALOG). */
  category: ServiceCategory | ''
  /** ID do ServiceTemplate selecionado (opcional, só em modo CATALOG). */
  serviceTemplateId: number | ''
  /** Nome do ServiceTemplate (exibição, só em modo CATALOG). */
  serviceTemplateName: string
  /** Descrição do serviço (HTML formatado em CATALOG, texto livre em SIMPLE). */
  description: string
  /** Preço do serviço (opcional). */
  price: string
}

const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

function nextRowKey(): string {
  return `row_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
}

/**
 * Verifica se um conteúdo HTML (do RichTextEditor) está vazio — sem texto
 * visível após remover as tags. Considera `<br>`, `<div></div>` e espaços
 * como vazio, da mesma forma que o `RichTextEditor` avalia seu `isEmpty`.
 */
function isHtmlEmpty(html: string | null | undefined): boolean {
  if (!html) return true
  return html.replace(/<[^>]+>/g, '').trim() === ''
}

/**
 * Heurística simples para inferir se uma descrição carregada do backend é
 * HTML (vem do modo CATALOG / RichTextEditor) ou texto puro (modo SIMPLE).
 * Usada no modo edição para restaurar o modo correto de cada linha.
 */
function looksLikeHtml(text: string | null | undefined): boolean {
  if (!text) return false
  return /<\/?[a-z][\s\S]*>/i.test(text)
}

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
  const [clientId, setClientId] = useState<number | null>(
    proposal?.customerId ?? proposal?.companyId ?? null,
  )
  const [clientLabel, setClientLabel] = useState<string>('')
  const [description, setDescription] = useState<string>(
    proposal?.description ?? '',
  )
  const [revision, setRevision] = useState<string>(
    proposal?.revision != null ? String(proposal.revision) : '',
  )
  const [technicalResponsible, setTechnicalResponsible] = useState<string>(
    proposal?.technicalResponsible ?? '',
  )
  const [responsibleEmail, setResponsibleEmail] = useState<string>(
    proposal?.email ?? '',
  )
  const [responsiblePhone, setResponsiblePhone] = useState<string>(
    proposal?.phone ?? '',
  )
  const [notes, setNotes] = useState<string>(proposal?.notes ?? '')
  const [generalPrice, setGeneralPrice] = useState<string>(
    proposal?.generalPrice != null ? formatBRLValue(proposal.generalPrice) : '',
  )

  // === código gerado pelo servidor ===
  const [code, setCode] = useState<string>(
    proposal?.code ?? initialCode ?? '',
  )
  const codeDirtyRef = useRef<boolean>(false)

  // === título da proposta ===
  const [title, setTitle] = useState<string>(() => {
    const base = `PROPOSTA TÉCNICA - COMERCIAL ${proposal?.code ?? initialCode ?? ''}`
    if (proposal?.revision != null) {
      return `${base} - REV. ${proposal.revision}`
    }
    return base
  })
  useEffect(() => {
    if (proposal) return
    if (initialCode == null) return
    const base = `PROPOSTA TÉCNICA - COMERCIAL ${initialCode}`
    setTitle(base)
  }, [initialCode, proposal])
  // Atualiza o título automaticamente quando a revisão muda.
  useEffect(() => {
    if (!code) return
    const base = `PROPOSTA TÉCNICA - COMERCIAL ${code}`
    const rev = revision.trim()
    if (rev) {
      setTitle(`${base} - REV. ${rev}`)
    } else {
      setTitle(base)
    }
  }, [code, revision])

  // === itens de serviço ===
  const [serviceItems, setServiceItems] = useState<ServiceDraft[]>(() => {
    if (proposal?.serviceItems && proposal.serviceItems.length > 0) {
      return proposal.serviceItems.map((s) => ({
        rowKey: nextRowKey(),
        // Infere o modo da linha pela natureza da descrição persistida:
        // HTML (RichTextEditor) → CATALOG; texto puro → SIMPLE.
        // Se veio com category e serviceTemplateId, é CATALOG.
        mode: (s.category && s.serviceTemplateId
          ? 'CATALOG'
          : looksLikeHtml(s.description)
            ? 'CATALOG'
            : 'SIMPLE') as ServiceItemMode,
        category: (s.category ?? '') as ServiceCategory | '',
        serviceTemplateId: s.serviceTemplateId ?? '',
        serviceTemplateName: '',
        description: s.description ?? '',
        price: s.price != null ? formatBRLValue(s.price) : '',
      }))
    }
    // Em modo create, iniciamos sem linhas — o usuário escolhe o tipo de
    // serviço (catalogado ou simples) ao clicar em um dos botões.
    return []
  })

  // === condições ===
  interface ConditionDraft {
    rowKey: string
    title: string
    content: string
  }

  const [conditions, setConditions] = useState<ConditionDraft[]>(() => {
    if (proposal?.conditions && proposal.conditions.length > 0) {
      return proposal.conditions.map((c) => ({
        rowKey: nextRowKey(),
        title: c.title,
        content: c.content ?? '',
      }))
    }
    return []
  })

  function addCondition() {
    setConditions((prev) => [
      ...prev,
      { rowKey: nextRowKey(), title: '', content: '' },
    ])
  }

  function removeCondition(rowKey: string) {
    setConditions((prev) => prev.filter((c) => c.rowKey !== rowKey))
  }

  function updateCondition(rowKey: string, patch: Partial<ConditionDraft>) {
    setConditions((prev) =>
      prev.map((c) => (c.rowKey === rowKey ? { ...c, ...patch } : c)),
    )
  }

  // === coleções auxiliares ===
  const [clientOptions, setClientOptions] = useState<ClientSummaryResponse[]>(
    [],
  )
  const [clientSearching, setClientSearching] = useState(false)

  // Cache de templates por categoria
  const [templatesByCategory, setTemplatesByCategory] = useState<
    Record<string, ServiceTemplateResponse[]>
  >({})
  const [templatesLoading, setTemplatesLoading] = useState<
    Record<string, boolean>
  >({})

  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const { shouldShowError, getBlurHandler, markAllTouched, reset } =
    useFieldTouched()

  // Sincroniza o código com `initialCode` que chega assincronamente.
  useEffect(() => {
    if (proposal) return
    if (initialCode == null) return
    if (codeDirtyRef.current) return
    setCode(initialCode)
  }, [initialCode, proposal])

  // Carrega os templates das categorias dos itens ao entrar em modo edição.
  useEffect(() => {
    if (!proposal?.serviceItems) return
    const categorias = new Set<string>()
    for (const s of proposal.serviceItems) {
      if (s.category) categorias.add(s.category)
    }
    for (const cat of categorias) {
      if (!templatesByCategory[cat]) {
        setTemplatesLoading((prev) => ({ ...prev, [cat]: true }))
        listServiceTemplatesByCategory(cat as ServiceCategory, { page: 0, size: 50 })
          .then((res) => {
            setTemplatesByCategory((prev) => ({
              ...prev,
              [cat]: res.content,
            }))
          })
          .catch(() => {
            setTemplatesByCategory((prev) => ({
              ...prev,
              [cat]: [],
            }))
          })
          .finally(() => {
            setTemplatesLoading((prev) => ({ ...prev, [cat]: false }))
          })
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [proposal?.serviceItems])

  // Pré-preenche o rótulo do cliente no modo edição.
  useEffect(() => {
    if (!proposal) return
    if (proposal.clientName) {
      setClientLabel(proposal.clientName)
    } else if (proposal.customerId || proposal.companyId) {
      const id = proposal.customerId ?? proposal.companyId ?? 0
      setClientLabel(`${String(id).slice(0, 8)}…`)
    }
    setClientOptions([])
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

  // === handlers de itens de serviço ===
  /** Adiciona uma linha de serviço catalogado (categoria + seleção + descrição HTML). */
  function addCatalogServiceItem() {
    setServiceItems((prev) => [
      ...prev,
      {
        rowKey: nextRowKey(),
        mode: 'CATALOG',
        category: '',
        serviceTemplateId: '',
        serviceTemplateName: '',
        description: '',
        price: '',
      },
    ])
  }
  /** Adiciona uma linha de serviço simples (descrição texto livre + preço). */
  function addSimpleServiceItem() {
    setServiceItems((prev) => [
      ...prev,
      {
        rowKey: nextRowKey(),
        mode: 'SIMPLE',
        category: '',
        serviceTemplateId: '',
        serviceTemplateName: '',
        description: '',
        price: '',
      },
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

  // Carrega templates por categoria quando o usuário seleciona uma categoria
  function handleCategoryChange(rowKey: string, category: ServiceCategory | '') {
    updateServiceItem(rowKey, {
      category,
      serviceTemplateId: '',
      serviceTemplateName: '',
      description: '',
      price: '',
    })
    if (category && !templatesByCategory[category]) {
      setTemplatesLoading((prev) => ({ ...prev, [category]: true }))
      listServiceTemplatesByCategory(category, { page: 0, size: 50 })
        .then((res) => {
          setTemplatesByCategory((prev) => ({
            ...prev,
            [category]: res.content,
          }))
        })
        .catch(() => {
          setTemplatesByCategory((prev) => ({
            ...prev,
            [category]: [],
          }))
        })
        .finally(() => {
          setTemplatesLoading((prev) => ({ ...prev, [category]: false }))
        })
    }
  }

  function handleServiceSelect(rowKey: string, template: ServiceTemplateResponse) {
    updateServiceItem(rowKey, {
      serviceTemplateId: template.id,
      serviceTemplateName: template.name,
      description: template.description ?? '',
    })
  }

  // === simulação de totais (debounced, sem persistir) ===
  const [simulation, setSimulation] =
    useState<TechnicalProposalSimulateResponse | null>(null)

  useEffect(() => {
    let cancelled = false
    const handle = setTimeout(() => {
      const servicePayload = serviceItems
        .filter((s) => !isHtmlEmpty(s.description) || s.price.trim() !== '')
        .map((s) => ({
          description: isHtmlEmpty(s.description) ? null : s.description,
          price: parseNumber(s.price) ?? null,
        }))
      const payload = {
        serviceItems: servicePayload.length > 0 ? servicePayload : null,
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
  ])

  const servicesSubtotal = simulation?.servicesSubtotal ?? 0
  const subtotal = simulation?.subtotal ?? 0
  const simulatedTotal = simulation?.total ?? 0
  const generalPriceValue = parseNumber(generalPrice) ?? 0
  const total = simulatedTotal + generalPriceValue

  // === validação e submit ===
  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!clientId) {
      errs.clientId = 'Selecione um cliente.'
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
    if (responsiblePhone.length > 20) {
      errs.phone = 'Telefone deve ter no máximo 20 caracteres.'
    }

    // Ao menos um serviço preenchido. Um serviço é
    // considerado válido quando possui descrição (HTML) ou preço.
    const validServices = serviceItems.filter(
      (s) => !isHtmlEmpty(s.description) || s.price.trim() !== '',
    )
    if (validServices.length === 0) {
      errs.items = 'A proposta deve ter ao menos um serviço.'
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
      .filter((s) => !isHtmlEmpty(s.description) || s.price.trim() !== '')
      .map((s) => ({
        description: isHtmlEmpty(s.description) ? null : s.description,
        price: parseNumber(s.price) ?? null,
        category: s.mode === 'CATALOG' && s.category ? s.category : null,
        serviceTemplateId: s.mode === 'CATALOG' && s.serviceTemplateId ? Number(s.serviceTemplateId) : null,
      }))

    const conditionsPayload: TechnicalProposalConditionRequest[] = conditions
      .filter((c) => c.title.trim() !== '')
      .map((c) => ({
        title: c.title.trim(),
        content: c.content.trim() || null,
      }))

    try {
      if (isEdit) {
        const payload: TechnicalProposalUpdateRequest = {
          description: description.trim() ? description.trim() : null,
          revision: revision.trim() ? Number(revision) : null,
          technicalResponsible: technicalResponsible.trim()
            ? technicalResponsible.trim()
            : null,
          email: responsibleEmail.trim() ? responsibleEmail.trim() : null,
          phone: responsiblePhone.trim() ? responsiblePhone.trim() : null,
          serviceItems: servicePayload.length > 0 ? servicePayload : null,
          conditions: conditionsPayload.length > 0 ? conditionsPayload : null,
        }
        if (clientType === 'CUSTOMER') {
          payload.customerId = clientId ? Number(clientId) : null
          payload.companyId = null
        } else {
          payload.companyId = clientId ? Number(clientId) : null
          payload.customerId = null
        }
        payload.notes = notes.trim() ? notes.trim() : null
        const gp = parseNumber(generalPrice)
        payload.generalPrice = gp != null ? gp : null

        await onSaveUpdate(payload)
        setSuccess('Proposta atualizada com sucesso!')
        reset()
      } else {
        const payload: TechnicalProposalCreateRequest = {
        }
        if (clientType === 'CUSTOMER') {
          payload.customerId = clientId ? Number(clientId) : null
        } else {
          payload.companyId = clientId ? Number(clientId) : null
        }
        if (description.trim()) payload.description = description.trim()
        if (revision.trim()) payload.revision = Number(revision)
        if (technicalResponsible.trim())
          payload.technicalResponsible = technicalResponsible.trim()
        if (responsibleEmail.trim()) payload.email = responsibleEmail.trim()
        if (responsiblePhone.trim()) payload.phone = responsiblePhone.trim()
        if (servicePayload.length > 0) payload.serviceItems = servicePayload
        if (conditionsPayload.length > 0) payload.conditions = conditionsPayload
        if (notes.trim()) payload.notes = notes.trim()
        const gp = parseNumber(generalPrice)
        if (gp != null) payload.generalPrice = gp

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

      {/* Cabeçalho — cliente + código */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Cliente</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Selecione o tipo de cliente (PF ou PJ), busque pelo nome ou código
          e informe a data de início do serviço.
        </p>

        <div className="grid gap-4 sm:grid-cols-[180px_100px_180px_1fr]">
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
          <Input
            label="Revisão"
            type="number"
            min={0}
            value={revision}
            onChange={(e) => setRevision(e.target.value)}
            placeholder="0"
            hint="Opcional"
          />
          <Select
            label="Tipo de cliente"
            value={clientType}
            onChange={(e) => {
              setClientType(e.target.value as TechnicalProposalClientType)
              setClientId(null)
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
                  setClientId(null)
                  handleClientQuery(e.target.value)
                }}
                onBlur={getBlurHandler('clientId')}
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
            {clientOptions.length > 0 && !clientId ? (
              <ul className="mt-1 max-h-56 overflow-y-auto rounded-lg border border-slate-200 bg-white text-sm shadow-sm dark:border-slate-700 dark:bg-slate-900">
                {clientOptions.map((c) => (
                  <li key={c.id}>
                    <button
                      type="button"
                      className="flex w-full items-start gap-2 px-3 py-2 text-left hover:bg-slate-100 dark:hover:bg-slate-800"
                      onClick={() => {
                          setClientId(c.id)
                          setClientLabel(c.name)
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
            {shouldShowError('clientId', fieldErrors.clientId) ? (
              <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
                {fieldErrors.clientId}
              </p>
            ) : null}
          </div>
        </div>

        {/* Título da proposta */}
        <div className="mt-4">
          <Input
            label="Título da proposta"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="PROPOSTA TÉCNICA - COMERCIAL …"
            className="text-center"
          />
        </div>

        <div className="mt-4">
          <label
            htmlFor="tp-description"
            className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
          >
            Apresentação:
          </label>
          <RichTextEditor
            id="tp-description"
            value={description}
            onChange={setDescription}
            onBlur={getBlurHandler('description')}
            maxLength={1000}
            placeholder="Formalização do serviço prestado — escopo, etapas, condições…"
          />
          {shouldShowError('description', fieldErrors.description) ? (
            <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">
              {fieldErrors.description}
            </p>
          ) : (
            <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">
              {description.length}/1000 caracteres (inclui formatação).
            </p>
          )}
        </div>

        {/* Responsável técnico + e-mail + telefone (opcionais) */}
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
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
          <Input
            label="Fone do responsável"
            value={responsiblePhone}
            onChange={(e) => setResponsiblePhone(e.target.value)}
            onBlur={getBlurHandler('phone')}
            error={shouldShowError('phone', fieldErrors.phone)}
            maxLength={20}
            placeholder="Ex.: (11) 99999-9999"
          />
        </div>
      </section>

      {/* Itens de serviço */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold">Serviços prestados</h3>
          <div className="flex flex-wrap items-center gap-2">
            <Button type="button" variant="primary" onClick={addCatalogServiceItem}>
              <Plus className="h-4 w-4" />
              Serviço Catálogo
            </Button>
            <Button type="button" variant="primary" onClick={addSimpleServiceItem}>
              <Plus className="h-4 w-4" />
              Serviço Simples
            </Button>
          </div>
        </div>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Adicione um <strong>serviço do catálogo</strong> (com descrição
          formatada em HTML) ou um <strong>serviço simples</strong> (descrição
          em texto livre). Em ambos os casos, a descrição é opcional.
        </p>

        {serviceItems.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
            Nenhum serviço. Clique em <strong>Serviço Catálogo</strong> ou
            <strong> Serviço Simples</strong> para começar.
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {serviceItems.map((s, idx) => (
              s.mode === 'CATALOG' ? (
                <ServiceRowCatalog
                  key={s.rowKey}
                  index={idx}
                  isFirst={idx === 0}
                  draft={s}
                  templates={
                    s.category
                      ? templatesByCategory[s.category] ?? []
                      : []
                  }
                  templatesLoading={
                    s.category ? templatesLoading[s.category] ?? false : false
                  }
                  onChange={(patch) => updateServiceItem(s.rowKey, patch)}
                  onCategoryChange={(cat) => handleCategoryChange(s.rowKey, cat)}
                  onServiceSelect={(template) => handleServiceSelect(s.rowKey, template)}
                  onRemove={() => removeServiceItem(s.rowKey)}
                />
              ) : (
                <ServiceRowSimple
                  key={s.rowKey}
                  index={idx}
                  isFirst={idx === 0}
                  draft={s}
                  onChange={(patch) => updateServiceItem(s.rowKey, patch)}
                  onRemove={() => removeServiceItem(s.rowKey)}
                />
              )
            ))}
          </div>
	        )}
		      </section>

		      {/* Preço geral (opcional, sem regras de cálculo) */}
		      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
		        <h3 className="mb-4 text-base font-semibold">Preço geral</h3>
		        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
		          Preço geral da proposta, de preenchimento livre. Não participa de
		          cálculos automáticos — apenas um valor informativo.
		        </p>
		        <div className="max-w-xs">
		          <Input
		            label="Preço geral"
		            value={generalPrice}
		            onChange={(e) => setGeneralPrice(e.target.value)}
		            onBlur={() => {
		              const trimmed = generalPrice.trim()
		              if (!trimmed) return
		              // Se não tem vírgula, adiciona ",00"
		              if (!trimmed.includes(',')) {
		                setGeneralPrice(trimmed + ',00')
		              } else {
		                // Se tem vírgula mas não tem centavos, completa
		                const parts = trimmed.split(',')
		                if (parts.length === 2 && parts[1].length === 0) {
		                  setGeneralPrice(trimmed + '00')
		                } else if (parts.length === 2 && parts[1].length === 1) {
		                  setGeneralPrice(trimmed + '0')
		                }
		              }
		            }}
		            placeholder="Ex.: 1.500,00"
		            hint="Opcional — preço informativo"
		          />
		        </div>
		      </section>

		      {/* Condições */}
	      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
	        <div className="mb-1 flex flex-wrap items-center justify-between gap-2">
	          <h3 className="text-base font-semibold">Condições</h3>
	          <Button type="button" variant="primary" onClick={addCondition}>
	            <Plus className="h-4 w-4" />
	            Adicionar Condição
	          </Button>
	        </div>
	        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
	          Adicione condições específicas da proposta (ex.: garantia, prazo de
	          pagamento, multa, forma de pagamento, etc.). Cada condição possui um
	          título e um conteúdo textual. A ordem é definida pela posição na lista.
	        </p>

	        {conditions.length === 0 ? (
	          <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
	            Nenhuma condição. Clique em <strong>Adicionar Condição</strong> para
	            começar.
	          </div>
	        ) : (
	          <div className="flex flex-col gap-3">
	            {conditions.map((c, idx) => (
	              <div
	                key={c.rowKey}
	                className="rounded-md border border-slate-200 p-3 dark:border-slate-700"
	              >
	                <div className="mb-2 flex items-center gap-2">
	                  <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-600 dark:bg-slate-800 dark:text-slate-300">
	                    Condição {idx + 1}
	                  </span>
	                  <button
	                    type="button"
	                    onClick={() => removeCondition(c.rowKey)}
	                    aria-label="Remover condição"
	                    title="Remover condição"
	                    className="ml-auto inline-flex h-8 w-8 items-center justify-center rounded-md text-slate-500 transition-colors hover:bg-slate-100 hover:text-red-600 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-red-400"
	                  >
	                    <Trash2 className="h-4 w-4" />
	                  </button>
	                </div>
	                <div className="grid gap-3">
	                  <Input
	                    label="Título"
	                    value={c.title}
	                    onChange={(e) =>
	                      updateCondition(c.rowKey, { title: e.target.value })
	                    }
	                    placeholder="Ex.: Garantia, Prazo de pagamento, Multa…"
	                    maxLength={150}
	                    required
	                  />
	                  <div>
	                    <label className="mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-300">
	                      Conteúdo <span className="font-normal text-slate-400">(opcional)</span>
	                    </label>
		                    <textarea
		                      aria-label="Conteúdo da condição"
		                      value={c.content}
		                      onChange={(e) =>
		                        updateCondition(c.rowKey, { content: e.target.value })
		                      }
		                      placeholder="Descreva os detalhes desta condição…"
		                      maxLength={5000}
		                      rows={3}
		                      className="w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none placeholder:text-slate-400 focus:border-focus focus:ring-1 focus:ring-focus/30 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500"
	                    />
	                  </div>
	                </div>
	              </div>
	            ))}
	          </div>
	        )}
	      </section>

		      {/* Totais */}
		      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
		        <h3 className="mb-4 text-base font-semibold">Totais</h3>
		        <dl className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-3">
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
		              Subtotal
		            </dt>
		            <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
		              {brlFormatter.format(subtotal)}
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
		      </section>
    </form>
  )
}

// =====================================================================
// Subcomponente: linha de serviço
// =====================================================================

// =====================================================================
// Subcomponente: linha de serviço catalogado (categoria + seleção + RichTextEditor)
// =====================================================================

interface ServiceRowCatalogProps {
  index: number
  isFirst: boolean
  draft: ServiceDraft
  templates: ServiceTemplateResponse[]
  templatesLoading: boolean
  onChange: (patch: Partial<ServiceDraft>) => void
  onCategoryChange: (category: ServiceCategory | '') => void
  onServiceSelect: (template: ServiceTemplateResponse) => void
  onRemove: () => void
}

function ServiceRowCatalog({
  index,
  isFirst,
  draft,
  templates,
  templatesLoading,
  onChange,
  onCategoryChange,
  onServiceSelect,
  onRemove,
}: ServiceRowCatalogProps) {
  const labelCls = 'mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-300'

  const categoryOptions = [
    { value: '', label: 'Selecione…' },
    ...SERVICE_CATEGORIES.map((c) => ({
      value: c.value,
      label: c.label,
    })),
  ]

  const serviceOptions = [
    { value: '', label: 'Selecione…' },
    ...templates.map((t) => ({
      value: String(t.id),
      label: t.name,
    })),
  ]

  return (
    <div className="rounded-md border border-slate-200 p-2 dark:border-slate-700">
      {/* Badge do modo + linha de categoria/serviço/remover */}
      <div className="mb-2 flex items-center gap-2">
        <span className="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-primary">
          Serviço Catálogo
        </span>
      </div>
      <div className="grid items-start gap-2 sm:grid-cols-[40px_200px_1fr_40px]">
        <div className="flex h-full items-center justify-center font-mono text-xs font-semibold text-slate-500 dark:text-slate-400">
          {String(index + 1).padStart(2, '0')}
        </div>
        <div>
          {isFirst ? <label className={labelCls}>Categoria</label> : null}
          <select
            aria-label="Categoria do serviço"
            value={draft.category}
            onChange={(e) => onCategoryChange(e.target.value as ServiceCategory | '')}
            className="h-9 w-full min-w-0 rounded-md border border-slate-300 bg-white px-2 text-sm text-slate-900 outline-none focus:border-focus focus:ring-1 focus:ring-focus/30 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          >
            {categoryOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          {isFirst ? <label className={labelCls}>Serviço</label> : null}
          {templatesLoading ? (
            <div className="flex h-9 items-center gap-2 text-sm text-slate-500">
              <Spinner size="sm" />
              Carregando…
            </div>
          ) : (
            <select
              aria-label="Serviço do catálogo"
              value={draft.serviceTemplateId !== '' ? String(draft.serviceTemplateId) : ''}
              onChange={(e) => {
                const id = e.target.value
                if (id) {
                  const template = templates.find((t) => String(t.id) === id)
                  if (template) onServiceSelect(template)
                } else {
                  onChange({ serviceTemplateId: '', serviceTemplateName: '' })
                }
              }}
              disabled={!draft.category}
              className="h-9 w-full min-w-0 rounded-md border border-slate-300 bg-white px-2 text-sm text-slate-900 outline-none focus:border-focus focus:ring-1 focus:ring-focus/30 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:disabled:bg-slate-800"
            >
              {serviceOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          )}
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

      {/* Linha 2: descrição do serviço (HTML formatado, editável) */}
      <div className="mt-2">
        {isFirst ? (
          <label className={labelCls}>
            Descrição do serviço <span className="font-normal text-slate-400">(opcional)</span>
          </label>
        ) : null}
        <RichTextEditor
          value={draft.description}
          onChange={(html) => onChange({ description: html })}
          placeholder="Descreva o serviço prestado… (ao selecionar um serviço do catálogo, a descrição é preenchida automaticamente)"
          maxLength={2000}
          className="[&_[role=textbox]]:min-h-[240px]"
        />
      </div>
    </div>
  )
}

// =====================================================================
// Subcomponente: linha de serviço simples (descrição texto livre)
// =====================================================================

interface ServiceRowSimpleProps {
  index: number
  isFirst: boolean
  draft: ServiceDraft
  onChange: (patch: Partial<ServiceDraft>) => void
  onRemove: () => void
}

function ServiceRowSimple({
  index,
  isFirst,
  draft,
  onChange,
  onRemove,
}: ServiceRowSimpleProps) {
  const labelCls = 'mb-1.5 block text-xs font-medium text-slate-600 dark:text-slate-300'
  const inputBase =
    'h-9 w-full min-w-0 rounded-md border border-slate-300 bg-white px-2 text-sm text-slate-900 outline-none ' +
    'placeholder:text-slate-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500 ' +
    'focus:border-focus focus:ring-1 focus:ring-focus/30 transition-colors duration-200'

  return (
    <div className="rounded-md border border-slate-200 p-2 dark:border-slate-700">
      {/* Badge do modo + linha de descrição/remover */}
      <div className="mb-2 flex items-center gap-2">
        <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-600 dark:bg-slate-800 dark:text-slate-300">
          Serviço Simples
        </span>
      </div>
      <div className="grid items-start gap-2 sm:grid-cols-[40px_1fr_40px]">
        <div className="flex h-full items-center justify-center font-mono text-xs font-semibold text-slate-500 dark:text-slate-400">
          {String(index + 1).padStart(2, '0')}
        </div>
        <div className="min-w-0">
          {isFirst ? <label className={labelCls}>Descrição</label> : null}
          <input
            type="text"
            aria-label="Descrição do serviço"
            placeholder="Descreva o serviço prestado…"
            value={draft.description}
            onChange={(e) => onChange({ description: e.target.value })}
            className={inputBase}
            maxLength={2000}
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
    </div>
  )
}

// =====================================================================
// Subcomponente: linha de serviço catalogado (categoria + seleção + RichTextEditor)
// =====================================================================