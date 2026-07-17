import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Wallet } from 'lucide-react'
import { Card } from '../ui/Card'
import { Badge } from '../ui/Badge'
import { Spinner } from '../ui/Spinner'
import { formatCurrency, formatDate } from '../../lib/format'
import { listAccountsReceivableOpen } from '../../api/finance.api'
import type { AccountReceivable, AccountStatus } from '../../types/finance'

/**
 * Widget de Contas a Receber — lista os próximos recebimentos em aberto.
 *
 * Preparado para ligar ao endpoint `/api/v1/accounts-receivable` quando o
 * backend financeiro existir. Hoje exibe estado vazio.
 */
const STATUS_LABEL: Record<AccountStatus, string> = {
  ABERTO: 'Aberto',
  PAGO: 'Pago',
  ATRASADO: 'Atrasado',
  CANCELADO: 'Cancelado',
}

const STATUS_TONE: Record<AccountStatus, 'neutral' | 'success' | 'warning' | 'danger'> = {
  ABERTO: 'neutral',
  PAGO: 'success',
  ATRASADO: 'danger',
  CANCELADO: 'neutral',
}

export function AccountsReceivableWidget() {
  const [items, setItems] = useState<AccountReceivable[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listAccountsReceivableOpen()
      .then((data) => {
        if (!cancelled) setItems(data)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <Card padded={false} className="flex flex-col">
      <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3 dark:border-slate-800">
        <div className="flex items-center gap-2">
          <Wallet className="h-4 w-4 text-primary" />
          <h2 className="text-sm font-semibold">Contas a Receber</h2>
        </div>
        <Link
          to="/accounts-receivable"
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
            {items.slice(0, 5).map((item) => (
              <li key={item.id} className="flex items-center justify-between py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{item.descricao}</p>
                  <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                    {item.cliente} · Venc. {formatDate(item.dataVencimento)}
                  </p>
                </div>
                <div className="ml-3 flex shrink-0 items-center gap-3">
                  <span className="text-sm font-semibold">
                    {formatCurrency(item.valor)}
                  </span>
                  <Badge tone={STATUS_TONE[item.status]}>
                    {STATUS_LABEL[item.status]}
                  </Badge>
                </div>
              </li>
            ))}
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