package br.com.toppower.erp_toppower.sales.salesorder.exception;

/**
 * Lançada quando o cliente (pessoa física ou jurídica) referenciado pelo
 * pedido não existe no banco.
 */
public class SalesOrderClientNotFoundException extends RuntimeException {

    public SalesOrderClientNotFoundException(Long id, String type) {
        super(type + " não encontrado: " + id);
    }
}