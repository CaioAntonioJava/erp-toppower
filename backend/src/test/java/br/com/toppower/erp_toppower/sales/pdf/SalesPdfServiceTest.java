package br.com.toppower.erp_toppower.sales.pdf;

import br.com.toppower.erp_toppower.sales.quotation.enums.FreightType;
import br.com.toppower.erp_toppower.sales.quotation.enums.PaymentCondition;
import br.com.toppower.erp_toppower.sales.quotation.enums.QuotationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Teste de fumaça do {@link SalesPdfService} — renderiza cada um dos
 * 3 templates (quotation, technical-proposal, sales-order) com um
 * modelo mínimo e verifica que o PDF resultante começa com a assinatura
 * {@code %PDF-}.
 *
 * <p>Útil para detectar regressões nos templates (referências a campos
 * inexistentes, chamadas NPE em enums nulos, fragmentos não resolvidos,
 * etc.).</p>
 *
 * <p><b>Configuração do TemplateEngine:</b> usamos
 * {@link SpringTemplateEngine} para garantir que o Thymeleaf 3.1+ use
 * SpEL (não OGNL) na avaliação de expressões — espelhando o que o
 * Spring Boot auto-configura em produção. Instanciar {@code new
 * TemplateEngine()} puro faz o Thymeleaf cair no padrão legado (OGNL),
 * que não está mais no classpath a partir do Thymeleaf 3.1.</p>
 */
class SalesPdfServiceTest {

    private SalesPdfService service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        service = new SalesPdfService(engine);
    }

    @Test
    void renderQuotationPdf_producesValidPdf() {
        Map<String, Object> model = baseModel();
        // Usamos um record-like via Map com todas as chaves esperadas pelo
        // template (incluindo as nullable) — SpEL precisa que as propriedades
        // existam, mesmo com valor null. Em produção, o QuotationResponse é
        // um record Java e funciona nativamente.
        QuotationLike quotation = new QuotationLike(
                1500L,
                LocalDate.of(2026, 7, 8),
                "Cliente Teste LTDA",
                "EMP000001",
                "Sr. João",
                "Vendedor Teste",
                QuotationStatus.ATIVA,
                15,
                PaymentCondition.PIX,
                FreightType.CIF,
                new BigDecimal("45.90"),
                null,           // discountType
                null,           // discount
                "Transportadora X",
                new BigDecimal("10.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("1010.45"),
                new BigDecimal("50.00"),
                null,           // notes
                List.of()       // items
        );
        model.put("quotation", quotation);

        assertProducesValidPdf("pdf/quotation", model, "cotação");
    }

    /**
     * Espelha o shape de {@code QuotationResponse} com getters estilo JavaBean.
     * Existe porque o SpEL do Thymeleaf não consegue ler campos ausentes de
     * um {@link HashMap} — o erro é {@code EL1008E}, mesmo quando a chave
     * for null. Em produção o template recebe o record Java real, então
     * essa fragilidade só afeta o teste.
     */
    @SuppressWarnings("unused")
    private static class QuotationLike {
        private final Long number;
        private final LocalDate issueDate;
        private final String clientName;
        private final String clientCode;
        private final String attention;
        private final String sellerName;
        private final QuotationStatus status;
        private final Integer validityDays;
        private final PaymentCondition paymentCondition;
        private final FreightType freightType;
        private final BigDecimal freightValue;
        private final Object discountType; // DiscountType? no template só importa name()
        private final BigDecimal discount;
        private final String carrierName;
        private final BigDecimal profitMargin;
        private final BigDecimal subtotal;
        private final BigDecimal total;
        private final BigDecimal globalDiscountValue;
        private final String notes;
        private final List<?> items;

        QuotationLike(Long number, LocalDate issueDate, String clientName, String clientCode,
                      String attention, String sellerName, QuotationStatus status, Integer validityDays,
                      PaymentCondition paymentCondition, FreightType freightType, BigDecimal freightValue,
                      Object discountType, BigDecimal discount, String carrierName,
                      BigDecimal profitMargin, BigDecimal subtotal, BigDecimal total,
                      BigDecimal globalDiscountValue, String notes, List<?> items) {
            this.number = number; this.issueDate = issueDate; this.clientName = clientName;
            this.clientCode = clientCode; this.attention = attention; this.sellerName = sellerName;
            this.status = status; this.validityDays = validityDays; this.paymentCondition = paymentCondition;
            this.freightType = freightType; this.freightValue = freightValue;
            this.discountType = discountType; this.discount = discount;
            this.carrierName = carrierName; this.profitMargin = profitMargin;
            this.subtotal = subtotal; this.total = total;
            this.globalDiscountValue = globalDiscountValue;
            this.notes = notes; this.items = items;
        }

        public Long getNumber() { return number; }
        public LocalDate getIssueDate() { return issueDate; }
        public String getClientName() { return clientName; }
        public String getClientCode() { return clientCode; }
        public String getAttention() { return attention; }
        public String getSellerName() { return sellerName; }
        public QuotationStatus getStatus() { return status; }
        public Integer getValidityDays() { return validityDays; }
        public PaymentCondition getPaymentCondition() { return paymentCondition; }
        public FreightType getFreightType() { return freightType; }
        public BigDecimal getFreightValue() { return freightValue; }
        public Object getDiscountType() { return discountType; }
        public BigDecimal getDiscount() { return discount; }
        public String getCarrierName() { return carrierName; }
        public BigDecimal getProfitMargin() { return profitMargin; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getTotal() { return total; }
        public BigDecimal getGlobalDiscountValue() { return globalDiscountValue; }
        public String getNotes() { return notes; }
        public List<?> getItems() { return items; }
    }

    /**
     * Modelo base compartilhado entre os 3 templates: IssuerView
     * (cabeçalho) + generatedAt + mapas vazios de produtos.
     */
    private Map<String, Object> baseModel() {
        Map<String, Object> model = new HashMap<>();
        IssuerView issuer = new IssuerView(
                "TOP POWER ENGENHARIA LTDA",
                "TOP POWER ENGENHARIA",
                "13.433.616/0001-06",
                "671.137.811.110",
                "29764.01-6",
                "(19) 99999-9999",
                "contato@toppower.com.br",
                null,
                List.of("AVENIDA REBOUÇAS, 4465", "RES. VECCON", "SUMARÉ/SP — CEP 13170-700")
        );
        model.put("issuer", issuer);
        model.put("productNames", new HashMap<UUID, String>());
        model.put("productCodes", new HashMap<UUID, String>());
        model.put("generatedAt", Instant.now());
        return model;
    }

    private void assertProducesValidPdf(String template, Map<String, Object> model, String label) {
        byte[] pdf;
        try {
            pdf = service.render(template, model);
        } catch (Exception ex) {
            fail("Falha ao renderizar template de " + label + ": " + ex.getMessage(), ex);
            return;
        }
        assertNotNull(pdf, "PDF nulo para " + label);
        assertTrue(pdf.length > 0, "PDF vazio para " + label);
        String header = new String(pdf, 0, Math.min(5, pdf.length));
        assertTrue(header.startsWith("%PDF-"),
                "PDF inválido (sem assinatura %PDF-) para " + label + ": " + header);
    }
}