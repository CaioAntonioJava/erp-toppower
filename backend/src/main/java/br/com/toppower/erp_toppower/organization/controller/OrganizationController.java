package br.com.toppower.erp_toppower.organization.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.organization.dto.OrganizationCreateRequest;
import br.com.toppower.erp_toppower.organization.dto.OrganizationResponse;
import br.com.toppower.erp_toppower.organization.dto.OrganizationSummary;
import br.com.toppower.erp_toppower.organization.dto.OrganizationUpdateRequest;
import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import br.com.toppower.erp_toppower.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Cadastro de empresas (Organizations) e listagem do usuário autenticado.")
public class OrganizationController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final OrganizationService organizationService;

    // =====================================================================
    // Gestão de Organizations (admin-only)
    // =====================================================================

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar Organization",
            description = "Cria uma nova empresa. CNPJ único. Status default = ATIVO se omitido.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organization criada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar Organizations (paginado)",
            description = "Lista todas as empresas paginadas. Filtro opcional por status. Acesso restrito a ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class)))
    })
    public ResponseEntity<PagedResponse<OrganizationResponse>> getAll(
            @Parameter(description = "Filtro opcional: ATIVO ou INATIVO.", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) OrganizationStatus status,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "corporateName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(organizationService.getAll(status, pageable));
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar Organizations (paginado)",
            description = "Busca por razão social ou nome fantasia. Ambos os parâmetros são opcionais.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class)))
    })
    public ResponseEntity<PagedResponse<OrganizationResponse>> search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "status", required = false) OrganizationStatus status,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "corporateName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(organizationService.search(query, status, pageable));
    }

    @GetMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar Organization por ID")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organization encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Organization não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar Organization (parcial)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organization atualizada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Organization não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<OrganizationResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody OrganizationUpdateRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @DeleteMapping("/{id:" + UUID_REGEX + "}")
    @Operation(summary = "Inativar Organization",
            description = "Define status como INATIVO. Não remove fisicamente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Organization inativada."),
            @ApiResponse(responseCode = "404", description = "Organization não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        organizationService.inactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar Organization")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organization reativada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Organization não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<OrganizationResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.activate(id));
    }

    // =====================================================================
    // Listagem do próprio usuário (alimenta o seletor de Organization)
    // =====================================================================

    @GetMapping(value = "/mine", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar Organizations do usuário autenticado",
            description = "Lista as empresas acessíveis ao usuário logado: ADMIN vê todas as ATIVAS; "
                    + "demais roles veem as vinculadas via user_organizations (com role por empresa e flag default).")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de Organizations.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OrganizationSummary.class)))
    })
    public ResponseEntity<List<OrganizationSummary>> listMine() {
        return ResponseEntity.ok(organizationService.listMine());
    }
}