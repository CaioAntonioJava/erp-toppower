package br.com.toppower.erp_toppower.payable.dto;

import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Representação pública completa de uma conta a pagar, retornada pelos
 * endpoints de detalhe/criação/atualização. Inclui as parcelas
 * programadas, o histórico de pagamentos e o saldo devedor calculado.
 */
@Schema(name = "PayableResponse",
        description = "Conta a pagar com parcelas, histórico de pagamentos e saldo devedor.")
public record PayableResponse(

        @Schema(description = "Identificador único (ID) da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Descrição/origem da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Valor total da conta.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value,

        @Schema(description = "Valor já pago (soma dos pagamentos).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal paidAmount,

        @Schema(description = "Saldo devedor (value - paidAmount).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balance,

        @Schema(description = "Data de emissão.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate issueDate,

        @Schema(description = "Vencimento-base (1ª parcela).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status atual.",
                allowableValues = {"ABERTO", "PAGO", "CANCELADO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        PayableStatus status,

        @Schema(description = "Origem da conta.",
                allowableValues = {"MANUAL", "BOLETO", "PURCHASE_INVOICE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        PayableSource sourceType,

        @Schema(description = "ID do fornecedor.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long supplierId,

        @Schema(description = "Nome resolvido do fornecedor.")
        String supplierName,

        @Schema(description = "CNPJ do fornecedor (para exibição).")
        String supplierTaxId,

        @Schema(description = "Condição de pagamento (mesmo domínio das propostas comerciais).")
        PaymentCondition paymentCondition,

        @Schema(description = "Quantidade de parcelas programadas.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int installmentsCount,

        @Schema(description = "ID do boleto de origem, se aplicável.")
        Long boletoId,

        @Schema(description = "ID da nota de compra de origem, se aplicável.")
        Long purchaseInvoiceId,

        @Schema(description = "Número da nota de compra de origem, se aplicável.",
                example = "NF-123")
        String purchaseInvoiceNumber,

        @Schema(description = "Data do último pagamento registrado.")
        LocalDate paymentDate,

        @Schema(description = "Parcelas programadas ordenadas por número.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<PayableInstallmentResponse> installments,

        @Schema(description = "Histórico de pagamentos ordenado por data.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<PayablePaymentResponse> payments,

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