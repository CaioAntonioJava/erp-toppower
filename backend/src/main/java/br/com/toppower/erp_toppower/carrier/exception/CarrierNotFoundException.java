package br.com.toppower.erp_toppower.carrier.exception;

public class CarrierNotFoundException extends RuntimeException {
    public CarrierNotFoundException(Long id) {
        super("Transportadora não encontrada: " + id);
    }
}