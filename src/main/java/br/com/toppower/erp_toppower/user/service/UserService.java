package br.com.toppower.erp_toppower.user.service;

import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.exception.EmailAlreadyExistsException;
import br.com.toppower.erp_toppower.user.mapper.UserMapper;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
}
