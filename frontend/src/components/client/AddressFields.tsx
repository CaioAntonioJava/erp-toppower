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
}

const UF_OPTIONS = BRAZILIAN_STATES.map((s) => ({
  value: s.uf,
  label: `${s.uf} — ${s.name}`,
}))

/** Subformulário para os dados de endereço do cliente. */
export function AddressFields({
  value,
  onChange,
  errors = {},
  disabled = false,
}: AddressFieldsProps) {
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
        error={errors.street}
        disabled={disabled}
        required
        placeholder="Av. Paulista"
        className="sm:col-span-6"
      />
      <Input
        label="Número"
        value={value.number}
        onChange={(e) => patch('number', e.target.value)}
        error={errors.number}
        disabled={disabled}
        required
        placeholder="1000 ou S/N"
        className="sm:col-span-2"
      />
      <Input
        label="Complemento"
        value={value.complement ?? ''}
        onChange={(e) => patch('complement', e.target.value)}
        error={errors.complement}
        disabled={disabled}
        placeholder="Apto 101, Bloco B"
        className="sm:col-span-4"
      />

      {/* Linha 2 — Bairro + Cidade + CEP */}
      <Input
        label="Bairro"
        value={value.neighborhood ?? ''}
        onChange={(e) => patch('neighborhood', e.target.value)}
        error={errors.neighborhood}
        disabled={disabled}
        placeholder="Bela Vista"
        className="sm:col-span-4"
      />
      <Input
        label="Cidade"
        value={value.city}
        onChange={(e) => patch('city', e.target.value)}
        error={errors.city}
        disabled={disabled}
        required
        placeholder="São Paulo"
        className="sm:col-span-5"
      />
      <Input
        label="CEP"
        value={value.zipCode}
        onChange={(e) => patch('zipCode', maskZipCode(e.target.value))}
        error={errors.zipCode}
        disabled={disabled}
        required
        maxLength={9}
        placeholder="01310-100"
        className="sm:col-span-3"
      />

      {/* Linha 3 — UF */}
      <Select
        label="UF"
        value={value.state}
        onChange={(e) => patch('state', e.target.value.toUpperCase())}
        error={errors.state}
        disabled={disabled}
        required
        options={UF_OPTIONS}
        placeholder="UF"
        className="sm:col-span-2"
      />
    </div>
  )
}
