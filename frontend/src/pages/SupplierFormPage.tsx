import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Power } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { SupplierForm } from '../components/client/SupplierForm'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  activateSupplier,
  createSupplier,
  getSupplier,
  inactivateSupplier,
  updateSupplier,
} from '../api/supplier.api'
import type {
  SupplierCreateRequest,
  SupplierResponse,
  SupplierUpdateRequest,
} from '../types/supplier'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar um fornecedor (pessoa jurídica).
 * - /suppliers/new        → modo create
 * - /suppliers/:id        → modo view (carrega GET /suppliers/{id})
 */
export function SupplierFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [supplier, setSupplier] = useState<SupplierResponse | null>(null)
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
    getSupplier(id)
      .then((data) => {
        if (cancelled) return
        setSupplier(data)
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

  async function handleCreate(payload: SupplierCreateRequest) {
    setSaving(true)
    try {
      await createSupplier(payload)
      // Após salvar, redireciona para a lista (com `replace` para que o
      // botão Voltar do navegador não traga o usuário de volta para o
      // formulário já enviado). O item recém-criado aparecerá na lista
      // após a recarga automática.
      navigate('/suppliers', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: SupplierUpdateRequest) {
    if (!supplier) return
    setSaving(true)
    try {
      const updated = await updateSupplier(supplier.uuid, payload)
      setSupplier(updated)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus() {
    if (!supplier) return
    setToggling(true)
    setToggleError(null)
    try {
      if (supplier.status === 'ATIVO') {
        await inactivateSupplier(supplier.uuid)
        try {
          const fresh = await getSupplier(supplier.uuid)
          setSupplier(fresh)
        } catch {
          setSupplier({ ...supplier, status: 'INATIVO' })
        }
      } else {
        const updated = await activateSupplier(supplier.uuid)
        setSupplier(updated)
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
            {mode === 'create' ? 'Novo fornecedor' : supplier?.legalName ?? 'Fornecedor'}
          </h1>
          {mode === 'view' && supplier ? (
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono text-xs">{supplier.taxId}</span>
              <span aria-hidden>•</span>
              <RegistrationStatusBadge status={supplier.status} />
            </div>
          ) : mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para cadastrar um novo fornecedor (pessoa jurídica).
            </p>
          ) : null}
        </div>

        {mode === 'view' && supplier ? (
          <Button
            variant={supplier.status === 'ATIVO' ? 'secondary' : 'primary'}
            onClick={() => {
              setToggleError(null)
              setConfirmToggle(true)
            }}
          >
            <Power className="h-4 w-4" />
            {supplier.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
          </Button>
        ) : null}
      </div>

      {loadError ? (
        <Alert variant="error">
          {loadError}.{' '}
          <BackButton />
        </Alert>
      ) : null}

      {isAdmin && mode === 'view' && supplier ? (
        <RegistrationAuditCard
          createdBy={supplier.createdBy}
          createdAt={supplier.createdAt}
          updatedBy={supplier.updatedBy}
          updatedAt={supplier.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <SupplierForm
          supplier={mode === 'view' ? supplier ?? undefined : undefined}
          isLoading={saving}
          onSaveCreate={handleCreate}
          onSaveUpdate={handleUpdate}
        />
      )}

      <ConfirmDialog
        open={confirmToggle}
        title={
          supplier?.status === 'ATIVO' ? 'Inativar fornecedor?' : 'Reativar fornecedor?'
        }
        description={
          supplier?.status === 'ATIVO'
            ? `O fornecedor "${supplier?.legalName}" será marcado como inativo. O registro não é apagado e pode ser reativado depois.`
            : `O fornecedor "${supplier?.legalName}" voltará a ficar ativo.`
        }
        confirmText={supplier?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={supplier?.status === 'ATIVO' ? 'danger' : 'primary'}
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