package br.com.toppower.erp_toppower.auth.dto;

import br.com.toppower.erp_toppower.enums.Role;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

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
                    allowableValues = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE"},
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Role role
    ) {
        public static AuthenticatedUser from(UserResponse user) {
            return new AuthenticatedUser(user.uuid(), user.email(), user.role());
        }
    }

    public static LoginResponse of(String accessToken, long expiresIn, UserResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, AuthenticatedUser.from(user));
    }
}
