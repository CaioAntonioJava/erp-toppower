package br.com.toppower.erp_toppower.contractfinances.dto;

import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryStatus;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Criação de um lançamento (gasto ou recebimento) no financeiro de um
 * contrato. O contrato é informado por {@code contractId} (o frontend
 * resolve pelo typeahead de código do contrato); o serviço localiza
 * (ou cria, se faltar) o controle e rejeita contratos INATIVO.
 */
@Schema(name = "ContractFinanceEntryCreateRequest",
        description = "Dados para lançar um gasto ou recebimento no financeiro de um contrato.")
public record ContractFinanceEntryCreateRequest(

        @Schema(description = "ID do contrato controlado.", example = "42",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Contrato é obrigatório")
        Long contractId,

        @Schema(description = "Natureza do lançamento (RECEBIDO = entrada; demais = gasto).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Tipo do lançamento é obrigatório")
        ContractFinanceEntryType type,

        @Schema(description = "Data da movimentação.", example = "2026-05-10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Data do lançamento é obrigatória")
        LocalDate entryDate,

        @Schema(description = "Valor do lançamento (positivo).", example = "850.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Valor do lançamento é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor do lançamento deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Descrição livre do gasto/recebimento.",
                example = "Cabo 50mm - trecho 2", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 500)
        @Size(max = 500, message = "Descrição deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ContractFinanceEntryStatus status
) {
}
