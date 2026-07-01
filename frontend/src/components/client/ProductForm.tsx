import { useState, type FormEvent } from 'react'
import { Save } from 'lucide-react'
import type {
  ProductCreateRequest,
  ProductResponse,
  ProductStatus,
  ProductUpdateRequest,
  UnitType,
} from '../../types/product'
import { UNIT_TYPE_OPTIONS } from '../../types/product'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Button } from '../ui/Button'
import { Alert } from '../ui/Alert'
import { toApiError } from '../../lib/errors'
import { useFieldTouched } from '../../hooks/useFieldTouched'

interface ProductFormProps {
  /** Produto existente (modo edição). Quando omitido, é cadastro novo. */
  product?: ProductResponse
  isLoading?: boolean
  onSaveCreate: (payload: ProductCreateRequest) => Promise<void>
  onSaveUpdate: (payload: ProductUpdateRequest) => Promise<void>
}

// Mesmo regex do backend (ProductCreateRequest#code).
const CODE_PATTERN = /^[A-Za-z0-9._-]+$/

/** Converte a string do input em número, ou `null` se vazia/inválida. */
function parseNumber(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  // Aceita vírgula como separador decimal.
  const normalized = trimmed.replace(',', '.')
  const n = Number(normalized)
  return Number.isFinite(n) ? n : null
}

export function ProductForm({
  product,
  isLoading = false,
  onSaveCreate,
  onSaveUpdate,
}: ProductFormProps) {
  const isEdit = !!product
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  // Erros só são exibidos após o usuário tocar no campo ou tentar submeter.
  const {
    shouldShowError,
    getBlurHandler,
    markAllTouched,
    reset,
  } = useFieldTouched()

  // Campos.
  // `code` é mutável no backend (validado por unicidade no PATCH), então
  // não é `disabled` no modo edição — apenas no cadastro inicial se
  // quisermos forçar, mas mantemos editável para casar com o backend.
  const [code, setCode] = useState(product?.code ?? '')
  const [name, setName] = useState(product?.name ?? '')
  const [unitType, setUnitType] = useState<UnitType>(
    product?.unitType ?? 'UNIDADE',
  )
  const [price, setPrice] = useState<string>(
    product?.price != null ? String(product.price) : '',
  )
  const [stockQuantity, setStockQuantity] = useState<string>(
    product?.stockQuantity != null ? String(product.stockQuantity) : '',
  )
  const [status, setStatus] = useState<ProductStatus>(
    product?.status ?? 'ATIVO',
  )

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    if (!code.trim()) {
      errs.code = 'Código é obrigatório.'
    } else if (code.length > 50) {
      errs.code = 'Código deve ter no máximo 50 caracteres.'
    } else if (!CODE_PATTERN.test(code)) {
      errs.code =
        'Código aceita apenas letras, números, ponto, underline e hífen.'
    }

    if (!name.trim()) {
      errs.name = 'Nome é obrigatório.'
    } else if (name.length > 150) {
      errs.name = 'Nome deve ter no máximo 150 caracteres.'
    }

    const priceNum = parseNumber(price)
    if (price === '' || priceNum === null) {
      errs.price = 'Preço é obrigatório.'
    } else if (priceNum <= 0) {
      errs.price = 'Preço deve ser maior que zero.'
    }

    const stockNum = parseNumber(stockQuantity)
    if (stockQuantity === '' || stockNum === null) {
      errs.stockQuantity = 'Estoque é obrigatório.'
    } else if (stockNum < 0) {
      errs.stockQuantity = 'Estoque não pode ser negativo.'
    }

    setFieldErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
    // Revela os erros de todos os campos no submit, mesmo os ainda não tocados.
    markAllTouched()
    if (!validateAll()) return

    const priceNum = parseNumber(price) ?? 0
    const stockNum = parseNumber(stockQuantity) ?? 0

    try {
      if (isEdit && product) {
        const payload: ProductUpdateRequest = {
          name: name.trim(),
          code: code.trim(),
          unitType,
          price: priceNum,
          stockQuantity: stockNum,
          status,
        }
        await onSaveUpdate(payload)
        setSuccess('Produto atualizado com sucesso!')
        reset()
      } else {
        const payload: ProductCreateRequest = {
          name: name.trim(),
          code: code.trim(),
          unitType,
          price: priceNum,
          stockQuantity: stockNum,
          status,
        }
        await onSaveCreate(payload)
        setSuccess('Produto criado com sucesso!')
        reset()
      }
    } catch (err) {
      const apiErr = toApiError(err)
      setFormError(apiErr.message)
      if (apiErr.fieldErrors) {
        setFieldErrors(apiErr.fieldErrors)
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6" noValidate>
      {formError ? <Alert variant="error">{formError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}

      {/* Identificação */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Identificação</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Código e nome do produto. O código (SKU) deve ser único.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Código (SKU)"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            onBlur={getBlurHandler('code')}
            error={shouldShowError('code', fieldErrors.code)}
            required
            maxLength={50}
          
          />
          <Input
            label="Nome"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onBlur={getBlurHandler('name')}
            error={shouldShowError('name', fieldErrors.name)}
            required
            maxLength={150}
          
          />
        </div>
      </section>

      {/* Detalhes */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Detalhes</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Unidade de medida, preço e estoque do produto.
        </p>

        <div className="grid gap-4 sm:grid-cols-3">
          <Select
            label="Unidade de medida"
            value={unitType}
            onChange={(e) => setUnitType(e.target.value as UnitType)}
            onBlur={getBlurHandler('unitType')}
            error={shouldShowError('unitType', fieldErrors.unitType)}
            required
            options={UNIT_TYPE_OPTIONS}
            aria-label="Unidade de medida"
          />
          <Input
            label="Preço (R$)"
            type="number"
            inputMode="decimal"
            step="0.01"
            min={0.01}
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            onBlur={getBlurHandler('price')}
            error={shouldShowError('price', fieldErrors.price)}
            required
            hint="Preço de venda por unidade."
          />
          <Input
            label="Estoque"
            type="number"
            inputMode="decimal"
            step="0.0001"
            min={0}
            value={stockQuantity}
            onChange={(e) => setStockQuantity(e.target.value)}
            onBlur={getBlurHandler('stockQuantity')}
            error={shouldShowError(
              'stockQuantity',
              fieldErrors.stockQuantity,
            )}
            required
            hint="Permite fracionamento (4 casas) para METROS/BOBINA."
          />
        </div>
      </section>

      {/* Status */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Status</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Define se o produto pode ser usado em novos pedidos. Produtos
          inativos continuam no cadastro para fins de histórico.
        </p>
        <div className="flex gap-2">
          {(['ATIVO', 'INATIVO'] as ProductStatus[]).map((s) => (
            <button
              type="button"
              key={s}
              onClick={() => setStatus(s)}
              className={[
                'inline-flex h-10 items-center rounded-lg border px-3 text-sm font-medium transition-colors',
                status === s
                  ? 'border-primary bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-200'
                  : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800',
              ].join(' ')}
            >
              {s}
            </button>
          ))}
        </div>
      </section>

      <div className="flex justify-end">
        <Button type="submit" isLoading={isLoading} size="lg">
          <Save className="h-4 w-4" />
          {isEdit ? 'Salvar alterações' : 'Cadastrar produto'}
        </Button>
      </div>
    </form>
  )
}