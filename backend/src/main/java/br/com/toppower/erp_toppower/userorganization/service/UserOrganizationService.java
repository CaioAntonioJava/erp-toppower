package br.com.toppower.erp_toppower.userorganization.service;

import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.exception.OrganizationNotFoundException;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.exception.UserNotFoundException;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import br.com.toppower.erp_toppower.userorganization.dto.UserOrganizationAssignRequest;
import br.com.toppower.erp_toppower.userorganization.dto.UserOrganizationResponse;
import br.com.toppower.erp_toppower.userorganization.entity.UserOrganization;
import br.com.toppower.erp_toppower.userorganization.exception.DuplicateUserOrganizationException;
import br.com.toppower.erp_toppower.userorganization.exception.UserOrganizationNotFoundException;
import br.com.toppower.erp_toppower.userorganization.mapper.UserOrganizationMapper;
import br.com.toppower.erp_toppower.userorganization.repository.UserOrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserOrganizationService {

    private final UserOrganizationRepository userOrganizationRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public UserOrganizationService(UserOrganizationRepository userOrganizationRepository,
                                   UserRepository userRepository,
                                   OrganizationRepository organizationRepository) {
        this.userOrganizationRepository = userOrganizationRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public UserOrganizationResponse assign(UserOrganizationAssignRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        Organization org = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(request.organizationId()));

        if (userOrganizationRepository.existsByUserUuidAndOrganizationUuid(
                request.userId(), request.organizationId())) {
            throw new DuplicateUserOrganizationException();
        }

        UserOrganization uo = new UserOrganization();
        uo.setUser(user);
        uo.setOrganization(org);
        uo.setRole(request.role());
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        uo.setDefault(makeDefault);

        if (makeDefault) {
            // Garante unicidade de default: desmarca as outras do mesmo usuário.
            userOrganizationRepository.findByUserUuid(request.userId()).stream()
                    .filter(other -> other.isDefault() && !other.getUuid().equals(uo.getUuid()))
                    .forEach(other -> other.setDefault(false));
        }

        UserOrganization saved = userOrganizationRepository.save(uo);
        return UserOrganizationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserOrganizationResponse> listByUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return userOrganizationRepository.findByUserUuid(userId).stream()
                .map(UserOrganizationMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserOrganizationResponse setDefault(UUID userId, UUID organizationId) {
        UserOrganization target = userOrganizationRepository
                .findByUserUuidAndOrganizationUuid(userId, organizationId)
                .orElseThrow(() -> new UserOrganizationNotFoundException(null));

        // Desmarca as outras.
        userOrganizationRepository.findByUserUuid(userId).stream()
                .filter(other -> other.isDefault() && !other.getUuid().equals(target.getUuid()))
                .forEach(other -> other.setDefault(false));
        target.setDefault(true);

        return UserOrganizationMapper.toResponse(userOrganizationRepository.save(target));
    }

    @Transactional
    public void unassign(UUID userOrganizationUuid) {
        UserOrganization uo = userOrganizationRepository.findById(userOrganizationUuid)
                .orElseThrow(() -> new UserOrganizationNotFoundException(userOrganizationUuid));
        userOrganizationRepository.delete(uo);
    }
}