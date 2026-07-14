package br.com.toppower.erp_toppower.customer.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("Cliente (pessoa física) não encontrado: " + id);
    }
}
