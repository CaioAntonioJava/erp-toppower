package br.com.toppower.erp_toppower.servicetemplate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ServiceTemplateCreateRequest", description = "Dados para cadastro de um novo serviço.")
public record ServiceTemplateCreateRequest(

        @Schema(description = "Nome do serviço.", example = "INSTALAÇÃO DE QUADRO ELÉTRICO",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 200, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Descrição detalhada do serviço.", example = "Instalação de quadro elétrico trifásico com disjuntores e DR",
                maxLength = 500)
        @Size(max = 500, message = "Descrição deve ter no máximo {max} caracteres")
        String description
) {
}
