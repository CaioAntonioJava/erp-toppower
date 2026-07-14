package br.com.toppower.erp_toppower.sales.quotation.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação resumida de uma proposta comercial, usada em listagens
 * paginadas. Não inclui a lista de itens (carregada sob demanda ao
 * abrir o detalhe).
 */
@Schema(name = "QuotationSummaryResponse", description = "Resumo de uma proposta comercial para listagens.")
public record QuotationSummaryResponse(

        @Schema(description = "Identificador único (UUID) da proposta.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Número sequencial da proposta (sem prefixo).", example = "1500",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long number,

        @Schema(description = "Data de emissão.", example = "2026-07-02",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate issueDate,

        @Schema(description = "Tipo do cliente (pessoa física ou jurídica).",
                allowableValues = {"CUSTOMER", "COMPANY"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        QuotationResponse.ClientType clientType,

        @Schema(description = "UUID do cliente referenciado (PF ou PJ).", requiredMode = Schema.RequiredMode.REQUIRED)
        Long clientId,

        @Schema(description = "Nome do cliente (PF ou nome fantasia/razão social da empresa).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String clientName,

        @Schema(description = "Código interno do cliente (ex.: \"CLI000001\", \"EMP000001\"). "
                + "Resolvido no backend a partir do UUID referenciado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientCode,

        @Schema(description = "UUID do vendedor.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long sellerId,

        @Schema(description = "Nome do vendedor (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String sellerName,

        @Schema(description = "Status atual.", allowableValues = {"ATIVA", "CONVERTIDA", "CANCELADA", "EXPIRADA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        QuotationStatus status,

        @Schema(description = "Total de itens (unidades).", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer totalQuantity,

        @Schema(description = "Total final da proposta (após descontos).", example = "1400.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal total,

        @Schema(description = "Condição de pagamento.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition
) {
}
