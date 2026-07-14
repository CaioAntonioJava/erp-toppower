package br.com.toppower.erp_toppower.contract.dto;

import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Resumo de um contrato usado na listagem paginada. Não traz os blocos
 * longos de texto (descrição, cláusula, serviços, produtos) — apenas
 * metadados para exibição rápida.
 */
@Schema(name = "ContractSummaryResponse",
        description = "Resumo de um contrato para listagem paginada.")
public record ContractSummaryResponse(

        @Schema(description = "Identificador único (UUID) do contrato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Código formatado completo (ex.: \"CT-001-2026\").",
                example = "CT-001-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Tipo do cliente referenciado pelo contrato.",
                allowableValues = {"CUSTOMER", "COMPANY"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ContractResponse.ClientType clientType,

        @Schema(description = "UUID do cliente (PF ou PJ).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long clientUuid,

        @Schema(description = "Nome de exibição do cliente (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientName,

        @Schema(description = "Código interno do cliente (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientCode,

        @Schema(description = "Status atual do contrato.",
                allowableValues = {"ABERTA", "EM_ANDAMENTO", "CONCLUIDA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ContractStatus status,

        @Schema(description = "Data de início da vigência.", example = "2026-07-10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate,

        @Schema(description = "Primeiros 120 caracteres da descrição do contrato "
                + "(para preview na listagem).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String descriptionPreview
) {
}