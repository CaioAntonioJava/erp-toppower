package br.com.toppower.erp_toppower.servicetemplate.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateCreateRequest;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateResponse;
import br.com.toppower.erp_toppower.servicetemplate.dto.ServiceTemplateUpdateRequest;
import br.com.toppower.erp_toppower.servicetemplate.enums.ServiceCategory;
import br.com.toppower.erp_toppower.servicetemplate.service.ServiceTemplateService;
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

@RestController
@RequestMapping("/api/v1/service-templates")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Cadastro e gestão de serviços (catálogo reutilizável em propostas e pedidos).")
public class ServiceTemplateController {

    private final ServiceTemplateService serviceTemplateService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar serviço",
            description = "Cria um novo serviço no catálogo.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceTemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Acesso negado (sem o módulo MODULE_SERVICE_TEMPLATES).", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ServiceTemplateResponse> create(@Valid @RequestBody ServiceTemplateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceTemplateService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar serviços (paginado)",
            description = "Lista serviços paginados, ordenados por nome.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ServiceTemplateResponse>> getAll(
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(serviceTemplateService.getAll(pageable));
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar serviços (paginado)",
            description = "Busca textual por nome (mínimo 2 caracteres). Sem parâmetros: retorna todos (paginado).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de serviços retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ServiceTemplateResponse>> search(
            @Parameter(description = "Termo OPCIONAL (mínimo 2 caracteres). Match em name.",
                    example = "instalação")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(serviceTemplateService.search(query, pageable));
    }

    @GetMapping(value = "/by-category", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar serviços por categoria (paginado)",
            description = "Retorna os serviços filtrados pela categoria informada, ordenados por nome.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ServiceTemplateResponse>> getByCategory(
            @Parameter(description = "Categoria do serviço.", required = true,
                    example = "EXECUÇÃO_SPDA")
            @RequestParam("category") ServiceCategory category,
            @Parameter(hidden = true) @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(serviceTemplateService.getByCategory(category, pageable));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar serviço por ID",
            description = "Retorna um serviço pelo ID.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceTemplateResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ServiceTemplateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceTemplateService.getById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar serviço (parcial)",
            description = "Atualiza apenas os campos enviados.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ServiceTemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ServiceTemplateResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody ServiceTemplateUpdateRequest request) {
        return ResponseEntity.ok(serviceTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir serviço",
            description = "Remove fisicamente o registro do serviço.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_SERVICE_TEMPLATES')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço excluído."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
