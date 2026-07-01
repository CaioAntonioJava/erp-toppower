import { History } from 'lucide-react'

interface RegistrationAuditCardProps {
  /** E-mail do usuário que criou o registro (ou null). */
  createdBy: string | null
  /** ISO 8601 — data de criação. */
  createdAt: string
  /** E-mail do usuário que fez a última atualização (ou null). */
  updatedBy: string | null
  /** ISO 8601 — data da última atualização. */
  updatedAt: string
}

/** Formata uma data ISO em pt-BR (ex: "01/07/2026 às 14:35"). */
function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * Card de auditoria genérico (exclusivo do ROLE_ADMIN).
 * Mostra quem criou e quem atualizou o registro, com as datas.
 * Tanto Company quanto Customer têm esses 4 campos na resposta.
 * Visível apenas para ADMIN porque o GET /{id} do backend é restrito a ele.
 */
export function RegistrationAuditCard({
  createdBy,
  createdAt,
  updatedBy,
  updatedAt,
}: RegistrationAuditCardProps) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="mb-4 flex items-center gap-2">
        <div className="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300">
          <History className="h-5 w-5" />
        </div>
        <div>
          <h3 className="text-base font-semibold">Auditoria</h3>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Informações de criação e última atualização do registro.
          </p>
        </div>
      </div>

      <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Criado por
          </dt>
          <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
            {createdBy ?? <span className="text-slate-400">—</span>}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Criado em
          </dt>
          <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
            {formatDateTime(createdAt)}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Atualizado por
          </dt>
          <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
            {updatedBy ?? <span className="text-slate-400">—</span>}
          </dd>
        </div>
        <div>
          <dt className="text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">
            Atualizado em
          </dt>
          <dd className="mt-1 text-sm text-slate-800 dark:text-slate-200">
            {formatDateTime(updatedAt)}
          </dd>
        </div>
      </dl>
    </section>
  )
}
