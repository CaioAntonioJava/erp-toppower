package br.com.toppower.erp_toppower.user.mapper;

import br.com.toppower.erp_toppower.enums.Role;
import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toEntity(UserCreateRequest request, String encodedPassword) {
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(encodedPassword);
        user.setRole(request.role() == null ? Role.ROLE_EMPLOYEE : request.role());
        return user;
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getUuid(), user.getEmail(), user.getRole());
    }
}
