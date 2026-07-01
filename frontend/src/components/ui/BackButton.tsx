import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Button } from './Button'

interface BackButtonProps {
  /** Tamanho do botão. Default: "sm" (compacto, ideal para cabeçalhos e Alerts). */
  size?: 'sm' | 'md' | 'lg'
}

/**
 * Botão "Voltar" padronizado: ícone `ArrowLeft` + texto "Voltar",
 * com a cor primária do sistema (`--color-primary`).
 *
 * Sempre redireciona para o dashboard (`/`). Usado em:
 * - cabeçalho das páginas de formulário,
 * - dentro do `Alert` de erro de carregamento,
 * - página 404.
 */
export function BackButton({ size = 'sm' }: BackButtonProps) {
  return (
    <Link to="/">
      <Button variant="primary" size={size}>
        <ArrowLeft className="h-4 w-4" />
        Voltar
      </Button>
    </Link>
  )
}