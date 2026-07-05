package br.com.toppower.erp_toppower.sales.technicalproposal.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.ClientSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.service.ClientSearchService;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.NextTechnicalProposalCodeResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalCreateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSimulateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSimulateResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalSummaryResponse;
import br.com.toppower.erp_toppower.sales.technicalproposal.dto.TechnicalProposalUpdateRequest;
import br.com.toppower.erp_toppower.sales.technicalproposal.enums.TechnicalProposalStatus;
import br.com.toppower.erp_toppower.sales.technicalproposal.service.TechnicalProposalService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/technical-proposals")
@RequiredArgsConstructor
@Tag(name = "Propostas Técnicas", description = "Gestão de propostas técnicas de serviço.")
public class TechnicalProposalController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final TechnicalProposalService service;
    private final ClientSearchService clientSearchService;

    // =====================================================================
    // CRUD
    // =====================================================================

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Criar proposta técnica",
            description = "Cria uma nova proposta técnica. O código é gerado automaticamente no "
                    + "formato PL-001-2026 (prefixo fixo, sequência reiniciando a 1 a cada novo "
                    + "ano, ano corrente). O status inicial é ABERTA e a data de início, se não "
                    + "informada, recebe a data atual. Deve referenciar exatamente um cliente "
                    + "(PF) ou uma empresa (PJ). Deve ter ao menos um item (serviço ou produto).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proposta criada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TechnicalProposalResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Erro de validação ou invariante violada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Cliente/Produto não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<TechnicalProposalResponse> create(
            @Valid @RequestBody TechnicalProposalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping(value = "/simulate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Simular totais de uma proposta técnica",
            description = "Calcula os totais (subtotal de serviços, subtotal de produtos, "
                    + "subtotal geral, desconto global em R$, total final) sem persistir nada. "
                    + "Permite que o frontend exiba um preview em tempo real delegando toda a "
                    + "lógica de cálculo ao backend.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Totais calculados com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TechnicalProposalSimulateResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Erro de validação ou invariante violada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<TechnicalProposalSimulateResponse> simulate(
            @Valid @RequestBody TechnicalProposalSimulateRequest request) {
        return ResponseEntity.ok(service.simulate(request));
    }

    @GetMapping(value = "/next-code", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pré-visualizar próximo código de proposta técnica",
            description = "Retorna o próximo código (ex.: PL-001-2026) que seria atribuído à "
                    + "próxima proposta. Não persiste nada.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Próximo código retornado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NextTechnicalProposalCodeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<NextTechnicalProposalCodeResponse> getNextCode() {
        return ResponseEntity.ok(service.getNextCode());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar/Buscar propostas técnicas (paginado)",
            description = "Lista propostas técnicas com filtros opcionais. Todos os filtros são "
                    + "opcionais; sem filtros, retorna todas as propostas paginadas. Ordenação "
                    + "padrão: data de início decrescente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Página de propostas técnicas retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<TechnicalProposalSummaryResponse>> search(
            @Parameter(description = "Filtro por status.",
                    schema = @Schema(allowableValues = {
                            "ABERTA", "EM_ANDAMENTO", "CONCLUIDA"}))
            @RequestParam(value = "status", required = false)
            TechnicalProposalStatus status,

            @Parameter(description = "Data de início a partir de (inclusive). Formato ISO: yyyy-MM-dd.",
                    example = "2026-01-01")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Data de início até (inclusive). Formato ISO: yyyy-MM-dd.",
                    example = "2026-12-31")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "UUID do cliente (PF ou PJ).")
            @RequestParam(value = "clientUuid", required = false) UUID clientUuid,

            @Parameter(description = "Trecho do código (ex.: 'PL-001' ou '2026').")
            @RequestParam(value = "code", required = false) String code,

            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = {"startDate"},
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(service.search(
                status, startDate, endDate, clientUuid, code, pageable));
    }

    @GetMapping(value = "/by-code/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar proposta técnica por código",
            description = "Retorna a proposta cujo código formatado bate exatamente com o "
                    + "informado (ex.: PL-001-2026).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TechnicalProposalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Código inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<TechnicalProposalResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping(value = "/{id:" + UUID_REGEX + "}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar proposta técnica por ID",
            description = "Retorna a proposta técnica com todos os itens e totais calculados.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TechnicalProposalResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<TechnicalProposalResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar proposta técnica (parcial)",
            description = "Atualiza apenas os campos enviados. Ao enviar a lista de serviços ou "
                    + "produtos, os anteriores são removidos e os novos criados (substituição "
                    + "completa por lista). Propostas CONCLUIDAS não podem ser alteradas — use "
                    + "o endpoint /reopen para reabrir antes de editar.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta atualizada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TechnicalProposalResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Erro de validação ou invariante violada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Proposta em estado que impede edição.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<TechnicalProposalResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TechnicalProposalUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // =====================================================================
    // Transições de status
    // =====================================================================

    @PostMapping(value = "/{id:" + UUID_REGEX + "}/start",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Iniciar execução (ABERTA → EM_ANDAMENTO)",
            description = "Transiciona a proposta do status ABERTA para EM_ANDAMENTO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta iniciada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TechnicalProposalResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Proposta em estado que impede iniciar.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<TechnicalProposalResponse> start(@PathVariable UUID id) {
        return ResponseEntity.ok(service.start(id));
    }

    @PostMapping(value = "/{id:" + UUID_REGEX + "}/complete",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Concluir execução (EM_ANDAMENTO → CONCLUIDA)",
            description = "Transiciona a proposta do status EM_ANDAMENTO para CONCLUIDA e "
                    + "preenche automaticamente a data de entrega com a data atual.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta concluída.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TechnicalProposalResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Proposta em estado que impede concluir.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<TechnicalProposalResponse> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(service.complete(id));
    }

    @PostMapping(value = "/{id:" + UUID_REGEX + "}/reopen",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reabrir proposta (CONCLUIDA → EM_ANDAMENTO)",
            description = "Reabre uma proposta CONCLUIDA, voltando-a para EM_ANDAMENTO e "
                    + "limpando a data de entrega. Útil para corrigir conclusões indevidas.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta reaberta.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TechnicalProposalResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Proposta em estado que impede reabrir.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<TechnicalProposalResponse> reopen(@PathVariable UUID id) {
        return ResponseEntity.ok(service.reopen(id));
    }

    // =====================================================================
    // Endpoint de busca de clientes (delegado ao ClientSearchService)
    // =====================================================================

    @GetMapping(value = "/clients/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar clientes (PF e PJ) para seleção em propostas técnicas",
            description = "Busca por palavras contidas no nome (todas as palavras devem casar) "
                    + "OU por match no código ou documento. Apenas clientes e empresas com "
                    + "status ATIVO são retornados. Mínimo 2 caracteres no termo de busca. "
                    + "Delegado ao serviço compartilhado com o módulo de propostas comerciais.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Lista de clientes retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ClientSummaryResponse>> searchClients(
            @Parameter(description = "Termo de busca (mínimo 2 caracteres).",
                    example = "João")
            @RequestParam("query") String query,

            @Parameter(description = "Limite de resultados (padrão 20, máximo 100).",
                    example = "20")
            @RequestParam(value = "limit", required = false) Integer limit,

            @Parameter(description = "Filtro opcional por tipo de cliente: 'CUSTOMER' (apenas PF) "
                    + "ou 'COMPANY' (apenas PJ). Quando omitido, retorna PF + PJ.",
                    example = "CUSTOMER",
                    schema = @Schema(allowableValues = {"CUSTOMER", "COMPANY"}))
            @RequestParam(value = "type", required = false) String type) {
        TechnicalProposalResponse.ClientType parsedType = parseClientType(type);
        return ResponseEntity.ok(clientSearchService.search(query, limit, toQuotationClientType(parsedType)));
    }

    /**
     * Converte o parâmetro {@code type} da query string para o enum
     * {@link TechnicalProposalResponse.ClientType}. Valores inválidos,
     * ausentes ou em branco retornam {@code null} (sem filtro).
     */
    private static TechnicalProposalResponse.ClientType parseClientType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TechnicalProposalResponse.ClientType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Converte o {@link TechnicalProposalResponse.ClientType} para o
     * {@link br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse.ClientType}
     * esperado pelo {@link ClientSearchService} compartilhado.
     */
    private static br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse.ClientType
            toQuotationClientType(TechnicalProposalResponse.ClientType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case CUSTOMER -> br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse.ClientType.CUSTOMER;
            case COMPANY -> br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse.ClientType.COMPANY;
        };
    }
}