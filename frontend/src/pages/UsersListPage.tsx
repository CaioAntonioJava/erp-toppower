import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Building2,
  KeyRound,
  Plus,
  Search,
  Trash2,
  UserPlus,
  Users as UsersIcon,
  X,
} from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { Badge } from '../components/ui/Badge'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { listUsers, resetUserPassword, deleteUser } from '../api/user.api'
import { useAuth } from '../context/AuthContext'
import type { UserResponse } from '../types/api'
import { toApiError } from '../lib/errors'

/** Modal simples para o admin digitar a nova senha do usuário.
 *  Separado do ConfirmDialog genérico (que não tem campos de input). */
function ResetPasswordDialog({
  user,
  onClose,
  onConfirm,
  isLoading,
  error,
}: {
  user: UserResponse
  onClose: () => void
  onConfirm: (newPassword: string) => void
  isLoading: boolean
  error: string | null
}) {
  const [newPassword, setNewPassword] = useState('')

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape' && !isLoading) onClose()
    }
    window.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [isLoading, onClose])

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onConfirm(newPassword)
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 backdrop-blur-sm"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !isLoading) onClose()
      }}
    >
      <form
        onSubmit={submit}
        className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900"
      >
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-4 dark:border-slate-800">
          <div className="flex items-start gap-3">
            <div className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-300">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-semibold">Redefinir senha</h2>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                Defina uma nova senha para <strong>{user.email}</strong>.
                Mínimo de 8 caracteres.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="rounded p-1 text-slate-500 hover:bg-slate-100 disabled:opacity-50 dark:text-slate-400 dark:hover:bg-slate-800"
            aria-label="Fechar"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="px-5 py-4">
          <Input
            label="Nova senha"
            type="password"
            required
            autoComplete="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={8}
            error={error}
          />
        </div>

        <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4 dark:border-slate-800">
          <Button type="button" variant="secondary" onClick={onClose} disabled={isLoading}>
            Cancelar
          </Button>
          <Button type="submit" isLoading={isLoading} disabled={newPassword.length < 8}>
            Redefinir senha
          </Button>
        </div>
      </form>
    </div>
  )
}

export function UsersListPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState<UserResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')

  const [resetTarget, setResetTarget] = useState<UserResponse | null>(null)
  const [resetting, setResetting] = useState(false)
  const [resetError, setResetError] = useState<string | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<UserResponse | null>(null)
  const [deleting, setDeleting] = useState(false)

  const [feedback, setFeedback] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await listUsers()
      setUsers(data)
    } catch (err) {
      setError(toApiError(err).message)
      setUsers([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return users
    return users.filter((u) => u.email.toLowerCase().includes(q))
  }, [users, query])

  async function handleResetPassword(newPassword: string) {
    if (!resetTarget) return
    setResetting(true)
    setResetError(null)
    try {
      await resetUserPassword(resetTarget.uuid, { newPassword })
      setFeedback(`Senha de "${resetTarget.email}" redefinida com sucesso.`)
      setResetTarget(null)
    } catch (err) {
      setResetError(toApiError(err).message)
    } finally {
      setResetting(false)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteUser(deleteTarget.uuid)
      setFeedback(`Usuário "${deleteTarget.email}" excluído com sucesso.`)
      setDeleteTarget(null)
      await load()
    } catch (err) {
      setError(toApiError(err).message)
      setDeleteTarget(null)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Usuários</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Gestão de usuários com acesso ao sistema.
            <span className="ml-2 inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
              Visão ADMIN
            </span>
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/users/new">
            <Button>
              <Plus className="h-4 w-4" />
              Novo usuário
            </Button>
          </Link>
        </div>
      </div>

      {feedback ? (
        <Alert variant="success">
          {feedback}
        </Alert>
      ) : null}
      {error ? <Alert variant="error">{error}</Alert> : null}

      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <Input
          placeholder="Buscar por e-mail…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          leftAdornment={<Search className="h-4 w-4" />}
        />
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th className="px-4 py-3 font-medium">E-mail</th>
                <th className="px-4 py-3 font-medium">Empresas</th>
                <th className="px-4 py-3 font-medium">Papel</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={4} className="px-4 py-12 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <UserPlus className="h-8 w-8 opacity-60" />
                      <p className="text-sm">
                        {query ? 'Nenhum usuário encontrado.' : 'Nenhum usuário cadastrado.'}
                      </p>
                      <Link to="/users/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Cadastrar o primeiro
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                filtered.map((u) => (
                  <tr
                    key={u.uuid}
                    className="hover:bg-slate-50 dark:hover:bg-slate-800/40"
                  >
                    <td className="px-4 py-3">
                      <div className="inline-flex items-center gap-2 font-medium text-slate-900 dark:text-slate-100">
                        <UsersIcon className="h-4 w-4 text-slate-400" />
                        {u.email}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {u.tenants && u.tenants.length > 0 ? (
                        <div className="flex flex-wrap gap-1">
                          {u.tenants.map((t) => (
                            <span
                              key={t.uuid}
                              className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs font-medium text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
                            >
                              <Building2 className="h-3 w-3 text-slate-400" />
                              {t.displayName}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span className="text-xs text-slate-400">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {u.role === 'ROLE_ADMIN' ? (
                        <Badge tone="info">Administrador</Badge>
                      ) : (
                        <Badge tone="neutral">Gestor</Badge>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => {
                            setResetError(null)
                            setResetTarget(u)
                          }}
                          title="Redefinir senha"
                          aria-label="Redefinir senha"
                        >
                          <KeyRound className="h-4 w-4" />
                          Redefinir senha
                        </Button>
                        <Button
                          size="sm"
                          variant="danger"
                          onClick={() => setDeleteTarget(u)}
                          disabled={currentUser?.uuid === u.uuid}
                          title={
                            currentUser?.uuid === u.uuid
                              ? 'Você não pode excluir sua própria conta'
                              : 'Excluir usuário'
                          }
                          aria-label="Excluir usuário"
                        >
                          <Trash2 className="h-4 w-4" />
                          Excluir
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="border-t border-slate-200 px-4 py-3 text-sm dark:border-slate-800">
          <span className="text-slate-500 dark:text-slate-400">
            {loading
              ? 'Carregando…'
              : `${filtered.length} usuário(s)`}
          </span>
        </div>
      </div>

      {resetTarget ? (
        <ResetPasswordDialog
          user={resetTarget}
          isLoading={resetting}
          error={resetError}
          onConfirm={handleResetPassword}
          onClose={() => {
            if (!resetting) {
              setResetTarget(null)
              setResetError(null)
            }
          }}
        />
      ) : null}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir usuário"
        description={
          deleteTarget
            ? `Tem certeza que deseja excluir "${deleteTarget.email}"? Esta ação remove o usuário e todos os seus vínculos com empresas, e não pode ser desfeita.`
            : ''
        }
        confirmText="Excluir"
        confirmVariant="danger"
        isLoading={deleting}
        onConfirm={handleDelete}
        onClose={() => {
          if (!deleting) setDeleteTarget(null)
        }}
      />
    </div>
  )
}