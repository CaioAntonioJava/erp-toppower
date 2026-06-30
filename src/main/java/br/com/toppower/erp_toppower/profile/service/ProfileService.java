package br.com.toppower.erp_toppower.profile.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.profile.dto.ProfileCreateRequest;
import br.com.toppower.erp_toppower.profile.dto.ProfileResponse;
import br.com.toppower.erp_toppower.profile.dto.ProfileUpdateRequest;
import br.com.toppower.erp_toppower.profile.entity.Profile;
import br.com.toppower.erp_toppower.profile.enums.ProfileStatus;
import br.com.toppower.erp_toppower.profile.exception.DuplicateProfileCpfException;
import br.com.toppower.erp_toppower.profile.exception.DuplicateProfileEmailException;
import br.com.toppower.erp_toppower.profile.exception.ProfileNotFoundException;
import br.com.toppower.erp_toppower.profile.exception.UserAlreadyHasProfileException;
import br.com.toppower.erp_toppower.profile.mapper.ProfileMapper;
import br.com.toppower.erp_toppower.profile.repository.ProfileRepository;
import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.exception.UserNotFoundException;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProfileResponse create(ProfileCreateRequest request, UUID userId) {
        if (profileRepository.existsByCpf(request.cpf())) {
            throw new DuplicateProfileCpfException(request.cpf());
        }
        if (profileRepository.existsByEmail(request.email())) {
            throw new DuplicateProfileEmailException(request.email());
        }
        if (profileRepository.existsByUserUuid(userId)) {
            throw new UserAlreadyHasProfileException(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Profile profile = ProfileMapper.toEntity(request, user);
        Profile saved = profileRepository.save(profile);
        return ProfileMapper.toResponse(saved);
    }

    /**
     * Lista paginada de perfis. Se {@code status} for nulo, retorna todos
     * (ativos e inativos); caso contrario filtra pelo status informado.
     * Acesso restrito a administradores.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProfileResponse> getAll(ProfileStatus status, Pageable pageable, UserDetailsImpl principal) {
        if (!principal.isAdmin()) {
            throw new AccessDeniedException("Apenas administradores podem listar todos os perfis");
        }
        Page<Profile> page = (status == null)
                ? profileRepository.findAll(pageable)
                : profileRepository.findByStatus(status, pageable);
        Page<ProfileResponse> mapped = page.map(ProfileMapper::toResponse);
        return PagedResponse.from(mapped);
    }

    /**
     * Busca perfil por ID. ADMIN pode consultar qualquer perfil;
     * demais usuarios so podem consultar o proprio.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getById(UUID id, UserDetailsImpl principal) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
        validateAccess(profile, principal);
        return ProfileMapper.toResponse(profile);
    }

    /**
     * Busca perfil pelo UUID do usuario. ADMIN pode consultar de qualquer;
     * demais usuarios so podem consultar o proprio.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getByUserId(UUID userId, UserDetailsImpl principal) {
        if (!principal.isAdmin() && !principal.uuid().equals(userId)) {
            throw new AccessDeniedException("Voce so pode consultar o seu proprio perfil");
        }
        return profileRepository.findByUserUuid(userId)
                .map(ProfileMapper::toResponse)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }

    /**
     * Atualizacao parcial do perfil. ADMIN pode alterar qualquer perfil;
     * demais usuarios so podem alterar o proprio.
     */
    @Transactional
    public ProfileResponse update(UUID id, ProfileUpdateRequest request, UserDetailsImpl principal) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));

        validateAccess(profile, principal);

        if (request.cpf() != null && !request.cpf().equals(profile.getCpf())) {
            if (profileRepository.existsByCpf(request.cpf())) {
                throw new DuplicateProfileCpfException(request.cpf());
            }
        }

        if (request.email() != null && !request.email().equals(profile.getEmail())) {
            if (profileRepository.existsByEmail(request.email())) {
                throw new DuplicateProfileEmailException(request.email());
            }
        }

        ProfileMapper.applyUpdate(profile, request);
        Profile saved = profileRepository.save(profile);
        return ProfileMapper.toResponse(saved);
    }

    /**
     * Soft delete: nao remove fisicamente o registro, apenas altera o status para INATIVO.
     * Preserva o historico de auditoria e o vinculo com o User.
     * Acesso restrito a administradores.
     */
    @Transactional
    public void softDelete(UUID id, UserDetailsImpl principal) {
        if (!principal.isAdmin()) {
            throw new AccessDeniedException("Apenas administradores podem inativar perfis");
        }
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
        profile.setStatus(ProfileStatus.INATIVO);
        profileRepository.save(profile);
    }

    /**
     * Verifica se o usuario autenticado pode acessar (ler/editar) o perfil informado.
     * ADMIN tem acesso total. Demais usuarios so podem acessar o proprio perfil
     * (aquele cujo {@code user.uuid} corresponde ao UUID do principal).
     */
    private void validateAccess(Profile profile, UserDetailsImpl principal) {
        if (principal.isAdmin()) {
            return;
        }
        if (profile.getUser() != null && profile.getUser().getUuid().equals(principal.uuid())) {
            return;
        }
        throw new AccessDeniedException("Voce so pode acessar o seu proprio perfil");
    }
}
