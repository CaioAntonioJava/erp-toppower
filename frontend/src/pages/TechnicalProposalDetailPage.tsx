import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CheckCircle2, Play, RotateCcw, Wrench } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { BackButton } from '../components/ui/BackButton'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { TechnicalProposalStatusBadge } from '../components/sales/TechnicalProposalStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  completeTechnicalProposal,
  getTechnicalProposal,
  reopenTechnicalProposal,
  startTechnicalProposal,
} from '../api/technicalProposal.api'
import { getProduct } from '../api/product.api'
import { toApiError } from '../lib/errors'
import type { TechnicalProposalResponse } from '../types/technicalProposal'
import {
  DISCOUNT_TYPE_LABELS,
  FREIGHT_TYPE_LABELS,
  PAYMENT_CONDITION_LABELS,
} from '../types/quotation'
import { TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS } from '../types/technicalProposal'
import { useAuth } from '../context/AuthContext'

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

function formatDiscount(
  type: string | null | undefined,
  value: number | null | undefined,
): string {
  if (type == null || value == null) return '—'
  if (type === 'PERCENT') return `${value}%`
  return brlFormatter.format(value)
}

export function TechnicalProposalDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [proposal, setProposal] = useState<TechnicalProposalResponse | null>(
    null,
  )
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Transições
  const [confirmStart, setConfirmStart] = useState(false)
  const [confirmComplete, setConfirmComplete] = useState(false)
  const [confirmReopen, setConfirmReopen] = useState(false)
  const [transitioning, setTransitioning] = useState(false)
  const [transitionError, setTransitionError] = useState<string | null>(null)

  // Resolução de nomes de produtos.
  const [productNames, setProductNames] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getTechnicalProposal(id)
      .then((data) => {
        if (cancelled) return
        setProposal(data)
        // Hidrata nomes de produtos.
        const uniqueUuids = Array.from(
          new Set(data.productItems.map((p) => p.productUuid)),
        )
        Promise.all(
          uniqueUuids.map(async (uuid) => {
            try {
              const p = await getProduct(uuid)
              return [uuid, p.name] as const
            } catch {
              return [uuid, uuid.slice(0, 8) + '…'] as const
            }
          }),
        ).then((entries) => {
          if (cancelled) return
          setProductNames(Object.fromEntries(entries))
        })
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

  async function runTransition(
    fn: (uuid: string) => Promise<TechnicalProposalResponse>,
  ) {
    if (!proposal) return
    setTransitioning(true)
    setTransitionError(null)
    try {
      const updated = await fn(proposal.uuid)
      setProposal(updated)
      setConfirmStart(false)
      setConfirmComplete(false)
      setConfirmReopen(false)
    } catch (err) {
      setTransitionError(toApiError(err).message)
    } finally {
      setTransitioning(false)
    }
  }

  const canStart = proposal?.status === 'ABERTA'
  const canComplete = proposal?.status === 'EM_ANDAMENTO'
  const canReopen = proposal?.status === 'CONCLUIDA'

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton
            label="Voltar para a lista"
            fallback="/technical-proposals"
          />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            Proposta Técnica
          </h1>
          {proposal ? (
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono">{proposal.code}</span>
              <span aria-hidden>•</span>
              <span>Início: {formatDate(proposal.startDate)}</span>
              {proposal.endDate ? (
                <>
                  <span aria-hidden>•</span>
                  <span>Término: {formatDate(proposal.endDate)}</span>
                </>
              ) : null}
              {proposal.deliveryDate ? (
                <>
                  <span aria-hidden>•</span>
                  <span>Entrega: {formatDate(proposal.deliveryDate)}</span>
                </>
              ) : null}
              <span aria-hidden>•</span>
              <TechnicalProposalStatusBadge status={proposal.status} />
            </div>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {proposal ? (
            <>
              <Button
                variant="secondary"
                onClick={() =>
                  navigate(`/technical-proposals/${proposal.uuid}/edit`)
                }
              >
                <Wrench className="h-4 w-4" />
                Editar
              </Button>
              {canStart ? (
                <Button
                  variant="secondary"
                  onClick={() => setConfirmStart(true)}
                >
                  <Play className="h-4 w-4" />
                  Iniciar execução
                </Button>
              ) : null}
              {canComplete ? (
                <Button onClick={() => setConfirmComplete(true)}>
                  <CheckCircle2 className="h-4 w-4" />
                  Concluir
                </Button>
              ) : null}
              {canReopen ? (
                <Button
                  variant="secondary"
                  onClick={() => setConfirmReopen(true)}
                >
                  <RotateCcw className="h-4 w-4" />
                  Reabrir
                </Button>
              ) : null}
            </>
          ) : null}
        </div>
      </div>

      {transitionError ? (
        <Alert variant="error">{transitionError}</Alert>
      ) : null}

      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : error ? (
        <Alert variant="error">
          {error}. <BackButton fallback="/technical-proposals" />
        </Alert>
      ) : proposal ? (
        <>
          {/* Resumo do cabeçalho */}
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Cliente
                </dt>
                <dd className="mt-1 font-medium text-slate-900 dark:text-slate-100">
                  {proposal.clientName ?? '—'}
                </dd>
                <dd className="text-xs text-slate-500 dark:text-slate-400">
                  {proposal.clientCode ?? ''}
                  {proposal.clientType
                    ? ` • ${TECHNICAL_PROPOSAL_CLIENT_TYPE_LABELS[proposal.clientType]}`
                    : ''}
                </dd>
              </div>
              <div className="sm:col-span-2 lg:col-span-3">
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Objetivos
                </dt>
                <dd className="mt-1">
                  {proposal.objectives.length === 0 ? (
                    <span className="text-slate-500 dark:text-slate-400">—</span>
                  ) : (
                    <ul className="list-inside list-disc space-y-1 text-slate-900 dark:text-slate-100">
                      {proposal.objectives.map((o) => (
                        <li key={o.uuid}>{o.description}</li>
                      ))}
                    </ul>
                  )}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Prazo de entrega
                </dt>
                <dd className="mt-1 text-slate-900 dark:text-slate-100">
                  {proposal.deliveryDeadline ?? '—'}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Validade da proposta
                </dt>
                <dd className="mt-1 text-slate-900 dark:text-slate-100">
                  {proposal.validity ?? '—'}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Condição de pagamento
                </dt>
                <dd className="mt-1 text-slate-900 dark:text-slate-100">
                  {proposal.paymentCondition
                    ? PAYMENT_CONDITION_LABELS[proposal.paymentCondition]
                    : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Tipo de entrega
                </dt>
                <dd className="mt-1 text-slate-900 dark:text-slate-100">
                  {proposal.deliveryType
                    ? FREIGHT_TYPE_LABELS[proposal.deliveryType]
                    : '—'}
                </dd>
              </div>
            </dl>

            {proposal.address ? (
              <div className="mt-6 border-t border-slate-200 pt-4 dark:border-slate-800">
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                  Endereço de execução
                </h3>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                  {[
                    proposal.address.street,
                    proposal.address.number,
                    proposal.address.complement,
                    proposal.address.neighborhood,
                    proposal.address.city,
                    proposal.address.state,
                    proposal.address.zipCode,
                  ]
                    .filter(Boolean)
                    .join(', ') || '—'}
                </p>
              </div>
            ) : null}

            {proposal.description ? (
              <div className="mt-6 border-t border-slate-200 pt-4 dark:border-slate-800">
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                  Descrição detalhada
                </h3>
                <div
                  className="prose prose-sm mt-1 max-w-none text-slate-700 dark:prose-invert dark:text-slate-300"
                  dangerouslySetInnerHTML={{ __html: proposal.description }}
                />
              </div>
            ) : null}

            {proposal.notes ? (
              <div className="mt-6 border-t border-slate-200 pt-4 dark:border-slate-800">
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                  Observações
                </h3>
                <div
                  className="prose prose-sm mt-1 max-w-none text-slate-700 dark:prose-invert dark:text-slate-300"
                  dangerouslySetInnerHTML={{ __html: proposal.notes }}
                />
              </div>
            ) : null}
          </section>

          {/* Itens de serviço */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="border-b border-slate-200 px-6 py-4 dark:border-slate-800">
              <h2 className="text-base font-semibold">Serviços prestados</h2>
            </div>
            {proposal.serviceItems.length === 0 ? (
              <p className="px-6 py-6 text-sm text-slate-500 dark:text-slate-400">
                Nenhum serviço informado.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                  <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                    <tr>
                      <th className="px-4 py-3 font-medium">#</th>
                      <th className="px-4 py-3 font-medium">Descrição</th>
                      <th className="px-4 py-3 text-right font-medium">Preço</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                    {proposal.serviceItems.map((s, idx) => (
                      <tr key={s.uuid}>
                        <td className="px-4 py-3 text-xs text-slate-500 dark:text-slate-400">
                          {String(idx + 1).padStart(2, '0')}
                        </td>
                        <td className="px-4 py-3 text-slate-700 dark:text-slate-200">
                          {s.description}
                        </td>
                        <td className="px-4 py-3 text-right font-mono text-xs text-slate-700 dark:text-slate-200">
                          {s.price != null ? brlFormatter.format(s.price) : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {/* Itens de produto */}
          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="border-b border-slate-200 px-6 py-4 dark:border-slate-800">
              <h2 className="text-base font-semibold">Produtos</h2>
            </div>
            {proposal.productItems.length === 0 ? (
              <p className="px-6 py-6 text-sm text-slate-500 dark:text-slate-400">
                Nenhum produto informado.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                  <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                    <tr>
                      <th className="px-4 py-3 font-medium">#</th>
                      <th className="px-4 py-3 font-medium">Produto</th>
                      <th className="px-4 py-3 text-right font-medium">Qtde</th>
                      <th className="px-4 py-3 text-right font-medium">Preço unit.</th>
                      <th className="px-4 py-3 text-right font-medium">Desconto</th>
                      <th className="px-4 py-3 text-right font-medium">Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                    {proposal.productItems.map((p, idx) => (
                      <tr key={p.uuid}>
                        <td className="px-4 py-3 text-xs text-slate-500 dark:text-slate-400">
                          {String(idx + 1).padStart(2, '0')}
                        </td>
                        <td className="px-4 py-3 text-slate-700 dark:text-slate-200">
                          {productNames[p.productUuid] ?? p.productUuid.slice(0, 8) + '…'}
                        </td>
                        <td className="px-4 py-3 text-right text-slate-700 dark:text-slate-200">
                          {p.quantity}
                        </td>
                        <td className="px-4 py-3 text-right font-mono text-xs text-slate-700 dark:text-slate-200">
                          {brlFormatter.format(p.unitPrice)}
                        </td>
                        <td className="px-4 py-3 text-right text-slate-600 dark:text-slate-300">
                          {formatDiscount(p.discountType, p.discount)}
                        </td>
                        <td className="px-4 py-3 text-right font-mono text-xs font-semibold text-slate-700 dark:text-slate-200">
                          {brlFormatter.format(p.totalPrice)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {/* Totais */}
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <h3 className="mb-4 text-base font-semibold">Totais</h3>
            <dl className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Subtotal de serviços
                </dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                  {brlFormatter.format(proposal.servicesSubtotal)}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Subtotal de produtos
                </dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                  {brlFormatter.format(proposal.productsSubtotal)}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Subtotal geral
                </dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                  {brlFormatter.format(proposal.subtotal)}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Margem de lucro
                </dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                  {proposal.profitMargin}%
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Desconto global ({proposal.discountType ? DISCOUNT_TYPE_LABELS[proposal.discountType] : '—'})
                </dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                  {brlFormatter.format(proposal.globalDiscountValue)}
                </dd>
              </div>
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Frete
                </dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900 dark:text-slate-100">
                  {proposal.freightValue != null
                    ? brlFormatter.format(proposal.freightValue)
                    : '—'}
                </dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Total final
                </dt>
                <dd className="mt-1 text-2xl font-semibold text-primary-700 dark:text-primary-200">
                  {brlFormatter.format(proposal.total)}
                </dd>
              </div>
            </dl>
          </section>

          {isAdmin ? (
            <RegistrationAuditCard
              createdBy={proposal.createdBy}
              createdAt={proposal.createdAt}
              updatedBy={proposal.updatedBy}
              updatedAt={proposal.updatedAt}
            />
          ) : null}
        </>
      ) : null}

      <ConfirmDialog
        open={confirmStart}
        title="Iniciar execução?"
        description="A proposta passará do status ABERTA para EM_ANDAMENTO."
        confirmText="Iniciar"
        confirmVariant="primary"
        isLoading={transitioning}
        onConfirm={() => runTransition(startTechnicalProposal)}
        onClose={() => {
          if (!transitioning) setConfirmStart(false)
        }}
      />
      <ConfirmDialog
        open={confirmComplete}
        title="Concluir execução?"
        description="A proposta passará para CONCLUIDA e a data de entrega será preenchida com a data de hoje."
        confirmText="Concluir"
        confirmVariant="primary"
        isLoading={transitioning}
        onConfirm={() =>
          runTransition(completeTechnicalProposal)
        }
        onClose={() => {
          if (!transitioning) setConfirmComplete(false)
        }}
      />
      <ConfirmDialog
        open={confirmReopen}
        title="Reabrir proposta?"
        description="A proposta voltará para EM_ANDAMENTO e a data de entrega será removida."
        confirmText="Reabrir"
        confirmVariant="primary"
        isLoading={transitioning}
        onConfirm={() => runTransition(reopenTechnicalProposal)}
        onClose={() => {
          if (!transitioning) setConfirmReopen(false)
        }}
      />
    </div>
  )
}