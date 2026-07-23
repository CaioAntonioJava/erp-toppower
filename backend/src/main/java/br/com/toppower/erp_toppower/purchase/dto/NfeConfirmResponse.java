package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resposta da confirmação de importação de NF-e.
 *
 * <p>Contém os IDs dos registros criados/atualizados.</p>
 */
@Schema(name = "NfeConfirmResponse",
        description = "Resumo da importação confirmada com IDs criados.")
public record NfeConfirmResponse(

        @Schema(description = "ID do fornecedor criado ou existente.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long supplierId,

        @Schema(description = "Indica se o fornecedor foi criado nesta importação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean supplierCreated,

        @Schema(description = "IDs dos produtos criados nesta importação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> createdProductIds,

        @Schema(description = "IDs dos produtos existentes que receberam entrada de estoque.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> existingProductIds,

        @Schema(description = "ID da conta a pagar criada.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long payableId,

        @Schema(description = "Número da NF-e importada.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String invoiceNumber,

        @Schema(description = "Chave de acesso da NF-e importada.")
        String accessKey,

        @Schema(description = "Quantidade de itens ignorados pelo usuário.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        int ignoredItemCount
) {
}