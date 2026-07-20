package br.com.toppower.erp_toppower.receivable.report.controller;

import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableAgingReportResponse;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableClientPositionReportResponse;
import br.com.toppower.erp_toppower.receivable.report.dto.ReceivableFlowReportResponse;
import br.com.toppower.erp_toppower.receivable.report.service.ReceivableReportService;
import br.com.toppower.erp_toppower.receivable.report.service.ReceivableReportService.Granularity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints de relatórios de contas a receber: aging (contas em aberto
 * por faixa de atraso), fluxo de recebimentos em um período e posição
 * consolidada por cliente.
 */
@RestController
@RequestMapping("/api/v1/accounts-receivable/reports")
@RequiredArgsConstructor
@Tag(name = "Contas a Receber — Relatórios",
        description = "Relatórios de contas a receber: aging, fluxo de recebimentos e posição por cliente.")
@SecurityRequirement(name = "bearerAuth")
public class ReceivableReportController {

    private final ReceivableReportService reportService;

    // ---------------------------------------------------------------------
    // Aging
    // ---------------------------------------------------------------------

    @GetMapping(value = "/aging", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Relatório aging de contas em aberto",
            description = "Totaliza o saldo devedor das contas ABERTO por faixa de atraso "
                    + "(0–30, 31–60, 61–90, 90+ dias) em relação à data de referência e por "
                    + "cliente. Data de referência default: hoje.")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableAgingReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableAgingReportResponse> aging(
            @Parameter(description = "Data de referência (yyyy-MM-dd). Default: hoje.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueTo,
            @Parameter(description = "Filtro por tipo de origem (opcional).")
            @RequestParam(required = false)
            ReceivableSource sourceType,
            @Parameter(description = "Filtro por cliente (customer ou company). Opcional.")
            @RequestParam(required = false)
            Long clientId) {
        return ResponseEntity.ok(reportService.aging(sourceType, clientId, dueTo));
    }

    // ---------------------------------------------------------------------
    // Fluxo de recebimentos
    // ---------------------------------------------------------------------

    @GetMapping(value = "/flow", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Relatório de recebimentos em um período",
            description = "Totaliza os pagamentos recebidos entre 'from' e 'to' (inclusive), "
                    + "agrupados por granularidade (dia/semana/mês) e por cliente.")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableFlowReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos (from/to obrigatórios).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableFlowReportResponse> flow(
            @Parameter(description = "Início do período (yyyy-MM-dd, inclusive). Obrigatório.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(description = "Fim do período (yyyy-MM-dd, inclusive). Obrigatório.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @Parameter(description = "Granularidade do agrupamento por período. Default: MONTH.")
            @RequestParam(required = false, defaultValue = "MONTH")
            Granularity granularity,
            @Parameter(description = "Filtro por tipo de origem (opcional).")
            @RequestParam(required = false)
            ReceivableSource sourceType,
            @Parameter(description = "Filtro por cliente (customer ou company). Opcional.")
            @RequestParam(required = false)
            Long clientId) {
        return ResponseEntity.ok(reportService.flow(sourceType, clientId, from, to, granularity));
    }

    // ---------------------------------------------------------------------
    // Posição por cliente
    // ---------------------------------------------------------------------

    @GetMapping(value = "/client-position", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Posição consolidada por cliente",
            description = "Total a receber, total recebido, contas em aberto, contas em atraso "
                    + "e maior atraso em dias por cliente. Data de referência default: hoje.")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableClientPositionReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableClientPositionReportResponse> clientPosition(
            @Parameter(description = "Data de referência (yyyy-MM-dd). Default: hoje.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueTo,
            @Parameter(description = "Filtro por tipo de origem (opcional).")
            @RequestParam(required = false)
            ReceivableSource sourceType,
            @Parameter(description = "Filtro por cliente (customer ou company). Opcional.")
            @RequestParam(required = false)
            Long clientId) {
        return ResponseEntity.ok(reportService.clientPosition(sourceType, clientId, dueTo));
    }
}