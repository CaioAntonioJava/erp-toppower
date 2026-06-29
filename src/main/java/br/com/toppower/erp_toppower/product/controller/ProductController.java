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
            summary = "Listar produtos (paginado)",
            description = "Retorna produtos paginados. Use o parametro 'status' (ATIVO ou INATIVO) "
                    + "para filtrar; se omitido, retorna todos. Ordenacao padrao por nome. "
                    + "Acesso para usuarios autenticados (ADMIN, MANAGER, EMPLOYEE)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de produtos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ProductResponse>> getAll(
            @Parameter(description = "Filtro opcional por status (ATIVO ou INATIVO). Se omitido, retorna ambos.",
                    example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) ProductStatus status,

            @Parameter(description = "Paginacao. Ex.: ?page=0&size=20&sort=name,asc",
                    example = "0")
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(productService.getAll(status, pageable));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Buscar produto por ID",
            description = "Retorna um produto pelo seu UUID. Acesso para usuarios autenticados."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
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
    public ResponseEntity<PagedResponse<ProductResponse>> search(
            @Parameter(description = "Termo de busca (minimo 2 caracteres). Match contra nome e codigo do produto.",
                    example = "cabo flexivel", required = true)
            @RequestParam("query") String query,

            @Parameter(description = "Paginacao. Ex.: ?page=0&size=20&sort=name,asc")
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(productService.search(query, pageable));
    }
}
