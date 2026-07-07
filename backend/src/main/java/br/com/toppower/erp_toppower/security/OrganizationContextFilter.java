package br.com.toppower.erp_toppower.security;

import br.com.toppower.erp_toppower.common.context.OrganizationContext;
import br.com.toppower.erp_toppower.organization.entity.Organization;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import br.com.toppower.erp_toppower.organization.exception.InvalidOrganizationHeaderException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationAccessDeniedException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationContextRequiredException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationInactiveException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationNotFoundException;
import br.com.toppower.erp_toppower.organization.repository.OrganizationRepository;
import br.com.toppower.erp_toppower.userorganization.repository.UserOrganizationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro que popula o {@link OrganizationContext} a partir do header
 * {@code X-Organization-Id} da requisição, validando:
 * <ol>
 *   <li>o usuário está autenticado;</li>
 *   <li>a Organization existe;</li>
 *   <li>a Organization está ATIVA;</li>
 *   <li>o usuário possui acesso (ADMIN = sempre sim; demais = existe
 *       vínculo em {@code user_organizations}).</li>
 * </ol>
 *
 * <p>Registrado <b>depois</b> do {@code JwtAuthenticationFilter}, de forma que
 * o principal já esteja resolvido em {@link SecurityContextHolder}.</p>
 *
 * <p>Paths isentos do header obrigatório (Organization ativa opcional):</p>
 * <ul>
 *   <li>{@code /api/v1/auth/login} — público;</li>
 *   <li>{@code /api/v1/me} — retorna dados do usuário, não da org;</li>
 *   <li>{@code /api/v1/organizations/**} — gestão de Organizations (o admin
 *       pode listar todas sem org ativa);</li>
 *   <li>{@code /api/v1/user-organizations/**} — gestão de vínculos;</li>
 *   <li>{@code /api/v1/profiles/**} — perfis não são isolados por Organization
 *       (a busca é por {@code userUuid} e a proteção de "ler só o próprio
 *       perfil" fica no service). Exigir o header aqui quebra o fluxo de
 *       primeiro acesso de um MANAGER antes de haver org ativa selecionada.</li>
 *   <li>Swagger/OpenAPI.</li>
 * </ul>
 *
 * <p>Para {@code ROLE_ADMIN}, o header é opcional: se ausente, segue sem
 * contexto (filter desabilitado → vê tudo). Para demais roles, o header é
 * obrigatório e a falta gera 400 {@link OrganizationContextRequiredException}.</p>
 */
@Component
public class OrganizationContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OrganizationContextFilter.class);

    private static final String ORGANIZATION_HEADER = "X-Organization-Id";

    /**
     * Paths em que a Organization ativa NÃO é obrigatória (header opcional).
     * A validação de acesso ainda ocorre se o header estiver presente.
     */
    private static final String[] ORG_OPTIONAL_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/me",
            "/api/v1/organizations",
            "/api/v1/user-organizations",
            // Perfis não são isolados por Organization (busca por userUuid;
            // a proteção "ler só o próprio perfil" fica no service). Exigir o
            // header aqui quebra o primeiro acesso de MANAGER sem org ativa.
            "/api/v1/profiles",
            // Swagger / OpenAPI
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars"
    };

    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    public OrganizationContextFilter(OrganizationRepository organizationRepository,
                                     UserOrganizationRepository userOrganizationRepository) {
        this.organizationRepository = organizationRepository;
        this.userOrganizationRepository = userOrganizationRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UserDetailsImpl;

            String header = request.getHeader(ORGANIZATION_HEADER);
            boolean orgOptional = isOrgOptional(request.getRequestURI());

            if (!isAuthenticated) {
                // Requisição pública (ex: /auth/login). Não há contexto de org.
                filterChain.doFilter(request, response);
                return;
            }

            UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();

            if (header == null || header.isBlank()) {
                if (principal.isAdmin() || orgOptional) {
                    // Admin sem org ativa, ou endpoint de gestão: segue sem contexto.
                    filterChain.doFilter(request, response);
                    return;
                }
                throw new OrganizationContextRequiredException();
            }

            UUID organizationId = parseOrganizationId(header);
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

            if (organization.getStatus() != OrganizationStatus.ATIVO) {
                throw new OrganizationInactiveException(organizationId);
            }

            // ADMIN acessa qualquer Organization. Demais roles precisam de vínculo.
            if (!principal.isAdmin()
                    && !userOrganizationRepository.existsByUserUuidAndOrganizationUuid(
                            principal.uuid(), organizationId)) {
                throw new OrganizationAccessDeniedException(organizationId);
            }

            OrganizationContext.set(organizationId);
            filterChain.doFilter(request, response);
        } finally {
            // ThreadLocal não pode vazar entre threads do pool do Tomcat.
            OrganizationContext.clear();
        }
    }

    private boolean isOrgOptional(String uri) {
        for (String prefix : ORG_OPTIONAL_PATHS) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private UUID parseOrganizationId(String header) {
        try {
            return UUID.fromString(header.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrganizationHeaderException(header);
        }
    }
}