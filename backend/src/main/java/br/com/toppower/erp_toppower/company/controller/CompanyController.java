package br.com.toppower.erp_toppower.company.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.company.dto.CompanyCreateRequest;
import br.com.toppower.erp_toppower.company.dto.CompanyResponse;
import br.com.toppower.erp_toppower.company.dto.CompanyUpdateRequest;
import br.com.toppower.erp_toppower.company.service.CompanyService;
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
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Cadastro e gestão de empresas (pessoas jurídicas) com endereço embutido.")
public class CompanyController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final CompanyService companyService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar empresa",
            description = "Cria uma nova empresa (pessoa jurídica). CNPJ é validado com dígitos verificadores. " +
                    "Status default = ATIVO se omitido.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empresa criada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação (CNPJ/CEP/UF/Endereço).", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Código ou CNPJ já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar empresas (paginado)",
            description = "Lista empresas paginadas, ordenadas por razão social. Filtro opcional por status. " +
                    "Todas as roles.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de empresas retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<CompanyResponse>> getAll(
            @Parameter(description = "Filtro opcional: ATIVO ou INATIVO.", example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) RegistrationStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "legalName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(companyService.getAll(status, pageable));
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar empresas (paginado)",
            description = "Busca flexível: ambos os parâmetros são opcionais. " +
                    "Filtrar apenas por status: ?status=ATIVO. " +
                    "Filtrar por texto: ?query=xpto. " +
                    "Combinar: ?status=ATIVO&query=xpto. " +
                    "Sem parâmetros: retorna todos (paginado). Todas as roles.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de empresas retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<CompanyResponse>> search(
            @Parameter(description = "Termo de busca OPCIONAL (mínimo 2 caracteres quando informado). Match em code, legalName, tradeName ou cnpj.",
                    example = "xpto")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(description = "Filtro OPCIONAL: ATIVO ou INATIVO. Omitido = ambos.",
                    example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) RegistrationStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "legalName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(companyService.search(query, status, pageable));
    }

    @GetMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar empresa por ID",
            description = "Retorna uma empresa pelo UUID. Disponível para ADMIN e MANAGER — quem pode criar/editar também pode visualizar.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CompanyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar empresa (parcial)",
            description = "Atualiza apenas os campos enviados. O CNPJ NÃO pode ser alterado. " +
                    "Para enviar um novo endereço, inclua o sub-objeto 'address' completo (substitui o anterior).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa atualizada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CompanyResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody CompanyUpdateRequest request) {
        return ResponseEntity.ok(companyService.update(id, request));
    }

    @DeleteMapping("/{id:" + UUID_REGEX + "}")
    @Operation(summary = "Inativar empresa (soft delete)",
            description = "Define status como INATIVO. Não remove fisicamente o registro. Todas as roles.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empresa inativada."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        companyService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar empresa",
            description = "Define status como ATIVO, reativando uma empresa inativa. Todas as roles.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa reativada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CompanyResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CompanyResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.activate(id));
    }
}
