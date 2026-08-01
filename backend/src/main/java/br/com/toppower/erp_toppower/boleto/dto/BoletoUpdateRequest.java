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

        @Schema(description = "Nova descrição do boleto.", maxLength = 200)
        @Size(max = 200, message = "Descrição do boleto deve ter no máximo {max} caracteres")
        String description,

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
        RegistrationStatus status,

        @Schema(description = "ID do fornecedor (supplier) vinculado. Quando informado e "
                + "ainda não houver conta a pagar vinculada ao boleto, dispara a geração "
                + "automática de uma conta a pagar.",
                example = "12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long supplierId,

        @Schema(description = "Nº de Contrato/Obra vinculado ao boleto (campo livre, opcional). "
                + "Envie string vazia para limpar.",
                example = "CT-001-2026", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 60)
        @Size(max = 60, message = "Nº Contrato/Obra deve ter no máximo {max} caracteres")
        String contractWorkNumber,

        @Schema(description = "Data de cadastro do boleto.",
                example = "2026-08-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate registrationDate
) {
}