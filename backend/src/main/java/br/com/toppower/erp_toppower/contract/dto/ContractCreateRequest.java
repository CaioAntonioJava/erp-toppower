package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados para criação de um novo contrato.
 *
 * <p>O código comercial ({@code CT-001-2026} para Top Power Engenharia
 * ou {@code CL-001-2026} para Top Power Materiais) é gerado
 * automaticamente pelo servidor: prefixo lido da {@code Organization}
 * ativa ({@code OrganizationContext.contractPrefix}), sequência
 * reiniciando a {@code 1} a cada novo ano, ano corrente. O status
 * inicial é {@code ABERTA} e a data de início, se não informada, recebe
 * {@code LocalDate.now()} na persistência.</p>
 *
 * <p>O cliente é obrigatório e referenciado por <b>exatamente um</b>
 * entre {@link #customerUuid} (pessoa física) e
 * {@link #companyUuid} (pessoa jurídica). A validação dessa invariante
 * é feita no serviço.</p>
 */
@Schema(name = "ContractCreateRequest",
        description = "Dados para cadastro de um novo contrato.")
public record ContractCreateRequest(

        @Schema(description = "UUID do cliente pessoa física contratante. "
                + "OBRIGATÓRIO se companyUuid não for informado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "UUID da empresa (pessoa jurídica) contratante. "
                + "OBRIGATÓRIO se customerUuid não for informado.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Endereço do contrato (opcional). Quando preenchido, "
                + "tipicamente é sugerido a partir do cadastro do cliente.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        ContractAddressRequest address,

        @Schema(description = "Descrição detalhada do contrato (~1000 caracteres).",
                example = "Prestação de serviços de manutenção preventiva...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Descrição do contrato é obrigatória")
        String description,

        @Schema(description = "Cláusula contratual (texto livre, geralmente longo). "
                + "Pensada para um input largo no formulário.",
                example = "As partes acordam que...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Cláusula é obrigatória")
        String clause,

        @Schema(description = "Bloco de texto descrevendo os serviços do contrato (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String servicesDescription,

        @Schema(description = "Bloco de texto descrevendo os produtos do contrato (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String productsDescription,

        @Schema(description = "Data de início da vigência do contrato (yyyy-MM-dd). "
                + "Se omitida, usa a data atual.",
                example = "2026-07-10", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate startDate
) {
}