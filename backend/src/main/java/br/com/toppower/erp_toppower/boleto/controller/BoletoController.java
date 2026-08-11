package br.com.toppower.erp_toppower.boleto.controller;

import br.com.toppower.erp_toppower.boleto.dto.BoletoCreateRequest;
import br.com.toppower.erp_toppower.boleto.dto.BoletoResponse;
import br.com.toppower.erp_toppower.boleto.dto.BoletoUpdateRequest;
import br.com.toppower.erp_toppower.boleto.dto.BoletoAttachmentResponse;
import br.com.toppower.erp_toppower.boleto.service.BoletoService;
import br.com.toppower.erp_toppower.boleto.service.BoletoAttachmentService;
import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.common.enums.RegistrationStatus;
import br.com.toppower.erp_toppower.payable.dto.PayableResponse;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/boletos")
@RequiredArgsConstructor
@Tag(name = "Boletos", description = "Cadastro e gestão de boletos (obra, responsável, empresa, NF, parcelas e vencimento).")
public class BoletoController {

    private final BoletoService boletoService;
    private final BoletoAttachmentService boletoAttachmentService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar boleto",
            description = "Cria um novo boleto (ou N boletos quando installmentsCount > 1). "
                    + "Cada boleto criado dispara a geração automática de uma conta a pagar. "
                    + "Retorna sempre uma lista (com 1 elemento para boleto avulso, ou N para parcelado).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Boleto(s) criado(s) com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "array", implementation = BoletoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Boleto duplicado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<BoletoResponse>> create(@Valid @RequestBody BoletoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boletoService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar boletos (paginado)",
            description = "Lista boletos paginados, ordenados por data de vencimento. " +
                    "Filtro opcional por status.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de boletos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<BoletoResponse>> getAll(
            @Parameter(description = "Filtro opcional: ATIVO ou INATIVO.", example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) RegistrationStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(boletoService.getAll(status, pageable));
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar boletos (paginado)",
            description = "Busca flexível: ambos os parâmetros são opcionais. " +
                    "Filtrar apenas por status: ?status=ATIVO. " +
                    "Filtrar por texto: ?query=xpto. " +
                    "Combinar: ?status=ATIVO&query=xpto. " +
                    "Sem parâmetros: retorna todos (paginado). Match em beneficiário, responsável, NF ou nº obra.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de boletos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<BoletoResponse>> search(
            @Parameter(description = "Termo de busca OPCIONAL (mínimo 2 caracteres quando informado). Match em beneficiário, responsável, NF ou nº obra.",
                    example = "Pagamento")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(description = "Filtro OPCIONAL: ATIVO ou INATIVO. Omitido = ambos.",
                    example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) RegistrationStatus status,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(boletoService.search(query, status, pageable));
    }

    @GetMapping(value = "/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Relatório de boletos (paginado e filtrado)",
            description = "Lista boletos paginados com filtros opcionais: status de registro " +
                    "(ATIVO/INATIVO) e status de pagamento (paid=true/false). O intervalo de " +
                    "datas (dueFrom/dueTo) é aplicado sobre a data de pagamento (paymentDate) " +
                    "apenas quando paid=true (boletos pagos); nos demais casos é ignorado. " +
                    "Ordenado por data de vencimento (asc).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de boletos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<BoletoResponse>> report(
            @Parameter(description = "Filtro OPCIONAL: ATIVO ou INATIVO. Omitido = ambos.",
                    example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "INATIVO"}))
            @RequestParam(value = "status", required = false) RegistrationStatus status,
            @Parameter(description = "Filtro OPCIONAL: true (pagos) ou false (em aberto). Omitido = ambos.",
                    example = "false")
            @RequestParam(value = "paid", required = false) Boolean paid,
            @Parameter(description = "Data a partir de (yyyy-MM-dd). Filtra por data de pagamento apenas quando paid=true; ignorado nos demais casos. Opcional.",
                    example = "2026-01-01")
            @RequestParam(value = "dueFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @Parameter(description = "Data até (yyyy-MM-dd). Filtra por data de pagamento apenas quando paid=true; ignorado nos demais casos. Opcional.",
                    example = "2026-12-31")
            @RequestParam(value = "dueTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @Parameter(description = "Filtro opcional por Nº Contrato/Obra (match exato).",
                    example = "CT-001-2026")
            @RequestParam(value = "contractWorkNumber", required = false) String contractWorkNumber,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(boletoService.getAllFiltered(status, paid, dueFrom, dueTo, contractWorkNumber, pageable));
    }

    @GetMapping(value = "/{id}/payment-receipt")
    @Operation(summary = "Baixar comprovante de pagamento do boleto",
            description = "Retorna o comprovante de pagamento (receipt) anexado à liquidação " +
                    "do boleto. Rastreia boleto → conta a pagar → pagamento → comprovante. " +
                    "Retorna 404 se o boleto não possuir comprovante vinculado.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprovante retornado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Boleto ou comprovante não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<byte[]> downloadPaymentReceipt(@PathVariable Long id) {
        br.com.toppower.erp_toppower.payable.service.PayablePaymentAttachmentService.LoadedFile loaded =
                boletoService.loadPaymentReceipt(id);
        if (loaded == null) {
            return ResponseEntity.notFound().build();
        }
        ContentDisposition cd = ContentDisposition.inline().filename(loaded.fileName()).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.parseMediaType(loaded.contentType()));
        headers.setContentLength(loaded.bytes().length);
        return ResponseEntity.ok().headers(headers).body(loaded.bytes());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar boleto por ID",
            description = "Retorna um boleto pelo ID.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Boleto encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BoletoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Boleto não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<BoletoResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(boletoService.getById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar boleto (parcial)",
            description = "Atualiza apenas os campos enviados (PATCH parcial).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Boleto atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BoletoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Boleto não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Boleto duplicado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<BoletoResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody BoletoUpdateRequest request) {
        return ResponseEntity.ok(boletoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar boleto (soft delete)",
            description = "Define status como INATIVO. Não remove fisicamente o registro.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Boleto inativado."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Boleto não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> inactivate(@PathVariable Long id) {
        boletoService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar boleto",
            description = "Define status como ATIVO, reativando um boleto inativo.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Boleto reativado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BoletoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Boleto não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<BoletoResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(boletoService.activate(id));
    }

    @PostMapping(value = "/{id}/settle",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liquidar boleto",
            description = "Marca o boleto como pago. Cria uma conta a pagar (se não existir) " +
                    "e a liquida, registrando o pagamento. Para boletos sem fornecedor, " +
                    "cria automaticamente o fornecedor genérico 'Boleto Avulso'. " +
                    "Aceita um comprovante de pagamento opcional (PDF/imagem).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Boleto liquidado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BoletoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Boleto não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Boleto já liquidado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<BoletoResponse> settle(
            @PathVariable Long id,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt) {
        return ResponseEntity.ok(boletoService.settle(id, receipt));
    }

    @PostMapping(value = "/{id}/to-payable", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Gerar conta a pagar a partir do boleto",
            description = "Gera uma conta a pagar no módulo de Contas a Pagar a partir deste " +
                    "boleto. O boleto deve possuir um fornecedor (supplier) vinculado. " +
                    "Idempotente: se já existe conta a pagar ativa vinculada, retorna o " +
                    "detalhe da conta existente.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta a pagar gerada (ou já existente).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PayableResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Boleto não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Boleto sem fornecedor vinculado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PayableResponse> toPayable(@PathVariable Long id) {
        return ResponseEntity.ok(boletoService.generatePayableFromBoleto(id));
    }

    // =================================================================
    // Anexos de boleto (PDF/imagens) — vários por boleto.
    // =================================================================

    @PostMapping(value = "/{boletoId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Anexar arquivo ao boleto",
            description = "Faz upload de um anexo (PDF, PNG ou JPEG; até 10MB) ao boleto. " +
                    "O boleto deve estar ATIVO. O arquivo é gravado em disco e os metadados no banco.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Anexo criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BoletoAttachmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido (tipo/tamanho) ou boleto inativo.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Boleto não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<BoletoAttachmentResponse> uploadAttachment(
            @PathVariable Long boletoId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boletoAttachmentService.upload(boletoId, file));
    }

    @GetMapping(value = "/{boletoId}/attachments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar anexos do boleto",
            description = "Retorna os metadados de todos os anexos de um boleto.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de anexos retornada com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<BoletoAttachmentResponse>> listAttachments(
            @PathVariable Long boletoId) {
        return ResponseEntity.ok(boletoAttachmentService.listByBoleto(boletoId));
    }

    @GetMapping(value = "/{boletoId}/attachments/{attachmentId}/file")
    @Operation(summary = "Baixar/exibir anexo do boleto",
            description = "Retorna o conteúdo do anexo. Use ?disposition=inline para exibir " +
                    "(preview/impressão no navegador) ou ?disposition=attachment para forçar download. " +
                    "Default: inline.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo retornado com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Anexo não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long boletoId,
            @PathVariable Long attachmentId,
            @RequestParam(value = "disposition", defaultValue = "inline") String disposition) {
        BoletoAttachmentService.LoadedFile loaded = boletoAttachmentService.loadFile(boletoId, attachmentId);
        ContentDisposition cd = "attachment".equalsIgnoreCase(disposition)
                ? ContentDisposition.attachment().filename(loaded.fileName()).build()
                : ContentDisposition.inline().filename(loaded.fileName()).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.parseMediaType(loaded.contentType()));
        headers.setContentLength(loaded.bytes().length);
        return ResponseEntity.ok().headers(headers).body(loaded.bytes());
    }

    @DeleteMapping(value = "/{boletoId}/attachments/{attachmentId}")
    @Operation(summary = "Remover anexo do boleto",
            description = "Remove o arquivo do disco e o registro de metadados do banco.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('MODULE_BOLETOS')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Anexo removido."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Anexo não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long boletoId,
            @PathVariable Long attachmentId) {
        boletoAttachmentService.delete(boletoId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}