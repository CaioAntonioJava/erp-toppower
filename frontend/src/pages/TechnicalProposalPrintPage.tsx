import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Printer, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { LogoTopPower } from '../components/ui/LogoTopPower'
import { getTechnicalProposal } from '../api/technicalProposal.api'
import { getProduct } from '../api/product.api'
import { toApiError } from '../lib/errors'
import type {
  TechnicalProposalResponse,
  TechnicalProposalStatus,
} from '../types/technicalProposal'
import {
  TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS,
  TECHNICAL_PROPOSAL_STATUS_LABELS,
} from '../types/technicalProposal'
import {
  DISCOUNT_TYPE_LABELS,
  FREIGHT_TYPE_LABELS,
  PAYMENT_CONDITION_LABELS,
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
function statusLabel(status: TechnicalProposalStatus): string {
  return TECHNICAL_PROPOSAL_STATUS_LABELS[status] ?? status
}

/** Renderiza o endereço de execução em uma única linha, quando há dados. */
function formatAddress(
  addr: TechnicalProposalResponse['address'],
): string | null {
  if (!addr) return null
  const line1 = [addr.street, addr.number].filter(Boolean).join(', ')
  const line2Raw = [addr.complement, addr.neighborhood].filter(Boolean).join(' — ')
  const cityState = [addr.city, addr.state].filter(Boolean).join('/')
  const cep = addr.zipCode ? ` — CEP ${addr.zipCode}` : ''
  const parts = [line1, line2Raw, cityState].filter(Boolean)
  if (parts.length === 0) return null
  return `${parts.join(' • ')}${cep}`
}

export function TechnicalProposalPrintPage() {
  const { id } = useParams<{ id: string }>()

  const [proposal, setProposal] = useState<TechnicalProposalResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // === resolução de nomes (produtos) ===
  // O `TechnicalProposalResponse` já traz o nome do cliente (`clientName` /
  // `clientCode`) resolvido no backend. Apenas os nomes de produtos ainda
  // precisam ser hidratados no frontend. Mantemos o UUID curto como fallback
  // caso a resolução falhe.
  const [productNames, setProductNames] = useState<Record<string, string>>({})
  const [namesResolved, setNamesResolved] = useState(false)

  // Guarda para não disparar window.print() mais de uma vez.
  const printedRef = useRef(false)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getTechnicalProposal(id)
      .then((data) => {
        if (cancelled) return
        setProposal(data)
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
    if (!proposal) return
    let cancelled = false

    // Produtos — dedupe por UUID para não buscar o mesmo item duas vezes.
    const uniqueProductUuids = Array.from(
      new Set(proposal.productItems.map((it) => it.productUuid)),
    )
    if (uniqueProductUuids.length === 0) {
      setProductNames({})
      setNamesResolved(true)
      return () => {
        cancelled = true
      }
    }
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
  }, [proposal])

  // === disparo automático do diálogo de impressão ===
  // Aguarda a proposta e os nomes estarem resolvidos antes de imprimir,
  // com um pequeno delay para garantir a pintura do layout.
  useEffect(() => {
    if (printedRef.current) return
    if (loading || error || !proposal || !namesResolved) return
    printedRef.current = true
    const t = setTimeout(() => {
      window.print()
    }, 300)
    return () => clearTimeout(t)
  }, [loading, error, proposal, namesResolved])

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center bg-white text-slate-900 no-print">
        <Spinner size="lg" />
      </div>
    )
  }

  if (error || !proposal) {
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

  // UUID "curto" usado como fallback visual quando o nome real não está
  // disponível no payload (cliente inativado/removido após a criação).
  const clientUuid =
    proposal.clientType === 'CUSTOMER'
      ? proposal.customerUuid
      : proposal.companyUuid
  const clientDisplay = proposal.clientName
    ? (proposal.clientCode
        ? `${proposal.clientCode} — ${proposal.clientName}`
        : proposal.clientName)
    : (clientUuid ? `${clientUuid.slice(0, 8)}…` : '—')

  // Endereço de execução (pode ser null).
  const executionAddress = formatAddress(proposal.address)

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
            Proposta Técnica nº {proposal.code}
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Status: {statusLabel(proposal.status)} • Início em{' '}
            {formatDate(proposal.startDate)}
            {proposal.endDate ? ` • Término: ${formatDate(proposal.endDate)}` : ''}
            {proposal.deliveryDate
              ? ` • Entrega: ${formatDate(proposal.deliveryDate)}`
              : ''}
          </p>
        </section>

        {/* Cliente / Responsável técnico. */}
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
                {TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS[proposal.clientType]}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Cliente
              </dt>
              <dd className="font-medium">{clientDisplay}</dd>
            </div>
            {proposal.technicalResponsible ? (
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500">
                  Responsável técnico
                </dt>
                <dd className="font-medium">{proposal.technicalResponsible}</dd>
              </div>
            ) : null}
            {proposal.email ? (
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500">
                  E-mail do responsável
                </dt>
                <dd className="font-medium">{proposal.email}</dd>
              </div>
            ) : null}
          </dl>
        </section>

        {/* Objetivos. */}
        {proposal.objectives.length > 0 ? (
          <section>
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Objetivos ({proposal.objectives.length})
            </h2>
            <ul className="list-inside list-disc space-y-1 text-sm text-slate-700">
              {proposal.objectives.map((o) => (
                <li key={o.uuid}>{o.description}</li>
              ))}
            </ul>
          </section>
        ) : null}

        {/* Endereço de execução. */}
        {executionAddress ? (
          <section>
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Endereço de execução
            </h2>
            <p className="text-sm text-slate-700">{executionAddress}</p>
          </section>
        ) : null}

        {/* Condições da proposta. */}
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
                {proposal.paymentCondition
                  ? PAYMENT_CONDITION_LABELS[proposal.paymentCondition]
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Desconto global
              </dt>
              <dd className="font-medium">
                {proposal.discountType
                  ? `${DISCOUNT_TYPE_LABELS[proposal.discountType]} — ${formatDiscount(
                      proposal.discountType,
                      proposal.discount,
                    )}`
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Tipo de entrega
              </dt>
              <dd className="font-medium">
                {proposal.deliveryType
                  ? FREIGHT_TYPE_LABELS[proposal.deliveryType]
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Valor do frete
              </dt>
              <dd className="font-mono">
                {proposal.freightValue != null
                  ? brlFormatter.format(proposal.freightValue)
                  : '—'}
              </dd>
            </div>
            {proposal.carrierName ? (
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500">
                  Transportadora
                </dt>
                <dd className="font-medium">{proposal.carrierName}</dd>
              </div>
            ) : null}
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Prazo de entrega
              </dt>
              <dd className="font-medium">
                {proposal.deliveryDeadline
                  ? formatDate(proposal.deliveryDeadline)
                  : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Validade da proposta
              </dt>
              <dd className="font-medium">
                {proposal.validity ? formatDate(proposal.validity) : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">
                Margem de lucro
              </dt>
              <dd className="font-mono">{proposal.profitMargin}%</dd>
            </div>
          </dl>
        </section>

        {/* Descrição detalhada. */}
        {proposal.description ? (
          <section>
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Descrição
            </h2>
            <div
              className="prose prose-sm max-w-none text-sm text-slate-700"
              dangerouslySetInnerHTML={{ __html: proposal.description }}
            />
          </section>
        ) : null}

        {/* Serviços prestados. */}
        {proposal.serviceItems.length > 0 ? (
          <section>
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Serviços prestados ({proposal.serviceItems.length})
            </h2>
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-300 text-left text-xs uppercase tracking-wide text-slate-500">
                  <th className="py-2 pr-4 font-medium">#</th>
                  <th className="py-2 px-4 font-medium">Descrição</th>
                  <th className="py-2 pl-4 text-right font-medium">Preço</th>
                </tr>
              </thead>
              <tbody>
                {proposal.serviceItems.map((it, idx) => (
                  <tr
                    key={it.uuid}
                    className="border-b border-slate-200 break-inside-avoid"
                  >
                    <td className="py-2 pr-4 font-mono text-slate-500">
                      {idx + 1}
                    </td>
                    <td className="py-2 px-4">{it.description}</td>
                    <td className="py-2 pl-4 text-right font-mono font-semibold">
                      {it.price != null
                        ? brlFormatter.format(it.price)
                        : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        ) : null}

        {/* Produtos. */}
        {proposal.productItems.length > 0 ? (
          <section>
            <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Produtos ({proposal.productItems.length})
            </h2>
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-300 text-left text-xs uppercase tracking-wide text-slate-500">
                  <th className="py-2 pr-4 font-medium">#</th>
                  <th className="py-2 px-4 font-medium">Produto</th>
                  <th className="py-2 px-4 text-right font-medium">Qtd.</th>
                  <th className="py-2 px-4 text-right font-medium">Preço un.</th>
                  <th className="py-2 px-4 text-right font-medium">Subtotal</th>
                  <th className="py-2 px-4 text-right font-medium">Desc.</th>
                  <th className="py-2 pl-4 text-right font-medium">Total</th>
                </tr>
              </thead>
              <tbody>
                {proposal.productItems.map((it, idx) => {
                  const productLabel = productNames[it.productUuid]
                  return (
                    <tr
                      key={it.uuid}
                      className="border-b border-slate-200 break-inside-avoid"
                    >
                      <td className="py-2 pr-4 font-mono text-slate-500">
                        {idx + 1}
                      </td>
                      <td className="py-2 px-4">
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
        ) : null}

        {/* Totais. */}
        <section className="flex justify-end">
          <dl className="w-full max-w-xs space-y-2 text-sm">
            <div className="flex justify-between gap-8">
              <dt className="text-slate-600">Subtotal de serviços</dt>
              <dd className="font-mono">
                {brlFormatter.format(proposal.servicesSubtotal)}
              </dd>
            </div>
            <div className="flex justify-between gap-8">
              <dt className="text-slate-600">Subtotal de produtos</dt>
              <dd className="font-mono">
                {brlFormatter.format(proposal.productsSubtotal)}
              </dd>
            </div>
            <div className="flex justify-between gap-8 border-t border-slate-200 pt-2">
              <dt className="text-slate-600">Subtotal</dt>
              <dd className="font-mono">
                {brlFormatter.format(proposal.subtotal)}
              </dd>
            </div>
            {proposal.globalDiscountValue > 0 ? (
              <div className="flex justify-between gap-8">
                <dt className="text-slate-600">Desconto global</dt>
                <dd className="font-mono">
                  - {brlFormatter.format(proposal.globalDiscountValue)}
                </dd>
              </div>
            ) : null}
            <div className="flex justify-between gap-8">
              <dt className="text-slate-600">Frete</dt>
              <dd className="font-mono">
                {proposal.freightValue != null
                  ? brlFormatter.format(proposal.freightValue)
                  : '—'}
              </dd>
            </div>
            <div className="flex justify-between gap-8 border-t border-slate-300 pt-2">
              <dt className="font-semibold">Total</dt>
              <dd className="font-mono text-base font-bold">
                {brlFormatter.format(proposal.total)}
              </dd>
            </div>
          </dl>
        </section>

        {/* Observações. */}
        {proposal.notes ? (
          <section>
            <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
              Observações
            </h2>
            <div
              className="prose prose-sm max-w-none text-sm text-slate-700"
              dangerouslySetInnerHTML={{ __html: proposal.notes }}
            />
          </section>
        ) : null}

        {/* Assinaturas. */}
        <section className="mt-16 grid grid-cols-2 gap-12 text-center text-xs text-slate-500">
          <div>
            <div className="border-t border-slate-400 pt-1">
              {proposal.technicalResponsible ?? 'Responsável técnico'}
            </div>
            <p>Responsável Técnico</p>
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
