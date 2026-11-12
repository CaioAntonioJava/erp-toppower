package br.com.toppower.erp_toppower.cashflow.controller;

import br.com.toppower.erp_toppower.cashflow.dto.CashFlowEntryResponse;
import br.com.toppower.erp_toppower.cashflow.dto.CashFlowSettingsRequest;
import br.com.toppower.erp_toppower.cashflow.dto.CashFlowSettingsResponse;
import br.com.toppower.erp_toppower.cashflow.dto.CashFlowSummaryResponse;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowClientType;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowDocumentSource;
import br.com.toppower.erp_toppower.cashflow.enums.CashFlowEntryType;
import br.com.toppower.erp_toppower.cashflow.service.CashFlowAggregator.Granularity;
import br.com.toppower.erp_toppower.cashflow.service.CashFlowService;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.ClientSummaryResponse;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints do fluxo de caixa consolidado: saldo inicial configurável,
 * resumo com totais por período (mês/ano) e lançamentos únicos
 * (entradas de contas a receber + saídas de contas a pagar) com
 * filtros por tipo, documento de origem, cliente e busca livre.
 */
@RestController
@RequestMapping("/api/v1/cash-flow")
@RequiredArgsConstructor
@Tag(name = "Fluxo de Caixa",
        description = "Controle consolidado de caixa: saldo inicial, entradas e saídas "
                + "ligadas a pedidos de venda, propostas técnicas, contratos, boletos e notas de compra.")
@SecurityRequirement(name = "bearerAuth")
public class CashFlowController {

    private final CashFlowService cashFlowService;

    // ---------------------------------------------------------------------
    // Saldo inicial
    // ---------------------------------------------------------------------

    @GetMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Configuração do fluxo de caixa",
            description = "Retorna o saldo inicial do caixa da organização corrente. "
                    + "Quando ainda não configurado, retorna R$ 0,00.")
    @PreAuthorize("hasAuthority('MODULE_CASH_FLOW')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuração retornada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CashFlowSettingsResponse.class)))
    })
    public ResponseEntity<CashFlowSettingsResponse> getSettings() {
        return ResponseEntity.ok(cashFlowService.getSettings());
    }

    @PutMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualiza o saldo inicial do caixa",
            description = "Cria ou atualiza (upsert) o saldo inicial do fluxo de caixa "
                    + "da organização corrente. Aceita valor negativo.")
    @PreAuthorize("hasAuthority('MODULE_CASH_FLOW')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo inicial atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CashFlowSettingsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido (saldo ausente ou "
                    + "com escala maior que 2 casas).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Organização inativa ou sem acesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CashFlowSettingsResponse> updateSettings(
            @Valid @RequestBody CashFlowSettingsRequest request) {
        return ResponseEntity.ok(cashFlowService.updateSettings(request));
    }

    // ---------------------------------------------------------------------
    // Resumo (totais + série por período)
    // ---------------------------------------------------------------------

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Resumo consolidado do fluxo de caixa",
            description = "Totais de entradas/saídas/resultado no período (respeitando os "
                    + "filtros), saldo anterior e saldo final reais do caixa e série por mês "
                    + "ou ano com saldo acumulado.")
    @PreAuthorize("hasAuthority('MODULE_CASH_FLOW')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CashFlowSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Período inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CashFlowSummaryResponse> summary(
            @Parameter(description = "Início do período (yyyy-MM-dd, inclusive). Opcional "
                    + "(null = desde o início).")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "Fim do período (yyyy-MM-dd, inclusive). Opcional "
                    + "(null = até hoje).")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @Parameter(description = "Granularidade da série por período. Default: MONTH.")
            @RequestParam(required = false, defaultValue = "MONTH")
            Granularity granularity,
            @Parameter(description = "Tipo de movimento (opcional).")
            @RequestParam(required = false)
            CashFlowEntryType type,
            @Parameter(description = "Documento de origem (opcional).")
            @RequestParam(required = false)
            CashFlowDocumentSource sourceType,
            @Parameter(description = "Tipo de cliente PF/PJ (opcional; aplica-se a entradas).")
            @RequestParam(required = false)
            CashFlowClientType clientType,
            @Parameter(description = "ID do cliente (customer ou company). Opcional.")
            @RequestParam(required = false)
            Long clientId,
            @Parameter(description = "Busca livre por cliente, documento, descrição ou observação.")
            @RequestParam(required = false)
            String query) {
        return ResponseEntity.ok(cashFlowService.summary(
                startDate, endDate, granularity, type, sourceType, clientType, clientId, query));
    }

    // ---------------------------------------------------------------------
    // Lançamentos consolidados
    // ---------------------------------------------------------------------

    @GetMapping(value = "/entries", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lançamentos consolidados do fluxo de caixa",
            description = "Lista paginada dos lançamentos consolidados (entradas e saídas) "
                    + "no período, com documento de origem, cliente/fornecedor e saldo do "
                    + "caixa após cada movimento, ordenados pela data mais recente. "
                    + "O saldo é calculado sobre todos os movimentos (independe dos filtros).")
    @PreAuthorize("hasAuthority('MODULE_CASH_FLOW')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamentos listados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Período inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<CashFlowEntryResponse>> entries(
            @Parameter(description = "Início do período (yyyy-MM-dd, inclusive). Opcional.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "Fim do período (yyyy-MM-dd, inclusive). Opcional.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @Parameter(description = "Tipo de movimento (opcional).")
            @RequestParam(required = false)
            CashFlowEntryType type,
            @Parameter(description = "Documento de origem (opcional).")
            @RequestParam(required = false)
            CashFlowDocumentSource sourceType,
            @Parameter(description = "Tipo de cliente PF/PJ (opcional; aplica-se a entradas).")
            @RequestParam(required = false)
            CashFlowClientType clientType,
            @Parameter(description = "ID do cliente (customer ou company). Opcional.")
            @RequestParam(required = false)
            Long clientId,
            @Parameter(description = "Busca livre por cliente, documento, descrição ou observação.")
            @RequestParam(required = false)
            String query,
            @Parameter(description = "Número da página (0-indexed). Default: 0.")
            @RequestParam(required = false, defaultValue = "0")
            int page,
            @Parameter(description = "Tamanho da página. Default: 20.")
            @RequestParam(required = false, defaultValue = "20")
            int size) {
        return ResponseEntity.ok(cashFlowService.entries(
                startDate, endDate, type, sourceType, clientType, clientId, query, page, size));
    }

    // ---------------------------------------------------------------------
    // Typeahead de clientes
    // ---------------------------------------------------------------------

    @GetMapping(value = "/clients/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Busca de clientes (PF e PJ) para o filtro",
            description = "Typeahead de clientes ativos (pessoas físicas e jurídicas) por "
                    + "nome, código ou documento (CPF/CNPJ). Mínimo de 2 caracteres.")
    @PreAuthorize("hasAuthority('MODULE_CASH_FLOW')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clientes encontrados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400", description = "Termo de busca com menos de 2 caracteres.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ClientSummaryResponse>> searchClients(
            @Parameter(description = "Termo de busca (nome, código, CPF ou CNPJ).")
            @RequestParam(required = false)
            String query,
            @Parameter(description = "Limite de resultados. Default: 20.")
            @RequestParam(required = false)
            Integer limit) {
        return ResponseEntity.ok(cashFlowService.searchClients(query, limit));
    }
}