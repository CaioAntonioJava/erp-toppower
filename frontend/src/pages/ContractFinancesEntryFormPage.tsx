import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ClipboardPen } from 'lucide-react'
import { Alert } from '../components/ui/Alert'
import { BackButton } from '../components/ui/BackButton'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Spinner } from '../components/ui/Spinner'
import { Textarea } from '../components/ui/Textarea'
import { ContractTypeahead } from '../components/contractfinances/ContractTypeahead'
import {
  createContractFinanceEntriesBatch,
  getContractFinanceEntry,
  updateContractFinanceEntry,
} from '../api/contractFinance.api'
import { toApiError } from '../lib/errors'
import { parseNumber } from '../lib/money'
import { useFieldTouched } from '../hooks/useFieldTouched'
import {
  CONTRACT_FINANCE_TYPES,
  CONTRACT_FINANCE_TYPE_LABELS,
  contractFinanceTypeBadgeTone,
} from '../types/contractFinance'
import type {
  ContractFinanceEntryResponse,
  ContractFinanceEntryType,
} from '../types/contractFinance'
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

interface BatchRow {
  amount: string
  description: string
}

function emptyRows(): Record<ContractFinanceEntryType, BatchRow> {
  const rows = {} as Record<ContractFinanceEntryType, BatchRow>
  for (const type of CONTRACT_FINANCE_TYPES) {
    rows[type] = { amount: '', description: '' }
  }
  return rows
}

