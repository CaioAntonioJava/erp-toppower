package br.com.toppower.erp_toppower.supplier.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.supplier.dto.SupplierCreateRequest;
import br.com.toppower.erp_toppower.supplier.dto.SupplierResponse;
import br.com.toppower.erp_toppower.supplier.dto.SupplierUpdateRequest;
import br.com.toppower.erp_toppower.supplier.enums.SupplierStatus;
import br.com.toppower.erp_toppower.supplier.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro e gestão de fornecedores (PJ) com endereço embutido.")
public class SupplierController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final SupplierService supplierService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar fornecedor",
            description = "Cria um novo fornecedor (sempre PJ). Validação do CNPJ com dígitos verificadores. " +
                    "Status default = ATIVO se omitido.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fornecedor criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SupplierResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação (CNPJ/endereço).", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar fornecedores (paginado)",
            description = "Lista fornecedores paginados, ordenados por razão social. Filtro opcional por status.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<SupplierResponse>> getAll(
            @Parameter(description = "Filtro opcional: ATIVO ou INATIVO.", example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) SupplierStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "legalName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(supplierService.getAll(status, pageable));
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar fornecedores (paginado)",
            description = "Busca flexível: ambos os parâmetros são opcionais. " +
                    "Filtrar por status: ?status=ATIVO. " +
                    "Filtrar por texto: ?query=xpto. " +
                    "Combinar: ?status=ATIVO&query=xpto. " +
                    "Sem parâmetros: retorna todos (paginado).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de fornecedores retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<SupplierResponse>> search(
            @Parameter(description = "Termo OPCIONAL (mínimo 2 caracteres). Match em legalName, tradeName, taxId ou contactName.",
                    example = "xpto")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(description = "Filtro OPCIONAL: ATIVO ou INATIVO. Omitido = ambos.",
                    example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) SupplierStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "legalName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(supplierService.search(query, status, pageable));
    }

    @GetMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar fornecedor por ID",
            description = "Retorna um fornecedor pelo UUID. Acesso restrito a administradores (ROLE_ADMIN).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fornecedor encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SupplierResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Acesso negado (sem ROLE_ADMIN).", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SupplierResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar fornecedor (parcial)",
            description = "Atualiza apenas os campos enviados. O CNPJ (taxId) NÃO pode ser alterado. " +
                    "Para enviar um novo endereço, inclua o sub-objeto 'address' completo (substitui o anterior).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fornecedor atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SupplierResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SupplierResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody SupplierUpdateRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id:" + UUID_REGEX + "}")
    @Operation(summary = "Inativar fornecedor (soft delete)",
            description = "Define status como INATIVO. Não remove fisicamente o registro.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fornecedor inativado."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        supplierService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar fornecedor",
            description = "Define status como ATIVO, reativando um fornecedor inativo.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fornecedor reativado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SupplierResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SupplierResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.activate(id));
    }
}
