import { useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  AlertTriangle,
  CheckCircle2,
  FileUp,
  Package,
  Building2,
  Receipt,
  Loader2,
  Link2,
  Plus,
  Ban,
} from 'lucide-react'
import { BackButton } from '../components/ui/BackButton'
import { Button } from '../components/ui/Button'
import { Alert } from '../components/ui/Alert'
import { Badge } from '../components/ui/Badge'
import { formatCurrency, formatDate } from '../lib/format'
import { previewNfeImport, confirmNfeImport } from '../api/purchase.api'
import { toApiError } from '../lib/errors'
import type {
  ItemAction,
  NfeConfirmItem,
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

const MATCH_REASON_LABEL: Record<string, string> = {
  FORNECEDOR: 'Código do fornecedor',
  EAN: 'Código EAN/GTIN',
  CODIGO: 'Código interno (SKU)',
  NOME: 'Similaridade de nome',
}

/** Ação padrão por status, pré-definida e ajustável pelo usuário. */
function defaultAction(status: NfeItemData['status']): ItemAction {
  switch (status) {
    case 'NOVO':
      return 'CADASTRAR'
    case 'EXISTENTE':
      return 'ESTOQUE'
    case 'DIVERGENTE':
      return 'CADASTRAR'
  }
}

export function PurchaseImportPage() {
  const [step, setStep] = useState<Step>('upload')
  const [preview, setPreview] = useState<NfePreviewResponse | null>(null)
  const [result, setResult] = useState<NfeConfirmResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [fileName, setFileName] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  // Decisões por item: itemIndex -> ação.
  const [actions, setActions] = useState<Record<number, ItemAction>>({})

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
      // Inicializa ações padrão por item.
      const defaults: Record<number, ItemAction> = {}
      for (const item of res.items) {
        defaults[item.itemIndex] = defaultAction(item.status)
      }
      setActions(defaults)
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
      const items: NfeConfirmItem[] = preview.items.map((item) => {
        const action = actions[item.itemIndex] ?? defaultAction(item.status)
        // existingProductId é enviado sempre que a ação for ESTOQUE:
        // - EXISTENTE: ID determinístico do matcher (productId).
        // - DIVERGENTE: candidato sugerido (candidateProductId), editável.
        const existingProductId =
          action === 'ESTOQUE'
            ? item.productId ?? item.candidateProductId ?? null
            : null
        return { itemIndex: item.itemIndex, action, existingProductId }
      })
      const res = await confirmNfeImport({ xmlBase64: preview.xmlBase64, items })
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
    setActions({})
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  function handleDrop(e: React.DragEvent): void {
    e.preventDefault()
    const file = e.dataTransfer.files?.[0]
    if (file && file.name.endsWith('.xml')) {
      void handleUpload(file)
    }
  }

  function setItemAction(itemIndex: number, action: ItemAction): void {
    setActions((prev) => ({ ...prev, [itemIndex]: action }))
  }

  const blockedByDuplicate = preview?.alreadyImported ?? false

  const summary = useMemo(() => {
    if (!preview) return { cadastrar: 0, estoque: 0, ignorar: 0 }
    let cadastrar = 0
    let estoque = 0
    let ignorar = 0
    for (const item of preview.items) {
      const a = actions[item.itemIndex] ?? defaultAction(item.status)
      if (a === 'CADASTRAR') cadastrar++
      else if (a === 'ESTOQUE') estoque++
      else ignorar++
    }
    return { cadastrar, estoque, ignorar }
  }, [preview, actions])

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
          {/* Aviso de duplicidade */}
          {blockedByDuplicate ? (
            <Alert variant="info">
              <strong>Esta NF-e já foi importada.</strong> A Chave de acesso{' '}
              <code className="rounded bg-slate-200 px-1 dark:bg-slate-700">
                {preview.accessKey}
              </code>{' '}
              já consta no sistema. A confirmação da importação está bloqueada.
            </Alert>
          ) : null}

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
            <p className="mt-3 text-xs text-slate-400 dark:text-slate-500">
              O fornecedor é determinado automaticamente pelo CNPJ do emitente no XML e não
              pode ser alterado.
            </p>
          </div>

          {/* Produtos */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="mb-4 flex items-center gap-2">
              <Package className="h-5 w-5 text-primary" />
              <h2 className="text-base font-semibold">
                Produtos ({preview.items.length})
              </h2>
              <div className="ml-auto flex gap-2 text-xs">
                <Badge tone="success">{summary.cadastrar} cadastrar</Badge>
                <Badge tone="info">{summary.estoque} estoque</Badge>
                <Badge tone="neutral">{summary.ignorar} ignorar</Badge>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-800">
                <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-950/40 dark:text-slate-400">
                  <tr>
                    <th className="px-3 py-2 font-medium">Status</th>
                    <th className="px-3 py-2 font-medium">Descrição</th>
                    <th className="px-3 py-2 font-medium">Código</th>
                    <th className="px-3 py-2 font-medium">NCM</th>
                    <th className="px-3 py-2 text-right font-medium">Qtd</th>
                    <th className="px-3 py-2 text-right font-medium">V. Unit.</th>
                    <th className="px-3 py-2 text-right font-medium">Total</th>
                    <th className="px-3 py-2 font-medium">Ação</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                  {preview.items.map((item, idx) => (
                    <ItemRow
                      key={idx}
                      item={item}
                      action={actions[item.itemIndex] ?? defaultAction(item.status)}
                      onActionChange={(a) => setItemAction(item.itemIndex, a)}
                    />
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
            {preview.payable.accessKey ? (
              <p className="mt-2 text-xs text-slate-500">
                Chave de acesso:{' '}
                <code className="rounded bg-slate-100 px-1 dark:bg-slate-800">
                  {preview.payable.accessKey}
                </code>
              </p>
            ) : null}
            {preview.payable.installments.length > 0 ? (
              <div className="mt-4">
                <span className="text-xs text-slate-500">
                  Parcelas ({preview.payable.installments.length})
                </span>
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
            <Button
              onClick={handleConfirm}
              isLoading={loading}
              disabled={blockedByDuplicate}
            >
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
            {result.accessKey ? (
              <p className="mt-1 text-xs text-emerald-600 dark:text-emerald-500">
                Chave de acesso: {result.accessKey}
              </p>
            ) : null}
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
              sub={`${result.existingProductIds.length} com entrada · ${result.ignoredItemCount} ignorados`}
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
              <Button>Ver conta a pagar</Button>
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

function ItemRow({
  item,
  action,
  onActionChange,
}: {
  item: NfeItemData
  action: ItemAction
  onActionChange: (a: ItemAction) => void
}) {
  const config = STATUS_CONFIG[item.status]
  return (
    <tr>
      <td className="px-3 py-2 align-top">
        <Badge tone={config.tone}>{config.label}</Badge>
        {item.matchReason ? (
          <p className="mt-1 text-[10px] text-slate-400 dark:text-slate-500">
            {MATCH_REASON_LABEL[item.matchReason] ?? item.matchReason}
          </p>
        ) : null}
      </td>
      <td className="px-3 py-2 align-top text-slate-900 dark:text-slate-100">
        <div>{item.name}</div>
        {item.status === 'DIVERGENTE' && item.existingProductName ? (
          <div className="mt-1 flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400">
            <AlertTriangle className="h-3 w-3" />
            <span>
              Similar a: <strong>{item.existingProductName}</strong>
            </span>
          </div>
        ) : null}
      </td>
      <td className="px-3 py-2 align-top text-slate-600 dark:text-slate-300">
        {item.code ?? '—'}
        {item.codigoBarras ? (
          <div className="text-[10px] text-slate-400">EAN: {item.codigoBarras}</div>
        ) : null}
      </td>
      <td className="px-3 py-2 align-top text-slate-600 dark:text-slate-300">{item.ncm}</td>
      <td className="px-3 py-2 align-top text-right text-slate-600 dark:text-slate-300">
        {item.quantity} {item.unit}
      </td>
      <td className="px-3 py-2 align-top text-right text-slate-600 dark:text-slate-300">
        {formatCurrency(item.unitValue)}
      </td>
      <td className="px-3 py-2 align-top text-right font-medium text-slate-900 dark:text-slate-100">
        {formatCurrency(item.totalValue)}
      </td>
      <td className="px-3 py-2 align-top">
        <ItemActionControl item={item} action={action} onActionChange={onActionChange} />
      </td>
    </tr>
  )
}

function ItemActionControl({
  item,
  action,
  onActionChange,
}: {
  item: NfeItemData
  action: ItemAction
  onActionChange: (a: ItemAction) => void
}) {
  // NOVO: checkbox "Cadastrar" (marcado) / desmarcar = IGNORAR.
  if (item.status === 'NOVO') {
    return (
      <label className="flex items-center gap-2 text-xs">
        <input
          type="checkbox"
          checked={action === 'CADASTRAR'}
          onChange={(e) => onActionChange(e.target.checked ? 'CADASTRAR' : 'IGNORAR')}
          className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary dark:border-slate-600"
        />
        <span className="flex items-center gap-1">
          <Plus className="h-3 w-3" /> Cadastrar
        </span>
      </label>
    )
  }

  // EXISTENTE: checkbox "Atualizar estoque" (marcado) / desmarcar = IGNORAR.
  if (item.status === 'EXISTENTE') {
    return (
      <label className="flex items-center gap-2 text-xs">
        <input
          type="checkbox"
          checked={action === 'ESTOQUE'}
          onChange={(e) => onActionChange(e.target.checked ? 'ESTOQUE' : 'IGNORAR')}
          className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary dark:border-slate-600"
        />
        <span className="flex items-center gap-1">
          <Package className="h-3 w-3" /> Entrada de estoque
        </span>
      </label>
    )
  }

  // DIVERGENTE: 3 opções (Cadastrar novo | Vincular ao existente | Ignorar).
  return (
    <div className="flex flex-col gap-1 text-xs">
      <label className="flex items-center gap-1.5">
        <input
          type="radio"
          name={`item-${item.itemIndex}`}
          checked={action === 'CADASTRAR'}
          onChange={() => onActionChange('CADASTRAR')}
          className="h-3.5 w-3.5 border-slate-300 text-primary focus:ring-primary dark:border-slate-600"
        />
        <span className="flex items-center gap-1">
          <Plus className="h-3 w-3" /> Cadastrar novo
        </span>
      </label>
      <label className="flex items-center gap-1.5">
        <input
          type="radio"
          name={`item-${item.itemIndex}`}
          checked={action === 'ESTOQUE'}
          onChange={() => onActionChange('ESTOQUE')}
          className="h-3.5 w-3.5 border-slate-300 text-primary focus:ring-primary dark:border-slate-600"
        />
        <span className="flex items-center gap-1">
          <Link2 className="h-3 w-3" /> Vincular ao existente
        </span>
      </label>
      <label className="flex items-center gap-1.5">
        <input
          type="radio"
          name={`item-${item.itemIndex}`}
          checked={action === 'IGNORAR'}
          onChange={() => onActionChange('IGNORAR')}
          className="h-3.5 w-3.5 border-slate-300 text-primary focus:ring-primary dark:border-slate-600"
        />
        <span className="flex items-center gap-1">
          <Ban className="h-3 w-3" /> Ignorar
        </span>
      </label>
    </div>
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