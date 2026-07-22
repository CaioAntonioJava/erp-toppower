import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  Check,
  DollarSign,
  ExternalLink,
  Plus,
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
import { PayableStatusBadge } from '../components/payable/PayableStatusBadge'
import { PayablePaymentModal } from '../components/payable/PayablePaymentModal'
import {
  activatePayable,
  cancelPayable,
  createPayable,
  downloadPaymentReceipt,
  getPayable,
  removePayment,
  settleInstallment,
  updatePayable,
} from '../api/payable.api'
import { searchSuppliers } from '../api/supplier.api'
import type {
  PayableCreateRequest,
  PayableInstallmentRequest,
  PayableResponse,
  PayableSource,
  PayableUpdateRequest,
} from '../types/payable'
import type { SupplierResponse } from '../types/supplier'
import { formatBRLValue, parseNumber } from '../lib/money'
import { toApiError } from '../lib/errors'

const SOURCE_LABEL: Record<PayableSource, string> = {
  MANUAL: 'Manual',
  BOLETO: 'Boleto',
  PURCHASE_INVOICE: 'Nota de compra',
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
  issueDate?: string
  dueDate?: string
  supplier?: string
  installments?: string
}

interface InstallmentFormRow {
  amount: string
  dueDate: string
}

export function PayableFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = !!id

  // --- Detalhe (modo edição/visualização) ---
  const [payable, setPayable] = useState<PayableResponse | null>(null)
  const [loadingDetail, setLoadingDetail] = useState(isEdit)
  const [detailError, setDetailError] = useState<string | null>(null)

  // --- Form (modo criação manual OU edição de campos editáveis) ---
  const [description, setDescription] = useState('')
  const [valueStr, setValueStr] = useState('')
  const [issueDate, setIssueDate] = useState(todayISO())
  const [dueDate, setDueDate] = useState(todayISO())
  const [paymentCondition, setPaymentCondition] = useState<PaymentCondition | ''>(
    '',
  )
  const [supplierQuery, setSupplierQuery] = useState('')
  const [supplierId, setSupplierId] = useState<number | null>(null)
  const [supplierOptions, setSupplierOptions] = useState<SupplierResponse[]>([])
  const [supplierSearching, setSupplierSearching] = useState(false)
  // Parcelas explícitas (criação manual). Cada linha = { amount, dueDate }.
  const [installmentRows, setInstallmentRows] = useState<InstallmentFormRow[]>([])
  const [touched, setTouched] = useState<Record<string, boolean>>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  // --- Modal de pagamento ---
  const [paymentOpen, setPaymentOpen] = useState(false)

  // --- Modal de liquidar parcela ---
  const [settleInstallmentTarget, setSettleInstallmentTarget] = useState<
    { installmentId: number; installmentNumber: number; balance: number } | null
  >(null)
  const [settlingInst, setSettlingInst] = useState(false)

  // --- Modal de remover pagamento ---
  const [removePayTarget, setRemovePayTarget] =
    useState<{ paymentId: number; amount: number } | null>(null)
  const [removingPay, setRemovingPay] = useState(false)

  // --- Download de comprovante ---
  const [downloadingReceipt, setDownloadingReceipt] = useState<number | null>(null)

  async function handleOpenReceipt(paymentId: number): Promise<void> {
    setDownloadingReceipt(paymentId)
    try {
      const { blob } = await downloadPaymentReceipt(paymentId)
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank')
      setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch {
      setSubmitError('Erro ao abrir comprovante.')
    } finally {
      setDownloadingReceipt(null)
    }
  }

  // --- Modal de cancelar/reativar conta ---
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [confirmActivate, setConfirmActivate] = useState(false)
  const [toggling, setToggling] = useState(false)

  // --- Carrega detalhe ---
  useEffect(() => {
    if (!isEdit || !id) return
    setLoadingDetail(true)
    getPayable(Number(id))
      .then((r) => {
        setPayable(r)
        setDescription(r.description)
        setValueStr(formatBRLValue(r.value))
        setIssueDate(r.issueDate)
        setDueDate(r.dueDate)
        setPaymentCondition(r.paymentCondition ?? '')
        setSupplierId(r.supplierId)
        setSupplierQuery(r.supplierName ?? '')
      })
      .catch((err) => setDetailError(toApiError(err).message))
      .finally(() => setLoadingDetail(false))
  }, [id, isEdit])

  // --- Busca de fornecedor (debounce) ---
  function handleSupplierQuery(value: string) {
    const trimmed = value.trim()
    if (trimmed.length < 2) {
      setSupplierOptions([])
      return
    }
    setSupplierSearching(true)
    const timer = setTimeout(async () => {
      try {
        const result = await searchSuppliers({ query: trimmed, page: 0, size: 20 })
        setSupplierOptions(result.content)
      } catch {
        setSupplierOptions([])
      } finally {
        setSupplierSearching(false)
      }
    }, 300)
    return () => clearTimeout(timer)
  }

  // --- Parcelas explícitas ---
  function addInstallmentRow() {
    setInstallmentRows((rows) => [
      ...rows,
      { amount: '', dueDate: todayISO() },
    ])
  }
  function removeInstallmentRow(idx: number) {
    setInstallmentRows((rows) => rows.filter((_, i) => i !== idx))
  }
  function updateInstallmentRow(idx: number, field: keyof InstallmentFormRow, value: string) {
    setInstallmentRows((rows) =>
      rows.map((r, i) => (i === idx ? { ...r, [field]: value } : r)),
    )
  }

  function validate(): FieldErrors {
    const errs: FieldErrors = {}
    if (!description.trim()) errs.description = 'Descrição é obrigatória.'
    const v = parseNumber(valueStr)
    if (v == null || v <= 0) errs.value = 'Valor deve ser maior que zero.'
    if (!issueDate) errs.issueDate = 'Data de emissão é obrigatória.'
    if (!dueDate) errs.dueDate = 'Data de vencimento é obrigatória.'
    if (!supplierId) errs.supplier = 'Selecione um fornecedor.'
    // Valida parcelas explícitas (apenas na criação): cada uma com valor > 0
    // e vencimento; soma deve bater com value (tolerância 0,01).
    if (!isEdit && installmentRows.length > 0) {
      let sum = 0
      let rowErr = false
      for (const row of installmentRows) {
        const amt = parseNumber(row.amount)
        if (amt == null || amt <= 0 || !row.dueDate) {
          rowErr = true
          break
        }
        sum += amt
      }
      if (rowErr) {
        errs.installments = 'Todas as parcelas devem ter valor e vencimento.'
      } else {
        const total = parseNumber(valueStr) ?? 0
        const diff = Math.abs(sum - total)
        if (diff > 0.01) {
          errs.installments = `A soma das parcelas (R$ ${sum.toFixed(2).replace('.', ',')}) não bate com o valor total (R$ ${total.toFixed(2).replace('.', ',')}).`
        }
      }
    }
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
    setTouched({
      description: true,
      value: true,
      issueDate: true,
      dueDate: true,
      supplier: true,
      installments: true,
    })
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setSubmitting(false)
      return
    }
    const value = parseNumber(valueStr) ?? 0
    try {
      if (isEdit && id) {
        const payload: PayableUpdateRequest = {
          description,
          issueDate,
          dueDate,
          paymentCondition: paymentCondition || null,
        }
        const updated = await updatePayable(Number(id), payload)
        setPayable(updated)
        setTouched({})
      } else {
        const installments: PayableInstallmentRequest[] | null =
          installmentRows.length > 0
            ? installmentRows.map((r) => ({
                amount: parseNumber(r.amount) ?? 0,
                dueDate: r.dueDate,
              }))
            : null
        const payload: PayableCreateRequest = {
          description,
          value,
          issueDate,
          dueDate,
          supplierId: supplierId as number,
          paymentCondition: paymentCondition || null,
          installments,
        }
        const created = await createPayable(payload)
        navigate(`/payables/${created.id}`)
      }
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRemovePayment() {
    if (!payable || !removePayTarget) return
    setRemovingPay(true)
    try {
      const updated = await removePayment(payable.id, removePayTarget.paymentId)
      setPayable(updated)
      setRemovePayTarget(null)
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setRemovingPay(false)
    }
  }

  async function handleSettleInstallment() {
    if (!payable || !settleInstallmentTarget) return
    setSettlingInst(true)
    try {
      const updated = await settleInstallment(
        payable.id,
        settleInstallmentTarget.installmentId,
      )
      setPayable(updated)
      setSettleInstallmentTarget(null)
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setSettlingInst(false)
    }
  }

  async function handleCancel() {
    if (!payable) return
    setToggling(true)
    try {
      await cancelPayable(payable.id)
      const refreshed = await getPayable(payable.id)
      setPayable(refreshed)
      setConfirmCancel(false)
    } catch (err) {
      setSubmitError(toApiError(err).message)
    } finally {
      setToggling(false)
    }
  }

  async function handleActivate() {
    if (!payable) return
    setToggling(true)
    try {
      const refreshed = await activatePayable(payable.id)
      setPayable(refreshed)
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
  if (detailError && !payable) {
    return (
      <div className="space-y-4">
        <BackButton variant="ghost" label="Voltar para a lista" fallback="/payables" />
        <Alert variant="error">{detailError}</Alert>
      </div>
    )
  }

  const readOnly = payable != null && payable.sourceType !== 'MANUAL'
  const canEdit = payable == null || payable.status === 'ABERTO'

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <BackButton fallback="/payables" />
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">
            {isEdit ? 'Conta a Pagar' : 'Nova Conta a Pagar'}
          </h1>
          {payable ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Origem: {SOURCE_LABEL[payable.sourceType]}
              {payable.boletoId ? ` • Boleto #${payable.boletoId}` : ''}
              {payable.purchaseInvoiceNumber ? ` • ${payable.purchaseInvoiceNumber}` : ''}
              {payable.installmentsCount > 1
                ? ` • ${payable.installmentsCount} parcela(s)`
                : ''}
            </p>
          ) : (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Cadastro manual de um pagamento a fornecedor.
            </p>
          )}
        </div>
        {payable ? <PayableStatusBadge status={payable.status} /> : null}
      </div>

      {submitError ? <Alert variant="error">{submitError}</Alert> : null}

      {/* Painel de totais (apenas em modo detalhe) */}
      {payable ? (
        <div className="grid gap-3 sm:grid-cols-3">
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="text-xs uppercase tracking-wide text-slate-500">Valor total</div>
            <div className="mt-1 text-xl font-semibold text-slate-900 dark:text-slate-100">
              R$ {formatBRLValue(payable.value)}
            </div>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="text-xs uppercase tracking-wide text-slate-500">Pago</div>
            <div className="mt-1 text-xl font-semibold text-emerald-700 dark:text-emerald-400">
              R$ {formatBRLValue(payable.paidAmount)}
            </div>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="text-xs uppercase tracking-wide text-slate-500">Saldo devedor</div>
            <div className="mt-1 text-xl font-semibold text-slate-900 dark:text-slate-100">
              R$ {formatBRLValue(payable.balance)}
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
            label="Data de emissão"
            required
            type="date"
            value={issueDate}
            onChange={(e) => setIssueDate(e.target.value)}
            onBlur={getBlurHandler('issueDate')}
            error={shouldShowError('issueDate', fieldErrors.issueDate) ? fieldErrors.issueDate : null}
            disabled={!canEdit}
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
                ? 'Usado quando nenhuma parcela explícita é informada (à vista).'
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
                ? 'Quando informada e sem parcelas explícitas, gera as parcelas automaticamente a partir dos prazos.'
                : undefined
            }
          />

          {/* Fornecedor */}
          <div className="sm:col-span-2">
            <Input
              label="Buscar fornecedor"
              placeholder="Buscar por nome ou CNPJ…"
              value={supplierQuery}
              onChange={(e) => {
                setSupplierQuery(e.target.value)
                setSupplierId(null)
                handleSupplierQuery(e.target.value)
              }}
              onBlur={getBlurHandler('supplier')}
              error={shouldShowError('supplier', fieldErrors.supplier) ? fieldErrors.supplier : null}
              disabled={isEdit}
              rightAdornment={supplierSearching ? <Spinner size="sm" /> : undefined}
            />
            {supplierOptions.length > 0 && !supplierId ? (
              <ul className="mt-1 max-h-56 overflow-auto rounded-lg border border-slate-200 bg-white text-sm dark:border-slate-700 dark:bg-slate-900">
                {supplierOptions.map((s) => (
                  <li key={s.id}>
                    <button
                      type="button"
                      className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left hover:bg-slate-50 dark:hover:bg-slate-800"
                      onClick={() => {
                        setSupplierId(s.id)
                        setSupplierQuery(s.tradeName || s.legalName)
                        setSupplierOptions([])
                      }}
                    >
                      <span className="text-slate-900 dark:text-slate-100">
                        {s.tradeName || s.legalName}
                      </span>
                      <span className="text-xs text-slate-500">{s.taxId}</span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
            {supplierId && supplierQuery ? (
              <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                Selecionado: <strong>{supplierQuery}</strong>
              </p>
            ) : null}
          </div>
        </div>

        {/* Parcelas explícitas (apenas na criação manual) */}
        {!isEdit ? (
          <div className="mt-6">
            <div className="mb-2 flex items-center justify-between">
              <h3 className="text-sm font-semibold">Parcelas (opcional)</h3>
              <Button size="sm" variant="secondary" type="button" onClick={addInstallmentRow}>
                <Plus className="h-4 w-4" />
                Adicionar parcela
              </Button>
            </div>
            <p className="mb-3 text-xs text-slate-500 dark:text-slate-400">
              Se nenhuma parcela for informada, será criada uma única parcela à
              vista com o valor total e o vencimento acima. Se a condição de
              pagamento estiver definida e houver múltiplos prazos, as parcelas
              serão geradas automaticamente a partir dos prazos.
            </p>
            {installmentRows.length > 0 ? (
              <div className="space-y-2">
                {installmentRows.map((row, idx) => (
                  <div
                    key={idx}
                    className="grid grid-cols-1 gap-2 rounded-lg border border-slate-200 p-3 dark:border-slate-700 sm:grid-cols-[1fr_180px_auto]"
                  >
                    <Input
                      label={idx === 0 ? 'Valor (R$)' : undefined}
                      placeholder="0,00"
                      value={row.amount}
                      onChange={(e) => updateInstallmentRow(idx, 'amount', e.target.value)}
                    />
                    <Input
                      label={idx === 0 ? 'Vencimento' : undefined}
                      type="date"
                      value={row.dueDate}
                      onChange={(e) => updateInstallmentRow(idx, 'dueDate', e.target.value)}
                    />
                    <div className="flex items-end">
                      <Button
                        size="sm"
                        variant="ghost"
                        type="button"
                        onClick={() => removeInstallmentRow(idx)}
                        title="Remover parcela"
                        aria-label="Remover parcela"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                ))}
                {shouldShowError('installments', fieldErrors.installments) ? (
                  <p className="text-sm text-red-600 dark:text-red-400">
                    {fieldErrors.installments}
                  </p>
                ) : null}
              </div>
            ) : null}
          </div>
        ) : null}
      </div>

      {/* Parcelas programadas + pagamentos (apenas detalhe) */}
      {payable ? (
        <div className="space-y-6">
          {/* Parcelas */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-base font-semibold">Parcelas</h2>
              {payable.status === 'ABERTO' ? (
                <Button size="sm" onClick={() => setPaymentOpen(true)}>
                  <DollarSign className="h-4 w-4" />
                  Registrar pagamento
                </Button>
              ) : null}
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
                  {payable.installments.map((inst) => (
                    <tr key={inst.id}>
                      <td className="px-3 py-2 text-slate-700 dark:text-slate-200">
                        {inst.installmentNumber}/{payable.installmentsCount}
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
                        <PayableStatusBadge status={inst.status} />
                      </td>
                      <td className="px-3 py-2 text-right">
                        {inst.status === 'ABERTO' && payable.status === 'ABERTO' ? (
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
            {payable.payments.length === 0 ? (
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Nenhum pagamento registrado. Saldo devedor: R$ {formatBRLValue(payable.balance)}.
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
                      <th className="px-3 py-2 font-medium">Comprovante</th>
                      <th className="px-3 py-2 text-right font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                    {payable.payments.map((p) => (
                      <tr key={p.id}>
                        <td className="whitespace-nowrap px-3 py-2 text-slate-700 dark:text-slate-200">
                          {formatDate(p.paymentDate)}
                        </td>
                        <td className="px-3 py-2 text-slate-600 dark:text-slate-300">
                          {p.installmentNumber}
                        </td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-emerald-700 dark:text-emerald-400">
                          R$ {formatBRLValue(p.amount)}
                        </td>
                        <td className="px-3 py-2 text-slate-600 dark:text-slate-300">
                          {p.notes ?? '—'}
                        </td>
                        <td className="px-3 py-2">
                          {p.receiptUrl ? (
                            <button
                              type="button"
                              onClick={() => handleOpenReceipt(p.id)}
                              disabled={downloadingReceipt === p.id}
                              className="inline-flex items-center gap-1 text-sm text-primary hover:underline disabled:opacity-50"
                              title="Abrir comprovante"
                            >
                              <ExternalLink className="h-3.5 w-3.5" />
                              {downloadingReceipt === p.id ? 'Abrindo…' : 'Visualizar'}
                            </button>
                          ) : (
                            <span className="text-xs text-slate-400 dark:text-slate-500">—</span>
                          )}
                        </td>
                        <td className="px-3 py-2 text-right">
                          {payable.status !== 'CANCELADO' ? (
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
        <Link to="/payables">
          <Button variant="secondary" type="button">Cancelar</Button>
        </Link>
        {payable && payable.status === 'ABERTO' ? (
          <Button variant="danger" type="button" onClick={() => setConfirmCancel(true)}>
            Cancelar conta
          </Button>
        ) : null}
        {payable && payable.status === 'CANCELADO' ? (
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
      <PayablePaymentModal
        payable={payable}
        open={paymentOpen}
        onClose={() => setPaymentOpen(false)}
        onSuccess={(updated) => setPayable(updated)}
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
        title="Cancelar conta a pagar?"
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
        title="Reativar conta a pagar?"
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