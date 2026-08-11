package br.com.toppower.erp_toppower.servicecategory.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryCreateRequest;
import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryResponse;
import br.com.toppower.erp_toppower.servicecategory.dto.ServiceCategoryUpdateRequest;
import br.com.toppower.erp_toppower.servicecategory.enums.ServiceCategoryStatus;
import br.com.toppower.erp_toppower.servicecategory.service.ServiceCategoryService;
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

@RestController
@RequestMapping("/api/v1/service-categories")
@RequiredArgsConstructor
@Tag(name = "Categorias de Serviço", description = "Cadastro e gestão de categorias de serviço. Acesso controlado pelo módulo MODULE_SERVICE_TEMPLATES.")
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar categoria de serviço",
            description = "Cria uma nova categoria de serviço. Status default = ATIVO se omitido.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Acesso negado (sem o módulo MODULE_SERVICE_TEMPLATES).", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ServiceCategoryResponse> create(@Valid @RequestBody ServiceCategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCategoryService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar categorias de serviço (paginado)",
            description = "Lista categorias paginadas, ordenadas por nome. Filtro opcional por status.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ServiceCategoryResponse>> getAll(
            @Parameter(description = "Filtro opcional: ATIVO ou INATIVO.", example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) ServiceCategoryStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(serviceCategoryService.getAll(status, pageable));
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar categorias ativas",
            description = "Retorna todas as categorias ativas (não paginado) para uso em dropdowns/selects de formulários.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de categorias ativas.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceCategoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ServiceCategoryResponse>> getActive() {
        return ResponseEntity.ok(serviceCategoryService.findAllActive());
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar categorias de serviço (paginado)",
            description = "Busca flexível: ambos os parâmetros são opcionais. " +
                    "Filtrar por status: ?status=ATIVO. " +
                    "Filtrar por texto: ?query=xpta. " +
                    "Combinar: ?status=ATIVO&query=xpta. " +
                    "Sem parâmetros: retorna todos (paginado).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de categorias retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ServiceCategoryResponse>> search(
            @Parameter(description = "Termo OPCIONAL (mínimo 2 caracteres). Match em name.",
                    example = "execução")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(description = "Filtro OPCIONAL: ATIVO ou INATIVO. Omitido = ambos.",
                    example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) ServiceCategoryStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(serviceCategoryService.search(query, status, pageable));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar categoria de serviço por ID",
            description = "Retorna uma categoria de serviço pelo ID.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceCategoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ServiceCategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCategoryService.getById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar categoria de serviço (parcial)",
            description = "Atualiza apenas os campos enviados.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ServiceCategoryResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody ServiceCategoryUpdateRequest request) {
        return ResponseEntity.ok(serviceCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar categoria de serviço (soft delete)",
            description = "Define status como INATIVO. Não remove fisicamente o registro.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria inativada."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> inactivate(@PathVariable Long id) {
        serviceCategoryService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar categoria de serviço",
            description = "Define status como ATIVO, reativando uma categoria inativa.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria reativada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceCategoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ServiceCategoryResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCategoryService.activate(id));
    }
}