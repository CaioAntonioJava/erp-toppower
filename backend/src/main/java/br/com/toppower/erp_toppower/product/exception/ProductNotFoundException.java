package br.com.toppower.erp_toppower.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Produto não encontrado: " + id);
    }
}
