import { Select } from '../ui/Select'
import { Input } from '../ui/Input'
import {
  FREIGHT_TYPE_OPTIONS,
  type FreightType,
} from '../../types/quotation'
import type { CarrierResponse } from '../../types/carrier'

export interface FreightConditionsFieldsProps {
  /** Tipo de frete (CIF/FOB). Vazio = não selecionado. */
  freightType: FreightType | ''
  onFreightTypeChange: (value: FreightType | '') => void
  /** Valor do frete em formato livre (string brasileira, ex.: "45,90"). */
  freightValue: string
  onFreightValueChange: (value: string) => void
  /** Normaliza/formata o valor no blur (ex.: "45" → "45,00"). */
  onFreightValueBlur: () => void
  freightValueError?: string | null
  /** ID da transportadora selecionada (opcional). */
  carrierId: number | null
  onCarrierIdChange: (value: number | null) => void
  carriers: CarrierResponse[]
  carriersLoading: boolean
  /** Permite customizar o label do tipo de frete (ex.: "Tipo de entrega"). */
  freightTypeLabel?: string
  /** Permite customizar a dica do tipo de frete. */
  freightTypeHint?: string
}

/**
 * Bloco de condições de frete compartilhado entre os formulários de vendas
 * (Proposta Comercial, Proposta Técnica e Pedido de Vendas).
 *
 * Renderiza, em grid de 3 colunas, ao lado do tipo de frete: Transportadora
 * (dropdown com as transportadoras cadastradas) e Valor do frete (digitação
 * manual). Centraliza a UI que antes era duplicada nos três forms.
 */
export function FreightConditionsFields({
  freightType,
  onFreightTypeChange,
  freightValue,
  onFreightValueChange,
  onFreightValueBlur,
  freightValueError,
  carrierId,
  onCarrierIdChange,
  carriers,
  carriersLoading,
  freightTypeLabel = 'Tipo de frete',
  freightTypeHint = 'CIF = por conta do remetente; FOB = por conta do destinatário.',
}: FreightConditionsFieldsProps) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <Select
        label={freightTypeLabel}
        value={freightType}
        onChange={(e) => onFreightTypeChange(e.target.value as FreightType | '')}
        options={[{ value: '', label: 'Selecione…' }, ...FREIGHT_TYPE_OPTIONS]}
        aria-label={freightTypeLabel}
        hint={freightTypeHint}
      />
      <Select
        label="Transportadora"
        value={carrierId != null ? String(carrierId) : ''}
        onChange={(e) => {
          const v = e.target.value
          onCarrierIdChange(v ? Number(v) : null)
        }}
        options={[
          {
            value: '',
            label: carriersLoading ? 'Carregando…' : 'Selecione…',
          },
          ...carriers.map((c) => ({ value: String(c.id), label: c.name })),
        ]}
        aria-label="Transportadora responsável pelo frete"
      />
      <Input
        label="Valor do frete (R$)"
        type="text"
        inputMode="decimal"
        placeholder="0,00"
        value={freightValue}
        onChange={(e) => onFreightValueChange(e.target.value)}
        onBlur={onFreightValueBlur}
        error={freightValueError ?? undefined}
        hint="Valor de frete informado manualmente."
      />
    </div>
  )
}