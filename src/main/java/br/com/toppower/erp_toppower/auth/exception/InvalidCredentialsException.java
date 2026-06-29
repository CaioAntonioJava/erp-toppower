package br.com.toppower.erp_toppower.auth.exception;

import org.springframework.security.authentication.BadCredentialsException;

public class InvalidCredentialsException extends BadCredentialsException {

    public InvalidCredentialsException() {
        super("E-mail e/ou senha inválidos");
    }
}
