import type { ComponentProps } from 'react'
import type { PayableStatus } from '../../types/payable'
import { Badge } from '../ui/Badge'

type Tone = ComponentProps<typeof Badge>['tone']

interface PayableStatusBadgeProps {
  status: PayableStatus
  className?: string
}

const toneByStatus: Record<PayableStatus, Tone> = {
  ABERTO: 'info',
  PAGO: 'success',
  CANCELADO: 'neutral',
}

const labelByStatus: Record<PayableStatus, string> = {
  ABERTO: 'Aberto',
  PAGO: 'Pago',
  CANCELADO: 'Cancelado',
}

/**
 * Badge de status de conta a pagar (ou parcela).
 * ABERTO=azul, PAGO=verde, CANCELADO=cinza.
 */
export function PayableStatusBadge({
  status,
  className,
}: PayableStatusBadgeProps) {
  return (
    <Badge tone={toneByStatus[status]} className={className}>
      {labelByStatus[status]}
    </Badge>
  )
}