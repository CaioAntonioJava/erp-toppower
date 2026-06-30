package br.com.toppower.erp_toppower.profile.dto;

import br.com.toppower.erp_toppower.profile.enums.ProfileStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "ProfileResponse", description = "Representacao publica de um perfil retornado pela API.")
public record ProfileResponse(

        @Schema(description = "Identificador unico (UUID) do perfil.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Nome completo da pessoa.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "E-mail de contato do perfil.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Telefone de contato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @Schema(description = "CPF unico do perfil.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,

        @Schema(description = "Status atual do perfil.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ProfileStatus status,

        @Schema(description = "UUID do usuario vinculado (relacionamento 1:1).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID userId,

        @Schema(description = "Data de criacao.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da ultima atualizacao.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuario que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuario que fez a ultima atualizacao.")
        String updatedBy
) {
}
