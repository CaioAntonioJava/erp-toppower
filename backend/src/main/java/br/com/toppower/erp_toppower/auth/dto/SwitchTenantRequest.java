package br.com.toppower.erp_toppower.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "SwitchTenantRequest",
        description = "Requisição para trocar o tenant (empresa) da sessão corrente, "
                + "reemitindo o JWT com o novo tenant. Requer que o usuário esteja vinculado ao tenant.")
public record SwitchTenantRequest(

        @Schema(description = "UUID do tenant de destino.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "tenantUuid é obrigatório")
        UUID tenantUuid
) {
}