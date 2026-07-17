package br.com.toppower.erp_toppower.boleto.dto;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Atualização parcial de um boleto (PATCH). Todos os campos são opcionais:
 * envie apenas os campos que deseja alterar.
 */
@Schema(name = "BoletoUpdateRequest", description = "Dados para atualização parcial de um boleto (PATCH).")
public record BoletoUpdateRequest(

        @Schema(description = "Novo número do documento.", maxLength = 50)
        @Size(max = 50, message = "Número do documento deve ter no máximo {max} caracteres")
        String documentNumber,

        @Schema(description = "Novo beneficiário.", maxLength = 200)
        @Size(max = 200, message = "Beneficiário deve ter no máximo {max} caracteres")
        String payee,

        @Schema(description = "Novo valor do boleto.")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Nova data de vencimento.")
        @FutureOrPresent(message = "Data de vencimento deve ser hoje ou uma data futura")
        LocalDate dueDate,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        RegistrationStatus status
) {
}