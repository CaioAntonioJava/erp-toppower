package br.com.toppower.erp_toppower.auth.dto;

import br.com.toppower.erp_toppower.enums.Role;
import br.com.toppower.erp_toppower.user.dto.UserResponse;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUser user
) {
    public record AuthenticatedUser(
            UUID uuid,
            String email,
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
