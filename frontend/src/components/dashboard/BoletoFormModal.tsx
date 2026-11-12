import { useEffect, useRef, useState } from 'react'
import { X, Paperclip } from 'lucide-react'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Spinner } from '../ui/Spinner'
import { Alert } from '../ui/Alert'
import { parseNumber } from '../../lib/money'
import { formatCurrency, formatDate } from '../../lib/format'
import { searchSuppliers } from '../../api/supplier.api'
import type { SupplierResponse } from '../../types/supplier'
import type { BoletoResponse, BoletoUpdateRequest } from '../../types/boleto'
import type { NovoBoletoInput } from '../../hooks/useBoletosStorage'

interface BoletoFormModalProps {
  open: boolean
  onClose: () => void
  onSubmit: (input: NovoBoletoInput) => void | Promise<void>
  /** Quando informado, o modal opera em modo de edição. */
  editBoleto?: BoletoResponse | null
  /** Callback chamado ao salvar a edição. */
  onUpdate?: (id: number, input: BoletoUpdateRequest) => void | Promise<void>
}

/** Estado do formulário de cadastro de boleto. */
interface FormState {
  contractWorkNumber: string
  responsibleName: string
  invoiceNumber: string
  invoiceDate: string
  installmentNumber: string
  valor: string
  dueDate: string
  installmentsCount: string
  installmentTerms: string
}

const EMPTY: FormState = {
  contractWorkNumber: '',
  responsibleName: '',
  invoiceNumber: '',
  invoiceDate: '',
  installmentNumber: '',
  valor: '',
  dueDate: '',
  installmentsCount: '1',
  installmentTerms: '',
}

/** Aceita PDF e imagens. */
const ACCEPT = '.pdf,.png,.jpg,.jpeg'
const MAX_SIZE_BYTES = 10 * 1024 * 1024 // 10MB

/** Preview das parcelas calculadas a partir do valor total, data base
 * (vencimento) e prazos informados. Replica a lógica do backend (divisão
 * igualitária com residual na última parcela) para confirmação visual. */
function ParcelasPreview({ form }: { form: FormState }): React.ReactNode {
  const total = parseNumber(form.valor)
  // Sem data base preenchida, não exibe o preview — evita calcular
  // vencimentos a partir de hoje sem o usuário ter informado a data base.
  if (!form.dueDate) return null
  const baseDate = form.dueDate
  const termos = form.installmentTerms.split('/').map((t) => parseInt(t.trim(), 10))
  const n = parseInt(form.installmentsCount, 10) || 1
  if (total == null || total <= 0 || n < 2) return null
  if (termos.length !== n || termos.some((t) => Number.isNaN(t) || t < 0)) return null

  const baseShare = total / n
  let acumulado = 0
  const parcelas = termos.map((dias, i) => {
    let valor: number
    if (i === n - 1) {
      valor = total - acumulado
    } else {
      valor = baseShare
      acumulado += baseShare
    }
    const venc = new Date(baseDate)
    venc.setDate(venc.getDate() + dias)
    return { num: i + 1, valor, venc: venc.toISOString().slice(0, 10) }
  })

  return (
    <ul className="mt-3 space-y-1 rounded-md bg-slate-50 p-2 text-xs dark:bg-slate-800/50">
      {parcelas.map((p) => (
        <li key={p.num} className="flex items-center justify-between">
          <span className="text-slate-600 dark:text-slate-300">
            Parcela {p.num}/{n} · {formatDate(p.venc)}
          </span>
          <span className="font-medium text-slate-900 dark:text-slate-100">
            {formatCurrency(p.valor)}
          </span>
        </li>
      ))}
    </ul>
  )
}

/**
 * Modal de cadastro/edição de boleto.
 *
 * Formulário controlado com validação simples (campos obrigatórios +
 * valor numérico válido + data no formato ISO yyyy-MM-dd). Segue o padrão
 * visual do ConfirmDialog: overlay, fecha com ESC/clique fora, bloqueia
 * scroll do body. Os dados são persistidos pelo hook `useBoletosStorage`
 * (chamadas à API `/api/v1/boletos`). O anexo (opcional) é enviado após
 * o cadastro do boleto, reusing o endpoint de anexos.
 */
