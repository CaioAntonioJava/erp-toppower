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
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserTenantRepository userTenantRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userTenantRepository = userTenantRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria um novo usuário e o vincula ao tenant do admin autenticado
     * (tenantUuid extraído do JWT). O usuário só poderá acessar os dados
     * daquele tenant — para dar acesso a outro tenant, o admin deve usar
     * um endpoint de vínculo (futuro) ou repetir o cadastro logado no
     * outro tenant.
     */
    @Transactional
    public UserResponse create(UserCreateRequest request, UUID tenantUuid) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = UserMapper.toEntity(request, encodedPassword);

        User saved = userRepository.save(user);

        // Vincula o usuário ao tenant do admin que o cadastrou.
        UserTenant link = new UserTenant(saved.getUuid(), tenantUuid);
        userTenantRepository.save(link);

        return UserMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toResponse)
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
