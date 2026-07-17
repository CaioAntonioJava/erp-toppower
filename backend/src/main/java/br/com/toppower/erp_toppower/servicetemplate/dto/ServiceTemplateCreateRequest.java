package br.com.toppower.erp_toppower.servicetemplate.dto;

import br.com.toppower.erp_toppower.servicetemplate.enums.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ServiceTemplateCreateRequest", description = "Dados para cadastro de um novo serviço.")
public record ServiceTemplateCreateRequest(

        @Schema(description = "Descrição detalhada do serviço em HTML.", example = "<p>Instalação de quadro elétrico trifásico com disjuntores e DR</p>", maxLength = 20000)
        @Size(max = 20000, message = "Descrição deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Categoria do serviço.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "EXECUÇÃO_SPDA")
        @NotNull(message = "Categoria é obrigatória")
        ServiceCategory category
) {
}
