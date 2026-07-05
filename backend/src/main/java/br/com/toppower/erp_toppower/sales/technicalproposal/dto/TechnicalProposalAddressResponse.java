package br.com.toppower.erp_toppower.sales.technicalproposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Endereço retornado pela API. Todos os campos podem ser nulos, pois o
 * endereço é opcional na proposta técnica.
 */
@Schema(name = "TechnicalProposalAddressResponse",
        description = "Endereço de execução da proposta técnica (opcional).")
public record TechnicalProposalAddressResponse(

        @Schema(description = "Logradouro.", example = "Av. Paulista",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String street,

        @Schema(description = "Número do imóvel.", example = "1000",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String number,

        @Schema(description = "Complemento.", example = "Apto 101",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String complement,

        @Schema(description = "Bairro.", example = "Bela Vista",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String neighborhood,

        @Schema(description = "Cidade.", example = "São Paulo",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String city,

        @Schema(description = "UF.", example = "SP",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String state,

        @Schema(description = "CEP.", example = "01310-100",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String zipCode
) {
}