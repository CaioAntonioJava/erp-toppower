import { useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  CheckCircle2,
  FileUp,
  Package,
  Building2,
  Receipt,
  AlertTriangle,
  Loader2,
} from 'lucide-react'
import { BackButton } from '../components/ui/BackButton'
import { Button } from '../components/ui/Button'
import { Alert } from '../components/ui/Alert'
import { Badge } from '../components/ui/Badge'
import { formatCurrency, formatDate } from '../lib/format'
import { previewNfeImport, confirmNfeImport } from '../api/purchase.api'
import { toApiError } from '../lib/errors'
import type {
  NfeConfirmResponse,
  NfeItemData,
  NfePreviewResponse,
} from '../types/purchase'

type Step = 'upload' | 'preview' | 'success' | 'error'

const STATUS_CONFIG = {
  NOVO: { tone: 'success' as const, label: 'Novo' },
  EXISTENTE: { tone: 'info' as const, label: 'Existente' },
  DIVERGENTE: { tone: 'warning' as const, label: 'Divergente' },
}

export function PurchaseImportPage() {
  const [step, setStep] = useState<Step>('upload')
  const [preview, setPreview] = useState<NfePreviewResponse | null>(null)
  const [result, setResult] = useState<NfeConfirmResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [fileName, setFileName] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  async function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>): Promise<void> {
    const file = e.target.files?.[0]
    if (!file) return
    await handleUpload(file)
  }

  async function handleUpload(file: File): Promise<void> {
    setError(null)
    setFileName(file.name)
    setLoading(true)
    setStep('upload')
    try {
      const res = await previewNfeImport(file)
      setPreview(res)
      setStep('preview')
    } catch (err) {
      setError(toApiError(err).message)
      setStep('error')
    } finally {
      setLoading(false)
    }
  }

  async function handleConfirm(): Promise<void> {
    if (!preview) return
    setError(null)
    setLoading(true)
    try {
      const res = await confirmNfeImport({ xmlBase64: preview.xmlBase64 })
      setResult(res)
      setStep('success')
    } catch (err) {
      setError(toApiError(err).message)
      setStep('error')
    } finally {
      setLoading(false)
    }
  }

  function handleReset(): void {
    setStep('upload')
    setPreview(null)
    setResult(null)
    setError(null)
    setFileName(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  function handleDrop(e: React.DragEvent): void {
    e.preventDefault()
    const file = e.dataTransfer.files?.[0]
    if (file && file.name.endsWith('.xml')) {
      void handleUpload(file)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <BackButton fallback="/payables" />
        <h1 className="mt-1 text-2xl font-semibold tracking-tight">
          Importar NF-e (Nota de Compra)
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Faça upload do XML da NF-e para cadastrar automaticamente fornecedor, produtos,
          entrada de estoque e conta a pagar.
        </p>
      </div>

      {error ? <Alert variant="error">{error}</Alert> : null}

      {/* Passo 1: Upload */}
      {step === 'upload' ? (
        <div
          className="rounded-2xl border-2 border-dashed border-slate-300 bg-white p-12 text-center dark:border-slate-700 dark:bg-slate-900"
          onDrop={handleDrop}
          onDragOver={(e) => e.preventDefault()}
        >
          {loading ? (
            <div className="flex flex-col items-center gap-3">
              <Loader2 className="h-10 w-10 animate-spin text-primary" />
              <p className="text-sm text-slate-500">Processando XML…</p>
            </div>
          ) : (
            <>
              <FileUp className="mx-auto h-12 w-12 text-slate-300 dark:text-slate-600" />
              <p className="mt-4 text-sm font-medium text-slate-700 dark:text-slate-200">
                Arraste o arquivo XML da NF-e aqui
              </p>
              <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                ou clique para selecionar
              </p>
              <label className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/90">
                <FileUp className="h-4 w-4" />
                Selecionar arquivo
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".xml"
                  onChange={handleFileSelect}
                  className="hidden"
                />
              </label>
              {fileName ? (
                <p className="mt-3 text-xs text-slate-500">Arquivo: {fileName}</p>
              ) : null}
            </>
          )}
        </div>
      ) : null}

      {/* Passo 2: Preview */}
      {step === 'preview' && preview ? (
        <div className="space-y-6">
          {/* Fornecedor */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-4 flex items-center gap-2">
              <Building2 className="h-5 w-5 text-primary" />
              <h2 className="text-base font-semibold">Fornecedor</h2>
              <Badge tone={preview.supplier.existing ? 'info' : 'success'}>
                {preview.supplier.existing ? 'Cadastrado' : 'Novo'}
              </Badge>
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              <div>
                <span className="text-xs text-slate-500">Razão Social</span>
                <p className="text-sm font-medium">{preview.supplier.legalName}</p>
              </div>
              <div>
                <span className="text-xs text-slate-500">CNPJ</span>
                <p className="text-sm font-medium">{preview.supplier.taxId}</p>
              </div>
              <div>
                <span className="text-xs text-slate-500">Nome Fantasia</span>
                <p className="text-sm font-medium">{preview.supplier.tradeName ?? '—'}</p>
              </div>
            </div>
          </div>

          {/* Produtos */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-4 flex items-center gap-2">
              <Package className="h-5 w-5 text-primary" />
              <h2 className="text-base font-semibold">
                Produtos ({preview.items.length})
              </h2>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                  <tr>
                    <th className="px-3 py-2 font-medium">Status</th>
                    <th className="px-3 py-2 font-medium">Código</th>
                    <th className="px-3 py-2 font-medium">Descrição</th>
                    <th className="px-3 py-2 font-medium">NCM</th>
                    <th className="px-3 py-2 text-right font-medium">Qtd</th>
                    <th className="px-3 py-2 text-right font-medium">V. Unit.</th>
                    <th className="px-3 py-2 text-right font-medium">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                  {preview.items.map((item, idx) => (
                    <ItemRow key={idx} item={item} />
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Conta a pagar */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-4 flex items-center gap-2">
              <Receipt className="h-5 w-5 text-primary" />
              <h2 className="text-base font-semibold">Conta a Pagar</h2>
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              <div>
                <span className="text-xs text-slate-500">Valor Total</span>
                <p className="text-lg font-semibold">{formatCurrency(preview.payable.value)}</p>
              </div>
              <div>
                <span className="text-xs text-slate-500">Emissão</span>
                <p className="text-sm font-medium">{formatDate(preview.payable.issueDate)}</p>
              </div>
              <div>
                <span className="text-xs text-slate-500">Nota</span>
                <p className="text-sm font-medium">{preview.payable.invoiceNumber}</p>
              </div>
            </div>
            {preview.payable.installments.length > 0 ? (
              <div className="mt-4">
                <span className="text-xs text-slate-500">Parcelas ({preview.payable.installments.length})</span>
                <div className="mt-2 flex flex-wrap gap-2">
                  {preview.payable.installments.map((inst, i) => (
                    <div
                      key={i}
                      className="rounded-lg border border-slate-200 px-3 py-2 text-xs dark:border-slate-700"
                    >
                      <span className="text-slate-500">Venc. {formatDate(inst.dueDate)}</span>
                      <span className="ml-2 font-semibold">{formatCurrency(inst.amount)}</span>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <p className="mt-3 text-xs text-slate-500">Pagamento à vista (sem parcelas).</p>
            )}
          </div>

          {/* Ações */}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={handleReset} disabled={loading}>
              Cancelar
            </Button>
            <Button onClick={handleConfirm} isLoading={loading}>
              <CheckCircle2 className="h-4 w-4" />
              Confirmar importação
            </Button>
          </div>
        </div>
      ) : null}

      {/* Passo 3: Sucesso */}
      {step === 'success' && result ? (
        <div className="space-y-6">
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-8 text-center dark:border-emerald-800 dark:bg-emerald-900/20">
            <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-600 dark:text-emerald-400" />
            <h2 className="mt-4 text-lg font-semibold text-emerald-800 dark:text-emerald-300">
              NF-e importada com sucesso!
            </h2>
            <p className="mt-1 text-sm text-emerald-700 dark:text-emerald-400">
              Nota {result.invoiceNumber} — todos os dados foram cadastrados.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <SummaryCard
              icon={<Building2 className="h-5 w-5 text-primary" />}
              label="Fornecedor"
              value={result.supplierCreated ? 'Criado' : 'Existente'}
              sub={`ID: ${result.supplierId}`}
            />
            <SummaryCard
              icon={<Package className="h-5 w-5 text-primary" />}
              label="Produtos"
              value={`${result.createdProductIds.length} novos`}
              sub={`${result.existingProductIds.length} existentes`}
            />
            <SummaryCard
              icon={<Receipt className="h-5 w-5 text-primary" />}
              label="Conta a Pagar"
              value={`ID: ${result.payableId}`}
              sub="Criada com parcelas"
            />
          </div>

          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={handleReset}>
              Importar outra nota
            </Button>
            <Link to={`/payables/${result.payableId}`}>
              <Button>
                Ver conta a pagar
              </Button>
            </Link>
          </div>
        </div>
      ) : null}

      {/* Erro */}
      {step === 'error' ? (
        <div className="flex justify-center">
          <Button variant="secondary" onClick={handleReset}>
            Tentar novamente
          </Button>
        </div>
      ) : null}
    </div>
  )
}

function ItemRow({ item }: { item: NfeItemData }) {
  const config = STATUS_CONFIG[item.status]
  return (
    <tr>
      <td className="px-3 py-2">
        <Badge tone={config.tone}>{config.label}</Badge>
      </td>
      <td className="px-3 py-2 text-slate-600 dark:text-slate-300">
        {item.code ?? '—'}
      </td>
      <td className="px-3 py-2 text-slate-900 dark:text-slate-100">
        {item.name}
        {item.status === 'DIVERGENTE' ? (
          <AlertTriangle className="ml-1 inline h-3 w-3 text-amber-500" />
        ) : null}
      </td>
      <td className="px-3 py-2 text-slate-600 dark:text-slate-300">{item.ncm}</td>
      <td className="px-3 py-2 text-right text-slate-600 dark:text-slate-300">
        {item.quantity} {item.unit}
      </td>
      <td className="px-3 py-2 text-right text-slate-600 dark:text-slate-300">
        {formatCurrency(item.unitValue)}
      </td>
      <td className="px-3 py-2 text-right font-medium text-slate-900 dark:text-slate-100">
        {formatCurrency(item.totalValue)}
      </td>
    </tr>
  )
}

function SummaryCard({
  icon,
  label,
  value,
  sub,
}: {
  icon: React.ReactNode
  label: string
  value: string
  sub: string
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center gap-2">
        {icon}
        <span className="text-xs uppercase tracking-wide text-slate-500">{label}</span>
      </div>
      <p className="mt-2 text-sm font-semibold">{value}</p>
      <p className="text-xs text-slate-500">{sub}</p>
    </div>
  )
}