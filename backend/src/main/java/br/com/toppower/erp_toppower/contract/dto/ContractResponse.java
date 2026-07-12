package br.com.toppower.erp_toppower.contract.dto;

import br.com.toppower.erp_toppower.contract.enums.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representação completa de um contrato retornada pela API.
 */
@Schema(name = "ContractResponse",
        description = "Representação completa de um contrato.")
public record ContractResponse(

        @Schema(description = "Identificador único (UUID) do contrato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID uuid,

        @Schema(description = "Prefixo do código (ex.: \"CT\" ou \"CL\").",
                example = "CT", requiredMode = Schema.RequiredMode.REQUIRED)
        String prefix,

        @Schema(description = "Numeral sequencial do código (reseta por ano).",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long sequence,

        @Schema(description = "Ano do código.", example = "2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer year,

        @Schema(description = "Código formatado completo (ex.: \"CT-001-2026\").",
                example = "CT-001-2026", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "UUID do cliente pessoa física contratante "
                + "(presente quando o cliente for PF).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID customerUuid,

        @Schema(description = "UUID da empresa (pessoa jurídica) contratante "
                + "(presente quando o cliente for PJ).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UUID companyUuid,

        @Schema(description = "Tipo do cliente referenciado pelo contrato.",
                allowableValues = {"CUSTOMER", "COMPANY"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ClientType clientType,

        @Schema(description = "Nome de exibição do cliente (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientName,

        @Schema(description = "Código interno do cliente (resolvido no backend).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String clientCode,

        @Schema(description = "Endereço do contrato (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ContractAddressResponse address,

        @Schema(description = "Descrição detalhada do contrato.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Cláusulas contratuais.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ContractClauseResponse> clauses,

        @Schema(description = "Bloco de texto descrevendo os serviços (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String servicesDescription,

        @Schema(description = "Bloco de texto descrevendo os produtos (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String productsDescription,

        @Schema(description = "Status atual do contrato.",
                allowableValues = {"ABERTA", "EM_ANDAMENTO", "CONCLUIDA"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        ContractStatus status,

        @Schema(description = "Data de início da vigência.", example = "2026-07-10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate,

        @Schema(description = "Data de criação do registro.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,

        @Schema(description = "Data da última atualização.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant updatedAt,

        @Schema(description = "E-mail do usuário que criou o registro.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String createdBy,

        @Schema(description = "E-mail do usuário que fez a última atualização.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String updatedBy,

        @Schema(description = "Itens de serviço do contrato (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ContractServiceItemResponse> serviceItems,

        @Schema(description = "Itens de produto do contrato (opcional).",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<ContractProductItemResponse> productItems,

        @Schema(description = "Valor total do contrato (preenchimento manual).",
                example = "15000.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal totalValue,

        @Schema(description = "Prazo de entrega do contrato (texto livre, opcional).",
                example = "30 dias úteis",
                maxLength = 500,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String deliveryDeadline
) {

    /**
     * Tipo do cliente referenciado pelo contrato (polimorfismo por
     * duas FKs nullable). Usado para indicar qual campo
     * ({@code customerUuid} ou {@code companyUuid}) está populado.
     */
    public enum ClientType {
        CUSTOMER,
        COMPANY
    }
}