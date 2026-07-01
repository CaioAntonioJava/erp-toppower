import type { RegistrationStatus } from '../../types/registration'
import { Badge } from '../ui/Badge'

interface RegistrationStatusBadgeProps {
  status: RegistrationStatus
  className?: string
}

/**
 * Badge genérico de status de registro (compartilhado por Company e Customer).
 * ATIVO = verde, INATIVO = cinza.
 */
export function RegistrationStatusBadge({
  status,
  className,
}: RegistrationStatusBadgeProps) {
  if (status === 'ATIVO') {
    return (
      <Badge tone="success" className={className}>
        Ativo
      </Badge>
    )
  }
  return (
    <Badge tone="neutral" className={className}>
      Inativo
    </Badge>
  )
}
