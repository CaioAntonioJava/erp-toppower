package br.com.toppower.erp_toppower.user.service;

import br.com.toppower.erp_toppower.user.dto.ChangePasswordRequest;
import br.com.toppower.erp_toppower.user.dto.ResetPasswordRequest;
import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.dto.UserUpdateRequest;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.user.exception.EmailAlreadyExistsException;
import br.com.toppower.erp_toppower.user.exception.IncorrectPasswordException;
import br.com.toppower.erp_toppower.user.exception.UserNotFoundException;
import br.com.toppower.erp_toppower.user.mapper.UserMapper;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       JdbcTemplate jdbcTemplate,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria um novo usuário. O e-mail deve ser único no sistema.
     */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = UserMapper.toEntity(request, encodedPassword);
        User saved = userRepository.save(user);

        return UserMapper.toResponse(saved);
    }

    /**
     * Atiza parcialmente um usuário (role e/ou módulos).
     *
     * <p>Guarda de segurança: impede que um administrador remova o próprio
     * papel {@code ROLE_ADMIN}, evitando lockout acidental do sistema.</p>
     */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, Long requesterId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.role() != null) {
            // Impede auto-rebaixamento de ADMIN: evita ficar sem nenhum admin.
            if (id.equals(requesterId)
                    && user.getRole() == Role.ROLE_ADMIN
                    && request.role() != Role.ROLE_ADMIN) {
                throw new AccessDeniedException(
                        "Você não pode remover seu próprio papel de administrador");
            }
            user.setRole(request.role());
        }

        if (request.modules() != null) {
            user.setModules(EnumSet.copyOf(request.modules()));
        }

        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
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
    public void changePassword(Long authenticatedUserId, Long targetUserId, ChangePasswordRequest request) {
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
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * Exclui um usuário (hard delete). Remove o profile referenciado (FK física
     * profiles.user_id → users.id) e os vínculos de permissões antes de excluí-lo.
     *
     * <p>Bloqueia a auto-exclusão: o admin não pode excluir a própria conta,
     * evitando lockout acidental.</p>
     */
    @Transactional
    public void delete(Long id, Long requesterId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (id.equals(requesterId)) {
            throw new AccessDeniedException("Você não pode excluir sua própria conta");
        }

        // Profile: FK física profiles.user_id → users.id. O profile é
        // opcional (admin pode não ter perfil); o SQL nativo é no-op quando
        // não há linha.
        jdbcTemplate.update("delete from profiles where user_id = ?", user.getId());

        userRepository.delete(user);
    }
}