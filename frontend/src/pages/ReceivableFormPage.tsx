import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  Check,
  DollarSign,
  Layers,
  Trash2,
} from 'lucide-react'
import { BackButton } from '../components/ui/BackButton'
import { PAYMENT_CONDITION_OPTIONS } from '../types/quotation'
import type { PaymentCondition } from '../types/quotation'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { ReceivableStatusBadge } from '../components/receivable/ReceivableStatusBadge'
import { ReceivablePaymentModal } from '../components/receivable/ReceivablePaymentModal'
import {
  activateReceivable,
  cancelReceivable,
  createReceivable,
  generateInstallments,
  getReceivable,
  previewInstallments,
  removePayment,
  settleInstallment,
  updateReceivable,
} from '../api/receivable.api'
import { searchContractClients } from '../api/contract.api'
import type {
  ReceivableClientType,
  ReceivableCreateRequest,
  ReceivableInstallmentPreviewResponse,
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
  const [paymentCondition, setPaymentCondition] = useState<PaymentCondition | ''>(
    '',
  )
  const [clientType, setClientType] = useState<ReceivableClientType>('CUSTOMER')
  const [clientId, setClientId] = useState<number | null>(null)
  const [clientLabel, setClientLabel] = useState('')
  const [clientOptions, setClientOptions] = useState<ClientSummaryResponse[]>([])
  const [clientSearching, setClientSearching] = useState(false)
  const [touched, setTouched] = useState<Record<string, boolean>>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  // --- Preview de parcelas (modo criação) ---
  const [preview, setPreview] = useState<ReceivableInstallmentPreviewResponse[]>([])
  const [previewLoading, setPreviewLoading] = useState(false)

  const clientTypeRef = useRef(clientType)
  useEffect(() => {
    clientTypeRef.current = clientType
  }, [clientType])

  // --- Modal de pagamento ---
  const [paymentOpen, setPaymentOpen] = useState(false)

  // --- Modal de liquidar parcela ---
  const [settleInstallmentTarget, setSettleInstallmentTarget] = useState<
    { installmentId: number; installmentNumber: number; balance: number } | null
  >(null)
  const [settlingInst, setSettlingInst] = useState(false)

  // --- Modal de Gerar parcelas (ação pós-criação) ---
  const [generateOpen, setGenerateOpen] = useState(false)
  const [generating, setGenerating] = useState(false)

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

  // --- Preview de parcelas (modo criação) ---
  // Sempre que a condição ou o valor mudarem (e houver condição), busca o
  // preview no backend. Usa a data de hoje como base.
  const previewValue = useMemo(() => parseNumber(valueStr), [valueStr])
  useEffect(() => {
    if (isEdit || !paymentCondition || previewValue == null || previewValue <= 0) {
      setPreview([])
      return
    }
    let cancelled = false
    setPreviewLoading(true)
    const timer = setTimeout(async () => {
      try {
        const result = await previewInstallments({
          paymentCondition,
          value: previewValue,
          baseDate: todayISO(),
        })
        if (!cancelled) setPreview(result)
      } catch {
        if (!cancelled) setPreview([])
      } finally {
        if (!cancelled) setPreviewLoading(false)
      }
    }, 350)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [isEdit, paymentCondition, previewValue])

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
        // Na criação, deixamos o backend gerar as parcelas a partir da
        // condição (quando parcelada) ou criar 1 parcela à vista.
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

  async function handleSettleInstallment() {
    if (!receivable || !settleInstallmentTarget) return
    setSettlingInst(true)
    try {
      const updated = await settleInstallment(
        receivable.id,
        settleInstallmentTarget.installmentId,
      )
      setReceivable(updated)
      setSettleInstallmentTarget(null)
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setSettlingInst(false)
    }
  }

  async function handleGenerateInstallments() {
    if (!receivable) return
    setGenerating(true)
    try {
      const updated = await generateInstallments(receivable.id, {
        paymentCondition: receivable.paymentCondition ?? null,
      })
      setReceivable(updated)
      setGenerateOpen(false)
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setGenerating(false)
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
        <BackButton variant="ghost" label="Voltar para a lista" fallback="/receivables" />
        <Alert variant="error">{detailError}</Alert>
      </div>
    )
  }

  const readOnly = receivable != null && receivable.sourceType !== 'MANUAL'
  const canEdit = receivable == null || receivable.status === 'ABERTO'
  // Botão "Gerar parcelas" aparece quando a conta é ABERTO, tem apenas 1
  // parcela (à vista), nenhum pagamento registrado e possui condição de
  // pagamento parcelada (ou qualquer condição — o backend valida).
  const canGenerateInstallments =
    receivable != null &&
    receivable.status === 'ABERTO' &&
    receivable.installmentsCount === 1 &&
    receivable.paidAmount === 0 &&
    receivable.payments.length === 0 &&
    receivable.paymentCondition != null

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <BackButton />
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">
            {isEdit ? 'Conta a Receber' : 'Nova Conta a Receber'}
          </h1>
          {receivable ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Origem: {SOURCE_LABEL[receivable.sourceType]}
              {receivable.contractCode ? ` • ${receivable.contractCode}` : ''}
              {receivable.technicalProposalCode ? ` • ${receivable.technicalProposalCode}` : ''}
              {receivable.salesOrderNumber ? ` • PV ${receivable.salesOrderNumber}` : ''}
              {receivable.installmentsCount > 1
                ? ` • ${receivable.installmentsCount} parcela(s)`
                : ''}
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
            label="Data de vencimento (1ª parcela)"
            required
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            onBlur={getBlurHandler('dueDate')}
            error={shouldShowError('dueDate', fieldErrors.dueDate) ? fieldErrors.dueDate : null}
            disabled={!canEdit}
            hint={
              !isEdit
                ? 'Vencimento-base usado quando a condição de pagamento não é informada (à vista).'
                : undefined
            }
          />
          <Select
            label="Condição de pagamento"
            value={paymentCondition}
            onChange={(e) =>
              setPaymentCondition(e.target.value as PaymentCondition | '')
            }
            options={[
              { value: '', label: 'Selecione...' },
              ...PAYMENT_CONDITION_OPTIONS,
            ]}
            aria-label="Condição de pagamento"
            disabled={readOnly || !canEdit}
            hint={
              !isEdit
                ? 'Quando informada e com múltiplos prazos, as parcelas são geradas automaticamente a partir dos prazos.'
                : undefined
            }
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

        {/* Preview de parcelas (apenas na criação, quando há condição parcelada) */}
        {!isEdit && (preview.length > 1 || previewLoading) ? (
          <div className="mt-6">
            <div className="mb-2 flex items-center justify-between">
              <h3 className="text-sm font-semibold">Preview das parcelas</h3>
              {previewLoading ? <Spinner size="sm" /> : null}
            </div>
            <p className="mb-3 text-xs text-slate-500 dark:text-slate-400">
              Parcelas que serão geradas a partir da condição de pagamento
              informada (base: hoje).
            </p>
            {preview.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                  <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                    <tr>
                      <th className="px-3 py-2 font-medium">Parcela</th>
                      <th className="px-3 py-2 text-right font-medium">Valor</th>
                      <th className="px-3 py-2 font-medium">Vencimento</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                    {preview.map((p) => (
                      <tr key={p.installmentNumber}>
                        <td className="px-3 py-2 text-slate-700 dark:text-slate-200">
                          {p.installmentNumber}
                        </td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-slate-900 dark:text-slate-100">
                          R$ {formatBRLValue(p.amount)}
                        </td>
                        <td className="whitespace-nowrap px-3 py-2 text-slate-600 dark:text-slate-300">
                          {formatDate(p.dueDate)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </div>
        ) : null}
      </div>

      {/* Parcelas programadas + pagamentos (apenas detalhe) */}
      {receivable ? (
        <div className="space-y-6">
          {/* Parcelas */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-base font-semibold">Parcelas</h2>
              <div className="flex items-center gap-2">
                {canGenerateInstallments ? (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => setGenerateOpen(true)}
                  >
                    <Layers className="h-4 w-4" />
                    Gerar parcelas
                  </Button>
                ) : null}
                {receivable.status === 'ABERTO' ? (
                  <Button size="sm" onClick={() => setPaymentOpen(true)}>
                    <DollarSign className="h-4 w-4" />
                    Registrar pagamento
                  </Button>
                ) : null}
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                  <tr>
                    <th className="px-3 py-2 font-medium">Parcela</th>
                    <th className="px-3 py-2 text-right font-medium">Valor</th>
                    <th className="px-3 py-2 text-right font-medium">Pago</th>
                    <th className="px-3 py-2 text-right font-medium">Saldo</th>
                    <th className="px-3 py-2 font-medium">Vencimento</th>
                    <th className="px-3 py-2 font-medium">Status</th>
                    <th className="px-3 py-2 text-right font-medium">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                  {receivable.installments.map((inst) => (
                    <tr key={inst.id}>
                      <td className="px-3 py-2 text-slate-700 dark:text-slate-200">
                        {inst.installmentNumber}/{receivable.installmentsCount}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-right text-slate-900 dark:text-slate-100">
                        R$ {formatBRLValue(inst.amount)}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-right text-emerald-700 dark:text-emerald-400">
                        R$ {formatBRLValue(inst.paidAmount)}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-slate-900 dark:text-slate-100">
                        R$ {formatBRLValue(inst.balance)}
                      </td>
                      <td className="whitespace-nowrap px-3 py-2 text-slate-600 dark:text-slate-300">
                        {formatDate(inst.dueDate)}
                      </td>
                      <td className="px-3 py-2">
                        <ReceivableStatusBadge status={inst.status} />
                      </td>
                      <td className="px-3 py-2 text-right">
                        {inst.status === 'ABERTO' && receivable.status === 'ABERTO' ? (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() =>
                              setSettleInstallmentTarget({
                                installmentId: inst.id,
                                installmentNumber: inst.installmentNumber,
                                balance: inst.balance,
                              })
                            }
                            title="Liquidar parcela"
                            aria-label="Liquidar parcela"
                            className="!text-emerald-600 hover:!text-emerald-600 dark:!text-emerald-500 dark:hover:!text-emerald-500"
                          >
                            <Check className="h-4 w-4" />
                          </Button>
                        ) : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Histórico de pagamentos */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h2 className="mb-4 text-base font-semibold">Pagamentos</h2>
            {receivable.payments.length === 0 ? (
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Nenhum pagamento registrado. Saldo devedor: R$ {formatBRLValue(receivable.balance)}.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                  <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                    <tr>
                      <th className="px-3 py-2 font-medium">Data</th>
                      <th className="px-3 py-2 font-medium">Parcela</th>
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
                        <td className="px-3 py-2 text-slate-600 dark:text-slate-300">
                          {p.installmentNumber || '—'}
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
      <ReceivablePaymentModal
        receivable={receivable}
        open={paymentOpen}
        onClose={() => setPaymentOpen(false)}
        onSuccess={(updated) => setReceivable(updated)}
      />

      <ConfirmDialog
        open={settleInstallmentTarget != null}
        title="Liquidar parcela?"
        description={
          settleInstallmentTarget
            ? `Será registrado um pagamento cobrindo todo o saldo devedor (R$ ${settleInstallmentTarget.balance.toFixed(2).replace('.', ',')}) da parcela ${settleInstallmentTarget.installmentNumber}, transitando-a para PAGO.`
            : ''
        }
        confirmText="Liquidar parcela"
        confirmVariant="primary"
        isLoading={settlingInst}
        onConfirm={handleSettleInstallment}
        onClose={() => {
          if (!settlingInst) setSettleInstallmentTarget(null)
        }}
      />

      <ConfirmDialog
        open={generateOpen}
        title="Gerar parcelas?"
        description={
          receivable?.paymentCondition
            ? `A conta será particionada em parcelas a partir da condição de pagamento "${receivable.paymentCondition}". A parcela única atual (sem pagamentos) será substituída. Esta operação não pode ser desfeita a partir da UI.`
            : 'A conta será particionada em parcelas a partir da condição de pagamento.'
        }
        confirmText="Gerar parcelas"
        confirmVariant="primary"
        isLoading={generating}
        onConfirm={handleGenerateInstallments}
        onClose={() => {
          if (!generating) setGenerateOpen(false)
        }}
      />

      <ConfirmDialog
        open={removePayTarget != null}
        title="Remover pagamento?"
        description={
          removePayTarget
            ? `O pagamento de R$ ${formatBRLValue(removePayTarget.amount)} será removido e o saldo devedor da parcela e da conta serão recalculados.`
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
        description="A conta será marcada como CANCELADA e poderá ser reativada depois. As parcelas em aberto sem pagamentos também serão canceladas."
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