package br.com.toppower.erp_toppower.userorganization.controller;

import br.com.toppower.erp_toppower.userorganization.dto.UserOrganizationAssignRequest;
import br.com.toppower.erp_toppower.userorganization.dto.UserOrganizationResponse;
import br.com.toppower.erp_toppower.userorganization.service.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-organizations")
@RequiredArgsConstructor
@Tag(name = "Vínculos Usuário↔Organization",
        description = "Gestão de acesso de usuários a Organizations. Acesso restrito a ADMIN.")
public class UserOrganizationController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final UserOrganizationService userOrganizationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Vincular usuário a Organization",
            description = "Cria um vínculo usuário↔Organization com uma role de negócio. "
                    + "Se isDefault=true, desmarca qualquer outra default do mesmo usuário.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vínculo criado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserOrganizationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Usuário ou Organization não encontrados.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Vínculo já existe.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<UserOrganizationResponse> assign(@Valid @RequestBody UserOrganizationAssignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userOrganizationService.assign(request));
    }

    @GetMapping(value = "/by-user/{userId:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar vínculos de um usuário")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de vínculos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserOrganizationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<UserOrganizationResponse>> listByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userOrganizationService.listByUser(userId));
    }

    @PatchMapping(value = "/{userId:" + UUID_REGEX + "}/{organizationId:" + UUID_REGEX + "}/default",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Definir Organization default do usuário",
            description = "Marca o vínculo como default e desmarca os outros do mesmo usuário.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserOrganizationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vínculo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<UserOrganizationResponse> setDefault(@PathVariable UUID userId,
                                                              @PathVariable UUID organizationId) {
        return ResponseEntity.ok(userOrganizationService.setDefault(userId, organizationId));
    }

    @DeleteMapping("/{userOrganizationId:" + UUID_REGEX + "}")
    @Operation(summary = "Remover vínculo usuário↔Organization")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vínculo removido."),
            @ApiResponse(responseCode = "404", description = "Vínculo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> unassign(@PathVariable UUID userOrganizationId) {
        userOrganizationService.unassign(userOrganizationId);
        return ResponseEntity.noContent().build();
    }
}