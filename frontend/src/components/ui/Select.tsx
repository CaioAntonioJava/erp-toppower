import { forwardRef, useId } from 'react'
import type { SelectHTMLAttributes, ReactNode } from 'react'

export interface SelectOption {
  value: string
  label: string
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string | null
  hint?: string
  options: ReadonlyArray<SelectOption>
  placeholder?: string
  leftAdornment?: ReactNode
}

/** Select estilizado com label, erro, hint e adornment à esquerda. */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  {
    label,
    error,
    hint,
    options,
    placeholder,
    leftAdornment,
    required,
    className = '',
    id,
    ...rest
  },
  ref,
) {
  const generatedId = useId()
  const selectId = id ?? generatedId
  return (
    <div className={className ? `w-full ${className}` : 'w-full'}>
      {label ? (
        <label
          htmlFor={selectId}
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

        <select
          ref={ref}
          id={selectId}
          required={required}
          className={[
            'h-11 w-full appearance-none bg-transparent px-3 text-sm text-slate-900 outline-none',
            'dark:text-slate-100',
            // Ícone de chevron desenhado como background SVG.
            'bg-[length:1rem_1rem] bg-[right_0.65rem_center] bg-no-repeat',
            'bg-[url("data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2216%22 height=%2216%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%2364748b%22 stroke-width=%222%22 stroke-linecap=%22round%22 stroke-linejoin=%22round%22><polyline points=%226 9 12 15 18 9%22></polyline></svg>")]',
            'pr-9',
            className,
          ].join(' ')}
          aria-invalid={!!error}
          {...rest}
        >
          {placeholder ? (
            <option value="" disabled>
              {placeholder}
            </option>
          ) : null}
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      {error ? (
        <p className="mt-1.5 text-sm text-red-600 dark:text-red-400">{error}</p>
      ) : hint ? (
        <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">{hint}</p>
      ) : null}
    </div>
  )
})
