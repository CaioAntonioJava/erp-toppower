package br.com.toppower.erp_toppower.contractfinances.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

/**
 * Linha do registro de controle do financeiro de um contrato: retrato
 * dos dados comerciais (código, cliente, data, valor) mais os totais
 * agregados dos lançamentos ATIVO pendurados no controle. O resultado
 * é {@code totalRecebido - totalGasto}.
 */
@Schema(name = "ContractFinanceControlResponse",
        description = "Controle do financeiro de um contrato com os totais agregados dos lançamentos.")
public record ContractFinanceControlResponse(

        @Schema(description = "ID do controle.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "ID do contrato controlado.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long contractId,

        @Schema(description = "Código comercial formatado do contrato. Ex.: CL-001-2026.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String contractCode,

        @Schema(description = "Tipo do cliente (CUSTOMER = PF, COMPANY = PJ).", example = "COMPANY")
        String clientType,

        @Schema(description = "Código do cliente. Ex.: EMP000001.")
        String clientCode,

        @Schema(description = "Nome do cliente.", requiredMode = Schema.RequiredMode.REQUIRED)
        String clientName,

        @Schema(description = "Data de vigência do contrato.", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate contractDate,

        @Schema(description = "Valor (preço) do contrato.", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal contractValue,

        @Schema(description = "Total recebido nos lançamentos ATIVO.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalRecebido,

        @Schema(description = "Total de gastos nos lançamentos ATIVO.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal totalGasto,

        @Schema(description = "Resultado do contrato: recebido − gasto.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal resultado,

        @Schema(description = "Quantidade de lançamentos ATIVO.", requiredMode = Schema.RequiredMode.REQUIRED)
        long entriesCount,

        @Schema(description = "Data de criação do controle.")
        Instant createdAt,

        @Schema(description = "Data da última alteração do controle.")
        Instant updatedAt,

        @Schema(description = "Usuário que criou o controle.")
        String createdBy,

        @Schema(description = "Usuário que alterou o controle por último.")
        String updatedBy
) {
}
