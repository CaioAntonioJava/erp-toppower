package br.com.toppower.erp_toppower.payable.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableCreateRequest;
import br.com.toppower.erp_toppower.payable.dto.PayableInstallmentResponse;
import br.com.toppower.erp_toppower.payable.dto.PayablePaymentRequest;
import br.com.toppower.erp_toppower.payable.dto.PayablePaymentResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableSummaryResponse;
import br.com.toppower.erp_toppower.payable.dto.PayableUpdateRequest;
import br.com.toppower.erp_toppower.payable.entity.PayableInstallment;
import br.com.toppower.erp_toppower.payable.entity.PayablePayment;
import br.com.toppower.erp_toppower.payable.enums.PayableSource;
import br.com.toppower.erp_toppower.payable.enums.PayableStatus;
import br.com.toppower.erp_toppower.payable.mapper.PayableMapper;
import br.com.toppower.erp_toppower.payable.service.PayablePaymentAttachmentService;
import br.com.toppower.erp_toppower.payable.service.PayableService;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounts-payable")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar",
        description = "Cadastro e gestão de contas a pagar, com parcelas programadas, pagamentos "
                + "parciais e geração automática a partir de boletos vinculados a fornecedores.")
public class PayableController {

    private final PayableService payableService;
    private final PayablePaymentAttachmentService paymentAttachmentService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar conta a pagar (manual)",
            description = "Cria manualmente uma conta a pagar. A origem é sempre MANUAL. "
                    + "Status default = ABERTO, paidAmount = 0. As parcelas podem ser informadas "
                    + "explicitamente ou geradas automaticamente a partir da condição de "
                    + "pagamento; quando nenhuma das duas, cria-se uma única parcela à vista.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> create(@Valid @RequestBody PayableCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payableService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar contas a pagar (paginado)",
            description = "Lista contas a pagar paginadas, com filtros opcionais por status, "
                    + "origem, fornecedor, intervalo de vencimento e texto (descrição/número de "
                    + "nota de compra).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<PayableSummaryResponse>> list(
            @Parameter(description = "Filtro opcional por status.",
                    example = "ABERTO",
                    schema = @Schema(allowableValues = {"ABERTO", "PAGO", "CANCELADO"}))
            @RequestParam(value = "status", required = false) PayableStatus status,
            @Parameter(description = "Filtro opcional por origem.",
                    example = "BOLETO",
                    schema = @Schema(allowableValues = {"MANUAL", "BOLETO", "PURCHASE_INVOICE"}))
            @RequestParam(value = "sourceType", required = false) PayableSource sourceType,
            @Parameter(description = "ID do fornecedor.", example = "12")
            @RequestParam(value = "supplierId", required = false) Long supplierId,
            @Parameter(description = "Vencimento a partir de (yyyy-MM-dd).", example = "2026-07-01")
            @RequestParam(value = "dueFrom", required = false) LocalDate dueFrom,
            @Parameter(description = "Vencimento até (yyyy-MM-dd).", example = "2026-12-31")
            @RequestParam(value = "dueTo", required = false) LocalDate dueTo,
            @Parameter(description = "Termo de busca opcional (mín. 2 caracteres). Match em descrição/número de nota.",
                    example = "boleto")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(payableService.search(status, sourceType, supplierId,
                dueFrom, dueTo, query, pageable));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar conta a pagar por ID",
            description = "Retorna uma conta a pagar pelo ID, incluindo as parcelas programadas "
                    + "e o histórico de pagamentos.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(payableService.getById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar conta a pagar (parcial)",
            description = "Atualiza apenas campos editáveis (descrição, datas, condição de "
                    + "pagamento). Valor, origem, fornecedor, parcelas e vínculos não são "
                    + "alteráveis. Bloqueada para contas PAGO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta atualizada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Conta PAGO não pode ser alterada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody PayableUpdateRequest request) {
        return ResponseEntity.ok(payableService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar conta a pagar (soft delete)",
            description = "Define status como CANCELADO. Não remove fisicamente o registro. "
                    + "Cancela também as parcelas ABERTO sem pagamentos. Bloqueada para contas PAGO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
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
        payableService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar conta a pagar",
            description = "Volta uma conta CANCELADA para ABERTO (ou PAGO, se já quitada). "
                    + "Reativa também as parcelas CANCELADAS sem pagamentos.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta reativada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Apenas contas CANCELADAS podem ser reativadas.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(payableService.activate(id));
    }

