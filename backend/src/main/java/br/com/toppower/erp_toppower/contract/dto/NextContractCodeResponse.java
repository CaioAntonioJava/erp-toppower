package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta do endpoint {@code GET /contracts/next-code}. Retorna o
 * código formatado previsto para o próximo contrato (ex.:
 * {@code "CT-001-2026"} para Top Power Engenharia ou
 * {@code "CL-001-2026"} para Top Power Materiais).
 */
@Schema(name = "NextContractCodeResponse",
        description = "Próximo código de contrato previsto.")
public record NextContractCodeResponse(

        @Schema(description = "Prefixo do código (ex.: \"CT\" ou \"CL\").",
                example = "CT", requiredMode = Schema.RequiredMode.REQUIRED)
        String prefix,

        @Schema(description = "Numeral sequencial do código (reseta por ano).",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long sequence,

        @Schema(description = "Ano corrente, parte final do código.",
                example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer year,

        @Schema(description = "Código formatado completo (ex.: \"CT-001-2026\").",
                example = "CT-001-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code
) {
}