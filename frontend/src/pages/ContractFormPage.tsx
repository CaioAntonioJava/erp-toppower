import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Power, Save, X } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { ContractForm } from '../components/contract/ContractForm'
import { StickyFormActions } from '../components/sales/StickyFormActions'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  activateContract,
  createContract,
  getContract,
  inactivateContract,
  updateContract,
} from '../api/contract.api'
import type {
  ContractCreateRequest,
  ContractResponse,
  ContractUpdateRequest,
} from '../types/contract'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar um contrato.
 * - /contracts/new        → modo create
 * - /contracts/:id        → modo view (carrega GET /contracts/{id})
 */
export function ContractFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [contract, setContract] = useState<ContractResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [confirmToggle, setConfirmToggle] = useState(false)
  const [toggling, setToggling] = useState(false)
  const [toggleError, setToggleError] = useState<string | null>(null)

  // Ref do contêiner de ações no cabeçalho. O `StickyFormActions`
  // observa esse elemento e revela o menu fixo quando ele sai do
  // viewport (página rolada), mantendo Salvar/Cancelar acessíveis.
  const actionsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!id) {
      setMode('create')
      return
    }
    let cancelled = false
    setMode('loading')
    setLoadError(null)
    getContract(Number(id!))
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
      navigate('/contracts', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: ContractUpdateRequest) {
    if (!contract) return
    setSaving(true)
    try {
      const updated = await updateContract(contract.id, payload)
      setContract(updated)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus() {
    if (!contract) return
    setToggling(true)
    setToggleError(null)
    try {
      if (contract.status === 'ATIVO') {
        await inactivateContract(contract.id)
        try {
          const fresh = await getContract(contract.id)
          setContract(fresh)
        } catch {
          setContract({ ...contract, status: 'INATIVO' })
        }
      } else {
        const updated = await activateContract(contract.id)
        setContract(updated)
      }
    } catch (err) {
      setToggleError(toApiError(err).message)
    } finally {
      setToggling(false)
      setConfirmToggle(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <BackButton />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            {mode === 'create' ? 'Novo contrato' : contract?.code ?? 'Contrato'}
          </h1>
          {mode === 'view' && contract ? (
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono text-xs">{contract.code}</span>
              <span aria-hidden>•</span>
              {contract.clientName ? (
                <>
                  <span>{contract.clientName}</span>
                  <span aria-hidden>•</span>
                </>
              ) : null}
              <RegistrationStatusBadge status={contract.status} />
            </div>
          ) : mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              O código comercial é gerado automaticamente a partir do prefixo
              da empresa ativa (CL/CT). O título é pré-preenchido pelo backend.
            </p>
          ) : null}
        </div>

        <div ref={actionsRef} className="flex flex-wrap items-center gap-2">
          {mode === 'view' && contract ? (
            <Button
              variant={contract.status === 'ATIVO' ? 'secondary' : 'primary'}
              onClick={() => {
                setToggleError(null)
                setConfirmToggle(true)
              }}
            >
              <Power className="h-4 w-4" />
              {contract.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
            </Button>
          ) : null}

          {mode !== 'loading' ? (
            <>
              <Button
                type="button"
                variant="secondary"
                onClick={() => navigate('/contracts')}
                size="md"
              >
                <X className="h-4 w-4" />
                Cancelar
              </Button>
              <Button
                type="submit"
                form="contract-form"
                isLoading={saving}
                size="md"
              >
                <Save className="h-4 w-4" />
                {mode === 'view' ? 'Salvar alterações' : 'Cadastrar contrato'}
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
          readOnly={false}
          canEdit={mode === 'view'}
          onCancel={() => navigate('/contracts')}
          title="CONTRATO"
        />
      ) : null}

      {loadError ? (
        <Alert variant="error">
          {loadError}.{' '}
          <BackButton />
        </Alert>
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
        <ContractForm
          contract={mode === 'view' ? contract ?? undefined : undefined}
          onSaveCreate={handleCreate}
          onSaveUpdate={handleUpdate}
        />
      )}

      <ConfirmDialog
        open={confirmToggle}
        title={
          contract?.status === 'ATIVO' ? 'Inativar contrato?' : 'Reativar contrato?'
        }
        description={
          contract?.status === 'ATIVO'
            ? `O contrato "${contract?.code}" será marcado como inativo. O registro não é apagado e pode ser reativado depois.`
            : `O contrato "${contract?.code}" voltará a ficar ativo.`
        }
        confirmText={contract?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={contract?.status === 'ATIVO' ? 'danger' : 'primary'}
        isLoading={toggling}
        onConfirm={handleToggleStatus}
        onClose={() => {
          if (!toggling) setConfirmToggle(false)
        }}
      />

      {toggleError ? <Alert variant="error">{toggleError}</Alert> : null}
    </div>
  )
}