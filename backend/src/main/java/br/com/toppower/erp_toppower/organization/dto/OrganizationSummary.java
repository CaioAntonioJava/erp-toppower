package br.com.toppower.erp_toppower.organization.dto;

import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import br.com.toppower.erp_toppower.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumo de uma Organization, usado no seletor do frontend e na resposta
 * de login. Inclui, quando aplicável, a role do usuário NAQUELA Organization
 * (vinda de {@code UserOrganization}) e se ela é a default do usuário.
 */
@Schema(name = "OrganizationSummary", description = "Resumo de uma Organization para o seletor.")
public record OrganizationSummary(

        @Schema(description = "ID da Organization.", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Razão social.", requiredMode = Schema.RequiredMode.REQUIRED)
        String corporateName,

        @Schema(description = "Nome fantasia.", requiredMode = Schema.RequiredMode.REQUIRED)
        String tradeName,

        @Schema(description = "CNPJ.", requiredMode = Schema.RequiredMode.REQUIRED)
        String cnpj,

        @Schema(description = "URL pública do logo da Organization "
                + "(ex.: /logos/<uuid>.png). Pode ser nula.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String logoUrl,

        @Schema(description = "Status.", requiredMode = Schema.RequiredMode.REQUIRED)
        OrganizationStatus status,

        @Schema(description = "Prefixo do código das Propostas Técnicas emitidas "
                + "por esta Organization.", example = "PT",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String proposalPrefix,

        @Schema(description = "Prefixo do código dos Contratos emitidos por esta "
                + "Organization.", example = "CT",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String contractPrefix,

        @Schema(description = "Texto HTML padrão pré-preenchido na descrição "
                + "de novos contratos. Pode ser nulo.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String contractDefaultDescription,

        @Schema(description = "Papel do usuário nesta Organization (quando aplicável).")
        Role role,

        @Schema(description = "Indica se esta é a Organization default do usuário.")
        boolean isDefault
) {
}