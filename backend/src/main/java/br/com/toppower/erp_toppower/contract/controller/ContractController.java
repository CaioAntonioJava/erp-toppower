package br.com.toppower.erp_toppower.contract.controller;

import br.com.toppower.erp_toppower.common.dto.PagedResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractCreateRequest;
import br.com.toppower.erp_toppower.contract.dto.ContractResponse;
import br.com.toppower.erp_toppower.contract.dto.ContractUpdateRequest;
import br.com.toppower.erp_toppower.contract.dto.NextContractCodeResponse;
import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import br.com.toppower.erp_toppower.contract.service.ContractPdfService;
import br.com.toppower.erp_toppower.contract.service.ContractService;
import br.com.toppower.erp_toppower.sales.quotation.dto.ClientSummaryResponse;
import br.com.toppower.erp_toppower.sales.quotation.dto.QuotationResponse;
import br.com.toppower.erp_toppower.sales.quotation.service.ClientSearchService;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contratos", description = "Cadastro e gestão de contratos de prestação de serviços. "
        + "O código comercial (CL-001-2026 / CT-001-2026) é gerado por Organization.")
public class ContractController {

    private final ContractService contractService;
    private final ContractPdfService contractPdfService;
    private final ClientSearchService clientSearchService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar contrato",
            description = "Cria um novo contrato. O código comercial (<prefix>-<seq>-<ano>) é gerado "
                    + "automaticamente pelo servidor a partir do prefixo da Organization ativa "
                    + "(CL = Top Power Materiais, CT = Top Power Engenharia). "
                    + "Deve referenciar exatamente um cliente (PF) ou uma empresa (PJ). "
                    + "O título é pré-preenchido como \"CONTRATO DE PRESTAÇÃO DE SERVIÇOS: <código>\" "
                    + "quando não enviado. A descrição pode ser pré-preenchida com o template padrão "
                    + "da Organization. Status default = ATIVO.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrato criado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação (cliente inválido, etc.).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "Organization ativa sem contract_prefix configurado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody ContractCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Listar contratos (paginado)",
            description = "Lista contratos paginados, ordenados pela data de vigência (mais recentes primeiro). "
                    + "Filtro opcional por status.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de contratos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ContractResponse>> getAll(
            @Parameter(description = "Filtro opcional: ATIVO, CONCLUIDO ou INATIVO.", example = "ATIVO",
                    schema = @Schema(allowableValues = {"ATIVO", "CONCLUIDO", "INATIVO"}))
            @RequestParam(value = "status", required = false) ContractStatus status,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "validityDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contractService.getAll(status, pageable));
    }

    @GetMapping(value = "/next-code", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pré-visualizar próximo código de contrato",
            description = "Retorna o próximo código comercial previsto (ex.: CL-001-2026) e o título padrão "
                    + "que seria atribuído, a partir do prefixo da Organization ativa e da sequência "
                    + "independente por Organization/ano. Não persiste nada.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Próximo código retornado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NextContractCodeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "Organization ativa sem contract_prefix configurado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<NextContractCodeResponse> getNextCode() {
        return ResponseEntity.ok(contractService.getNextCode());
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar contratos (paginado)",
            description = "Busca flexível: ambos os parâmetros são opcionais. "
                    + "Filtrar apenas por status: ?status=ATIVO. "
                    + "Filtrar por texto: ?query=CL-001. "
                    + "Combinar: ?status=ATIVO&query=prestação. "
                    + "Sem parâmetros: retorna todos (paginado). Match em código, título ou descrição.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de contratos retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<PagedResponse<ContractResponse>> search(
            @Parameter(description = "Termo de busca OPCIONAL (mínimo 2 caracteres quando informado). "
                    + "Match em código, título ou descrição.", example = "CL-001")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(description = "Filtro OPCIONAL: ATIVO, CONCLUIDO ou INATIVO. Omitido = todos.",
                    example = "ATIVO", schema = @Schema(allowableValues = {"ATIVO", "CONCLUIDO", "INATIVO"}))
            @RequestParam(value = "status", required = false) ContractStatus status,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "validityDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contractService.search(query, status, pageable));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar contrato por ID",
            description = "Retorna um contrato pelo ID.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getById(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar contrato (parcial)",
            description = "Atualiza apenas os campos enviados. O código comercial NÃO pode ser alterado. "
                    + "O cliente pode ser alterado enviando customerId ou companyId (nunca ambos). "
                    + "O título e a descrição são livremente editáveis.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato atualizado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ContractUpdateRequest request) {
        return ResponseEntity.ok(contractService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar contrato (soft delete)",
            description = "Define status como INATIVO. Não remove fisicamente o registro.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contrato inativado."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<Void> inactivate(@PathVariable Long id) {
        contractService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reativar contrato",
            description = "Define status como ATIVO, reativando um contrato inativo.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato reativado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.activate(id));
    }

    @PostMapping(value = "/{id}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Concluir contrato (ATIVO → CONCLUIDO)",
            description = "Transiciona o contrato do status ATIVO para CONCLUIDO e preenche "
                    + "automaticamente a data de entrega com a data atual.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato concluído.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Contrato em estado que impede concluir.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.complete(id));
    }

    @PostMapping(value = "/{id}/reopen", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reabrir contrato (CONCLUIDO → ATIVO)",
            description = "Reabre um contrato CONCLUIDO, voltando-o para ATIVO e limpando a "
                    + "data de entrega. Útil para corrigir conclusões indevidas.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato reaberto.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Contrato em estado que impede reabrir.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ContractResponse> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.reopen(id));
    }

    // ---------------------------------------------------------------------
    // PDF
    // ---------------------------------------------------------------------

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Baixar PDF do contrato",
            description = "Gera o PDF do contrato com cabeçalho da empresa emissora, dados do "
                    + "contratante, descrição e cláusulas. Use disposition=inline para preview "
                    + "no navegador ou disposition=attachment para download.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id,
            @Parameter(description = "inline (preview) ou attachment (download).",
                    example = "inline")
            @RequestParam(defaultValue = "inline") String disposition) {
        ContractResponse contract = contractService.getById(id);
        byte[] pdf = contractPdfService.renderPdf(id);
        String filename = "contrato-" + contract.code() + ".pdf";
        ContentDisposition cd = "attachment".equalsIgnoreCase(disposition)
                ? ContentDisposition.attachment().filename(filename).build()
                : ContentDisposition.inline().filename(filename).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // ---------------------------------------------------------------------
    // Endpoint de busca de clientes (PF e PJ) para seleção no contrato
    // ---------------------------------------------------------------------

    @GetMapping(value = "/clients/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar clientes (PF e PJ) para seleção em contratos",
            description = "Busca clientes (pessoa física) e empresas (pessoa jurídica) por match "
                    + "no nome (todas as palavras), no código ou no documento (CPF/CNPJ). "
                    + "Apenas clientes e empresas com status ATIVO são retornados. "
                    + "Resultados ordenados por nome, limitados a 20 (padrão) ou até 100.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ClientSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Termo de busca inválido (mín. 2 caracteres).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ClientSummaryResponse>> searchClients(
            @Parameter(description = "Termo de busca (mínimo 2 caracteres). Match em nome, código ou documento.",
                    example = "João", required = true)
            @RequestParam(value = "query") String query,
            @Parameter(description = "Limite máximo de resultados (padrão 20, máximo 100).")
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(description = "Filtro opcional por tipo de cliente: 'CUSTOMER' (apenas PF) "
                    + "ou 'COMPANY' (apenas PJ). Omitido = ambos.")
            @RequestParam(value = "type", required = false) String type) {
        QuotationResponse.ClientType parsedType = parseClientType(type);
        return ResponseEntity.ok(clientSearchService.search(query, limit, parsedType));
    }

    /**
     * Converte uma string bruta (ex.: {@code "CUSTOMER"}, {@code "COMPANY"})
     * para o enum {@link QuotationResponse.ClientType}. Valores inválidos,
     * ausentes ou em branco retornam {@code null} (sem filtro de tipo).
     */
    private static QuotationResponse.ClientType parseClientType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return QuotationResponse.ClientType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}