package br.com.toppower.erp_toppower.seller.exception;

public class DuplicateSellerEmailException extends RuntimeException {

    public DuplicateSellerEmailException(String email) {
        super("Já existe um vendedor cadastrado com o e-mail: " + email);
    }
}
