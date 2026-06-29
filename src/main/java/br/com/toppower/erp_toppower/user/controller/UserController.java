package br.com.toppower.erp_toppower.user.controller;

import br.com.toppower.erp_toppower.security.UserDetailsImpl;
import br.com.toppower.erp_toppower.user.dto.ChangePasswordRequest;
import br.com.toppower.erp_toppower.user.dto.ResetPasswordRequest;
import br.com.toppower.erp_toppower.user.dto.UserCreateRequest;
import br.com.toppower.erp_toppower.user.dto.UserResponse;
import br.com.toppower.erp_toppower.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints de gerenciamento de usuarios do ERP TopPower.")
public class UserController {

    private final UserService userService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Cadastrar novo usuario",
            description = "Cria um novo usuario no sistema. Endpoint publico (nao exige token JWT) "
                    + "para permitir o bootstrap do primeiro usuario administrador. O e-mail deve ser unico."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validacao nos campos enviados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Ja existe um usuario com o e-mail informado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Listar todos os usuarios",
            description = "Retorna todos os usuarios cadastrados. Acesso restrito a administradores (ROLE_ADMIN)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui ROLE_ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Buscar usuario por ID",
            description = "Retorna os dados de um usuario pelo seu UUID. Acesso restrito a administradores (ROLE_ADMIN)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui ROLE_ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PatchMapping(value = "/{id}/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Alterar propria senha",
            description = "Permite que o usuario autenticado altere sua propria senha, "
                    + "informando a senha atual e a nova senha. O UUID do token JWT deve corresponder ao ID do path. "
                    + "A senha atual eh validada antes da atualizacao."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou erro de validacao.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Tentativa de alterar senha de outro usuario.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetailsImpl principal,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.uuid(), id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Redefinir senha de usuario (ADMIN)",
            description = "Permite que um administrador redefina a senha de qualquer usuario, "
                    + "informando a nova senha a ser atribuida. Acesso restrito a administradores (ROLE_ADMIN)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida com sucesso."),
            @ApiResponse(responseCode = "400", description = "Erro de validacao nos campos enviados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui ROLE_ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id,
                                               @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
