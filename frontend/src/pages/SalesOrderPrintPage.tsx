import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getSalesOrder, getSalesOrderPdf } from '../api/salesOrder.api'
import { PdfPreview } from '../components/ui/PdfPreview'

/**
 * Preview do Pedido de Venda em PDF (gerado pelo backend).
 *
 * <p>Esta página é renderizada SEM o AppLayout (rota sem sidebar) para
 * que o preview ocupe a tela inteira. O backend ({@code GET
 * /api/v1/sales-orders/{id}/pdf}) é responsável por montar o PDF com
 * cabeçalho dinâmico (logo + dados da Organization ativa).</p>
 */
export function SalesOrderPrintPage() {
  const { id } = useParams<{ id: string }>()
  const [title, setTitle] = useState<string>('Pedido de Venda')

  useEffect(() => {
    if (!id) return
    let cancelled = false
    getSalesOrder(Number(id!))
      .then((o) => {
        if (!cancelled) setTitle(`Pedido de Venda nº ${o.code}`)
      })
      .catch(() => {/* mantém título genérico */})
    return () => { cancelled = true }
  }, [id])

  const fetcher = useCallback(async () => {
    if (!id) throw new Error('ID do pedido ausente.')
    return getSalesOrderPdf(Number(id!), 'inline')
  }, [id])

  return <PdfPreview title={title} fetcher={fetcher} />
}