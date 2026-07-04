package br.com.toppower.erp_toppower.carrier.dto;

import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Atualização parcial da transportadora (PATCH). Todos os campos são
 * opcionais: envie apenas os campos que deseja alterar.
 */
@Schema(name = "CarrierUpdateRequest", description = "Dados para atualização parcial de uma transportadora (PATCH).")
public record CarrierUpdateRequest(

        @Schema(description = "Novo nome. Será salvo em MAIÚSCULAS.", maxLength = 150)
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Novo valor padrão do frete. Mínimo 0.00, até 2 casas decimais.",
                minimum = "0.0", nullable = true)
        @DecimalMin(value = "0.00", message = "Valor do frete deve ser no mínimo 0")
        @Digits(integer = 8, fraction = 2, message = "Valor do frete deve ter no máximo 2 casas decimais")
        BigDecimal freightValue,

        @Schema(description = "Novo status da transportadora.",
                allowableValues = {"ATIVO", "INATIVO"})
        CarrierStatus status
) {
}
