package br.com.toppower.erp_toppower.auth.dto;

import br.com.toppower.erp_toppower.tenant.dto.TenantSummary;
import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "LoginResponse", description = "Resposta do login com o token JWT e dados do usuário autenticado.")
public record LoginResponse(

        @Schema(description = "Token JWT gerado. Enviar no header Authorization como: Bearer <token>.",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,

        @Schema(description = "Tipo do token. Sempre 'Bearer' para esta API.",
                example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
        String tokenType,

        @Schema(description = "Tempo de expiracao do token em segundos.",
                example = "86400", requiredMode = Schema.RequiredMode.REQUIRED)
        long expiresIn,

        @Schema(description = "Dados do usuário autenticado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AuthenticatedUser user
) {
    @Schema(name = "AuthenticatedUser", description = "Dados do usuário autenticado retornados pelo login.")
    public record AuthenticatedUser(

            @Schema(description = "UUID do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
            UUID uuid,

            @Schema(description = "E-mail do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
            String email,

            @Schema(description = "Papel do usuário.",
                    example = "ROLE_ADMIN",
                    allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER"},
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Role role,

            @Schema(description = "UUID do tenant selecionado para a sessão corrente.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            UUID tenantUuid,

            @Schema(description = "Lista de tenants aos quais o usuário tem acesso. "
                    + "Permite ao frontend montar o seletor de empresa (switch) sem chamada extra.")
            List<TenantSummary> tenants
    ) {
        public static AuthenticatedUser from(UserResponse user, UUID tenantUuid, List<TenantSummary> tenants) {
            return new AuthenticatedUser(user.uuid(), user.email(), user.role(), tenantUuid, tenants);
        }
    }

    public static LoginResponse of(String accessToken, long expiresIn, UserResponse user,
                                   UUID tenantUuid, List<TenantSummary> tenants) {
        return new LoginResponse(accessToken, "Bearer", expiresIn,
                AuthenticatedUser.from(user, tenantUuid, tenants));
    }
}
