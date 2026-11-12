import { forwardRef, useId } from 'react'
import type { ReactNode, TextareaHTMLAttributes } from 'react'

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string | null
  hint?: string
  rightAdornment?: ReactNode
}

/**
 * Textarea com label, erro e hint — primitivo espelhando o Input
 * (borda padrão, ring no focus com --color-focus, erro só pela mensagem
 * abaixo do campo).
 */
export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { label, error, hint, rightAdornment, required, className = '', id, ...rest },
  ref,
) {
  const generatedId = useId()
  const textareaId = id ?? generatedId

  return (
    <div className={className ? `w-full ${className}` : 'w-full'}>
      {label ? (
        <label
          htmlFor={textareaId}
          className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200"
        >
          {label}
          {required && <span className="ml-0.5 text-red-500">*</span>}
        </label>
      ) : null}

      <div className="relative w-full">
        <textarea
          ref={ref}
          id={textareaId}
          aria-required={required || undefined}
          className={[
            'min-h-[96px] w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm',
            'text-slate-900 outline-none placeholder:text-slate-400',
            'dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500',
            'transition-[border-color,box-shadow] duration-500 ease-in-out focus:border-focus focus:ring-2 focus:ring-focus/30',
            rightAdornment ? 'pr-9' : '',
          ].join(' ')}
          aria-invalid={!!error}
          aria-describedby={error ? `${textareaId}-error` : undefined}
          {...rest}
        />
        {rightAdornment ? (
          <span className="absolute right-3 top-2.5 text-slate-400 dark:text-slate-500">
            {rightAdornment}
          </span>
        ) : null}
      </div>

      {error ? (
        <p id={`${textareaId}-error`} className="mt-1.5 text-sm text-red-600 dark:text-red-400">
          {error}
        </p>
      ) : hint ? (
        <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">{hint}</p>
      ) : null}
    </div>
  )
})
