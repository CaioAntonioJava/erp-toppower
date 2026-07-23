package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Decisão do usuário para um item da NF-e na confirmação da
 * importação. O {@code itemIndex} corresponde à posição do item
 * no preview (campo {@link NfeItemData#itemIndex}).
 */
@Schema(name = "NfeConfirmItem",
        description = "Ação do usuário por item da NF-e na confirmação.")
public record NfeConfirmItem(

        @Schema(description = "Índice do item no preview (0-based).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int itemIndex,

        @Schema(description = "Ação escolhida para o item.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Ação do item é obrigatória")
        ItemAction action,

        @Schema(description = "ID do produto existente. Obrigatório quando "
                + "action = ESTOQUE e o item estava DIVERGENTE (o usuário "
                + "decide vincular ao candidato). Ignorado nos demais casos.")
        Long existingProductId
) {
}