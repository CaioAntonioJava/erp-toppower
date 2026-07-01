import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Button } from './Button'

interface BackButtonProps {
  /** Rota de destino do botão "voltar". */
  to: string
  /** Texto exibido ao lado do ícone. Default: "Voltar". */
  label?: string
  /** Tamanho do botão. Default: "sm" (compacto, ideal para cabeçalhos e Alerts). */
  size?: 'sm' | 'md' | 'lg'
}

/**
 * Botão "voltar" padronizado: ícone `ArrowLeft` + texto, com a cor
 * primária do sistema (`--color-primary`, variante `primary` do `Button`).
 *
 * Usado em:
 * - cabeçalho das páginas de formulário (voltar para a lista),
 * - dentro do `Alert` de erro de carregamento,
 * - página 404.
 */
export function BackButton({
  to,
  label = 'Voltar',
  size = 'sm',
}: BackButtonProps) {
  return (
    <Link to={to}>
      <Button variant="primary" size={size}>
        <ArrowLeft className="h-4 w-4" />
        {label}
      </Button>
    </Link>
  )
}