import { useEffect, useState } from 'react'
import { TrendingUp } from 'lucide-react'
import { Card } from '../ui/Card'
import { Spinner } from '../ui/Spinner'
import { formatCurrency } from '../../lib/format'
import { listPayables } from '../../api/payable.api'
import { listReceivables } from '../../api/receivable.api'
import { listBoletos } from '../../api/boleto.api'
import type { BoletoResponse } from '../../types/boleto'

/**
 * Linha de indicadores financeiros no dashboard.
 *
 * Todos os totais são calculados no frontend a partir dos endpoints reais
 * de contas a pagar, contas a receber e boletos — não há endpoint de
 * resumo agregado no backend. Percorre as contas/boletos em aberto
 * paginando até esgotar (size 100 por página).
 */
interface Indicator {
  label: string
  value: number
  hint: string
  tone: 'default' | 'warning' | 'danger'
}

/** Calcula dias até o vencimento a partir de uma data ISO (negativo = vencido). */
function diasAteVencimento(dataVencimento: string): number {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const venc = new Date(`${dataVencimento}T00:00:00`)
  return Math.round((venc.getTime() - hoje.getTime()) / (24 * 60 * 60 * 1000))
}

/** Calcula os totais a pagar (aberto e vencido) a partir das contas ABERTO. */
async function fetchPayableTotals(): Promise<{ aberto: number; vencido: number }> {
  let aberto = 0
  let vencido = 0
  let page = 0
  for (;;) {
    const result = await listPayables({ status: 'ABERTO', size: 100, page })
    for (const item of result.content) {
      aberto += item.balance
      if (diasAteVencimento(item.dueDate) < 0) vencido += item.balance
    }
    if (result.last || result.content.length === 0) break
    page += 1
  }
  return { aberto, vencido }
}

/** Calcula os totais a receber (aberto e vencido) a partir das contas ABERTO. */
async function fetchReceivableTotals(): Promise<{ aberto: number; vencido: number }> {
  let aberto = 0
  let vencido = 0
  let page = 0
  for (;;) {
    const result = await listReceivables({ status: 'ABERTO', size: 100, page })
    for (const item of result.content) {
      aberto += item.balance
      if (diasAteVencimento(item.dueDate) < 0) vencido += item.balance
    }
    if (result.last || result.content.length === 0) break
    page += 1
  }
  return { aberto, vencido }
}

/** Conta boletos não pagos vencendo (7 dias) e vencidos. */
async function fetchBoletoCounts(): Promise<{ proximos: number; vencidos: number }> {
  let proximos = 0
  let vencidos = 0
  let page = 0
  for (;;) {
    const result = await listBoletos({ status: 'ATIVO', size: 100, page })
    for (const boleto of result.content as BoletoResponse[]) {
      if (boleto.paid) continue
      const dias = diasAteVencimento(boleto.dueDate)
      if (dias < 0) {
        vencidos += 1
      } else if (dias <= 7) {
        proximos += 1
      }
    }
    if (result.last || result.content.length === 0) break
    page += 1
  }
  return { proximos, vencidos }
}

export function FinanceSummaryWidget() {
  const [pagarAberto, setPagarAberto] = useState(0)
  const [pagarVencido, setPagarVencido] = useState(0)
  const [receberAberto, setReceberAberto] = useState(0)
  const [receberVencido, setReceberVencido] = useState(0)
  const [boletosProximos, setBoletosProximos] = useState(0)
  const [boletosVencidos, setBoletosVencidos] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    Promise.all([
      fetchPayableTotals(),
      fetchReceivableTotals(),
      fetchBoletoCounts(),
    ])
      .then(([p, r, b]) => {
        if (cancelled) return
        setPagarAberto(p.aberto)
        setPagarVencido(p.vencido)
        setReceberAberto(r.aberto)
        setReceberVencido(r.vencido)
        setBoletosProximos(b.proximos)
        setBoletosVencidos(b.vencidos)
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
      value: pagarAberto,
      hint: 'Total de despesas em aberto',
      tone: 'default',
    },
    {
      label: 'A pagar vencido',
      value: pagarVencido,
      hint: 'Despesas em atraso',
      tone: pagarVencido ? 'danger' : 'default',
    },
    {
      label: 'A receber (aberto)',
      value: receberAberto,
      hint: 'Total a receber em aberto',
      tone: 'default',
    },
    {
      label: 'A receber vencido',
      value: receberVencido,
      hint: 'Recebimentos em atraso',
      tone: receberVencido ? 'warning' : 'default',
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
            {boletosProximos}
          </strong>
        </span>
        <span className="text-slate-500 dark:text-slate-400">
          Boletos vencidos:{' '}
          <strong className="text-red-600 dark:text-red-400">
            {boletosVencidos}
          </strong>
        </span>
      </div>
    </Card>
  )
}