    // =====================================================================
    // Parcelas programadas
    // =====================================================================

    @GetMapping(value = "/{id}/installments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar parcelas programadas da conta",
            description = "Retorna as parcelas programadas da conta, ordenadas por número.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de parcelas.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableInstallmentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<PayableInstallmentResponse>> listInstallments(@PathVariable Long id) {
        List<PayableInstallment> installments = payableService.listInstallments(id);
        return ResponseEntity.ok(installments.stream()
                .map(PayableMapper::toInstallmentResponse)
                .toList());
    }

    // =====================================================================
    // Pagamentos (contra parcelas)
    // =====================================================================

    @PostMapping(value = "/{id}/installments/{installmentId}/payments",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar pagamento em parcela",
            description = "Adiciona um pagamento avulso contra uma parcela, abatendo do saldo "
                    + "da parcela. A parcela transita para PAGO automaticamente quando o saldo "
                    + "zerar, e a conta transita para PAGO quando todas as parcelas estão "
                    + "quitadas. Bloqueia pagamentos que excedam o saldo da parcela.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento registrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conta ou parcela não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Pagamento excede saldo ou parcela não está ABERTO.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> registerPayment(@PathVariable Long id,
                                                           @PathVariable Long installmentId,
                                                           @Valid @RequestBody PayablePaymentRequest request) {
        return ResponseEntity.ok(payableService.registerPayment(id, installmentId, request));
    }

    @PostMapping(value = "/{id}/installments/{installmentId}/settle",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liquidar saldo de parcela",
            description = "Cria um único pagamento cobrindo todo o saldo devedor restante da "
                    + "parcela, transitando-a para PAGO. Rejeita parcelas que não estejam ABERTO "
                    + "ou que já estejam totalmente quitadas.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcela liquidada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta ou parcela não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Parcela não está ABERTO ou não há saldo.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> settleInstallment(@PathVariable Long id,
                                                            @PathVariable Long installmentId) {
        return ResponseEntity.ok(payableService.settleInstallment(id, installmentId));
    }

    @PostMapping(value = "/{id}/settle", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liquidar todas as parcelas abertas",
            description = "Cria pagamentos cobrindo o saldo devedor de todas as parcelas ABERTO "
                    + "da conta, transitando-a para PAGO. Rejeita contas que não estejam ABERTO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta liquidada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Conta não está ABERTO.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> settle(@PathVariable Long id) {
        return ResponseEntity.ok(payableService.settle(id));
    }

    @DeleteMapping(value = "/{id}/payments/{paymentId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Remover pagamento",
            description = "Remove um pagamento da conta e recalcula paidAmount/status da "
                    + "parcela e da conta. Se a parcela volta a ter saldo devedor, status vira "
                    + "ABERTO novamente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento removido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta ou pagamento não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Conta CANCELADA não permite remover pagamentos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> removePayment(@PathVariable Long id,
                                                        @PathVariable Long paymentId) {
        return ResponseEntity.ok(payableService.removePayment(id, paymentId));
    }

    @GetMapping(value = "/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar pagamentos da conta",
            description = "Retorna o histórico de pagamentos da conta, ordenado por data.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pagamentos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayablePaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<PayablePaymentResponse>> listPayments(@PathVariable Long id) {
        List<PayablePayment> payments = payableService.listPayments(id);
        List<PayableInstallment> installments = payableService.listInstallments(id);
        Map<Long, PayableInstallment> installmentById = installments.stream()
                .collect(Collectors.toMap(PayableInstallment::getId, Function.identity()));
        return ResponseEntity.ok(payments.stream()
                .map(p -> PayableMapper.toPaymentResponse(p, installmentById))
                .toList());
    }

    // =====================================================================
    // Comprovante de pagamento (receipt)
    // =====================================================================

    @GetMapping(value = "/payments/{paymentId}/receipt")
    @Operation(summary = "Baixar comprovante de pagamento",
            description = "Retorna o arquivo de comprovante anexado ao pagamento.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_PAYABLES')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo retornado com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Pagamento ou comprovante não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long paymentId) {
        PayablePaymentAttachmentService.LoadedFile loaded = paymentAttachmentService.loadFile(paymentId);
        ContentDisposition cd = ContentDisposition.inline().filename(loaded.fileName()).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.parseMediaType(loaded.contentType()));
        headers.setContentLength(loaded.bytes().length);
        return ResponseEntity.ok().headers(headers).body(loaded.bytes());
    }
}