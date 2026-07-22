import { useEffect, useRef, useState } from 'react'
import { X, Paperclip, FileText } from 'lucide-react'
import { Button } from '../ui/Button'
import { Alert } from '../ui/Alert'
import { formatCurrency } from '../../lib/format'
import type { BoletoDue } from '../../types/finance'

interface SettleBoletoModalProps {
  open: boolean
  boleto: BoletoDue | null
  onClose: () => void
  onConfirm: (id: number, receipt?: File) => Promise<void>
}

const ACCEPT = '.pdf,.png,.jpg,.jpeg'
const MAX_SIZE_BYTES = 10 * 1024 * 1024 // 10MB

/**
 * Modal de liquidação de boleto com opção de anexar comprovante.
 *
 * Exibe os dados do boleto (descrição, valor, vencimento) e permite
 * ao usuário anexar um comprovante de pagamento (PDF ou imagem) antes
 * de confirmar a liquidação.
 */
export function SettleBoletoModal({ open, boleto, onClose, onConfirm }: SettleBoletoModalProps) {
  const [receipt, setReceipt] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

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

  // Reseta o estado a cada abertura.
  useEffect(() => {
    if (open) {
      setReceipt(null)
      setError(null)
      setSubmitting(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }, [open])

  if (!open || !boleto) return null

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>): void {
    const file = e.target.files?.[0] ?? null
    if (file != null && file.size > MAX_SIZE_BYTES) {
      setError('Comprovante excede o limite de 10MB.')
      if (fileInputRef.current) fileInputRef.current.value = ''
      return
    }
    setError(null)
    setReceipt(file)
  }

  async function handleConfirm(): Promise<void> {
    setError(null)
    setSubmitting(true)
    try {
      await onConfirm(boleto.id, receipt ?? undefined)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Falha ao liquidar o boleto. Tente novamente.',
      )
      setSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 backdrop-blur-sm"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
          <h2 className="text-base font-semibold">Liquidar boleto</h2>
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
          {/* Dados do boleto */}
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/50">
            <div className="flex items-start gap-3">
              <FileText className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  {boleto.descricao}
                </p>
                <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  {boleto.pagador}
                </p>
                <div className="mt-2 flex items-center gap-4 text-sm">
                  <span className="font-semibold text-slate-900 dark:text-slate-100">
                    {formatCurrency(boleto.valor)}
                  </span>
                  <span className="text-slate-500 dark:text-slate-400">
                    Venc. {boleto.dataVencimento}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Upload de comprovante */}
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              Comprovante de pagamento (opcional)
            </label>
            <div className="flex items-center gap-3">
              <label
                className="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-slate-300 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
              >
                <Paperclip className="h-4 w-4" />
                Escolher arquivo
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={ACCEPT}
                  onChange={handleFileChange}
                  disabled={submitting}
                  className="hidden"
                />
              </label>
              {receipt ? (
                <span className="truncate text-sm text-slate-600 dark:text-slate-300">
                  {receipt.name}
                </span>
              ) : (
                <span className="text-xs text-slate-400 dark:text-slate-500">
                  PDF, PNG ou JPEG — até 10MB
                </span>
              )}
            </div>
          </div>

          {error ? (
            <Alert variant="error">{error}</Alert>
          ) : null}
        </div>

        <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4 dark:border-slate-800">
          <Button type="button" variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="button" onClick={handleConfirm} isLoading={submitting}>
            Confirmar liquidação
          </Button>
        </div>
      </div>
    </div>
  )
}
