package br.com.toppower.erp_toppower.contract.dto;

import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Representação pública de um contrato retornada pela API.
 *
 * <p>Inclui os dados do cliente resolvido ({@link #clientType},
 * {@link #clientName}, {@link #clientCode}) para exibição no frontend,
 * além dos IDs originais ({@link #customerId}, {@link #companyId}).</p>
 */
@Schema(name = "ContractResponse", description = "Representação pública de um contrato retornada pela API.")
public record ContractResponse(

        @Schema(description = "Identificador único (ID) do contrato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "Prefixo do código comercial (ex.: \"CL\" ou \"CT\").",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String prefix,

        @Schema(description = "Numeral sequencial do código (reseta por ano).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long sequence,

        @Schema(description = "Ano de emissão do contrato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer year,

        @Schema(description = "Código formatado completo (ex.: \"CL-001-2026\").",
                example = "CL-001-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "ID do cliente pessoa física (Customer), quando aplicável.")
        Long customerId,

        @Schema(description = "ID da empresa pessoa jurídica (Company), quando aplicável.")
        Long companyId,

        @Schema(description = "Tipo do cliente: CUSTOMER (PF) ou COMPANY (PJ).",
                allowableValues = {"CUSTOMER", "COMPANY"})
        String clientType,

        @Schema(description = "Nome de exibição do cliente (PF: nome; PJ: nome fantasia).")
        String clientName,

        @Schema(description = "Código interno do cliente (ex.: CLI000001, EMP000001).")
        String clientCode,

        @Schema(description = "Título do contrato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "Descrição detalhada do contrato (texto livre / HTML).")
        String description,

        @Schema(description = "Status atual do contrato.",
                allowableValues = {"ATIVO", "INATIVO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ContractStatus status,

        @Schema(description = "Data de vigência do contrato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate validityDate,

        @Schema(description = "Preço do contrato (valor informativo, em reais). "
                + "Não é exibido no PDF do contrato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal price,

        @Schema(description = "Data de entrega (conclusão). Preenchida quando o "
                + "contrato transita para CONCLUIDO.")
        LocalDate deliveryDate,

        @Schema(description = "Data de criação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.")
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.")
        String updatedBy,

        @Schema(description = "Cláusulas do contrato, ordenadas por número.")
        List<ContractClauseResponse> clauses
) {
}