package br.com.toppower.erp_toppower.purchase.controller;

import br.com.toppower.erp_toppower.purchase.dto.NfeConfirmRequest;
import br.com.toppower.erp_toppower.purchase.dto.NfeConfirmResponse;
import br.com.toppower.erp_toppower.purchase.dto.NfePreviewResponse;
import br.com.toppower.erp_toppower.purchase.service.PurchaseImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/purchases/import-xml")
@RequiredArgsConstructor
@Tag(name = "Importação de NF-e",
        description = "Importação de XML de NF-e (nota de compra/entrada) com cadastro automático " +
                "de fornecedor, produtos, entrada de estoque e conta a pagar.")
public class PurchaseImportController {

    private final PurchaseImportService importService;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Preview da importação de NF-e",
            description = "Faz upload do XML da NF-e e retorna os dados extraídos (fornecedor, " +
                    "produtos com status e conta a pagar) sem persistir nada. O usuário revisa " +
                    "e confirma via endpoint /confirm.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preview gerado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NfePreviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "XML inválido ou mal formatado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<NfePreviewResponse> preview(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(importService.preview(file));
    }

    @PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirmar importação de NF-e",
            description = "Recebe o XML em Base64 (retornado no preview) e as decisões do usuário "
                    + "por item (cadastrar/estoque/ignorar), e efetiva a importação: cria fornecedor "
                    + "(sempre pelo CNPJ do emitente no XML — o usuário não pode atribuir a outro), "
                    + "produtos novos, entrada de estoque e conta a pagar com parcelas. Idempotente: "
                    + "rejeita notas já importadas pela Chave de Acesso.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Importação confirmada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NfeConfirmResponse.class))),
            @ApiResponse(responseCode = "400", description = "XML inválido, nota já importada ou decisão inconsistente.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Nota já importada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<NfeConfirmResponse> confirm(@Valid @RequestBody NfeConfirmRequest request) {
        return ResponseEntity.ok(importService.confirm(request.xmlBase64(), request.items()));
    }
}