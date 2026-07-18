import type { ComponentProps } from 'react'
import type { ReceivableStatus } from '../../types/receivable'
import { Badge } from '../ui/Badge'

type Tone = ComponentProps<typeof Badge>['tone']

interface ReceivableStatusBadgeProps {
  status: ReceivableStatus
  className?: string
}

const toneByStatus: Record<ReceivableStatus, Tone> = {
  ABERTO: 'info',
  PAGO: 'success',
  CANCELADO: 'neutral',
}

const labelByStatus: Record<ReceivableStatus, string> = {
  ABERTO: 'Em aberto',
  PAGO: 'Pago',
  CANCELADO: 'Cancelado',
}

/**
 * Badge de status de conta a receber.
 * ABERTO=azul, PAGO=verde, CANCELADO=cinza.
 */
export function ReceivableStatusBadge({
  status,
  className,
}: ReceivableStatusBadgeProps) {
  return (
    <Badge tone={toneByStatus[status]} className={className}>
      {labelByStatus[status]}
    </Badge>
  )
}