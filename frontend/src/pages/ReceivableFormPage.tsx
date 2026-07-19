import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  Check,
  DollarSign,
  Trash2,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { ReceivableStatusBadge } from '../components/receivable/ReceivableStatusBadge'
import {
  activateReceivable,
  cancelReceivable,
  createReceivable,
  getReceivable,
  registerPayment,
  removePayment,
  updateReceivable,
} from '../api/receivable.api'
import { searchContractClients } from '../api/contract.api'
import type {
  ReceivableClientType,
  ReceivableCreateRequest,
  ReceivablePaymentRequest,
  ReceivableResponse,
  ReceivableSource,
  ReceivableUpdateRequest,
} from '../types/receivable'
import type { ClientSummaryResponse } from '../types/contract'
import { formatBRLValue, parseNumber } from '../lib/money'
import { toApiError } from '../lib/errors'

const CLIENT_TYPE_OPTIONS = [
  { value: 'CUSTOMER', label: 'Cliente (PF)' },
  { value: 'COMPANY', label: 'Empresa (PJ)' },
]

const SOURCE_LABEL: Record<ReceivableSource, string> = {
  MANUAL: 'Manual',
  SALES_ORDER: 'Pedido de venda',
  TECHNICAL_PROPOSAL: 'Proposta técnica',
  CONTRACT: 'Contrato',
}

function todayISO(): string {
  return new Date().toISOString().slice(0, 10)
}

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  })
}

interface FieldErrors {
  description?: string
  value?: string
  dueDate?: string
  client?: string
  paymentAmount?: string
  paymentDate?: string
}

