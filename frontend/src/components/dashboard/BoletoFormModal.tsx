import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Alert } from '../ui/Alert'
import { parseNumber } from '../../lib/money'
import type { NovoBoletoInput } from '../../hooks/useBoletosStorage'

interface BoletoFormModalProps {
  open: boolean
  onClose: () => void
  onSubmit: (input: NovoBoletoInput) => void | Promise<void>
}

/** Estado do formulário de cadastro de boleto. */
interface FormState {
  documentNumber: string
  payee: string
  valor: string
  dueDate: string
}

const EMPTY: FormState = {
  documentNumber: '',
  payee: '',
  valor: '',
  dueDate: '',
}

/**
 * Modal de cadastro de boleto.
 *
 * Formulário controlado com validação simples (campos obrigatórios +
 * valor numérico válido + data no formato ISO yyyy-MM-dd). Segue o padrão
 * visual do ConfirmDialog: overlay, fecha com ESC/clique fora, bloqueia
 * scroll do body. Os dados são persistidos pelo hook `useBoletosStorage`
 * (chamadas à API `/api/v1/boletos`).
 */
export function BoletoFormModal({ open, onClose, onSubmit }: BoletoFormModalProps) {
  const [form, setForm] = useState<FormState>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // Bloqueia scroll do body enquanto aberto.
  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [open])

  // Fecha com ESC.
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  // Reseta o formulário a cada abertura.
  useEffect(() => {
    if (open) {
      setForm(EMPTY)
      setError(null)
      setSubmitting(false)
    }
  }, [open])

  if (!open) return null

  function setField<K extends keyof FormState>(key: K, value: FormState[K]): void {
    setForm((f) => ({ ...f, [key]: value }))
  }

  async function handleSubmit(e: React.FormEvent): Promise<void> {
    e.preventDefault()
    const documentNumber = form.documentNumber.trim()
    const payee = form.payee.trim()
    const valor = parseNumber(form.valor)
    const dueDate = form.dueDate

    if (!documentNumber) {
      setError('Informe o número do documento/boleto.')
      return
    }
    if (!payee) {
      setError('Informe o beneficiário (cliente ou fornecedor).')
      return
    }
    if (valor == null || valor <= 0) {
      setError('Informe um valor válido maior que zero.')
      return
    }
    if (!dueDate) {
      setError('Informe a data de vencimento.')
      return
    }

    setError(null)
    setSubmitting(true)
    try {
      await onSubmit({ documentNumber, payee, value: valor, dueDate })
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Falha ao cadastrar o boleto. Tente novamente.',
      )
      setSubmitting(false)
      return
    }
    setSubmitting(false)
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 backdrop-blur-sm"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
          <h2 className="text-base font-semibold">Cadastrar boleto</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-500 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800"
            aria-label="Fechar"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="space-y-4 px-5 py-5">
          <Input
            label="Número do documento"
            required
            placeholder="Ex.: 12345/1"
            value={form.documentNumber}
            onChange={(e) => setField('documentNumber', e.target.value)}
          />
          <Input
            label="Beneficiário"
            required
            placeholder="Cliente ou fornecedor"
            value={form.payee}
            onChange={(e) => setField('payee', e.target.value)}
          />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="Valor (R$)"
              required
              inputMode="decimal"
              placeholder="0,00"
              leftAdornment={<span className="text-sm">R$</span>}
              value={form.valor}
              onChange={(e) => setField('valor', e.target.value)}
            />
            <Input
              label="Vencimento"
              required
              type="date"
              value={form.dueDate}
              onChange={(e) => setField('dueDate', e.target.value)}
            />
          </div>

          {error ? (
            <Alert variant="error">{error}</Alert>
          ) : null}
        </div>

        <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4 dark:border-slate-800">
          <Button type="button" variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" isLoading={submitting}>
            Cadastrar
          </Button>
        </div>
      </form>
    </div>
  )
}