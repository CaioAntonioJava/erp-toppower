package br.com.toppower.erp_toppower.sales.salesorder.exception;

import java.util.UUID;

/**
 * Lançada quando nenhum pedido de venda é encontrado para o identificador
 * (UUID ou número) informado.
 */
public class SalesOrderNotFoundException extends RuntimeException {

    public SalesOrderNotFoundException(UUID uuid) {
        super("Pedido não encontrado: " + uuid);
    }

    public SalesOrderNotFoundException(Long number) {
        super("Pedido não encontrado: " + number);
    }
}