package br.com.toppower.erp_toppower.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(name = "PagedResponse", description = "Resposta paginada genérica.")
public record PagedResponse<T>(

        @Schema(description = "Lista de elementos da página atual.")
        List<T> content,

        @Schema(description = "Número da página atual (0-indexed).", example = "0")
        int page,

        @Schema(description = "Tamanho da página solicitada.", example = "20")
        int size,

        @Schema(description = "Total de elementos em todas as páginas.", example = "125")
        long totalElements,

        @Schema(description = "Total de páginas disponíveis.", example = "7")
        int totalPages,

        @Schema(description = "Indica se esta é a primeira página.", example = "true")
        boolean first,

        @Schema(description = "Indica se esta é a última página.", example = "false")
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
