package br.com.toppower.erp_toppower.cep.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumo da importacao do CSV de CEPs executada via
 * {@code POST /api/v1/ceps/import}.
 */
@Schema(name = "CepImportResult", description = "Estatisticas da importacao do CSV de CEPs.")
public record CepImportResult(

        @Schema(description = "Total de linhas lidas do CSV (excluindo o header).",
                example = "900123")
        long totalLinhas,

        @Schema(description = "Registros efetivamente inseridos (INSERT IGNORE).",
                example = "898540")
        long importados,

        @Schema(description = "Linhas com CEP duplicado ignoradas pela PK.",
                example = "1583")
        long duplicadosIgnorados,

        @Schema(description = "Linhas invalidas/descartadas (CEP fora do formato, etc.).",
                example = "0")
        long erros,

        @Schema(description = "Duracao da importacao em milissegundos.", example = "41200")
        long duracaoMs
) {
}