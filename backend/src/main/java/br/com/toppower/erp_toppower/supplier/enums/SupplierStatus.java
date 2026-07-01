package br.com.toppower.erp_toppower.supplier.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado do fornecedor no cadastro: indica se está ativo
 * (pode fornecer produtos) ou inativo.
 */
@Schema(name = "SupplierStatus", description = "Status do fornecedor no cadastro.",
        allowableValues = {"ATIVO", "INATIVO"})
public enum SupplierStatus {
    ATIVO,
    INATIVO
}
