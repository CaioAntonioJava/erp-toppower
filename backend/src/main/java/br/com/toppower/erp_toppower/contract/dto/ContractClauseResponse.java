package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Representação pública de uma cláusula de contrato retornada pela API.
 */
@Schema(name = "ContractClauseResponse", description = "Representação pública de uma cláusula de contrato.")
public record ContractClauseResponse(

        @Schema(description = "Identificador único (ID) da cláusula.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Número da cláusula (1 a 11).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer clauseNumber,

        @Schema(description = "Título da cláusula (ex.: \"DO OBJETO\").",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "Texto completo da cláusula (HTML ou texto puro).")
        String content,

        @Schema(description = "ID do ServiceTemplate referenciado (apenas cláusula 1).")
        Long serviceTemplateId,

        @Schema(description = "Data de criação.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt
) {
}