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

        @Schema(description = "Descrição do boleto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Beneficiário do boleto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String payee,

        @Schema(description = "Valor do boleto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value,

        @Schema(description = "Data de vencimento do boleto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status atual do boleto.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        RegistrationStatus status,

        @Schema(description = "ID do fornecedor vinculado, se aplicável.")
        Long supplierId,

        @Schema(description = "Nome de exibição do fornecedor vinculado, se aplicável.")
        String supplierName,

        @Schema(description = "Indica se o boleto foi liquidado (pago).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean paid,

        @Schema(description = "Data de liquidação do boleto, se pago.")
        LocalDate paymentDate,

        @Schema(description = "Nº de Contrato/Obra vinculado ao boleto, se houver.")
        String contractWorkNumber,

        @Schema(description = "Data de cadastro do boleto (informável, distinta do createdAt de auditoria).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate registrationDate,

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