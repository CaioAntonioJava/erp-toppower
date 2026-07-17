package br.com.toppower.erp_toppower.servicetemplate.dto;

import br.com.toppower.erp_toppower.servicetemplate.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial do serviço (PATCH). Todos os campos são opcionais.
 */
@Schema(name = "ServiceTemplateUpdateRequest", description = "Dados para atualização parcial de um serviço (PATCH).")
public record ServiceTemplateUpdateRequest(

        @Schema(description = "Nova descrição do serviço em HTML.", maxLength = 20000)
        @Size(max = 20000, message = "Descrição deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Nova categoria do serviço.", example = "EXECUÇÃO_SPDA")
        ServiceCategory category
) {
}
