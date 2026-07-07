package br.com.toppower.erp_toppower.user.service;

import br.com.toppower.erp_toppower.user.dto.ChangePasswordRequest;
import br.com.toppower.erp_toppower.user.dto.ResetPasswordRequest;
import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;
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

import java.util.List;
import java.util.UUID;

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
     * Exclui um usuário (hard delete). Remove o profile referenciado (FK física
     * profiles.user_id → users.uuid) antes de excluí-lo.
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
        // não há linha.
        //
        // O UUID é persistido como BINARY(16); passá-lo como java.util.UUID
        // ao JdbcTemplate pode não converter corretamente, então usamos
        // UNHEX(hex).
        String userHex = user.getUuid().toString().replace("-", "");
        jdbcTemplate.update(
                "delete from profiles where user_id = UNHEX('" + userHex + "')");

        userRepository.delete(user);
    }
}