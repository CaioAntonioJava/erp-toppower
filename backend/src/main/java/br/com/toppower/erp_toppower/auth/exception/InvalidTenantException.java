package br.com.toppower.erp_toppower.auth.exception;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Lançada quando o usuário tenta logar ou trocar para um tenant ao qual não
 * está vinculado (não existe registro em {@code user_tenants}).
 *
 * <p>Estende {@link BadCredentialsException} para ser tratada pelo Spring Security
 * da mesma forma que credenciais inválidas (401), evitando revelar
 * diferencialmente se o problema foi email/senha vs. tenant — defesa
 * contra enumeração.</p>
 */
public class InvalidTenantException extends BadCredentialsException {

    public InvalidTenantException() {
        super("Usuário não possui acesso ao tenant informado");
    }
}