import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getTechnicalProposal, getTechnicalProposalPdf } from '../api/technicalProposal.api'
import { PdfPreview } from '../components/ui/PdfPreview'

/**
 * Preview da Proposta Técnica em PDF (gerado pelo backend).
 *
 * <p>Esta página é renderizada SEM o AppLayout (rota sem sidebar) para
 * que o preview ocupe a tela inteira. O backend ({@code GET
 * /api/v1/technical-proposals/{id}/pdf}) é responsável por montar o PDF
 * com cabeçalho dinâmico (logo + dados da Organization ativa).</p>
 */
export function TechnicalProposalPrintPage() {
  const { id } = useParams<{ id: string }>()
  const [title, setTitle] = useState<string>('Proposta Técnica')

  useEffect(() => {
    if (!id) return
    let cancelled = false
    getTechnicalProposal(Number(id!))
      .then((p) => {
        if (!cancelled) setTitle(`Proposta Técnica nº ${p.code}`)
      })
      .catch(() => {/* mantém título genérico */})
    return () => { cancelled = true }
  }, [id])

  const fetcher = useCallback(async () => {
    if (!id) throw new Error('ID da proposta ausente.')
    return getTechnicalProposalPdf(Number(id!), 'inline')
  }, [id])

  return <PdfPreview title={title} fetcher={fetcher} />
}