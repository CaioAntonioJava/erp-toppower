import { useEffect, useMemo, useState } from 'react'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Alert } from '../ui/Alert'
import { registerInstallmentPayment } from '../../api/receivable.api'
import type {
  ReceivableInstallmentResponse,
  ReceivablePaymentRequest,
  ReceivableResponse,
} from '../../types/receivable'
import { formatBRLValue, parseNumber } from '../../lib/money'
import { toApiError } from '../../lib/errors'

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

interface ReceivablePaymentModalProps {
  /** Conta a receber alvo do pagamento (com parcelas). */
  receivable: ReceivableResponse | null
  /** Controla a exibição do modal. */
  open: boolean
  /** Fecha o modal (backdrop, botão Cancelar, ESC após sucesso). */
  onClose: () => void
  /** Chamado após o registro bem-sucedido com a conta atualizada. */
  onSuccess: (updated: ReceivableResponse) => void
}

/**
 * Modal de registrar pagamento em conta a receber. O pagamento é
 * contra uma parcela específica — por isso o modal exibe um select das
 * parcelas ABERTO com o saldo devedor de cada uma.
 */
export function ReceivablePaymentModal({
  receivable,
  open,
  onClose,
  onSuccess,
}: ReceivablePaymentModalProps) {
  const [installmentId, setInstallmentId] = useState<number | ''>('')
  const [amount, setAmount] = useState('')
  const [paymentDate, setPaymentDate] = useState(todayISO())
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Parcelas ABERTO disponíveis para baixa. Memoizado para evitar
  // reexecução do useEffect a cada render (identidade estável).
  const openInstallments: ReceivableInstallmentResponse[] = useMemo(
    () =>
      receivable
        ? receivable.installments.filter((i) => i.status === 'ABERTO')
        : [],
    [receivable],
  )

  // Reseta os campos sempre que o modal é (re)aberto.
  useEffect(() => {
    if (open) {
      setInstallmentId('')
      setAmount('')
      setPaymentDate(todayISO())
      setNotes('')
      setError(null)
    }
  }, [open])

  // Pré-seleciona a primeira parcela ABERTO quando o receivable muda.
  useEffect(() => {
    if (open && openInstallments.length > 0 && installmentId === '') {
      setInstallmentId(openInstallments[0].id)
    }
  }, [open, receivable, installmentId, openInstallments])

  if (!open || !receivable) return null

  const selectedInstallment = openInstallments.find(
    (i) => i.id === installmentId,
  )

  async function handleSubmit() {
    if (!receivable || !installmentId) return
    const value = parseNumber(amount)
    if (value == null || value <= 0) {
      setError('Valor do pagamento inválido.')
      return
    }
    if (!paymentDate) {
      setError('Data do pagamento é obrigatória.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const payload: ReceivablePaymentRequest = {
        amount: value,
        paymentDate,
        notes: notes || null,
      }
      const updated = await registerInstallmentPayment(
        receivable.id,
        installmentId,
        payload,
      )
      onSuccess(updated)
      onClose()
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setSubmitting(false)
    }
  }

  const installmentOptions =
    openInstallments.length === 0
      ? [{ value: '', label: 'Sem parcelas em aberto' }]
      : openInstallments.map((i) => ({
          value: String(i.id),
          label: `Parcela ${i.installmentNumber} — vence ${formatDate(i.dueDate)} • saldo R$ ${formatBRLValue(i.balance)}`,
        }))

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 backdrop-blur-sm"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !submitting) onClose()
      }}
    >
      <div className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900">
        <div className="border-b border-slate-200 px-5 py-4 dark:border-slate-800">
          <h2 className="text-base font-semibold">Registrar pagamento</h2>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {receivable.description}
          </p>
        </div>
        <div className="space-y-4 px-5 py-4">
          {error ? <Alert variant="error">{error}</Alert> : null}
          {openInstallments.length === 0 ? (
            <Alert variant="info">
              Esta conta não possui parcelas em aberto para receber
              pagamentos.
            </Alert>
          ) : null}
          <Select
            label="Parcela"
            required
            options={installmentOptions}
            value={String(installmentId)}
            onChange={(e) =>
              setInstallmentId(e.target.value ? Number(e.target.value) : '')
            }
            disabled={openInstallments.length === 0}
            aria-label="Selecionar parcela"
          />
          {selectedInstallment ? (
            <p className="-mt-2 text-xs text-slate-500 dark:text-slate-400">
              Saldo da parcela: R$ {formatBRLValue(selectedInstallment.balance)}
            </p>
          ) : null}
          <Input
            label="Valor do pagamento (R$)"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="0,00"
            disabled={openInstallments.length === 0}
          />
          <Input
            label="Data do pagamento"
            required
            type="date"
            value={paymentDate}
            onChange={(e) => setPaymentDate(e.target.value)}
            disabled={openInstallments.length === 0}
          />
          <Input
            label="Observações"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Ex.: PIX, transferência…"
            maxLength={500}
            disabled={openInstallments.length === 0}
          />
        </div>
        <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4 dark:border-slate-800">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button
            onClick={handleSubmit}
            isLoading={submitting}
            disabled={openInstallments.length === 0}
          >
            Registrar
          </Button>
        </div>
      </div>
    </div>
  )
}