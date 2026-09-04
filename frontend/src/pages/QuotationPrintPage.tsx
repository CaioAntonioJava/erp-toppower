import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getQuotation, getQuotationPdf } from '../api/quotation.api'
import { PdfPreview } from '../components/ui/PdfPreview'
import { toApiError } from '../lib/errors'

/**
 * Página de preview da Proposta Comercial (Cotação) — exibe o PDF
 * gerado pelo backend em um iframe.
 *
 * <p>O backend ({@code GET /api/v1/quotations/{id}/pdf}) produz o PDF
 * com cabeçalho dinâmico (logo + dados da Organization ativa — Top
 * Power Engenharia ou Materiais). Esta página apenas busca o PDF
 * autenticado, apresenta-o em iframe e oferece o botão "Baixar PDF".</p>
 *
 * <p>Esta página é renderizada SEM o AppLayout (rota sem sidebar) para
 * que o preview ocupe a tela inteira.</p>
 */
export function QuotationPrintPage() {
  const { id } = useParams<{ id: string }>()
  const [title, setTitle] = useState<string>('Proposta Comercial')

  // Carrega o número da proposta para mostrar no título da página.
  // Falha silenciosa — o título permanece genérico.
  useEffect(() => {
    if (!id) return
    let cancelled = false
    getQuotation(Number(id!))
      .then((q) => {
        if (!cancelled) setTitle(`Proposta Comercial nº ${q.number}`)
      })
      .catch(() => {/* mantém título genérico */})
    return () => { cancelled = true }
  }, [id])

  const fetcher = useCallback(async () => {
    if (!id) throw new Error('ID da proposta ausente.')
    return getQuotationPdf(Number(id!), 'inline')
  }, [id])

  return <PdfPreview title={title} fetcher={fetcher} />
}

/**
 * Re-export do helper de erro para evitar tree-shake remover o import
 * caso outras páginas deste módulo precisem dele no futuro.
 */
export { toApiError }