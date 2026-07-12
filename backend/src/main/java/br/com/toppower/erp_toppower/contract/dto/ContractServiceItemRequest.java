package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Item de serviço de um contrato (request).
 *
 * <p>Diferente da Proposta Técnica, este item possui apenas descrição —
 * sem preço, sem margem de lucro.</p>
 */
@Schema(name = "ContractServiceItemRequest",
        description = "Item de serviço de um contrato (apenas descrição).")
public record ContractServiceItemRequest(

        @Schema(description = "Descrição do serviço.",
                example = "Manutenção preventiva de equipamentos elétricos",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Descrição do serviço é obrigatória")
        @Size(max = 2000, message = "Descrição do serviço deve ter no máximo 2000 caracteres")
        String description
) {
}
