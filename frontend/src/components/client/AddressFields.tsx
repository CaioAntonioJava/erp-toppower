import { useState, type FocusEvent } from 'react'
import type { Address } from '../../types/company'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'
import { BRAZILIAN_STATES } from '../../lib/brazilianStates'
import { maskZipCode } from '../../lib/documents'

interface AddressFieldsProps {
  value: Address
  onChange: (next: Address) => void
  errors?: Partial<Record<keyof Address, string>>
  disabled?: boolean
  /**
   * Quando true, força a exibição de todos os erros independente do
   * estado "tocado" de cada campo. Usado no submit do formulário pai.
   */
  forceShowErrors?: boolean
}

const UF_OPTIONS = BRAZILIAN_STATES.map((s) => ({
  value: s.uf,
  label: `${s.uf} — ${s.name}`,
}))

// IDs internos de cada campo, usados para rastrear o estado "tocado".
type AddressFieldKey =
  | 'street'
  | 'number'
  | 'complement'
  | 'neighborhood'
  | 'city'
  | 'zipCode'
  | 'state'

/** Subformulário para os dados de endereço do cliente. */
export function AddressFields({
  value,
  onChange,
  errors = {},
  disabled = false,
  forceShowErrors = false,
}: AddressFieldsProps) {
  // Estado de campos tocados (focados e desfocados ao menos uma vez).
  const [touched, setTouched] = useState<ReadonlySet<AddressFieldKey>>(
    () => new Set(),
  )

  function markTouched(field: AddressFieldKey) {
    setTouched((prev) => {
      if (prev.has(field)) return prev
      const next = new Set(prev)
      next.add(field)
      return next
    })
  }

  function onBlurField(field: AddressFieldKey) {
    return (_e: FocusEvent<HTMLInputElement | HTMLSelectElement>) => {
      markTouched(field)
    }
  }

  /**
   * Mostra o erro apenas se o campo foi tocado OU se o pai já tentou
   * submeter (`forceShowErrors`). Caso contrário devolve undefined.
   */
  function showError(
    field: AddressFieldKey,
    error: string | undefined,
  ): string | undefined {
    if (!error) return undefined
    if (forceShowErrors || touched.has(field)) return error
    return undefined
  }

  function patch<K extends keyof Address>(key: K, val: Address[K]) {
    onChange({ ...value, [key]: val })
  }

  return (
    // Grid 12-cols otimizado para desktop (max-w-7xl = 1280px no AppLayout).
    // Layout em 3 linhas com campos de texto longo em destaque:
    //   Linha 1: Logradouro (6/12) + Número (2/12) + Complemento (4/12) —
    //            logradouro ganha metade da linha para endereços longos.
    //   Linha 2: Bairro (4/12) + Cidade (5/12) + CEP (3/12) — bairro e
    //            cidade recebem mais espaço para nomes completos.
    //   Linha 3: UF (2/12) — para manter o select compacto na última linha.
    <div className="grid gap-4 sm:grid-cols-12 lg:gap-5">
      {/* Linha 1 — Logradouro (largo) + Número + Complemento */}
      <Input
        label="Logradouro"
        value={value.street}
        onChange={(e) => patch('street', e.target.value)}
        onBlur={onBlurField('street')}
        error={showError('street', errors.street)}
        disabled={disabled}
        required
        className="sm:col-span-6"
      />
      <Input
        label="Número"
        value={value.number}
        onChange={(e) => patch('number', e.target.value)}
        onBlur={onBlurField('number')}
        error={showError('number', errors.number)}
        disabled={disabled}
        required
        className="sm:col-span-2"
      />
      <Input
        label="Complemento"
        value={value.complement ?? ''}
        onChange={(e) => patch('complement', e.target.value)}
        onBlur={onBlurField('complement')}
        error={showError('complement', errors.complement)}
        disabled={disabled}
        className="sm:col-span-4"
      />

      {/* Linha 2 — Bairro + Cidade + CEP */}
      <Input
        label="Bairro"
        value={value.neighborhood ?? ''}
        onChange={(e) => patch('neighborhood', e.target.value)}
        onBlur={onBlurField('neighborhood')}
        error={showError('neighborhood', errors.neighborhood)}
        disabled={disabled}
        className="sm:col-span-4"
      />
      <Input
        label="Cidade"
        value={value.city}
        onChange={(e) => patch('city', e.target.value)}
        onBlur={onBlurField('city')}
        error={showError('city', errors.city)}
        disabled={disabled}
        required
        className="sm:col-span-5"
      />
      <Input
        label="CEP"
        value={value.zipCode}
        onChange={(e) => patch('zipCode', maskZipCode(e.target.value))}
        onBlur={onBlurField('zipCode')}
        error={showError('zipCode', errors.zipCode)}
        disabled={disabled}
        required
        maxLength={9}
        className="sm:col-span-3"
      />

      {/* Linha 3 — UF */}
      <Select
        label="UF"
        value={value.state}
        onChange={(e) => patch('state', e.target.value.toUpperCase())}
        onBlur={onBlurField('state')}
        error={showError('state', errors.state)}
        disabled={disabled}
        required
        options={UF_OPTIONS}
        placeholder="UF"
        className="sm:col-span-2"
      />
    </div>
  )
}