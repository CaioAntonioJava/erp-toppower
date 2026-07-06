package br.com.toppower.erp_toppower.user.service;

import br.com.toppower.erp_toppower.user.dto.ChangePasswordRequest;
import br.com.toppower.erp_toppower.user.dto.ResetPasswordRequest;
import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.entity.UserTenant;
import br.com.toppower.erp_toppower.user.exception.DuplicateUserTenantException;
import br.com.toppower.erp_toppower.user.exception.EmailAlreadyExistsException;
import br.com.toppower.erp_toppower.user.exception.IncorrectPasswordException;
import br.com.toppower.erp_toppower.user.exception.UserNotFoundException;
import br.com.toppower.erp_toppower.user.mapper.UserMapper;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import br.com.toppower.erp_toppower.user.repository.UserTenantRepository;
import br.com.toppower.erp_toppower.tenant.repository.TenantRepository;
import br.com.toppower.erp_toppower.tenant.exception.TenantNotFoundException;
import br.com.toppower.erp_toppower.auth.service.TenantQueryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final TenantQueryService tenantQueryService;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserTenantRepository userTenantRepository,
                       TenantRepository tenantRepository,
                       TenantQueryService tenantQueryService,
                       JdbcTemplate jdbcTemplate,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userTenantRepository = userTenantRepository;
        this.tenantRepository = tenantRepository;
        this.tenantQueryService = tenantQueryService;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria um novo usuário e o vincula a cada uma das empresas (tenants)
     * selecionadas pelo admin no formulário. O usuário poderá acessar todas
     * as empresas informadas — para alterar o conjunto depois, o admin deve
     * usar o endpoint de vínculo/desvínculo (futuro).
     */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = UserMapper.toEntity(request, encodedPassword);
        User saved = userRepository.save(user);

        // Vincula o usuário a cada tenant selecionado. Valida existência de
        // cada tenant e rejeita duplicidade (defensivo — o admin não deveria
        // enviar o mesmo UUID duas vezes, mas a constraint unique protege).
        for (UUID tenantUuid : request.tenantUuids()) {
            tenantRepository.findById(tenantUuid)
                    .orElseThrow(() -> new TenantNotFoundException(tenantUuid));
            if (!userTenantRepository.existsByUserUuidAndTenantUuid(saved.getUuid(), tenantUuid)) {
                userTenantRepository.save(new UserTenant(saved.getUuid(), tenantUuid));
            }
        }

        return UserMapper.toResponse(saved, tenantQueryService.listTenantsByUserUuid(saved.getUuid()));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(user -> UserMapper.toResponse(user, tenantQueryService.listTenantsByUserUuid(user.getUuid())))
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(user -> UserMapper.toResponse(user, tenantQueryService.listTenantsByUserUuid(user.getUuid())))
                .toList();
    }

    @Transactional
    public void changePassword(UUID authenticatedUserId, UUID targetUserId, ChangePasswordRequest request) {
        if (!authenticatedUserId.equals(targetUserId)) {
            throw new AccessDeniedException("Você só pode alterar sua própria senha");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IncorrectPasswordException();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * Exclui um usuário (hard delete). Remove, em ordem, os registros que
     * referenciam o usuário antes de excluí-lo:
     * <ol>
     *   <li>{@code profiles} — FK física não-nulável para {@code users.uuid}.
     *       Deletado via {@link JdbcTemplate} (SQL nativo) para contornar o
     *       {@code tenantFilter} do Hibernate: o admin pode estar logado em
     *       um tenant diferente daquele do profile do usuário excluído, e o
     *       filtro JPQL adicionaria {@code WHERE tenant_uuid = ?} — o que
     *       poderia afetar zero linhas e deixar o profile órfão. SQL nativo
     *       via JdbcTemplate não passa pelo filtro (mesmo padrão do
     *       {@code TenantBackfillRunner}).</li>
     *   <li>{@code user_tenants} — vínculos lógicos (sem FK física), via
     *       bulk delete JPQL ({@code UserTenant} não é tenant-scoped).</li>
     * </ol>
     *
     * <p>Bloqueia a auto-exclusão: o admin não pode excluir a própria conta,
     * evitando lockout acidental.</p>
     */
    @Transactional
    public void delete(UUID id, UUID requesterUuid) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (id.equals(requesterUuid)) {
            throw new AccessDeniedException("Você não pode excluir sua própria conta");
        }

        // Profile: FK física profiles.user_id → users.uuid. O profile é
        // opcional (admin pode não ter perfil); o SQL nativo é no-op quando
        // não há linha. Bypass do tenantFilter via JdbcTemplate.
        //
        // O UUID é persistido como BINARY(16); passá-lo como java.util.UUID
        // ao JdbcTemplate pode não converter corretamente, então usamos
        // UNHEX(hex) — mesmo padrão do TenantBackfillRunner.
        String userHex = user.getUuid().toString().replace("-", "");
        jdbcTemplate.update(
                "delete from profiles where user_id = UNHEX('" + userHex + "')");
        // user_tenants: sem FK física, mas limpamos para não deixar vínculos
        // órfãos. UserTenant NÃO é tenant-scoped, então o bulk delete JPQL
        // não sofre filtragem.
        userTenantRepository.deleteAllByUserUuid(user.getUuid());

        userRepository.delete(user);
    }

    /**
     * Vincula um usuário a um tenant adicional. Permite que o admin dê a um
     * usuário acesso a mais de uma empresa (tenant) — o usuário poderá então
     * alternar entre elas via switch-tenant no login.
     *
     * <p>Valida que o usuário existe e o tenant existe. Rejeita vínculo
     * duplicado (usuário já vinculado àquele tenant).</p>
     */
    @Transactional
    public void linkTenant(UUID userId, UUID tenantId) {
        // Valida que o usuário existe (lança 404 se não).
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        // Valida que o tenant existe (lança 404 se não).
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        // Rejeita vínculo duplicado.
        if (userTenantRepository.existsByUserUuidAndTenantUuid(userId, tenantId)) {
            throw new DuplicateUserTenantException(userId, tenantId);
        }
        userTenantRepository.save(new UserTenant(userId, tenantId));
    }
}
