import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle, ArrowRight, Clock, FileClock } from 'lucide-react'
import { Card } from '../ui/Card'
import { Badge } from '../ui/Badge'
import { Spinner } from '../ui/Spinner'
import { formatCurrency, formatDate } from '../../lib/format'
import { listBoletos } from '../../api/boleto.api'
import type { BoletoResponse } from '../../types/boleto'
import type { BoletoDue } from '../../types/finance'

/**
 * Widget de Boletos próximos do vencimento.
 *
 * Destaca boletos vencendo nos próximos 7 dias e boletos já vencidos.
 * Busca os boletos ativos não pagos do endpoint `/api/v1/boletos` e
 * deriva os campos de apresentação (diasAteVencimento, status) no frontend.
 */

/** Calcula dias até o vencimento a partir de uma data ISO (negativo = vencido). */
function diasAteVencimento(dataVencimento: string): number {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const venc = new Date(`${dataVencimento}T00:00:00`)
  return Math.round((venc.getTime() - hoje.getTime()) / (24 * 60 * 60 * 1000))
}

/** Converte um BoletoResponse em BoletoDue (com campos derivados). */
function toDue(boleto: BoletoResponse): BoletoDue {
  const dias = diasAteVencimento(boleto.dueDate)
  return {
    id: boleto.id,
    descricao: boleto.description,
    pagador: boleto.payee,
    valor: boleto.value,
    dataVencimento: boleto.dueDate,
    diasAteVencimento: dias,
    status: dias < 0 ? 'ATRASADO' : 'ABERTO',
    paid: boleto.paid,
    paymentDate: boleto.paymentDate,
  }
}

export function BoletosDueWidget() {
  const [items, setItems] = useState<BoletoDue[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    // Busca boletos ativos e filtra os não pagos que vencem em até 7 dias
    // ou já estão vencidos.
    listBoletos({ status: 'ATIVO', size: 100 })
      .then((page) => {
        if (cancelled) return
        const due = page.content
          .map(toDue)
          .filter((b) => !b.paid && b.diasAteVencimento <= 7)
          .sort((a, b) => a.diasAteVencimento - b.diasAteVencimento)
        setItems(due)
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