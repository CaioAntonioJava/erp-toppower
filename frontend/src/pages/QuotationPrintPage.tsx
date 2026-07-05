import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Printer, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { LogoTopPower } from '../components/ui/LogoTopPower'
import { getQuotation } from '../api/quotation.api'
import { getCustomer } from '../api/customer.api'
import { getCompany } from '../api/company.api'
import { getProduct } from '../api/product.api'
import { getCarrier } from '../api/carrier.api'
import { toApiError } from '../lib/errors'
import type {
  QuotationResponse,
  QuotationStatus,
} from '../types/quotation'
import {
  DISCOUNT_TYPE_LABELS,
  FREIGHT_TYPE_LABELS,
  PAYMENT_CONDITION_LABELS,
  QUOTATION_CLIENT_TYPE_LABELS,
  QUOTATION_STATUS_LABELS,
} from '../types/quotation'

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
function statusLabel(status: QuotationStatus): string {
  return QUOTATION_STATUS_LABELS[status] ?? status
}

export function QuotationPrintPage() {
  const { id } = useParams<{ id: string }>()

  const [quotation, setQuotation] = useState<QuotationResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // === resolução de nomes (cliente, produtos e transportadora) ===
  // O `QuotationResponse` traz apenas UUIDs de cliente/produtos/transportadora;
  // o nome do vendedor já vem resolvido no payload (`sellerName`). Para o PDF
  // buscamos os nomes reais em paralelo. Mantemos o UUID curto como fallback
  // caso a resolução falhe (registro inativado, removido, ou erro de rede).
  const [clientName, setClientName] = useState<string | null>(null)
  const [carrierName, setCarrierName] = useState<string | null>(null)
  const [productNames, setProductNames] = useState<Record<string, string>>({})
  const [namesResolved, setNamesResolved] = useState(false)

  // Guarda para não disparar window.print() mais de uma vez.
  const printedRef = useRef(false)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getQuotation(id)
      .then((data) => {
        if (cancelled) return
        setQuotation(data)
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
    if (!quotation) return
    let cancelled = false

    // Cliente (PF ou PJ) — busca conforme o tipo persistido.
    const clientUuid =
      quotation.clientType === 'CUSTOMER'
        ? quotation.customerUuid
        : quotation.companyUuid
    const clientPromise =
      clientUuid != null
        ? quotation.clientType === 'CUSTOMER'
          ? getCustomer(clientUuid)
              .then((c) => `${c.code} — ${c.name}`)
              .catch(() => null)
          : getCompany(clientUuid)
              .then((c) => `${c.code} — ${c.legalName}`)
              .catch(() => null)
        : Promise.resolve(null)

    // Vendedor — o nome já vem resolvido no payload (`sellerName`), então
    // não precisamos de um round-trip adicional a GET /sellers/{id}.

    // Transportadora — opcional; só busca se houver FK.
    const carrierPromise = quotation.carrierUuid
      ? getCarrier(quotation.carrierUuid)
          .then((c) => c.carrierName)
          .catch(() => null)
      : Promise.resolve(null)

    // Produtos — dedupe por UUID para não buscar o mesmo item duas vezes.
    const uniqueProductUuids = Array.from(
      new Set(quotation.items.map((it) => it.productUuid)),
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

    Promise.all([clientPromise, carrierPromise, productEntriesPromise])
      .then(([client, carrier, products]) => {
        if (cancelled) return
        setClientName(client)
        setCarrierName(carrier)
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
  }, [quotation])

  // === disparo automático do diálogo de impressão ===
  // Aguarda a proposta e os nomes estarem resolvidos antes de imprimir,
  // com um pequeno delay para garantir a pintura do layout.
  useEffect(() => {
    if (printedRef.current) return
    if (loading || error || !quotation || !namesResolved) return
    printedRef.current = true
    const t = setTimeout(() => {
      window.print()
    }, 300)
    return () => clearTimeout(t)
  }, [loading, error, quotation, namesResolved])

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center bg-white text-slate-900 no-print">
        <Spinner size="lg" />
      </div>
    )
  }

  if (error || !quotation) {
    return (
      <div className="space-y-4 bg-white p-6 text-slate-900 no-print">
        <Button
          variant="ghost"
          onClick={() => window.close()}
        >
          <X className="h-4 w-4" />
          Fechar
        </Button>
        <Alert variant="error">{error ?? 'Proposta não encontrada.'}</Alert>
      </div>
    )
  }

  // UUIDs "curtos" usados como fallback visual enquanto os nomes reais
  // não chegam (ou quando a resolução falha).
  const clientUuid =
    quotation.clientType === 'CUSTOMER'
      ? quotation.customerUuid
      : quotation.companyUuid
  const clientDisplay = clientName ?? (clientUuid ? `${clientUuid.slice(0, 8)}…` : '—')
  const sellerDisplay = quotation.sellerName ?? 'Vendedor não encontrado'

  // Desconto global em valor monetário (calculado pelo backend). Usado
  // apenas para a linha de totais no PDF — o valor já considera a margem
  // de lucro aplicada sobre o subtotal dos itens.
  const globalDiscountAmount = quotation.globalDiscountValue

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

        {/* Título da proposta. */}
        <section>
          <h1 className="text-xl font-bold uppercase tracking-tight">
            Proposta Comercial nº {quotation.number}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Status: {statusLabel(quotation.status)} • Emitida em{' '}
            {formatDate(quotation.issueDate)}
            {quotation.validityDays != null
              ? ` • Validade: ${quotation.validityDays} dia(s)`
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
                {QUOTATION_CLIENT_TYPE_LABELS[quotation.clientType]}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Cliente
              </dt>
              <dd className="font-medium">{clientDisplay}</dd>
            </div>
            {quotation.attention ? (
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500">
                  Aos cuidados de
                </dt>
                <dd className="font-medium">{quotation.attention}</dd>
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
                {quotation.paymentCondition
                  ? PAYMENT_CONDITION_LABELS[quotation.paymentCondition]
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Desconto global
              </dt>
              <dd className="font-medium">
                {quotation.discountType
                  ? `${DISCOUNT_TYPE_LABELS[quotation.discountType]} — ${formatDiscount(
                      quotation.discountType,
                      quotation.discount,
                    )}`
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Tipo de frete
              </dt>
              <dd className="font-medium">
                {quotation.freightType
                  ? FREIGHT_TYPE_LABELS[quotation.freightType]
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Transportadora
              </dt>
              <dd className="font-medium">
                {quotation.carrierUuid
                  ? (carrierName ?? `${quotation.carrierUuid.slice(0, 8)}…`)
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Valor do frete
              </dt>
              <dd className="font-mono">
                {quotation.freightValue != null
                  ? brlFormatter.format(quotation.freightValue)
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Validade
              </dt>
              <dd className="font-medium">
                {quotation.validityDays != null
                  ? `${quotation.validityDays} dia(s)`
                  : '—'}
              </dd>
            </div>
          </dl>
        </section>

        {/* Itens. */}
        <section>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Itens ({quotation.items.length} • {quotation.totalQuantity}{' '}
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
              {quotation.items.map((it) => {
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
              <dd className="font-mono">{brlFormatter.format(quotation.subtotal)}</dd>
            </div>
            <div className="flex justify-between gap-8">
              <dt className="text-slate-600">Desconto global</dt>
              <dd className="font-mono">- {brlFormatter.format(globalDiscountAmount)}</dd>
            </div>
            <div className="flex justify-between gap-8">
              <dt className="text-slate-600">Frete</dt>
              <dd className="font-mono">
                {quotation.freightValue != null
                  ? brlFormatter.format(quotation.freightValue)
                  : '—'}
              </dd>
            </div>
            <div className="flex justify-between gap-8 border-t border-slate-300 pt-2">
              <dt className="font-semibold">Total</dt>
              <dd className="font-mono text-base font-bold">
                {brlFormatter.format(quotation.total)}
              </dd>
            </div>
          </dl>
        </section>

        {/* Observações. */}
        {quotation.notes ? (
          <section>
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Observações
            </h2>
            <p className="whitespace-pre-wrap text-sm text-slate-700">
              {quotation.notes}
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