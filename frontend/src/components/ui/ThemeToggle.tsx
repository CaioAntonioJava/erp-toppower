import { Moon, Sun } from 'lucide-react'
import { useTheme } from '../../context/ThemeContext'

interface ThemeToggleProps {
  className?: string
}

/** Botão sol/lua para alternar entre light e dark. */
export function ThemeToggle({ className = '' }: ThemeToggleProps) {
  const { theme, toggle } = useTheme()
  const isDark = theme === 'dark'
  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={isDark ? 'Trocar para tema claro' : 'Trocar para tema escuro'}
      title={isDark ? 'Tema claro' : 'Tema escuro'}
      className={[
        'inline-flex h-10 w-10 items-center justify-center rounded-lg',
        'border border-slate-200 bg-white text-slate-700 transition-colors',
        'hover:bg-slate-100 active:bg-slate-200',
        'dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200',
        'dark:hover:bg-slate-700',
        className,
      ].join(' ')}
    >
      {isDark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
    </button>
  )
}
