import { AlertTriangle } from 'lucide-react'
import { BackButton } from '../components/ui/BackButton'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <div className="max-w-md text-center">
        <div className="mx-auto mb-4 inline-flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-300">
          <AlertTriangle className="h-6 w-6" />
        </div>
        <h1 className="text-2xl font-semibold">Página não encontrada</h1>
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
          A página que você tentou acessar não existe ou foi movida.
        </p>
        <div className="mt-6">
          <BackButton size="md" />
        </div>
      </div>
    </div>
  )
}