package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumo de uma proposta técnica usado na listagem paginada. Não traz
 * os itens individuais — apenas os totais calculados.
 */
@Schema(name = "TechnicalProposalSummaryResponse",
        description = "Resumo de uma proposta técnica para listagem paginada.")
public record TechnicalProposalSummaryResponse(

        @Schema(description = "Identificador único (UUID) da proposta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Código formatado completo (ex.: \"PL-001-2026\").",
                example = "PL-001-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Tipo de cliente referenciado pela proposta.",
                allowableValues = {"CUSTOMER", "COMPANY"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        TechnicalProposalResponse.ClientType clientType,

        @Schema(description = "UUID do cliente (PF ou PJ).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long clientUuid,

        @Schema(description = "Nome de exibição do cliente (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientName,

        @Schema(description = "Código interno do cliente (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientCode,

        @Schema(description = "Status atual da proposta.",
                allowableValues = {"ABERTA", "EM_ANDAMENTO", "CONCLUIDA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        TechnicalProposalStatus status,

        @Schema(description = "Data da proposta (data de emissão).", example = "2026-07-14",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate,

        @Schema(description = "Data de entrega (preenchida ao concluir).",
                example = "2026-07-15", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate deliveryDate,

        @Schema(description = "Total final da proposta.",
                example = "649.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal total,

        @Schema(description = "Preço geral da proposta, de preenchimento livre pelo usuário. "
                + "Não participa de cálculos automáticos.",
                example = "1500.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal generalPrice,

        @Schema(description = "Condição de pagamento.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition
) {
}