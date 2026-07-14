package br.com.toppower.erp_toppower.auth.dto;

import br.com.toppower.erp_toppower.organization.dto.OrganizationSummary;
import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "LoginResponse", description = "Resposta do login com o token JWT, dados do usuário e Organizations acessíveis.")
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
        AuthenticatedUser user,

        @Schema(description = "Organizations acessíveis ao usuário (para o seletor do frontend).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<OrganizationSummary> organizations,

        @Schema(description = "ID da Organization default (pré-selecionada após o login). Pode ser null.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long defaultOrganizationId
) {
    @Schema(name = "AuthenticatedUser", description = "Dados do usuário autenticado retornados pelo login.")
    public record AuthenticatedUser(

            @Schema(description = "ID do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
            Long id,

            @Schema(description = "E-mail do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
            String email,

            @Schema(description = "Papel do usuário.",
                    example = "ROLE_ADMIN",
                    allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER"},
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Role role
    ) {
        public static AuthenticatedUser from(UserResponse user) {
            return new AuthenticatedUser(user.id(), user.email(), user.role());
        }
    }

    public static LoginResponse of(String accessToken, long expiresIn, UserResponse user,
                                   List<OrganizationSummary> organizations, Long defaultOrganizationId) {
        return new LoginResponse(accessToken, "Bearer", expiresIn,
                AuthenticatedUser.from(user), organizations, defaultOrganizationId);
    }
}