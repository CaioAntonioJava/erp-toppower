package br.com.toppower.erp_toppower.contract.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractCreateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.ClientSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
import br.com.toppower.erp_toppower.sales.quotation.service.ClientSearchService;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractSummaryResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractUpdateRequest;
import br.com.toppower.erp_toppower.contract.dto.NextContractCodeResponse;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import br.com.toppower.erp_toppower.contract.service.ContractPdfService;
import br.com.toppower.erp_toppower.contract.service.ContractService;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

/**
 * Endpoints REST para gestão de contratos.
 *
 * <p>Base path: {@code /api/v1/contracts}. Todos os endpoints exigem
 * autenticação Bearer e perfil {@code ADMIN}, {@code MANAGER} ou
 * {@code SELLER}.</p>
 */
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contratos", description = "Gestão de contratos emitidos pela empresa.")
public class ContractController {

    private final ContractService service;
    private final ContractPdfService pdfService;
    private final ClientSearchService clientSearchService;

    // =====================================================================
    // CRUD
    // =====================================================================

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Criar contrato",
            description = "Cria um novo contrato. O código é gerado automaticamente no "
                    + "formato <prefixo>-<seq>-<ano> (ex.: CT-001-2026 ou CL-001-2026), onde o "
                    + "prefixo é o `contractPrefix` da Organization ativa (header X-Organization-Id) "
                    + "e a sequência reinicia a 1 a cada novo ano, independentemente por empresa. "
                    + "O status inicial é ABERTA e a data de início, se não informada, recebe a "
                    + "data atual. O cliente (PF) é obrigatório.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrato criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> create(
            @Valid @RequestBody ContractCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping(value = "/next-code", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pré-visualizar próximo código de contrato",
            description = "Retorna o próximo código (ex.: CT-001-2026 ou CL-001-2026, conforme "
                    + "o `contractPrefix` da Organization ativa) que seria atribuído ao próximo "
                    + "contrato. A sequência é independente por Organization/ano. Não persiste nada.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Próximo código retornado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NextContractCodeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<NextContractCodeResponse> getNextCode() {
        return ResponseEntity.ok(service.getNextCode());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar/Buscar contratos (paginado)",
            description = "Lista contratos com filtros opcionais. Todos os filtros são opcionais; "
                    + "sem filtros, retorna todos os contratos paginados. Ordenação padrão: data "
                    + "de início decrescente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Página de contratos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ContractSummaryResponse>> search(
            @Parameter(description = "Filtro por status.",
                    schema = @Schema(allowableValues = {
                            "ABERTA", "EM_ANDAMENTO", "CONCLUIDA"}))
            @RequestParam(value = "status", required = false)
            ContractStatus status,

            @Parameter(description = "Data de início a partir de (inclusive). Formato ISO: yyyy-MM-dd.",
                    example = "2026-01-01")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Data de início até (inclusive). Formato ISO: yyyy-MM-dd.",
                    example = "2026-12-31")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "ID do cliente (PF).")
            @RequestParam(value = "customerId", required = false) Long customerId,

            @Parameter(description = "Trecho do código (ex.: 'CT-001' ou '2026').")
            @RequestParam(value = "code", required = false) String code,

            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = {"startDate"},
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(service.search(
                status, startDate, endDate, customerId, code, pageable));
    }

    @GetMapping(value = "/by-code/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar contrato por código",
            description = "Retorna o contrato cujo código formatado bate exatamente com o "
                    + "informado (ex.: CT-001-2026 ou CL-001-2026). A busca é restrita à "
                    + "Organization ativa.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "400", description = "Código inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping(value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar contrato por ID",
            description = "Retorna o contrato completo, incluindo os blocos de texto e o "
                    + "endereço opcional.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PatchMapping(value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar contrato (parcial)",
            description = "Atualiza apenas os campos enviados. Para limpar um campo de texto "
                    + "opcional, envie string vazia (\"\"). Contratos CONCLUIDOS não podem ser "
                    + "alterados — use o endpoint /reopen para reabrir antes de editar.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato/Cliente não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Contrato em estado que impede edição.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ContractUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // =====================================================================
    // Transições de status
    // =====================================================================

    @PostMapping(value = "/{id}/start",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Iniciar contrato (ABERTA → EM_ANDAMENTO)",
            description = "Transiciona o contrato do status ABERTA para EM_ANDAMENTO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato iniciado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Contrato em estado que impede iniciar.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> start(@PathVariable Long id) {
        return ResponseEntity.ok(service.start(id));
    }

    @PostMapping(value = "/{id}/complete",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Concluir contrato (EM_ANDAMENTO → CONCLUIDA)",
            description = "Transiciona o contrato do status EM_ANDAMENTO para CONCLUIDA.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato concluído.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Contrato em estado que impede concluir.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(service.complete(id));
    }

    @PostMapping(value = "/{id}/reopen",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reabrir contrato (CONCLUIDA → EM_ANDAMENTO)",
            description = "Reabre um contrato CONCLUIDO, voltando-o para EM_ANDAMENTO. Útil "
                    + "para corrigir conclusões indevidas.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato reaberto.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Contrato em estado que impede reabrir.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(service.reopen(id));
    }

    // =====================================================================
    // Endpoint de busca de clientes (delegado ao ClientSearchService)
    // =====================================================================

    @GetMapping(value = "/clients/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar clientes (PF e PJ) para seleção em contratos",
            description = "Busca por palavras contidas no nome (todas as palavras devem casar) "
                    + "OU por match no código ou documento. Apenas clientes e empresas com "
                    + "status ATIVO são retornados. Mínimo 2 caracteres no termo de busca. "
                    + "Delegado ao serviço compartilhado com os módulos de propostas comerciais "
                    + "e técnicas.")
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
        ContractResponse.ClientType parsedType = parseClientType(type);
        return ResponseEntity.ok(
                clientSearchService.search(query, limit, toQuotationClientType(parsedType)));
    }

    /**
     * Converte o parâmetro {@code type} da query string para o enum
     * {@link ContractResponse.ClientType}. Valores inválidos, ausentes
     * ou em branco retornam {@code null} (sem filtro).
     */
    private static ContractResponse.ClientType parseClientType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ContractResponse.ClientType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Converte o {@link ContractResponse.ClientType} para o
     * {@link QuotationResponse.ClientType} esperado pelo
     * {@link ClientSearchService} compartilhado.
     */
    private static QuotationResponse.ClientType toQuotationClientType(
            ContractResponse.ClientType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case CUSTOMER -> QuotationResponse.ClientType.CUSTOMER;
            case COMPANY -> QuotationResponse.ClientType.COMPANY;
        };
    }

    // =====================================================================
    // Geração de PDF
    // =====================================================================

    /**
     * Gera o PDF A4 do contrato, com cabeçalho dinâmico (logo + dados da
     * Organization ativa). Suporta {@code disposition=inline} (default —
     * preview em iframe) e {@code attachment} (download).
     */
    @GetMapping(value = "/{id}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Gerar PDF do contrato",
            description = "Retorna o PDF (A4) com cabeçalho do emissor (Organization ativa), "
                    + "cliente, descrição, cláusula, endereço (quando houver), blocos de texto "
                    + "de serviços e produtos (quando houver) e bloco de assinatura.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long id,
            @Parameter(description = "Modo de disposição: 'inline' (preview) ou 'attachment' (download).",
                    schema = @Schema(allowableValues = {"inline", "attachment"}))
            @RequestParam(value = "disposition", defaultValue = "inline") String disposition) {
        ContractResponse contract = service.getById(id);
        byte[] pdf = pdfService.renderPdf(id);

        String fname = "contrato-" + contract.code() + ".pdf";
        ContentDisposition cd = "attachment".equalsIgnoreCase(disposition)
                ? ContentDisposition.attachment().filename(fname).build()
                : ContentDisposition.inline().filename(fname).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}