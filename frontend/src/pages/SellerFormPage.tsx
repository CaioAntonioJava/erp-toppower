import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Power } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { SellerForm } from '../components/client/SellerForm'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  activateSeller,
  createSeller,
  getSeller,
  inactivateSeller,
  updateSeller,
} from '../api/seller.api'
import type {
  SellerCreateRequest,
  SellerResponse,
  SellerUpdateRequest,
} from '../types/seller'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar um vendedor.
 * - /sellers/new        → modo create
 * - /sellers/:id        → modo view (carrega GET /sellers/{id})
 */
export function SellerFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [seller, setSeller] = useState<SellerResponse | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [confirmToggle, setConfirmToggle] = useState(false)
  const [toggling, setToggling] = useState(false)
  const [toggleError, setToggleError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) {
      setMode('create')
      return
    }
    let cancelled = false
    setMode('loading')
    setLoadError(null)
    getSeller(id)
      .then((data) => {
        if (cancelled) return
        setSeller(data)
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

  async function handleCreate(payload: SellerCreateRequest) {
    setSaving(true)
    try {
      await createSeller(payload)
      // Após salvar, redireciona para a lista (com `replace` para que o
      // botão Voltar do navegador não traga o usuário de volta para o
      // formulário já enviado). O item recém-criado aparecerá na lista
      // após a recarga automática.
      navigate('/sellers', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: SellerUpdateRequest) {
    if (!seller) return
    setSaving(true)
    try {
      const updated = await updateSeller(seller.uuid, payload)
      setSeller(updated)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus() {
    if (!seller) return
    setToggling(true)
    setToggleError(null)
    try {
      if (seller.status === 'ATIVO') {
        await inactivateSeller(seller.uuid)
        try {
          const fresh = await getSeller(seller.uuid)
          setSeller(fresh)
        } catch {
          setSeller({ ...seller, status: 'INATIVO' })
        }
      } else {
        const updated = await activateSeller(seller.uuid)
        setSeller(updated)
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
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">
            {mode === 'create' ? 'Novo vendedor' : seller?.name ?? 'Vendedor'}
          </h1>
          {mode === 'view' && seller ? (
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono text-xs">{seller.cpf}</span>
              <span aria-hidden>•</span>
              <RegistrationStatusBadge status={seller.status} />
            </div>
          ) : mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para cadastrar um novo vendedor.
            </p>
          ) : null}
        </div>

        {mode === 'view' && seller ? (
          <Button
            variant={seller.status === 'ATIVO' ? 'secondary' : 'primary'}
            onClick={() => {
              setToggleError(null)
              setConfirmToggle(true)
            }}
          >
            <Power className="h-4 w-4" />
            {seller.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
          </Button>
        ) : null}
      </div>

      {loadError ? (
        <Alert variant="error">
          {loadError}.{' '}
          <BackButton />
        </Alert>
      ) : null}

      {isAdmin && mode === 'view' && seller ? (
        <RegistrationAuditCard
          createdBy={seller.createdBy}
          createdAt={seller.createdAt}
          updatedBy={seller.updatedBy}
          updatedAt={seller.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <SellerForm
          seller={mode === 'view' ? seller ?? undefined : undefined}
          isLoading={saving}
          onSaveCreate={handleCreate}
          onSaveUpdate={handleUpdate}
        />
      )}

      <ConfirmDialog
        open={confirmToggle}
        title={
          seller?.status === 'ATIVO' ? 'Inativar vendedor?' : 'Reativar vendedor?'
        }
        description={
          seller?.status === 'ATIVO'
            ? `O vendedor "${seller?.name}" será marcado como inativo. O registro não é apagado e pode ser reativado depois.`
            : `O vendedor "${seller?.name}" voltará a ficar ativo.`
        }
        confirmText={seller?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={seller?.status === 'ATIVO' ? 'danger' : 'primary'}
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