package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Payload para confirmar a importação de NF-e.
 *
 * <p>Contém o XML original em Base64, que é re-parseado e persistido
 * no backend, e a lista de decisões do usuário por item
 * (cadastrar/estoque/ignorar). O fornecedor é sempre determinado pelo
 * CNPJ do emitente no XML — o usuário não pode atribuir a outro.</p>
 */
@Schema(name = "NfeConfirmRequest",
        description = "Payload para confirmar a importação de NF-e.")
public record NfeConfirmRequest(

        @Schema(description = "XML da NF-e em Base64.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "XML em Base64 é obrigatório")
        String xmlBase64,

        @Schema(description = "Decisões do usuário por item da nota.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "As decisões por item são obrigatórias")
        @Valid
        List<NfeConfirmItem> items
) {
}