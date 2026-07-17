import { useEffect, useRef, useState } from 'react'
import { FileText, ImageIcon, Paperclip, Printer, Trash2, X } from 'lucide-react'
import { Alert } from '../ui/Alert'
import { Button } from '../ui/Button'
import { Spinner } from '../ui/Spinner'
import { useBoletoAttachments } from '../../hooks/useBoletoAttachments'
import { toApiError } from '../../lib/errors'

interface BoletoAttachmentsModalProps {
  open: boolean
  boletoId: number | null
  boletoLabel: string
  onClose: () => void
}

/** Aceita PDF e imagens. */
const ACCEPT = '.pdf,.png,.jpg,.jpeg'
const MAX_SIZE_BYTES = 10 * 1024 * 1024 // 10MB

/** Formata tamanho em bytes para texto amigável. */
function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * Modal de anexos de um boleto.
 *
 * Permite anexar (PDF/imagens, até 10MB), visualizar/imprimir cada
 * anexo (via blob URL autenticado) e remover. O acesso ao conteúdo é
 * autenticado, então a impressão usa fetch+blob + URL.createObjectURL.
 */
export function BoletoAttachmentsModal({
  open,
  boletoId,
  boletoLabel,
  onClose,
}: BoletoAttachmentsModalProps) {
  const { attachments, loading, error, upload, remove, print } =
    useBoletoAttachments(open ? boletoId : null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  // Reseta erros ao abrir/fechar.
  useEffect(() => {
    if (open) setActionError(null)
  }, [open])

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

  if (!open) return null

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>): Promise<void> {
    const file = e.target.files?.[0]
    if (file == null) return
    setActionError(null)

    if (file.size > MAX_SIZE_BYTES) {
      setActionError('Arquivo excede o limite de 10MB.')
      if (fileInputRef.current) fileInputRef.current.value = ''
      return
    }

    setBusy(true)
    try {
      await upload(file)
    } catch (err) {
      setActionError(toApiError(err).message)
    } finally {
      setBusy(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  async function handlePrint(id: number, contentType: string): Promise<void> {
    setActionError(null)
    setBusy(true)
    try {
      await print(id, contentType)
    } catch (err) {
      setActionError(toApiError(err).message)
    } finally {
      setBusy(false)
    }
  }

  async function handleRemove(id: number): Promise<void> {
    setActionError(null)
    setBusy(true)
    try {
      await remove(id)
    } catch (err) {
      setActionError(toApiError(err).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 backdrop-blur-sm"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !busy) onClose()
      }}
    >
      <div className="w-full max-w-xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
          <div className="flex items-center gap-2">
            <Paperclip className="h-4 w-4 text-primary" />
            <div>
              <h2 className="text-base font-semibold">Anexos do boleto</h2>
              <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                {boletoLabel}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="rounded p-1 text-slate-500 hover:bg-slate-100 disabled:opacity-50 dark:text-slate-400 dark:hover:bg-slate-800"
            aria-label="Fechar"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="px-5 py-4">
          <div className="flex items-center gap-3">
            <input
              ref={fileInputRef}
              type="file"
              accept={ACCEPT}
              onChange={handleFileChange}
              disabled={busy}
              className="block text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-primary file:px-3 file:py-2 file:text-sm file:font-medium file:text-white hover:file:bg-primary-600 disabled:opacity-50 dark:text-slate-300"
            />
            {busy ? <Spinner /> : null}
          </div>
          <p className="mt-2 text-xs text-slate-400 dark:text-slate-500">
            PDF, PNG ou JPEG — até 10MB.
          </p>

          {error ? (
            <Alert variant="error">{error}</Alert>
          ) : actionError ? (
            <Alert variant="error">{actionError}</Alert>
          ) : null}

          <div className="mt-4">
            {loading ? (
              <div className="flex justify-center py-6">
                <Spinner />
              </div>
            ) : attachments.length === 0 ? (
              <p className="py-6 text-center text-sm text-slate-400 dark:text-slate-500">
                Nenhum anexo. Selecione um arquivo acima para anexar.
              </p>
            ) : (
              <ul className="divide-y divide-slate-100 dark:divide-slate-800">
                {attachments.map((att) => {
                  const isPdf = att.contentType === 'application/pdf'
                  const Icon = isPdf ? FileText : ImageIcon
                  return (
                    <li key={att.id} className="flex items-center justify-between py-3">
                      <div className="flex min-w-0 items-center gap-2">
                        <Icon className="h-4 w-4 shrink-0 text-slate-400" />
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium">{att.fileName}</p>
                          <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                            {formatSize(att.sizeBytes)}
                          </p>
                        </div>
                      </div>
                      <div className="ml-3 flex shrink-0 items-center gap-1">
                        <button
                          type="button"
                          onClick={() => handlePrint(att.id, att.contentType)}
                          disabled={busy}
                          className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-primary hover:bg-primary-50 disabled:opacity-50 dark:hover:bg-primary-900/30"
                          aria-label="Imprimir/visualizar"
                        >
                          <Printer className="h-3.5 w-3.5" />
                          Imprimir
                        </button>
                        <button
                          type="button"
                          onClick={() => handleRemove(att.id)}
                          disabled={busy}
                          className="rounded p-1 text-slate-400 hover:bg-red-50 hover:text-red-600 disabled:opacity-50 dark:hover:bg-red-900/30 dark:hover:text-red-400"
                          aria-label="Remover anexo"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        </div>

        <div className="flex justify-end border-t border-slate-200 px-5 py-4 dark:border-slate-800">
          <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>
            Fechar
          </Button>
        </div>
      </div>
    </div>
  )
}