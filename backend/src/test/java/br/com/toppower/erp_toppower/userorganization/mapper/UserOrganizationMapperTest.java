package br.com.toppower.erp_toppower.userorganization.mapper;

import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.userorganization.entity.UserOrganization;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link UserOrganizationMapper}.
 *
 * <p>Cobre toResponse.</p>
 */
class UserOrganizationMapperTest {

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@email.com");

        Organization org = new Organization();
        org.setId(10L);
        org.setCorporateName("Empresa Ltda");

        UserOrganization uo = new UserOrganization();
        uo.setId(99L);
        uo.setUser(user);
        uo.setOrganization(org);
        uo.setRole(Role.ROLE_MANAGER);
        uo.setDefault(true);

        var response = UserOrganizationMapper.toResponse(uo);

        assertEquals(99L, response.id());
        assertEquals(1L, response.userId());
        assertEquals("user@email.com", response.userEmail());
        assertEquals(10L, response.organizationId());
        assertEquals("Empresa Ltda", response.organizationCorporateName());
        assertEquals(Role.ROLE_MANAGER, response.role());
        assertTrue(response.isDefault());
    }
}
