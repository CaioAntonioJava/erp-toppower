package br.com.toppower.erp_toppower.sales.salesorder.dto;

import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Dados opcionais para conversão de uma {@code Quotation} em pedido de
 * venda.
 *
 * <p>Todos os campos são opcionais: quando omitidos, o pedido herda os
 * valores da proposta de origem. Quando informados, <b>sobrescrevem</b>
 * os valores da proposta no pedido (não alteram a proposta).</p>
 *
 * <p>Cliente, vendedor, itens, descontos e frete são sempre copiados
 * da proposta (snapshot) — não podem ser sobrescritos neste request.
 * Para alterá-los, edite o pedido após a conversão.</p>
 */
@Schema(name = "SalesOrderFromQuotationRequest",
        description = "Dados opcionais para conversão de proposta em pedido de venda.")
public record SalesOrderFromQuotationRequest(

        @Schema(description = "Sobrescreve 'Aos cuidados de' da proposta. Quando omitido, copia da proposta.",
                example = "Sr. João Silva", maxLength = 150,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 150, message = "Aos cuidados de deve ter no máximo {max} caracteres")
        String attention,

        @Schema(description = "Sobrescreve a condição de pagamento da proposta.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        PaymentCondition paymentCondition,

        @Schema(description = "Sobrescreve as observações da proposta (enviar string vazia para limpar).",
                example = "Entrega em até 5 dias úteis.", maxLength = 2000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2000, message = "Observações devem ter no máximo {max} caracteres")
        String notes
) {
}