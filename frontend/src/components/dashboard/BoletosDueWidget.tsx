import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle, ArrowRight, Clock, FileClock } from 'lucide-react'
import { Card } from '../ui/Card'
import { Badge } from '../ui/Badge'
import { Spinner } from '../ui/Spinner'
import { formatCurrency, formatDate } from '../../lib/format'
import { listBoletosDue } from '../../api/finance.api'
import type { BoletoDue } from '../../types/finance'

/**
 * Widget de Boletos próximos do vencimento.
 *
 * Destaca boletos vencendo nos próximos 7 dias e boletos já vencidos.
 * Preparado para ligar ao endpoint `/api/v1/boletos/due` quando o backend
 * financeiro existir. Hoje exibe estado vazio.
 */
export function BoletosDueWidget() {
  const [items, setItems] = useState<BoletoDue[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    listBoletosDue()
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
          <FileClock className="h-4 w-4 text-primary" />
          <h2 className="text-sm font-semibold">Boletos próximos do vencimento</h2>
        </div>
        <Link
          to="/boletos"
          className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
        >
          Ver todos
          <ArrowRight className="h-3 w-3" />
        </Link>
      </div>

      <div className="flex-1 px-5 py-4">
        {loading ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : items.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-400 dark:text-slate-500">
            Nenhum boleto próximo do vencimento.
          </p>
        ) : (
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {items.slice(0, 6).map((boleto) => {
              const vencido = boleto.diasAteVencimento < 0
              return (
                <li key={boleto.id} className="flex items-center justify-between py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">
                      {boleto.descricao}
                    </p>
                    <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                      {boleto.pagador} · Venc. {formatDate(boleto.dataVencimento)}
                    </p>
                  </div>
                  <div className="ml-3 flex shrink-0 items-center gap-3">
                    <span className="text-sm font-semibold">
                      {formatCurrency(boleto.valor)}
                    </span>
                    {vencido ? (
                      <Badge tone="danger">
                        <AlertTriangle className="mr-1 h-3 w-3" />
                        Vencido
                      </Badge>
                    ) : (
                      <Badge tone="warning">
                        <Clock className="mr-1 h-3 w-3" />
                        {boleto.diasAteVencimento}d
                      </Badge>
                    )}
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