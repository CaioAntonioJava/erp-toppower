import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Trash2, Plus } from 'lucide-react'
import type {
  ClientSummaryResponse,
  ContractClientType,
  ContractClauseResponse,
  ContractCreateRequest,
  ContractResponse,
  ContractUpdateRequest,
  RegistrationStatus,
} from '../../types/contract'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Alert } from '../ui/Alert'
import { Spinner } from '../ui/Spinner'
import { Button } from '../ui/Button'
import { RichTextEditor } from '../ui/RichTextEditor'
import { toApiError } from '../../lib/errors'
import { parseNumber, formatBRLValue } from '../../lib/money'
import { useFieldTouched } from '../../hooks/useFieldTouched'
import { getNextContractCode, searchContractClients } from '../../api/contract.api'
import { listServiceTemplates } from '../../api/servicetemplate.api'
import type { ServiceTemplateResponse } from '../../types/servicetemplate'

const CLIENT_TYPE_OPTIONS = [
  { value: 'CUSTOMER', label: 'Cliente (PF)' },
  { value: 'COMPANY', label: 'Empresa (PJ)' },
]

// --- Cláusulas padrão (cláusula 1 vazia, 2–11 do contrato modelo Top Power) ---
interface ClauseDraft {
  rowKey: string
  clauseNumber: number
  title: string
  content: string
  serviceTemplateId: number | ''
}

