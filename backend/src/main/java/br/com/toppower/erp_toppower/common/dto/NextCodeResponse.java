package br.com.toppower.erp_toppower.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta do endpoint de "próximo código" ({@code GET /customers/next-code}
 * ou {@code GET /companies/next-code}). Apenas o código que seria atribuído
 * no próximo cadastro — não cria registro.
 */
@Schema(name = "NextCodeResponse", description = "Próximo código sequencial que será atribuído no próximo cadastro.")
public record NextCodeResponse(

        @Schema(description = "Código gerado no formato PREFIXNNNNNN (ex.: CLI000001, EMP000007).",
                example = "CLI000007", requiredMode = Schema.RequiredMode.REQUIRED)
        String code
) {
}