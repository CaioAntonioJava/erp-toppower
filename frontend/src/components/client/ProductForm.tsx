import { useState, type FormEvent } from 'react'
import type {
  OrigemProduto,
  ProductCreateRequest,
  ProductResponse,
  ProductStatus,
  ProductUpdateRequest,
  UnitType,
} from '../../types/product'
import {
  CSOSN_OPTIONS,
  CST_COFINS_OPTIONS,
  CST_IPI_OPTIONS,
  CST_PIS_OPTIONS,
  ORIGEM_OPTIONS,
  UNIT_TYPE_OPTIONS,
} from '../../types/product'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { Alert } from '../ui/Alert'
import { toApiError } from '../../lib/errors'
import { useFieldTouched } from '../../hooks/useFieldTouched'

interface ProductFormProps {
  /** Produto existente (modo edição). Quando omitido, é cadastro novo. */
  product?: ProductResponse
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

/**
 * Completa os centavos de um valor monetário digitado: se o usuário não
 * informou a parte decimal, anexa ",00" (ex.: "5" → "5,00"). Caso termine
 * com um separador sem casas (ex.: "5,"), anexa "00". Os demais casos são
 * devolvidos inalterados, respeitando o que o usuário digitou.
 */
function fillCents(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return trimmed
  if (!/[.,]/.test(trimmed)) return `${trimmed},00`
  if (/[.,]$/.test(trimmed)) return `${trimmed}00`
  return trimmed
}

/** Remove qualquer não-dígito — usado para normalizar códigos fiscais (NCM, CEST, etc.). */
function onlyDigits(value: string): string {
  return value.replace(/\D+/g, '')
}

export function ProductForm({
  product,
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
    product?.price != null ? fillCents(String(product.price)) : '',
  )
  const [stockQuantity, setStockQuantity] = useState<string>(
    product?.stockQuantity != null ? String(product.stockQuantity) : '',
  )
  const [status, setStatus] = useState<ProductStatus>(
    product?.status ?? 'ATIVO',
  )

  // Campos fiscais (Simples Nacional).
  const [ncm, setNcm] = useState(product?.ncm ?? '')
  const [origem, setOrigem] = useState<OrigemProduto>(
    product?.origem ?? 'NACIONAL',
  )
  const [codigoBarras, setCodigoBarras] = useState(product?.codigoBarras ?? '')
  const [cest, setCest] = useState(product?.cest ?? '')
  const [exTipi, setExTipi] = useState(product?.exTipi ?? '')
  const [pesoLiquido, setPesoLiquido] = useState<string>(
    product?.pesoLiquido != null ? String(product.pesoLiquido) : '',
  )
  const [pesoBruto, setPesoBruto] = useState<string>(
    product?.pesoBruto != null ? String(product.pesoBruto) : '',
  )
  const [csosn, setCsosn] = useState<string>(product?.csosn ?? '102')
  const [aliquotaIcmsSt, setAliquotaIcmsSt] = useState<string>(
    product?.aliquotaIcmsSt != null ? String(product.aliquotaIcmsSt) : '',
  )
  const [mvaSt, setMvaSt] = useState<string>(
    product?.mvaSt != null ? String(product.mvaSt) : '',
  )
  const [cstIpi, setCstIpi] = useState<string>(product?.cstIpi ?? '99')
  const [classeEnqIpi, setClasseEnqIpi] = useState(product?.classeEnqIpi ?? '')
  const [cstPis, setCstPis] = useState<string>(product?.cstPis ?? '49')
  const [cstCofins, setCstCofins] = useState<string>(product?.cstCofins ?? '49')

  function validateAll(): boolean {
    const errs: Record<string, string> = {}

    // code (SKU) é opcional no backend. Validamos formato/tamanho apenas
    // quando o usuário preencheu o campo — string vazia é tratada como
    // "sem SKU".
    const trimmedCode = code.trim()
    if (trimmedCode.length > 0) {
      if (trimmedCode.length > 50) {
        errs.code = 'Código deve ter no máximo 50 caracteres.'
      } else if (!CODE_PATTERN.test(trimmedCode)) {
        errs.code =
          'Código aceita apenas letras, números, ponto, underline e hífen.'
      }
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

    // NCM — obrigatório, 8 dígitos.
    const ncmDigits = onlyDigits(ncm)
    if (ncmDigits.length === 0) {
      errs.ncm = 'NCM é obrigatório.'
    } else if (ncmDigits.length !== 8) {
      errs.ncm = 'NCM deve ter exatamente 8 dígitos numéricos.'
    }

    // Códigos fiscais opcionais: validam apenas quando preenchidos.
    if (codigoBarras.trim().length > 0) {
      const digits = onlyDigits(codigoBarras)
      if (digits.length < 8 || digits.length > 14) {
        errs.codigoBarras = 'GTIN deve ter entre 8 e 14 dígitos numéricos.'
      }
    }
    if (cest.trim().length > 0 && onlyDigits(cest).length !== 7) {
      errs.cest = 'CEST deve ter exatamente 7 dígitos numéricos.'
    }
    if (exTipi.trim().length > 0 && onlyDigits(exTipi).length !== 2) {
      errs.exTipi = 'EX TIPI deve ter exatamente 2 dígitos numéricos.'
    }
    if (classeEnqIpi.trim().length > 0 && onlyDigits(classeEnqIpi).length !== 5) {
      errs.classeEnqIpi =
        'Classe de enquadramento do IPI deve ter 5 dígitos numéricos.'
    }
    if (csosn.trim().length > 0 && onlyDigits(csosn).length !== 3) {
      errs.csosn = 'CSOSN deve ter exatamente 3 dígitos numéricos.'
    }
    if (cstIpi.trim().length > 0 && onlyDigits(cstIpi).length !== 2) {
      errs.cstIpi = 'CST do IPI deve ter exatamente 2 dígitos numéricos.'
    }
    if (cstPis.trim().length > 0 && onlyDigits(cstPis).length !== 2) {
      errs.cstPis = 'CST do PIS deve ter exatamente 2 dígitos numéricos.'
    }
    if (cstCofins.trim().length > 0 && onlyDigits(cstCofins).length !== 2) {
      errs.cstCofins = 'CST do COFINS deve ter exatamente 2 dígitos numéricos.'
    }

    // Pesos e alíquotas: >= 0 quando preenchidos.
    const pesoLiqNum = parseNumber(pesoLiquido)
    if (pesoLiquido.trim().length > 0 && (pesoLiqNum === null || pesoLiqNum < 0)) {
      errs.pesoLiquido = 'Peso líquido deve ser um número não negativo.'
    }
    const pesoBrutoNum = parseNumber(pesoBruto)
    if (pesoBruto.trim().length > 0 && (pesoBrutoNum === null || pesoBrutoNum < 0)) {
      errs.pesoBruto = 'Peso bruto deve ser um número não negativo.'
    }
    const aliquotaStNum = parseNumber(aliquotaIcmsSt)
    if (aliquotaIcmsSt.trim().length > 0 && (aliquotaStNum === null || aliquotaStNum < 0)) {
      errs.aliquotaIcmsSt = 'Alíquota do ICMS-ST deve ser um número não negativo.'
    }
    const mvaNum = parseNumber(mvaSt)
    if (mvaSt.trim().length > 0 && (mvaNum === null || mvaNum < 0)) {
      errs.mvaSt = 'MVA deve ser um número não negativo.'
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
    const ncmDigits = onlyDigits(ncm)

    // Helper: se o campo opcional estiver vazio, não vai no payload.
    const optionalText = (v: string): string | undefined =>
      v.trim() ? onlyDigits(v) : undefined
    const optionalRaw = (v: string): string | undefined =>
      v.trim() ? v.trim() : undefined
    const optionalNum = (v: string): number | undefined => {
      if (!v.trim()) return undefined
      const n = parseNumber(v)
      return n === null ? undefined : n
    }

    try {
      // code (SKU) é opcional: só enviamos quando há valor. Enviá-lo
      // como string vazia quebraria o `@Pattern` do backend.
      const trimmedCode = code.trim()
      const codeField = trimmedCode ? { code: trimmedCode } : {}

      // Espalha um campo no payload apenas quando há valor. Em vez de
      // enviar `undefined` (que o `@Pattern` do backend rejeita quando a
      // chave existe no JSON), omitimos a chave por completo. O genéricico
      // `V` preserva o tipo do valor para o TS não alargar para `string | number`.
      const spreadIf = <K extends string, V extends string | number>(
        key: K,
        value: V | undefined,
      ): Partial<Record<K, V>> =>
        value === undefined ? ({} as Partial<Record<K, V>>) : ({ [key]: value } as Partial<Record<K, V>>)

      if (isEdit && product) {
        const payload: ProductUpdateRequest = {
          name: name.trim(),
          ...codeField,
          unitType,
          price: priceNum,
          stockQuantity: stockNum,
          status,
          // Fiscais — omitir vazios: no PATCH, omitir preserva o valor
          // existente (o backend só sobrescreve campos não-nulos).
          ncm: ncmDigits,
          origem,
          ...spreadIf('codigoBarras', optionalRaw(codigoBarras)),
          ...spreadIf('cest', optionalText(cest)),
          ...spreadIf('exTipi', optionalText(exTipi)),
          ...spreadIf('pesoLiquido', optionalNum(pesoLiquido)),
          ...spreadIf('pesoBruto', optionalNum(pesoBruto)),
          ...spreadIf('csosn', optionalText(csosn)),
          ...spreadIf('aliquotaIcmsSt', optionalNum(aliquotaIcmsSt)),
          ...spreadIf('mvaSt', optionalNum(mvaSt)),
          ...spreadIf('cstIpi', optionalText(cstIpi)),
          ...spreadIf('classeEnqIpi', optionalText(classeEnqIpi)),
          ...spreadIf('cstPis', optionalText(cstPis)),
          ...spreadIf('cstCofins', optionalText(cstCofins)),
        }
        await onSaveUpdate(payload)
        setSuccess('Produto atualizado com sucesso!')
        reset()
      } else {
        const payload: ProductCreateRequest = {
          name: name.trim(),
          ...codeField,
          unitType,
          price: priceNum,
          stockQuantity: stockNum,
          status,
          // Fiscais — campos vazios são omitidos; o backend aplica defaults
          // (Simples Nacional) via @PrePersist quando nulos.
          ncm: ncmDigits,
          origem,
          ...spreadIf('codigoBarras', optionalRaw(codigoBarras)),
          ...spreadIf('cest', optionalText(cest)),
          ...spreadIf('exTipi', optionalText(exTipi)),
          ...spreadIf('pesoLiquido', optionalNum(pesoLiquido)),
          ...spreadIf('pesoBruto', optionalNum(pesoBruto)),
          ...spreadIf('csosn', optionalText(csosn)),
          ...spreadIf('aliquotaIcmsSt', optionalNum(aliquotaIcmsSt)),
          ...spreadIf('mvaSt', optionalNum(mvaSt)),
          ...spreadIf('cstIpi', optionalText(cstIpi)),
          ...spreadIf('classeEnqIpi', optionalText(classeEnqIpi)),
          ...spreadIf('cstPis', optionalText(cstPis)),
          ...spreadIf('cstCofins', optionalText(cstCofins)),
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
    <form
      id="product-form"
      onSubmit={handleSubmit}
      className="flex flex-col gap-6"
      noValidate
    >
      {formError ? <Alert variant="error">{formError}</Alert> : null}
      {success ? <Alert variant="success">{success}</Alert> : null}

      {/* Identificação */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Identificação</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Código (SKU, opcional) e nome do produto. Quando informado, o
          código deve ser único.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Código (SKU)"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            onBlur={getBlurHandler('code')}
            error={shouldShowError('code', fieldErrors.code)}
            maxLength={50}
            hint="Opcional. Use letras, números, ponto, underline ou hífen (até 50 caracteres)."

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
            type="text"
            inputMode="decimal"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            onBlur={() => {
              getBlurHandler('price')()
              setPrice((prev) => fillCents(prev))
            }}
            error={shouldShowError('price', fieldErrors.price)}
            required
            hint="Preço de custo por unidade. Use vírgula para os centavos."
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
          />
        </div>
      </section>

      {/* Fiscal (Simples Nacional) */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h3 className="mb-1 text-base font-semibold">Fiscal (Simples Nacional)</h3>
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Campos usados na emissão de NF-e. O sistema não emite notas, mas
          mantém os dados prontos para integração com emissores fiscais e
          exportação contábil. NCM e origem são obrigatórios; os demais são
          opcionais (defaults do Simples Nacional já preenchidos).
        </p>

        <div className="grid gap-4 sm:grid-cols-3">
          <Input
            label="NCM"
            value={ncm}
            onChange={(e) => setNcm(onlyDigits(e.target.value))}
            onBlur={getBlurHandler('ncm')}
            error={shouldShowError('ncm', fieldErrors.ncm)}
            required
            maxLength={8}
            inputMode="numeric"
            hint="8 dígitos. Nomenclatura Comum do Mercosul."
          />
          <Select
            label="Origem"
            value={origem}
            onChange={(e) => setOrigem(e.target.value as OrigemProduto)}
            onBlur={getBlurHandler('origem')}
            error={shouldShowError('origem', fieldErrors.origem)}
            required
            options={ORIGEM_OPTIONS}
            aria-label="Origem da mercadoria"
          />
          <Input
            label="Código de barras / GTIN"
            value={codigoBarras}
            onChange={(e) => setCodigoBarras(onlyDigits(e.target.value))}
            onBlur={getBlurHandler('codigoBarras')}
            error={shouldShowError('codigoBarras', fieldErrors.codigoBarras)}
            maxLength={14}
            inputMode="numeric"
            hint="Opcional. EAN-13/14 ou GTIN (8 a 14 dígitos)."
          />
          <Input
            label="CEST"
            value={cest}
            onChange={(e) => setCest(onlyDigits(e.target.value))}
            onBlur={getBlurHandler('cest')}
            error={shouldShowError('cest', fieldErrors.cest)}
            maxLength={7}
            inputMode="numeric"
            hint="Opcional. 7 dígitos — substituição tributária."
          />
          <Input
            label="EX TIPI"
            value={exTipi}
            onChange={(e) => setExTipi(onlyDigits(e.target.value))}
            onBlur={getBlurHandler('exTipi')}
            error={shouldShowError('exTipi', fieldErrors.exTipi)}
            maxLength={2}
            inputMode="numeric"
            hint="Opcional. 2 dígitos."
          />
          <Input
            label="Classe enq. IPI"
            value={classeEnqIpi}
            onChange={(e) => setClasseEnqIpi(onlyDigits(e.target.value))}
            onBlur={getBlurHandler('classeEnqIpi')}
            error={shouldShowError('classeEnqIpi', fieldErrors.classeEnqIpi)}
            maxLength={5}
            inputMode="numeric"
            hint="Opcional. 5 dígitos."
          />
          <Input
            label="Peso líquido (kg)"
            type="number"
            inputMode="decimal"
            step="0.0001"
            min={0}
            value={pesoLiquido}
            onChange={(e) => setPesoLiquido(e.target.value)}
            onBlur={getBlurHandler('pesoLiquido')}
            error={shouldShowError('pesoLiquido', fieldErrors.pesoLiquido)}
            hint="Opcional. Usado no transporte da NF-e."
          />
          <Input
            label="Peso bruto (kg)"
            type="number"
            inputMode="decimal"
            step="0.0001"
            min={0}
            value={pesoBruto}
            onChange={(e) => setPesoBruto(e.target.value)}
            onBlur={getBlurHandler('pesoBruto')}
            error={shouldShowError('pesoBruto', fieldErrors.pesoBruto)}
            hint="Opcional. Usado no transporte da NF-e."
          />
          <Select
            label="CSOSN"
            value={csosn}
            onChange={(e) => setCsosn(e.target.value)}
            onBlur={getBlurHandler('csosn')}
            error={shouldShowError('csosn', fieldErrors.csosn)}
            options={CSOSN_OPTIONS}
            hint="Código de Situação no Simples Nacional."
            aria-label="CSOSN"
          />
          <Input
            label="Alíquota ICMS-ST (%)"
            type="number"
            inputMode="decimal"
            step="0.01"
            min={0}
            value={aliquotaIcmsSt}
            onChange={(e) => setAliquotaIcmsSt(e.target.value)}
            onBlur={getBlurHandler('aliquotaIcmsSt')}
            error={shouldShowError('aliquotaIcmsSt', fieldErrors.aliquotaIcmsSt)}
            hint="Opcional. Apenas para produtos com ST."
          />
          <Input
            label="MVA-ST (%)"
            type="number"
            inputMode="decimal"
            step="0.01"
            min={0}
            value={mvaSt}
            onChange={(e) => setMvaSt(e.target.value)}
            onBlur={getBlurHandler('mvaSt')}
            error={shouldShowError('mvaSt', fieldErrors.mvaSt)}
            hint="Opcional. Margem de Valor Adicionado para ST."
          />
          <Select
            label="CST IPI"
            value={cstIpi}
            onChange={(e) => setCstIpi(e.target.value)}
            onBlur={getBlurHandler('cstIpi')}
            error={shouldShowError('cstIpi', fieldErrors.cstIpi)}
            options={CST_IPI_OPTIONS}
            aria-label="CST do IPI"
          />
          <Select
            label="CST PIS"
            value={cstPis}
            onChange={(e) => setCstPis(e.target.value)}
            onBlur={getBlurHandler('cstPis')}
            error={shouldShowError('cstPis', fieldErrors.cstPis)}
            options={CST_PIS_OPTIONS}
            aria-label="CST do PIS"
          />
          <Select
            label="CST COFINS"
            value={cstCofins}
            onChange={(e) => setCstCofins(e.target.value)}
            onBlur={getBlurHandler('cstCofins')}
            error={shouldShowError('cstCofins', fieldErrors.cstCofins)}
            options={CST_COFINS_OPTIONS}
            aria-label="CST do COFINS"
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
    </form>
  )
}