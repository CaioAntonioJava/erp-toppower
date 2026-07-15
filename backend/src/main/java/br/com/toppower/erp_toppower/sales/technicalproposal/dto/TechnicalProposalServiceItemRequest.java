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
 * <p>Cada linha possui uma descrição (HTML formatado, opcional) e um preço
 * (opcional). Quando o serviço é do catálogo, os campos {@code category} e
 * {@code serviceTemplateId} são preenchidos para permitir a restauração
 * do estado no formulário de edição.</p>
 */
@Schema(name = "TechnicalProposalServiceItemRequest",
        description = "Linha de serviço de uma proposta técnica.")
public record TechnicalProposalServiceItemRequest(

        @Schema(description = "Descrição do serviço prestado (HTML formatado, texto livre). Opcional.",
                example = "Instalação de quadro de distribuição.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 2000, message = "Descrição do serviço deve ter no máximo {max} caracteres")
        String description,

        @Schema(description = "Preço do serviço prestado. Opcional — omitir quando o serviço "
                + "for gratuito/incluso.",
                example = "350.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.00", message = "Preço do serviço não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Preço do serviço inválido")
        BigDecimal price,

        @Schema(description = "Categoria do serviço no catálogo (ex.: \"EXECUÇÃO_SPDA\"). "
                + "Opcional — presente apenas quando o item é do catálogo.",
                example = "EXECUÇÃO_SPDA", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String category,

        @Schema(description = "ID do ServiceTemplate que originou este item. "
                + "Opcional — presente apenas quando o item é do catálogo.",
                example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long serviceTemplateId
) {
}