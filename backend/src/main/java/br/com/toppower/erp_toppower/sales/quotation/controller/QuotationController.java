package br.com.toppower.erp_toppower.sales.quotation.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.ClientSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.NextQuotationNumberResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationCreateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSimulateRequest;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSimulateResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationUpdateRequest;
import br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus;
import br.com.toppower.erp_toppower.sales.quotation.service.ClientSearchService;
import br.com.toppower.erp_toppower.sales.quotation.service.QuotationPdfService;
import br.com.toppower.erp_toppower.sales.quotation.service.QuotationService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/quotations")
@RequiredArgsConstructor
@Tag(name = "Propostas Comerciais", description = "Gestão de propostas/orçamentos de venda.")
public class QuotationController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final QuotationService quotationService;
    private final ClientSearchService clientSearchService;
    private final QuotationPdfService quotationPdfService;

    // =====================================================================
    // Endpoints de propostas
    // =====================================================================

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Criar proposta comercial",
            description = "Cria uma nova proposta. O número é gerado automaticamente a partir de "
                    + "1500 (incremento de +1 por proposta). A data de emissão é preenchida com a "
                    + "data atual. Deve referenciar exatamente um cliente (PF) ou uma empresa (PJ). "
                    + "Deve ter ao menos um item.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proposta criada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuotationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou invariante violada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Cliente/Vendedor não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<QuotationResponse> create(@Valid @RequestBody QuotationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quotationService.create(request));
    }

    @PostMapping(value = "/simulate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Simular totais de uma proposta",
            description = "Calcula os totais de uma proposta (subtotal, desconto global em R$, total e "
                    + "total de unidades) sem persistir nada. Permite que o frontend exiba um preview em "
                    + "tempo real delegando toda a lógica de cálculo ao backend.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Totais calculados com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuotationSimulateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou invariante violada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<QuotationSimulateResponse> simulate(@Valid @RequestBody QuotationSimulateRequest request) {
        return ResponseEntity.ok(quotationService.simulate(request));
    }

    @GetMapping(value = "/next-number", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pré-visualizar próximo número de proposta",
            description = "Retorna o próximo número sequencial (ex.: 1500, 1501, 1502, ...) "
                    + "que seria atribuído à próxima proposta. Não persiste nada.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Próximo número retornado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NextQuotationNumberResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<NextQuotationNumberResponse> getNextNumber() {
        return ResponseEntity.ok(new NextQuotationNumberResponse(quotationService.getNextNumber()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar/Buscar propostas (paginado)",
            description = "Lista propostas com filtros opcionais. Todos os filtros são opcionais; "
                    + "sem filtros, retorna todas as propostas paginadas. Ordenação padrão: "
                    + "data de emissão decrescente, depois número decrescente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de propostas retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<QuotationSummaryResponse>> search(
            @Parameter(description = "Filtro por status.", schema = @Schema(allowableValues = {
                    "ATIVA", "CONVERTIDA", "CANCELADA", "EXPIRADA"}))
            @RequestParam(value = "status", required = false) QuotationStatus status,

            @Parameter(description = "Data de emissão a partir de (inclusive). Formato ISO: yyyy-MM-dd.",
                    example = "2026-01-01")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Data de emissão até (inclusive). Formato ISO: yyyy-MM-dd.",
                    example = "2026-12-31")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "UUID do cliente (PF ou PJ).")
            @RequestParam(value = "clientUuid", required = false) UUID clientUuid,

            @Parameter(description = "UUID do vendedor.")
            @RequestParam(value = "sellerUuid", required = false) UUID sellerUuid,

            @Parameter(description = "Trecho do número (ex.: '150' para 1500, 1501, ...).",
                    example = "150")
            @RequestParam(value = "number", required = false) String number,

            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = {"issueDate", "number"}, direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(quotationService.search(
                status, startDate, endDate, clientUuid, sellerUuid, number, pageable));
    }

    @GetMapping(value = "/by-number/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar proposta por número",
            description = "Retorna a proposta cujo número bate exatamente com o informado.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuotationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Número inválido (não numérico).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<QuotationResponse> getByNumber(@PathVariable Long number) {
        return ResponseEntity.ok(quotationService.getByNumber(number));
    }

    @GetMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar proposta por ID",
            description = "Retorna a proposta com todos os itens e totais calculados.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuotationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<QuotationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(quotationService.getById(id));
    }

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar proposta (parcial)",
            description = "Atualiza apenas os campos enviados. Ao enviar a lista de itens, "
                    + "os anteriores são removidos e os novos criados. Propostas CONVERTIDAS "
                    + "não podem ser alteradas.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta atualizada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuotationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou invariante violada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Proposta em estado que impede edição.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<QuotationResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody QuotationUpdateRequest request) {
        return ResponseEntity.ok(quotationService.update(id, request));
    }

    @DeleteMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cancelar proposta (soft)",
            description = "Define o status da proposta como CANCELADA. Não remove fisicamente o registro. "
                    + "Propostas CONVERTIDAS não podem ser canceladas por este endpoint.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposta cancelada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuotationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Proposta em estado que impede cancelamento.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<QuotationResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(quotationService.cancel(id));
    }

    // =====================================================================
    // Geração de PDF
    // =====================================================================

    /**
     * Gera o PDF A4 da proposta comercial, com cabeçalho dinâmico
     * (logo e dados da Organization ativa). Suporta dois modos via
     * query param {@code disposition}:
     * <ul>
     *   <li>{@code inline} (default) — usado pelo frontend para exibir
     *       o PDF em um iframe (preview).</li>
     *   <li>{@code attachment} — dispara o download direto do arquivo.</li>
     * </ul>
     */
    @GetMapping(value = "/{id:" + UUID_REGEX + "}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Gerar PDF da proposta",
            description = "Retorna o PDF (A4) com cabeçalho do emissor (Organization ativa), "
                    + "cliente, condições, itens, totais e observações.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @Parameter(description = "Modo de disposição: 'inline' (preview) ou 'attachment' (download).",
                    schema = @Schema(allowableValues = {"inline", "attachment"}))
            @RequestParam(value = "disposition", defaultValue = "inline") String disposition) {
        QuotationResponse q = quotationService.getById(id);
        byte[] pdf = quotationPdfService.renderPdf(id);

        ContentDisposition cd = "attachment".equalsIgnoreCase(disposition)
                ? ContentDisposition.attachment().filename("proposta-" + q.number() + ".pdf").build()
                : ContentDisposition.inline().filename("proposta-" + q.number() + ".pdf").build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // =====================================================================
    // Endpoint de busca de clientes
    // =====================================================================

    @GetMapping(value = "/clients/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar clientes (PF e PJ) para seleção em propostas",
            description = "Busca por palavras contidas no nome (todas as palavras devem casar) "
                    + "OU por match no código. Apenas clientes e empresas com status ATIVO são "
                    + "retornados. Mínimo 2 caracteres no termo de busca.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ClientSummaryResponse>> searchClients(
            @Parameter(description = "Termo de busca (mínimo 2 caracteres). Match por palavras "
                    + "contidas no nome OU por trecho do código.", example = "João")
            @RequestParam("query") String query,

            @Parameter(description = "Limite de resultados (padrão 20, máximo 100).", example = "20")
            @RequestParam(value = "limit", required = false) Integer limit,

            @Parameter(description = "Filtro opcional por tipo de cliente: 'CUSTOMER' (apenas PF) "
                    + "ou 'COMPANY' (apenas PJ). Quando omitido, retorna PF + PJ.",
                    example = "CUSTOMER",
                    schema = @Schema(allowableValues = {"CUSTOMER", "COMPANY"}))
            @RequestParam(value = "type", required = false) String type) {
        QuotationResponse.ClientType parsedType = parseClientType(type);
        return ResponseEntity.ok(clientSearchService.search(query, limit, parsedType));
    }

    /**
     * Converte o parâmetro {@code type} da query string para o enum
     * {@link QuotationResponse.ClientType}. Valores inválidos, ausentes
     * ou em branco retornam {@code null} (sem filtro — comportamento
     * original de PF + PJ).
     */
    private static QuotationResponse.ClientType parseClientType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return QuotationResponse.ClientType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Valor desconhecido: ignora o filtro em vez de devolver 400,
            // para preservar compatibilidade com callers antigos.
            return null;
        }
    }
}
