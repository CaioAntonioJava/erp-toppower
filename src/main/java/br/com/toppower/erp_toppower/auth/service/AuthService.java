package br.com.toppower.erp_toppower.auth.service;

import br.com.toppower.erp_toppower.auth.dto.LoginRequest;
import br.com.toppower.erp_toppower.auth.dto.LoginResponse;
import br.com.toppower.erp_toppower.auth.exception.InvalidCredentialsException;
import br.com.toppower.erp_toppower.security.JwtService;
import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.entity.User;
import br.com.toppower.erp_toppower.user.mapper.UserMapper;
import br.com.toppower.erp_toppower.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            // Traduz a exceção interna do Spring (mensagem "Bad credentials") pela
            // mensagem amigável e localizada do domínio de autenticação.
            throw new InvalidCredentialsException();
        }

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        User user = userRepository.findByEmail(principal.email())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no banco"));
        UserResponse userResponse = UserMapper.toResponse(user);

        return LoginResponse.of(token, jwtService.getExpirationSeconds(), userResponse);
    }
}
