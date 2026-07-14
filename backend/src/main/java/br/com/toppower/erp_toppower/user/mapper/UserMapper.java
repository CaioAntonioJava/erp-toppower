package br.com.toppower.erp_toppower.user.mapper;

import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Cria uma entidade a partir do request de cadastro.
     * A role é SEMPRE {@link Role#ROLE_MANAGER}; promoções para ADMIN são feitas
     * diretamente no banco (ou via endpoint dedicado a ser implementado).
     */
    public static User toEntity(UserCreateRequest request, String encodedPassword) {
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(encodedPassword);
        user.setRole(Role.ROLE_MANAGER);
        return user;
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}