export function ReceivableFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = !!id

  // --- Detalhe (modo edição/visualização) ---
  const [receivable, setReceivable] = useState<ReceivableResponse | null>(null)
  const [loadingDetail, setLoadingDetail] = useState(isEdit)
  const [detailError, setDetailError] = useState<string | null>(null)

  // --- Form (modo criação manual OU edição de campos editáveis) ---
  const [description, setDescription] = useState('')
  const [valueStr, setValueStr] = useState('')
  const [dueDate, setDueDate] = useState(todayISO())
  const [paymentCondition, setPaymentCondition] = useState('')
  const [clientType, setClientType] = useState<ReceivableClientType>('CUSTOMER')
  const [clientId, setClientId] = useState<number | null>(null)
  const [clientLabel, setClientLabel] = useState('')
  const [clientOptions, setClientOptions] = useState<ClientSummaryResponse[]>([])
  const [clientSearching, setClientSearching] = useState(false)
  const [touched, setTouched] = useState<Record<string, boolean>>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const clientTypeRef = useRef(clientType)
  useEffect(() => {
    clientTypeRef.current = clientType
  }, [clientType])

  // --- Modal de pagamento ---
  const [paymentOpen, setPaymentOpen] = useState(false)
  const [paymentAmount, setPaymentAmount] = useState('')
  const [paymentDate, setPaymentDate] = useState(todayISO())
  const [paymentNotes, setPaymentNotes] = useState('')
  const [paymentSubmitting, setPaymentSubmitting] = useState(false)
  const [paymentError, setPaymentError] = useState<string | null>(null)

  // --- Modal de remover pagamento ---
  const [removePayTarget, setRemovePayTarget] =
    useState<{ paymentId: number; amount: number } | null>(null)
  const [removingPay, setRemovingPay] = useState(false)

  // --- Modal de cancelar/reativar conta ---
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [confirmActivate, setConfirmActivate] = useState(false)
  const [toggling, setToggling] = useState(false)

  // --- Carrega detalhe ---
  useEffect(() => {
    if (!isEdit || !id) return
    setLoadingDetail(true)
    getReceivable(Number(id))
      .then((r) => {
        setReceivable(r)
        setDescription(r.description)
        setValueStr(formatBRLValue(r.value))
        setDueDate(r.dueDate)
        setPaymentCondition(r.paymentCondition ?? '')
        setClientId(r.customerId ?? r.companyId ?? null)
        setClientLabel(r.clientName ?? '')
        setClientType(r.customerId ? 'CUSTOMER' : 'COMPANY')
      })
      .catch((err) => setDetailError(toApiError(err).message))
      .finally(() => setLoadingDetail(false))
  }, [id, isEdit])

  // --- Busca de cliente (debounce) ---
  function handleClientQuery(value: string) {
    const trimmed = value.trim()
    if (trimmed.length < 2) {
      setClientOptions([])
      return
    }
    setClientSearching(true)
    const timer = setTimeout(async () => {
      try {
        const results = await searchContractClients(trimmed, 20, clientTypeRef.current)
        setClientOptions(results)
      } catch {
        setClientOptions([])
      } finally {
        setClientSearching(false)
      }
    }, 300)
    return () => clearTimeout(timer)
  }

  function validate(): FieldErrors {
    const errs: FieldErrors = {}
    if (!description.trim()) errs.description = 'Descrição é obrigatória.'
    const v = parseNumber(valueStr)
    if (v == null || v <= 0) errs.value = 'Valor deve ser maior que zero.'
    if (!dueDate) errs.dueDate = 'Data de vencimento é obrigatória.'
    if (!clientId) errs.client = 'Selecione um cliente ou empresa.'
    return errs
  }

  const fieldErrors = validate()
  const shouldShowError = (field: keyof FieldErrors, msg?: string) =>
    (touched[field] || submitting) && !!msg

  const getBlurHandler = (field: keyof FieldErrors) =>
    () => setTouched((t) => ({ ...t, [field]: true }))

  async function handleSubmit() {
    setSubmitting(true)
    setSubmitError(null)
    setTouched({ description: true, value: true, dueDate: true, client: true })
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setSubmitting(false)
      return
    }
    const value = parseNumber(valueStr) ?? 0
    try {
      if (isEdit && id) {
        const payload: ReceivableUpdateRequest = {
          description,
          dueDate,
          paymentCondition: paymentCondition || null,
        }
        const updated = await updateReceivable(Number(id), payload)
        setReceivable(updated)
        setTouched({})
      } else {
        const payload: ReceivableCreateRequest = {
          description,
          value,
          dueDate,
          customerId: clientType === 'CUSTOMER' ? clientId : null,
          companyId: clientType === 'COMPANY' ? clientId : null,
          paymentCondition: paymentCondition || null,
        }
        const created = await createReceivable(payload)
        navigate(`/receivables/${created.id}`)
      }
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRegisterPayment() {
    if (!receivable) return
    const amount = parseNumber(paymentAmount)
    if (amount == null || amount <= 0) {
      setPaymentError('Valor do pagamento inválido.')
      return
    }
    if (!paymentDate) {
      setPaymentError('Data do pagamento é obrigatória.')
      return
    }
    setPaymentSubmitting(true)
    setPaymentError(null)
    try {
      const payload: ReceivablePaymentRequest = {
        amount,
        paymentDate,
        notes: paymentNotes || null,
      }
      const updated = await registerPayment(receivable.id, payload)
      setReceivable(updated)
      setPaymentOpen(false)
      setPaymentAmount('')
      setPaymentNotes('')
      setPaymentDate(todayISO())
    } catch (err) {
      setPaymentError(toApiError(err).message)
    } finally {
      setPaymentSubmitting(false)
    }
  }

  async function handleRemovePayment() {
    if (!receivable || !removePayTarget) return
    setRemovingPay(true)
    try {
      const updated = await removePayment(receivable.id, removePayTarget.paymentId)
      setReceivable(updated)
      setRemovePayTarget(null)
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setRemovingPay(false)
    }
  }

  async function handleCancel() {
    if (!receivable) return
    setToggling(true)
    try {
      await cancelReceivable(receivable.id)
      const refreshed = await getReceivable(receivable.id)
      setReceivable(refreshed)
      setConfirmCancel(false)
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setToggling(false)
    }
  }

  async function handleActivate() {
    if (!receivable) return
    setToggling(true)
    try {
      const refreshed = await activateReceivable(receivable.id)
      setReceivable(refreshed)
      setConfirmActivate(false)
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setToggling(false)
    }
  }

  if (loadingDetail) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner /> <span className="ml-2 text-slate-500">Carregando conta…</span>
      </div>
    )
  }
  if (detailError && !receivable) {
    return (
      <div className="space-y-4">
        <Link to="/receivables" className="inline-flex items-center gap-1 text-sm text-primary hover:underline">
          <ArrowLeft className="h-4 w-4" /> Voltar à lista
        </Link>
        <Alert variant="error">{detailError}</Alert>
      </div>
    )
  }

  const readOnly = receivable != null && receivable.sourceType !== 'MANUAL'
  const canEdit = receivable == null || receivable.status === 'ABERTO'
  const balance = receivable?.balance ?? parseNumber(valueStr) ?? 0

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <Link
            to="/receivables"
            className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
          >
            <ArrowLeft className="h-4 w-4" /> Contas a Receber
          </Link>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">
            {isEdit ? 'Conta a Receber' : 'Nova Conta a Receber'}
          </h1>
          {receivable ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Origem: {SOURCE_LABEL[receivable.sourceType]}
              {receivable.contractCode ? ` • ${receivable.contractCode}` : ''}
              {receivable.technicalProposalCode ? ` • ${receivable.technicalProposalCode}` : ''}
              {receivable.salesOrderNumber ? ` • PV ${receivable.salesOrderNumber}` : ''}
            </p>
          ) : (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Cadastro manual de um recebimento.
            </p>
          )}
        </div>
        {receivable ? <ReceivableStatusBadge status={receivable.status} /> : null}
      </div>

      {submitError ? <Alert variant="error">{submitError}</Alert> : null}

      {/* Painel de totais (apenas em modo detalhe) */}
      {receivable ? (
        <div className="grid gap-3 sm:grid-cols-3">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="text-xs uppercase tracking-wide text-slate-500">Valor total</div>
            <div className="mt-1 text-xl font-semibold text-slate-900 dark:text-slate-100">
              R$ {formatBRLValue(receivable.value)}
            </div>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="text-xs uppercase tracking-wide text-slate-500">Recebido</div>
            <div className="mt-1 text-xl font-semibold text-emerald-700 dark:text-emerald-400">
              R$ {formatBRLValue(receivable.paidAmount)}
            </div>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="text-xs uppercase tracking-wide text-slate-500">Saldo devedor</div>
            <div className="mt-1 text-xl font-semibold text-slate-900 dark:text-slate-100">
              R$ {formatBRLValue(receivable.balance)}
            </div>
          </div>
        </div>
      ) : null}

      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h2 className="mb-4 text-base font-semibold">Dados da conta</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Input
              label="Descrição"
              required
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              onBlur={getBlurHandler('description')}
              error={shouldShowError('description', fieldErrors.description) ? fieldErrors.description : null}
              disabled={readOnly || !canEdit}
              maxLength={300}
            />
          </div>
          <Input
            label="Valor total (R$)"
            required
            value={valueStr}
            onChange={(e) => setValueStr(e.target.value)}
            onBlur={getBlurHandler('value')}
            error={shouldShowError('value', fieldErrors.value) ? fieldErrors.value : null}
            disabled={isEdit}
            placeholder="0,00"
          />
          <Input
            label="Data de vencimento"
            required
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            onBlur={getBlurHandler('dueDate')}
            error={shouldShowError('dueDate', fieldErrors.dueDate) ? fieldErrors.dueDate : null}
            disabled={!canEdit}
          />
          <Input
            label="Condição de pagamento"
            value={paymentCondition}
            onChange={(e) => setPaymentCondition(e.target.value)}
            placeholder="Ex.: 30/60/90"
            maxLength={100}
            disabled={readOnly || !canEdit}
          />

          {/* Cliente */}
          <div className="sm:col-span-2">
            <Select
              label="Tipo de cliente"
              options={CLIENT_TYPE_OPTIONS}
              value={clientType}
              onChange={(e) => {
                setClientType(e.target.value as ReceivableClientType)
                setClientId(null)
                setClientLabel('')
                setClientOptions([])
              }}
              disabled={isEdit}
              aria-label="Tipo de cliente"
            />
          </div>
          <div className="sm:col-span-2">
            <Input
              label={clientType === 'CUSTOMER' ? 'Buscar cliente (PF)' : 'Buscar empresa (PJ)'}
              placeholder={
                clientType === 'CUSTOMER'
                  ? 'Buscar por nome ou código…'
                  : 'Buscar por nome fantasia ou código…'
              }
              value={clientLabel}
              onChange={(e) => {
                setClientLabel(e.target.value)
                setClientId(null)
                handleClientQuery(e.target.value)
              }}
              onBlur={getBlurHandler('client')}
              error={shouldShowError('client', fieldErrors.client) ? fieldErrors.client : null}
              disabled={isEdit}
              rightAdornment={clientSearching ? <Spinner size="sm" /> : undefined}
            />
            {clientOptions.length > 0 && !clientId ? (
              <ul className="mt-1 max-h-56 overflow-auto rounded-lg border border-slate-200 bg-white text-sm dark:border-slate-700 dark:bg-slate-900">
                {clientOptions.map((c) => (
                  <li key={`${c.type}-${c.id}`}>
                    <button
                      type="button"
                      className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left hover:bg-slate-50 dark:hover:bg-slate-800"
                      onClick={() => {
                        setClientId(c.id)
                        setClientLabel(c.name)
                        setClientOptions([])
                      }}
                    >
                      <span className="text-slate-900 dark:text-slate-100">{c.name}</span>
                      <span className="text-xs text-slate-500">{c.code}</span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
            {clientId && clientLabel ? (
              <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                Selecionado: <strong>{clientLabel}</strong>
              </p>
            ) : null}
          </div>
        </div>
      </div>

      {/* Histórico de pagamentos (apenas detalhe) */}
      {receivable ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-base font-semibold">Pagamentos</h2>
            {receivable.status === 'ABERTO' ? (
              <Button size="sm" onClick={() => setPaymentOpen(true)}>
                <DollarSign className="h-4 w-4" />
                Registrar pagamento
              </Button>
            ) : null}
          </div>
          {receivable.payments.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Nenhum pagamento registrado. Saldo devedor: R$ {formatBRLValue(balance)}.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                  <tr>
                    <th className="px-3 py-2 font-medium">Data</th>
                    <th className="px-3 py-2 text-right font-medium">Valor</th>
                    <th className="px-3 py-2 font-medium">Observações</th>
                    <th className="px-3 py-2 text-right font-medium">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                  {receivable.payments.map((p) => (
                    <tr key={p.id}>
                      <td className="whitespace-nowrap px-3 py-2 text-slate-700 dark:text-slate-200">
                        {formatDate(p.paymentDate)}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-right text-emerald-700 dark:text-emerald-400">
                        R$ {formatBRLValue(p.amount)}
                      </td>
                      <td className="px-3 py-2 text-slate-600 dark:text-slate-300">
                        {p.notes ?? '—'}
                      </td>
                      <td className="px-3 py-2 text-right">
                        {receivable.status !== 'CANCELADO' ? (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => setRemovePayTarget({ paymentId: p.id, amount: p.amount })}
                            title="Remover pagamento"
                            aria-label="Remover pagamento"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        ) : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : null}

      {/* Action bar */}
      <div className="sticky bottom-0 z-10 flex flex-wrap items-center justify-end gap-2 rounded-2xl border border-slate-200 bg-white/95 p-4 shadow-sm backdrop-blur dark:border-slate-800 dark:bg-slate-900/95">
        <Link to="/receivables">
          <Button variant="secondary" type="button">Cancelar</Button>
        </Link>
        {receivable && receivable.status === 'ABERTO' ? (
          <Button variant="danger" type="button" onClick={() => setConfirmCancel(true)}>
            Cancelar conta
          </Button>
        ) : null}
        {receivable && receivable.status === 'CANCELADO' ? (
          <Button variant="secondary" type="button" onClick={() => setConfirmActivate(true)}>
            Reativar conta
          </Button>
        ) : null}
        {!readOnly && canEdit ? (
          <Button onClick={handleSubmit} isLoading={submitting}>
            <Check className="h-4 w-4" />
            {isEdit ? 'Salvar alterações' : 'Criar conta'}
          </Button>
        ) : null}
        {readOnly ? (
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Conta gerada automaticamente — apenas campos editáveis podem ser alterados.
          </span>
        ) : null}
      </div>

      {/* Modal de registrar pagamento */}
      {paymentOpen ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 backdrop-blur-sm"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget && !paymentSubmitting) setPaymentOpen(false)
          }}
        >
          <div className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900">
            <div className="border-b border-slate-200 px-5 py-4 dark:border-slate-800">
              <h2 className="text-base font-semibold">Registrar pagamento</h2>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                Saldo devedor atual: R$ {formatBRLValue(balance)}.
              </p>
            </div>
            <div className="space-y-4 px-5 py-4">
              {paymentError ? <Alert variant="error">{paymentError}</Alert> : null}
              <Input
                label="Valor do pagamento (R$)"
                required
                value={paymentAmount}
                onChange={(e) => setPaymentAmount(e.target.value)}
                placeholder="0,00"
              />
              <Input
                label="Data do pagamento"
                required
                type="date"
                value={paymentDate}
                onChange={(e) => setPaymentDate(e.target.value)}
              />
              <Input
                label="Observações"
                value={paymentNotes}
                onChange={(e) => setPaymentNotes(e.target.value)}
                placeholder="Ex.: PIX, transferência…"
                maxLength={500}
              />
            </div>
            <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4 dark:border-slate-800">
              <Button variant="secondary" onClick={() => setPaymentOpen(false)} disabled={paymentSubmitting}>
                Cancelar
              </Button>
              <Button onClick={handleRegisterPayment} isLoading={paymentSubmitting}>
                Registrar
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      <ConfirmDialog
        open={removePayTarget != null}
        title="Remover pagamento?"
        description={
          removePayTarget
            ? `O pagamento de R$ ${formatBRLValue(removePayTarget.amount)} será removido e o saldo devedor recalculado.`
            : ''
        }
        confirmText="Remover"
        confirmVariant="danger"
        isLoading={removingPay}
        onConfirm={handleRemovePayment}
        onClose={() => {
          if (!removingPay) setRemovePayTarget(null)
        }}
      />

      <ConfirmDialog
        open={confirmCancel}
        title="Cancelar conta a receber?"
        description="A conta será marcada como CANCELADA e poderá ser reativada depois."
        confirmText="Cancelar conta"
        confirmVariant="danger"
        isLoading={toggling}
        onConfirm={handleCancel}
        onClose={() => {
          if (!toggling) setConfirmCancel(false)
        }}
      />

      <ConfirmDialog
        open={confirmActivate}
        title="Reativar conta a receber?"
        description="A conta voltará a ficar ativa (ou paga, se já quitada)."
        confirmText="Reativar"
        confirmVariant="primary"
        isLoading={toggling}
        onConfirm={handleActivate}
        onClose={() => {
          if (!toggling) setConfirmActivate(false)
        }}
      />
    </div>
  )
}