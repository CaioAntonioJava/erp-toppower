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
  /** UUID da transportadora selecionada (opcional). */
  carrierUuid: string
  onCarrierUuidChange: (value: string) => void
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
  carrierUuid,
  onCarrierUuidChange,
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
        value={carrierUuid}
        onChange={(e) => onCarrierUuidChange(e.target.value)}
        options={[
          {
            value: '',
            label: carriersLoading ? 'Carregando…' : 'Selecione…',
          },
          ...carriers.map((c) => ({ value: c.uuid, label: c.name })),
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