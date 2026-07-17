package br.com.toppower.erp_toppower.boleto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "BoletoAttachmentResponse",
        description = "Representação pública de um anexo de boleto (PDF ou imagem) retornado pela API.")
public record BoletoAttachmentResponse(

        @Schema(description = "Identificador único (ID) do anexo.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Identificador do boleto ao qual o anexo pertence.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long boletoId,

        @Schema(description = "Nome original do arquivo enviado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String fileName,

        @Schema(description = "Content-Type do arquivo (application/pdf, image/png, image/jpeg).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String contentType,

        @Schema(description = "Tamanho do arquivo em bytes.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long sizeBytes,

        @Schema(description = "URL (autenticada) para baixar/exibir o anexo.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String publicUrl,

        @Schema(description = "Data de criação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy
) {
}