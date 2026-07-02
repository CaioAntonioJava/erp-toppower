import type { QuotationStatus } from '../../types/quotation'
import { QUOTATION_STATUS_LABELS } from '../../types/quotation'
import { Badge } from '../ui/Badge'
import type { ComponentProps } from 'react'

type Tone = ComponentProps<typeof Badge>['tone']

interface QuotationStatusBadgeProps {
  status: QuotationStatus
  className?: string
}

const toneByStatus: Record<QuotationStatus, Tone> = {
  ATIVA: 'success',
  CONVERTIDA: 'info',
  CANCELADA: 'danger',
  EXPIRADA: 'neutral',
}

/**
 * Badge de status de proposta comercial.
 * ATIVA=verde, CONVERTIDA=azul, CANCELADA=vermelho, EXPIRADA=cinza.
 */
export function QuotationStatusBadge({
  status,
  className,
}: QuotationStatusBadgeProps) {
  return (
    <Badge tone={toneByStatus[status]} className={className}>
      {QUOTATION_STATUS_LABELS[status]}
    </Badge>
  )
}