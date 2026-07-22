package br.com.toppower.erp_toppower.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dados do fornecedor (emitente) extraídos da NF-e.
 */
@Schema(name = "NfeSupplierData",
        description = "Dados do fornecedor (emitente) extraídos da NF-e.")
public record NfeSupplierData(

        @Schema(description = "Indica se o fornecedor já está cadastrado.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean existing,

        @Schema(description = "ID do fornecedor, se já cadastrado.")
        Long id,

        @Schema(description = "CNPJ do emitente.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String taxId,

        @Schema(description = "Razão social do emitente.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String legalName,

        @Schema(description = "Nome fantasia do emitente.")
        String tradeName,

        @Schema(description = "Inscrição Estadual.")
        String stateRegistration,

        @Schema(description = "Inscrição Municipal.")
        String municipalRegistration,

        @Schema(description = "Endereço — logradouro.")
        String street,

        @Schema(description = "Endereço — número.")
        String number,

        @Schema(description = "Endereço — complemento.")
        String complement,

        @Schema(description = "Endereço — bairro.")
        String neighborhood,

        @Schema(description = "Endereço — cidade.")
        String city,

        @Schema(description = "Endereço — UF.")
        String state,

        @Schema(description = "Endereço — CEP.")
        String zipCode
) {
}