/**
 * Formulário de lançamentos do financeiro de contrato.
 *
 * <ul>
 *   <li><b>Criação (em lote)</b>: contrato e data compartilhados no topo e
 *       uma linha por natureza (Recebido, Material, Combustível, Refeição,
 *       Pedágio, Terceiros, Salários, Outros) com valor e descrição
 *       opcionais — apenas as linhas preenchidas são lançadas, então é
 *       possível lançar só o combustível e deixar as demais em branco;</li>
 *   <li><b>Edição (individual)</b>: um lançamento existente — natureza,
 *       data, valor e descrição editáveis; contrato fixo.</li>
 * </ul>
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
  const [rowErrors, setRowErrors] = useState<Partial<Record<ContractFinanceEntryType, string>>>({})
  const { shouldShowError, getBlurHandler, markAllTouched } = useFieldTouched()

  // Campos compartilhados (criação em lote = contrato + data; edição = data).
  const [selectedContract, setSelectedContract] = useState<ContractResponse | null>(null)
  const [prefillContractId, setPrefillContractId] = useState<number | null>(
    searchParams.get('contractId') ? Number(searchParams.get('contractId')) : null,
  )
  const [entryDate, setEntryDate] = useState(todayIso())

  // Linhas do lote (criação): valor/descrição por natureza.
  const [rows, setRows] = useState<Record<ContractFinanceEntryType, BatchRow>>(emptyRows)

  // Campos individuais (edição).
  const [singleType, setSingleType] = useState<ContractFinanceEntryType | ''>('')
  const [singleAmount, setSingleAmount] = useState('')
  const [singleDescription, setSingleDescription] = useState('')
  const [entry, setEntry] = useState<ContractFinanceEntryResponse | null>(null)

  const load = useCallback(async () => {
    setMode('loading')
    setFormError(null)
    try {
      if (isEdit && entryId != null) {
        const data = await getContractFinanceEntry(entryId)
        setEntry(data)
        if (data.contractId != null) setPrefillContractId(data.contractId)
        setSingleType(data.type)
        setEntryDate(data.entryDate)
        setSingleAmount(String(data.amount))
        setSingleDescription(data.description ?? '')
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

  function setRow(type: ContractFinanceEntryType, patch: Partial<BatchRow>) {
    setRows((prev) => ({ ...prev, [type]: { ...prev[type], ...patch } }))
    setRowErrors((prev) => ({ ...prev, [type]: undefined }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setFormError(null)
    setRowErrors({})
    markAllTouched()

    if (isEdit) {
      const errs: Record<string, string> = {}
      if (!singleType) errs.type = 'Selecione a natureza do lançamento.'
      if (!entryDate) errs.entryDate = 'Informe a data do lançamento.'
      const parsed = parseNumber(singleAmount)
      if (parsed == null || parsed <= 0) errs.amount = 'Informe um valor maior que zero.'
      setFieldErrors(errs)
      if (Object.keys(errs).length > 0) return

      setSaving(true)
      try {
        const updated = await updateContractFinanceEntry(entryId!, {
          type: singleType as ContractFinanceEntryType,
          entryDate,
          amount: Number(parsed),
          description: singleDescription.trim() || undefined,
        })
        navigate(`/contract-finances/controls/${updated.controlId}`, { replace: true })
      } catch (err) {
        const apiErr = toApiError(err)
        setFormError(apiErr.message)
        if (apiErr.fieldErrors) setFieldErrors(apiErr.fieldErrors)
      } finally {
        setSaving(false)
      }
      return
    }

    // Criação em lote: apenas as linhas preenchidas são enviadas.
    const errs: Record<string, string> = {}
    if (!selectedContract) errs.contractId = 'Selecione o contrato (busque pelo código ou cliente).'
    if (!entryDate) errs.entryDate = 'Informe a data do lançamento.'
    const rowErrs: Partial<Record<ContractFinanceEntryType, string>> = {}
    const items = CONTRACT_FINANCE_TYPES
      .map((type) => {
        const row = rows[type]
        const trimmedDescription = row.description.trim()
        if (!row.amount.trim() && !trimmedDescription) return null
        const parsed = parseNumber(row.amount)
        if (parsed == null || parsed <= 0) {
          rowErrs[type] = row.amount.trim()
            ? 'Valor inválido — informe um número maior que zero.'
            : 'Preencha o valor para lançar esta natureza (ou apague a descrição).'
          return null
        }
        return { type, amount: Number(parsed), description: trimmedDescription || undefined }
      })
      .filter((item): item is NonNullable<typeof item> => item != null)
    setFieldErrors(errs)
    setRowErrors(rowErrs)
    if (Object.keys(errs).length > 0) return
    if (items.length === 0) {
      setFormError('Preencha o valor de pelo menos um lançamento para salvar.')
      return
    }

    setSaving(true)
    try {
      const created = await createContractFinanceEntriesBatch({
        contractId: selectedContract!.id,
        entryDate,
        items,
      })
      const controlId = created[0]?.controlId ?? selectedContract?.id
      navigate(
        controlId != null ? `/contract-finances/controls/${controlId}` : '/contract-finances',
        { replace: true },
      )
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) setFieldErrors(apiErr.fieldErrors)
    } finally {
      setSaving(false)
    }
  }

  const filledCount = isEdit
    ? 1
    : CONTRACT_FINANCE_TYPES.filter((type) => {
        const row = rows[type]
        const parsedAmount = parseNumber(row.amount)
        return (parsedAmount != null && parsedAmount > 0) || row.description.trim() !== ''
      }).length

  const submitLabel = isEdit
    ? 'Salvar alterações'
    : filledCount > 0
      ? `Lançar ${filledCount} natureza(s)`
      : 'Lançar'

  return (
    <div className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <BackButton label="Voltar" fallback="/contract-finances" />

      <div className="mt-4 mb-6 flex items-start gap-3">
        <ClipboardPen className="mt-1 h-6 w-6 text-primary" />
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            {isEdit ? 'Editar lançamento' : 'Lançar gastos e recebimentos'}
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {isEdit
              ? 'Somente natureza, data, valor e descrição podem ser alterados; o lançamento permanece vinculado ao contrato original.'
              : 'Escolha o contrato e a data, e preencha apenas as naturezas que tem — as linhas em branco são ignoradas.'}
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
          {isEdit ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <ContractTypeahead
                  selectedContractId={prefillContractId ?? null}
                  onSelect={() => {
                    /* contrato fixo na edição; prévia apenas informativa */
                  }}
                  error={undefined}
                />
              </div>
              <Select
                label="Natureza"
                required
                options={TYPE_OPTIONS}
                value={singleType}
                onChange={(e) => {
                  setSingleType(e.target.value as ContractFinanceEntryType | '')
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
                value={singleAmount}
                onChange={(e) => setSingleAmount(e.target.value)}
                onBlur={getBlurHandler('amount')}
                error={shouldShowError('amount', fieldErrors.amount) ? fieldErrors.amount : null}
                hint="Ex.: 1.250,00 — RECEBIDO soma nos totais; as demais naturezas são gastos."
                aria-label="Valor do lançamento em reais"
              />
              <Textarea
                label="Descrição"
                value={singleDescription}
                onChange={(e) => setSingleDescription(e.target.value)}
                placeholder="Ex.: cabo 50mm — trecho 2"
                hint="Opcional (máx. 500 caracteres)."
                aria-label="Descrição do lançamento"
              />
            </div>
          ) : (
            <>
              <div className="grid gap-4 sm:grid-cols-2">
                <ContractTypeahead
                  selectedContractId={prefillContractId ?? null}
                  onSelect={(contract) => {
                    setSelectedContract(contract)
                    setFieldErrors((prev) => ({ ...prev, contractId: '' }))
                  }}
                  error={shouldShowError('contractId', fieldErrors.contractId)
                    ? fieldErrors.contractId
                    : null}
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
                  hint="Aplicada a todas as naturezas lançadas nesta tela."
                  aria-label="Data do lançamento"
                />
              </div>

              {/* Linhas por natureza: apenas as preenchidas são lançadas. */}
              <div className="mt-6">
                <div className="hidden items-center py-2 text-xs font-semibold uppercase tracking-wide text-slate-500 sm:grid sm:grid-cols-[170px_180px_1fr] sm:gap-3 dark:text-slate-400">
                  <span>Natureza</span>
                  <span>Valor (R$)</span>
                  <span>Descrição</span>
                </div>
                <div className="divide-y divide-slate-100 dark:divide-slate-800/60">
                  {CONTRACT_FINANCE_TYPES.map((type) => (
                    <div
                      key={type}
                      className="grid items-center gap-2 py-2.5 sm:grid-cols-[170px_180px_1fr] sm:gap-3"
                    >
                      <div>
                        <Badge tone={contractFinanceTypeBadgeTone(type)}>
                          {CONTRACT_FINANCE_TYPE_LABELS[type]}
                        </Badge>
                      </div>
                      <Input
                        value={rows[type].amount}
                        onChange={(e) => setRow(type, { amount: e.target.value })}
                        placeholder="0,00"
                        inputMode="decimal"
                        aria-label={`Valor em reais de ${CONTRACT_FINANCE_TYPE_LABELS[type]}`}
                      />
                      <div>
                        <Input
                          value={rows[type].description}
                          onChange={(e) => setRow(type, { description: e.target.value })}
                          placeholder="Descrição (opcional)"
                          aria-label={`Descrição de ${CONTRACT_FINANCE_TYPE_LABELS[type]}`}
                        />
                        {rowErrors[type] ? (
                          <p className="mt-1 text-xs text-red-600 dark:text-red-400">
                            {rowErrors[type]}
                          </p>
                        ) : null}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
              <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
                {filledCount} natureza(s) preenchida(s) — só elas serão lançadas.
              </p>
            </>
          )}

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
              {submitLabel}
            </Button>
          </div>
        </form>
      )}
    </div>
  )
}
