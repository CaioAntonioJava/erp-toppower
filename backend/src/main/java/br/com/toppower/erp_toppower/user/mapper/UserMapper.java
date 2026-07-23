package br.com.toppower.erp_toppower.user.mapper;

import br.com.toppower.erp_toppower.user.enums.Module;
import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;

import java.util.EnumSet;
import java.util.Set;

public final class UserMapper {

    private UserMapper() {
    }

    /**
     * Cria uma entidade a partir do request de cadastro. A role e os módulos
     * vêm do request (não há mais fixação em ROLE_MANAGER).
     */
    public static User toEntity(UserCreateRequest request, String encodedPassword) {
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(encodedPassword);
        user.setRole(request.role());
        user.setModules(request.modules() == null ? Set.of() : EnumSet.copyOf(request.modules()));
        return user;
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(),
                effectiveModules(user));
    }

    /**
     * Módulos efetivamente acessíveis ao usuário: todos para ADMIN/MANAGER,
     * apenas os concedidos para EMPLOYEE.
     */
    public static Set<Module> effectiveModules(User user) {
        if (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_MANAGER) {
            return EnumSet.allOf(Module.class);
        }
        return user.getModules() == null ? Set.of() : EnumSet.copyOf(user.getModules());
    }
}