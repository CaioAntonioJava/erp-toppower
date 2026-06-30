package br.com.toppower.erp_toppower.common.exception;

import br.com.toppower.erp_toppower.auth.exception.InvalidCredentialsException;
import org.springframework.security.authentication.BadCredentialsException;
import br.com.toppower.erp_toppower.product.exception.DuplicateProductCodeException;
import br.com.toppower.erp_toppower.product.exception.ProductNotFoundException;
import br.com.toppower.erp_toppower.profile.exception.DuplicateProfileCpfException;
import br.com.toppower.erp_toppower.profile.exception.DuplicateProfileEmailException;
import br.com.toppower.erp_toppower.profile.exception.ProfileNotFoundException;
import br.com.toppower.erp_toppower.profile.exception.UserAlreadyHasProfileException;
import br.com.toppower.erp_toppower.user.exception.EmailAlreadyExistsException;
import br.com.toppower.erp_toppower.user.exception.IncorrectPasswordException;
import br.com.toppower.erp_toppower.user.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> handleInvalidCredentials(BadCredentialsException ex) {
        // Mensagem generica para evitar login oracle (nao revela se o email existe ou nao)
        String message = (ex instanceof InvalidCredentialsException)
                ? ex.getMessage()
                : "E-mail e/ou senha inválidos";
        return build(HttpStatus.UNAUTHORIZED, message);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<ApiError> handleIncorrectPassword(IncorrectPasswordException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(ProductNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateProductCodeException.class)
    public ResponseEntity<ApiError> handleDuplicateProductCode(DuplicateProductCodeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiError> handleProfileNotFound(ProfileNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateProfileCpfException.class)
    public ResponseEntity<ApiError> handleDuplicateProfileCpf(DuplicateProfileCpfException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DuplicateProfileEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateProfileEmail(DuplicateProfileEmailException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyHasProfileException.class)
    public ResponseEntity<ApiError> handleUserAlreadyHasProfile(UserAlreadyHasProfileException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                Instant.now(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message) {
        ApiError error = new ApiError(status.value(), message, Instant.now());
        return ResponseEntity.status(status).body(error);
    }

    public record ApiError(
            int status,
            String message,
            Instant timestamp,
            Map<String, String> fieldErrors
    ) {
        public ApiError(int status, String message, Instant timestamp) {
            this(status, message, timestamp, null);
        }
    }
}
