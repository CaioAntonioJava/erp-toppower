package br.com.toppower.erp_toppower.customer.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(UUID uuid) {
        super("Cliente (pessoa física) não encontrado: " + uuid);
    }
}
