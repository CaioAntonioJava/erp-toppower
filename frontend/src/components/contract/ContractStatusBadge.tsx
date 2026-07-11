import type { ContractStatus } from '../../types/contract'
import { CONTRACT_STATUS_LABELS } from '../../types/contract'
import { Badge } from '../ui/Badge'
import type { ComponentProps } from 'react'

type Tone = ComponentProps<typeof Badge>['tone']

interface ContractStatusBadgeProps {
  status: ContractStatus
  className?: string
}

const toneByStatus: Record<ContractStatus, Tone> = {
  ABERTA: 'neutral',
  EM_ANDAMENTO: 'info',
  CONCLUIDA: 'success',
}

/**
 * Badge de status de contrato.
 * ABERTA=cinza, EM_ANDAMENTO=azul, CONCLUIDA=verde.
 */
export function ContractStatusBadge({
  status,
  className,
}: ContractStatusBadgeProps) {
  return (
    <Badge tone={toneByStatus[status]} className={className}>
      {CONTRACT_STATUS_LABELS[status]}
    </Badge>
  )
}