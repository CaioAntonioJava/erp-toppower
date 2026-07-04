package br.com.toppower.erp_toppower.carrier.dto;

import br.com.toppower.erp_toppower.carrier.enums.CarrierName;
import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/**
 * Dados para cadastro de uma nova transportadora.
 *
 * <p>Todos os campos são opcionais, conforme regra de negócio: uma
 * transportadora pode ser cadastrada apenas com o nome (enum
 * {@link CarrierName}), apenas com o valor de frete, ou com ambos.
 * O {@code status}, quando omitido, assume {@code ATIVO} via
 * {@code @PrePersist} da entidade.</p>
 */
@Schema(name = "CarrierCreateRequest", description = "Dados para cadastro de uma nova transportadora.")
public record CarrierCreateRequest(

        @Schema(description = "Nome padronizado da transportadora. Opcional.",
                example = "CORREIOS_SEDEX",
                allowableValues = {"CORREIOS_SEDEX", "CORREIOS_PAC", "JADLOG", "OUTRAS_TRANSPORTADORAS"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        CarrierName carrierName,

        @Schema(description = "Valor padrão do frete. Opcional. Mínimo 0.00, até 2 casas decimais.",
                example = "150.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                minimum = "0.0", nullable = true)
        @DecimalMin(value = "0.00", message = "Valor do frete deve ser no mínimo 0")
        @Digits(integer = 8, fraction = 2, message = "Valor do frete deve ter no máximo 2 casas decimais")
        BigDecimal freightValue,

        @Schema(description = "Status inicial da transportadora. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        CarrierStatus status
) {
}
