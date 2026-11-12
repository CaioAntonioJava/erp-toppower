package br.com.toppower.erp_toppower.contractfinances.dto;

import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Edição parcial (PATCH) de um lançamento: todos os campos são
 * opcionais; nulos preservam o valor atual. O vínculo com o contrato e
 * o status não são alteráveis por aqui (status via inativar/ativar).
 */
@Schema(name = "ContractFinanceEntryUpdateRequest",
        description = "Dados para editar um lançamento do financeiro de contrato (PATCH parcial).")
public record ContractFinanceEntryUpdateRequest(

        @Schema(description = "Natureza do lançamento (RECEBIDO = entrada; demais = gasto).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ContractFinanceEntryType type,

        @Schema(description = "Data da movimentação.", example = "2026-05-10",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate entryDate,

        @Schema(description = "Valor do lançamento (positivo).", example = "850.00",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.01", message = "Valor do lançamento deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Descrição livre do gasto/recebimento.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 500)
        @Size(max = 500, message = "Descrição deve ter no máximo {max} caracteres")
        String description
) {
}
