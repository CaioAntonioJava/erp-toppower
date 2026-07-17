import { useAuth } from '../context/AuthContext'
import {
  AccountsPayableWidget,
  AccountsReceivableWidget,
  BoletosCadastradosWidget,
  BoletosDueWidget,
  FinanceSummaryWidget,
} from '../components/dashboard'

/**
 * Dashboard principal do ERP.
 *
 * Layout em seções, preparado para os módulos financeiros
 * (Contas a Pagar, Contas a Receber e Boletos próximos do vencimento).
 * Os widgets já buscam dados via `api/finance.api.ts` — hoje retornam
 * listas vazias/resumo zero enquanto o backend financeiro não existe.
 */
export function DashboardPage() {
  const { user } = useAuth()

  return (
    <div className="space-y-6">
      {/* Cabeçalho — boas-vindas + identificação da sessão. */}
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Bem-vindo,{' '}
          <span className="font-medium text-slate-700 dark:text-slate-200">
            {user?.email}
          </span>
          . Utilize o menu lateral para navegar pelos módulos e acessar os
          recursos disponíveis.
        </p>
      </div>

      {/* Boletos cadastrados pela usuária — cadastro e listagem inline. */}
      <BoletosCadastradosWidget />

      {/* Indicadores financeiros — totais a pagar/receber e boletos. */}
      <FinanceSummaryWidget />

      {/* Colunas: contas a pagar vs contas a receber. */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <AccountsPayableWidget />
        <AccountsReceivableWidget />
      </div>

      {/* Boletos próximos do vencimento — largura total. */}
      <BoletosDueWidget />
    </div>
  )
}