export function BoletoFormModal({ open, onClose, onSubmit, editBoleto, onUpdate }: BoletoFormModalProps) {
  const isEditing = editBoleto != null
  const [form, setForm] = useState<FormState>(EMPTY)
  const [attachment, setAttachment] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  // Modo parcelamento ativo quando parcelas > 1 (apenas na criação).
  const parcelado = !isEditing && (parseInt(form.installmentsCount, 10) || 1) > 1
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  // Empresa (fornecedor) vinculado (opcional). Quando selecionado, o
  // backend gera automaticamente uma conta a pagar a partir do boleto.
  const [supplierQuery, setSupplierQuery] = useState('')
  const [supplierId, setSupplierId] = useState<number | null>(null)
  const [supplierOptions, setSupplierOptions] = useState<SupplierResponse[]>([])
  const [supplierSearching, setSupplierSearching] = useState(false)

  // Bloqueia scroll do body enquanto aberto.
  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [open])

  // Fecha com ESC.
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  // Reseta o formulário a cada abertura.
  useEffect(() => {
    if (open) {
      if (editBoleto) {
        setForm({
          contractWorkNumber: editBoleto.contractWorkNumber ?? '',
          responsibleName: editBoleto.responsibleName ?? '',
          invoiceNumber: editBoleto.invoiceNumber ?? '',
          invoiceDate: editBoleto.invoiceDate ?? '',
          installmentNumber: editBoleto.installmentNumber != null ? String(editBoleto.installmentNumber) : '',
          valor: String(editBoleto.value),
          dueDate: editBoleto.dueDate,
          // No modo edição não há parcelamento (parcelas só na criação).
          installmentsCount: '1',
          installmentTerms: '',
        })
        setSupplierQuery(editBoleto.supplierName ?? '')
        setSupplierId(editBoleto.supplierId)
      } else {
        setForm(EMPTY)
        setSupplierQuery('')
        setSupplierId(null)
      }
      setAttachment(null)
      setError(null)
      setSubmitting(false)
      setSupplierOptions([])
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }, [open, editBoleto])

  // Busca de fornecedor (debounce) — opcional. Quando um fornecedor é
  // selecionado, o backend gera uma conta a pagar ao cadastrar o boleto.
  function handleSupplierQuery(value: string): void {
    const trimmed = value.trim()
    if (trimmed.length < 2) {
      setSupplierOptions([])
      return
    }
    setSupplierSearching(true)
    const timer = setTimeout(async () => {
      try {
        const result = await searchSuppliers({ query: trimmed, page: 0, size: 20 })
        setSupplierOptions(result.content)
      } catch {
        setSupplierOptions([])
      } finally {
        setSupplierSearching(false)
      }
    }, 300)
    void timer
  }

  if (!open) return null

  function setField<K extends keyof FormState>(key: K, value: FormState[K]): void {
    setForm((f) => ({ ...f, [key]: value }))
  }

  async function handleSubmit(e: React.FormEvent): Promise<void> {
    e.preventDefault()
    const contractWorkNumber = form.contractWorkNumber.trim()
    const responsibleName = form.responsibleName.trim()
    const invoiceNumber = form.invoiceNumber.trim()
    const invoiceDate = form.invoiceDate || undefined
    const installmentNumberRaw = form.installmentNumber.trim()
    const installmentNumber = installmentNumberRaw ? parseInt(installmentNumberRaw, 10) : null
    const valor = parseNumber(form.valor)
    const dueDate = form.dueDate
    const installmentsCount = parseInt(form.installmentsCount, 10) || 1
    const installmentTerms = form.installmentTerms.trim()

    if (valor == null || valor <= 0) {
      setError('Informe um valor válido maior que zero.')
      return
    }
    if (!dueDate) {
      setError('Informe a data de vencimento / data base.')
      return
    }
    if (installmentsCount > 1) {
      // Em modo parcelamento, o dueDate é a data base e os vencimentos
      // são calculados a partir dele + installmentTerms.
      if (!installmentTerms) {
        setError('Informe os prazos das parcelas (ex.: 30/60/90).')
        return
      }
      const termos = installmentTerms.split('/').map((t) => parseInt(t.trim(), 10))
      if (termos.length !== installmentsCount || termos.some((t) => Number.isNaN(t) || t < 0)) {
        setError(
          `Informe exatamente ${installmentsCount} prazo(s) em dias, separados por barra (ex.: 30/60/90).`,
        )
        return
      }
    }

    setError(null)
    setSubmitting(true)
    try {
      if (isEditing && editBoleto && onUpdate) {
        await onUpdate(editBoleto.id, {
          contractWorkNumber: contractWorkNumber || null,
          responsibleName: responsibleName || null,
          value: valor,
          dueDate,
          supplierId: supplierId ?? null,
          invoiceNumber: invoiceNumber || null,
          invoiceDate: invoiceDate || null,
          installmentNumber: installmentNumber != null && installmentNumber >= 1 ? installmentNumber : null,
        })
      } else {
        await onSubmit({
          contractWorkNumber: contractWorkNumber || null,
          responsibleName: responsibleName || null,
          value: valor,
          dueDate,
          supplierId: supplierId ?? null,
          attachment: attachment ?? undefined,
          invoiceNumber: invoiceNumber || null,
          invoiceDate: invoiceDate || null,
          installmentNumber: !parcelado && installmentNumber != null && installmentNumber >= 1
            ? installmentNumber
            : null,
          installmentsCount,
          installmentTerms: installmentsCount > 1 ? installmentTerms : undefined,
        })
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Falha ao salvar o boleto. Tente novamente.',
      )
      setSubmitting(false)
      return
    }
    setSubmitting(false)
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>): void {
    const file = e.target.files?.[0] ?? null
    if (file != null && file.size > MAX_SIZE_BYTES) {
      setError('Anexo excede o limite de 10MB.')
      if (fileInputRef.current) fileInputRef.current.value = ''
      return
    }
    setError(null)
    setAttachment(file)
  }

  /**
   * Ao sair do campo de valor, completa com `,00` quando o usuário
   * digitou apenas a parte inteira (sem separador decimal). Se já houver
   * vírgula ou ponto, mantém como está (o parseNumber normaliza depois).
   */
  function handleValorBlur(e: React.FocusEvent<HTMLInputElement>): void {
    const raw = e.target.value.trim()
    if (!raw) return
    if (!raw.includes(',') && !raw.includes('.')) {
      setField('valor', `${raw},00`)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 backdrop-blur-sm"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-3xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
          <h2 className="text-base font-semibold">{isEditing ? 'Editar boleto' : 'Cadastrar boleto'}</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-slate-500 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800"
            aria-label="Fechar"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="space-y-4 px-5 py-5">
          {/* Linha 1: Nº Obra + Nome do responsável */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="Nº Obra (opcional)"
              placeholder="Ex.: CT-001-2026"
              value={form.contractWorkNumber}
              onChange={(e) => setField('contractWorkNumber', e.target.value)}
            />
            <Input
              label="Nome do responsável (opcional)"
              placeholder="Ex.: João da Silva"
              value={form.responsibleName}
              onChange={(e) => setField('responsibleName', e.target.value)}
            />
          </div>

          {/* Empresa (fornecedor) vinculado (opcional). Quando selecionado,
              o backend gera automaticamente uma conta a pagar a partir
              deste boleto. Ocupa largura total no modo paisagem. */}
          <div>
            <Input
              label="Empresa / Fornecedor vinculado (opcional)"
              placeholder="Buscar por nome ou CNPJ…"
              value={supplierQuery}
              onChange={(e) => {
                setSupplierQuery(e.target.value)
                setSupplierId(null)
                handleSupplierQuery(e.target.value)
              }}
              hint={
                supplierId
                  ? 'Ao cadastrar o boleto, será gerada uma conta a pagar no módulo de Contas a Pagar.'
                  : 'Selecione uma empresa para gerar automaticamente uma conta a pagar.'
              }
              rightAdornment={supplierSearching ? <Spinner size="sm" /> : undefined}
            />
            {supplierOptions.length > 0 && !supplierId ? (
              <ul className="mt-1 max-h-48 overflow-auto rounded-lg border border-slate-200 bg-white text-sm dark:border-slate-700 dark:bg-slate-900">
                {supplierOptions.map((s) => (
                  <li key={s.id}>
                    <button
                      type="button"
                      className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left hover:bg-slate-50 dark:hover:bg-slate-800"
                      onClick={() => {
                        setSupplierId(s.id)
                        setSupplierQuery(s.tradeName || s.legalName)
                        setSupplierOptions([])
                      }}
                    >
                      <span className="text-slate-900 dark:text-slate-100">
                        {s.tradeName || s.legalName}
                      </span>
                      <span className="text-xs text-slate-500">{s.taxId}</span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>

          {/* Linha 2: Nota fiscal + Data da NF + Nº parcela */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Input
              label="Nota fiscal (opcional)"
              placeholder="Ex.: NF-00123"
              value={form.invoiceNumber}
              onChange={(e) => setField('invoiceNumber', e.target.value)}
            />
            <Input
              label="Data da NF (opcional)"
              type="date"
              value={form.invoiceDate}
              onChange={(e) => setField('invoiceDate', e.target.value)}
            />
            <Input
              label="Nº parcela (opcional)"
              type="number"
              min={1}
              inputMode="numeric"
              placeholder="Ex.: 1"
              value={form.installmentNumber}
              onChange={(e) => setField('installmentNumber', e.target.value)}
              disabled={parcelado}
              hint={parcelado ? 'Gerado automaticamente em parcelamento.' : undefined}
            />
          </div>

          {/* Linha 3: Valor + Vencimento */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="Valor (R$)"
              required
              inputMode="decimal"
              placeholder="0,00"
              leftAdornment={<span className="text-sm">R$</span>}
              value={form.valor}
              onChange={(e) => setField('valor', e.target.value)}
              onBlur={handleValorBlur}
              hint={parcelado ? 'Valor total que será dividido entre as parcelas.' : undefined}
            />
            <Input
              label="Vencimento / Data Base"
              required
              type="date"
              value={form.dueDate}
              onChange={(e) => setField('dueDate', e.target.value)}
            />
          </div>

          {/* Parcelamento — só na criação (não disponível na edição). */}
          {!isEditing ? (
            <div className="rounded-lg border border-slate-200 p-3 dark:border-slate-800">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <Input
                  label="Parcelas"
                  type="number"
                  min={1}
                  inputMode="numeric"
                  value={form.installmentsCount}
                  onChange={(e) => setField('installmentsCount', e.target.value)}
                />
                <Input
                  label="Prazos (dias)"
                  placeholder="Ex.: 30/60/90"
                  value={form.installmentTerms}
                  onChange={(e) => setField('installmentTerms', e.target.value)}
                  disabled={!parcelado}
                />
              </div>
              {parcelado ? <ParcelasPreview form={form} /> : null}
            </div>
          ) : null}

          {!isEditing ? (
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
                Anexo (opcional)
              </label>
              <div className="flex items-center gap-3">
                <label
                  className="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-slate-300 bg-slate-50 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                >
                  <Paperclip className="h-4 w-4" />
                  Escolher arquivo
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept={ACCEPT}
                    onChange={handleFileChange}
                    disabled={submitting}
                    className="hidden"
                  />
                </label>
                {attachment ? (
                  <span className="truncate text-sm text-slate-600 dark:text-slate-300">
                    {attachment.name}
                  </span>
                ) : (
                  <span className="text-xs text-slate-400 dark:text-slate-500">
                    PDF, PNG ou JPEG — até 10MB
                  </span>
                )}
              </div>
            </div>
          ) : null}

          {error ? (
            <Alert variant="error">{error}</Alert>
          ) : null}
        </div>

        <div className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4 dark:border-slate-800">
          <Button type="button" variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" isLoading={submitting}>
            {isEditing ? 'Salvar' : 'Cadastrar'}
          </Button>
        </div>
      </form>
    </div>
  )
}