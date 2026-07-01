package br.com.toppower.erp_toppower.customer.dto;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.common.validation.ValidCpf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "CustomerCreateRequest", description = "Dados para cadastro de um novo cliente (pessoa física).")
public record CustomerCreateRequest(

        @Schema(description = "Nome completo da pessoa.", example = "Caio Antônio da Silva",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "E-mail de contato (deve ser único entre clientes PF).",
                example = "caio@toppower.com.br", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Telefone de contato.", example = "(11) 98765-4321",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 20, message = "Telefone deve ter no máximo {max} caracteres")
        String phone,

        @Schema(description = "CPF único (somente dígitos ou formatado).",
                example = "123.456.789-00",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 14)
        @NotBlank(message = "CPF é obrigatório")
        @ValidCpf(message = "CPF inválido")
        @Size(max = 14, message = "CPF deve ter no máximo {max} caracteres")
        String cpf,

        @Schema(description = "Código interno único do cliente.", example = "CUS-001",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
        @NotBlank(message = "Código é obrigatório")
        @Size(max = 50, message = "Código deve ter no máximo {max} caracteres")
        String code,

        @Schema(description = "Endereço do cliente.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Endereço é obrigatório")
        @Valid
        AddressDto address,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        RegistrationStatus status
) {
}
