import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { NotebookPen, Plus, Search } from 'lucide-react'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Spinner } from '../components/ui/Spinner'
import { listContractFinancesControls } from '../api/contractFinance.api'
import { toApiError } from '../lib/errors'
import { formatCurrency, formatDate } from '../lib/format'
import type { ContractFinanceControlResponse, ContractFinanceControlsPage } from '../types/contractFinance'

/** Tamanho da página da tabela de controles. */
const PAGE_SIZE = 20

/**
 * Página do Financeiro de Contratos: lista os controles por contrato
 * (código, cliente, data, valor) com os totais agregados de recebido,
 * gasto e resultado. Cada linha abre o detalhe do controle, onde são
 * lançados os gastos/recebimentos.
 */
export function ContractFinancesListPage() {
  const navigate = useNavigate()
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<ContractFinanceControlsPage | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await listContractFinancesControls({ query, page, size: PAGE_SIZE }))
    } catch (err) {
      setError(toApiError(err).message)
    } finally {
      setLoading(false)
    }
  }, [query, page])

  useEffect(() => {
    void reload()
  }, [reload])

  // Debounce da busca livre (300ms, padrão do projeto).
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(queryInput.trim())
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [queryInput])

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            Financeiro de Contratos
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Controle de gastos e recebimentos por contrato. Abra um contrato
            para lançar os movimentos.
          </p>
        </div>
        <Button variant="primary" onClick={() => navigate('/contract-finances/new')}>
          <Plus className="mr-1.5 h-4 w-4" />
          Lançar
        </Button>
      </div>

      <section className="mb-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <Input
          label="Buscar"
          value={queryInput}
          onChange={(e) => setQueryInput(e.target.value)}
          placeholder="Código do contrato (CL-001-2026) ou cliente…"
          leftAdornment={<Search className="h-4 w-4" />}
          aria-label="Buscar contrato ou cliente"
        />
      </section>

      {error ? <Alert variant="error">{error}</Alert> : null}

      {loading ? (
        <div className="flex justify-center py-12">
          <Spinner className="h-8 w-8 text-primary" />
        </div>
      ) : data ? (
        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
            <h2 className="inline-flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
              <NotebookPen className="h-4 w-4" />
              Contratos ({data.totalElements})
            </h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
              <thead>
                <tr className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  <th className="px-4 py-3">Contrato</th>
                  <th className="px-4 py-3">Cliente</th>
                  <th className="px-4 py-3">Data</th>
                  <th className="px-4 py-3 text-right">Valor do contrato</th>
                  <th className="px-4 py-3 text-right">Recebido</th>
                  <th className="px-4 py-3 text-right">Gastos</th>
                  <th className="px-4 py-3 text-right">Resultado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                {data.content.length === 0 ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-10 text-center text-sm text-slate-500 dark:text-slate-400"
                    >
                      Nenhum contrato encontrado com os filtros aplicados.
                    </td>
                  </tr>
                ) : (
                  data.content.map((control) => (
                    <ContratoRow
                      key={control.id}
                      control={control}
                      onOpen={() => navigate(`/contract-finances/controls/${control.id}`)}
                    />
                  ))
                )}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between gap-2 border-t border-slate-200 px-4 py-3 text-sm dark:border-slate-800">
            <span className="text-slate-500 dark:text-slate-400">
              {data.totalElements} contrato(s) — Página {data.page + 1} de {data.totalPages || 1}
            </span>
            <div className="flex gap-2">
              <Button size="sm" variant="secondary" disabled={data.first} onClick={() => setPage((p) => Math.max(0, p - 1))}>
                Anterior
              </Button>
              <Button size="sm" variant="secondary" disabled={data.last} onClick={() => setPage((p) => p + 1)}>
                Próxima
              </Button>
            </div>
          </div>
        </section>
      ) : null}
    </div>
  )
}

/** Linha da tabela de controles, clicável para abrir o detalhe. */
function ContratoRow({
  control,
  onOpen,
}: {
  control: ContractFinanceControlResponse
  onOpen: () => void
}) {
  return (
    <tr
      className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/50"
      onClick={onOpen}
    >
      <td className="whitespace-nowrap px-4 py-3 font-mono text-xs font-medium text-primary">
        {control.contractCode}
      </td>
      <td className="px-4 py-3">
        <div className="font-medium text-slate-800 dark:text-slate-200">{control.clientName ?? '—'}</div>
        <div className="text-xs text-slate-500 dark:text-slate-400">{control.clientCode ?? ''}</div>
      </td>
      <td className="whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300">
        {formatDate(control.contractDate) || '—'}
      </td>
      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs">
        {formatCurrency(control.contractValue)}
      </td>
      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs text-emerald-600 dark:text-emerald-400">
        {formatCurrency(control.totalRecebido)}
      </td>
      <td className="whitespace-nowrap px-4 py-3 text-right font-mono text-xs text-red-600 dark:text-red-400">
        {formatCurrency(control.totalGasto)}
      </td>
      <td
        className={[
          'whitespace-nowrap px-4 py-3 text-right font-mono text-xs font-semibold',
          control.resultado < 0
            ? 'text-red-600 dark:text-red-400'
            : 'text-slate-900 dark:text-slate-100',
        ].join(' ')}
      >
        {formatCurrency(control.resultado)}
      </td>
    </tr>
  )
}
