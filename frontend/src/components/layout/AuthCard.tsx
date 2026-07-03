import type { ReactNode } from 'react'
import { ThemeToggle } from '../ui/ThemeToggle'
import { LogoTopPower } from '../ui/LogoTopPower'

interface AuthCardProps {
  title: string
  subtitle?: string
  children: ReactNode
  footer?: ReactNode
}

/** Layout centralizado para as telas de Login e Cadastro. */
export function AuthCard({ title, subtitle, children, footer }: AuthCardProps) {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <div className="absolute right-4 top-4">
        <ThemeToggle />
      </div>

      <div className="flex min-h-screen items-center justify-center px-4">
        <div className="w-full max-w-md">
          <div className="mb-8 flex flex-col items-center">
            {/* Logo TopPower como SVG inline — texto adapta a cor
               automaticamente (escuro no light, claro no dark). */}
            <LogoTopPower className="h-20 w-auto" />
            {/* Tagline institucional exibida abaixo da logo — comunica
               o propósito do ERP logo na primeira tela. */}
            <p className="mt-2 text-center text-sm text-slate-500 dark:text-slate-400">
              Uma plataforma completa para administrar sua empresa
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            {/* Título centralizado dentro do box para criar uma
               hierarquia visual mais forte: o cabeçalho do formulário
               (ex.: "Acessar sua conta") ganha destaque logo no topo
               do card, acima dos campos. */}
            <h4 className="text-center text-lg font-bold text-slate-900 dark:text-slate-100">
              {title}
            </h4>
            {subtitle ? (
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                {subtitle}
              </p>
            ) : null}
            <div className="mt-4">{children}</div>
          </div>

          {footer ? (
            <div className="mt-4 text-center text-sm text-slate-600 dark:text-slate-400">
              {footer}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  )
}
