import { useEffect, useState } from 'react'
import { TrendingUp } from 'lucide-react'
import { Card } from '../ui/Card'
import { Spinner } from '../ui/Spinner'
import { formatCurrency } from '../../lib/format'
import { getFinanceSummary } from '../../api/finance.api'
import type { FinanceSummary } from '../../types/finance'

/**
 * Linha de indicadores financeiros no topo do dashboard.
 *
 * Mostra totais a pagar/receber (aberto e vencido) e contadores de boletos.
 * Enquanto o backend financeiro não existe, exibe zeros.
 */
interface Indicator {
  label: string
  value: number
  hint: string
  tone: 'default' | 'warning' | 'danger'
}

export function FinanceSummaryWidget() {
  const [summary, setSummary] = useState<FinanceSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    getFinanceSummary()
      .then((data) => {
        if (!cancelled) setSummary(data)
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
      value: summary?.totalReceberAberto ?? 0,
      hint: 'Total a receber em aberto',
      tone: 'default',
    },
    {
      label: 'A receber vencido',
      value: summary?.totalReceberVencido ?? 0,
      hint: 'Recebimentos em atraso',
      tone: summary?.totalReceberVencido ? 'warning' : 'default',
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