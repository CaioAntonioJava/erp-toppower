package br.com.toppower.erp_toppower.organization.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Status de uma Organization (empresa) no sistema.
 *
 * <p>Uma Organization {@link #ATIVO} pode ser selecionada como Organization
 * ativa nas requisições; {@link #INATIVO} é mantida para histórico, mas não
 * aceita novas operações de negócio.</p>
 */
@Schema(name = "OrganizationStatus", description = "Status da Organization (empresa).",
        allowableValues = {"ATIVO", "INATIVO"})
public enum OrganizationStatus {
    ATIVO,
    INATIVO
}