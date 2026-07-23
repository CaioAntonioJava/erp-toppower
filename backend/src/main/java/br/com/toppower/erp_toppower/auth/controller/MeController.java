package br.com.toppower.erp_toppower.auth.controller;

import br.com.toppower.erp_toppower.auth.dto.LoginResponse.AuthenticatedUser;
import br.com.toppower.erp_toppower.user.enums.Role;
import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint protegido que expõe os dados do usuário autenticado a partir do JWT.
 * Útil também como smoke test do fluxo stateless: um token válido deve retornar 200,
 * enquanto um token ausente/inválido é rejeitado pelo {@code JwtAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e dados do usuário autenticado.")
public class MeController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Obter dados do usuário autenticado",
            description = "Retorna os dados (uuid, e-mail, role) do usuário cujo token JWT foi "
                    + "enviado na requisição. Endpoint protegido — requer um JWT válido no header Authorization."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário autenticado retornado com sucesso.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthenticatedUser.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, inválido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public ResponseEntity<AuthenticatedUser> me(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(new AuthenticatedUser(
                principal.id(), principal.email(), Role.valueOf(principal.role()),
                principal.modules()));
    }
}