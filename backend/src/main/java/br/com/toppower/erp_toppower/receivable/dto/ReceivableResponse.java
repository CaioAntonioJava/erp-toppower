package br.com.toppower.erp_toppower.receivable.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Representação pública completa de uma conta a receber, retornada pelos
 * endpoints de detalhe/criação/atualização. Inclui o histórico de
 * pagamentos e o saldo devedor calculado.
 */
@Schema(name = "ReceivableResponse",
        description = "Conta a receber com histórico de pagamentos e saldo devedor.")
public record ReceivableResponse(

        @Schema(description = "Identificador único (ID) da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição/origem da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Valor total da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value,

        @Schema(description = "Valor já recebido (soma dos pagamentos).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal paidAmount,

        @Schema(description = "Saldo devedor (value - paidAmount).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balance,

        @Schema(description = "Data de vencimento.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status atual.",
                allowableValues = {"ABERTO", "PAGO", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ReceivableStatus status,

        @Schema(description = "Origem da conta.",
                allowableValues = {"MANUAL", "SALES_ORDER", "TECHNICAL_PROPOSAL", "CONTRACT"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ReceivableSource sourceType,

        @Schema(description = "ID do cliente (pessoa física), se aplicável.")
        Long customerId,

        @Schema(description = "ID da empresa (pessoa jurídica), se aplicável.")
        Long companyId,

        @Schema(description = "Nome resolvido do cliente/empresa.")
        String clientName,

        @Schema(description = "Código resolvido do cliente/empresa.")
        String clientCode,

        @Schema(description = "Condição de pagamento (mesmo domínio das propostas comerciais).")
        PaymentCondition paymentCondition,

        @Schema(description = "ID do pedido de venda de origem, se aplicável.")
        Long salesOrderId,

        @Schema(description = "Número do pedido de venda de origem, se aplicável.")
        Long salesOrderNumber,

        @Schema(description = "Código formatado do pedido de venda de origem, se aplicável.",
                example = "PV-2800-2026")
        String salesOrderCode,

        @Schema(description = "ID da proposta técnica de origem, se aplicável.")
        Long technicalProposalId,

        @Schema(description = "Código da proposta técnica de origem, se aplicável.")
        String technicalProposalCode,

        @Schema(description = "ID do contrato de origem, se aplicável.")
        Long contractId,

        @Schema(description = "Código do contrato de origem, se aplicável.")
        String contractCode,

        @Schema(description = "Data do último pagamento registrado.")
        LocalDate paymentDate,

        @Schema(description = "Histórico de pagamentos ordenado por data.")
        List<ReceivablePaymentResponse> payments,

        @Schema(description = "Data de criação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.")
        String updatedBy
) {
}