package br.com.toppower.erp_toppower.seller.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado do vendedor no cadastro: indica se está ativo
 * para uso no sistema (pode receber vendas) ou inativo.
 */
@Schema(name = "SellerStatus", description = "Status do vendedor no cadastro.",
        allowableValues = {"ATIVO", "INATIVO"})
public enum SellerStatus {
    ATIVO,
    INATIVO
}
