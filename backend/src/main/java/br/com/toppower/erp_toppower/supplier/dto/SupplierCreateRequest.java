package br.com.toppower.erp_toppower.supplier.dto;

import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "SupplierCreateRequest", description = "Dados para cadastro de um novo fornecedor (sempre PJ).")
public record SupplierCreateRequest(

        @Schema(description = "Razão social.", example = "Fornecedor XPTO Ltda",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 200, message = "Razão social deve ter no máximo {max} caracteres")
        String legalName,

        @Schema(description = "Nome fantasia.", example = "XPTO Distribuidora", maxLength = 200)
        @Size(max = 200, message = "Nome fantasia deve ter no máximo {max} caracteres")
        String tradeName,

        @Schema(description = "CNPJ (14 dígitos com DV).", example = "11.222.333/0001-81",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
        @NotBlank(message = "CNPJ é obrigatório")
        @Size(max = 20, message = "CNPJ deve ter no máximo {max} caracteres")
        String taxId,

        @Schema(description = "Inscrição Estadual.", example = "123.456.789.012", maxLength = 30)
        @Size(max = 30, message = "Inscrição estadual deve ter no máximo {max} caracteres")
        String stateRegistration,

        @Schema(description = "Inscrição Municipal.", example = "9876543", maxLength = 30)
        @Size(max = 30, message = "Inscrição municipal deve ter no máximo {max} caracteres")
        String municipalRegistration,

        @Schema(description = "E-mail de contato.", example = "contato@fornecedorxpto.com.br",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Telefone de contato.", example = "(11) 98765-4321", maxLength = 20)
        @Size(max = 20, message = "Telefone deve ter no máximo {max} caracteres")
        String phone,

        @Schema(description = "Nome da pessoa de contato no fornecedor.", example = "João - Vendas", maxLength = 150)
        @Size(max = 150, message = "Nome do contato deve ter no máximo {max} caracteres")
        String contactName,

        @Schema(description = "Endereço do fornecedor.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Endereço é obrigatório")
        @Valid
        AddressDto address,

        @Schema(description = "Status inicial. Se omitido, assume ATIVO.",
                example = "ATIVO", allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        SupplierStatus status
) {
}
