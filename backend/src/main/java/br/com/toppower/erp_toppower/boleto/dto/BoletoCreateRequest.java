package br.com.toppower.erp_toppower.boleto.dto;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "BoletoCreateRequest", description = "Dados para cadastro de um novo boleto.")
public record BoletoCreateRequest(

        @Schema(description = "Descrição do boleto.",
                example = "Pagamento fornecedor XYZ", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Descrição do boleto é obrigatória")
        @Size(max = 200, message = "Descrição do boleto deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Beneficiário do boleto (quem recebe o pagamento).",
                example = "EMPRESA XPTO LTDA", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Beneficiário é obrigatório")
        @Size(max = 200, message = "Beneficiário deve ter no máximo {max} caracteres")
        String payee,

        @Schema(description = "Valor do boleto.",
                example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Data de vencimento do boleto.",
                example = "2026-08-17", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Data de vencimento é obrigatória")
        @FutureOrPresent(message = "Data de vencimento deve ser hoje ou uma data futura")
        LocalDate dueDate,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        RegistrationStatus status
) {
}