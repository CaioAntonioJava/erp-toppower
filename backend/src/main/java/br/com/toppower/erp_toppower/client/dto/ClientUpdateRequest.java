package br.com.toppower.erp_toppower.client.dto;

import br.com.toppower.erp_toppower.client.enums.ClientStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial do cliente (PATCH). Todos os campos são opcionais:
 * envie apenas os campos que deseja alterar.
 * <p>O tipo de pessoa e o taxId NÃO podem ser alterados após o cadastro
 * (mudaria a identidade fiscal do cliente). Para trocar o tipo, é
 * necessário inativar o cliente atual e criar um novo.</p>
 */
@Schema(name = "ClientUpdateRequest", description = "Dados para atualização parcial de um cliente (PATCH).")
public record ClientUpdateRequest(

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

        @Schema(description = "Novo endereço (substitui o atual).")
        @Valid
        AddressDto address,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        ClientStatus status
) {
}
