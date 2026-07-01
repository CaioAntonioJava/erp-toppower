package br.com.toppower.erp_toppower.product.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado do produto no cadastro: indica se está ativo para uso no sistema.
 */
@Schema(name = "ProductStatus", description = "Status do produto no cadastro.",
        allowableValues = {"ATIVO", "INATIVO"})
public enum ProductStatus {
    ATIVO,
    INATIVO
}
