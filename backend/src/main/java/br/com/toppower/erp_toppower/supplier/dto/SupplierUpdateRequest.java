package br.com.toppower.erp_toppower.supplier.dto;

import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial do fornecedor (PATCH). Todos os campos são opcionais.
 * <p>O CNPJ (taxId) NÃO pode ser alterado após o cadastro (identidade fiscal).
 * Para trocar o CNPJ, é necessário inativar o fornecedor atual e criar um novo.</p>
 */
@Schema(name = "SupplierUpdateRequest", description = "Dados para atualização parcial de um fornecedor (PATCH).")
public record SupplierUpdateRequest(

        @Schema(description = "Nova razão social.", maxLength = 200)
        @Size(max = 200, message = "Razão social deve ter no máximo {max} caracteres")
        String legalName,

        @Schema(description = "Novo nome fantasia.", maxLength = 200)
        @Size(max = 200, message = "Nome fantasia deve ter no máximo {max} caracteres")
        String tradeName,

        @Schema(description = "Nova inscrição estadual.", maxLength = 30)
        @Size(max = 30, message = "Inscrição estadual deve ter no máximo {max} caracteres")
        String stateRegistration,

        @Schema(description = "Nova inscrição municipal.", maxLength = 30)
        @Size(max = 30, message = "Inscrição municipal deve ter no máximo {max} caracteres")
        String municipalRegistration,

        @Schema(description = "Novo e-mail de contato.", maxLength = 150)
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Novo telefone de contato.", maxLength = 20)
        @Size(max = 20, message = "Telefone deve ter no máximo {max} caracteres")
        String phone,

        @Schema(description = "Novo nome do contato.", maxLength = 150)
        @Size(max = 150, message = "Nome do contato deve ter no máximo {max} caracteres")
        String contactName,

        @Schema(description = "Novo endereço (substitui o atual).")
        @Valid
        AddressDto address,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        SupplierStatus status
) {
}
