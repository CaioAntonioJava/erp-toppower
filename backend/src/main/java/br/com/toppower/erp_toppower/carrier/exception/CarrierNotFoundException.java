package br.com.toppower.erp_toppower.carrier.exception;

import java.util.UUID;

public class CarrierNotFoundException extends RuntimeException {
    public CarrierNotFoundException(UUID uuid) {
        super("Transportadora não encontrada: " + uuid);
    }
}