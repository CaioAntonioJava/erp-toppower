package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Resposta do endpoint {@code GET /contracts/next-code}.
 *
 * <p>Retorna o código formatado previsto para o próximo contrato
 * (ex.: {@code "CL-001-2026"}), com o prefixo lido da Organization ativa
 * e a sequência independente por Organization/ano.</p>
 */
@Schema(name = "NextContractCodeResponse",
        description = "Próximo código de contrato previsto.")
public record NextContractCodeResponse(

        @Schema(description = "Prefixo do código (ex.: \"CL\" ou \"CT\").",
                example = "CL", requiredMode = Schema.RequiredMode.REQUIRED)
        String prefix,

        @Schema(description = "Numeral sequencial do código (reseta por ano).",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long sequence,

        @Schema(description = "Ano corrente, parte final do código.",
                example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer year,

        @Schema(description = "Código formatado completo (ex.: \"CL-001-2026\").",
                example = "CL-001-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Título padrão que seria atribuído ao contrato: "
                + "\"CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>\".",
                example = "CONTRATO DE PRESTAÇÃO DE SERVIÇOS: CL-001-2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String defaultTitle,

        @Schema(description = "Data de vigência padrão (data atual).",
                example = "2026-07-17", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate defaultValidityDate,

        @Schema(description = "Descrição padrão do contrato (template HTML da Organization ativa). "
                + "Pode ser nulo quando a Organization não tiver template configurado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String defaultDescription
) {
}