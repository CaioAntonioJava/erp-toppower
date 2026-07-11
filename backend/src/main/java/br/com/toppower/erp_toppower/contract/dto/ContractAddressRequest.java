package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Endereço opcional vinculado ao contrato. <b>Variante permissiva</b>:
 * nenhum campo é obrigatório (todos são nullable). Quando o objeto
 * inteiro for nulo no request, nenhum endereço é persistido. Quando o
 * endereço é preenchido, é tipicamente sugerido a partir do cadastro do
 * cliente selecionado — mas pode ser livremente editado antes de
 * salvar.
 */
@Schema(name = "ContractAddressRequest",
        description = "Endereço do contrato (opcional).")
public record ContractAddressRequest(

        @Schema(description = "Logradouro.", example = "Av. Paulista",
                maxLength = 200, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 200, message = "Logradouro deve ter no máximo {max} caracteres")
        String street,

        @Schema(description = "Número do imóvel. Aceita 'S/N'.", example = "1000",
                maxLength = 20, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 20, message = "Número deve ter no máximo {max} caracteres")
        String number,

        @Schema(description = "Complemento (apto, bloco, sala).", example = "Apto 101",
                maxLength = 100, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Complemento deve ter no máximo {max} caracteres")
        String complement,

        @Schema(description = "Bairro.", example = "Bela Vista",
                maxLength = 100, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Bairro deve ter no máximo {max} caracteres")
        String neighborhood,

        @Schema(description = "Cidade.", example = "São Paulo",
                maxLength = 100, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "Cidade deve ter no máximo {max} caracteres")
        String city,

        @Schema(description = "UF (2 letras).", example = "SP", minLength = 2, maxLength = 2,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(min = 2, max = 2, message = "UF deve ter exatamente 2 caracteres")
        @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "UF inválida")
        String state,

        @Schema(description = "CEP (8 dígitos, com ou sem hífen).", example = "01310-100",
                minLength = 8, maxLength = 9, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "\\d{5}-?\\d{3}",
                message = "CEP deve estar no formato 00000-000 ou conter 8 dígitos")
        String zipCode
) {
}