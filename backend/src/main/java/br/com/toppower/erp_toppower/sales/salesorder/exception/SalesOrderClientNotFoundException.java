package br.com.toppower.erp_toppower.sales.salesorder.exception;

import java.util.UUID;

/**
 * Lançada quando o cliente (pessoa física ou jurídica) referenciado pelo
 * pedido não existe no banco.
 */
public class SalesOrderClientNotFoundException extends RuntimeException {

    public SalesOrderClientNotFoundException(UUID uuid, String type) {
        super(type + " não encontrado: " + uuid);
    }
}