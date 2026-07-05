import type { SalesOrderStatus } from '../../types/salesOrder'
import { SALES_ORDER_STATUS_LABELS } from '../../types/salesOrder'
import { Badge } from '../ui/Badge'
import type { ComponentProps } from 'react'

type Tone = ComponentProps<typeof Badge>['tone']

interface SalesOrderStatusBadgeProps {
  status: SalesOrderStatus
  className?: string
}

const toneByStatus: Record<SalesOrderStatus, Tone> = {
  ABERTO: 'info',
  FINALIZADO: 'success',
  CANCELADO: 'danger',
}

/**
 * Badge de status do pedido de venda.
 * ABERTO=azul, FINALIZADO=verde, CANCELADO=vermelho.
 */
export function SalesOrderStatusBadge({
  status,
  className,
}: SalesOrderStatusBadgeProps) {
  return (
    <Badge tone={toneByStatus[status]} className={className}>
      {SALES_ORDER_STATUS_LABELS[status]}
    </Badge>
  )
}