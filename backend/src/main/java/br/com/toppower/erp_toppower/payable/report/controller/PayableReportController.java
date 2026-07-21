package br.com.toppower.erp_toppower.payable.report.controller;

import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.report.dto.PayableAgingReportResponse;
import br.com.toppower.erp_toppower.payable.report.dto.PayableFlowReportResponse;
import br.com.toppower.erp_toppower.payable.report.dto.PayableSupplierPositionReportResponse;
import br.com.toppower.erp_toppower.payable.report.service.PayableReportService;
import br.com.toppower.erp_toppower.payable.report.service.PayableReportService.Granularity;
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
 * Endpoints de relatórios de contas a pagar: aging (parcelas em aberto
 * por faixa de atraso), fluxo de pagamentos em um período e posição
 * consolidada por fornecedor.
 */
@RestController
@RequestMapping("/api/v1/accounts-payable/reports")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar — Relatórios",
        description = "Relatórios de contas a pagar: aging, fluxo de pagamentos e posição por fornecedor.")
@SecurityRequirement(name = "bearerAuth")
public class PayableReportController {

    private final PayableReportService reportService;

    // ---------------------------------------------------------------------
    // Aging
    // ---------------------------------------------------------------------

    @GetMapping(value = "/aging", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Relatório aging de parcelas em aberto",
            description = "Totaliza o saldo devedor das parcelas ABERTO por faixa de atraso "
                    + "(0–30, 31–60, 61–90, 90+ dias) em relação à data de referência e por "
                    + "fornecedor. O aging é calculado pelo vencimento da parcela. "
                    + "Data de referência default: hoje.")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableAgingReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableAgingReportResponse> aging(
            @Parameter(description = "Data de referência (yyyy-MM-dd). Default: hoje.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueTo,
            @Parameter(description = "Filtro por tipo de origem (opcional).")
            @RequestParam(required = false)
            PayableSource sourceType,
            @Parameter(description = "Filtro por fornecedor. Opcional.")
            @RequestParam(required = false)
            Long supplierId) {
        return ResponseEntity.ok(reportService.aging(sourceType, supplierId, dueTo));
    }

    // ---------------------------------------------------------------------
    // Fluxo de pagamentos
    // ---------------------------------------------------------------------

    @GetMapping(value = "/flow", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Relatório de pagamentos em um período",
            description = "Totaliza os pagamentos realizados entre 'from' e 'to' (inclusive), "
                    + "agrupados por granularidade (dia/semana/mês) e por fornecedor.")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableFlowReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos (from/to obrigatórios).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableFlowReportResponse> flow(
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
            PayableSource sourceType,
            @Parameter(description = "Filtro por fornecedor. Opcional.")
            @RequestParam(required = false)
            Long supplierId) {
        return ResponseEntity.ok(reportService.flow(sourceType, supplierId, from, to, granularity));
    }

    // ---------------------------------------------------------------------
    // Posição por fornecedor
    // ---------------------------------------------------------------------

    @GetMapping(value = "/supplier-position", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Posição consolidada por fornecedor",
            description = "Total a pagar, total pago, parcelas em aberto, parcelas em atraso "
                    + "e maior atraso em dias por fornecedor. Data de referência default: hoje.")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableSupplierPositionReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableSupplierPositionReportResponse> supplierPosition(
            @Parameter(description = "Data de referência (yyyy-MM-dd). Default: hoje.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueTo,
            @Parameter(description = "Filtro por tipo de origem (opcional).")
            @RequestParam(required = false)
            PayableSource sourceType,
            @Parameter(description = "Filtro por fornecedor. Opcional.")
            @RequestParam(required = false)
            Long supplierId) {
        return ResponseEntity.ok(reportService.supplierPosition(sourceType, supplierId, dueTo));
    }
}