import { forwardRef, useId, useState } from 'react'
import type { InputHTMLAttributes, ReactNode } from 'react'
import { Eye, EyeOff } from 'lucide-react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string | null
  hint?: string
  leftAdornment?: ReactNode
}

/**
 * Input com label, erro, hint e botão de mostrar/ocultar senha
 * (quando type="password"). Encapsula o estado do toggle internamente.
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, hint, leftAdornment, required, type = 'text', className = '', id, ...rest },
  ref,
) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const [showPwd, setShowPwd] = useState(false)
  const isPassword = type === 'password'
  const resolvedType = isPassword && showPwd ? 'text' : type

  return (
    <div className={className ? `w-full ${className}` : 'w-full'}>
      {label ? (
        <label
          htmlFor={inputId}
          className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
        >
          {label}
          {required && <span className="ml-0.5 text-red-500">*</span>}
        </label>
      ) : null}

      <div
        className={[
          'flex items-stretch overflow-hidden rounded-lg border bg-white dark:bg-slate-900',
          error
            ? 'border-red-500 focus-within:ring-2 focus-within:ring-red-500/30'
            : 'border-slate-300 focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20 dark:border-slate-700',
          'transition-colors',
        ].join(' ')}
      >
        {leftAdornment ? (
          <span className="flex items-center px-3 text-slate-500 dark:text-slate-400">
            {leftAdornment}
          </span>
        ) : null}

        <input
          ref={ref}
          id={inputId}
          type={resolvedType}
          required={required}
          className={[
            'h-11 w-full bg-transparent px-3 text-sm text-slate-900 outline-none',
            'placeholder:text-slate-400 dark:text-slate-100 dark:placeholder:text-slate-500',
            className,
          ].join(' ')}
          aria-invalid={!!error}
          aria-describedby={error ? `${inputId}-error` : undefined}
          {...rest}
        />

        {isPassword ? (
          <button
            type="button"
            onClick={() => setShowPwd((v) => !v)}
            className="flex items-center px-3 text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
            aria-label={showPwd ? 'Ocultar senha' : 'Mostrar senha'}
            tabIndex={-1}
          >
            {showPwd ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        ) : null}
      </div>

      {error ? (
        <p
          id={`${inputId}-error`}
          className="mt-1.5 text-sm text-red-600 dark:text-red-400"
        >
          {error}
        </p>
      ) : hint ? (
        <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">
          {hint}
        </p>
      ) : null}
    </div>
  )
})
