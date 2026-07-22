import { useEffect, useRef, useState } from 'react'
import { X, Paperclip } from 'lucide-react'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Spinner } from '../ui/Spinner'
import { Alert } from '../ui/Alert'
import { parseNumber } from '../../lib/money'
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
  description: string
  payee: string
  valor: string
  dueDate: string
}

const EMPTY: FormState = {
  description: '',
  payee: '',
  valor: '',
  dueDate: '',
}

/** Aceita PDF e imagens. */
const ACCEPT = '.pdf,.png,.jpg,.jpeg'
const MAX_SIZE_BYTES = 10 * 1024 * 1024 // 10MB

/**
 * Modal de cadastro de boleto.
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
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  // Fornecedor vinculado (opcional). Quando selecionado, o backend gera
  // automaticamente uma conta a pagar a partir do boleto.
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
          description: editBoleto.description,
          payee: editBoleto.payee,
          valor: String(editBoleto.value),
          dueDate: editBoleto.dueDate,
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
    const description = form.description.trim()
    const payee = form.payee.trim()
    const valor = parseNumber(form.valor)
    const dueDate = form.dueDate

    if (!description) {
      setError('Informe a descrição do boleto.')
      return
    }
    if (!payee) {
      setError('Informe o beneficiário (cliente ou fornecedor).')
      return
    }
    if (valor == null || valor <= 0) {
      setError('Informe um valor válido maior que zero.')
      return
    }
    if (!dueDate) {
      setError('Informe a data de vencimento.')
      return
    }

    setError(null)
    setSubmitting(true)
    try {
      if (isEditing && editBoleto && onUpdate) {
        await onUpdate(editBoleto.id, {
          description,
          payee,
          value: valor,
          dueDate,
          supplierId: supplierId ?? null,
        })
      } else {
        await onSubmit({
          description,
          payee,
          value: valor,
          dueDate,
          supplierId: supplierId ?? null,
          attachment: attachment ?? undefined,
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
        className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900"
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
          <Input
            label="Descrição do boleto"
            required
            placeholder="Ex.: Pagamento fornecedor XYZ"
            value={form.description}
            onChange={(e) => setField('description', e.target.value)}
          />
          <Input
            label="Beneficiário"
            required
            placeholder="Cliente ou fornecedor"
            value={form.payee}
            onChange={(e) => setField('payee', e.target.value)}
          />

          {/* Fornecedor vinculado (opcional). Quando selecionado, o
              backend gera automaticamente uma conta a pagar a partir
              deste boleto. */}
          <div>
            <Input
              label="Fornecedor vinculado (opcional)"
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
                  : 'Selecione um fornecedor para gerar automaticamente uma conta a pagar.'
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
            />
            <Input
              label="Vencimento"
              required
              type="date"
              value={form.dueDate}
              onChange={(e) => setField('dueDate', e.target.value)}
            />
          </div>

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