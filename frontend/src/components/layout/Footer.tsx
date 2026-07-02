import { ThemeToggle } from '../ui/ThemeToggle'

/**
 * Rodapé da aplicação autenticada.
 *
 * Layout em três colunas no desktop:
 *  - esquerda: copyright
 *  - centro: crédito ao desenvolvedor (centralizado)
 *  - direita: botão de seleção de tema (claro/escuro)
 *
 * Em telas pequenas os blocos são empilhados verticalmente,
 * todos centralizados.
 */
export function Footer() {
  const year = new Date().getFullYear()

  return (
    <footer className="flex flex-col items-center gap-3 border-t border-slate-200 bg-white px-4 py-3 dark:border-slate-800 dark:bg-slate-900 md:grid md:grid-cols-3 md:items-center md:gap-4">
      <span className="text-center text-xs text-slate-500 dark:text-slate-400 md:text-left md:justify-self-start">
        © {year} ERP TOP POWER. Todos os direitos reservados.
      </span>

      <span className="text-center text-xs text-slate-500 dark:text-slate-400">
        Desenvolvido por:{' '}
        <a
          href="mailto:caioantonio.dev@gmail.com"
          className="font-medium text-slate-700 underline-offset-2 transition-colors hover:text-primary hover:underline dark:text-slate-200 dark:hover:text-primary-200"
        >
          Caio Henrique Antonio
        </a>
      </span>

      <div className="flex justify-center md:justify-end">
        <ThemeToggle />
      </div>
    </footer>
  )
}