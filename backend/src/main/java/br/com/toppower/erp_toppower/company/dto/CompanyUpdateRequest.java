package br.com.toppower.erp_toppower.company.dto;

import br.com.toppower.erp_toppower.common.dto.AddressDto;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial de uma empresa (PATCH). Todos os campos são opcionais:
 * envie apenas os campos que deseja alterar.
 *
 * <p>O CNPJ NÃO pode ser alterado após o cadastro (mudaria a identidade
 * fiscal da empresa). Para trocar o CNPJ, é necessário inativar a empresa
 * atual e criar uma nova.</p>
 */
@Schema(name = "CompanyUpdateRequest", description = "Dados para atualização parcial de uma empresa (PATCH).")
public record CompanyUpdateRequest(

        @Schema(description = "Nova razão social.", maxLength = 200)
        @Size(max = 200, message = "Razão social deve ter no máximo {max} caracteres")
        String legalName,

        @Schema(description = "Novo nome fantasia.", maxLength = 200)
        @Size(max = 200, message = "Nome fantasia deve ter no máximo {max} caracteres")
        String tradeName,

        @Schema(description = "Nova inscrição estadual.", maxLength = 30)
        @Size(max = 30, message = "Inscrição estadual deve ter no máximo {max} caracteres")
        String stateRegistration,

        @Schema(description = "Indica se a empresa é ISENTA de Inscrição Estadual (IE Isento). "
                + "Envie true para marcar como isenta, false para desmarcar. "
                + "Quando omitido, o valor atual é preservado.",
                example = "false")
        Boolean stateRegistrationExempt,

        @Schema(description = "Nova inscrição municipal.", maxLength = 30)
        @Size(max = 30, message = "Inscrição municipal deve ter no máximo {max} caracteres")
        String municipalRegistration,

        @Schema(description = "Novo endereço (substitui o atual).")
        @Valid
        AddressDto address,

        @Schema(description = "Novo status.",
                allowableValues = {"ATIVO", "INATIVO"})
        RegistrationStatus status
) {
}
