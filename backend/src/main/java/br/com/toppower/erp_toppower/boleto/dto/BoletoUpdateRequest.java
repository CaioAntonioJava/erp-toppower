package br.com.toppower.erp_toppower.boleto.dto;

import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Atualização parcial de um boleto (PATCH). Todos os campos são opcionais:
 * envie apenas os campos que deseja alterar.
 */
@Schema(name = "BoletoUpdateRequest", description = "Dados para atualização parcial de um boleto (PATCH).")
public record BoletoUpdateRequest(

        @Schema(description = "Nº da obra/contrato vinculado ao boleto (campo livre, opcional). "
                + "Envie string vazia para limpar.",
                example = "CT-001-2026", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 60)
        @Size(max = 60, message = "Nº Obra deve ter no máximo {max} caracteres")
        String contractWorkNumber,

        @Schema(description = "Nome do responsável pelo boleto. Envie string vazia para limpar.",
                maxLength = 120, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 120, message = "Nome do responsável deve ter no máximo {max} caracteres")
        String responsibleName,

        @Schema(description = "Novo valor do boleto.")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Nova data de vencimento.")
        @FutureOrPresent(message = "Data de vencimento deve ser hoje ou uma data futura")
        LocalDate dueDate,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        RegistrationStatus status,

        @Schema(description = "ID da empresa (fornecedor) vinculado. Quando informado e "
                + "ainda não houver conta a pagar vinculada ao boleto, dispara a geração "
                + "automática de uma conta a pagar.",
                example = "12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long supplierId,

        @Schema(description = "Número da nota fiscal vinculada ao boleto. Envie string vazia para limpar.",
                maxLength = 60, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 60, message = "Nota fiscal deve ter no máximo {max} caracteres")
        String invoiceNumber,

        @Schema(description = "Data da nota fiscal vinculada ao boleto.",
                example = "2026-08-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate invoiceDate,

        @Schema(description = "Nº parcelas (manual).",
                example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED, minimum = "1")
        @Min(value = 1, message = "Nº parcelas deve ser no mínimo 1")
        Integer installmentNumber
) {
}