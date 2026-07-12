package br.com.toppower.erp_toppower.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

        @Schema(description = "Cláusulas contratuais (lista de textos livres). "
                + "O contrato deve ter ao menos uma cláusula.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "O contrato deve ter ao menos uma cláusula")
        @Valid
        List<ContractClauseRequest> clauses,

        @Schema(description = "Bloco de texto descrevendo os serviços do contrato (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String servicesDescription,

        @Schema(description = "Bloco de texto descrevendo os produtos do contrato (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String productsDescription,

        @Schema(description = "Data de início da vigência do contrato (yyyy-MM-dd). "
                + "Se omitida, usa a data atual.",
                example = "2026-07-10", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate startDate,

        @Schema(description = "Itens de serviço do contrato (opcional). Cada item "
                + "possui apenas descrição — sem preço.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<ContractServiceItemRequest> serviceItems,

        @Schema(description = "Itens de produto do contrato (opcional). Cada item "
                + "possui referência ao produto + quantidade — sem preço.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        List<ContractProductItemRequest> productItems,

        @Schema(description = "Valor total do contrato (preenchimento manual). "
                + "Opcional — sem cálculo automático.",
                example = "15000.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal totalValue,

        @Schema(description = "Prazo de entrega do contrato (texto livre). "
                + "Opcional — sem semântica de data. Ex.: '30 dias úteis', "
                + "'15 dias após a assinatura', 'entrega imediata'.",
                example = "30 dias úteis",
                maxLength = 500,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 500, message = "Prazo de entrega deve ter no máximo {max} caracteres")
        String deliveryDeadline,

        @Schema(description = "Bloco de texto adicional ao final do contrato (opcional). "
                + "Renderizado após a Cláusula 3, antes do valor total, "
                + "no formulário, na página de detalhe e no PDF.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String additionalDescription
) {
}