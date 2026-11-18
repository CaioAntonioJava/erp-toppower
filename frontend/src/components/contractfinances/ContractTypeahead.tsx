import { useEffect, useRef, useState } from 'react'
import { Search, X } from 'lucide-react'
import { Input } from '../ui/Input'
import { getContract, searchContracts } from '../../api/contract.api'
import { formatBRLValue } from '../../lib/money'
import type { ContractResponse } from '../../types/contract'

/**
 * Typeahead de contratos por código/nome do cliente para o financeiro
 * de contratos. Busca em /contracts/search (debounce 300ms, mínimo de 2
 * caracteres) e, ao selecionar, mostra uma prévia do retrato comercial
 * do contrato (código, cliente, data de vigência e valor).
 */
export function ContractTypeahead({
  selectedContractId,
  onSelect,
  error,
}: {
  /** ID do contrato selecionado (controla o item em modo de edição). */
  selectedContractId: number | null
  onSelect: (contract: ContractResponse | null) => void
  error?: string | null
}) {
  const [text, setText] = useState('')
  const [selected, setSelected] = useState<ContractResponse | null>(null)
  const [options, setOptions] = useState<ContractResponse[]>([])
  const [open, setOpen] = useState(false)
  const [searching, setSearching] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const wrapperRef = useRef<HTMLDivElement | null>(null)

  // Busca o contrato já selecionado (modo edição/prefill) uma única vez.
  useEffect(() => {
    if (selectedContractId == null || selected?.id === selectedContractId) return
    let cancelled = false
    void (async () => {
      try {
        const contract = await getContract(selectedContractId)
        if (cancelled) return
        setSelected(contract)
        setText(contract.code)
        // Notifica o pai para manter o estado do formulário sincronizado
        // com a pré-seleção (?contractId= ou edição).
        onSelect(contract)
      } catch {
        if (!cancelled) setLoadError('Não foi possível carregar o contrato selecionado.')
      }
    })()
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- roda só quando o id externo muda
  }, [selectedContractId])

  // Busca com debounce — apenas quando não há contrato selecionado.
  useEffect(() => {
    if (selected) return
    const trimmed = text.trim()
    if (trimmed.length < 2) {
      setOptions([])
      setOpen(false)
      return
    }
    let cancelled = false
    const timer = setTimeout(async () => {
      setSearching(true)
      try {
        const data = await searchContracts({ query: trimmed, page: 0, size: 8 })
        if (cancelled) return
        setOptions(data.content)
        setOpen(data.content.length > 0)
      } catch {
        if (!cancelled) {
          setOptions([])
          setOpen(false)
        }
      } finally {
        if (!cancelled) setSearching(false)
      }
    }, 300)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [text, selected])

  // Fecha o dropdown ao clicar fora.
  useEffect(() => {
    if (!open) return
    function onPointerDown(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [open])

  function pick(contract: ContractResponse) {
    setSelected(contract)
    setText(contract.code)
    onSelect(contract)
    setOpen(false)
    setOptions([])
  }

  function clear() {
    setSelected(null)
    setText('')
    setLoadError(null)
    onSelect(null)
  }

  function handleTextChange(value: string) {
    // Digitar de novo libera o typeahead (e desmarca o contrato até nova escolha).
    if (selected) {
      setSelected(null)
      onSelect(null)
    }
    setText(value)
  }

  return (
    <div className="w-full" ref={wrapperRef}>
      <Input
        label="Contrato"
        value={text}
        onChange={(e) => handleTextChange(e.target.value)}
        placeholder="Código (CL-001-2026) ou cliente…"
        error={error ?? loadError}
        required
        aria-label="Buscar contrato pelo código ou cliente"
        rightAdornment={
          selected ? (
            <button
              type="button"
              onClick={clear}
              className="rounded p-0.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800"
              aria-label="Limpar contrato selecionado"
            >
              <X className="h-4 w-4" />
            </button>
          ) : (
            <Search className="h-4 w-4" />
          )
        }
      />
      {open ? (
        <div className="mt-1 max-h-60 w-full overflow-y-auto rounded-lg border border-slate-200 bg-white shadow-lg dark:border-slate-700 dark:bg-slate-900">
          {searching ? (
            <div className="px-3 py-2 text-xs text-slate-500 dark:text-slate-400">Buscando…</div>
          ) : (
            options.map((contract) => (
              <button
                key={contract.id}
                type="button"
                className="flex w-full flex-col items-start px-3 py-2 text-left text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
                onClick={() => pick(contract)}
              >
                <span className="font-mono font-medium text-slate-800 dark:text-slate-200">
                  {contract.code}
                </span>
                <span className="text-xs text-slate-500 dark:text-slate-400">
                  {contract.clientName ?? '—'} · R$ {formatBRLValue(contract.price)}
                </span>
              </button>
            ))
          )}
        </div>
      ) : null}

      {selected ? (
        <div className="mt-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs dark:border-slate-700 dark:bg-slate-800/60">
          <div className="font-mono text-sm font-medium text-slate-800 dark:text-slate-200">
            {selected.code}
          </div>
          <div className="mt-0.5 text-slate-500 dark:text-slate-400">
            Cliente: {selected.clientName ?? '—'}
            {selected.validityDate
              ? ` · Vigência: ${selected.validityDate.split('-').reverse().join('/')}`
              : ''}
            {` · Valor do contrato: R$ ${formatBRLValue(selected.price)}`}
          </div>
        </div>
      ) : null}
    </div>
  )
}
