package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resposta do preview de importação de NF-e.
 *
 * <p>Contém todos os dados extraídos do XML — fornecedor, itens e
 * conta a pagar — sem persistir nada no banco. O usuário revisa e
 * confirma via {@link NfeConfirmRequest}.</p>
 */
@Schema(name = "NfePreviewResponse",
        description = "Preview da importação de NF-e — dados extraídos sem persistir.")
public record NfePreviewResponse(

        @Schema(description = "XML da NF-e em Base64 para envio na confirmação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String xmlBase64,

        @Schema(description = "Dados do fornecedor (emitente).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        NfeSupplierData supplier,

        @Schema(description = "Itens da nota com status de importação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<NfeItemData> items,

        @Schema(description = "Dados da conta a pagar.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        NfePayableData payable
) {
}