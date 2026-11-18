package br.com.toppower.erp_toppower.contractfinances.dto;

import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Criação em lote de lançamentos no financeiro de um contrato: um
 * contrato e uma data compartilhados, com várias naturezas de uma vez.
 * Cada {@code item} com valor preenchido vira um lançamento — a UI envia
 * apenas as linhas que o usuário preencheu (preencher Combustível sem
 * preencher Refeição é permitido); campos individuais são opcionais,
 * mas os itens enviados precisam de natureza e valor.
 */
@Schema(name = "ContractFinanceEntryBatchCreateRequest",
        description = "Dados para lançar vários gastos/recebimentos de um contrato em uma única chamada.")
public record ContractFinanceEntryBatchCreateRequest(

        @Schema(description = "ID do contrato controlado.", example = "42",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Contrato é obrigatório")
        Long contractId,

        @Schema(description = "Data da movimentação, compartilhada por todos os itens.",
                example = "2026-05-10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Data do lançamento é obrigatória")
        LocalDate entryDate,

        @Schema(description = "Itens a lançar (apenas as linhas preenchidas pela UI).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Informe ao menos um lançamento")
        @Valid
        List<BatchItemRequest> items
) {

        /**
         * Um item do lote: natureza, valor e descrição opcional.
         */
        @Schema(name = "ContractFinanceEntryBatchItemRequest",
                description = "Um lançamento do lote (natureza + valor + descrição).")
        public record BatchItemRequest(

                @Schema(description = "Natureza do lançamento.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "Natureza do lançamento é obrigatória")
                ContractFinanceEntryType type,

                @Schema(description = "Valor do lançamento (positivo).", example = "150.00",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "Valor do lançamento é obrigatório")
                @DecimalMin(value = "0.01", message = "Valor do lançamento deve ser maior que zero")
                BigDecimal amount,

                @Schema(description = "Descrição livre do gasto/recebimento.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED, maxLength = 500)
                @Size(max = 500, message = "Descrição deve ter no máximo {max} caracteres")
                String description
        ) {
        }
}
