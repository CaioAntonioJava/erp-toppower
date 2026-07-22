package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dados da conta a pagar extraídos da NF-e.
 */
@Schema(name = "NfePayableData",
        description = "Dados da conta a pagar gerada a partir da NF-e.")
public record NfePayableData(

        @Schema(description = "Valor total da nota (vNF).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value,

        @Schema(description = "Data de emissão (dhEmi).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate issueDate,

        @Schema(description = "Descrição da conta a pagar.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Número da NF-e (nNF/série) para idempotência.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String invoiceNumber,

        @Schema(description = "Chave de acesso da NF-e.")
        String accessKey,

        @Schema(description = "Parcelas/duplicatas. Vazia se à vista.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<NfeInstallmentData> installments
) {
}