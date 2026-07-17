import { useCallback, useEffect, useState } from 'react'
import {
  deleteBoletoAttachment,
  downloadBoletoAttachment,
  listBoletoAttachments,
  uploadBoletoAttachment,
} from '../api/boleto.api'
import { toApiError } from '../lib/errors'
import type { BoletoAttachmentResponse } from '../types/boleto'

/**
 * Hook que gerencia os anexos de um boleto (PDF/imagens).
 *
 * Carrega a lista de anexos, permite upload e remoção, e oferece
 * `print(attachmentId, contentType)` que baixa o anexo como blob e abre
 * numa nova janela para impressão (PDF) ou visualização (imagem). O
 * acesso é autenticado, então a impressão usa um blob URL temporário
 * (não expõe o JWT na URL).
 */
export function useBoletoAttachments(boletoId: number | null) {
  const [attachments, setAttachments] = useState<BoletoAttachmentResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async (): Promise<void> => {
    if (boletoId == null) {
      setAttachments([])
      return
    }
    setLoading(true)
    setError(null)
    try {
      setAttachments(await listBoletoAttachments(boletoId))
    } catch (err) {
      setError(toApiError(err).message)
      setAttachments([])
    } finally {
      setLoading(false)
    }
  }, [boletoId])

  useEffect(() => {
    void reload()
  }, [reload])

  /** Faz upload de um anexo. Repassa erros do backend ao caller. */
  const upload = useCallback(
    async (file: File): Promise<BoletoAttachmentResponse> => {
      const created = await uploadBoletoAttachment(boletoId as number, file)
      setAttachments((prev) => [...prev, created])
      return created
    },
    [boletoId],
  )

  /** Remove um anexo pelo id. Repassa erros do backend ao caller. */
  const remove = useCallback(
    async (attachmentId: number): Promise<void> => {
      await deleteBoletoAttachment(boletoId as number, attachmentId)
      setAttachments((prev) => prev.filter((a) => a.id !== attachmentId))
    },
    [boletoId],
  )

  /**
   * Baixa o anexo como blob e abre para impressão/visualização.
   * PDFs usam o viewer nativo do navegador (window.print); imagens são
   * abertas numa nova aba. Revoga a URL criada após abrir.
   */
  const print = useCallback(
    async (attachmentId: number, contentType: string): Promise<void> => {
      const { blob } = await downloadBoletoAttachment(
        boletoId as number,
        attachmentId,
        'inline',
      )
      const url = URL.createObjectURL(blob)
      const win = window.open(url, '_blank')
      if (win) {
        // Para PDFs, o navegador abre o viewer e dispara a impressão.
        // Para imagens, a janela exibe o conteúdo; o usuário pode imprimir.
        win.onload = () => {
          if (contentType === 'application/pdf') {
            win.print()
          }
        }
      }
      // Revoga a URL após um tempo para liberar memória.
      setTimeout(() => URL.revokeObjectURL(url), 60000)
    },
    [boletoId],
  )

  return { attachments, loading, error, upload, remove, print, reload }
}