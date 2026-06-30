package br.com.toppower.erp_toppower.seller.exception;

public class DuplicateSellerCpfException extends RuntimeException {

    public DuplicateSellerCpfException(String cpf) {
        super("Já existe um vendedor cadastrado com o CPF: " + cpf);
    }
}
