package br.com.toppower.erp_toppower.contractfinances.dto;

import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryStatus;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

/**
 * Lançamento do financeiro de contrato, com os dados do controle
 * (código do contrato, cliente) já resolvidos para exibição na tela.
 */
@Schema(name = "ContractFinanceEntryResponse",
        description = "Lançamento do financeiro de contrato (gasto ou recebimento).")
public record ContractFinanceEntryResponse(

        @Schema(description = "ID do lançamento.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "ID do controle de contrato vinculado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long controlId,

        @Schema(description = "ID do contrato.", requiredMode = Schema.RequiredMode.REQUIRED)
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

        @Schema(description = "Natureza do lançamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        ContractFinanceEntryType type,

        @Schema(description = "Data da movimentação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate entryDate,

        @Schema(description = "Valor do lançamento (positivo; o sentido é dado pelo tipo).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Descrição livre do gasto/recebimento.")
        String description,

        @Schema(description = "Status do lançamento (INATIVO não soma nos totais).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        ContractFinanceEntryStatus status,

        @Schema(description = "Data de criação.")
        Instant createdAt,

        @Schema(description = "Data da última alteração.")
        Instant updatedAt,

        @Schema(description = "Usuário que criou o lançamento.")
        String createdBy,

        @Schema(description = "Usuário que alterou o lançamento por último.")
        String updatedBy
) {
}
