package br.com.toppower.erp_toppower.auth.service;

import br.com.toppower.erp_toppower.auth.dto.LoginRequest;
import br.com.toppower.erp_toppower.auth.dto.LoginResponse;
import br.com.toppower.erp_toppower.auth.exception.InvalidCredentialsException;
import br.com.toppower.erp_toppower.organization.dto.OrganizationSummary;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import br.com.toppower.erp_toppower.organization.mapper.OrganizationMapper;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import br.com.toppower.erp_toppower.security.JwtService;
import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.mapper.UserMapper;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import br.com.toppower.erp_toppower.userorganization.entity.UserOrganization;
import br.com.toppower.erp_toppower.userorganization.repository.UserOrganizationRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       UserOrganizationRepository userOrganizationRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.userOrganizationRepository = userOrganizationRepository;
    }

    /**
     * Autentica o usuário pelo e-mail e senha e emite um JWT. Fluxo:
     * <ol>
     *   <li>Autentica email+senha (Spring Security).</li>
     *   <li>Emite o JWT contendo o claim {@code role}.</li>
     *   <li>Monta a lista de Organizations acessíveis ao usuário + a default,
     *       para o frontend pré-selecionar a Organization ativa.</li>
     * </ol>
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        UserDetailsImpl authenticated = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByEmail(authenticated.email())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no banco"));

        UserDetailsImpl principal = UserDetailsImpl.from(user);
        String token = jwtService.generateToken(principal);

        // --- Organizations acessíveis + default ---
        List<OrganizationSummary> organizations;
        UUID defaultOrganizationId;
        if (principal.isAdmin()) {
            // ADMIN global: vê todas as Organizations ATIVAS, sem vínculo.
            List<Organization> active = organizationRepository.findAll().stream()
                    .filter(o -> o.getStatus() == OrganizationStatus.ATIVO)
                    .toList();
            organizations = active.stream().map(OrganizationMapper::toSummary).toList();
            defaultOrganizationId = active.stream().findFirst().map(Organization::getUuid).orElse(null);
        } else {
            List<UserOrganization> links = userOrganizationRepository.findActiveByUserUuid(user.getUuid());
            UUID defaultUuid = userOrganizationRepository
                    .findFirstByUserUuidAndIsDefaultTrue(user.getUuid())
                    .map(uo -> uo.getOrganization().getUuid())
                    .orElse(null);
            final UUID finalDefault = defaultUuid;
            organizations = links.stream()
                    .map(uo -> OrganizationMapper.toSummary(
                            uo.getOrganization(),
                            uo.getRole(),
                            uo.getOrganization().getUuid().equals(finalDefault)))
                    .toList();
            defaultOrganizationId = defaultUuid;
        }

        UserResponse userResponse = UserMapper.toResponse(user);
        return LoginResponse.of(token, jwtService.getExpirationSeconds(), userResponse,
                organizations, defaultOrganizationId);
    }
}