package br.com.toppower.erp_toppower.seller.exception;

import java.util.UUID;

public class SellerNotFoundException extends RuntimeException {

    public SellerNotFoundException(UUID uuid) {
        super("Vendedor não encontrado: " + uuid);
    }
}
