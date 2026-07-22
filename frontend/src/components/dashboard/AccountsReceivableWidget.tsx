import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Wallet } from 'lucide-react'
import { Card } from '../ui/Card'
import { Badge } from '../ui/Badge'
import { Spinner } from '../ui/Spinner'
import { formatCurrency, formatDate } from '../../lib/format'
import { listReceivables } from '../../api/receivable.api'
import { useDashboardRefresh } from '../../hooks/useDashboardRefresh'
import type { ReceivableSummaryResponse } from '../../types/receivable'

/**
 * Widget de Contas a Receber — lista os próximos recebimentos em aberto
 * (status ABERTO), ordenados por vencimento ascendente.
 */
function isOverdue(iso: string): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return new Date(`${iso}T00:00:00`) < today
}

/** Status de apresentação derivado do vencimento: ATRASADO quando vencido. */
function displayStatus(item: ReceivableSummaryResponse): 'ABERTO' | 'ATRASADO' | 'PAGO' | 'CANCELADO' {
  if (item.status === 'ABERTO' && isOverdue(item.dueDate)) return 'ATRASADO'
  return item.status
}

const STATUS_LABEL: Record<'ABERTO' | 'ATRASADO' | 'PAGO' | 'CANCELADO', string> = {
  ABERTO: 'Aberto',
  PAGO: 'Pago',
  ATRASADO: 'Atrasado',
  CANCELADO: 'Cancelado',
}

const STATUS_TONE: Record<'ABERTO' | 'ATRASADO' | 'PAGO' | 'CANCELADO', 'neutral' | 'success' | 'warning' | 'danger'> = {
  ABERTO: 'neutral',
  PAGO: 'success',
  ATRASADO: 'danger',
  CANCELADO: 'neutral',
}

export function AccountsReceivableWidget() {
  const [items, setItems] = useState<ReceivableSummaryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const refreshKey = useDashboardRefresh()

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listReceivables({ status: 'ABERTO', size: 5 })
      .then((page) => {
        if (!cancelled) setItems(page.content)
      })
      .catch(() => {
        if (!cancelled) setItems([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [refreshKey])

  return (
    <Card padded={false} className="flex flex-col">
      <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3 dark:border-slate-800">
        <div className="flex items-center gap-2">
          <Wallet className="h-4 w-4 text-primary" />
          <h2 className="text-sm font-semibold">Contas a Receber</h2>
        </div>
        <Link
          to="/receivables"
          className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
        >
          Ver todas
          <ArrowRight className="h-3 w-3" />
        </Link>
      </div>

      <div className="flex-1 px-5 py-4">
        {loading ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : items.length === 0 ? (
          <EmptyState text="Nenhuma conta a receber em aberto." />
        ) : (
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {items.map((item) => {
              const status = displayStatus(item)
              return (
                <li key={item.id} className="flex items-center justify-between py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{item.description}</p>
                    <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                      {item.clientName ?? '—'} · Venc. {formatDate(item.dueDate)}
                    </p>
                  </div>
                  <div className="ml-3 flex shrink-0 items-center gap-3">
                    <span className="text-sm font-semibold">
                      {formatCurrency(item.balance)}
                    </span>
                    <Badge tone={STATUS_TONE[status]}>
                      {STATUS_LABEL[status]}
                    </Badge>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </Card>
  )
}

function EmptyState({ text }: { text: string }) {
  return (
    <p className="py-8 text-center text-sm text-slate-400 dark:text-slate-500">
      {text}
    </p>
  )
}