package br.com.toppower.erp_toppower.sales.quotation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta do endpoint de "próximo número" de proposta
 * ({@code GET /quotations/next-number}). Apenas o número que seria
 * atribuído na próxima criação — não cria registro.
 */
@Schema(name = "NextQuotationNumberResponse", description = "Próximo número sequencial que será atribuído à próxima proposta.")
public record NextQuotationNumberResponse(

        @Schema(description = "Número gerado, sem prefixo. Primeira proposta do sistema: 1500.",
                example = "1500", requiredMode = Schema.RequiredMode.REQUIRED)
        Long number
) {
}
