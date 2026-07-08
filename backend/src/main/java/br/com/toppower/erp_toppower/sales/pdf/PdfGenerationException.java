package br.com.toppower.erp_toppower.sales.pdf;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Falha ao gerar um PDF (renderização Thymeleaf, conversão Flying
 * Saucer, ou dados insuficientes). Mapeada para HTTP 500 pelo
 * {@link org.springframework.http.HttpStatus#INTERNAL_SERVER_ERROR}
 * via {@link ResponseStatus} — distinguindo-a do
 * {@link IllegalStateException} genérico, que é capturado pelo
 * {@code GlobalExceptionHandler} e devolvido como 400 ("selecione uma
 * empresa ativa"), mascarando a causa real.
 *
 * <p>Mensagem inclui o template que falhou e o motivo raiz, para o
 * admin/frontend conseguir debugar.</p>
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message) {
        super(message);
    }

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}