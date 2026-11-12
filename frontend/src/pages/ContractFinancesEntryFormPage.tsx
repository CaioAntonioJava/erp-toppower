import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ClipboardPen } from 'lucide-react'
import { Alert } from '../components/ui/Alert'
import { BackButton } from '../components/ui/BackButton'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Textarea } from '../components/ui/Textarea'
import { ContractTypeahead } from '../components/contractfinances/ContractTypeahead'
import {
  createContractFinanceEntry,
  getContractFinanceEntry,
  updateContractFinanceEntry,
} from '../api/contractFinance.api'
import { toApiError } from '../lib/errors'
import { parseNumber } from '../lib/money'
import { useFieldTouched } from '../hooks/useFieldTouched'
import { CONTRACT_FINANCE_TYPES, CONTRACT_FINANCE_TYPE_LABELS } from '../types/contractFinance'
import type { ContractFinanceEntryResponse, ContractFinanceEntryType } from '../types/contractFinance'
import type { ContractResponse } from '../types/contract'

/** Data atual no formato ISO yyyy-MM-dd (fuso local). */
function todayIso(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const TYPE_OPTIONS = [
  { value: '', label: 'Selecione a natureza…' },
  ...CONTRACT_FINANCE_TYPES.map((value) => ({ value, label: CONTRACT_FINANCE_TYPE_LABELS[value] })),
]

/**
 * Formulário do lançamento do financeiro de um contrato (gasto ou
 * recebimento). Em criação, o usuário digita o código do contrato no
 * typeahead (aceita pré-seleção via ?contractId=, vindo do detalhe). Em
 * edição, o contrato é fixo — apenas natureza, data, valor e descrição
 * são editáveis.
 */
export function ContractFinancesEntryFormPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const entryId = id ? Number(id) : null
  const isEdit = entryId != null && Number.isFinite(entryId)

  const [mode, setMode] = useState<'loading' | 'ready'>('loading')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const { shouldShowError, getBlurHandler, markAllTouched } = useFieldTouched()

  // Campos do formulário.
  const [selectedContract, setSelectedContract] = useState<ContractResponse | null>(null)
  const [prefillContractId, setPrefillContractId] = useState<number | null>(
    searchParams.get('contractId') ? Number(searchParams.get('contractId')) : null,
  )
  const [type, setType] = useState<ContractFinanceEntryType | ''>('')
  const [entryDate, setEntryDate] = useState(todayIso())
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  // Contrato do lançamento em modo edição (fixo).
  const [entry, setEntry] = useState<ContractFinanceEntryResponse | null>(null)

  const load = useCallback(async () => {
    setMode('loading')
    setFormError(null)
    try {
      if (isEdit && entryId != null) {
        const data = await getContractFinanceEntry(entryId)
        setEntry(data)
        if (data.contractId != null) setPrefillContractId(data.contractId)
        setType(data.type)
        setEntryDate(data.entryDate)
        setAmount(String(data.amount))
        setDescription(data.description ?? '')
      }
      setMode('ready')
    } catch (err) {
      setFormError(toApiError(err).message)
      setMode('ready')
    }
  }, [entryId, isEdit])

  useEffect(() => {
    void load()
  }, [load])

  function validateAll(): Record<string, string> {
    const errs: Record<string, string> = {}
    if (!isEdit && selectedContract == null) {
      errs.contractId = 'Selecione o contrato (busque pelo código ou cliente).'
    }
    if (!type) {
      errs.type = 'Selecione a natureza do lançamento.'
    }
    if (!entryDate) {
      errs.entryDate = 'Informe a data do lançamento.'
    }
    const parsed = parseNumber(amount)
    if (parsed == null || parsed <= 0) {
      errs.amount = 'Informe um valor maior que zero.'
    }
    return errs
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormError(null)
    const errs = validateAll()
    setFieldErrors(errs)
    markAllTouched()
    if (Object.keys(errs).length > 0) return

    const parsedAmount = Number(parseNumber(amount))
    setSaving(true)
    try {
      if (isEdit && entryId != null) {
        await updateContractFinanceEntry(entryId, {
          type: type as ContractFinanceEntryType,
          entryDate,
          amount: parsedAmount,
          description: description.trim() || undefined,
        })
        if (entry?.controlId != null) {
          navigate(`/contract-finances/controls/${entry.controlId}`, { replace: true })
        } else {
          navigate('/contract-finances', { replace: true })
        }
      } else if (selectedContract != null) {
        await createContractFinanceEntry({
          contractId: selectedContract.id,
          type: type as ContractFinanceEntryType,
          entryDate,
          amount: parsedAmount,
          description: description.trim() || undefined,
        })
        navigate('/contract-finances', { replace: true })
      }
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) {
        setFieldErrors(apiErr.fieldErrors)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
      <BackButton label="Voltar" fallback="/contract-finances" />

      <div className="mt-4 mb-6 flex items-start gap-3">
        <ClipboardPen className="mt-1 h-6 w-6 text-primary" />
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            {isEdit ? 'Editar lançamento' : 'Lançar gasto ou recebimento'}
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {isEdit
              ? 'Somente natureza, data, valor e descrição podem ser alterados; o lançamento permanece vinculado ao contrato original.'
              : 'Digite o código do contrato, escolha a natureza e informe data, valor e descrição.'}
          </p>
        </div>
      </div>

      {formError ? <Alert variant="error">{formError}</Alert> : null}

      {mode === 'loading' ? (
        <div className="flex justify-center py-12">
          <Spinner className="h-8 w-8 text-primary" />
        </div>
      ) : (
        <form
          id="contract-finance-entry-form"
          onSubmit={handleSubmit}
          className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6 dark:border-slate-800 dark:bg-slate-900"
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <ContractTypeahead
                selectedContractId={isEdit ? (prefillContractId ?? null) : prefillContractId}
                onSelect={(contract) => {
                  setSelectedContract(contract)
                  setFieldErrors((prev) => ({ ...prev, contractId: '' }))
                }}
                error={shouldShowError('contractId', fieldErrors.contractId)
                  ? fieldErrors.contractId
                  : null}
              />
            </div>

            <Select
              label="Natureza"
              required
              options={TYPE_OPTIONS}
              value={type}
              onChange={(e) => {
                setType(e.target.value as ContractFinanceEntryType | '')
                setFieldErrors((prev) => ({ ...prev, type: '' }))
              }}
              onBlur={getBlurHandler('type')}
              error={shouldShowError('type', fieldErrors.type) ? fieldErrors.type : null}
              aria-label="Natureza do lançamento"
            />

            <Input
              label="Data do lançamento"
              required
              type="date"
              value={entryDate}
              onChange={(e) => setEntryDate(e.target.value)}
              onBlur={getBlurHandler('entryDate')}
              error={shouldShowError('entryDate', fieldErrors.entryDate)
                ? fieldErrors.entryDate
                : null}
              aria-label="Data do lançamento"
            />

            <Input
              label="Valor (R$)"
              required
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              onBlur={getBlurHandler('amount')}
              error={shouldShowError('amount', fieldErrors.amount) ? fieldErrors.amount : null}
              hint="Ex.: 1.250,00 — RECEBIDO soma nos totais; as demais naturezas são gastos."
              aria-label="Valor do lançamento em reais"
            />

            <Textarea
              label="Descrição"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ex.: cabo 50mm — trecho 2"
              hint="Opcional (máx. 500 caracteres)."
              aria-label="Descrição do lançamento"
            />
          </div>

          <div className="mt-6 flex justify-end gap-2">
            <Button
              variant="secondary"
              type="button"
              onClick={() =>
                entry?.controlId != null
                  ? navigate(`/contract-finances/controls/${entry.controlId}`)
                  : navigate('/contract-finances')
              }
              disabled={saving}
            >
              Cancelar
            </Button>
            <Button variant="primary" type="submit" form="contract-finance-entry-form" isLoading={saving}>
              {isEdit ? 'Salvar alterações' : 'Lançar'}
            </Button>
          </div>
        </form>
      )}
    </div>
  )
}