function nextRowKey(): string {
  return `row_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
}

/** Cláusulas padrão extraídas do contrato modelo (PDF) Top Power mão de obra.
 *  Cláusula 1 (DO OBJETO) fica vazia — o usuário seleciona um ServiceTemplate.
 *  Conteúdos em HTML (<p>) para que as quebras de linha sejam visíveis no RichTextEditor. */
function buildDefaultClauses(): ClauseDraft[] {
  const defaults: Omit<ClauseDraft, 'rowKey'>[] = [
    { clauseNumber: 1, title: 'CLÁUSULA PRIMEIRA - DO OBJETO', content: '', serviceTemplateId: '' },
    {
      clauseNumber: 2,
      title: 'CLÁUSULA SEGUNDA - RESPONSABILIDADE DA CONTRATADA',
      content: '<p>2.1. Fornecimento de mão-de-obra especializada, ferramental, roupa e equipamentos para o bom desempenho dos trabalhos;</p><p>2.2. Fornecimento de transporte e alimentação apropriado para os funcionários;</p><p>2.3. Suporte Técnico Engenheiro com registro ativo no CREA, para supervisão;</p><p>2.4. Sigilo sobre as atividades da BERNARDI HORTO EMPREENDIMENTOS IMOBILIARIOS SPE LTDA.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 3,
      title: 'CLÁUSULA TERCEIRA - RESPONSABILIDADE DA CONTRATANTE',
      content: '<p>3.1. Liberação da área de trabalho, em condições de desenvolver seus serviços em tempo; hábil para o cumprimento do prazo de execução previsto.</p><p>3.2. Fornecimento de documentação técnica.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 4,
      title: 'CLÁUSULA QUARTA - DO PRAZO DE ENTREGA',
      content: '<p>4.1. 120 (CENTO E VINTE DIAS) a partir da autorização e serviço.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 5,
      title: 'CLÁUSULA QUINTA - DOS PREÇOS E FORMA DE PAGAMENTO',
      content: '<p>5.1. A CONTRATANTE pagará ao CONTRATADO, pelos serviços o valor total de R$ 230.800,00 (duzentos e trinta mil, oitocentos reais).</p><p>5.2. Pagamento/Parcelas:</p><p>. 15% na assinatura contrato – R$ 34.620,00</p><p>. 20% 30 DDL - R$ 46.160,00</p><p>. 20% 60 DDL - R$ 46.160,00</p><p>. 25% 90 DDL - R$ 57.700,00</p><p>. 20% 10 DDL após a finalização da obra - R$ 46.160,00</p><p>5.3. Dados para transferência bancária</p><p>Banco do Brasil</p><p>Agencia 990-3</p><p>Conta corrente 117.254-9</p><p>5.4. No valor citado na clausula quinta estão inclusas as despesas com impostos e encargos sociais pertinentes a este contrato. Estamos considerando o recolhimento da ART (Anotação de Responsabilidade Técnica) para a execução dos itens objetos desta proposta.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 6,
      title: 'CLÁUSULA SEXTA - DA VIGÊNCIA',
      content: '<p>6.1. O presente Contrato vigorará durante o período necessário para a elaboração dos serviços descritos na Cláusula Primeira, limitado ao prazo estabelecido na Cláusula Segunda.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 7,
      title: 'CLÁUSULA SETIMA - DA RESCISÃO',
      content: '<p>7.1. Será motivo para rescisão imediata deste contrato o descumprimento de quaisquer de suas cláusulas, devendo a parte infratora arcar com as perdas e danos decorrentes do fato, honorários advocatícios e demais cominações legais.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 8,
      title: 'CLÁUSULA OITAVA - DA MULTA',
      content: '<p>8.1. Caso alguma das partes não cumpra o disposto nas cláusulas estabelecidas neste instrumento, responsabilizar-se-á pelo pagamento de multa equivalente a 20% (vinte por cento) do valor total do objeto do contrato, operando a rescisão automática do presente Contrato com vencimento antecipado das demais parcelas, bem como as perdas e danos, se couber.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 9,
      title: 'CLÁUSULA NONA - DO EXERCÍCIO DOS DIREITOS',
      content: '<p>9.1. Aplicam-se ao presente Contrato as disposições do Código Civil e do Código de Defesa do Consumidor naquilo em que lhe forem compatíveis.</p><p>9.2. Caso seja necessário qualquer outro tipo de serviço técnico em eletricidade, além do objeto descrito no item 1, o mesmo deverá ser discutido antes da execução, cabendo aditivo a este Contrato.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 10,
      title: 'CLÁUSULA DECIMA - DO TÍTULO EXTRA JUDICIAL',
      content: '<p>10.1. O presente contrato constitui título executivo extrajudicial, nos termos do artigo 585, II do Código de processo Civil.</p>',
      serviceTemplateId: '',
    },
    {
      clauseNumber: 11,
      title: 'CLÁUSULA DECIMA PRIMEIRA - DISPOSIÇÕES GERAIS',
      content: '<p>11.1. A CONTRATADA assume a responsabilidade técnica dos serviços a serem executados, declarando, neste ato, que conhece os equipamentos e o local da prestação de serviços – previamente visitado em vistoria técnica realizada pelo Engenheiro responsável.</p><p>11.2. A CONTRATADA se compromete a proteger e preservar o meio ambiente, bem como a prevenir contra as práticas danosas ao ecossistema, executando seus serviços em observância dos atos legais normativos e administrativos relativos à área de meio ambiente e as correlatas emanadas das esferas do Governo Federal, Estadual.</p><p>11.3. As partes de comum acordo elegem o Foro da Comarca de Sumaré/SP para dirimir qualquer lide oriunda do presente Contrato, com renúncia expressa de qualquer outro por mais privilegiado que seja.</p>',
      serviceTemplateId: '',
    },
  ]
  return defaults.map((d) => ({ ...d, rowKey: nextRowKey() }))
}

function clausesFromResponse(clauses: ContractClauseResponse[]): ClauseDraft[] {
  return clauses.map((c) => ({
    rowKey: nextRowKey(),
    clauseNumber: c.clauseNumber,
    title: c.title,
    content: c.content ?? '',
    serviceTemplateId: c.serviceTemplateId ?? '',
  }))
}

interface ContractFormProps {
  /** Contrato existente (modo edição). Quando omitido, é cadastro novo. */
  contract?: ContractResponse
  onSaveCreate: (payload: ContractCreateRequest) => Promise<void>
  onSaveUpdate: (payload: ContractUpdateRequest) => Promise<void>
}

/**
 * Formulário de criação/edição de um contrato.
 *
 * <p>O código comercial ({@code prefix-seq-year}) é gerado pelo servidor e
 * exibido em campo desabilitado. Em cadastro novo, pré-baixamos o próximo
 * código + título padrão via {@code GET /contracts/next-code} para exibir
 * o valor previsto antes do POST. O cliente é selecionado via busca
 * integrada (PF/PJ), similar ao padrão usado em propostas comerciais.
 * O título e a descrição são livremente editáveis; a descrição usa o
 * {@link RichTextEditor} (HTML).</p>
 */
export function ContractForm({
  contract,
  onSaveCreate,
  onSaveUpdate,
}: ContractFormProps) {
  const isEdit = !!contract
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const {
    shouldShowError,
    getBlurHandler,
    markAllTouched,
    reset,
  } = useFieldTouched()

  // --- Código (imutável) e data de vigência (editável) ---
  const [code, setCode] = useState(contract?.code ?? '')
  const [validityDate, setValidityDate] = useState(contract?.validityDate ?? '')

  // --- Cliente (PF/PJ) ---
  const [clientType, setClientType] = useState<ContractClientType>(
    contract?.clientType ?? 'CUSTOMER',
  )
  const [clientId, setClientId] = useState<number | null>(
    contract?.customerId ?? contract?.companyId ?? null,
  )
  const [clientLabel, setClientLabel] = useState<string>('')
  const [clientOptions, setClientOptions] = useState<ClientSummaryResponse[]>([])
  const [clientSearching, setClientSearching] = useState(false)

  // Ref para manter o clientType atual no debounce sem depender do estado.
  const clientTypeRef = useRef(clientType)
  useEffect(() => {
    clientTypeRef.current = clientType
  }, [clientType])

  // --- Título e descrição ---
  const [title, setTitle] = useState(contract?.title ?? '')
  const [description, setDescription] = useState(contract?.description ?? '')
  const [status, setStatus] = useState<RegistrationStatus>(
    contract?.status ?? 'ATIVO',
  )

  // --- Preço (valor informativo, não exibido no PDF) ---
  const [price, setPrice] = useState<string>(
    contract?.price != null ? formatBRLValue(contract.price) : '',
  )

  // --- Cláusulas ---
  const [clauses, setClauses] = useState<ClauseDraft[]>(
    contract?.clauses ? clausesFromResponse(contract.clauses) : buildDefaultClauses(),
  )
  const [serviceTemplates, setServiceTemplates] = useState<ServiceTemplateResponse[]>([])
  const [templatesLoading, setTemplatesLoading] = useState(false)

  // Pré-busca o próximo código + título padrão + data de vigência padrão
  // ao entrar no modo de cadastro. Em edição, os valores já vieram no `contract`.
  useEffect(() => {
    if (contract) return
    let cancelled = false
    getNextContractCode()
      .then((next) => {
        if (cancelled) return
        setCode(next.code)
        setTitle((prev) => (prev.trim() ? prev : next.defaultTitle))
        setValidityDate(next.defaultValidityDate)
        setDescription(next.defaultDescription ?? '')
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [contract])

  // Carrega a lista de ServiceTemplates para a cláusula 1 (DO OBJETO).
  useEffect(() => {
    let cancelled = false
    setTemplatesLoading(true)
    listServiceTemplates({ page: 0, size: 100 })
      .then((res) => {
        if (cancelled) return
        setServiceTemplates(res.content)
      })
      .catch(() => {
        if (!cancelled) setServiceTemplates([])
      })
      .finally(() => {
        if (!cancelled) setTemplatesLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  // Em modo edição, preenche o label do cliente a partir do response.
  useEffect(() => {
    if (contract) {
      setClientLabel(contract.clientName ?? '')
    }
  }, [contract])

  // --- Busca de cliente com debounce ---
  const handleClientQuery = (value: string) => {
    const trimmed = value.trim()
    if (trimmed.length < 2) {
      setClientOptions([])
      return
    }
    setClientSearching(true)
    // Debounce de 300ms
    const timer = setTimeout(async () => {
      try {
        const results = await searchContractClients(
          trimmed,
          20,
          clientTypeRef.current,
        )
        setClientOptions(results)
      } catch {
        setClientOptions([])
      } finally {
        setClientSearching(false)
      }
    }, 300)
    return () => clearTimeout(timer)
  }

  // --- Cláusulas: helpers de manipulação ---
  function updateClause(rowKey: string, patch: Partial<ClauseDraft>) {
    setClauses((prev) =>
      prev.map((c) => (c.rowKey === rowKey ? { ...c, ...patch } : c)),
    )
  }

  function removeClause(rowKey: string) {
    setClauses((prev) => prev.filter((c) => c.rowKey !== rowKey))
  }

  function addClause() {
    setClauses((prev) => [
      ...prev,
      {
        rowKey: nextRowKey(),
        clauseNumber: prev.length + 1,
        title: '',
        content: '',
        serviceTemplateId: '',
      },
    ])
  }

  /** Ao selecionar um ServiceTemplate na cláusula 1, copia a descrição. */
  function handleServiceTemplateSelect(rowKey: string, templateId: string) {
    const id = templateId ? Number(templateId) : ''
    const template = id
      ? serviceTemplates.find((t) => t.id === id) ?? null
      : null
    updateClause(rowKey, {
      serviceTemplateId: id,
      content: template?.description ?? '',
    })
  }

  // --- Validação ---
  function validateAll(): boolean {
    const errs: Record<string, string> = {}
    if (!clientId) {
      errs.client = 'Selecione um cliente.'
    }
    if (!title.trim()) {
      errs.title = 'O título é obrigatório.'
    } else if (title.trim().length > 300) {
      errs.title = 'O título deve ter no máximo 300 caracteres.'
    }
    const priceValue = parseNumber(price)
    if (price.trim() === '' || priceValue == null) {
      errs.price = 'O preço do contrato é obrigatório.'
    } else if (priceValue < 0) {
      errs.price = 'O preço do contrato não pode ser negativo.'
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

    // Mapeia cláusulas para o payload
    const clausesPayload = clauses
      .filter((c) => c.title.trim() !== '')
      .map((c) => ({
        clauseNumber: c.clauseNumber,
        title: c.title.trim(),
        content: c.content || null,
        serviceTemplateId: c.serviceTemplateId !== '' ? Number(c.serviceTemplateId) : null,
      }))

    try {
      const priceValue = parseNumber(price)
      if (isEdit && contract) {
        const payload: ContractUpdateRequest = {
          title: title.trim(),
          description: description ?? '',
          status,
          validityDate: validityDate || undefined,
          price: priceValue ?? 0,
          clauses: clausesPayload,
        }
        // Cliente: envia o ID correto conforme o tipo
        if (clientType === 'CUSTOMER') {
          payload.customerId = clientId ? Number(clientId) : null
          payload.companyId = null
        } else {
          payload.companyId = clientId ? Number(clientId) : null
          payload.customerId = null
        }
        await onSaveUpdate(payload)
        setSuccess('Contrato atualizado com sucesso!')
        reset()
      } else {
        const payload: ContractCreateRequest = {
          title: title.trim(),
          description: description ?? '',
          validityDate: validityDate || undefined,
          price: priceValue ?? 0,
          clauses: clausesPayload,
        }
        if (clientType === 'CUSTOMER') {
          payload.customerId = clientId ? Number(clientId) : null
          payload.companyId = null
        } else {
          payload.companyId = clientId ? Number(clientId) : null
          payload.customerId = null
        }
        await onSaveCreate(payload)
        setSuccess('Contrato criado com sucesso!')
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
      id="contract-form"
      onSubmit={handleSubmit}
      className="flex flex-col gap-6"
      noValidate
    >
      {formError ? <Alert variant="error">{formError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}

      {/* Identificação */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Identificação</h3>

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Código"
            value={code}
            disabled
            readOnly
          />
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">
              Data de vigência
            </label>
            <input
              type="date"
              value={validityDate}
              onChange={(e) => setValidityDate(e.target.value)}
              className={[
                'block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition-[border-color,box-shadow] duration-500 ease-in-out',
                'dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100',
                'focus:border-focus focus:ring-2 focus:ring-focus/30',
                'aria-[invalid]:border-red-500 aria-[invalid]:ring-2 aria-[invalid]:ring-red-500/30',
              ].join(' ')}
            />
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              {isEdit ? 'Pode ser alterada livremente.' : 'Pré-preenchida com a data atual. Pode ser alterada.'}
            </p>
          </div>
          <div className="sm:col-span-2">
            <Input
              label="Título"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onBlur={getBlurHandler('title')}
              error={shouldShowError('title', fieldErrors.title)}
              required
              maxLength={300}
            />
          </div>
        </div>
      </section>

      {/* Cliente */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Cliente</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Selecione o cliente (pessoa física) ou empresa (pessoa jurídica)
          contratada. A busca é feita por nome, código ou documento.
        </p>

        <div className="grid gap-4 sm:grid-cols-[220px_1fr]">
          <Select
            label="Tipo"
            options={CLIENT_TYPE_OPTIONS}
            value={clientType}
            onChange={(e) => {
              setClientType(e.target.value as ContractClientType)
              setClientId(null)
              setClientLabel('')
              setClientOptions([])
            }}
            aria-label="Tipo de cliente"
          />

          <div className="relative">
            <Input
              label={clientType === 'CUSTOMER' ? 'Buscar cliente (PF)' : 'Buscar empresa (PJ)'}
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
              onBlur={getBlurHandler('client')}
              error={shouldShowError('client', fieldErrors.client)}
              rightAdornment={
                clientSearching ? <Spinner size="sm" /> : undefined
              }
            />

            {/* Dropdown de resultados */}
            {clientOptions.length > 0 && !clientId ? (
              <ul className="absolute left-0 top-full z-20 mt-1 w-full rounded-lg border border-slate-200 bg-white shadow-lg dark:border-slate-700 dark:bg-slate-900">
                {clientOptions.map((c) => (
                  <li key={`${c.type}-${c.id}`}>
                    <button
                      type="button"
                      onClick={() => {
                        setClientId(c.id)
                        setClientLabel(c.name)
                        setClientOptions([])
                      }}
                      className="flex w-full items-center gap-3 px-4 py-2.5 text-left text-sm transition-colors hover:bg-slate-50 dark:hover:bg-slate-800"
                    >
                      <span
                        className={[
                          'inline-flex h-5 min-w-[28px] items-center justify-center rounded px-1.5 text-[10px] font-bold uppercase leading-none text-white',
                          c.type === 'CUSTOMER'
                            ? 'bg-blue-500'
                            : 'bg-emerald-600',
                        ].join(' ')}
                      >
                        {c.type === 'CUSTOMER' ? 'PF' : 'PJ'}
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="truncate font-medium text-slate-900 dark:text-slate-100">
                          {c.name}
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-400">
                          {c.code} &middot; {c.document}
                        </div>
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
        </div>

        {clientId && clientLabel ? (
          <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
            Cliente selecionado: <strong>{clientLabel}</strong>
          </p>
        ) : null}
      </section>

      {/* Preço (valor informativo, não exibido no PDF) */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-4 text-base font-semibold">Preço do contrato</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Preço do contrato, de preenchimento obrigatório. Valor informativo
          para controle interno — <strong>não</strong> é exibido no PDF do
          contrato (o valor comercial deve constar nas cláusulas).
        </p>
        <div className="max-w-xs">
          <Input
            label="Preço do contrato"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            onBlur={() => {
              const trimmed = price.trim()
              if (trimmed) {
                // Se não tem vírgula, adiciona ",00"
                if (!trimmed.includes(',')) {
                  setPrice(trimmed + ',00')
                } else {
                  // Se tem vírgula mas não tem centavos, completa
                  const parts = trimmed.split(',')
                  if (parts.length === 2 && parts[1].length === 0) {
                    setPrice(trimmed + '00')
                  } else if (parts.length === 2 && parts[1].length === 1) {
                    setPrice(trimmed + '0')
                  }
                }
              }
              getBlurHandler('price')()
            }}
            error={shouldShowError('price', fieldErrors.price)}
            placeholder="Ex.: 1.500,00"
            required
            hint="Obrigatório — não aparece no PDF"
          />
        </div>
      </section>

      {/* Descrição */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Descrição do contrato</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Texto livre com o conteúdo detalhado do contrato. Suporta
          formatação rica (negrito, itálico, listas, cores). Pode ser
          pré-preenchido com o template padrão da empresa ativa.
        </p>
        <RichTextEditor
          value={description ?? ''}
          onChange={setDescription}
          placeholder="Descreva as cláusulas e condições do contrato…"
          maxLength={20000}
          minHeight={320}
        />
      </section>

      {/* Cláusulas */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h3 className="text-base font-semibold">Cláusulas do contrato</h3>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              As cláusulas 2–11 vêm pré-preenchidas com o contrato modelo Top Power.
              A cláusula 1 (DO OBJETO) é preenchida ao selecionar um serviço do catálogo.
            </p>
          </div>
          <Button type="button" variant="secondary" size="sm" onClick={addClause}>
            <Plus className="h-4 w-4" />
            Adicionar
          </Button>
        </div>

        <div className="flex flex-col gap-6">
          {clauses.map((clause, idx) => (
            <div
              key={clause.rowKey}
              className="rounded-xl border border-slate-200 p-4 dark:border-slate-700"
            >
              {/* Cabeçalho: número + título + remover */}
              <div className="mb-3 flex items-start gap-3">
                <div className="flex h-8 w-12 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-sm font-bold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                  {String(clause.clauseNumber).padStart(2, '0')}
                </div>
                <Input
                  placeholder="Título da cláusula (ex.: DO OBJETO)"
                  value={clause.title}
                  onChange={(e) => updateClause(clause.rowKey, { title: e.target.value })}
                  maxLength={200}
                />
                <button
                  type="button"
                  onClick={() => removeClause(clause.rowKey)}
                  aria-label="Remover cláusula"
                  title="Remover cláusula"
                  className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-red-600 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-red-400"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>

              {/* Cláusula 1: seletor de ServiceTemplate */}
              {clause.clauseNumber === 1 ? (
                <div className="mb-3">
                  <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-300">
                    Serviço do catálogo (ServiceTemplate)
                  </label>
                  {templatesLoading ? (
                    <div className="flex items-center gap-2 text-sm text-slate-500">
                      <Spinner size="sm" /> Carregando serviços…
                    </div>
                  ) : (
                    <select
                      value={clause.serviceTemplateId !== '' ? String(clause.serviceTemplateId) : ''}
                      onChange={(e) => handleServiceTemplateSelect(clause.rowKey, e.target.value)}
                      className="h-9 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-900 outline-none focus:border-focus focus:ring-2 focus:ring-focus/30 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                    >
                      <option value="">Selecione um serviço do catálogo…</option>
                      {serviceTemplates.map((t) => (
                        <option key={t.id} value={String(t.id)}>
                          {t.name}
                        </option>
                      ))}
                    </select>
                  )}
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                    Ao selecionar, a descrição do serviço é copiada para o texto da cláusula.
                  </p>
                </div>
              ) : null}

              {/* Texto da cláusula */}
              <RichTextEditor
                value={clause.content}
                onChange={(html) => updateClause(clause.rowKey, { content: html })}
                placeholder="Texto da cláusula…"
                maxLength={20000}
                minHeight={idx === 0 ? 200 : 120}
              />
            </div>
          ))}

          {clauses.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-300 p-8 text-center text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
              Nenhuma cláusula. Clique em "Adicionar" para incluir.
            </div>
          ) : null}
        </div>
      </section>

      {/* Status */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Status</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Define se o contrato está vigente. Contratos inativos permanecem
          no cadastro para fins de histórico.
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