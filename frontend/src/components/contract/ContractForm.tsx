import { useEffect, useRef, useState, type FormEvent } from 'react'
import type {
  ClientSummaryResponse,
  ContractClientType,
  ContractCreateRequest,
  ContractResponse,
  ContractUpdateRequest,
  RegistrationStatus,
} from '../../types/contract'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Alert } from '../ui/Alert'
import { Spinner } from '../ui/Spinner'
import { RichTextEditor } from '../ui/RichTextEditor'
import { toApiError } from '../../lib/errors'
import { useFieldTouched } from '../../hooks/useFieldTouched'
import { getNextContractCode, searchContractClients } from '../../api/contract.api'

const CLIENT_TYPE_OPTIONS = [
  { value: 'CUSTOMER', label: 'Cliente (PF)' },
  { value: 'COMPANY', label: 'Empresa (PJ)' },
]

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
    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
    markAllTouched()
    if (!validateAll()) return

    try {
      if (isEdit && contract) {
        const payload: ContractUpdateRequest = {
          title: title.trim(),
          description: description ?? '',
          status,
          validityDate: validityDate || undefined,
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
        />
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