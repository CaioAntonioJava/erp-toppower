package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de produto de um contrato (request).
 *
 * <p>Diferente da Proposta Técnica, este item possui apenas a referência
 * ao produto e a quantidade — sem preço, desconto ou margem de lucro.</p>
 */
@Schema(name = "ContractProductItemRequest",
        description = "Item de produto de um contrato (referência + quantidade).")
public record ContractProductItemRequest(

        @Schema(description = "UUID do produto cadastrado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Produto é obrigatório")
        UUID productUuid,

        @Schema(description = "Quantidade contratada.",
                example = "10.0000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
        @Digits(integer = 6, fraction = 4,
                message = "Quantidade deve ter no máximo 6 dígitos inteiros e 4 decimais")
        BigDecimal quantity
) {
}
