package br.com.toppower.erp_toppower.profile.exception;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(Long id) {
        super("Perfil não encontrado: " + id);
    }
}
