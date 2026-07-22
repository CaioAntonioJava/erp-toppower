import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle, CheckCircle2, Clock, Plus, Printer, Paperclip, Trash2, FileText, BarChart3 } from 'lucide-react'
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
import { getBoleto, listBoletoAttachments, downloadBoletoAttachment } from '../../api/boleto.api'
import type { BoletoResponse, BoletoUpdateRequest } from '../../types/boleto'
import type { BoletoDue } from '../../types/finance'
import { BoletoFormModal } from './BoletoFormModal'
import { BoletoAttachmentsModal } from './BoletoAttachmentsModal'
import { SettleBoletoModal } from './SettleBoletoModal'

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
  const { items, loading, error, add, update, settle, remove } = useBoletosStorage()
  // O widget de cadastro mostra apenas boletos em aberto (não pagos).
  // Boletos liquidados ficam visíveis no relatório (/boletos).
  const openItems = items.filter((b) => !b.paid)
  const [modalOpen, setModalOpen] = useState(false)
  const [editandoBoleto, setEditandoBoleto] = useState<BoletoResponse | null>(null)
  const [removerId, setRemoverId] = useState<number | null>(null)
  const [removing, setRemoving] = useState(false)
  const [settlingBoleto, setSettlingBoleto] = useState<BoletoDue | null>(null)
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

  async function handleEdit(boletoId: number): Promise<void> {
    setActionError(null)
    try {
      const boleto = await getBoleto(boletoId)
      setEditandoBoleto(boleto)
    } catch (err) {
      setActionError(toApiError(err).message)
    }
  }

  async function handleUpdate(id: number, input: BoletoUpdateRequest): Promise<void> {
    setActionError(null)
    try {
      await update(id, input)
      setEditandoBoleto(null)
    } catch (err) {
      // Repassa a mensagem do backend ao modal.
      throw new Error(toApiError(err).message)
    }
  }

  async function handleSettleConfirm(id: number, receipt?: File): Promise<void> {
    setActionError(null)
    try {
      await settle(id, receipt)
      setSettlingBoleto(null)
    } catch (err) {
      // Repassa a mensagem do backend ao modal.
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
        <div className="flex items-center gap-2">
          <Link to="/boletos">
            <Button size="sm" variant="secondary">
              <BarChart3 className="h-4 w-4" />
              Relatório
            </Button>
          </Link>
          <Button size="sm" onClick={() => setModalOpen(true)}>
            <Plus className="h-4 w-4" />
            Novo boleto
          </Button>
        </div>
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
        ) : openItems.length === 0 ? (
          <div className="py-10 text-center">
            <FileText className="mx-auto h-8 w-8 text-slate-300 dark:text-slate-600" />
            <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
              Nenhum boleto em aberto.
            </p>
            <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">
              Clique em <strong>Novo boleto</strong> para adicionar uma conta.
            </p>
          </div>
        ) : (
          <div className="space-y-1">
            {/* Cabeçalho das colunas */}
            <div className="hidden grid-cols-12 gap-3 px-3 py-2 text-xs font-medium uppercase tracking-wider text-slate-400 dark:text-slate-500 sm:grid">
              <span className="col-span-4">Descrição</span>
              <span className="col-span-3">Fornecedor</span>
              <span className="col-span-2 text-right">Valor</span>
              <span className="col-span-2">Vencimento</span>
              <span className="col-span-1" />
            </div>

            <ul className="divide-y divide-slate-100 dark:divide-slate-800">
              {openItems.map((boleto) => {
                const vencido = boleto.diasAteVencimento < 0
                return (
                  <li key={boleto.id}>
                    <button
                      type="button"
                      onClick={() => handleEdit(boleto.id)}
                      className="group grid w-full cursor-pointer grid-cols-12 gap-3 rounded-lg px-3 py-3 text-left transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/50"
                    >
                      {/* Descrição */}
                      <span className="col-span-4 truncate text-sm font-medium text-slate-900 dark:text-slate-100">
                        {boleto.descricao}
                      </span>

                      {/* Fornecedor / Pagador */}
                      <span className="col-span-3 truncate text-sm text-slate-600 dark:text-slate-400">
                        {boleto.pagador}
                      </span>

                      {/* Valor */}
                      <span className="col-span-2 truncate text-right text-sm font-semibold text-slate-900 dark:text-slate-100">
                        {formatCurrency(boleto.valor)}
                      </span>

                      {/* Vencimento */}
                      <span className="col-span-2 truncate text-sm text-slate-600 dark:text-slate-400">
                        {formatDate(boleto.dataVencimento)}
                      </span>

                      {/* Status + ações */}
                      <span className="col-span-1 flex items-center justify-end gap-1">
                        {boleto.paid ? (
                          <Badge tone="success" className="shrink-0">
                            <CheckCircle2 className="mr-0.5 h-3 w-3" />
                            Pago
                          </Badge>
                        ) : vencido ? (
                          <Badge tone="danger" className="shrink-0">
                            <AlertTriangle className="mr-0.5 h-3 w-3" />
                            Vencido
                          </Badge>
                        ) : (
                          <Badge tone="warning" className="shrink-0">
                            <Clock className="mr-0.5 h-3 w-3" />
                            {boleto.diasAteVencimento}d
                          </Badge>
                        )}
                        <span className="hidden items-center gap-0.5 sm:flex">
                          {!boleto.paid ? (
                            <span
                              role="button"
                              tabIndex={0}
                              onClick={(e) => {
                                e.stopPropagation()
                                setSettlingBoleto(boleto)
                              }}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                  e.stopPropagation()
                                  setSettlingBoleto(boleto)
                                }
                              }}
                              className="rounded p-1 text-slate-400 transition-opacity hover:bg-green-50 hover:text-green-600 dark:hover:bg-green-900/30 dark:hover:text-green-400"
                              aria-label="Liquidar boleto"
                              title="Liquidar"
                            >
                              <CheckCircle2 className="h-3.5 w-3.5" />
                            </span>
                          ) : null}
                          <span
                            role="button"
                            tabIndex={0}
                            onClick={(e) => {
                              e.stopPropagation()
                              setActionError(null)
                              setAnexosBoleto({
                                id: boleto.id,
                                label: `${boleto.descricao} · ${boleto.pagador}`,
                              })
                            }}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' || e.key === ' ') {
                                e.stopPropagation()
                                setActionError(null)
                                setAnexosBoleto({
                                  id: boleto.id,
                                  label: `${boleto.descricao} · ${boleto.pagador}`,
                                })
                              }
                            }}
                            className="rounded p-1 text-slate-400 transition-opacity hover:bg-primary-50 hover:text-primary dark:hover:bg-primary-900/30"
                            aria-label="Anexos do boleto"
                            title="Anexos"
                          >
                            <Paperclip className="h-3.5 w-3.5" />
                          </span>
                          <span
                            role="button"
                            tabIndex={0}
                            onClick={(e) => {
                              e.stopPropagation()
                              handlePrint(boleto.id)
                            }}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' || e.key === ' ') {
                                e.stopPropagation()
                                handlePrint(boleto.id)
                              }
                            }}
                            className="rounded p-1 text-slate-400 transition-opacity hover:bg-primary-50 hover:text-primary disabled:opacity-50 dark:hover:bg-primary-900/30"
                            aria-label="Imprimir boleto"
                            title="Imprimir"
                          >
                            <Printer className="h-3.5 w-3.5" />
                          </span>
                          <span
                            role="button"
                            tabIndex={0}
                            onClick={(e) => {
                              e.stopPropagation()
                              setActionError(null)
                              setRemoverId(boleto.id)
                            }}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' || e.key === ' ') {
                                e.stopPropagation()
                                setActionError(null)
                                setRemoverId(boleto.id)
                              }
                            }}
                            className="rounded p-1 text-slate-400 transition-opacity hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/30 dark:hover:text-red-400"
                            aria-label="Remover boleto"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </span>
                        </span>
                      </span>
                    </button>

                    {/* Barra de ações (aparece abaixo em mobile, ao lado no desktop) */}
                    <div className="flex items-center justify-end gap-1 px-3 pb-2 sm:hidden">
                      {!boleto.paid ? (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation()
                            setSettlingBoleto(boleto)
                          }}
                          className="rounded p-1.5 text-slate-400 hover:bg-green-50 hover:text-green-600 dark:hover:bg-green-900/30 dark:hover:text-green-400"
                          aria-label="Liquidar boleto"
                          title="Liquidar"
                        >
                          <CheckCircle2 className="h-4 w-4" />
                        </button>
                      ) : null}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          setActionError(null)
                          setAnexosBoleto({
                            id: boleto.id,
                            label: `${boleto.descricao} · ${boleto.pagador}`,
                          })
                        }}
                        className="rounded p-1.5 text-slate-400 hover:bg-primary-50 hover:text-primary dark:hover:bg-primary-900/30"
                        aria-label="Anexos do boleto"
                        title="Anexos"
                      >
                        <Paperclip className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          handlePrint(boleto.id)
                        }}
                        disabled={printing === boleto.id}
                        className="rounded p-1.5 text-slate-400 hover:bg-primary-50 hover:text-primary disabled:opacity-50 dark:hover:bg-primary-900/30"
                        aria-label="Imprimir boleto"
                        title="Imprimir"
                      >
                        <Printer className="h-4 w-4" />
                      </button>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          setActionError(null)
                          setRemoverId(boleto.id)
                        }}
                        className="rounded p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/30 dark:hover:text-red-400"
                        aria-label="Remover boleto"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </li>
                )
              })}
            </ul>
          </div>
        )}
      </div>

      <BoletoFormModal
        open={modalOpen || editandoBoleto != null}
        onClose={() => {
          setModalOpen(false)
          setEditandoBoleto(null)
        }}
        onSubmit={handleSubmit}
        editBoleto={editandoBoleto}
        onUpdate={handleUpdate}
      />

      <SettleBoletoModal
        open={settlingBoleto != null}
        boleto={settlingBoleto}
        onClose={() => setSettlingBoleto(null)}
        onConfirm={handleSettleConfirm}
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