package br.com.toppower.erp_toppower.user.mapper;

import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.enums.Module;
import br.com.toppower.erp_toppower.user.enums.Role;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link UserMapper}.
 *
 * <p>Cobre toEntity, toResponse e effectiveModules.</p>
 */
class UserMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        UserCreateRequest request = new UserCreateRequest(
                "user@email.com", "senha123", "senha123",
                Role.ROLE_MANAGER, EnumSet.of(Module.MODULE_COMPANIES, Module.MODULE_CUSTOMERS));

        User result = UserMapper.toEntity(request, "encodedPassword");

        assertEquals("user@email.com", result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(Role.ROLE_MANAGER, result.getRole());
        assertEquals(Set.of(Module.MODULE_COMPANIES, Module.MODULE_CUSTOMERS), result.getModules());
    }

    @Test
    void toEntity_modulesNulo_usaSetVazio() {
        UserCreateRequest request = new UserCreateRequest(
                "user@email.com", "senha123", "senha123",
                Role.ROLE_EMPLOYEE, null);

        User result = UserMapper.toEntity(request, "encoded");
        assertTrue(result.getModules().isEmpty());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@email.com");
        user.setRole(Role.ROLE_EMPLOYEE);
        user.setModules(EnumSet.of(Module.MODULE_COMPANIES));

        var response = UserMapper.toResponse(user);

        assertEquals(1L, response.id());
        assertEquals("user@email.com", response.email());
        assertEquals(Role.ROLE_EMPLOYEE, response.role());
        assertEquals(Set.of(Module.MODULE_COMPANIES), response.modules());
    }

    @Test
    void effectiveModules_admin_retornaTodos() {
        User user = new User();
        user.setRole(Role.ROLE_ADMIN);
        user.setModules(EnumSet.of(Module.MODULE_COMPANIES));

        Set<Module> result = UserMapper.effectiveModules(user);
        assertEquals(EnumSet.allOf(Module.class), result);
    }

    @Test
    void effectiveModules_manager_retornaTodos() {
        User user = new User();
        user.setRole(Role.ROLE_MANAGER);
        user.setModules(EnumSet.of(Module.MODULE_COMPANIES));

        Set<Module> result = UserMapper.effectiveModules(user);
        assertEquals(EnumSet.allOf(Module.class), result);
    }

    @Test
    void effectiveModules_employee_retornaApenasConcedidos() {
        User user = new User();
        user.setRole(Role.ROLE_EMPLOYEE);
        user.setModules(EnumSet.of(Module.MODULE_COMPANIES, Module.MODULE_CUSTOMERS));

        Set<Module> result = UserMapper.effectiveModules(user);
        assertEquals(Set.of(Module.MODULE_COMPANIES, Module.MODULE_CUSTOMERS), result);
    }

    @Test
    void effectiveModules_employeeModulesNulo_retornaVazio() {
        User user = new User();
        user.setRole(Role.ROLE_EMPLOYEE);
        user.setModules(null);

        Set<Module> result = UserMapper.effectiveModules(user);
        assertTrue(result.isEmpty());
    }
}
