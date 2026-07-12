import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CheckCircle2, Play, RotateCcw, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { ContractForm } from '../components/contract/ContractForm'
import { ContractStatusBadge } from '../components/contract/ContractStatusBadge'
import { StickyFormActions } from '../components/sales/StickyFormActions'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  completeContract,
  createContract,
  getContract,
  getNextContractCode,
  reopenContract,
  startContract,
  updateContract,
} from '../api/contract.api'
import type {
  ContractCreateRequest,
  ContractResponse,
  ContractUpdateRequest,
} from '../types/contract'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'
import { useOrganization } from '../context/OrganizationContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar um contrato.
 * - /contracts/new    → modo create
 * - /contracts/:id/edit → modo view/edit (carrega GET /contracts/{id})
 */
export function ContractFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  // Importante: no modo create, o `next-code` precisa ser re-buscado
  // sempre que a Organization ativa muda (cada empresa tem seu próprio
  // prefixo de contrato, ex.: CT-001-2026 vs CL-001-2026).
  const { activeOrganization, revision } = useOrganization()
  const activeOrganizationUuid = activeOrganization?.uuid ?? null
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [contract, setContract] = useState<ContractResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [nextCode, setNextCode] = useState<string | null>(null)
  const [nextCodeLoading, setNextCodeLoading] = useState(false)
  const [nextCodeError, setNextCodeError] = useState<string | null>(null)

  // Transições de status
  const [confirmStart, setConfirmStart] = useState(false)
  const [confirmComplete, setConfirmComplete] = useState(false)
  const [confirmReopen, setConfirmReopen] = useState(false)
  const [transitioning, setTransitioning] = useState(false)
  const [transitionError, setTransitionError] = useState<string | null>(null)

  const actionsRef = useRef<HTMLDivElement>(null)

  // Modo CREATE: re-busca o próximo código sempre que a Organization
  // ativa muda (cada empresa tem seu próprio prefixo e sua própria
  // sequência por ano). A dependência `revision` (incrementada a cada
  // troca de Org pelo OrganizationContext) é uma salvaguarda extra
  // caso o UUID permaneça igual entre seleções sucessivas.
  useEffect(() => {
    if (id) return
    if (!activeOrganizationUuid) {
      setMode('create')
      setNextCode(null)
      setNextCodeError(null)
      return
    }
    let cancelled = false
    setMode('create')
    setNextCode(null)
    setNextCodeError(null)
    setNextCodeLoading(true)
    getNextContractCode()
      .then((r) => {
        if (cancelled) return
        setNextCode(r.code)
      })
      .catch((err) => {
        if (cancelled) return
        setNextCode(null)
        setNextCodeError(toApiError(err).message)
      })
      .finally(() => {
        if (!cancelled) setNextCodeLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id, activeOrganizationUuid, revision])

  // Modo VIEW/EDIT: carrega o contrato pelo ID.
  useEffect(() => {
    if (!id) return
    let cancelled = false
    setMode('loading')
    setLoadError(null)
    getContract(id)
      .then((data) => {
        if (cancelled) return
        setContract(data)
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

  async function handleCreate(payload: ContractCreateRequest) {
    setSaving(true)
    try {
      await createContract(payload)
      // Após criar um novo contrato, volta direto para a lista
      // (não abre a tela de detalhe do contrato recém-criado).
      navigate('/contracts', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: ContractUpdateRequest) {
    if (!contract) return
    setSaving(true)
    try {
      const updated = await updateContract(contract.uuid, payload)
      setContract(updated)
    } finally {
      setSaving(false)
    }
  }

  function handleCancel() {
    navigate('/contracts')
  }

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

  // Contratos CONCLUIDOS não podem ser editados via PATCH.
  const readOnly = mode === 'view' && contract?.status === 'CONCLUIDA'
  const canEdit = mode === 'view' && !readOnly

  const canStart = mode === 'view' && contract?.status === 'ABERTA'
  const canComplete = mode === 'view' && contract?.status === 'EM_ANDAMENTO'
  const canReopen = mode === 'view' && contract?.status === 'CONCLUIDA'

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton label="Voltar para a lista" fallback="/contracts" />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            Contrato
          </h1>
          {mode === 'create' ? (
            <div className="mt-1 space-y-2">
              <div className="flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
                {activeOrganization?.contractPrefix ? (
                  <span
                    className="inline-flex h-6 items-center rounded-md border border-primary/30 bg-primary/10 px-2 font-mono text-[11px] font-semibold tracking-wide text-primary"
                    title="Prefixo da empresa ativa (define o início do código dos contratos)"
                  >
                    {activeOrganization.contractPrefix}
                  </span>
                ) : null}
                {nextCodeLoading ? (
                  <>
                    <span>Próximo código:</span>
                    <span className="inline-flex items-center gap-1 text-slate-400">
                      <Spinner size="sm" /> carregando…
                    </span>
                  </>
                ) : nextCode ? (
                  <>
                    <span>Próximo código:</span>
                    <span className="font-mono font-medium text-slate-700 dark:text-slate-200">
                      {nextCode}
                    </span>
                  </>
                ) : (
                  <span>Preencha os dados para criar o contrato.</span>
                )}
              </div>
              {!activeOrganization?.contractPrefix ? (
                <Alert variant="error">
                  A empresa ativa não possui <strong>contractPrefix</strong>
                  {' '}configurado. Atualize o cadastro da empresa antes de
                  emitir contratos.
                </Alert>
              ) : null}
              {nextCodeError ? (
                <Alert variant="error">
                  Falha ao buscar próximo código: {nextCodeError}
                </Alert>
              ) : null}
            </div>
          ) : mode === 'view' && contract ? (
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono">{contract.code}</span>
              <span aria-hidden>•</span>
              <span>Início: {contract.startDate}</span>
              <span aria-hidden>•</span>
              <ContractStatusBadge status={contract.status} />
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
              {canStart ? (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setConfirmStart(true)}
                  size="md"
                >
                  <Play className="h-4 w-4" />
                  Iniciar
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
                form="contract-form"
                isLoading={saving}
                size="md"
                disabled={readOnly}
              >
                <Save className="h-4 w-4" />
                {canEdit ? 'Salvar alterações' : 'Cadastrar contrato'}
              </Button>
            </>
          ) : null}
        </div>
      </div>

      {mode !== 'loading' ? (
        <StickyFormActions
          triggerRef={actionsRef}
          formId="contract-form"
          saving={saving}
          readOnly={readOnly}
          canEdit={canEdit}
          onCancel={handleCancel}
          title="CONTRATO"
        />
      ) : null}

      {loadError ? (
        <Alert variant="error">
          {loadError}. <BackButton fallback="/contracts" />
        </Alert>
      ) : null}

      {transitionError ? (
        <Alert variant="error">{transitionError}</Alert>
      ) : null}

      {isAdmin && mode === 'view' && contract ? (
        <RegistrationAuditCard
          createdBy={contract.createdBy}
          createdAt={contract.createdAt}
          updatedBy={contract.updatedBy}
          updatedAt={contract.updatedAt}
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
                Contratos CONCLUIDOS não podem ser editados. Use
                <strong> Reabrir</strong> para voltar para EM_ANDAMENTO
                antes de editar.
              </Alert>
            </div>
          ) : null}
          <fieldset disabled={readOnly} className={readOnly ? 'opacity-70' : ''}>
            {/*
              key = `${activeOrganizationUuid}-${mode}-${id ?? 'new'}` força
              o remount do form sempre que a Organization ativa muda
              (cada empresa tem seu próprio prefixo de contrato e sua
              própria sequência) ou quando o modo/produto carregado muda.
            */}
            <ContractForm
              key={`${activeOrganizationUuid ?? 'no-org'}-${mode}-${id ?? 'new'}`}
              contract={canEdit ? contract ?? undefined : undefined}
              initialCode={mode === 'create' ? nextCode : null}
              onSaveCreate={handleCreate}
              onSaveUpdate={handleUpdate}
            />
          </fieldset>
        </div>
      )}

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