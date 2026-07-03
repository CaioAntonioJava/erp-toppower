import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { FileEdit, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { BackButton } from '../components/ui/BackButton'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { QuotationStatusBadge } from '../components/sales/QuotationStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import { cancelQuotation, getQuotation } from '../api/quotation.api'
import { toApiError } from '../lib/errors'
import type { QuotationResponse } from '../types/quotation'
import {
  DISCOUNT_TYPE_LABELS,
  PAYMENT_CONDITION_LABELS,
  QUOTATION_CLIENT_TYPE_LABELS,
} from '../types/quotation'
import { useAuth } from '../context/AuthContext'

/** Formatador de moeda BRL. */
const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDiscount(
  type: string | null | undefined,
  value: number | null | undefined,
): string {
  if (type == null || value == null) return '—'
  if (type === 'PERCENT') return `${value}%`
  return brlFormatter.format(value)
}

export function QuotationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [quotation, setQuotation] = useState<QuotationResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const [canceling, setCanceling] = useState(false)
  const [cancelError, setCancelError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getQuotation(id)
      .then((data) => {
        if (cancelled) return
        setQuotation(data)
      })
      .catch((err) => {
        if (cancelled) return
        setError(toApiError(err).message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id])

  async function handleCancel() {
    if (!quotation) return
    setCanceling(true)
    setCancelError(null)
    try {
      const updated = await cancelQuotation(quotation.uuid)
      setQuotation(updated)
      setConfirmCancel(false)
    } catch (err) {
      setCancelError(toApiError(err).message)
    } finally {
      setCanceling(false)
    }
  }

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  if (error || !quotation) {
    return (
      <div className="space-y-4">
        <BackButton
          variant="ghost"
          label="Voltar para a lista"
          fallback="/quotations"
        />
        <Alert variant="error">
          {error ?? 'Proposta não encontrada.'}
        </Alert>
      </div>
    )
  }

  const canEdit =
    quotation.status === 'ATIVA' || quotation.status === 'EXPIRADA'
  const canCancel = canEdit

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton variant="ghost" />
          <h1 className="mt-4 flex items-center gap-3 text-2xl font-semibold tracking-tight">
            <span>Proposta {quotation.number}</span>
            <QuotationStatusBadge status={quotation.status} />
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Emitida em {formatDate(quotation.issueDate)}
            {quotation.validityDays != null
              ? ` • Validade: ${quotation.validityDays} dia(s)`
              : ''}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {canEdit ? (
            <Button
              variant="secondary"
              onClick={() => navigate(`/quotations/${quotation.uuid}/edit`)}
            >
              <FileEdit className="h-4 w-4" />
              Editar
            </Button>
          ) : null}
          {canCancel ? (
            <Button
              variant="danger"
              onClick={() => {
                setCancelError(null)
                setConfirmCancel(true)
              }}
            >
              <X className="h-4 w-4" />
              Cancelar proposta
            </Button>
          ) : null}
        </div>
      </div>

      {cancelError ? <Alert variant="error">{cancelError}</Alert> : null}

      {/* Resumo */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-4 text-base font-semibold">Resumo</h3>
        <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Tipo de cliente
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {QUOTATION_CLIENT_TYPE_LABELS[quotation.clientType]}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              UUID do cliente
            </dt>
            <dd className="mt-1 break-all font-mono text-xs text-slate-800 dark:text-slate-200">
              {quotation.clientType === 'CUSTOMER'
                ? quotation.customerUuid
                : quotation.companyUuid}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Vendedor (UUID)
            </dt>
            <dd className="mt-1 break-all font-mono text-xs text-slate-800 dark:text-slate-200">
              {quotation.sellerUuid}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Condição de pagamento
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {quotation.paymentCondition
                ? PAYMENT_CONDITION_LABELS[quotation.paymentCondition]
                : '—'}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Desconto global
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {quotation.discountType
                ? `${DISCOUNT_TYPE_LABELS[quotation.discountType]} — ${formatDiscount(
                    quotation.discountType,
                    quotation.discount,
                  )}`
                : '—'}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
              Validade
            </dt>
            <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
              {quotation.validityDays != null
                ? `${quotation.validityDays} dia(s)`
                : '—'}
            </dd>
          </div>
          {quotation.attention ? (
            <div className="sm:col-span-2 lg:col-span-3">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
                Aos cuidados de
              </dt>
              <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
                {quotation.attention}
              </dd>
            </div>
          ) : null}
        </dl>
      </section>

      {/* Itens */}
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="border-b border-slate-200 p-6 dark:border-slate-800">
          <h3 className="text-base font-semibold">Itens</h3>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {quotation.items.length} item(ns) • {quotation.totalQuantity}{' '}
            unidade(s)
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">Produto</th>
                <th className="px-4 py-3 font-medium text-right">Qtd.</th>
                <th className="px-4 py-3 font-medium text-right">Preço un.</th>
                <th className="px-4 py-3 font-medium text-right">Subtotal</th>
                <th className="px-4 py-3 font-medium text-right">Desc.</th>
                <th className="px-4 py-3 font-medium text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {quotation.items.map((it) => (
                <tr key={it.uuid}>
                  <td className="px-4 py-3">
                    <span className="break-all font-mono text-xs text-slate-700 dark:text-slate-200">
                      {it.productUuid}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {it.quantity}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {brlFormatter.format(it.unitPrice)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {brlFormatter.format(it.lineSubtotal)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
                    {formatDiscount(it.discountType, it.discount)}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold">
                    {brlFormatter.format(it.totalPrice)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="flex flex-col items-stretch gap-2 border-t border-slate-200 p-4 text-sm sm:flex-row sm:items-center sm:justify-end dark:border-slate-800">
          <div className="flex justify-between gap-8 sm:justify-end">
            <span className="text-slate-500 dark:text-slate-400">Subtotal:</span>
            <span className="font-mono font-semibold">
              {brlFormatter.format(quotation.subtotal)}
            </span>
          </div>
          <div className="flex justify-between gap-8 sm:justify-end">
            <span className="text-slate-500 dark:text-slate-400">Total:</span>
            <span className="font-mono text-base font-semibold text-primary-700 dark:text-primary-200">
              {brlFormatter.format(quotation.total)}
            </span>
          </div>
        </div>
      </section>

      {/* Observações */}
      {quotation.notes ? (
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <h3 className="mb-2 text-base font-semibold">Observações</h3>
          <p className="whitespace-pre-wrap text-sm text-slate-700 dark:text-slate-200">
            {quotation.notes}
          </p>
        </section>
      ) : null}

      {/* Auditoria (admin) */}
      {isAdmin ? (
        <RegistrationAuditCard
          createdBy={quotation.createdBy}
          createdAt={quotation.createdAt}
          updatedBy={quotation.updatedBy}
          updatedAt={formatDateTime(quotation.updatedAt)}
        />
      ) : null}

      <ConfirmDialog
        open={confirmCancel}
        title="Cancelar proposta?"
        description={`A proposta ${quotation.number} será marcada como CANCELADA. O registro não é apagado. Propostas CANCELADAS não podem ser revertidas por este endpoint.`}
        confirmText="Cancelar proposta"
        confirmVariant="danger"
        isLoading={canceling}
        onConfirm={handleCancel}
        onClose={() => {
          if (!canceling) setConfirmCancel(false)
        }}
      />
    </div>
  )
}