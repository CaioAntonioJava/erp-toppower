package br.com.toppower.erp_toppower.carrier.dto;

import br.com.toppower.erp_toppower.carrier.enums.CarrierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial da transportadora (PATCH). Todos os campos são opcionais.
 */
@Schema(name = "CarrierUpdateRequest", description = "Dados para atualização parcial de uma transportadora (PATCH).")
public record CarrierUpdateRequest(

        @Schema(description = "Novo nome da transportadora.", maxLength = 200)
        @Size(max = 200, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        CarrierStatus status
) {
}