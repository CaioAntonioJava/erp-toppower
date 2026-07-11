import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  CheckCircle2,
  Pencil,
  Play,
  Printer,
  RotateCcw,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { BackButton } from '../components/ui/BackButton'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { ContractStatusBadge } from '../components/contract/ContractStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  completeContract,
  getContract,
  reopenContract,
  startContract,
} from '../api/contract.api'
import { toApiError } from '../lib/errors'
import type { ContractResponse } from '../types/contract'
import { useAuth } from '../context/AuthContext'

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

export function ContractDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [contract, setContract] = useState<ContractResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Transições
  const [confirmStart, setConfirmStart] = useState(false)
  const [confirmComplete, setConfirmComplete] = useState(false)
  const [confirmReopen, setConfirmReopen] = useState(false)
  const [transitioning, setTransitioning] = useState(false)
  const [transitionError, setTransitionError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getContract(id)
      .then((data) => {
        if (cancelled) return
        setContract(data)
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
    fn: (uuid: string) => Promise<ContractResponse>,
  ) {
    if (!contract) return
    setTransitioning(true)
    setTransitionError(null)
    try {
      const updated = await fn(contract.uuid)
      setContract(updated)
      setConfirmStart(false)
      setConfirmComplete(false)
      setConfirmReopen(false)
    } catch (err) {
      setTransitionError(toApiError(err).message)
    } finally {
      setTransitioning(false)
    }
  }

  const canStart = contract?.status === 'ABERTA'
  const canComplete = contract?.status === 'EM_ANDAMENTO'
  const canReopen = contract?.status === 'CONCLUIDA'

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton label="Voltar para a lista" fallback="/contracts" />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            Contrato
          </h1>
          {contract ? (
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono">{contract.code}</span>
              <span aria-hidden>•</span>
              <span>Início: {formatDate(contract.startDate)}</span>
              <span aria-hidden>•</span>
              <ContractStatusBadge status={contract.status} />
            </div>
          ) : null}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {contract ? (
            <>
              <Button
                variant="secondary"
                onClick={() =>
                  window.open(`/contracts/${contract.uuid}/pdf`, '_blank')
                }
              >
                <Printer className="h-4 w-4" />
                Gerar PDF
              </Button>
              <Button
                variant="secondary"
                onClick={() =>
                  navigate(`/contracts/${contract.uuid}/edit`)
                }
              >
                <Pencil className="h-4 w-4" />
                Editar
              </Button>
              {canStart ? (
                <Button
                  variant="secondary"
                  onClick={() => setConfirmStart(true)}
                >
                  <Play className="h-4 w-4" />
                  Iniciar
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
          {error}. <BackButton fallback="/contracts" />
        </Alert>
      ) : contract ? (
        <>
          {/* Resumo do cabeçalho */}
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Cliente
                </dt>
                <dd className="mt-1 font-medium text-slate-900 dark:text-slate-100">
                  {contract.clientName ?? '—'}
                </dd>
                <dd className="text-xs text-slate-500 dark:text-slate-400">
                  {contract.clientCode ?? ''}
                  {contract.clientType ? ` • ${contract.clientType === 'CUSTOMER' ? 'Cliente (PF)' : 'Empresa (PJ)'}` : ''}
                </dd>
              </div>
              <div className="sm:col-span-2 lg:col-span-2">
                <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  Início da vigência
                </dt>
                <dd className="mt-1 text-slate-900 dark:text-slate-100">
                  {formatDate(contract.startDate)}
                </dd>
              </div>
            </dl>

            {contract.address ? (
              <div className="mt-6 border-t border-slate-200 pt-4 dark:border-slate-800">
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                  Endereço
                </h3>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                  {[
                    contract.address.street,
                    contract.address.number,
                    contract.address.complement,
                    contract.address.neighborhood,
                    contract.address.city,
                    contract.address.state,
                    contract.address.zipCode,
                  ]
                    .filter(Boolean)
                    .join(', ') || '—'}
                </p>
              </div>
            ) : null}

            {contract.description ? (
              <div className="mt-6 border-t border-slate-200 pt-4 dark:border-slate-800">
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                  Descrição do contrato
                </h3>
                <div
                  className="prose prose-sm mt-1 max-w-none text-slate-700 dark:prose-invert dark:text-slate-300"
                  dangerouslySetInnerHTML={{ __html: contract.description }}
                />
              </div>
            ) : null}

            {contract.clause ? (
              <div className="mt-6 border-t border-slate-200 pt-4 dark:border-slate-800">
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
                  Cláusula
                </h3>
                <div className="prose prose-sm mt-1 max-w-none whitespace-pre-line text-slate-700 dark:prose-invert dark:text-slate-300">
                  {contract.clause}
                </div>
              </div>
            ) : null}
          </section>

          {/* Serviços */}
          {contract.servicesDescription ? (
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <h2 className="mb-3 text-base font-semibold">Serviços</h2>
              <div
                className="prose prose-sm max-w-none text-slate-700 dark:prose-invert dark:text-slate-300"
                dangerouslySetInnerHTML={{
                  __html: contract.servicesDescription,
                }}
              />
            </section>
          ) : null}

          {/* Produtos */}
          {contract.productsDescription ? (
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <h2 className="mb-3 text-base font-semibold">Produtos</h2>
              <div
                className="prose prose-sm max-w-none text-slate-700 dark:prose-invert dark:text-slate-300"
                dangerouslySetInnerHTML={{
                  __html: contract.productsDescription,
                }}
              />
            </section>
          ) : null}

          {isAdmin ? (
            <RegistrationAuditCard
              createdBy={contract.createdBy}
              createdAt={contract.createdAt}
              updatedBy={contract.updatedBy}
              updatedAt={contract.updatedAt}
            />
          ) : null}
        </>
      ) : null}

      <ConfirmDialog
        open={confirmStart}
        title="Iniciar contrato?"
        description="O contrato passará do status ABERTA para EM_ANDAMENTO."
        confirmText="Iniciar"
        confirmVariant="primary"
        isLoading={transitioning}
        onConfirm={() => runTransition(startContract)}
        onClose={() => {
          if (!transitioning) setConfirmStart(false)
        }}
      />
      <ConfirmDialog
        open={confirmComplete}
        title="Concluir contrato?"
        description="O contrato passará para CONCLUIDA."
        confirmText="Concluir"
        confirmVariant="primary"
        isLoading={transitioning}
        onConfirm={() => runTransition(completeContract)}
        onClose={() => {
          if (!transitioning) setConfirmComplete(false)
        }}
      />
      <ConfirmDialog
        open={confirmReopen}
        title="Reabrir contrato?"
        description="O contrato voltará para EM_ANDAMENTO."
        confirmText="Reabrir"
        confirmVariant="primary"
        isLoading={transitioning}
        onConfirm={() => runTransition(reopenContract)}
        onClose={() => {
          if (!transitioning) setConfirmReopen(false)
        }}
      />
    </div>
  )
}