package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta do endpoint {@code GET /technical-proposals/next-code}.
 * Retorna o código formatado previsto para a próxima proposta técnica
 * (ex.: {@code "PL-001-2026"}).
 */
@Schema(name = "NextTechnicalProposalCodeResponse",
        description = "Próximo código de proposta técnica previsto.")
public record NextTechnicalProposalCodeResponse(

        @Schema(description = "Prefixo do código (ex.: \"PL\").",
                example = "PL", requiredMode = Schema.RequiredMode.REQUIRED)
        String prefix,

        @Schema(description = "Numeral sequencial do código (reseta por ano).",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long sequence,

        @Schema(description = "Ano corrente, parte final do código.",
                example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer year,

        @Schema(description = "Código formatado completo (ex.: \"PL-001-2026\").",
                example = "PL-001-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code
) {
}