package br.com.toppower.erp_toppower.sales.salesorder.exception;

/**
 * Lançada quando nenhum pedido de venda é encontrado para o identificador
 * (ID numérico ou código formatado) informado.
 */
public class SalesOrderNotFoundException extends RuntimeException {

    public SalesOrderNotFoundException(Long id) {
        super("Pedido não encontrado: " + id);
    }

    public SalesOrderNotFoundException(String code) {
        super("Pedido não encontrado: " + code);
    }
}