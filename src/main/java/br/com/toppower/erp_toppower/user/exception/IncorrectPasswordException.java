package br.com.toppower.erp_toppower.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException() {
        super("Senha atual incorreta");
    }
}
