package br.com.toppower.erp_toppower.product.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.product.dto.ProductCreateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.dto.ProductUpdateRequest;
import br.com.toppower.erp_toppower.product.enums.ProductStatus;
import br.com.toppower.erp_toppower.product.service.ProductService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Cadastro e gestao de produtos.")
public class ProductController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final ProductService productService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar produto",
            description = "Cria novo produto. Todas as roles autenticadas. Status default = ATIVO se omitido.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Código já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar produtos (paginado)",
            description = "Lista produtos paginados. Filtro opcional por status. Todas as roles.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ProductResponse>> getAll(
            @Parameter(hidden = true, description = "Filtro opcional: ATIVO ou INATIVO.", example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) ProductStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getAll(status, pageable));
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar produtos ATIVOS (paginado)",
            description = "Atalho para listar apenas produtos com status ATIVO. Equivalente a GET /products?status=ATIVO. Todas as roles.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de produtos ativos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ProductResponse>> getActive(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.findActive(pageable));
    }

    @GetMapping(value = "/inactive", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar produtos INATIVOS (paginado)",
            description = "Atalho para listar apenas produtos com status INATIVO. Equivalente a GET /products?status=INATIVO. " +
                    "Útil para relatórios de produtos a serem reativados ou removidos. Todas as roles.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de produtos inativos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ProductResponse>> getInactive(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.findInactive(pageable));
    }

    @GetMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar produto por ID", description = "Retorna um produto pelo UUID.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar produto (parcial)",
            description = "Atualiza apenas os campos enviados. Todas as roles autenticadas. Código duplicado -> 409.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Código já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id:" + UUID_REGEX + "}")
    @Operation(summary = "Inativar produto (soft delete)",
            description = "Define status como INATIVO. Todas as roles autenticadas. Resposta 204 No Content.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto inativado."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })

    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        productService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar produtos por termos (paginado)",
            description = "Busca case-insensitive por substring em nome OU código. Inclui produtos ATIVOS e INATIVOS. Todas as roles.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de produtos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ProductResponse>> search(
            @Parameter(description = "Termo de busca (mínimo 2 caracteres).", example = "cabo flexível", required = true)
            @RequestParam("query") String query,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.search(query, pageable));
    }
}
