package br.com.toppower.erp_toppower.sales.quotation.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Resumo de um cliente (pessoa física ou jurídica) usado em
 * typeaheads/selects, retornado pelo endpoint de busca.
 *
 * <p>Une os dois tipos de cliente em um único DTO, discriminando pelo
 * campo {@link #type} para que o caller saiba qual entidade referenciar
 * ao selecionar um item.</p>
 */
@Schema(name = "ClientSummaryResponse", description = "Resumo de um cliente (PF ou PJ) para seleção em propostas.")
public record ClientSummaryResponse(

        @Schema(description = "Tipo do cliente.",
                allowableValues = {"CUSTOMER", "COMPANY"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        QuotationResponse.ClientType type,

        @Schema(description = "UUID do cliente.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Código interno (CLI000001, EMP000001, etc.).", example = "CLI000042",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Nome de exibição (nome da pessoa, razão social ou nome fantasia).",
                example = "CAIO ANTONIO DA SILVA",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Documento (CPF ou CNPJ), formatado.", example = "123.456.789-00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String document
) {
}
