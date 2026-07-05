import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CheckCircle2, Play, RotateCcw, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { TechnicalProposalForm } from '../components/sales/TechnicalProposalForm'
import { TechnicalProposalStatusBadge } from '../components/sales/TechnicalProposalStatusBadge'
import { StickyFormActions } from '../components/sales/StickyFormActions'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  completeTechnicalProposal,
  createTechnicalProposal,
  getNextTechnicalProposalCode,
  getTechnicalProposal,
  reopenTechnicalProposal,
  startTechnicalProposal,
  updateTechnicalProposal,
} from '../api/technicalProposal.api'
import type {
  TechnicalProposalCreateRequest,
  TechnicalProposalResponse,
  TechnicalProposalUpdateRequest,
} from '../types/technicalProposal'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar uma proposta técnica.
 * - /technical-proposals/new    → modo create
 * - /technical-proposals/:id    → modo view/edit (carrega GET /technical-proposals/{id})
 */
export function TechnicalProposalFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [proposal, setProposal] = useState<TechnicalProposalResponse | null>(
    null,
  )
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [nextCode, setNextCode] = useState<string | null>(null)

  // Transições de status
  const [confirmStart, setConfirmStart] = useState(false)
  const [confirmComplete, setConfirmComplete] = useState(false)
  const [confirmReopen, setConfirmReopen] = useState(false)
  const [transitioning, setTransitioning] = useState(false)
  const [transitionError, setTransitionError] = useState<string | null>(null)

  const actionsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!id) {
      setMode('create')
      getNextTechnicalProposalCode()
        .then((r) => setNextCode(r.code))
        .catch(() => setNextCode(null))
      return
    }
    let cancelled = false
    setMode('loading')
    setLoadError(null)
    getTechnicalProposal(id)
      .then((data) => {
        if (cancelled) return
        setProposal(data)
        setMode('view')
      })
      .catch((err) => {
        if (cancelled) return
        setLoadError(toApiError(err).message)
        setMode('create')
      })
    return () => {
      cancelled = true
    }
  }, [id])

  async function handleCreate(payload: TechnicalProposalCreateRequest) {
    setSaving(true)
    try {
      const created = await createTechnicalProposal(payload)
      navigate(`/technical-proposals/${created.uuid}/edit`, { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: TechnicalProposalUpdateRequest) {
    if (!proposal) return
    setSaving(true)
    try {
      const updated = await updateTechnicalProposal(proposal.uuid, payload)
      setProposal(updated)
    } finally {
      setSaving(false)
    }
  }

  function handleCancel() {
    navigate('/technical-proposals')
  }

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

  // Propostas CONCLUIDAS não podem ser editadas via PATCH.
  const readOnly = mode === 'view' && proposal?.status === 'CONCLUIDA'
  const canEdit = mode === 'view' && !readOnly

  // Transições permitidas.
  const canStart = mode === 'view' && proposal?.status === 'ABERTA'
  const canComplete = mode === 'view' && proposal?.status === 'EM_ANDAMENTO'
  const canReopen = mode === 'view' && proposal?.status === 'CONCLUIDA'

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
          {mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para criar a proposta técnica.
            </p>
          ) : mode === 'view' && proposal ? (
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono">{proposal.code}</span>
              <span aria-hidden>•</span>
              <span>Início: {proposal.startDate}</span>
              {proposal.endDate ? (
                <>
                  <span aria-hidden>•</span>
                  <span>Término: {proposal.endDate}</span>
                </>
              ) : null}
              {proposal.deliveryDate ? (
                <>
                  <span aria-hidden>•</span>
                  <span>Entrega: {proposal.deliveryDate}</span>
                </>
              ) : null}
              <span aria-hidden>•</span>
              <TechnicalProposalStatusBadge status={proposal.status} />
            </div>
          ) : null}
        </div>

        <div ref={actionsRef} className="flex flex-wrap items-center gap-2">
          {mode !== 'loading' ? (
            <>
              <Button
                type="button"
                variant="secondary"
                onClick={handleCancel}
                size="md"
              >
                <X className="h-4 w-4" />
                Cancelar
              </Button>
              {canStart ? (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setConfirmStart(true)}
                  size="md"
                >
                  <Play className="h-4 w-4" />
                  Iniciar execução
                </Button>
              ) : null}
              {canComplete ? (
                <Button
                  type="button"
                  variant="primary"
                  onClick={() => setConfirmComplete(true)}
                  size="md"
                >
                  <CheckCircle2 className="h-4 w-4" />
                  Concluir
                </Button>
              ) : null}
              {canReopen ? (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setConfirmReopen(true)}
                  size="md"
                >
                  <RotateCcw className="h-4 w-4" />
                  Reabrir
                </Button>
              ) : null}
              <Button
                type="submit"
                form="technical-proposal-form"
                isLoading={saving}
                size="md"
                disabled={readOnly}
              >
                <Save className="h-4 w-4" />
                {canEdit ? 'Salvar alterações' : 'Cadastrar proposta'}
              </Button>
            </>
          ) : null}
        </div>
      </div>

      {mode !== 'loading' ? (
        <StickyFormActions
          triggerRef={actionsRef}
          formId="technical-proposal-form"
          saving={saving}
          readOnly={readOnly}
          canEdit={canEdit}
          onCancel={handleCancel}
          title="PROPOSTA TÉCNICA"
        />
      ) : null}

      {loadError ? (
        <Alert variant="error">
          {loadError}. <BackButton />
        </Alert>
      ) : null}

      {transitionError ? (
        <Alert variant="error">{transitionError}</Alert>
      ) : null}

      {isAdmin && mode === 'view' && proposal ? (
        <RegistrationAuditCard
          createdBy={proposal.createdBy}
          createdAt={proposal.createdAt}
          updatedBy={proposal.updatedBy}
          updatedAt={proposal.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="relative">
          {readOnly ? (
            <div className="mb-4">
              <Alert variant="info">
                Propostas técnicas CONCLUIDAS não podem ser editadas. Use
                <strong> Reabrir</strong> para voltar para EM_ANDAMENTO antes
                de editar.
              </Alert>
            </div>
          ) : null}
          <fieldset disabled={readOnly} className={readOnly ? 'opacity-70' : ''}>
            <TechnicalProposalForm
              proposal={canEdit ? proposal ?? undefined : undefined}
              initialCode={mode === 'create' ? nextCode : null}
              onSaveCreate={handleCreate}
              onSaveUpdate={handleUpdate}
            />
          </fieldset>
        </div>
      )}

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