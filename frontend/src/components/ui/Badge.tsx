import type { ReactNode } from 'react'

type Tone = 'neutral' | 'success' | 'warning' | 'danger' | 'info'

interface BadgeProps {
  tone?: Tone
  children: ReactNode
  className?: string
}

const tones: Record<Tone, string> = {
  neutral:
    'bg-slate-100 text-slate-700 border-slate-200 ' +
    'dark:bg-slate-800 dark:text-slate-200 dark:border-slate-700',
  success:
    'bg-emerald-50 text-emerald-700 border-emerald-200 ' +
    'dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-900',
  warning:
    'bg-amber-50 text-amber-700 border-amber-200 ' +
    'dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-900',
  danger:
    'bg-red-50 text-red-700 border-red-200 ' +
    'dark:bg-red-950/40 dark:text-red-300 dark:border-red-900',
  info:
    'bg-sky-50 text-sky-700 border-sky-200 ' +
    'dark:bg-sky-950/40 dark:text-sky-300 dark:border-sky-900',
}

export function Badge({ tone = 'neutral', children, className = '' }: BadgeProps) {
  return (
    <span
      className={[
        'inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium',
        tones[tone],
        className,
      ].join(' ')}
    >
      {children}
    </span>
  )
}
