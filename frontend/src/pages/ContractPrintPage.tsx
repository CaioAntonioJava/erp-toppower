import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getContract, getContractPdf } from '../api/contract.api'
import { PdfPreview } from '../components/ui/PdfPreview'

/**
 * Página de preview do PDF do contrato — exibe o PDF gerado pelo
 * backend em um iframe.
 *
 * <p>O backend ({@code GET /api/v1/contracts/{id}/pdf}) produz o PDF
 * com cabeçalho dinâmico (logo + dados da Organization ativa), dados do
 * contratante, descrição e cláusulas. Esta página apenas busca o PDF
 * autenticado, apresenta-o em iframe e oferece os botões "Baixar PDF"
 * e "Imprimir".</p>
 *
 * <p>Renderizada SEM o AppLayout (rota sem sidebar) para que o preview
 * ocupe a tela inteira.</p>
 */
export function ContractPrintPage() {
  const { id } = useParams<{ id: string }>()
  const [title, setTitle] = useState<string>('Contrato')

  // Carrega o código do contrato para o título da página.
  useEffect(() => {
    if (!id) return
    let cancelled = false
    getContract(Number(id!))
      .then((c) => {
        if (!cancelled) setTitle(`Contrato ${c.code}`)
      })
      .catch(() => {/* mantém título genérico */})
    return () => { cancelled = true }
  }, [id])

  const fetcher = useCallback(async () => {
    if (!id) throw new Error('ID do contrato ausente.')
    return getContractPdf(Number(id!), 'inline')
  }, [id])

  return <PdfPreview title={title} fetcher={fetcher} />
}