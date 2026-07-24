package br.com.toppower.erp_toppower.profile.mapper;

import br.com.toppower.erp_toppower.profile.dto.ProfileCreateRequest;
import br.com.toppower.erp_toppower.profile.dto.ProfileUpdateRequest;
import br.com.toppower.erp_toppower.profile.entity.Profile;
import br.com.toppower.erp_toppower.profile.enums.ProfileStatus;
import br.com.toppower.erp_toppower.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários de {@link ProfileMapper}.
 *
 * <p>Cobre toEntity, toResponse e applyUpdate.</p>
 */
class ProfileMapperTest {

    @Test
    void toEntity_mapeiaCamposCorretamente() {
        User user = new User();
        user.setId(1L);

        ProfileCreateRequest request = new ProfileCreateRequest(
                "João Perfil", "joao@email.com", "11999999999", "123.456.789-09",
                ProfileStatus.ATIVO);

        Profile result = ProfileMapper.toEntity(request, user);

        assertEquals("João Perfil", result.getName());
        assertEquals("joao@email.com", result.getEmail());
        assertEquals("11999999999", result.getPhone());
        assertEquals("123.456.789-09", result.getCpf());
        assertEquals(ProfileStatus.ATIVO, result.getStatus());
        assertNotNull(result.getUser());
        assertEquals(1L, result.getUser().getId());
    }

    @Test
    void toEntity_statusNulo_naoAplicaDefault() {
        User user = new User();
        user.setId(1L);

        ProfileCreateRequest request = new ProfileCreateRequest(
                "João", "joao@email.com", "11999999999", "123.456.789-09", null);

        Profile result = ProfileMapper.toEntity(request, user);
        assertNull(result.getStatus());
    }

    @Test
    void toResponse_mapeiaCamposCorretamente() {
        User user = new User();
        user.setId(42L);

        Profile profile = new Profile();
        profile.setId(1L);
        profile.setName("Maria Perfil");
        profile.setEmail("maria@email.com");
        profile.setPhone("11888888888");
        profile.setCpf("529.982.247-25");
        profile.setStatus(ProfileStatus.ATIVO);
        profile.setUser(user);

        var response = ProfileMapper.toResponse(profile);

        assertEquals(1L, response.id());
        assertEquals("Maria Perfil", response.name());
        assertEquals("maria@email.com", response.email());
        assertEquals("529.982.247-25", response.cpf());
        assertEquals(ProfileStatus.ATIVO, response.status());
        assertEquals(42L, response.userId());
    }

    @Test
    void toResponse_userNulo_userIdNulo() {
        Profile profile = new Profile();
        profile.setId(1L);
        profile.setName("Sem User");
        profile.setEmail("sem@email.com");
        profile.setCpf("123.456.789-09");
        profile.setStatus(ProfileStatus.ATIVO);
        profile.setUser(null);

        var response = ProfileMapper.toResponse(profile);
        assertNull(response.userId());
    }

    @Test
    void applyUpdate_camposNaoNulos_atualiza() {
        Profile profile = new Profile();
        profile.setName("Original");
        profile.setEmail("original@email.com");
        profile.setPhone("11111111111");
        profile.setCpf("123.456.789-09");
        profile.setStatus(ProfileStatus.ATIVO);

        ProfileUpdateRequest update = new ProfileUpdateRequest(
                "Novo Nome", "novo@email.com", "22222222222",
                "529.982.247-25", ProfileStatus.INATIVO);

        ProfileMapper.applyUpdate(profile, update);

        assertEquals("Novo Nome", profile.getName());
        assertEquals("novo@email.com", profile.getEmail());
        assertEquals("22222222222", profile.getPhone());
        assertEquals("529.982.247-25", profile.getCpf());
        assertEquals(ProfileStatus.INATIVO, profile.getStatus());
    }

    @Test
    void applyUpdate_camposNulos_naoAltera() {
        Profile profile = new Profile();
        profile.setName("Original");
        profile.setStatus(ProfileStatus.ATIVO);

        ProfileUpdateRequest update = new ProfileUpdateRequest(
                null, null, null, null, null);

        ProfileMapper.applyUpdate(profile, update);

        assertEquals("Original", profile.getName());
        assertEquals(ProfileStatus.ATIVO, profile.getStatus());
    }
}
