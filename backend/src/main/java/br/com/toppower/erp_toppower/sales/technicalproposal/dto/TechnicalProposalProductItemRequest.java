package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Linha de produto de uma proposta técnica. Usado tanto no
 * {@code TechnicalProposalCreateRequest} quanto no
 * {@code TechnicalProposalUpdateRequest}.
 *
 * <p>O {@code totalPrice} <b>não</b> é informado pelo cliente — ele é
 * calculado pelo serviço como {@code unitPrice * quantity}.</p>
 */
@Schema(name = "TechnicalProposalProductItemRequest",
        description = "Linha de produto de uma proposta técnica.")
public record TechnicalProposalProductItemRequest(

        @Schema(description = "Identificador (UUID) do produto a ser incluído na linha.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Produto é obrigatório")
        Long productId,

        @Schema(description = "Quantidade do produto. Suporta até 4 casas decimais (ex.: metros).",
                example = "2.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
        @Digits(integer = 6, fraction = 4, message = "Quantidade inválida")
        BigDecimal quantity,

        @Schema(description = "Preço unitário do produto no momento da emissão (snapshot).",
                example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.00", message = "Preço unitário não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Preço unitário inválido")
        BigDecimal unitPrice
) {
}