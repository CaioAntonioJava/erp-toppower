import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Printer, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { LogoTopPower } from '../components/ui/LogoTopPower'
import { getSalesOrder } from '../api/salesOrder.api'
import { getProduct } from '../api/product.api'
import { toApiError } from '../lib/errors'
import type {
  SalesOrderResponse,
  SalesOrderStatus,
} from '../types/salesOrder'
import {
  DISCOUNT_TYPE_LABELS,
  FREIGHT_TYPE_LABELS,
  PAYMENT_CONDITION_LABELS,
  SALES_ORDER_CLIENT_TYPE_LABELS,
  SALES_ORDER_STATUS_LABELS,
} from '../types/salesOrder'

/** Formatador de moeda BRL. */
const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDiscount(
  type: string | null | undefined,
  value: number | null | undefined,
): string {
  if (type == null || value == null) return '—'
  if (type === 'PERCENT') return `${value}%`
  return brlFormatter.format(value)
}

/** Rótulo textual do status (sem badge colorido) para saída de impressão. */
function statusLabel(status: SalesOrderStatus): string {
  return SALES_ORDER_STATUS_LABELS[status] ?? status
}

export function SalesOrderPrintPage() {
  const { id } = useParams<{ id: string }>()

  const [salesOrder, setSalesOrder] = useState<SalesOrderResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // === resolução de nomes (produtos) ===
  // O `SalesOrderResponse` já traz o nome do cliente (`clientName`/`clientCode`)
  // e do vendedor (`sellerName`) resolvidos no backend. Apenas os nomes de
  // produtos ainda precisam ser hidratados no frontend.
  // Mantemos o UUID curto como fallback caso a resolução falhe.
  const [productNames, setProductNames] = useState<Record<string, string>>({})
  const [namesResolved, setNamesResolved] = useState(false)

  // Guarda para não disparar window.print() mais de uma vez.
  const printedRef = useRef(false)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getSalesOrder(id)
      .then((data) => {
        if (cancelled) return
        setSalesOrder(data)
      })
      .catch((err) => {
        if (cancelled) return
        setError(toApiError(err).message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id])

  useEffect(() => {
    if (!salesOrder) return
    let cancelled = false

    // Produtos — dedupe por UUID para não buscar o mesmo item duas vezes.
    const uniqueProductUuids = Array.from(
      new Set(salesOrder.items.map((it) => it.productUuid)),
    )
    const productEntriesPromise = Promise.all(
      uniqueProductUuids.map(async (uuid) => {
        try {
          const p = await getProduct(uuid)
          const label = p.code ? `${p.code} — ${p.name}` : p.name
          return [uuid, label] as const
        } catch {
          return [uuid, null] as const
        }
      }),
    ).then((entries) => {
      const map: Record<string, string> = {}
      for (const [uuid, label] of entries) {
        if (label != null) map[uuid] = label
      }
      return map
    })

    productEntriesPromise
      .then((products) => {
        if (cancelled) return
        setProductNames(products)
        setNamesResolved(true)
      })
      .catch(() => {
        /* mantém fallbacks (UUID curto) */
        if (!cancelled) setNamesResolved(true)
      })

    return () => {
      cancelled = true
    }
  }, [salesOrder])

  // === disparo automático do diálogo de impressão ===
  // Aguarda o pedido e os nomes estarem resolvidos antes de imprimir,
  // com um pequeno delay para garantir a pintura do layout.
  useEffect(() => {
    if (printedRef.current) return
    if (loading || error || !salesOrder || !namesResolved) return
    printedRef.current = true
    const t = setTimeout(() => {
      window.print()
    }, 300)
    return () => clearTimeout(t)
  }, [loading, error, salesOrder, namesResolved])

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center bg-white text-slate-900 no-print">
        <Spinner size="lg" />
      </div>
    )
  }

  if (error || !salesOrder) {
    return (
      <div className="space-y-4 bg-white p-6 text-slate-900 no-print">
        <Button
          variant="ghost"
          onClick={() => window.close()}
        >
          <X className="h-4 w-4" />
          Fechar
        </Button>
        <Alert variant="error">{error ?? 'Pedido não encontrado.'}</Alert>
      </div>
    )
  }

  // UUID "curto" usado como fallback visual quando o nome real não está
  // disponível no payload (cliente inativado/removido após a criação).
  const clientUuid =
    salesOrder.clientType === 'CUSTOMER'
      ? salesOrder.customerUuid
      : salesOrder.companyUuid
  const clientDisplay = salesOrder.clientName
    ? (salesOrder.clientCode
        ? `${salesOrder.clientCode} — ${salesOrder.clientName}`
        : salesOrder.clientName)
    : (clientUuid ? `${clientUuid.slice(0, 8)}…` : '—')
  const sellerDisplay = salesOrder.sellerName ?? 'Vendedor não encontrado'

  return (
    <div className="min-h-screen bg-white px-8 py-10 text-slate-900 print:bg-white">
      {/* Barra de ações — não aparece na impressão. */}
      <div className="mb-6 flex justify-end gap-2 no-print">
        <Button variant="secondary" onClick={() => window.print()}>
          <Printer className="h-4 w-4" />
          Imprimir / Salvar PDF
        </Button>
        <Button variant="ghost" onClick={() => window.close()}>
          <X className="h-4 w-4" />
          Fechar
        </Button>
      </div>

      {/* Conteúdo impressível. */}
      <div className="mx-auto max-w-[800px] space-y-8 print:max-w-none">
        {/* Cabeçalho com logo e dados do emissor. */}
        <header className="flex items-start justify-between gap-6 border-b border-slate-300 pb-6">
          <LogoTopPower className="h-16 w-auto" />
          {/* TODO: substituir por dados reais da empresa emissora quando
              disponíveis no backend. */}
          <div className="text-right text-xs leading-relaxed text-slate-600">
            <p className="text-sm font-semibold text-slate-900">TOP POWER</p>
            <p>CNPJ: 00.000.000/0001-00</p>
            <p>Telefone: (00) 0000-0000</p>
            <p>contato@toppower.com.br</p>
          </div>
        </header>

        {/* Título do pedido. */}
        <section>
          <h1 className="text-xl font-bold uppercase tracking-tight">
            Pedido de Venda nº {salesOrder.number}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Status: {statusLabel(salesOrder.status)} • Emitido em{' '}
            {formatDate(salesOrder.orderDate)}
            {salesOrder.quotationNumber != null
              ? ` • Originado da proposta ${salesOrder.quotationNumber}`
              : ''}
          </p>
        </section>

        {/* Cliente / Vendedor. */}
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Cliente
          </h2>
          <dl className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Tipo de cliente
              </dt>
              <dd className="font-medium">
                {SALES_ORDER_CLIENT_TYPE_LABELS[salesOrder.clientType]}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Cliente
              </dt>
              <dd className="font-medium">{clientDisplay}</dd>
            </div>
            {salesOrder.attention ? (
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500">
                  Aos cuidados de
                </dt>
                <dd className="font-medium">{salesOrder.attention}</dd>
              </div>
            ) : null}
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Vendedor
              </dt>
              <dd className="font-medium">{sellerDisplay}</dd>
            </div>
          </dl>
        </section>

        {/* Condições comerciais. */}
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Condições
          </h2>
          <dl className="grid grid-cols-2 gap-x-8 gap-y-3 text-sm">
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Condição de pagamento
              </dt>
              <dd className="font-medium">
                {salesOrder.paymentCondition
                  ? PAYMENT_CONDITION_LABELS[salesOrder.paymentCondition]
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Desconto global
              </dt>
              <dd className="font-medium">
                {salesOrder.discountType
                  ? `${DISCOUNT_TYPE_LABELS[salesOrder.discountType]} — ${formatDiscount(
                      salesOrder.discountType,
                      salesOrder.discount,
                    )}`
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Tipo de frete
              </dt>
              <dd className="font-medium">
                {salesOrder.freightType
                  ? FREIGHT_TYPE_LABELS[salesOrder.freightType]
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Valor do frete
              </dt>
              <dd className="font-mono">
                {salesOrder.freightValue != null
                  ? brlFormatter.format(salesOrder.freightValue)
                  : '—'}
              </dd>
            </div>
          </dl>
        </section>

        {/* Itens. */}
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Itens ({salesOrder.items.length} • {salesOrder.totalQuantity}{' '}
            unidade(s))
          </h2>
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-300 text-left text-xs uppercase tracking-wide text-slate-500">
                <th className="py-2 pr-4 font-medium">Produto</th>
                <th className="py-2 px-4 text-right font-medium">Qtd.</th>
                <th className="py-2 px-4 text-right font-medium">Preço un.</th>
                <th className="py-2 px-4 text-right font-medium">Subtotal</th>
                <th className="py-2 px-4 text-right font-medium">Desc.</th>
                <th className="py-2 pl-4 text-right font-medium">Total</th>
              </tr>
            </thead>
            <tbody>
              {salesOrder.items.map((it) => {
                const productLabel = productNames[it.productUuid]
                return (
                  <tr
                    key={it.uuid}
                    className="border-b border-slate-200 break-inside-avoid"
                  >
                    <td className="py-2 pr-4">
                      {productLabel != null
                        ? productLabel
                        : `${it.productUuid.slice(0, 8)}…`}
                    </td>
                    <td className="py-2 px-4 text-right font-mono">
                      {it.quantity}
                    </td>
                    <td className="py-2 px-4 text-right font-mono">
                      {brlFormatter.format(it.unitPrice)}
                    </td>
                    <td className="py-2 px-4 text-right font-mono">
                      {brlFormatter.format(it.lineSubtotal)}
                    </td>
                    <td className="py-2 px-4 text-right font-mono">
                      {formatDiscount(it.discountType, it.discount)}
                    </td>
                    <td className="py-2 pl-4 text-right font-mono font-semibold">
                      {brlFormatter.format(it.totalPrice)}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </section>

        {/* Totais. */}
        <section className="flex justify-end">
          <dl className="w-full max-w-xs space-y-2 text-sm">
            <div className="flex justify-between gap-8">
              <dt className="text-slate-600">Subtotal</dt>
              <dd className="font-mono">{brlFormatter.format(salesOrder.subtotal)}</dd>
            </div>
            <div className="flex justify-between gap-8">
              <dt className="text-slate-600">Desconto global</dt>
              <dd className="font-mono">- {brlFormatter.format(salesOrder.globalDiscountValue)}</dd>
            </div>
            <div className="flex justify-between gap-8">
              <dt className="text-slate-600">Frete</dt>
              <dd className="font-mono">
                {salesOrder.freightValue != null
                  ? brlFormatter.format(salesOrder.freightValue)
                  : '—'}
              </dd>
            </div>
            <div className="flex justify-between gap-8 border-t border-slate-300 pt-2">
              <dt className="font-semibold">Total</dt>
              <dd className="font-mono text-base font-bold">
                {brlFormatter.format(salesOrder.total)}
              </dd>
            </div>
          </dl>
        </section>

        {/* Observações. */}
        {salesOrder.notes ? (
          <section>
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Observações
            </h2>
            <p className="whitespace-pre-wrap text-sm text-slate-700">
              {salesOrder.notes}
            </p>
          </section>
        ) : null}

        {/* Assinaturas. */}
        <section className="mt-16 grid grid-cols-2 gap-12 text-center text-xs text-slate-500">
          <div>
            <div className="border-t border-slate-400 pt-1">{sellerDisplay}</div>
            <p>Vendedor</p>
          </div>
          <div>
            <div className="border-t border-slate-400 pt-1">{clientDisplay}</div>
            <p>Cliente</p>
          </div>
        </section>

        {/* Rodapé de geração. */}
        <footer className="border-t border-slate-200 pt-4 text-center text-[10px] text-slate-400">
          Documento gerado em {formatDateTime(new Date().toISOString())}
        </footer>
      </div>
    </div>
  )
}