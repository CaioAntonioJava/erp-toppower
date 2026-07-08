package br.com.toppower.erp_toppower.organization.dto;

import br.com.toppower.erp_toppower.organization.enums.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "OrganizationResponse", description = "Representação completa de uma Organization.")
public record OrganizationResponse(

        @Schema(description = "UUID da Organization.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Razão social.", requiredMode = Schema.RequiredMode.REQUIRED)
        String corporateName,

        @Schema(description = "Nome fantasia.", requiredMode = Schema.RequiredMode.REQUIRED)
        String tradeName,

        @Schema(description = "CNPJ.", requiredMode = Schema.RequiredMode.REQUIRED)
        String cnpj,

        @Schema(description = "Inscrição estadual.")
        String stateRegistration,

        @Schema(description = "Inscrição municipal.")
        String municipalRegistration,

        @Schema(description = "Telefone.")
        String phone,

        @Schema(description = "E-mail.")
        String email,

        @Schema(description = "CEP.")
        String zipCode,

        @Schema(description = "Logradouro.")
        String street,

        @Schema(description = "Número.")
        String number,

        @Schema(description = "Bairro.")
        String district,

        @Schema(description = "Cidade.")
        String city,

        @Schema(description = "UF.")
        String state,

        @Schema(description = "Complemento.")
        String complement,

        @Schema(description = "URL pública do logo da Organization "
                + "(ex.: /logos/<uuid>.png). Pode ser nula — nesse caso o "
                + "template do PDF usa o logo padrão.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String logoUrl,

        @Schema(description = "Status.", requiredMode = Schema.RequiredMode.REQUIRED)
        OrganizationStatus status,

        @Schema(description = "Prefixo do código das Propostas Técnicas emitidas "
                + "por esta Organization.", example = "PT",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String proposalPrefix,

        @Schema(description = "Data de criação.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data de atualização.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt
) {
}