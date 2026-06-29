package br.com.toppower.erp_toppower.product.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID uuid) {
        super("Produto nao encontrado: " + uuid);
    }
}
