package br.com.toppower.erp_toppower.auth.controller;

import br.com.toppower.erp_toppower.auth.dto.LoginResponse.AuthenticatedUser;
import br.com.toppower.erp_toppower.enums.Role;
import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoint protegido que expõe os dados do usuário autenticado a partir do JWT.
 * Útil também como smoke test do fluxo stateless: um token válido deve retornar 200,
 * enquanto um token ausente/inválido é rejeitado pelo {@code JwtAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    @GetMapping
    public ResponseEntity<AuthenticatedUser> me(@AuthenticationPrincipal UserDetailsImpl principal) {
        UUID uuid = principal.uuid();
        String email = principal.email();
        Role role = Role.valueOf(principal.role());
        return ResponseEntity.ok(new AuthenticatedUser(uuid, email, role));
    }
}
