package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Atualização parcial (PATCH) de um contrato.
 *
 * <p>Todos os campos são opcionais: envie apenas os que deseja alterar.
 * Para campos de texto livre, enviar {@code null} significa "não
 * alterar"; enviar string vazia ({@code ""}) significa "limpar o valor"
 * (converte para {@code null} na entidade).</p>
 *
 * <p>A invariante "exatamente um entre {@code customerUuid} e
 * {@code companyUuid}" é revalidada no serviço sempre que qualquer um
 * dos dois campos for enviado no PATCH.</p>
 *
 * <p>O código comercial ({@code CT-001-2026}), o status e os dados de
 * criação <b>não</b> podem ser alterados por este request. Use os
 * endpoints dedicados para transições de status
 * ({@code /start}, {@code /complete}, {@code /reopen}).</p>
 */
@Schema(name = "ContractUpdateRequest",
        description = "Dados para atualização parcial de um contrato (PATCH).")
public record ContractUpdateRequest(

        @Schema(description = "Novo UUID do cliente pessoa física contratante. "
                + "OBRIGATÓRIO se companyUuid não for informado neste PATCH.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "Novo UUID da empresa (pessoa jurídica) contratante. "
                + "OBRIGATÓRIO se customerUuid não for informado neste PATCH.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Novo endereço. Envie um objeto vazio (todos os campos "
                + "nulos) ou {@code null} para remover o endereço.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        ContractAddressRequest address,

        @Schema(description = "Nova descrição detalhada do contrato.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Nova lista de cláusulas (substitui a anterior por completo). "
                + "Se informada, deve conter ao menos uma cláusula.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<ContractClauseRequest> clauses,

        @Schema(description = "Nova descrição dos serviços. Envie string vazia para limpar.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String servicesDescription,

        @Schema(description = "Nova descrição dos produtos. Envie string vazia para limpar.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String productsDescription,

        @Schema(description = "Nova data de início da vigência (yyyy-MM-dd).",
                example = "2026-07-10", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate startDate,

        @Schema(description = "Nova lista de itens de serviço (substitui a anterior "
                + "por completo). Envie lista vazia para limpar.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<ContractServiceItemRequest> serviceItems,

        @Schema(description = "Nova lista de itens de produto (substitui a anterior "
                + "por completo). Envie lista vazia para limpar.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<ContractProductItemRequest> productItems,

        @Schema(description = "Novo valor total do contrato (preenchimento manual). "
                + "Envie null para não alterar, zero para limpar.",
                example = "15000.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal totalValue
) {
}