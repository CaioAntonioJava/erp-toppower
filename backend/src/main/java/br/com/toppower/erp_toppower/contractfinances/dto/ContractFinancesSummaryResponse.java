package br.com.toppower.erp_toppower.contractfinances.dto;

import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumo consolidado do financeiro de contratos respeitando os filtros
 * informados: total recebido, total gasto, resultado (recebido − gasto)
 * e quebra por natureza do lançamento. Considera apenas lançamentos
 * ATIVO (inativos não somam).
 */
@Schema(name = "ContractFinancesSummaryResponse",
        description = "Resumo consolidado (por filtros) do financeiro de contratos.")
public record ContractFinancesSummaryResponse(

        @Schema(description = "Total recebido (RECEBIDO).", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalRecebido,

        @Schema(description = "Total de gastos (todas as categorias de gasto).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalGasto,

        @Schema(description = "Resultado do controle: recebido − gasto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal resultado,

        @Schema(description = "Quantidade de lançamentos considerados.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long entriesCount,

        @Schema(description = "Quebra dos totais por natureza do lançamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<TypeTotal> porTipo
) {

        /**
         * Total de uma natureza específica de lançamento.
         */
        @Schema(name = "ContractFinancesTypeTotal", description = "Total por natureza do lançamento.")
        public record TypeTotal(

                @Schema(description = "Natureza do lançamento.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                ContractFinanceEntryType type,

                @Schema(description = "Soma do valor dos lançamentos dessa natureza.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                BigDecimal total,

                @Schema(description = "Quantidade de lançamentos dessa natureza.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                long count
        ) {
        }
}
