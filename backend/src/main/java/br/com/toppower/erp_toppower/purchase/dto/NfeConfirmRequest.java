package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload para confirmar a importação de NF-e.
 *
 * <p>Contém o XML original em Base64, que é re-parseado e persistido
 * no backend.</p>
 */
@Schema(name = "NfeConfirmRequest",
        description = "Payload para confirmar a importação de NF-e.")
public record NfeConfirmRequest(

        @Schema(description = "XML da NF-e em Base64.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "XML em Base64 é obrigatório")
        String xmlBase64
) {
}