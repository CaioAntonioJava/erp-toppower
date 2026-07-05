package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Linha da lista de serviços prestados de uma proposta técnica. Usado
 * tanto no {@code TechnicalProposalCreateRequest} quanto no
 * {@code TechnicalProposalUpdateRequest}.
 *
 * <p>Cada linha possui apenas uma descrição (texto) e um preço
 * (opcional). Não há cálculo adicional sobre a linha — o preço informado
 * entra diretamente no somatório do subtotal da proposta.</p>
 */
@Schema(name = "TechnicalProposalServiceItemRequest",
        description = "Linha de serviço de uma proposta técnica.")
public record TechnicalProposalServiceItemRequest(

        @Schema(description = "Descrição do serviço prestado (texto livre).",
                example = "Instalação de quadro de distribuição.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Size(min = 1, max = 2000, message = "Descrição do serviço deve ter entre 1 e {max} caracteres")
        String description,

        @Schema(description = "Preço do serviço prestado. Opcional — omitir quando o serviço "
                + "for gratuito/incluso.",
                example = "350.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Preço do serviço não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Preço do serviço inválido")
        BigDecimal price
) {
}