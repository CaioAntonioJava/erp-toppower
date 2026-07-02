import { useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Button } from './Button'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

interface BackButtonProps {
  /** Tamanho do botão. Default: "sm" (compacto, ideal para cabeçalhos e Alerts). */
  size?: Size
  /** Variante visual do botão. Default: "primary". */
  variant?: Variant
  /**
   * Texto exibido ao lado do ícone. Default: "Voltar".
   * Use algo mais específico quando o destino implícito não for óbvio
   * (ex.: "Voltar para a lista").
   */
  label?: string
  /**
   * Destino usado como fallback quando não há histórico de navegação
   * dentro do SPA (ex.: acesso por deep link). Default: "/" (dashboard).
   */
  fallback?: string
}

/**
 * Botão "Voltar" padronizado: ícone `ArrowLeft` + texto configurável,
 * com a cor primária do sistema (`--color-primary`) por padrão.
 *
 * Comportamento: navega para a **página anterior** via `navigate(-1)`.
 * Se não houver histórico (caso o usuário tenha aberto a página por
 * deep link), cai no `fallback` informado — por padrão, o dashboard.
 *
 * Usado em:
 * - cabeçalho das páginas de formulário,
 * - dentro do `Alert` de erro de carregamento,
 * - página 404.
 */
export function BackButton({
  size = 'sm',
  variant = 'primary',
  label = 'Voltar',
  fallback = '/',
}: BackButtonProps) {
  const navigate = useNavigate()

  function handleClick() {
    // `window.history.length` inclui a entrada atual; > 1 significa
    // que existe ao menos uma página anterior para a qual voltar.
    if (window.history.length > 1) {
      navigate(-1)
    } else {
      navigate(fallback)
    }
  }

  return (
    <Button variant={variant} size={size} onClick={handleClick}>
      <ArrowLeft className="h-4 w-4" />
      {label}
    </Button>
  )
}
