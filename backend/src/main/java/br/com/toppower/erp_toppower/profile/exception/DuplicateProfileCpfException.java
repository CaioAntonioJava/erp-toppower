package br.com.toppower.erp_toppower.profile.exception;

public class DuplicateProfileCpfException extends RuntimeException {

    public DuplicateProfileCpfException(String cpf) {
        super("Já existe um perfil cadastrado com o CPF: " + cpf);
    }
}
