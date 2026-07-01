package br.com.toppower.erp_toppower.client.exception;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(UUID uuid) {
        super("Cliente não encontrado: " + uuid);
    }
}
