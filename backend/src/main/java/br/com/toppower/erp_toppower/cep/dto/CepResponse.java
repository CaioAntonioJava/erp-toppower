package br.com.toppower.erp_toppower.cep.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Resposta do lookup de CEP ({@code GET /api/v1/ceps/{cep}}).
 *
 * <p>Os nomes dos campos espelham propositadamente o {@code AddressDto}
 * e o {@code Address} embutivel ({@code street, neighborhood, city, state,
 * zipCode}) para que o frontend possa atribuir diretamente a resposta
 * ao sub-objeto {@code address} dos formularios de Customer, Company e
 * Supplier.</p>
 */
@Schema(name = "CepResponse", description = "Endereco retornado pelo lookup de CEP na base local offline.")
public record CepResponse(

        @Schema(description = "CEP formatado com hifen (00000-000).",
                example = "01310-100", requiredMode = Schema.RequiredMode.REQUIRED)
        String zipCode,

        @Schema(description = "Logradouro (rua, avenida, etc.). Pode ser nulo em CEPs genericos de cidade.",
                example = "Avenida Paulista")
        String street,

        @Schema(description = "Bairro. Pode ser nulo.", example = "Bela Vista")
        String neighborhood,

        @Schema(description = "Cidade.", example = "São Paulo",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String city,

        @Schema(description = "UF (2 letras).", example = "SP",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String state,

        @Schema(description = "Latitude (decimal, opcional).", example = "-23.5613")
        BigDecimal latitude,

        @Schema(description = "Longitude (decimal, opcional).", example = "-46.6565")
        BigDecimal longitude
) {
}