import type { ComponentProps } from 'react'
import type { ContractStatus } from '../../types/contract'
import { Badge } from '../ui/Badge'

type Tone = ComponentProps<typeof Badge>['tone']

interface ContractStatusBadgeProps {
  status: ContractStatus
  className?: string
}

const toneByStatus: Record<ContractStatus, Tone> = {
  ATIVO: 'info',
  CONCLUIDO: 'success',
  INATIVO: 'neutral',
}

const labelByStatus: Record<ContractStatus, string> = {
  ATIVO: 'Ativo',
  CONCLUIDO: 'Concluído',
  INATIVO: 'Inativo',
}

/**
 * Badge de status de contrato.
 * ATIVO=azul, CONCLUIDO=verde, INATIVO=cinza.
 */
export function ContractStatusBadge({
  status,
  className,
}: ContractStatusBadgeProps) {
  return (
    <Badge tone={toneByStatus[status]} className={className}>
      {labelByStatus[status]}
    </Badge>
  )
}