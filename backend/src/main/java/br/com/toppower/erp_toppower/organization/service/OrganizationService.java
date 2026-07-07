package br.com.toppower.erp_toppower.organization.service;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.organization.dto.OrganizationCreateRequest;
import br.com.toppower.erp_toppower.organization.dto.OrganizationResponse;
import br.com.toppower.erp_toppower.organization.dto.OrganizationSummary;
import br.com.toppower.erp_toppower.organization.dto.OrganizationUpdateRequest;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import br.com.toppower.erp_toppower.organization.exception.DuplicateOrganizationCnpjException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationNotFoundException;
import br.com.toppower.erp_toppower.organization.mapper.OrganizationMapper;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import br.com.toppower.erp_toppower.userorganization.entity.UserOrganization;
import br.com.toppower.erp_toppower.userorganization.repository.UserOrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserOrganizationRepository userOrganizationRepository) {
        this.organizationRepository = organizationRepository;
        this.userOrganizationRepository = userOrganizationRepository;
    }

    // =====================================================================
    // CRUD (admin-only no controller)
    // =====================================================================

    @Transactional
    public OrganizationResponse create(OrganizationCreateRequest request) {
        if (organizationRepository.existsByCnpj(request.cnpj())) {
            throw new DuplicateOrganizationCnpjException(request.cnpj());
        }
        Organization org = OrganizationMapper.toEntity(request);
        Organization saved = organizationRepository.save(org);
        return OrganizationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(UUID id) {
        return organizationRepository.findById(id)
                .map(OrganizationMapper::toResponse)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrganizationResponse> getAll(OrganizationStatus status, Pageable pageable) {
        Page<Organization> page = (status == null)
                ? organizationRepository.findAll(pageable)
                : organizationRepository.findByStatus(status, pageable);
        return PagedResponse.from(page.map(OrganizationMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrganizationResponse> search(String query, OrganizationStatus status, Pageable pageable) {
        Page<Organization> page = organizationRepository.search(status, query, pageable);
        return PagedResponse.from(page.map(OrganizationMapper::toResponse));
    }

    @Transactional
    public OrganizationResponse update(UUID id, OrganizationUpdateRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
        OrganizationMapper.applyUpdate(org, request);
        return OrganizationMapper.toResponse(organizationRepository.save(org));
    }

    @Transactional
    public void inactivate(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
        org.setStatus(OrganizationStatus.INATIVO);
        organizationRepository.save(org);
    }

    @Transactional
    public OrganizationResponse activate(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(id));
        org.setStatus(OrganizationStatus.ATIVO);
        return OrganizationMapper.toResponse(organizationRepository.save(org));
    }

    // =====================================================================
    // Listagem do usuário autenticado (para o seletor de Organization)
    // =====================================================================

    /**
     * Lista as Organizations acessíveis pelo usuário autenticado:
     * <ul>
     *   <li>ADMIN (role global) → todas as Organizations ATIVAS;</li>
     *   <li>demais roles → as Organizations ATIVAS vinculadas via
     *       {@code UserOrganization}, com a role por empresa e flag default.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public List<OrganizationSummary> listMine() {
        UserDetailsImpl principal = currentPrincipal();
        if (principal.isAdmin()) {
            return organizationRepository.findAll().stream()
                    .filter(o -> o.getStatus() == OrganizationStatus.ATIVO)
                    .map(OrganizationMapper::toSummary)
                    .toList();
        }
        List<UserOrganization> links = userOrganizationRepository.findActiveByUserUuid(principal.uuid());
        UUID defaultOrgId = userOrganizationRepository
                .findFirstByUserUuidAndIsDefaultTrue(principal.uuid())
                .map(uo -> uo.getOrganization().getUuid())
                .orElse(null);
        return links.stream()
                .map(uo -> OrganizationMapper.toSummary(
                        uo.getOrganization(),
                        uo.getRole(),
                        uo.getOrganization().getUuid().equals(defaultOrgId)))
                .toList();
    }

    /**
     * Resolve a Organization default do usuário autenticado (usada no login
     * para pré-selecionar a org ativa no frontend). Para ADMIN, retorna a
     * primeira Organization ATIVA encontrada (ou null se nenhuma existir).
     */
    @Transactional(readOnly = true)
    public UUID resolveDefaultOrganizationId(UserDetailsImpl principal) {
        if (principal.isAdmin()) {
            return organizationRepository.findAll().stream()
                    .filter(o -> o.getStatus() == OrganizationStatus.ATIVO)
                    .map(Organization::getUuid)
                    .findFirst()
                    .orElse(null);
        }
        return userOrganizationRepository
                .findFirstByUserUuidAndIsDefaultTrue(principal.uuid())
                .map(uo -> uo.getOrganization().getUuid())
                .orElse(null);
    }

    private UserDetailsImpl currentPrincipal() {
        return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}