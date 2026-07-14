import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getContract, getContractPdf } from '../api/contract.api'
import { PdfPreview } from '../components/ui/PdfPreview'

/**
 * Preview do Contrato em PDF (gerado pelo backend).
 *
 * <p>Esta página é renderizada SEM o AppLayout (rota sem sidebar) para
 * que o preview ocupe a tela inteira. O backend
 * ({@code GET /api/v1/contracts/{id}/pdf}) é responsável por montar o
 * PDF com cabeçalho dinâmico (logo + dados da Organization ativa).</p>
 */
export function ContractPrintPage() {
  const { id } = useParams<{ id: string }>()
  const [title, setTitle] = useState<string>('Contrato')

  useEffect(() => {
    if (!id) return
    let cancelled = false
    getContract(Number(id!))
      .then((c) => {
        if (!cancelled) setTitle(`Contrato nº ${c.code}`)
      })
      .catch(() => {/* mantém título genérico */})
    return () => {
      cancelled = true
    }
  }, [id])

  const fetcher = useCallback(async () => {
    if (!id) throw new Error('ID do contrato ausente.')
    return getContractPdf(Number(id!), 'inline')
  }, [id])

  return <PdfPreview title={title} fetcher={fetcher} />
}