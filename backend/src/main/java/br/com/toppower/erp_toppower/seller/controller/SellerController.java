package br.com.toppower.erp_toppower.seller.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.seller.dto.SellerCreateRequest;
import br.com.toppower.erp_toppower.seller.dto.SellerResponse;
import br.com.toppower.erp_toppower.seller.dto.SellerUpdateRequest;
import br.com.toppower.erp_toppower.seller.enums.SellerStatus;
import br.com.toppower.erp_toppower.seller.service.SellerService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Vendedores", description = "Cadastro e gestão de vendedores com percentual de comissão.")
public class SellerController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final SellerService sellerService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar vendedor",
            description = "Cria um novo vendedor. Todas as roles autenticadas.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vendedor criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SellerResponse> create(@Valid @RequestBody SellerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar vendedores (paginado)",
            description = "Lista vendedores paginados, ordenados por nome. Todas as roles autenticadas.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de vendedores retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<SellerResponse>> getAll(
            @Parameter(description = "Filtro opcional: ATIVO ou INATIVO.", example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) SellerStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(sellerService.getAll(status, pageable));
    }

    @GetMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar vendedor por ID",
            description = "Retorna um vendedor pelo UUID. Acesso restrito a administradores (ROLE_ADMIN).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vendedor encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Acesso negado (sem ROLE_ADMIN).", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Vendedor não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SellerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(sellerService.getById(id));
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar vendedor (parcial)",
            description = "Atualiza apenas os campos enviados. Todas as roles autenticadas. CPF/e-mail duplicado -> 409.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vendedor atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Vendedor não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SellerResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody SellerUpdateRequest request) {
        return ResponseEntity.ok(sellerService.update(id, request));
    }

    @DeleteMapping("/{id:" + UUID_REGEX + "}")
    @Operation(summary = "Inativar vendedor (soft delete)",
            description = "Define status como INATIVO. Não remove fisicamente o registro. " +
                    "Acesso restrito a administradores (ROLE_ADMIN). Resposta 204 No Content.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vendedor inativado."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Acesso negado (sem ROLE_ADMIN).", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Vendedor não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        sellerService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar vendedor",
            description = "Define status como ATIVO, reativando um vendedor inativo. " +
                    "Acesso restrito a administradores (ROLE_ADMIN).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vendedor reativado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Acesso negado (sem ROLE_ADMIN).", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Vendedor não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SellerResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(sellerService.activate(id));
    }
}
