package br.com.toppower.erp_toppower.sales.salesorder.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.NextSalesOrderNumberResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderCreateRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderFromQuotationRequest;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderSummaryResponse;
import br.com.toppower.erp_toppower.sales.salesorder.dto.SalesOrderUpdateRequest;
import br.com.toppower.erp_toppower.sales.salesorder.enums.SalesOrderStatus;
import br.com.toppower.erp_toppower.sales.salesorder.service.SalesOrderService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales-orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos de Venda", description = "Gestão de pedidos de venda (conversão de propostas ou criação direta).")
public class SalesOrderController {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final SalesOrderService salesOrderService;

    // =====================================================================
    // Criação
    // =====================================================================

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Criar pedido de venda (direto)",
            description = "Cria um novo pedido de venda sem proposta de origem. O número é gerado "
                    + "automaticamente a partir de 1000 (incremento de +1 por pedido). A data de "
                    + "emissão é preenchida com a data atual e o status inicial é ABERTO. Deve "
                    + "referenciar exatamente um cliente (PF) ou uma empresa (PJ). Deve ter ao "
                    + "menos um item. Não há margem de lucro — o pedido é o documento externo "
                    + "enviado ao cliente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SalesOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou invariante violada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Cliente/Vendedor não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SalesOrderResponse> create(@Valid @RequestBody SalesOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesOrderService.create(request));
    }

    @PostMapping(value = "/from-quotation/{quotationId:" + UUID_REGEX + "}",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Converter proposta em pedido de venda",
            description = "Cria um pedido de venda a partir de uma proposta ATIVA, copiando cliente, "
                    + "itens (snapshot), descontos, frete, condição de pagamento e observações. "
                    + "Campos opcionais no corpo (attention, paymentCondition, notes) sobrescrevem "
                    + "os valores da proposta. A margem de lucro NÃO é copiada — o pedido é o "
                    + "documento externo enviado ao cliente. A proposta é marcada como CONVERTIDA "
                    + "após a conversão e não pode ser reconvertida.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado a partir da proposta.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SalesOrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Proposta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Proposta não pode ser convertida "
                    + "(já convertida, cancelada ou expirada).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SalesOrderResponse> createFromQuotation(
            @PathVariable UUID quotationId,
            @RequestBody(required = false) SalesOrderFromQuotationRequest override) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(salesOrderService.createFromQuotation(quotationId, override));
    }

    // =====================================================================
    // Pré-visualização de número
    // =====================================================================

    @GetMapping(value = "/next-number", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pré-visualizar próximo número de pedido",
            description = "Retorna o próximo número sequencial (ex.: 1000, 1001, 1002, ...) "
                    + "que seria atribuído ao próximo pedido. Não persiste nada.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Próximo número retornado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NextSalesOrderNumberResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<NextSalesOrderNumberResponse> getNextNumber() {
        return ResponseEntity.ok(new NextSalesOrderNumberResponse(salesOrderService.getNextNumber()));
    }

    // =====================================================================
    // Busca / listagem
    // =====================================================================

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar/Buscar pedidos (paginado)",
            description = "Lista pedidos com filtros opcionais. Todos os filtros são opcionais; "
                    + "sem filtros, retorna todos os pedidos paginados. Ordenação padrão: "
                    + "data de emissão decrescente, depois número decrescente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de pedidos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<SalesOrderSummaryResponse>> search(
            @Parameter(description = "Filtro por status.", schema = @Schema(allowableValues = {
                    "ABERTO", "FINALIZADO", "CANCELADO"}))
            @RequestParam(value = "status", required = false) SalesOrderStatus status,

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

            @Parameter(description = "Trecho do número (ex.: '100' para 1000, 1001, ...).",
                    example = "100")
            @RequestParam(value = "number", required = false) String number,

            @Parameter(description = "Número da proposta de origem (filtro exato).",
                    example = "1500")
            @RequestParam(value = "quotationNumber", required = false) Long quotationNumber,

            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = {"orderDate", "number"}, direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(salesOrderService.search(
                status, startDate, endDate, clientUuid, sellerUuid, number, quotationNumber, pageable));
    }

    @GetMapping(value = "/by-number/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar pedido por número",
            description = "Retorna o pedido cujo número bate exatamente com o informado.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SalesOrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SalesOrderResponse> getByNumber(@PathVariable Long number) {
        return ResponseEntity.ok(salesOrderService.getByNumber(number));
    }

    @GetMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar pedido por ID",
            description = "Retorna o pedido com todos os itens e totais calculados.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SalesOrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SalesOrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(salesOrderService.getById(id));
    }

    // =====================================================================
    // Atualização
    // =====================================================================

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar pedido (parcial)",
            description = "Atualiza apenas os campos enviados. Ao enviar a lista de itens, "
                    + "os anteriores são removidos e os novos criados. Pedidos FINALIZADO "
                    + "ou CANCELADO não podem ser alterados. O número, a data de "
                    + "emissão, o status e a origem (quotationUuid/quotationNumber) não "
                    + "podem ser alterados por este endpoint.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SalesOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou invariante violada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Pedido em estado que impede edição.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SalesOrderResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody SalesOrderUpdateRequest request) {
        return ResponseEntity.ok(salesOrderService.update(id, request));
    }

    // =====================================================================
    // Transições de status
    // =====================================================================

    @PatchMapping(value = "/{id:" + UUID_REGEX + "}/advance-status",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Avançar status do pedido",
            description = "Avança o status do pedido para o próximo estado do ciclo: "
                    + "ABERTO → FINALIZADO. Ao finalizar, baixa o estoque dos itens "
                    + "(saídas registradas no diário de movimentações). Se o saldo de "
                    + "algum item for insuficiente, retorna 422 e o status não é "
                    + "alterado. Avançar a partir de estado terminal "
                    + "(FINALIZADO/CANCELADO) retorna 409.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status avançado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SalesOrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Não há próximo status a partir do atual.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SalesOrderResponse> advanceStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(salesOrderService.advanceStatus(id));
    }

    @DeleteMapping(value = "/{id:" + UUID_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cancelar pedido (soft)",
            description = "Define o status do pedido como CANCELADO. Não remove fisicamente o "
                    + "registro. Cancelar um pedido FINALIZADO estorna automaticamente as "
                    + "saídas de estoque registradas no finalizar, devolvendo o saldo dos "
                    + "itens ao estoque. Pedidos ABERTO são apenas cancelados (sem efeito "
                    + "sobre o estoque). Pedidos já CANCELADO retornam 409.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SalesOrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Pedido em estado que impede cancelamento.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<SalesOrderResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(salesOrderService.cancel(id));
    }
}