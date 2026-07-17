import type { ReactNode } from 'react'

/**
 * Superfície de cartão reutilizável — bordas arredondadas, fundo claro/escuro
 * e padding opcional. Substitui as divs Tailwind duplicadas nos dashboards e
 * listagens (`rounded-2xl border border-slate-200 bg-white p-5 ...`).
 */
interface CardProps {
  children: ReactNode
  /** Remove o padding interno (útil quando o conteúdo controla seu próprio espaçamento). */
  padded?: boolean
  className?: string
}

export function Card({ children, padded = true, className = '' }: CardProps) {
  return (
    <div
      className={[
        'rounded-2xl border border-slate-200 bg-white shadow-sm',
        'dark:border-slate-800 dark:bg-slate-900',
        padded ? 'p-5' : '',
        className,
      ].join(' ')}
    >
      {children}
    </div>
  )
}