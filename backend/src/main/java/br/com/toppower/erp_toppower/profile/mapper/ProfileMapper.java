package br.com.toppower.erp_toppower.profile.mapper;

import br.com.toppower.erp_toppower.profile.dto.ProfileCreateRequest;
import br.com.toppower.erp_toppower.profile.dto.ProfileResponse;
import br.com.toppower.erp_toppower.profile.dto.ProfileUpdateRequest;
import br.com.toppower.erp_toppower.profile.entity.Profile;
import br.com.toppower.erp_toppower.user.entity.User;

public final class ProfileMapper {

    private ProfileMapper() {
    }

    /**
     * Cria uma nova entidade a partir do request de criação.
     * O {@link User} deve ser resolvido pelo service antes de chamar este método.
     * O {@code status} pode ser {@code null}; o {@code @PrePersist} da entidade
     * cuida de aplicar o default {@code ATIVO}.
     */
    public static Profile toEntity(ProfileCreateRequest request, User user) {
        Profile profile = new Profile();
        profile.setName(request.name());
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        profile.setCpf(request.cpf());
        profile.setStatus(request.status());
        profile.setUser(user);
        return profile;
    }

    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getCpf(),
                profile.getStatus(),
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getCreatedBy(),
                profile.getUpdatedBy()
        );
    }

    /**
     * Aplica uma atualização parcial (PATCH) na entidade carregada.
     * Apenas campos não nulos do request sobrescrevem o estado atual.
     * O vínculo com o User NÃO pode ser alterado por este método.
     */
    public static void applyUpdate(Profile profile, ProfileUpdateRequest request) {
        if (request.name() != null) {
            profile.setName(request.name());
        }
        if (request.email() != null) {
            profile.setEmail(request.email());
        }
        if (request.phone() != null) {
            profile.setPhone(request.phone());
        }
        if (request.cpf() != null) {
            profile.setCpf(request.cpf());
        }
        if (request.status() != null) {
            profile.setStatus(request.status());
        }
    }
}
