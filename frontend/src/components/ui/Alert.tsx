import type { ReactNode } from 'react'
import { AlertCircle, CheckCircle2, Info } from 'lucide-react'

type Variant = 'error' | 'success' | 'info'

interface AlertProps {
  variant?: Variant
  children: ReactNode
  className?: string
}

const styles: Record<Variant, { box: string; icon: string; Icon: typeof Info }> = {
  error: {
    box: 'bg-red-50 text-red-800 border-red-200 dark:bg-red-950/40 dark:text-red-200 dark:border-red-900',
    icon: 'text-red-600 dark:text-red-400',
    Icon: AlertCircle,
  },
  success: {
    box: 'bg-emerald-50 text-emerald-800 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-200 dark:border-emerald-900',
    icon: 'text-emerald-600 dark:text-emerald-400',
    Icon: CheckCircle2,
  },
  info: {
    box: 'bg-sky-50 text-sky-800 border-sky-200 dark:bg-sky-950/40 dark:text-sky-200 dark:border-sky-900',
    icon: 'text-sky-600 dark:text-sky-400',
    Icon: Info,
  },
}

export function Alert({ variant = 'info', children, className = '' }: AlertProps) {
  const { box, icon, Icon } = styles[variant]
  return (
    <div
      role={variant === 'error' ? 'alert' : 'status'}
      className={[
        'flex items-start gap-2 rounded-lg border px-3 py-2 text-sm',
        box,
        className,
      ].join(' ')}
    >
      <Icon className={['mt-0.5 h-4 w-4 shrink-0', icon].join(' ')} />
      <div className="flex-1">{children}</div>
    </div>
  )
}
