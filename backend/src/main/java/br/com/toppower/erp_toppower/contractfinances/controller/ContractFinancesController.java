package br.com.toppower.erp_toppower.contractfinances.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceControlResponse;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryCreateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryResponse;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinanceEntryUpdateRequest;
import br.com.toppower.erp_toppower.contractfinances.dto.ContractFinancesSummaryResponse;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryStatus;
import br.com.toppower.erp_toppower.contractfinances.enums.ContractFinanceEntryType;
import br.com.toppower.erp_toppower.contractfinances.service.ContractFinancesService;
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

/**
 * Endpoints do financeiro de contratos: controles automáticos por
 * contrato (com totais recebido/gasto/resultado) e lançamentos de
 * gastos e recebimentos (RECEBIDO, MATERIAL, COMBUSTIVEL, REFEICAO,
 * PEDAGIO, TERCEIROS, OUTROS, SALARIOS).
 */
@RestController
@RequestMapping("/api/v1/contract-finances")
@RequiredArgsConstructor
@Tag(name = "Financeiro de Contratos",
        description = "Controle de gastos e recebimentos por contrato: controles automáticos "
                + "com totais e lançamentos de gasto/recebimento vinculados.")
@SecurityRequirement(name = "bearerAuth")
public class ContractFinancesController {

    private final ContractFinancesService service;

    // ---------------------------------------------------------------------
    // Controles (um por contrato)
    // ---------------------------------------------------------------------

    @GetMapping(value = "/controls", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lista os controles do financeiro de contratos",
            description = "Lista paginada dos controles de contrato, com busca opcional por "
                    + "código do contrato ou nome do cliente, já com os totais agregados "
                    + "(recebido, gasto, resultado) dos lançamentos ATIVO.")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Controles listados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado (sem o módulo MODULE_CONTRACT_FINANCES).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ContractFinanceControlResponse>> listControls(
            @Parameter(description = "Termo de busca (código do contrato ou nome do cliente).")
            @RequestParam(required = false)
            String query,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "contractDate",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listControls(query, pageable));
    }

    @GetMapping(value = "/controls/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Detalhe do controle de um contrato",
            description = "Retrato comercial do contrato (código, cliente, data, valor) com os "
                    + "totais agregados dos lançamentos ATIVO.")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Controle retornado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractFinanceControlResponse.class))),
            @ApiResponse(responseCode = "404", description = "Controle não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractFinanceControlResponse> getControl(@PathVariable Long id) {
        return ResponseEntity.ok(service.getControl(id));
    }

    // ---------------------------------------------------------------------
    // Lançamentos (gastos/recebimentos)
    // ---------------------------------------------------------------------

    @PostMapping(value = "/entries", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lança um gasto ou recebimento no financeiro de um contrato",
            description = "Cria um lançamento vinculado ao controle do contrato informado. "
                    + "Contratos INATIVO não aceitam lançamentos.")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lançamento criado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractFinanceEntryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "Contrato INATIVO não aceita lançamentos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractFinanceEntryResponse> createEntry(
            @Valid @RequestBody ContractFinanceEntryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createEntry(request));
    }

    @GetMapping(value = "/entries", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lista os lançamentos do financeiro de contratos",
            description = "Lista paginada dos lançamentos com filtros combináveis (contrato, "
                    + "natureza, status e período), ordenada pela data mais recente.")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamentos listados.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class)))
    })
    public ResponseEntity<PagedResponse<ContractFinanceEntryResponse>> listEntries(
            @Parameter(description = "ID do controle do contrato (null = todos os contratos).")
            @RequestParam(required = false) Long controlId,
            @Parameter(description = "Natureza do lançamento (opcional).")
            @RequestParam(required = false) ContractFinanceEntryType type,
            @Parameter(description = "Status do lançamento (opcional).")
            @RequestParam(required = false) ContractFinanceEntryStatus status,
            @Parameter(description = "Início do período (yyyy-MM-dd, inclusive; opcional).")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fim do período (yyyy-MM-dd, inclusive; opcional).")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "entryDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(service.listEntries(controlId, type, status, from, to, pageable));
    }

    @GetMapping(value = "/entries/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Resumo consolidado do financeiro de contratos",
            description = "Totais de recebido, gasto, resultado e quebra por natureza, "
                    + "considerando apenas os lançamentos ATIVO e respeitando os filtros "
                    + "informados.")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo gerado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractFinancesSummaryResponse.class)))
    })
    public ResponseEntity<ContractFinancesSummaryResponse> summary(
            @Parameter(description = "ID do controle do contrato (opcional).")
            @RequestParam(required = false) Long controlId,
            @Parameter(description = "Natureza do lançamento (opcional).")
            @RequestParam(required = false) ContractFinanceEntryType type,
            @Parameter(description = "Início do período (opcional).")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fim do período (opcional).")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.summary(controlId, type, from, to));
    }

    @GetMapping(value = "/entries/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Detalhe do lançamento",
            description = "Retorna o lançamento com os dados do controle do contrato resolvidos.")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamento retornado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractFinanceEntryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractFinanceEntryResponse> getEntry(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEntry(id));
    }

    @PatchMapping(value = "/entries/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Edita um lançamento (PATCH parcial)",
            description = "Campos nulos preservam o valor atual. Não altera o vínculo com o "
                    + "contrato nem o status.")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamento atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractFinanceEntryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractFinanceEntryResponse> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody ContractFinanceEntryUpdateRequest request) {
        return ResponseEntity.ok(service.updateEntry(id, request));
    }

    @DeleteMapping("/entries/{id}")
    @Operation(summary = "Inativa um lançamento (soft delete)",
            description = "Marca o lançamento como INATIVO — permanece no histórico e para de "
                    + "somar nos totais.")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lançamento inativado."),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> softDeleteEntry(@PathVariable Long id) {
        service.softDeleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/entries/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativa um lançamento inativado",
            description = "Marca o lançamento como ATIVO (volta a somar nos totais).")
    @PreAuthorize("hasAuthority('MODULE_CONTRACT_FINANCES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lançamento reativado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractFinanceEntryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Lançamento não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractFinanceEntryResponse> activateEntry(@PathVariable Long id) {
        return ResponseEntity.ok(service.activateEntry(id));
    }
}
