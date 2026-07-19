import { useEffect, useState } from 'react'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Alert } from '../ui/Alert'
import { registerPayment } from '../../api/receivable.api'
import type {
  ReceivablePaymentRequest,
  ReceivableResponse,
} from '../../types/receivable'
import { formatBRLValue, parseNumber } from '../../lib/money'
import { toApiError } from '../../lib/errors'

function todayISO(): string {
  return new Date().toISOString().slice(0, 10)
}

interface ReceivablePaymentModalProps {
  /** Conta a receber alvo do pagamento. */
  receivable: { id: number; balance: number; description?: string } | null
  /** Controla a exibição do modal. */
  open: boolean
  /** Fecha o modal (backdrop, botão Cancelar, ESC após sucesso). */
  onClose: () => void
  /** Chamado após o registro bem-sucedido com a conta atualizada. */
  onSuccess: (updated: ReceivableResponse) => void
}

/**
 * Modal de registrar pagamento em conta a receber.
 * Reutilizável pela listagem e pela página de detalhe.
 */
export function ReceivablePaymentModal({
  receivable,
  open,
  onClose,
  onSuccess,
}: ReceivablePaymentModalProps) {
  const [amount, setAmount] = useState('')
  const [paymentDate, setPaymentDate] = useState(todayISO())
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Reseta os campos sempre que o modal é (re)aberto.
  useEffect(() => {
    if (open) {
      setAmount('')
      setPaymentDate(todayISO())
      setNotes('')
      setError(null)
    }
  }, [open])

  if (!open || !receivable) return null

  async function handleSubmit() {
    if (!receivable) return
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
      const updated = await registerPayment(receivable.id, payload)
      onSuccess(updated)
      onClose()
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setSubmitting(false)
    }
  }

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
            Saldo devedor atual: R$ {formatBRLValue(receivable.balance)}.
          </p>
        </div>
        <div className="space-y-4 px-5 py-4">
          {error ? <Alert variant="error">{error}</Alert> : null}
          <Input
            label="Valor do pagamento (R$)"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
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
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Ex.: PIX, transferência…"
            maxLength={500}
          />
        </div>
        <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4 dark:border-slate-800">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} isLoading={submitting}>
            Registrar
          </Button>
        </div>
      </div>
    </div>
  )
}