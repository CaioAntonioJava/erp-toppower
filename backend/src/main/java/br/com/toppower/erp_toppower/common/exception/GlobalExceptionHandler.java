package br.com.toppower.erp_toppower.common.exception;

import br.com.toppower.erp_toppower.auth.exception.InvalidCredentialsException;
import br.com.toppower.erp_toppower.carrier.exception.CarrierNotFoundException;
import br.com.toppower.erp_toppower.cep.exception.CepNotFoundException;
import br.com.toppower.erp_toppower.organization.exception.DuplicateOrganizationCnpjException;
import br.com.toppower.erp_toppower.organization.exception.DuplicateOrganizationContractPrefixException;
import br.com.toppower.erp_toppower.organization.exception.InvalidLogoException;
import br.com.toppower.erp_toppower.organization.exception.InvalidOrganizationHeaderException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationAccessDeniedException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationContextRequiredException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationInactiveException;
import br.com.toppower.erp_toppower.organization.exception.OrganizationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import br.com.toppower.erp_toppower.company.exception.CompanyNotFoundException;
import br.com.toppower.erp_toppower.company.exception.DuplicateCompanyCnpjException;
import br.com.toppower.erp_toppower.contract.exception.ContractBusinessException;
import br.com.toppower.erp_toppower.contract.exception.ContractCompanyNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.ContractCustomerNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.ContractNotFoundException;
import br.com.toppower.erp_toppower.contract.exception.InvalidContractClientException;
import br.com.toppower.erp_toppower.customer.exception.CustomerNotFoundException;
import br.com.toppower.erp_toppower.customer.exception.DuplicateCustomerCpfException;
import br.com.toppower.erp_toppower.product.exception.DuplicateProductCodeException;
import br.com.toppower.erp_toppower.product.exception.ProductNotFoundException;
import br.com.toppower.erp_toppower.profile.exception.DuplicateProfileCpfException;
import br.com.toppower.erp_toppower.profile.exception.DuplicateProfileEmailException;
import br.com.toppower.erp_toppower.profile.exception.ProfileNotFoundException;
import br.com.toppower.erp_toppower.profile.exception.UserAlreadyHasProfileException;
import br.com.toppower.erp_toppower.sales.quotation.exception.InvalidQuotationClientException;
import br.com.toppower.erp_toppower.sales.quotation.exception.QuotationBusinessException;
import br.com.toppower.erp_toppower.sales.quotation.exception.QuotationClientNotFoundException;
import br.com.toppower.erp_toppower.sales.quotation.exception.QuotationNotFoundException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.InvalidSalesOrderClientException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.QuotationAlreadyConvertedException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.SalesOrderBusinessException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.SalesOrderClientNotFoundException;
import br.com.toppower.erp_toppower.sales.salesorder.exception.SalesOrderNotFoundException;
import br.com.toppower.erp_toppower.seller.exception.DuplicateSellerCpfException;
import br.com.toppower.erp_toppower.stock.exception.InsufficientStockException;
import br.com.toppower.erp_toppower.stock.exception.StockMovementNotFoundException;
import br.com.toppower.erp_toppower.seller.exception.DuplicateSellerEmailException;
import br.com.toppower.erp_toppower.seller.exception.SellerNotFoundException;
import br.com.toppower.erp_toppower.supplier.exception.DuplicateSupplierCnpjException;
import br.com.toppower.erp_toppower.supplier.exception.SupplierNotFoundException;
import br.com.toppower.erp_toppower.user.exception.EmailAlreadyExistsException;
import br.com.toppower.erp_toppower.user.exception.IncorrectPasswordException;
import br.com.toppower.erp_toppower.user.exception.UserNotFoundException;
import br.com.toppower.erp_toppower.userorganization.exception.DuplicateUserOrganizationException;
import br.com.toppower.erp_toppower.userorganization.exception.UserOrganizationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> handleInvalidCredentials(BadCredentialsException ex) {
        // Mensagem genérica para evitar login oracle (não revela se o e-mail existe ou não)
        String message = (ex instanceof InvalidCredentialsException)
                ? ex.getMessage()
                : "E-mail e/ou senha inválidos";
        return build(HttpStatus.UNAUTHORIZED, message);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        // Mensagem genérica para não expor detalhes internos (ex: "Full authentication is required...").
        // Loga o detalhe técnico no servidor para diagnóstico.
        log.debug("Falha de autenticação: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Falha na autenticação. Faça login para continuar.");
    }

    /**
     * Acesso negado: o usuário está autenticado, mas não tem permissão
     * para a operação (ex: @PreAuthorize falhou).
     * <p>Spring Security 6+ lança {@link AuthorizationDeniedException};
     * versões anteriores e cenários de filtro lançam {@link AccessDeniedException}.
     * Tratamos ambos para manter compatibilidade.</p>
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiError> handleAccessDenied(RuntimeException ex) {
        // Log em WARN (não DEBUG) com as authorities do principal autenticado,
        // para permitir diagnosticar 403 inesperados em produção. O handler
        // mascara a mensagem original do Spring Security com um texto genérico,
        // então sem este log a causa real fica invisível.
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        String authorities = (auth != null)
                ? auth.getAuthorities().stream()
                        .map(java.util.Objects::toString)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("(nenhuma)")
                : "(sem autenticação)";
        log.warn("Acesso negado [authorities={}]: {}", authorities, ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Acesso negado. Você não tem permissão para esta operação.");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Falha ao gerar um PDF (Thymeleaf/OpenHTMLtoPDF). Loga a stack
     * trace completa em ERROR (necessário para diagnosticar problemas
     * de template em produção) e devolve 500 para o cliente.
     *
     * <p><b>Por que handler dedicado?</b> Antes, o
     * {@link SalesPdfService} lançava {@link IllegalStateException} ao
     * falhar a renderização. Esse handler genérico abaixo converte
     * qualquer {@link IllegalStateException} em HTTP 400 com a mensagem
     * "Selecione uma empresa ativa..." — mascarando a causa real (erro
     * de template, campo nulo, etc.) como se fosse problema de
     * Organization ativa.</p>
     */
    @ExceptionHandler(br.com.toppower.erp_toppower.sales.pdf.PdfGenerationException.class)
    public ResponseEntity<ApiError> handlePdfGeneration(br.com.toppower.erp_toppower.sales.pdf.PdfGenerationException ex) {
        log.error("Falha ao gerar PDF: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Falha ao gerar o PDF. Verifique os logs do servidor para detalhes.");
    }

    @ExceptionHandler(CepNotFoundException.class)
    public ResponseEntity<ApiError> handleCepNotFound(CepNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Trata {@link IllegalStateException} lançado quando uma operação de
     * negócio exige uma Organization ativa (via
     * {@code OrganizationContext.require()}) mas o contexto não está
     * populado — situação que ocorre quando o usuário (ex: ADMIN) opera
     * sem uma empresa selecionada no seletor do Topbar.
     *
     * <p>Em vez de propagar um 500 genérico, devolve um 400 com mensagem
     * acionável, orientando o usuário a selecionar uma empresa ativa antes
     * de cadastrar propostas/pedidos.</p>
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        log.warn("Estado inválido (Organization ativa ausente?): {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST,
                "Selecione uma empresa ativa no topo da tela antes de realizar esta operação.");
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

    @ExceptionHandler(SellerNotFoundException.class)
    public ResponseEntity<ApiError> handleSellerNotFound(SellerNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateSellerCpfException.class)
    public ResponseEntity<ApiError> handleDuplicateSellerCpf(DuplicateSellerCpfException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DuplicateSellerEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateSellerEmail(DuplicateSellerEmailException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ApiError> handleCompanyNotFound(CompanyNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateCompanyCnpjException.class)
    public ResponseEntity<ApiError> handleDuplicateCompanyCnpj(DuplicateCompanyCnpjException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleCustomerNotFound(CustomerNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateCustomerCpfException.class)
    public ResponseEntity<ApiError> handleDuplicateCustomerCpf(DuplicateCustomerCpfException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // =====================================================================
    // Propostas comerciais (Quotation)
    // =====================================================================

    @ExceptionHandler(QuotationNotFoundException.class)
    public ResponseEntity<ApiError> handleQuotationNotFound(QuotationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(QuotationClientNotFoundException.class)
    public ResponseEntity<ApiError> handleQuotationClientNotFound(QuotationClientNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidQuotationClientException.class)
    public ResponseEntity<ApiError> handleInvalidQuotationClient(InvalidQuotationClientException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(QuotationBusinessException.class)
    public ResponseEntity<ApiError> handleQuotationBusiness(QuotationBusinessException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // =====================================================================
    // Pedidos de venda (SalesOrder)
    // =====================================================================

    @ExceptionHandler(SalesOrderNotFoundException.class)
    public ResponseEntity<ApiError> handleSalesOrderNotFound(SalesOrderNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SalesOrderClientNotFoundException.class)
    public ResponseEntity<ApiError> handleSalesOrderClientNotFound(SalesOrderClientNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidSalesOrderClientException.class)
    public ResponseEntity<ApiError> handleInvalidSalesOrderClient(InvalidSalesOrderClientException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SalesOrderBusinessException.class)
    public ResponseEntity<ApiError> handleSalesOrderBusiness(SalesOrderBusinessException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(QuotationAlreadyConvertedException.class)
    public ResponseEntity<ApiError> handleQuotationAlreadyConverted(QuotationAlreadyConvertedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // =====================================================================
    // Estoque (Stock)
    // =====================================================================

    /**
     * Saldo insuficiente para concluir uma saída de estoque (ex.: ao
     * finalizar um pedido de venda). Usa 422 UNPROCESSABLE_ENTITY: a
     * requisição é sintaticamente válida, mas a regra de negócio impede
     * a execução. Distinto de 409 (conflito de estado) e 400 (entrada
     * malformada).
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(StockMovementNotFoundException.class)
    public ResponseEntity<ApiError> handleStockMovementNotFound(StockMovementNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // =====================================================================
    // Contratos (Contract)
    // =====================================================================

    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<ApiError> handleContractNotFound(ContractNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ContractCustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleContractCustomerNotFound(ContractCustomerNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ContractCompanyNotFoundException.class)
    public ResponseEntity<ApiError> handleContractCompanyNotFound(ContractCompanyNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidContractClientException.class)
    public ResponseEntity<ApiError> handleInvalidContractClient(InvalidContractClientException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ContractBusinessException.class)
    public ResponseEntity<ApiError> handleContractBusiness(ContractBusinessException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<ApiError> handleSupplierNotFound(SupplierNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateSupplierCnpjException.class)
    public ResponseEntity<ApiError> handleDuplicateSupplierCnpj(DuplicateSupplierCnpjException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CarrierNotFoundException.class)
    public ResponseEntity<ApiError> handleCarrierNotFound(CarrierNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // =====================================================================
    // Organization (multiempresa)
    // =====================================================================

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<ApiError> handleOrganizationNotFound(OrganizationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateOrganizationCnpjException.class)
    public ResponseEntity<ApiError> handleDuplicateOrganizationCnpj(DuplicateOrganizationCnpjException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DuplicateOrganizationContractPrefixException.class)
    public ResponseEntity<ApiError> handleDuplicateOrganizationContractPrefix(
            DuplicateOrganizationContractPrefixException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OrganizationInactiveException.class)
    public ResponseEntity<ApiError> handleOrganizationInactive(OrganizationInactiveException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(OrganizationAccessDeniedException.class)
    public ResponseEntity<ApiError> handleOrganizationAccessDenied(OrganizationAccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(OrganizationContextRequiredException.class)
    public ResponseEntity<ApiError> handleOrganizationContextRequired(OrganizationContextRequiredException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidOrganizationHeaderException.class)
    public ResponseEntity<ApiError> handleInvalidOrganizationHeader(InvalidOrganizationHeaderException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidLogoException.class)
    public ResponseEntity<ApiError> handleInvalidLogo(InvalidLogoException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserOrganizationException.class)
    public ResponseEntity<ApiError> handleDuplicateUserOrganization(DuplicateUserOrganizationException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UserOrganizationNotFoundException.class)
    public ResponseEntity<ApiError> handleUserOrganizationNotFound(UserOrganizationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
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

    /**
     * Parâmetro de URL ou query com tipo incorreto (ex.: UUID malformado,
     * data em formato inválido, valor de enum fora do conjunto).
     * Substitui a resposta 500 padrão do Spring por uma 400 com mensagem
     * útil para o cliente.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        Object value = ex.getValue();
        Class<?> requiredType = ex.getRequiredType();
        String typeName = (requiredType != null) ? requiredType.getSimpleName() : "valor válido";
        String message = String.format(
                "Parâmetro '%s' com valor '%s' não pode ser convertido para %s.",
                paramName, value, typeName);
        return build(HttpStatus.BAD_REQUEST, message);
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
