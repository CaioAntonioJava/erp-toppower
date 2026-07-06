package br.com.toppower.erp_toppower.auth.service;

import br.com.toppower.erp_toppower.auth.dto.LoginRequest;
import br.com.toppower.erp_toppower.auth.dto.LoginResponse;
import br.com.toppower.erp_toppower.auth.dto.SwitchTenantRequest;
import br.com.toppower.erp_toppower.auth.exception.InvalidCredentialsException;
import br.com.toppower.erp_toppower.auth.exception.InvalidTenantException;
import br.com.toppower.erp_toppower.security.JwtService;
import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import br.com.toppower.erp_toppower.tenant.dto.TenantSummary;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.entity.UserTenant;
import br.com.toppower.erp_toppower.user.mapper.UserMapper;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import br.com.toppower.erp_toppower.user.repository.UserTenantRepository;
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
    private final UserTenantRepository userTenantRepository;
    private final TenantQueryService tenantQueryService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository,
                       UserTenantRepository userTenantRepository,
                       TenantQueryService tenantQueryService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userTenantRepository = userTenantRepository;
        this.tenantQueryService = tenantQueryService;
    }

    /**
     * Autentica o usuário para um tenant específico. O fluxo:
     * <ol>
     *   <li>Autentica email+senha (Spring Security).</li>
     *   <li>Valida que o usuário está vinculado ao {@code tenantUuid} informado
     *       (existe registro em {@code user_tenants}). Se não, lança
     *       {@link InvalidTenantException} (tratado como 401, sem revelar
     *       diferencialmente se foi email/senha vs. tenant).</li>
     *   <li>Emite o JWT contendo o claim {@code tenant} = {@code tenantUuid}.</li>
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

        // Valida vínculo usuário↔tenant antes de emitir o token.
        if (!userTenantRepository.existsByUserUuidAndTenantUuid(user.getUuid(), request.tenantUuid())) {
            throw new InvalidTenantException();
        }

        // Reconstroi o principal com o tenant da sessão (para o claim "tenant" ir no JWT).
        UserDetailsImpl principal = UserDetailsImpl.from(user, request.tenantUuid());
        String token = jwtService.generateToken(principal);

        UserResponse userResponse = UserMapper.toResponse(user);
        List<TenantSummary> tenants = resolveTenants(user.getUuid());

        return LoginResponse.of(token, jwtService.getExpirationSeconds(), userResponse,
                request.tenantUuid(), tenants);
    }

    /**
     * Troca o tenant da sessão corrente, reemitindo o JWT com o novo tenant.
     * Requer autenticação prévia ( usuário já logado em outro tenant ).
     */
    @Transactional(readOnly = true)
    public LoginResponse switchTenant(SwitchTenantRequest request, UserDetailsImpl current) {
        UUID userUuid = current.uuid();
        if (!userTenantRepository.existsByUserUuidAndTenantUuid(userUuid, request.tenantUuid())) {
            throw new InvalidTenantException();
        }
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no banco"));
        UserDetailsImpl principal = UserDetailsImpl.from(user, request.tenantUuid());
        String token = jwtService.generateToken(principal);

        UserResponse userResponse = UserMapper.toResponse(user);
        List<TenantSummary> tenants = resolveTenants(userUuid);

        return LoginResponse.of(token, jwtService.getExpirationSeconds(), userResponse,
                request.tenantUuid(), tenants);
    }

    private List<TenantSummary> resolveTenants(UUID userUuid) {
        return tenantQueryService.listTenantsByUserUuid(userUuid);
    }
}