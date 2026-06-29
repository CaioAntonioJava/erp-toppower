package br.com.toppower.erp_toppower.product.controller;

import br.com.toppower.erp_toppower.product.dto.ProductCreateRequest;
import br.com.toppower.erp_toppower.product.dto.ProductResponse;
import br.com.toppower.erp_toppower.product.dto.ProductUpdateRequest;
import br.com.toppower.erp_toppower.product.service.ProductService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Cadastro e gestao de produtos.")
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Cadastrar produto",
            description = "Cria um novo produto. Acesso restrito a administradores (ROLE_ADMIN). "
                    + "O codigo (SKU) deve ser unico. Status default = ATIVO se omitido."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validacao nos campos enviados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui ROLE_ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Ja existe um produto com o codigo informado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Listar produtos",
            description = "Retorna todos os produtos cadastrados (ativos e inativos). "
                    + "Acesso para usuarios autenticados (ADMIN, MANAGER, EMPLOYEE)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de produtos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Buscar produto por ID",
            description = "Retorna um produto pelo seu UUID. Acesso para usuarios autenticados."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Atualizar produto (parcial)",
            description = "Atualiza apenas os campos enviados no corpo (PATCH parcial). "
                    + "Acesso restrito a administradores (ROLE_ADMIN). Codigo duplicado -> 409."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validacao nos campos enviados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui ROLE_ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Ja existe um produto com o codigo informado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Inativar produto (soft delete)",
            description = "Define o status do produto como INATIVO (soft delete), preservando "
                    + "o historico. Acesso restrito a administradores (ROLE_ADMIN)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto inativado com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Usuario nao possui ROLE_ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Produto nao encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        productService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/search")
    @Operation(
            summary = "Buscar produtos (ativos)",
            description = "Busca case-insensitive por substring em nome OU codigo. "
                    + "Retorna apenas produtos com status ATIVO, ordenados por nome. "
                    + "Acesso para usuarios autenticados (ADMIN, MANAGER, EMPLOYEE)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de produtos encontrados (vazia se nao houver resultados).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo de busca invalido (vazio ou com menos de 2 caracteres).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ProductResponse>> search(
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "Termo de busca (minimo 2 caracteres). Match contra nome e código do produto.",
                    example = "cabo flexivel",
                    required = true)
            @RequestParam("query") String query) {
        return ResponseEntity.ok(productService.search(query));
    }
}
