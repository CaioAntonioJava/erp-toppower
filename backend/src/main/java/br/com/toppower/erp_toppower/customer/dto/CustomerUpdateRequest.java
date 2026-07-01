package br.com.toppower.erp_toppower.customer.dto;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.common.validation.ValidCpf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial de um cliente pessoa física (PATCH). Todos os campos
 * são opcionais: envie apenas os campos que deseja alterar.
 *
 * <p>O CPF NÃO pode ser alterado após o cadastro (mudaria a identidade
 * fiscal da pessoa). Para trocar o CPF, é necessário inativar o cliente
 * atual e criar um novo.</p>
 */
@Schema(name = "CustomerUpdateRequest", description = "Dados para atualização parcial de um cliente (PATCH).")
public record CustomerUpdateRequest(

        @Schema(description = "Novo nome completo.", maxLength = 150)
        @Size(max = 150, message = "Nome deve ter no máximo {max} caracteres")
        String name,

        @Schema(description = "Novo e-mail de contato.", maxLength = 150)
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo {max} caracteres")
        String email,

        @Schema(description = "Novo telefone de contato.", maxLength = 20)
        @Size(max = 20, message = "Telefone deve ter no máximo {max} caracteres")
        String phone,

        @Schema(description = "Novo CPF.", maxLength = 14)
        @ValidCpf(message = "CPF inválido")
        @Size(max = 14, message = "CPF deve ter no máximo {max} caracteres")
        String cpf,

        @Schema(description = "Novo endereço (substitui o atual).")
        @Valid
        AddressDto address,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        RegistrationStatus status
) {
}
