import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Building2, Eye, ImageIcon, Plus, Power } from 'lucide-react'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { Alert } from '../components/ui/Alert'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { Badge } from '../components/ui/Badge'
import {
  activateOrganization,
  getOrganization,
  inactivateOrganization,
  listAllOrganizations,
} from '../api/organization.api'
import type { OrganizationResponse } from '../types/api'
import { toApiError } from '../lib/errors'

/**
 * Lista de Organizations (empresas emissoras) — visível apenas para ADMIN.
 *
 * <p>Como o sistema é multi-tenant e tem tipicamente poucas orgs
 * cadastradas (Top Power Engenharia, Top Power Materiais...), a lista
 * é simples: traz todas via {@code GET /organizations/all}, sem
 * paginação/busca. O foco da tela é dar acesso rápido à gestão do
 * logo e dos dados de cada empresa.</p>
 */
export function OrganizationsListPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<OrganizationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [toggling, setToggling] = useState<OrganizationResponse | null>(null)
  const [toggleRunning, setToggleRunning] = useState(false)
  const [toggleError, setToggleError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    listAllOrganizations()
      .then(async (summaries) => {
        if (cancelled) return
        // O endpoint /all retorna apenas o OrganizationSummary. Para a lista
        // administrativa queremos ver logo + endereço, então re-baixamos
        // o detalhe de cada uma. Em paralelo para não encadear.
        const full = await Promise.all(
          summaries.map(async (s) => {
            try {
              return await getOrganization(s.uuid)
            } catch {
              // fallback: monta um OrganizationResponse mínimo a partir do summary
              return {
                uuid: s.uuid,
                corporateName: s.corporateName,
                tradeName: s.tradeName,
                cnpj: s.cnpj,
                logoUrl: s.logoUrl ?? null,
                status: s.status,
                proposalPrefix: s.proposalPrefix,
                createdAt: '',
                updatedAt: '',
              } as OrganizationResponse
            }
          }),
        )
        if (!cancelled) setItems(full)
      })
      .catch((err) => {
        if (!cancelled) setError(toApiError(err).message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [])

  async function handleToggle() {
    if (!toggling) return
    setToggleRunning(true)
    setToggleError(null)
    try {
      if (toggling.status === 'ATIVO') {
        await inactivateOrganization(toggling.uuid)
      } else {
        await activateOrganization(toggling.uuid)
      }
      // Re-busca o estado atual da org para pegar o status atualizado
      const fresh = await getOrganization(toggling.uuid)
      // Atualiza a lista in-place
      setItems((prev: OrganizationResponse[]) =>
        prev.map((o) => (o.uuid === fresh.uuid ? fresh : o)),
      )
      setToggling(null)
    } catch (err) {
      setToggleError(toApiError(err).message)
    } finally {
      setToggleRunning(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Empresas (Organizations)</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Empresas emissoras do grupo (multi-tenant). Cada empresa tem dados, endereço e logo próprios.
            <span className="ml-2 inline-flex items-center rounded-full border border-primary/30 bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700 dark:border-primary-900 dark:bg-primary-900/30 dark:text-primary-200">
              Visão ADMIN
            </span>
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/organizations/new">
            <Button>
              <Plus className="h-4 w-4" />
              Nova empresa
            </Button>
          </Link>
        </div>
      </div>

      {error ? <Alert variant="error">{error}</Alert> : null}
      {toggleError ? <Alert variant="error">{toggleError}</Alert> : null}

      {/* Tabela */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
              <tr>
                <th scope="col" className="w-16 px-4 py-3">Logo</th>
                <th className="px-4 py-3 font-medium">Razão social</th>
                <th className="px-4 py-3 font-medium">Nome fantasia</th>
                <th className="px-4 py-3 font-medium">CNPJ</th>
                <th className="px-4 py-3 font-medium">Prefixo</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Spinner size="sm" /> Carregando…
                    </div>
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center">
                    <div className="flex flex-col items-center gap-2 text-slate-500 dark:text-slate-400">
                      <Building2 className="h-8 w-8 opacity-60" />
                      <p className="text-sm">Nenhuma empresa cadastrada.</p>
                      <Link to="/organizations/new">
                        <Button size="sm" variant="secondary">
                          <Plus className="h-4 w-4" />
                          Cadastrar a primeira
                        </Button>
                      </Link>
                    </div>
                  </td>
                </tr>
              ) : (
                items.map((org) => (
                  <tr
                    key={org.uuid}
                    className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/40"
                    onClick={() => navigate(`/organizations/${org.uuid}`)}
                  >
                    <td className="px-4 py-3">
                      <div className="flex h-10 w-10 items-center justify-center overflow-hidden rounded-md border border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800">
                        {org.logoUrl ? (
                          <img
                            src={org.logoUrl}
                            alt={`Logo ${org.tradeName}`}
                            className="h-full w-full object-contain"
                          />
                        ) : (
                          <ImageIcon className="h-4 w-4 text-slate-400" />
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900 dark:text-slate-100">
                        {org.corporateName}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-700 dark:text-slate-300">
                      {org.tradeName}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-300">
                      {org.cnpj}
                    </td>
                    <td className="px-4 py-3">
                      <Badge tone="info">{org.proposalPrefix}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <Badge tone={org.status === 'ATIVO' ? 'success' : 'neutral'}>
                        {org.status === 'ATIVO' ? 'Ativo' : 'Inativo'}
                      </Badge>
                    </td>
                    <td
                      className="px-4 py-3"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          size="sm" variant="ghost"
                          onClick={() => navigate(`/organizations/${org.uuid}`)}
                          title="Ver / editar" aria-label="Ver / editar"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant={org.status === 'ATIVO' ? 'ghost' : 'secondary'}
                          onClick={() => {
                            setToggleError(null)
                            setToggling(org)
                          }}
                          title={org.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
                          aria-label={org.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
                        >
                          <Power className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal: inativar/reativar */}
      <ConfirmDialog
        open={!!toggling}
        title={toggling?.status === 'ATIVO' ? 'Inativar empresa?' : 'Reativar empresa?'}
        description={
          toggling?.status === 'ATIVO'
            ? `A empresa "${toggling?.corporateName}" será marcada como inativa. O registro não é apagado e pode ser reativado depois.`
            : `A empresa "${toggling?.corporateName}" voltará a ficar ativa.`
        }
        confirmText={toggling?.status === 'ATIVO' ? 'Inativar' : 'Reativar'}
        confirmVariant={toggling?.status === 'ATIVO' ? 'danger' : 'primary'}
        isLoading={toggleRunning}
        onConfirm={handleToggle}
        onClose={() => {
          if (!toggleRunning) setToggling(null)
        }}
      />
    </div>
  )
}