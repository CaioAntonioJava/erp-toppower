import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Receipt } from 'lucide-react'
import { Card } from '../ui/Card'
import { Badge } from '../ui/Badge'
import { Spinner } from '../ui/Spinner'
import { formatCurrency, formatDate } from '../../lib/format'
import { listAccountsPayableOpen } from '../../api/finance.api'
import type { AccountPayable, AccountStatus } from '../../types/finance'

/**
 * Widget de Contas a Pagar — lista as próximas despesas em aberto.
 *
 * Preparado para ligar ao endpoint `/api/v1/accounts-payable` quando o
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

export function AccountsPayableWidget() {
  const [items, setItems] = useState<AccountPayable[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listAccountsPayableOpen()
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
          <Receipt className="h-4 w-4 text-primary" />
          <h2 className="text-sm font-semibold">Contas a Pagar</h2>
        </div>
        <Link
          to="/accounts-payable"
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
          <EmptyState text="Nenhuma conta a pagar em aberto." />
        ) : (
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {items.slice(0, 5).map((item) => (
              <li key={item.id} className="flex items-center justify-between py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{item.descricao}</p>
                  <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                    {item.fornecedor} · Venc. {formatDate(item.dataVencimento)}
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