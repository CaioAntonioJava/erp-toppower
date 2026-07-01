import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Power } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { BackButton } from '../components/ui/BackButton'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { CompanyForm } from '../components/client/CompanyForm'
import { RegistrationStatusBadge } from '../components/client/RegistrationStatusBadge'
import { RegistrationAuditCard } from '../components/client/RegistrationAuditCard'
import {
  activateCompany,
  createCompany,
  getCompany,
  inactivateCompany,
  updateCompany,
} from '../api/company.api'
import type {
  CompanyCreateRequest,
  CompanyResponse,
  CompanyUpdateRequest,
} from '../types/company'
import { toApiError } from '../lib/errors'
import { useAuth } from '../context/AuthContext'

type Mode = 'loading' | 'create' | 'view'

/**
 * Página unificada para criar/visualizar/editar uma empresa.
 * - /companies/new        → modo create
 * - /companies/:id        → modo view (carrega GET /companies/{id})
 */
export function CompanyFormPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'

  const [mode, setMode] = useState<Mode>('loading')
  const [company, setCompany] = useState<CompanyResponse | null>(null)
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
    getCompany(id)
      .then((data) => {
        if (cancelled) return
        setCompany(data)
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

  async function handleCreate(payload: CompanyCreateRequest) {
    setSaving(true)
    try {
      await createCompany(payload)
      // Após salvar, redireciona para a lista (com `replace` para que o
      // botão Voltar do navegador não traga o usuário de volta para o
      // formulário já enviado). O item recém-criado aparecerá na lista
      // após a recarga automática.
      navigate('/companies', { replace: true })
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(payload: CompanyUpdateRequest) {
    if (!company) return
    setSaving(true)
    try {
      const updated = await updateCompany(company.uuid, payload)
      setCompany(updated)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleStatus() {
    if (!company) return
    setToggling(true)
    setToggleError(null)
    try {
      if (company.status === 'ATIVO') {
        await inactivateCompany(company.uuid)
        try {
          const fresh = await getCompany(company.uuid)
          setCompany(fresh)
        } catch {
          setCompany({ ...company, status: 'INATIVO' })
        }
      } else {
        const updated = await activateCompany(company.uuid)
        setCompany(updated)
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
          <BackButton to="/companies" label="Voltar para a lista" />
          <h1 className="mt-1 text-2xl font-semibold tracking-tight">
            {mode === 'create' ? 'Nova empresa' : company?.legalName ?? 'Empresa'}
          </h1>
          {mode === 'view' && company ? (
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
              <span className="font-mono text-xs">{company.code}</span>
              <span aria-hidden>•</span>
              <RegistrationStatusBadge status={company.status} />
            </div>
          ) : mode === 'create' ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Preencha os dados para cadastrar uma nova empresa.
            </p>
          ) : null}
        </div>

        {mode === 'view' && company ? (
          <Button
            variant={company.status === 'ATIVO' ? 'secondary' : 'primary'}
            onClick={() => {
              setToggleError(null)
              setConfirmToggle(true)
            }}
          >
            <Power className="h-4 w-4" />
            {company.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
          </Button>
        ) : null}
      </div>

      {loadError ? (
        <Alert variant="error">
          {loadError}.{' '}
          <BackButton to="/companies" label="Voltar para a lista" />
        </Alert>
      ) : null}

      {isAdmin && mode === 'view' && company ? (
        <RegistrationAuditCard
          createdBy={company.createdBy}
          createdAt={company.createdAt}
          updatedBy={company.updatedBy}
          updatedAt={company.updatedAt}
        />
      ) : null}

      {mode === 'loading' ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <CompanyForm
          company={mode === 'view' ? company ?? undefined : undefined}
          isLoading={saving}
          onSaveCreate={handleCreate}
          onSaveUpdate={handleUpdate}
        />
      )}

      <ConfirmDialog
        open={confirmToggle}
        title={
          company?.status === 'ATIVO' ? 'Inativar empresa?' : 'Reativar empresa?'
        }
        description={
          company?.status === 'ATIVO'
            ? `A empresa "${company?.legalName}" será marcada como inativa. O registro não é apagado e pode ser reativado depois.`
            : `A empresa "${company?.legalName}" voltará a ficar ativa.`
        }
        confirmText={company?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={company?.status === 'ATIVO' ? 'danger' : 'primary'}
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
