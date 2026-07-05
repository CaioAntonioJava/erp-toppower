import type { TechnicalProposalStatus } from '../../types/technicalProposal'
import { TECHNICAL_PROPOSAL_STATUS_LABELS } from '../../types/technicalProposal'
import { Badge } from '../ui/Badge'
import type { ComponentProps } from 'react'

type Tone = ComponentProps<typeof Badge>['tone']

interface TechnicalProposalStatusBadgeProps {
  status: TechnicalProposalStatus
  className?: string
}

const toneByStatus: Record<TechnicalProposalStatus, Tone> = {
  ABERTA: 'neutral',
  EM_ANDAMENTO: 'info',
  CONCLUIDA: 'success',
}

/**
 * Badge de status de proposta técnica.
 * ABERTA=cinza, EM_ANDAMENTO=azul, CONCLUIDA=verde.
 */
export function TechnicalProposalStatusBadge({
  status,
  className,
}: TechnicalProposalStatusBadgeProps) {
  return (
    <Badge tone={toneByStatus[status]} className={className}>
      {TECHNICAL_PROPOSAL_STATUS_LABELS[status]}
    </Badge>
  )
}