package br.com.toppower.erp_toppower.boleto.dto;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(name = "BoletoResponse", description = "Representação pública de um boleto retornada pela API.")
public record BoletoResponse(

        @Schema(description = "Identificador único (ID) do boleto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Nº da obra/contrato vinculado ao boleto, se houver.")
        String contractWorkNumber,

        @Schema(description = "Nome do responsável pelo boleto, se houver.")
        String responsibleName,

        @Schema(description = "Valor da parcela do boleto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value,

        @Schema(description = "Data de vencimento da parcela.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status atual do boleto.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        RegistrationStatus status,

        @Schema(description = "ID da empresa (fornecedor) vinculada, se aplicável.")
        Long supplierId,

        @Schema(description = "Nome de exibição da empresa (fornecedor) vinculada, se aplicável.")
        String supplierName,

        @Schema(description = "Indica se o boleto foi liquidado (pago).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean paid,

        @Schema(description = "Data de liquidação (pagamento) do boleto, se pago.")
        LocalDate paymentDate,

        @Schema(description = "Número da nota fiscal vinculada ao boleto, se houver.")
        String invoiceNumber,

        @Schema(description = "Data da nota fiscal vinculada ao boleto, se houver.")
        LocalDate invoiceDate,

        @Schema(description = "Nº parcela do boleto, se houver.")
        Integer installmentNumber,

        @Schema(description = "ID do plano de parcelamento que agrupa as parcelas, se houver.")
        String installmentPlanId,

        @Schema(description = "Data de criação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.")
        String updatedBy
) {
}