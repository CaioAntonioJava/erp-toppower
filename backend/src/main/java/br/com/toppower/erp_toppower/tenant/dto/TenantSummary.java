package br.com.toppower.erp_toppower.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Resumo de um tenant, usado em contextos onde não é necessário retornar
 * todos os dados (ex.: dropdown da tela de login, listagem de tenants do
 * usuário logado para troca de empresa).
 */
@Schema(name = "TenantSummary", description = "Resumo de um tenant para seleção/troca de empresa.")
public record TenantSummary(

        @Schema(description = "Identificador único (UUID) do tenant.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Nome fantasia (ou razão social quando nome fantasia é nulo).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String displayName,

        @Schema(description = "Código interno do tenant.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "CNPJ do tenant.")
        String cnpj
) {
}