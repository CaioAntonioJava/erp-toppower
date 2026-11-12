package br.com.toppower.erp_toppower.cashflow.dto;

import br.com.toppower.erp_toppower.cashflow.enums.CashFlowDocumentSource;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowEntryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lançamento consolidado do fluxo de caixa: um pagamento efetivado
 * (entrada em contas a receber ou saída em contas a pagar), enriquecido
 * com o documento de origem, o cliente/fornecedor e o saldo do caixa
 * após o movimento.
 */
@Schema(name = "CashFlowEntryResponse",
        description = "Lançamento consolidado do fluxo de caixa.")
public record CashFlowEntryResponse(

        @Schema(description = "ID do pagamento (ReceivablePayment ou PayablePayment).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Tipo de movimento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String entryType,

        @Schema(description = "Data do pagamento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @Schema(description = "Valor do movimento (sempre positivo; o sentido é dado por entryType).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Documento de origem do movimento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String sourceType,

        @Schema(description = "Código formatado do documento de origem (ex.: 'PV-2800-2026', "
                + "'PL-001-2026', 'CL-001-2026'). Nulo quando não aplicável (manual/boleto).")
        String sourceCode,

        @Schema(description = "Rótulo completo do documento de origem em pt-BR "
                + "(ex.: 'Pedido de Venda PV-2800-2026').",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String documentLabel,

        @Schema(description = "ID do documento de origem (pedido, proposta técnica, contrato, "
                + "boleto ou nota de compra), para navegação. Nulo quando não aplicável.")
        Long sourceDocId,

        @Schema(description = "Tipo de cliente da entrada (apenas entradas; nulo nas saídas).",
                allowableValues = {"CUSTOMER", "COMPANY"})
        String clientType,

        @Schema(description = "ID do cliente (customer ou company). Nulo nas saídas.")
        Long clientId,

        @Schema(description = "Nome de exibição do cliente. Nulo nas saídas.")
        String clientName,

        @Schema(description = "Código do cliente. Nulo nas saídas.")
        String clientCode,

        @Schema(description = "Nome do fornecedor (apenas saídas).")
        String counterpartyName,

        @Schema(description = "Descrição da conta de origem.")
        String description,

        @Schema(description = "Observações do pagamento.")
        String notes,

        @Schema(description = "Saldo do caixa após este movimento (real, considera todos os "
                + "movimentos, independentemente dos filtros aplicados).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balanceAfter
) {
}