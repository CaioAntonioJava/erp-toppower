package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Item de serviço de um contrato (response).
 */
@Schema(name = "ContractServiceItemResponse",
        description = "Item de serviço de um contrato.")
public record ContractServiceItemResponse(

        @Schema(description = "Identificador único do item.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição do serviço.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description
) {
}
