package br.com.toppower.erp_toppower.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO reutilizável que representa um endereço em requisições/respostas.
 * Aplicado em sub-objetos de DTOs de entidades que possuem endereço
 * (Company, Customer, Supplier, ...).
 */
@Schema(name = "AddressDto", description = "Endereço (logradouro, número, cidade, UF, CEP).")
public record AddressDto(

        @Schema(description = "Logradouro (rua, avenida, etc.).", example = "Av. Paulista",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Logradouro é obrigatório")
        @Size(max = 200, message = "Logradouro deve ter no máximo {max} caracteres")
        String street,

        @Schema(description = "Número do imóvel. Aceita 'S/N' para sem número.",
                example = "1000", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
        @NotBlank(message = "Número é obrigatório")
        @Size(max = 20, message = "Número deve ter no máximo {max} caracteres")
        String number,

        @Schema(description = "Complemento (apto, bloco, sala).", example = "Apto 101",
                maxLength = 100)
        @Size(max = 100, message = "Complemento deve ter no máximo {max} caracteres")
        String complement,

        @Schema(description = "Bairro.", example = "Bela Vista", maxLength = 100)
        @Size(max = 100, message = "Bairro deve ter no máximo {max} caracteres")
        String neighborhood,

        @Schema(description = "Cidade.", example = "São Paulo",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 100, message = "Cidade deve ter no máximo {max} caracteres")
        String city,

        @Schema(description = "Unidade Federativa (UF) - 2 letras.", example = "SP",
                requiredMode = Schema.RequiredMode.REQUIRED, minLength = 2, maxLength = 2)
        @NotBlank(message = "UF é obrigatória")
        @Size(min = 2, max = 2, message = "UF deve ter exatamente 2 caracteres")
        @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "UF inválida")
        String state,

        @Schema(description = "CEP (8 dígitos, com ou sem hífen).", example = "01310-100",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, minLength = 8, maxLength = 9)
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve estar no formato 00000-000 ou conter 8 dígitos")
        String zipCode
) {
}
