package br.com.toppower.erp_toppower.cep.controller;

import br.com.toppower.erp_toppower.cep.dto.CepImportResult;
import br.com.toppower.erp_toppower.cep.dto.CepResponse;
import br.com.toppower.erp_toppower.cep.service.CepImportService;
import br.com.toppower.erp_toppower.cep.service.CepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ceps")
@RequiredArgsConstructor
@Tag(name = "CEPs", description = "Lookup de endereço por CEP na base local (offline, sem API externa).")
public class CepController {

    /** CEP no path: 8 dígitos ou formato 00000-000. */
    private static final String CEP_REGEX = "\\d{5}-?\\d{3}";

    private final CepService cepService;
    private final CepImportService cepImportService;

    @GetMapping(value = "/{cep:" + CEP_REGEX + "}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buscar endereço por CEP (base local)",
            description = "Lookup 100% offline na tabela local `ceps`. Não consulta APIs externas. "
                    + "Use para preenchimento automático de endereço em formulários. "
                    + "Requer que a base tenha sido carregada via POST /api/v1/ceps/import.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CepResponse.class))),
            @ApiResponse(responseCode = "400", description = "CEP em formato inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "CEP não encontrado na base local.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CepResponse> findByCep(
            @Parameter(description = "CEP em formato 00000-000 ou 8 dígitos.", example = "01310-100")
            @PathVariable String cep) {
        return ResponseEntity.ok(cepService.findByCep(cep));
    }

    @GetMapping(value = "/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Quantidade de CEPs carregados",
            description = "Retorna o número de registros na base local. Útil para confirmar se a "
                    + "importação foi concluída. Apenas ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contagem retornada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CepCountResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Acesso negado (não é ADMIN).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CepCountResponse> count() {
        return ResponseEntity.ok(new CepCountResponse(cepService.count()));
    }

    @PostMapping(value = "/import", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Importar CSV de CEPs para a base local",
            description = "Carrega o CSV apontado por `app.cep.import.csv-path` (variável CEP_CSV_PATH) "
                    + "para a tabela `ceps` via INSERT IGNORE em lote. ~900k linhas em 30-60s. "
                    + "Por padrão aborta se a base já estiver populada; use `?force=true` para "
                    + "truncar e reimportar. Apenas ADMIN. Síncrono (bloqueante).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Importação concluída.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CepImportResult.class))),
            @ApiResponse(responseCode = "400", description = "CSV não configurado ou base já populada sem force=true.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "Acesso negado (não é ADMIN).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<CepImportResult> importCsv(
            @Parameter(description = "Se true, trunca a base existente antes de reimportar.",
                    example = "false")
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        return ResponseEntity.ok(cepImportService.importFromCsv(force));
    }

    @Schema(name = "CepCountResponse", description = "Quantidade de CEPs na base local.")
    public record CepCountResponse(
            @Schema(description = "Total de registros na tabela ceps.", example = "900123")
            long total
    ) {
    }
}