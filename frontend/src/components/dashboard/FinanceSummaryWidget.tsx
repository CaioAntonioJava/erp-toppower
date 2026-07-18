import { useEffect, useState } from 'react'
import { TrendingUp } from 'lucide-react'
import { Card } from '../ui/Card'
import { Spinner } from '../ui/Spinner'
import { formatCurrency } from '../../lib/format'
import { getFinanceSummary } from '../../api/finance.api'
import { listReceivables } from '../../api/receivable.api'
import type { FinanceSummary } from '../../types/finance'

/**
 * Linha de indicadores financeiros no topo do dashboard.
 *
 * Os totais "a pagar" e os contadores de boletos ainda dependem de
 * `getFinanceSummary` (stub enquanto o backend de contas a pagar não
 * existe). Os totais "a receber" (aberto e vencido) são calculados no
 * frontend a partir do endpoint `/api/v1/accounts-receivable?status=ABERTO`.
 */
interface Indicator {
  label: string
  value: number
  hint: string
  tone: 'default' | 'warning' | 'danger'
}

/** Calcula os totais a receber (aberto e vencido) a partir das contas ABERTO. */
async function fetchReceivableTotals(): Promise<{ aberto: number; vencido: number }> {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  let aberto = 0
  let vencido = 0
  let page = 0
  // Percorre até esgotar as contas em aberto (tamanho 100 por página).
  for (;;) {
    const result = await listReceivables({ status: 'ABERTO', size: 100, page })
    for (const item of result.content) {
      aberto += item.balance
      const due = new Date(`${item.dueDate}T00:00:00`)
      if (due < today) vencido += item.balance
    }
    if (result.last || result.content.length === 0) break
    page += 1
  }
  return { aberto, vencido }
}

export function FinanceSummaryWidget() {
  const [summary, setSummary] = useState<FinanceSummary | null>(null)
  const [receivableAberto, setReceivableAberto] = useState(0)
  const [receivableVencido, setReceivableVencido] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    Promise.all([getFinanceSummary(), fetchReceivableTotals()])
      .then(([s, r]) => {
        if (cancelled) return
        setSummary(s)
        setReceivableAberto(r.aberto)
        setReceivableVencido(r.vencido)
      })
      .catch(() => {
        // Mantém zeros em caso de erro — o dashboard segue utilizável.
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return (
      <Card className="flex items-center justify-center py-10">
        <Spinner />
      </Card>
    )
  }

  const indicators: Indicator[] = [
    {
      label: 'A pagar (aberto)',
      value: summary?.totalPagarAberto ?? 0,
      hint: 'Total de despesas em aberto',
      tone: 'default',
    },
    {
      label: 'A pagar vencido',
      value: summary?.totalPagarVencido ?? 0,
      hint: 'Despesas em atraso',
      tone: summary?.totalPagarVencido ? 'danger' : 'default',
    },
    {
      label: 'A receber (aberto)',
      value: receivableAberto,
      hint: 'Total a receber em aberto',
      tone: 'default',
    },
    {
      label: 'A receber vencido',
      value: receivableVencido,
      hint: 'Recebimentos em atraso',
      tone: receivableVencido ? 'warning' : 'default',
    },
  ]

  const toneClasses: Record<Indicator['tone'], string> = {
    default: 'text-slate-900 dark:text-slate-100',
    warning: 'text-amber-600 dark:text-amber-400',
    danger: 'text-red-600 dark:text-red-400',
  }

  return (
    <Card padded={false}>
      <div className="flex items-center gap-2 border-b border-slate-200 px-5 py-3 dark:border-slate-800">
        <TrendingUp className="h-4 w-4 text-primary" />
        <h2 className="text-sm font-semibold">Indicadores Financeiros</h2>
      </div>
      <div className="grid grid-cols-1 gap-4 p-5 sm:grid-cols-2 lg:grid-cols-4">
        {indicators.map((ind) => (
          <div
            key={ind.label}
            className="rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-800/40"
          >
            <p className="text-xs font-medium text-slate-500 dark:text-slate-400">
              {ind.label}
            </p>
            <p className={['mt-1 text-xl font-semibold', toneClasses[ind.tone]].join(' ')}>
              {formatCurrency(ind.value)}
            </p>
            <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">
              {ind.hint}
            </p>
          </div>
        ))}
      </div>
      <div className="flex gap-6 border-t border-slate-200 px-5 py-3 text-xs dark:border-slate-800">
        <span className="text-slate-500 dark:text-slate-400">
          Boletos vencendo (7 dias):{' '}
          <strong className="text-slate-900 dark:text-slate-100">
            {summary?.boletosProximosVencimento ?? 0}
          </strong>
        </span>
        <span className="text-slate-500 dark:text-slate-400">
          Boletos vencidos:{' '}
          <strong className="text-red-600 dark:text-red-400">
            {summary?.boletosVencidos ?? 0}
          </strong>
        </span>
      </div>
    </Card>
  )
}