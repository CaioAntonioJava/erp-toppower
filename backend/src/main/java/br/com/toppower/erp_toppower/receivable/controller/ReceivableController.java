package br.com.toppower.erp_toppower.receivable.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableCreateRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentRequest;
import br.com.toppower.erp_toppower.receivable.dto.ReceivablePaymentResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableSummaryResponse;
import br.com.toppower.erp_toppower.receivable.dto.ReceivableUpdateRequest;
import br.com.toppower.erp_toppower.receivable.entity.ReceivablePayment;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableSource;
import br.com.toppower.erp_toppower.receivable.enums.ReceivableStatus;
import br.com.toppower.erp_toppower.receivable.mapper.ReceivableMapper;
import br.com.toppower.erp_toppower.receivable.service.ReceivableService;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts-receivable")
@RequiredArgsConstructor
@Tag(name = "Contas a Receber",
        description = "Cadastro e gestão de contas a receber, com pagamentos parciais e geração "
                + "automática a partir de pedidos de venda, propostas técnicas e contratos.")
public class ReceivableController {

    private final ReceivableService receivableService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar conta a receber (manual)",
            description = "Cria manualmente uma conta a receber. A origem é sempre MANUAL. "
                    + "Status default = ABERTO, paidAmount = 0.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody ReceivableCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receivableService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar contas a receber (paginado)",
            description = "Lista contas a receber paginadas, com filtros opcionais por status, "
                    + "origem, cliente, intervalo de vencimento e texto (descrição/código).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ReceivableSummaryResponse>> list(
            @Parameter(description = "Filtro opcional por status.",
                    example = "ABERTO",
                    schema = @Schema(allowableValues = {"ABERTO", "PAGO", "CANCELADO"}))
            @RequestParam(value = "status", required = false) ReceivableStatus status,
            @Parameter(description = "Filtro opcional por origem.",
                    example = "CONTRACT",
                    schema = @Schema(allowableValues = {"MANUAL", "SALES_ORDER", "TECHNICAL_PROPOSAL", "CONTRACT"}))
            @RequestParam(value = "sourceType", required = false) ReceivableSource sourceType,
            @Parameter(description = "ID do cliente/empresa (PF ou PJ).", example = "12")
            @RequestParam(value = "clientId", required = false) Long clientId,
            @Parameter(description = "Vencimento a partir de (yyyy-MM-dd).", example = "2026-07-01")
            @RequestParam(value = "dueFrom", required = false) LocalDate dueFrom,
            @Parameter(description = "Vencimento até (yyyy-MM-dd).", example = "2026-12-31")
            @RequestParam(value = "dueTo", required = false) LocalDate dueTo,
            @Parameter(description = "Termo de busca opcional (mín. 2 caracteres). Match em descrição/código.",
                    example = "contrato")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(receivableService.search(status, sourceType, clientId,
                dueFrom, dueTo, query, pageable));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar conta a receber por ID",
            description = "Retorna uma conta a receber pelo ID, incluindo o histórico de pagamentos.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(receivableService.getById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar conta a receber (parcial)",
            description = "Atualiza apenas campos editáveis (descrição, vencimento, condição de "
                    + "pagamento). Valor e origem não são alteráveis. Bloqueada para contas PAGO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta atualizada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Conta PAGO não pode ser alterada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody ReceivableUpdateRequest request) {
        return ResponseEntity.ok(receivableService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar conta a receber (soft delete)",
            description = "Define status como CANCELADO. Não remove fisicamente o registro. "
                    + "Bloqueada para contas PAGO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta cancelada."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Conta PAGO não pode ser cancelada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        receivableService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar conta a receber",
            description = "Volta uma conta CANCELADA para ABERTO (ou PAGO, se já quitada).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta reativada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Apenas contas CANCELADAS podem ser reativadas.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(receivableService.activate(id));
    }

    // =====================================================================
    // Pagamentos avulsos
    // =====================================================================

    @PostMapping(value = "/{id}/payments", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar pagamento",
            description = "Adiciona um pagamento avulso à conta, abatendo do saldo devedor. "
                    + "A conta transita para PAGO automaticamente quando o saldo zerar. "
                    + "Bloqueia pagamentos que excedam o saldo devedor.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento registrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Pagamento excede saldo ou conta não está ABERTO.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableResponse> registerPayment(@PathVariable Long id,
                                                             @Valid @RequestBody ReceivablePaymentRequest request) {
        return ResponseEntity.ok(receivableService.registerPayment(id, request));
    }

    @PostMapping(value = "/{id}/settle", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liquidar saldo devedor",
            description = "Cria um único pagamento cobrindo todo o saldo devedor restante da conta, "
                    + "transitando-a para PAGO. Rejeita contas que não estejam ABERTO ou que já "
                    + "estejam totalmente quitadas.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta liquidada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Conta não está ABERTO ou não há saldo devedor.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableResponse> settle(@PathVariable Long id) {
        return ResponseEntity.ok(receivableService.settle(id));
    }

    @DeleteMapping(value = "/{id}/payments/{paymentId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Remover pagamento",
            description = "Remove um pagamento da conta e recalcula paidAmount/status. "
                    + "Se a conta volta a ter saldo devedor, status vira ABERTO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento removido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivableResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta ou pagamento não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Conta CANCELADA não permite remover pagamentos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ReceivableResponse> removePayment(@PathVariable Long id,
                                                            @PathVariable Long paymentId) {
        return ResponseEntity.ok(receivableService.removePayment(id, paymentId));
    }

    @GetMapping(value = "/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar pagamentos da conta",
            description = "Retorna o histórico de pagamentos da conta, ordenado por data.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pagamentos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReceivablePaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ReceivablePaymentResponse>> listPayments(@PathVariable Long id) {
        List<ReceivablePayment> payments = receivableService.listPayments(id);
        return ResponseEntity.ok(payments.stream().map(ReceivableMapper::toPaymentResponse).toList());
    }
}