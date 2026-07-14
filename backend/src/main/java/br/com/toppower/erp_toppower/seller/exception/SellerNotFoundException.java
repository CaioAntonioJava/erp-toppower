package br.com.toppower.erp_toppower.seller.exception;

public class SellerNotFoundException extends RuntimeException {

    public SellerNotFoundException(Long id) {
        super("Vendedor não encontrado: " + id);
    }
}
