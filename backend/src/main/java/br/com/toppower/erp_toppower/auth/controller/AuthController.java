package br.com.toppower.erp_toppower.auth.controller;

import br.com.toppower.erp_toppower.auth.dto.LoginRequest;
import br.com.toppower.erp_toppower.auth.dto.LoginResponse;
import br.com.toppower.erp_toppower.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação da API.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Realizar login",
            description = "Autentica o usuário pelo e-mail e senha. Retorna um token JWT no formato Bearer. "
                    + "Use esse token no header Authorization das requisições protegidas."
    )
    @SecurityRequirements // endpoint público: não exige bearerAuth
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login bem-sucedido, token JWT retornado.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Erro de validação (e-mail/senha em formato incorreto).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "E-mail e/ou senha inválidos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}