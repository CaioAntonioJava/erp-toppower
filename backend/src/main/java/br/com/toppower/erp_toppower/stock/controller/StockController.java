package br.com.toppower.erp_toppower.stock.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.stock.dto.StockMovementResponse;
import br.com.toppower.erp_toppower.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock/movements")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Diário de movimentações de estoque (auditoria e histórico).")
public class StockController {

    private final StockService stockService;

    @GetMapping(value = "/product/{productId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Histórico de movimentações de um produto",
            description = "Lista paginada das movimentações de estoque do produto, "
                    + "mais recente primeiro. Inclui entradas, saídas e estornos. "
                    + "Nome e SKU do produto são resolvidos no momento da consulta.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de movimentações.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<StockMovementResponse>> historicoPorProduto(
            @PathVariable Long productId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(stockService.historicoPorProduto(productId, pageable));
    }

    @GetMapping(value = "/{movementId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Detalhe de uma movimentação",
            description = "Retorna os dados de uma movimentação de estoque específica, "
                    + "incluindo o saldo antes/depois e a referência ao documento de origem.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimentação encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StockMovementResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Movimentação não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<StockMovementResponse> movimentacaoPorId(@PathVariable Long movementId) {
        return ResponseEntity.ok(stockService.movimentacaoPorId(movementId));
    }
}