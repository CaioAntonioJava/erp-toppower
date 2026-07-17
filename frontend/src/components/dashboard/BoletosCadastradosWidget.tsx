import { useState } from 'react'
import { AlertTriangle, Clock, Plus, Printer, Paperclip, Trash2, FileText } from 'lucide-react'
import { Card } from '../ui/Card'
import { Badge } from '../ui/Badge'
import { Button } from '../ui/Button'
import { Spinner } from '../ui/Spinner'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { Alert } from '../ui/Alert'
import { formatCurrency, formatDate } from '../../lib/format'
import { useBoletosStorage } from '../../hooks/useBoletosStorage'
import type { NovoBoletoInput } from '../../hooks/useBoletosStorage'
import { toApiError } from '../../lib/errors'
import { listBoletoAttachments, downloadBoletoAttachment } from '../../api/boleto.api'
import { BoletoFormModal } from './BoletoFormModal'
import { BoletoAttachmentsModal } from './BoletoAttachmentsModal'

/**
 * Bloco de boletos cadastrados pela usuária.
 *
 * Permite cadastrar boletos (contas a pagar/receber) diretamente do
 * dashboard, com listagem inline e remoção (soft delete). Persistência
 * no backend `/api/v1/boletos` (multi-tenant via header
 * `X-Organization-Id`). A lista recarrega automaticamente quando a
 * organização ativa muda.
 *
 * Os boletos cadastrados aqui complementam o widget `BoletosDueWidget`
 * (que mostra boletos vencendo / vencidos).
 */
export function BoletosCadastradosWidget() {
  const { items, loading, error, add, remove } = useBoletosStorage()
  const [modalOpen, setModalOpen] = useState(false)
  const [removerId, setRemoverId] = useState<number | null>(null)
  const [removing, setRemoving] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [anexosBoleto, setAnexosBoleto] = useState<{ id: number; label: string } | null>(null)
  const [printing, setPrinting] = useState<number | null>(null)

  /**
   * Busca o primeiro anexo do boleto e abre para impressão.
   * Se não houver anexos, exibe um alerta de erro.
   */
  async function handlePrint(boletoId: number): Promise<void> {
    setActionError(null)
    setPrinting(boletoId)
    try {
      const attachments = await listBoletoAttachments(boletoId)
      if (attachments.length === 0) {
        setActionError('Nenhum anexo encontrado para impressão.')
        return
      }
      const first = attachments[0]
      const { blob } = await downloadBoletoAttachment(boletoId, first.id, 'inline')
      const url = URL.createObjectURL(blob)
      const win = window.open(url, '_blank')
      if (win && first.contentType === 'application/pdf') {
        win.onload = () => win.print()
      }
      setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch (err) {
      setActionError(toApiError(err).message)
    } finally {
      setPrinting(null)
    }
  }

  async function handleSubmit(input: NovoBoletoInput): Promise<void> {
    setActionError(null)
    try {
      await add(input)
      setModalOpen(false)
    } catch (err) {
      // Repassa a mensagem do backend (ex.: documento duplicado) ao modal.
      throw new Error(toApiError(err).message)
    }
  }

  async function confirmarRemocao(): Promise<void> {
    if (removerId == null) return
    setRemoving(true)
    setActionError(null)
    try {
      await remove(removerId)
      setRemoverId(null)
    } catch (err) {
      setActionError(toApiError(err).message)
    } finally {
      setRemoving(false)
    }
  }

  return (
    <Card padded={false} className="flex flex-col">
      <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3 dark:border-slate-800">
        <div className="flex items-center gap-2">
          <FileText className="h-4 w-4 text-primary" />
          <h2 className="text-sm font-semibold">Meus boletos cadastrados</h2>
        </div>
        <Button size="sm" onClick={() => setModalOpen(true)}>
          <Plus className="h-4 w-4" />
          Novo boleto
        </Button>
      </div>

      <div className="flex-1 px-5 py-4">
        {error ? (
          <Alert variant="error">{error}</Alert>
        ) : actionError ? (
          <Alert variant="error">{actionError}</Alert>
        ) : loading ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : items.length === 0 ? (
          <div className="py-10 text-center">
            <FileText className="mx-auto h-8 w-8 text-slate-300 dark:text-slate-600" />
            <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
              Nenhum boleto cadastrado.
            </p>
            <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">
              Clique em <strong>Novo boleto</strong> para adicionar uma conta.
            </p>
          </div>
        ) : (
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {items.map((boleto) => {
              const vencido = boleto.diasAteVencimento < 0
              return (
                <li key={boleto.id} className="flex items-center justify-between py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">
                      {boleto.numeroDocumento}
                    </p>
                    <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                      {boleto.pagador} · Venc. {formatDate(boleto.dataVencimento)}
                    </p>
                  </div>
                  <div className="ml-3 flex shrink-0 items-center gap-3">
                    <span className="text-sm font-semibold">
                      {formatCurrency(boleto.valor)}
                    </span>
                    {vencido ? (
                      <Badge tone="danger">
                        <AlertTriangle className="mr-1 h-3 w-3" />
                        Vencido
                      </Badge>
                    ) : (
                      <Badge tone="warning">
                        <Clock className="mr-1 h-3 w-3" />
                        {boleto.diasAteVencimento}d
                      </Badge>
                    )}
                    <button
                      type="button"
                      onClick={() => {
                        setActionError(null)
                        setAnexosBoleto({
                          id: boleto.id,
                          label: `${boleto.numeroDocumento} · ${boleto.pagador}`,
                        })
                      }}
                      className="rounded p-1 text-slate-400 hover:bg-primary-50 hover:text-primary dark:hover:bg-primary-900/30"
                      aria-label="Anexos do boleto"
                      title="Anexos"
                    >
                      <Paperclip className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePrint(boleto.id)}
                      disabled={printing === boleto.id}
                      className="rounded p-1 text-slate-400 hover:bg-primary-50 hover:text-primary disabled:opacity-50 dark:hover:bg-primary-900/30"
                      aria-label="Imprimir boleto"
                      title="Imprimir"
                    >
                      <Printer className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setActionError(null)
                        setRemoverId(boleto.id)
                      }}
                      className="rounded p-1 text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/30 dark:hover:text-red-400"
                      aria-label="Remover boleto"
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

      <BoletoFormModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmDialog
        open={removerId != null}
        title="Remover boleto"
        description="Tem certeza que deseja remover este boleto? Esta ação não pode ser desfeita."
        confirmText="Remover"
        confirmVariant="danger"
        isLoading={removing}
        onConfirm={confirmarRemocao}
        onClose={() => setRemoverId(null)}
      />

      <BoletoAttachmentsModal
        open={anexosBoleto != null}
        boletoId={anexosBoleto?.id ?? null}
        boletoLabel={anexosBoleto?.label ?? ''}
        onClose={() => setAnexosBoleto(null)}
      />
    </Card>
  )
}